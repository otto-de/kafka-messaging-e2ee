package de.otto.kafka.messaging.e2ee.vault;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonArray;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.JsonValue;
import io.github.jopenlibs.vault.json.WriterConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is intended to be used as a 2nd-Level-Cache for the vault access. Meaning the results
 * of all method calls should be cached with a 1st-Level-Cache.
 */
public final class CachedEncryptionKeyProvider implements EncryptionKeyProvider {

  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX");
  private static final Logger log = LoggerFactory.getLogger(CachedEncryptionKeyProvider.class);

  private static final String NAME_ENTRIES = "entries";
  private static final String NAME_TOPIC = "topic";
  private static final String NAME_VERSION = "version";
  private static final String NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME = "encryptionKeyAttributeName";
  private static final String NAME_ENCODED_KEY = "encodedKey";
  private static final String NAME_EXPIRE_AT = "expireAt";

  private final ReentrantLock lock = new ReentrantLock();
  private final EncryptionKeyProvider realEncryptionKeyProvider;
  private final SecondLevelCacheStorage cacheStorage;
  private final Clock clock;
  private final Duration cachingDuration;
  private final int maxCacheSize;

  /**
   * @param realEncryptionKeyProvider the VaultEncryptionKeyProvider
   * @param cacheStorage              the 2nd-level cache storage
   * @param cachingDuration           the cache duration for the encryption keys. The decryption
   *                                  keys will never expire.
   */
  public CachedEncryptionKeyProvider(
      EncryptionKeyProvider realEncryptionKeyProvider,
      SecondLevelCacheStorage cacheStorage,
      Duration cachingDuration) {
    this(realEncryptionKeyProvider, cacheStorage, Clock.systemDefaultZone(), cachingDuration,
        Integer.MAX_VALUE);
  }

  /**
   * @param realEncryptionKeyProvider the VaultEncryptionKeyProvider
   * @param cacheStorage              the 2nd-level cache storage
   * @param cachingDuration           the cache duration for the encryption keys. The decryption
   *                                  keys will never expire.
   * @param clock                     a clock (used in unit tests)
   */
  public CachedEncryptionKeyProvider(
      EncryptionKeyProvider realEncryptionKeyProvider,
      SecondLevelCacheStorage cacheStorage,
      Clock clock,
      Duration cachingDuration) {
    this(realEncryptionKeyProvider, cacheStorage, clock, cachingDuration, Integer.MAX_VALUE);
  }

  /**
   * @param realEncryptionKeyProvider the VaultEncryptionKeyProvider
   * @param cacheStorage              the 2nd-level cache storage
   * @param cachingDuration           the cache duration for the encryption keys. The decryption
   *                                  keys will never expire.
   * @param clock                     a clock (used in unit tests)
   * @param maxCacheSize              the maximum allowed size (number of characters) of the cache
   *                                  storage - must be at least 500
   */
  CachedEncryptionKeyProvider(
      EncryptionKeyProvider realEncryptionKeyProvider,
      SecondLevelCacheStorage cacheStorage,
      Clock clock,
      Duration cachingDuration,
      Integer maxCacheSize) {
    this.realEncryptionKeyProvider = Objects.requireNonNull(realEncryptionKeyProvider,
        "realEncryptionKeyProvider is required");
    this.cacheStorage = Objects.requireNonNull(cacheStorage,
        "cacheStorage is required");
    this.clock = Objects.requireNonNull(clock,
        "clock is required");
    this.cachingDuration = Objects.requireNonNull(cachingDuration,
        "cachingDuration is required");
    this.maxCacheSize = Objects.requireNonNull(maxCacheSize,
        "maxCacheSize is required");
    if (this.maxCacheSize < 500) {
      throw new IllegalArgumentException("maxCacheSize must be at least 500 characters");
    }
  }

  /**
   * @return a builder for that class
   */
  public static CachedEncryptionKeyProviderBuilder builder() {
    return new CachedEncryptionKeyProviderBuilder();
  }

