package de.otto.messaging.kafka.e2ee;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.messaging.kafka.e2ee.fieldlevel.FieldLevelDecryptionService;
import de.otto.messaging.kafka.e2ee.fieldlevel.FieldLevelEncryptionService;
import de.otto.messaging.kafka.e2ee.helper.DummyEncryptionKeyProvider;
import org.junit.jupiter.api.Test;

public class PayloadEncryptionTest {

  @Test
  void shouldEncryptAndDecryptMessage() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    EncryptionService encryptionService = new EncryptionService(keyProvider);
    DecryptionService decryptionService = new DecryptionService(keyProvider);

    // encrypt message
    AesEncryptedPayload aesEncryptedPayload = encryptionService.encryptPayloadWithAes("someTopic",
        "Hello World!");
    assertThat(aesEncryptedPayload).isNotNull();

    // decrypt message
    String result = decryptionService.decryptToString("someTopic", aesEncryptedPayload);
    assertThat(result).isEqualTo("Hello World!");
  }

  @Test
  void shouldEncryptAndDecryptFieldValue() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 9);
    EncryptionService encryptionService = new EncryptionService(keyProvider);
    DecryptionService decryptionService = new DecryptionService(keyProvider);
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        encryptionService);
    FieldLevelDecryptionService fieldLevelDecryptionService = new FieldLevelDecryptionService(
        decryptionService);

    // encrypt message
    String encryptFieldValue = fieldLevelEncryptionService.encryptFieldValueToString("someTopic",
        "Hello World!");
    assertThat(encryptFieldValue).isNotNull();

    // decrypt message
    String result = fieldLevelDecryptionService.decryptFieldValue("someTopic", encryptFieldValue);
    assertThat(result).isEqualTo("Hello World!");
  }
}
