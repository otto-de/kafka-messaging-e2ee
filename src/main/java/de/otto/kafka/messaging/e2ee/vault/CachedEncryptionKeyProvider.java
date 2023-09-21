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
public class CachedEncryptionKeyProvider implements EncryptionKeyProvider {

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

  /**
   * @param realEncryptionKeyProvider the VaultEncryptionKeyProvider
   * @param cacheStorage              the 2nd-level cache storage
   */
  public CachedEncryptionKeyProvider(
      EncryptionKeyProvider realEncryptionKeyProvider,
      SecondLevelCacheStorage cacheStorage) {
    this(realEncryptionKeyProvider, cacheStorage, Clock.systemDefaultZone(), Duration.ofHours(2));
  }

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
    this(realEncryptionKeyProvider, cacheStorage, Clock.systemDefaultZone(), cachingDuration);
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
    this.realEncryptionKeyProvider = Objects.requireNonNull(realEncryptionKeyProvider,
        "realEncryptionKeyProvider is required");
    this.cacheStorage = Objects.requireNonNull(cacheStorage,
        "cacheStorage is required");
    this.clock = Objects.requireNonNull(clock,
        "clock is required");
    this.cachingDuration = Objects.requireNonNull(cachingDuration,
        "cachingDuration is required");
  }

  @Override
  public KeyVersion retrieveKeyForEncryption(String topic) {
    lock.lock();
    try {
      List<JsonObject> cacheEntries = loadCacheEntries();

      KeyVersion cachedKeyVersion = null;
      JsonObject cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.getString(NAME_TOPIC))
              && entry.getString(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME) != null
              && entry.getString(NAME_EXPIRE_AT) != null,
          this.sortByVersion());

      if (cachedKeyEntry != null) {
        int version = cachedKeyEntry.getInt(NAME_VERSION);
        String encryptionKeyAttributeName = cachedKeyEntry.getString(
            NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME);
        String encodedKey = cachedKeyEntry.getString(NAME_ENCODED_KEY);
        cachedKeyVersion = new KeyVersion(version, encryptionKeyAttributeName, encodedKey);

        OffsetDateTime latestKeyVersionExpiredAt = OffsetDateTime.parse(
                cachedKeyEntry.getString(NAME_EXPIRE_AT), DTF)
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
      String expiredAtTimestampString = retrieveNewExpiredAtTimestamp();
      try {
        keyVersion = realEncryptionKeyProvider.retrieveKeyForEncryption(topic);

        if (keyVersion == null) {
          // no encryption is needed, so no caching is needed
          return null;
        }

        // update cache
        if (cachedKeyEntry == null || !keyVersion.equals(cachedKeyVersion)) {
          JsonObject jsonObjectSingleKeyVersion = new JsonObject();
          jsonObjectSingleKeyVersion.add(NAME_TOPIC, Json.value(topic));
          jsonObjectSingleKeyVersion.add(NAME_VERSION, Json.value(keyVersion.version()));
          jsonObjectSingleKeyVersion.add(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME,
              Json.value(Objects.requireNonNull(keyVersion.encryptionKeyAttributeName())));
          jsonObjectSingleKeyVersion.add(NAME_ENCODED_KEY, Json.value(keyVersion.encodedKey()));
          jsonObjectSingleKeyVersion.add(NAME_EXPIRE_AT, Json.value(expiredAtTimestampString));
          cacheEntries.add(jsonObjectSingleKeyVersion);
        } else {
          log.debug("update cached key version for topic {} with new expiry date {}", topic,
              expiredAtTimestampString);
          cachedKeyEntry.set(NAME_EXPIRE_AT, Json.value(expiredAtTimestampString));
        }
      } catch (Exception ex) {
        if (cachedKeyEntry == null) {
          throw ex;
        }
        log.warn("Retrieval of Vault EncryptionKey failed. Use cached EncryptionKey instead.", ex);
        cachedKeyEntry.set(NAME_EXPIRE_AT, Json.value(expiredAtTimestampString));
        keyVersion = cachedKeyVersion;
      }

      JsonArray jsonArrayEntries = new JsonArray();
      for (JsonValue cacheEntry : cacheEntries) {
        jsonArrayEntries.add(cacheEntry);
      }

      JsonObject jsonObjectRoot = new JsonObject();
      jsonObjectRoot.add(NAME_ENTRIES, jsonArrayEntries);
      String newCachePayload = jsonObjectRoot.toString(WriterConfig.MINIMAL);

      try {
        cacheStorage.storeEntry(newCachePayload);
      } catch (Exception ex) {
        if (log.isDebugEnabled()) {
          log.debug(ex.getMessage(), ex);
        } else {
          log.warn("Failed to store 2nd-level cache value. Error: {}", ex.getMessage());
        }
      }
      return keyVersion;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version) {
    lock.lock();
    try {
      List<JsonObject> cacheEntries = loadCacheEntries();
      JsonObject cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.getString(NAME_TOPIC))
              && version == entry.getInt(NAME_VERSION)
              && entry.getString(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME) == null,
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.getString(NAME_ENCODED_KEY);
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version);

      // update cache
      JsonObject jsonObjectSingleKeyVersion = new JsonObject();
      jsonObjectSingleKeyVersion.add(NAME_TOPIC, Json.value(topic));
      jsonObjectSingleKeyVersion.add(NAME_VERSION, Json.value(version));
      jsonObjectSingleKeyVersion.add(NAME_ENCODED_KEY, Json.value(encodedKey));
      cacheEntries.add(jsonObjectSingleKeyVersion);

      JsonArray jsonArrayEntries = new JsonArray();
      for (JsonValue cacheEntry : cacheEntries) {
        jsonArrayEntries.add(cacheEntry);
      }

      JsonObject jsonObjectRoot = new JsonObject();
      jsonObjectRoot.add(NAME_ENTRIES, jsonArrayEntries);
      String newCachePayload = jsonObjectRoot.toString(WriterConfig.MINIMAL);

      try {
        cacheStorage.storeEntry(newCachePayload);
      } catch (Exception ex) {
        if (log.isDebugEnabled()) {
          log.debug(ex.getMessage(), ex);
        } else {
          log.warn("Failed to store 2nd-level cache value. Error: {}", ex.getMessage());
        }
      }

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
      List<JsonObject> cacheEntries = loadCacheEntries();
      JsonObject cachedKeyEntry = findAtMostOneEntry(cacheEntries,
          entry -> Objects.equals(topic, entry.getString(NAME_TOPIC))
              && version == entry.getInt(NAME_VERSION)
              && Objects.equals(encryptionKeyAttributeName,
              entry.getString(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME)),
          this.noSortOrder());

      if (cachedKeyEntry != null) {
        return cachedKeyEntry.getString(NAME_ENCODED_KEY);
      }

      // fetch key for decryption from real vault
      String encodedKey = realEncryptionKeyProvider.retrieveKeyForDecryption(topic, version,
          encryptionKeyAttributeName);

      // update cache
      JsonObject jsonObjectSingleKeyVersion = new JsonObject();
      jsonObjectSingleKeyVersion.add(NAME_TOPIC, Json.value(topic));
      jsonObjectSingleKeyVersion.add(NAME_VERSION, Json.value(version));
      jsonObjectSingleKeyVersion.add(NAME_ENCRYPTION_KEY_ATTRIBUTE_NAME,
          Json.value(Objects.requireNonNull(encryptionKeyAttributeName)));
      jsonObjectSingleKeyVersion.add(NAME_ENCODED_KEY, Json.value(encodedKey));
      cacheEntries.add(jsonObjectSingleKeyVersion);

      JsonArray jsonArrayEntries = new JsonArray();
      for (JsonValue cacheEntry : cacheEntries) {
        jsonArrayEntries.add(cacheEntry);
      }

      JsonObject jsonObjectRoot = new JsonObject();
      jsonObjectRoot.add(NAME_ENTRIES, jsonArrayEntries);
      String newCachePayload = jsonObjectRoot.toString(WriterConfig.MINIMAL);

      cacheStorage.storeEntry(newCachePayload);
      return encodedKey;
    } finally {
      lock.unlock();
    }
  }

  private String retrieveNewExpiredAtTimestamp() {
    return OffsetDateTime.now(clock)
        .withOffsetSameInstant(ZoneOffset.UTC)
        .plus(cachingDuration)
        .format(DTF);
  }

  private List<JsonObject> loadCacheEntries() {
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

    if (cachedPayload == null || cachedPayload.isEmpty()) {
      return new ArrayList<>();
    }
    JsonObject jsonObjectRoot = Json.parse(cachedPayload).asObject();
    if (jsonObjectRoot.get(NAME_ENTRIES) == null) {
      return new ArrayList<>();
    }
    JsonArray jsonArrayEntries = jsonObjectRoot.get(NAME_ENTRIES).asArray();
    List<JsonObject> cacheEntries = new ArrayList<>();
    for (JsonValue jsonValue : jsonArrayEntries.values()) {
      cacheEntries.add(jsonValue.asObject());
    }
    return cacheEntries;
  }

  private Comparator<JsonObject> sortByVersion() {
    return Comparator.comparingInt(entry -> entry.getInt(NAME_VERSION));
  }

  private Comparator<JsonObject> noSortOrder() {
    return Comparator.comparingInt(entry -> 0);
  }

  private JsonObject findAtMostOneEntry(List<JsonObject> values,
      Predicate<JsonObject> filter,
      Comparator<JsonObject> comparator) {
    JsonObject currentBestValue = null;
    List<JsonObject> matchingCurrentBestValues = new ArrayList<>();
    for (JsonObject singleValue : values) {
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
      throw new VaultRuntimeException("None deterministic encryption key.");
    }

    return currentBestValue;
  }
}