  @Override
  public KeyVersion retrieveKeyForEncryption(String topic) {
    lock.lock();
    try {
      List<CacheEntry> cacheEntries = loadCacheEntries();

      KeyVersion cachedKeyVersion = null;
      CacheEntry cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.topic())
              && entry.encryptionKeyName() != null
              && entry.expiredAtText() != null,
          this.sortByVersion());

      if (cachedKeyEntry != null) {
        // we have found a cache entry for that topic
        int version = cachedKeyEntry.version();
        String encryptionKeyAttributeName = cachedKeyEntry.encryptionKeyName();
        String encodedKey = cachedKeyEntry.encodedKey();
        cachedKeyVersion = new KeyVersion(version, encryptionKeyAttributeName, encodedKey);

        OffsetDateTime latestKeyVersionExpiredAt = cachedKeyEntry.expiredAtOffsetDateTime()
            // add up to 2 minutes, so we prevent peek cache expiration
            .plus(Math.round(Math.random() * 120_000), ChronoUnit.MILLIS);

        if (OffsetDateTime.now(clock).isBefore(latestKeyVersionExpiredAt)) {
          // create key version from cache entry
          log.debug("use cached key version for topic {}", topic);
          return cachedKeyVersion;
        }
      }

      // we have no cache entry or the cache entry has expired, so retrieve value from the real vault
      KeyVersion keyVersion;
      OffsetDateTime newExpiredAtTimestamp = retrieveNewExpiredAtTimestamp();
      try {
        keyVersion = realEncryptionKeyProvider.retrieveKeyForEncryption(topic);

        if (keyVersion == null) {
          // no encryption is needed, so no caching is needed
          return null;
        }

        // update cache
        if (cachedKeyEntry == null || !keyVersion.equals(cachedKeyVersion)) {
          CacheEntry cacheEntry = CacheEntry.of(topic, keyVersion)
              .updateExpiredAt(newExpiredAtTimestamp);
          cacheEntries.add(cacheEntry);
        } else {
          log.debug("update cached key version for topic {} with new expiry date {}", topic,
              newExpiredAtTimestamp);
          cachedKeyEntry.updateExpiredAt(newExpiredAtTimestamp);
        }
      } catch (Exception ex) {
        if (cachedKeyEntry == null) {
          throw ex;
        }
        log.warn("Retrieval of Vault EncryptionKey failed. Use cached EncryptionKey instead.", ex);
        cachedKeyEntry.updateExpiredAt(newExpiredAtTimestamp);
        keyVersion = cachedKeyVersion;
      }
      updateCache(cacheEntries, null);

