package de.otto.kafka.messaging.e2ee.fieldlevel;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.helper.DummyEncryptionKeyProvider;
import org.junit.jupiter.api.Test;

class SingleTopicFieldLevelDecryptionServiceTest {

  @Test
  void shouldDecryptValueOfTypeString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelDecryptionService singleTopicFieldLevelDecryptionService
        = new SingleTopicFieldLevelDecryptionService(keyProvider, topic);

    // given: a encrypted string a topic
    String encryptedString = "encAesV1.3.JPsMcIDBGACHUveT.Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==";

    // when: method is called
    String result = singleTopicFieldLevelDecryptionService.decrypt(encryptedString);

    // then: returned string should be as the plain text
    String expectedValue = "Hello World!";
    assertThat(result).isEqualTo(expectedValue);
  }

  @Test
  void shouldDecryptValueOfTypeEncryptedString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelDecryptionService singleTopicFieldLevelDecryptionService
        = new SingleTopicFieldLevelDecryptionService(keyProvider, topic);

    // given: a encrypted string a topic
    EncryptedString encryptedString = EncryptedString.of(
        "encAesV1.3.JPsMcIDBGACHUveT.Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==");

    // when: method is called
    String result = singleTopicFieldLevelDecryptionService.decrypt(encryptedString);

    // then: returned string should be as the plain text
    String expectedValue = "Hello World!";
    assertThat(result).isEqualTo(expectedValue);
  }

  @Test
  void shouldReturnNullForNullOfTypeString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelDecryptionService singleTopicFieldLevelDecryptionService
        = new SingleTopicFieldLevelDecryptionService(keyProvider, topic);

    // when: method is called
    String result = singleTopicFieldLevelDecryptionService.decrypt((String) null);

    // then: returned string should be null
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullForNullOfTypeEncryptedString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    String topic = "someTopic";
    SingleTopicFieldLevelDecryptionService singleTopicFieldLevelDecryptionService
        = new SingleTopicFieldLevelDecryptionService(keyProvider, topic);

    // when: method is called
    String result = singleTopicFieldLevelDecryptionService.decrypt((EncryptedString) null);

    // then: returned string should be null
    assertThat(result).isNull();
  }

}