package de.otto.messaging.kafka.e2ee;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider.KeyVersion;
import de.otto.messaging.kafka.e2ee.helper.DummyEncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.vault.VaultRuntimeException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class DecryptionServiceTest {

  @Test
  void shouldDecryptPayload() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    DecryptionService decryptionService = new DecryptionService(keyProvider);

    int keyVersion = 3;
    String ivBase64 = "2rW2tDnRdwRg87Ta";
    byte[] encryptedPayloadByteArray = Base64.getDecoder()
        .decode("6ttHpHYw7eYQ1OnvrhZAFi0PPsUGl9NR18hXFQ==");

    AesEncryptedPayload encryptedPayload = new AesEncryptedPayload(encryptedPayloadByteArray,
        ivBase64,
        keyVersion);
    String result = decryptionService.decryptToString("someTopic", encryptedPayload);
    assertThat(result).isEqualTo("Hello World!");
  }

  @Test
  void shouldDecryptPayloadWithGivenEncryptionKeyAttributeName() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        new KeyVersion(3, "aes", "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY="));
    DecryptionService decryptionService = new DecryptionService(keyProvider);

    int keyVersion = 3;
    String encryptionKeyAttributeName = "aes";
    String ivBase64 = "2rW2tDnRdwRg87Ta";
    byte[] encryptedPayloadByteArray = Base64.getDecoder()
        .decode("6ttHpHYw7eYQ1OnvrhZAFi0PPsUGl9NR18hXFQ==");

    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(
        encryptedPayloadByteArray,
        ivBase64, keyVersion, encryptionKeyAttributeName);

    String result = decryptionService.decryptToString("someTopic", encryptedPayload);
    assertThat(result).isEqualTo("Hello World!");
  }

  @Test
  void shouldThrowExceptionWhenDecryptPayloadWithGivenInvalidEncryptionKeyAttributeName() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        new KeyVersion(3, "aes", "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY="));
    DecryptionService decryptionService = new DecryptionService(keyProvider);

    int keyVersion = 3;
    String encryptionKeyAttributeName = "invalid_aes";
    String ivBase64 = "2rW2tDnRdwRg87Ta";
    byte[] encryptedPayloadByteArray = Base64.getDecoder()
        .decode("6ttHpHYw7eYQ1OnvrhZAFi0PPsUGl9NR18hXFQ==");

    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(
        encryptedPayloadByteArray, ivBase64, keyVersion, encryptionKeyAttributeName);

    assertThrows(VaultRuntimeException.class,
        () -> decryptionService.decryptToString("someTopic", encryptedPayload));
  }

  @Test
  void shouldNotDecryptPayloadWhenItIsNotEncrypted() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    DecryptionService decryptionService = new DecryptionService(keyProvider);

    byte[] plaintextByteArray = "Hello World!".getBytes(StandardCharsets.UTF_8);

    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofUnencryptedPayload(
        plaintextByteArray);
    String result = decryptionService.decryptToString("someTopic", encryptedPayload);
    assertThat(result).isEqualTo("Hello World!");
  }
}