      return keyVersion;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version) {
    lock.lock();
    try {
      List<CacheEntry> cacheEntries = loadCacheEntries();
      CacheEntry cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.topic())
              && version == entry.version()
              && entry.encryptionKeyName() == null,
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.encodedKey();
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version);

      // update cache
      CacheEntry newCacheEntry = CacheEntry.of(topic, version, encodedKey);
      updateCache(cacheEntries, newCacheEntry);

      return encodedKey;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version,
      String encryptionKeyAttributeName) {
    lock.lock();
    try {
      List<CacheEntry> cacheEntries = loadCacheEntries();
      CacheEntry cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.topic())
              && version == entry.version()
              && Objects.equals(encryptionKeyAttributeName, entry.encryptionKeyName()),
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.encodedKey();
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version,
          encryptionKeyAttributeName);

      // update cache
      CacheEntry newCacheEntry = CacheEntry.of(topic, version, encryptionKeyAttributeName,
          encodedKey);
      updateCache(cacheEntries, newCacheEntry);

      return encodedKey;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isEncryptedTopic(String kafkaTopicName) {
    return realEncryptionKeyProvider.isEncryptedTopic(kafkaTopicName);
  }

  private void updateCache(List<CacheEntry> oldCacheEntries, CacheEntry newEntry) {
    if (newEntry != null) {
      oldCacheEntries.add(newEntry);
    }

    List<CacheEntry> cacheEntries = sanitizeCacheEntries(oldCacheEntries);
    JsonArray jsonArrayEntries = new JsonArray();
    for (CacheEntry cacheEntry : cacheEntries) {
      jsonArrayEntries.add(cacheEntry.toJson());
    }
    JsonObject jsonObjectRoot = new JsonObject();
    jsonObjectRoot.add(NAME_ENTRIES, jsonArrayEntries);
    String newCachePayload = jsonObjectRoot.toString(WriterConfig.MINIMAL);
    storeNewCacheEntry(newCachePayload);
  }

  private List<CacheEntry> sanitizeCacheEntries(List<CacheEntry> oldCacheEntries) {
    // order entries by topic and version
    return oldCacheEntries;
  }

  private void storeNewCacheEntry(String newCachePayload) {
    if (newCachePayload.length() >= maxCacheSize) {
      // wipe the cache if it is too long
      log.info(
          "2nd-level cache value is too long to store. Just use an empty value for the cache.");
      newCachePayload = "{}";
    }

    try {
      cacheStorage.storeEntry(newCachePayload);
    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug(ex.getMessage(), ex);
      } else {
        log.warn("Failed to store 2nd-level cache value. Error: {}", ex.getMessage());
      }
    }
  }

  private OffsetDateTime retrieveNewExpiredAtTimestamp() {
    return OffsetDateTime.now(clock)
        .withOffsetSameInstant(ZoneOffset.UTC)
        .plus(cachingDuration);
  }

  private List<CacheEntry> loadCacheEntries() {
    String cachedPayload = null;
    try {
      cachedPayload = cacheStorage.retrieveEntry();
    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug(ex.getMessage(), ex);
      } else {
        log.warn("Failed to load 2nd-level cache value. Error: {}", ex.getMessage());
      }
    }

    if (cachedPayload == null || cachedPayload.isEmpty() || "{}".equals(cachedPayload)) {
      return new ArrayList<>();
    }
    JsonObject jsonObjectRoot = Json.parse(cachedPayload).asObject();
    if (jsonObjectRoot.get(NAME_ENTRIES) == null) {
      return new ArrayList<>();
    }
    JsonArray jsonArrayEntries = jsonObjectRoot.get(NAME_ENTRIES).asArray();
    List<CacheEntry> cacheEntries = new ArrayList<>();
    for (JsonValue jsonValue : jsonArrayEntries.values()) {
      cacheEntries.add(CacheEntry.of(jsonValue.asObject()));
    }
    return cacheEntries;
  }

  private Comparator<CacheEntry> sortByVersion() {
    return Comparator.comparingInt(CacheEntry::version);
  }

  private Comparator<CacheEntry> noSortOrder() {
    return Comparator.comparingInt(entry -> 0);
  }

  private CacheEntry findAtMostOneEntry(List<CacheEntry> values,
      Predicate<CacheEntry> filter,
      Comparator<CacheEntry> comparator) {
    CacheEntry currentBestValue = null;
    List<CacheEntry> matchingCurrentBestValues = new ArrayList<>();
    for (CacheEntry singleValue : values) {
      if (!filter.test(singleValue)) {
        // singleValue does not match the given filter
        continue;
      }

      int compValue = -1;
      if (currentBestValue != null) {
        compValue = comparator.compare(currentBestValue, singleValue);
      }

      if (compValue < 0) {
        // singleValue has a newer version then the latest value
        matchingCurrentBestValues.clear();
        matchingCurrentBestValues.add(singleValue);
        currentBestValue = singleValue;
      } else if (compValue == 0) {
        // singleValue has the same version then the latest value
        matchingCurrentBestValues.add(singleValue);
      }
    }

    if (matchingCurrentBestValues.size() > 1) {
      throw new VaultRuntimeException(
          "None deterministic encryption key. May clear your 2nd-Level-Cache to resolve the issue.");
    }

    return currentBestValue;
  }

  private static class CacheEntry {

    private final String topic;
    private final int version;
    private final String encryptionKeyName;
    private final String encodedKey;
    private String expiredAtText;

    CacheEntry(String topic, int version, String encryptionKeyName, String encodedKey,
        String expiredAtText) {
      this.topic = Objects.requireNonNull(topic, "topic");
      this.version = version;
      this.encryptionKeyName = encryptionKeyName;
      this.encodedKey = Objects.requireNonNull(encodedKey, "encodedKey");
      this.expiredAtText = expiredAtText;
    }

    static CacheEntry of(String topic, KeyVersion keyVersion) {
      return new CacheEntry(topic, keyVersion.version(), keyVersion.encryptionKeyAttributeName(),
          keyVersion.encodedKey(), null);
    }

    static CacheEntry of(String topic, int version, String encodedKey) {
      return new CacheEntry(topic, version, null, encodedKey, null);
    }

    static CacheEntry of(String topic, int version, String encryptionKeyName, String encodedKey) {
      Objects.requireNonNull(encryptionKeyName, "encryptionKeyName");
      return new CacheEntry(topic, version, encryptionKeyName, encodedKey, null);
    }


    static CacheEntry of(JsonObject json) {
      String topic = json.getString(NAME_TOPIC);
      int version = json.getInt(NAME_VERSION);
      String encryptionKeyName = json.getString(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME);
      String encodedKey = json.getString(NAME_ENCODED_KEY);
      String expiredAtText = json.getString(NAME_EXPIRE_AT);
      return new CacheEntry(topic, version, encryptionKeyName, encodedKey, expiredAtText);
    }

    JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.add(NAME_TOPIC, Json.value(topic));
      json.add(NAME_VERSION, Json.value(version));
      if (encryptionKeyName != null) {
        json.add(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME, Json.value(encryptionKeyName));
      }
      json.add(NAME_ENCODED_KEY, Json.value(encodedKey));
      if (expiredAtText != null) {
        json.add(NAME_EXPIRE_AT, Json.value(expiredAtText));
      }
      return json;
    }

    String topic() {
      return topic;
    }

    int version() {
      return version;
    }

    String encryptionKeyName() {
      return encryptionKeyName;
    }

    String encodedKey() {
      return encodedKey;
    }

    String expiredAtText() {
      return expiredAtText;
    }

    OffsetDateTime expiredAtOffsetDateTime() {
      Objects.requireNonNull(expiredAtText, "expiredAtText");
      return OffsetDateTime.parse(expiredAtText, DTF);
    }

    CacheEntry updateExpiredAt(OffsetDateTime newExpiredAt) {
      this.expiredAtText = newExpiredAt.format(DTF);
      return this;
    }
  }

  public static class CachedEncryptionKeyProviderBuilder {

    private EncryptionKeyProvider realEncryptionKeyProvider;
    private SecondLevelCacheStorage cacheStorage;
    private Clock clock;
    private Duration cachingDuration;
    private Integer maxCacheSize;

    /**
     * @param realEncryptionKeyProvider the real EncryptionKeyProvider
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder realEncryptionKeyProvider(
        EncryptionKeyProvider realEncryptionKeyProvider) {
      this.realEncryptionKeyProvider = realEncryptionKeyProvider;
      return this;
    }

    /**
     * @param cacheStorage the second level cache storage interface
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder cacheStorage(SecondLevelCacheStorage cacheStorage) {
      this.cacheStorage = cacheStorage;
      return this;
    }

    /**
     * @param clock a clock (set that for tests only)
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * @param cachingDuration the cache duration for the encryption keys. The decryption keys will
     *                        never expire.
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder cachingDuration(Duration cachingDuration) {
      this.cachingDuration = cachingDuration;
      return this;
    }

    /**
     * @param maxCacheSize the maximum allowed size (number of characters) of the cache storage -
     *                     must be at least 500
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder maxCacheSize(Integer maxCacheSize) {
      this.maxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * @return the built CachedEncryptionKeyProvider
     */
    public CachedEncryptionKeyProvider build() {
      if (clock == null) {
        clock = Clock.systemDefaultZone();
      }
      if (maxCacheSize == null) {
        maxCacheSize = Integer.MAX_VALUE;
      }

      return new CachedEncryptionKeyProvider(realEncryptionKeyProvider, cacheStorage, clock,
          cachingDuration, maxCacheSize);
    }
  }
}
