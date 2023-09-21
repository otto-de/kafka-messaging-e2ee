package de.otto.messaging.kafka.e2ee.fieldlevel;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.EncryptionService;
import de.otto.messaging.kafka.e2ee.InitializationVectorFactory;
import de.otto.messaging.kafka.e2ee.helper.DummyEncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.helper.DummyInitializationVectorFactory;
import org.junit.jupiter.api.Test;

class SingleTopicFieldLevelEncryptionServiceTest {

  @Test
  void shouldEncryptToString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    InitializationVectorFactory initializationVectorFactory = new DummyInitializationVectorFactory(
        "JPsMcIDBGACHUveT");
    EncryptionService encryptionService = new EncryptionService(keyProvider,
        initializationVectorFactory);
    String topic = "someTopic";
    SingleTopicFieldLevelEncryptionService singleTopicFieldLevelEncryptionService = new SingleTopicFieldLevelEncryptionService(
        encryptionService, topic);

    // given: a string to encode and topic
    String plainText = "Hello World!";

    // when: method is called
    String result = singleTopicFieldLevelEncryptionService.encryptFieldValueToString(plainText);

    // then: returned string should be encrypted
    assertThat(result).isEqualTo(
        "encAesV1.3.JPsMcIDBGACHUveT.Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==");
  }

  @Test
  void shouldEncryptToEncryptedString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelEncryptionService singleTopicFieldLevelEncryptionService = new SingleTopicFieldLevelEncryptionService(
        keyProvider, topic);

    // given: a string to encode and topic
    String plainText = "Hello World!";

    // when: method is called
    EncryptedString result = singleTopicFieldLevelEncryptionService.encryptToEncryptedString(
        plainText);

    // then: returned string should be encrypted
    assertThat(result).isNotNull();
    assertThat(result.value()).startsWith("encAesV1.3.");
  }

  @Test
  void shouldEncryptToNullString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelEncryptionService singleTopicFieldLevelEncryptionService = new SingleTopicFieldLevelEncryptionService(
        keyProvider, topic);

    // when: method is called
    String result = singleTopicFieldLevelEncryptionService.encryptFieldValueToString(null);

    // then: returned string should be null
    assertThat(result).isNull();
  }

  @Test
  void shouldEncryptToNullEncryptedString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelEncryptionService singleTopicFieldLevelEncryptionService = new SingleTopicFieldLevelEncryptionService(
        keyProvider, topic);

    // when: method is called
    EncryptedString result = singleTopicFieldLevelEncryptionService.encryptToEncryptedString(null);

    // then: returned string should be null
    assertThat(result).isNull();
  }

}