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
    cachedEncryptionKeyProvider = new CachedEncryptionKeyProvider(realEncryptionKeyProvider,
        cacheStorage, testClock,
        Duration.ofHours(3));
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenNoCacheEntryIsPresent() {
    // given: an empty cache
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:45Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: cacheStorage should have been called
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T20:45Z\"}]}";
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
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret-v2\",\"expireAt\":\"2023-08-01T17:00Z\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T16:55Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
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
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:05Z\"},{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T17:00Z\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:05Z\"},{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T20:10Z\"}]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentButIsExpiredAndWithNewVersion() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:05Z\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:05Z\"},{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T20:10Z\"}]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndIsExpiredButRealEncryptionProviderHasAnException() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T17:00Z\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T17:10Z");
    // given: realEncryptionKeyProvider throws an exception
    realEncryptionKeyProvider.setThrowException(true);
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
    assertThat(result.version()).isEqualTo(2);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
    assertThat(result.encodedKey()).isEqualTo("someSecret");
    // then: cacheStorage should have been called
    assertThat(cacheStorage.getMethodCalls())
        .containsExactly("retrieveEntry()", "storeEntry(..)");
    // then: cache should have the correct value
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":2,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T20:10Z\"}]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForEncryptionWhenCacheIsPresentAndNotExpiredButHasTwoEntries() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":1,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T16:30Z\"},{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret\",\"expireAt\":\"2023-08-01T17:00Z\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // given: current time
    testClock.setCurrentTime("2023-08-01T16:20Z");
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
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
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someOtherTopic\",\"version\":5,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\"}]}";
    cacheStorage.initEntry(cacheEntryPayload);
    // when: method is called
    KeyVersion result = cachedEncryptionKeyProvider.retrieveKeyForEncryption(TOPIC);
    // then: result should be valid
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":3,\"encodedKey\":\"someSecret2\"}]}";
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
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":5,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\"}]}";
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":5,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\"},{\"topic\":\"someTopic\",\"version\":6,\"encodedKey\":\"someSecret2\"}]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":6,\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:45Z\"}]}";
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":3,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret3\"}]}";
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
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":6,\"encryptionKeyAttributeName\":\"ssl\",\"encodedKey\":\"someOtherSecret\"}]}";
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
    String expectedCacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":6,\"encryptionKeyAttributeName\":\"ssl\",\"encodedKey\":\"someOtherSecret\"},{\"topic\":\"someTopic\",\"version\":6,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someSecret3\"}]}";
    assertThat(cacheStorage.retrieveEntry())
        .isEqualTo(expectedCacheEntryPayload);
  }

  @Test
  void shouldRetrieveKeyForDecryptionWithNameWhenCacheIsPresent() {
    // given: a cache entry
    String cacheEntryPayload = "{\"entries\":[{\"topic\":\"someTopic\",\"version\":6,\"encryptionKeyAttributeName\":\"aes\",\"encodedKey\":\"someOtherSecret\",\"expireAt\":\"2023-08-01T17:45Z\"}]}";
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
}