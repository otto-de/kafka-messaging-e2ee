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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T20:45Z kn:aes ek:someSecret
        """;
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
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:2 ex:2023-08-01T17:00Z kn:aes ek:someSecret-v2
        """;
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
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentButIsExpiredAndUnchanged() {
    // given: a cache entry
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:2 ex:2023-08-01T17:05Z kn:aes ek:someOtherSecret
        v:3 ex:2023-08-01T17:00Z kn:aes ek:someSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T20:10Z kn:aes ek:someSecret
        v:2 ex:2023-08-01T17:05Z kn:aes ek:someOtherSecret
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentButIsExpiredAndWithNewVersion() {
    // given: a cache entry
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:2 ex:2023-08-01T17:05Z kn:aes ek:someOtherSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T20:10Z kn:aes ek:someSecret
        v:2 ex:2023-08-01T17:05Z kn:aes ek:someOtherSecret
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndIsExpiredButRealEncryptionProviderHasAnException() {
    // given: a cache entry
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:2 ex:2023-08-01T17:00Z kn:aes ek:someSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:2 ex:2023-08-01T20:10Z kn:aes ek:someSecret
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndNotExpiredButHasTwoEntries() {
    // given: a cache entry
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T17:00Z kn:aes ek:someSecret
        v:1 ex:2023-08-01T16:30Z kn:aes ek:someOtherSecret
        """;
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
    String cacheEntryPayload = """
        v1
        t:someOtherTopic
        v:5 kn:aes ek:someOtherSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ek:someSecret2
        """;
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
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:5 kn:aes ek:someOtherSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:6 ek:someSecret2
        v:5 kn:aes ek:someOtherSecret
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:6 ek:someOtherSecret
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 kn:aes ek:someSecret3
        """;
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
    String cacheEntryPayload = """
        v1
        t:someTopic
        v:6 kn:ssl ek:someOtherSecret
        """;
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
    String expectedCacheEntryPayload ="""
        v1
        t:someTopic
        v:6 kn:aes ek:someSecret3
        v:6 kn:ssl ek:someOtherSecret
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWithNameWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload ="""
        v1
        t:someTopic
        v:6 kn:aes ek:someOtherSecret
        """;
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
    sb.append("v1\n")
        .append("t:someOtherTopic\n");
    sb.append("{\"entries\":[");
    for (int version = 1; version < 12; version++) {
      String part = "v:9999 ex:2023-08-01T17:45Z kn:aes ek:someOtherSecret\n"
          .replace("9999", Integer.toString(version));
      sb.append(part);
    }
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T20:45Z kn:aes ek:someSecret
        v:3 kn:aes ek:someSecret3
        """;
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
    String expectedCacheEntryPayload = """
        v1
        t:someTopic
        v:3 ex:2023-08-01T20:45Z kn:aes ek:someSecret
        v:3 ek:someSecret2
        """;
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }
}