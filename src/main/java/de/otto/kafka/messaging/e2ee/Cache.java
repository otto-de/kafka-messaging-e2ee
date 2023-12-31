package de.otto.kafka.messaging.e2ee;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple cache. It removes the cache entry one hour (it's the "cachingDuration") after it has
 * been created (added to the cache).
 *
 * @param <K> type of the cache key
 * @param <V> type of the cache entries
 */
class Cache<K, V> {

  private static final Logger log = LoggerFactory.getLogger(Cache.class);

  private final ConcurrentHashMap<K, CacheEntry<V>> cacheEntries;
  private final Duration cachingDuration;
  private final Clock clock;

  /**
   * @param cachingDuration the duration after which a cache entry will be removed from the cache.
   */
  public Cache(Duration cachingDuration) {
    this(cachingDuration, Clock.systemDefaultZone());
  }

  /**
   * @param cachingDuration the duration after which a cache entry will be removed from the cache.
   * @param clock           a clock - used in unit tests
   */
  public Cache(Duration cachingDuration, Clock clock) {
    this.cachingDuration = Objects.requireNonNull(cachingDuration,
        "cachingDuration must not be null");
    this.clock = Objects.requireNonNull(clock,
        "clock must not be null");
    this.cacheEntries = new ConcurrentHashMap<>();
  }

  /**
   * tries to get the cache entry with the given key. If it is not present or the caching duration
   * was exceeded, the "cacheMissValueFunction" will be called to create a new cache entry.
   *
   * @param key                    the cache key
   * @param cacheMissValueFunction callback function to somehow retrieve a value that will be
   *                               cached. The function can return <code>null</code>.
   * @return the cached value. It can be <code>null</code>.
   */
  public V getOrRetrieve(K key, Function<K, V> cacheMissValueFunction) {
    CacheEntry<V> cacheEntry = cacheEntries.computeIfAbsent(key,
        k -> createCacheEntry(k, cacheMissValueFunction));
    if (cacheEntry.validUntil().isBefore(LocalDateTime.now(clock))) {
      cacheEntry = createCacheEntry(key, cacheMissValueFunction);
      cacheEntries.put(key, cacheEntry);
    }
    return cacheEntry.value();
  }

  private CacheEntry<V> createCacheEntry(K key, Function<K, V> cacheMissValueFunction) {
    V value = cacheMissValueFunction.apply(key);
    LocalDateTime validUntil = LocalDateTime.now(clock)
        .plus(cachingDuration)
        // add up to 2 minutes, so we prevent peek cache expirations
        .plus(Math.round(Math.random() * 120_000), ChronoUnit.MILLIS);

    log.debug("Create 1st-level cache entry for key={} which expires at {}", key, validUntil);
    return new CacheEntry<>(value, validUntil);
  }

  private record CacheEntry<V>(
      V value,
      LocalDateTime validUntil
  ) {

    private CacheEntry {
      Objects.requireNonNull(validUntil);
    }
  }
}