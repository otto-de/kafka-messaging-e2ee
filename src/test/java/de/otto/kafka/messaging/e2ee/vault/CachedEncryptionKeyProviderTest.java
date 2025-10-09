package de.otto.kafka.messaging.e2ee.vault;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider.KeyVersion;
import de.otto.kafka.messaging.e2ee.helper.CacheStorageMock;
import de.otto.kafka.messaging.e2ee.helper.EncryptionKeyProviderMock;
import de.otto.kafka.messaging.e2ee.helper.TestClock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedEncryptionKeyProviderTest {

  private static final String TOPIC = "someTopic";
  private CachedEncryptionKeyProvider cachedEncryptionKeyProvider;

  private TestClock testClock;
  private EncryptionKeyProviderMock realEncryptionKeyProvider;
  private CacheStorageMock cacheStorage;

  @BeforeEach
  void setup() {
    testClock = new TestClock("2023-08-01T17:45Z");
    realEncryptionKeyProvider = new EncryptionKeyProviderMock();
    cacheStorage = new CacheStorageMock();
    cachedEncryptionKeyProvider = CachedEncryptionKeyProvider.builder()
        .realEncryptionKeyProvider(realEncryptionKeyProvider)
        .cacheStorage(cacheStorage)
        .clock(testClock)
        .cachingDuration(Duration.ofHours(3))
        .maxCacheSize(500)
        .build();
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenNoCacheEntryIsPresent() {
    // given: an empty cache
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: cacheStorage should have been called
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":[{\"t\":\"someTopic\",\"e\":[{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:45Z\",\"k\":\"someSecret\"}]}]}";
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForEncryption(someTopic)");
  }

  @Test
  void shouldRetrieveKeyForEncryptionWithEmptyCache() {
    // given: an empty cache
    cacheStorage.initEntry("{}");
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // when: method is called a 2nd time
    cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)", "retrieveEntry()");
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndNotExpired() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:00Z\",\"k\":\"someSecret-v2\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T16:55Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(2);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret-v2");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls()).isEmpty();
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()");
  }

  @Test
  void shouldLoadCacheEntriesForVersion1() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":["
        + "{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:05Z\"},"
        + "{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T17:00Z\"}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForEncryption(someTopic)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // then: cache should have the correct value
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:10Z\",\"k\":\"someSecret\"},"
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:05Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentButIsExpiredAndUnchanged() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T17:00Z\",\"k\":\"someSecret\"},"
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:05Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForEncryption(someTopic)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // then: cache should have the correct value
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:10Z\",\"k\":\"someSecret\"},"
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:05Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentButIsExpiredAndWithNewVersion() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:05Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForEncryption(someTopic)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // then: cache should have the correct value
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:10Z\",\"k\":\"someSecret\"},"
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:05Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndIsExpiredButRealEncryptionProviderHasAnException() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T17:00Z\",\"k\":\"someSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // given: realEncryptionKeyProvider throws an exception
    realEncryptionKeyProvider.setThrowException(true);
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(2);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // then: cache should have the correct value
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":2,\"n\":\"aes\",\"x\":\"2023-08-01T20:10Z\",\"k\":\"someSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndNotExpiredButHasTwoEntries() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T17:00Z\",\"k\":\"someSecret\"},"
        + "{\"v\":1,\"n\":\"aes\",\"x\":\"2023-08-01T16:30Z\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T16:20Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls()).isEmpty();
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()");
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheForTopicIsNotPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someOtherTopic\",\"e\":["
        + "{\"v\":5,\"n\":\"aes\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result).isNotNull();
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: realEncryptionKeyProvider should not have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForEncryption(someTopic)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
  }

  @Test
  void shouldRetrieveKeyForDecryptionWhenCacheIsNotPresent() {
    // given: an empty cache
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3);
    // then: result should be valid
    assertThat(result).isEqualTo("someSecret2");
    // then: cacheStorage should have been called
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"k\":\"someSecret2\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForDecryption(someTopic, 3)");
  }

  @Test
  void shouldRetrieveKeyForDecryptionWhenCacheIsFilledButVersionIsNotPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":5,\"n\":\"aes\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 6);
    // then: result should be valid
    assertThat(result).isEqualTo("someSecret2");
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForDecryption(someTopic, 6)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":6,\"k\":\"someSecret2\"},"
        + "{\"v\":5,\"n\":\"aes\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":6,\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 6);
    // then: result should be valid
    assertThat(result).isEqualTo("someOtherSecret");
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls()).isEmpty();
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()");
  }


  @Test
  void shouldRetrieveKeyForDecryptionWithNameWhenCacheIsNotPresent() {
    // given: an empty cache
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3, "aes");
    // then: result should be valid
    assertThat(result).isEqualTo("someSecret3");
    // then: cacheStorage should have been called
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"k\":\"someSecret3\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForDecryption(someTopic, 3, aes)");
  }

  @Test
  void shouldRetrieveKeyForDecryptionWithNameWhenCacheIsFilledButVersionAndNameIsNotPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":6,\"n\":\"ssl\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 6, "aes");
    // then: result should be valid
    assertThat(result).isEqualTo("someSecret3");
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForDecryption(someTopic, 6, aes)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":6,\"n\":\"aes\",\"k\":\"someSecret3\"},"
        + "{\"v\":6,\"n\":\"ssl\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWithNameWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":6,\"n\":\"aes\",\"k\":\"someOtherSecret\"}"
        + "]}"
        + "]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // when: method is called
    String result = cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 6, "aes");
    // then: result should be valid
    assertThat(result).isEqualTo("someOtherSecret");
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls()).isEmpty();
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()");
  }

  @Test
  void shouldShrinkStoredPayload() {
    // given: a very long cache entry
    StringBuilder sb = new StringBuilder();
    sb.append("{\"v\":2,\"t\":[{\"t\":\"someOtherTopic\",\"e\":[");
    for (int version = 1; version <= 8; version++) {
      String part = "{\"v\":9999,\"n\":\"aes\",\"k\":\"someOtherSecret\",\"x\":\"2023-08-01T17:45Z\"}"
          .replace("9999", Integer.toString(version));
      if (version > 1) {
        sb.append(",");
      }
      sb.append(part);
    }
    sb.append("]}]}");
    String cacheEntryPayload = sb.toString();
    assertThat(cacheEntryPayload.length()).isGreaterThan(500);
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 6, "aes");
    // then: realEncryptionKeyProvider should have been called
    assertThat(realEncryptionKeyProvider.getMethodCalls())
        .containsExactly("retrieveKeyForDecryption(someTopic, 6, aes)");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    String expectedShrinkedCacheEntryPayload = "{}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedShrinkedCacheEntryPayload);
  }

  @Test
  void shouldNotStoreDataAmbiguously_KnownEncryptionKeyAttributeName() {
    // given: an empty cache
    // when: retrieving the key for decryption (which has no expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3, "aes");
    // when: retrieving the key for encryption (which has an expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // when: retrieving the key for decryption (which has no expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3, "aes");
    // then: only one entry should exist (that one with an expiration time)
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:45Z\",\"k\":\"someSecret\"},"
        + "{\"v\":3,\"n\":\"aes\",\"k\":\"someSecret3\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldNotStoreDataAmbiguously_UnknownEncryptionKeyAttributeName() {
    // given: an empty cache
    // when: retrieving the key for decryption (which has no expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3);
    // when: retrieving the key for encryption (which has an expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // when: retrieving the key for decryption (which has no expiration time)
    cachedEncryptionKeyProvider.retrieveKeyForDecryption(TOPIC, 3);
    // then: only one entry should exist (that one with an expiration time)
    String expectedCacheEntryPayload = "{\"v\":2,\"t\":["
        + "{\"t\":\"someTopic\",\"e\":["
        + "{\"v\":3,\"n\":\"aes\",\"x\":\"2023-08-01T20:45Z\",\"k\":\"someSecret\"},"
        + "{\"v\":3,\"k\":\"someSecret2\"}"
        + "]}"
        + "]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }
}