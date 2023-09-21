package de.otto.messaging.kafka.e2ee.fieldlevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.helper.DummyEncryptionKeyProvider;
import org.junit.jupiter.api.Test;

class FieldLevelDecryptionServiceTest {
  @Test
  void shouldDecryptString() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    FieldLevelDecryptionService fieldLevelDecryptionService = new FieldLevelDecryptionService(
        keyProvider);

    // given: a encrypted string a topic
    String encryptedString = "encAesV1.3.y1rGSsCWGnhEMXPr.wpMBv+YxFQqMb2k0cGHhZPSd9KZdHh9xxDOiQg==";
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelDecryptionService.decryptFieldValue(topic, encryptedString);

    // then: returned string should be null
    String expectedValue = "Hello World!";
    assertThat(result).isEqualTo(expectedValue);
  }

  @Test
  void shouldReturnInputStringWhenPrefixIsMissing() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    FieldLevelDecryptionService fieldLevelDecryptionService = new FieldLevelDecryptionService(
        keyProvider);

    // given: a encrypted string a topic
    String encryptedString = "NotEncryptedString";
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelDecryptionService.decryptFieldValue(topic, encryptedString);

    // then: returned string should be null
    String expectedValue = "NotEncryptedString";
    assertThat(result).isEqualTo(expectedValue);
  }

  @Test
  void shouldReturnNullWhenInputIsNull() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    FieldLevelDecryptionService fieldLevelDecryptionService = new FieldLevelDecryptionService(
        keyProvider);

    // given: a encrypted string a topic
    String encryptedString = null;
    String topic = "someTopic";

    // when: method is called
    String result = fieldLevelDecryptionService.decryptFieldValue(topic, encryptedString);

    // then: returned string should be null
    assertThat(result).isNull();
  }
}