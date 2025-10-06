package de.otto.kafka.messaging.e2ee.vault;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
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

  private final ReentrantLock lock = new ReentrantLock();
  private final EncryptionKeyProvider realEncryptionKeyProvider;
  private final SecondLevelCacheStorage cacheStorage;
  private final Clock clock;
  private final Duration cachingDuration;
  private final int maxCacheSize;

  /**
   * Constructor of that class.
   *
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
   * Test constructor of that class.
   *
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
   * Test constructor of that class.
   *
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
   * Returns a builder for that class.
   *
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
              && entry.hasEncryptionKeyName()
              && entry.hasExpiredAt(),
          this.sortByVersion());

      if (cachedKeyEntry != null) {
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
      OffsetDateTime newExpiredAt = retrieveNewExpiredAtTimestamp();
      try {
        keyVersion = realEncryptionKeyProvider.retrieveKeyForEncryption(topic);

        if (keyVersion == null) {
          // no encryption is needed, so no caching is needed
          return null;
        }

        // update cache
        if (cachedKeyEntry == null || !keyVersion.equals(cachedKeyVersion)) {
          CacheEntry newCacheEntry = CacheEntry.forDecryption(topic, keyVersion, newExpiredAt);
          cacheEntries.add(newCacheEntry);
        } else {
          log.debug("update cached key version for topic {} with new expiry date {}", topic,
              newExpiredAt);
          cachedKeyEntry.updateExpiredAt(newExpiredAt);
        }
      } catch (Exception ex) {
        if (cachedKeyEntry == null) {
          throw ex;
        }
        log.warn("Retrieval of Vault EncryptionKey failed. Use cached EncryptionKey instead.", ex);
        cachedKeyEntry.updateExpiredAt(newExpiredAt);
        keyVersion = cachedKeyVersion;
      }
      updateCache(cacheEntries);

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
              && !entry.hasExpiredAt()
              && !entry.hasEncryptionKeyName(),
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.encodedKey();
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version);

      // update cache
      CacheEntry cacheEntry = CacheEntry.forEncryption(topic, version, encodedKey);
      cacheEntries.add(cacheEntry);
      updateCache(cacheEntries);

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
              && !entry.hasExpiredAt()
              && Objects.equals(encryptionKeyAttributeName, entry.encryptionKeyName()),
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.encodedKey();
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version,
          encryptionKeyAttributeName);

      // update cache
      CacheEntry cacheEntry = CacheEntry.forEncryptionWithKeyName(topic, version,
          encryptionKeyAttributeName, encodedKey);
      cacheEntries.add(cacheEntry);
      updateCache(cacheEntries);

      return encodedKey;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isEncryptedTopic(String kafkaTopicName) {
    return realEncryptionKeyProvider.isEncryptedTopic(kafkaTopicName);
  }

  private void updateCache(List<CacheEntry> oldCacheEntries) {
    // order entries by topic and version
    oldCacheEntries.sort(CacheEntry::compare);

    // build cache content
    StringBuilder cacheContent = new StringBuilder();
    cacheContent.append("v1\n");

    String currentTopic = "";
    for (CacheEntry entry : oldCacheEntries) {
      if (!entry.topic().equals(currentTopic)) {
        cacheContent.append("t:").append(entry.topic()).append("\n");
        currentTopic = entry.topic();
      }
      cacheContent.append("v:").append(entry.version());
      if (entry.expiredAtText() != null) {
        cacheContent.append(" ex:").append(entry.expiredAtText());
      }
      if (entry.encryptionKeyName() != null) {
        cacheContent.append(" kn:").append(entry.encryptionKeyName());
      }
      cacheContent.append(" ek:").append(entry.encodedKey()).append("\n");
    }

    // store cache content
    storeNewCacheEntry(cacheContent.toString());
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

  private List<CacheEntry> loadCacheEntries() {
    ArrayList<CacheEntry> cacheEntries = new ArrayList<>();
    try {
      String cachedPayload = cacheStorage.retrieveEntry();

      if (cachedPayload == null || !cachedPayload.startsWith("v1\n")) {
        return cacheEntries;
      }

      String[] lines = cachedPayload.split("\n");
      String topic = null;
      for (String line : lines) {
        if (line.startsWith("t:")) {
          // new topic
          topic = line.substring(2);
        } else if (line.startsWith("v:")) {
          String[] rawDataArray = line.split("\\s+");

          String versionText = "N/A";
          String encryptionKeyName = null;
          String encodedKey = null;
          String expiredAtText = null;

          for (String rawData : rawDataArray) {
            if (rawData.startsWith("v:")) {
              versionText = rawData.substring(2);
            } else if (rawData.startsWith("kn:")) {
              encryptionKeyName = rawData.substring(3);
            } else if (rawData.startsWith("ek:")) {
              encodedKey = rawData.substring(3);
            } else if (rawData.startsWith("ex:")) {
              expiredAtText = rawData.substring(3);
            } else {
              log.debug("Ignore entry {} in cache line: {}", rawData, line);
            }
          }

          cacheEntries.add(new CacheEntry(topic, Integer.parseInt(versionText),
              encryptionKeyName, encodedKey, expiredAtText));
        } else if (!line.equals("v1")) {
          log.debug("Ignore cache line: {}", line);
        }
      }
    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug(ex.getMessage(), ex);
      } else {
        log.warn("Failed to load 2nd-level cache value. Error: {}", ex.getMessage());
      }
    }

    return cacheEntries;
  }

  private OffsetDateTime retrieveNewExpiredAtTimestamp() {
    return OffsetDateTime.now(clock)
        .withOffsetSameInstant(ZoneOffset.UTC)
        .plus(cachingDuration);
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

    public static CacheEntry forDecryption(String topic, KeyVersion keyVersion,
        OffsetDateTime expiredAt) {
      return new CacheEntry(topic, keyVersion.version(), keyVersion.encryptionKeyAttributeName(),
          keyVersion.encodedKey(), expiredAt.format(DTF));
    }

    public static CacheEntry forEncryption(String topic, int version, String encodedKey) {
      return new CacheEntry(topic, version, null, encodedKey, null);
    }

    public static CacheEntry forEncryptionWithKeyName(String topic, int version,
        String encryptionKeyName,
        String encodedKey) {
      return new CacheEntry(topic, version, encryptionKeyName, encodedKey, null);
    }

    String topic() {
      return topic;
    }

    int version() {
      return version;
    }

    boolean hasEncryptionKeyName() {
      return encryptionKeyName != null;
    }

    String encryptionKeyName() {
      return encryptionKeyName;
    }

    String encodedKey() {
      return encodedKey;
    }

    boolean hasExpiredAt() {
      return expiredAtText != null;
    }

    String expiredAtText() {
      return expiredAtText;
    }

    OffsetDateTime expiredAtOffsetDateTime() {
      Objects.requireNonNull(expiredAtText, "expiredAtText");
      return OffsetDateTime.parse(expiredAtText, DTF);
    }

    void updateExpiredAt(OffsetDateTime newExpiredAt) {
      this.expiredAtText = newExpiredAt.format(DTF);
    }

    @Override
    public String toString() {
      return "CacheEntry{" + topic + ", version=" + version
          + (encryptionKeyName == null ? "" : ", encryptionKeyName='" + encryptionKeyName + '\'')
          + (expiredAtText == null ? "" : ", expiredAtText='" + expiredAtText + '\'')
          + ", encodedKey='" + encodedKey + '\''
          + '}';
    }

    public int compare(CacheEntry other) {
      int comp = compareString(this.topic, other.topic);
      if (comp != 0) {
        return comp;
      }

      comp = Integer.compare(this.version, other.version);
      if (comp != 0) {
        return -1 * comp;
      }

      comp = compareString(this.encryptionKeyName, other.encryptionKeyName);
      if (comp != 0) {
        return comp;
      }

      return compareString(this.expiredAtText, other.expiredAtText);
    }

    private static int compareString(String a, String b) {
      if (a != null && b != null) {
        return a.compareTo(b);
      }
      if (a == null && b != null) {
        return 1;
      }
      if (a != null) {
        return -1;
      }
      return 0;
    }
  }

  /**
   * Builder for CachedEncryptionKeyProvider instances.
   */
  public static class CachedEncryptionKeyProviderBuilder {

    private EncryptionKeyProvider realEncryptionKeyProvider;
    private SecondLevelCacheStorage cacheStorage;
    private Clock clock;
    private Duration cachingDuration;
    private Integer maxCacheSize;

    /**
     * The standard constructor.
     */
    public CachedEncryptionKeyProviderBuilder() {
    }

    /**
     * Sets the real EncryptionKeyProvider.
     *
     * @param realEncryptionKeyProvider the real EncryptionKeyProvider
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder realEncryptionKeyProvider(
        EncryptionKeyProvider realEncryptionKeyProvider) {
      this.realEncryptionKeyProvider = realEncryptionKeyProvider;
      return this;
    }

    /**
     * Sets the cache storage engine.
     *
     * @param cacheStorage the second level cache storage interface
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder cacheStorage(SecondLevelCacheStorage cacheStorage) {
      this.cacheStorage = cacheStorage;
      return this;
    }

    /**
     * Sets the clock which is needed to expire the cache entries.
     *
     * @param clock a clock (set that for tests only)
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Sets the max caching duration for encryption keys.
     *
     * @param cachingDuration the cache duration for the encryption keys. The decryption keys will
     *                        never expire.
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder cachingDuration(Duration cachingDuration) {
      this.cachingDuration = cachingDuration;
      return this;
    }

    /**
     * Sets the cache size limit.
     *
     * @param maxCacheSize the maximum allowed size (number of characters) of the cache storage -
     *                     must be at least 500
     * @return this
     */
    public CachedEncryptionKeyProviderBuilder maxCacheSize(Integer maxCacheSize) {
      this.maxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * Creates the CachedEncryptionKeyProvider.
     *
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
