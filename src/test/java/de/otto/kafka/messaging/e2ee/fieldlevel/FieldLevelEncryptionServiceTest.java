package de.otto.kafka.messaging.e2ee.fieldlevel;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import de.otto.kafka.messaging.e2ee.InitializationVectorFactory;
import de.otto.kafka.messaging.e2ee.helper.DummyEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.helper.DummyInitializationVectorFactory;
import org.junit.jupiter.api.Test;

class FieldLevelEncryptionServiceTest {

  @Test
  void shouldEncryptStringWithDeterministicIv() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 4711);
    InitializationVectorFactory initializationVectorFactory = new DummyInitializationVectorFactory(
        "JPsMcIDBGACHUveT");
    EncryptionService encryptionService = new EncryptionService(keyProvider,
        initializationVectorFactory);
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        encryptionService);

    // given: a string to encode and topic
    String plainText = "Hello World!";
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelEncryptionService.encryptFieldValueToString(topic, plainText);

    // then: returned string should be encrypted
    assertThat(result).isEqualTo(
        "encAesV1.4711.JPsMcIDBGACHUveT.Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==");
  }

  @Test
  void shouldEncryptStringWithRandomIv() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 475);
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        keyProvider);

    // given: a string to encode and topic
    String plainText = "Hello World!";
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelEncryptionService.encryptFieldValueToString(topic, plainText);

    // then: returned string should be encrypted
    assertThat(result).startsWith("encAesV1.475.");
  }

  @Test
  void shouldReturnNullWhenPlainTextIsNull() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        keyProvider);

    // given: a null string and topic
    String plainText = null;
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelEncryptionService.encryptFieldValueToString(topic, plainText);

    // then: returned string should be null
    assertThat(result).isNull();
  }

  @Test
  void shouldNotEncryptWhenNotNeeded() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(null);
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        keyProvider);

    // given: a null string and topic
    String plainText = "Hello World!";
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelEncryptionService.encryptFieldValueToString(topic, plainText);

    // then: returned string should be null
    assertThat(result).isEqualTo(plainText);
  }
}