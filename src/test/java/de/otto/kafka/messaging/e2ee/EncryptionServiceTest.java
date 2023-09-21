package de.otto.kafka.messaging.e2ee;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider.KeyVersion;
import de.otto.kafka.messaging.e2ee.helper.DummyEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.helper.DummyInitializationVectorFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

  @Test
  void shouldEncryptMessage() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY=", "encryption_key", 3);
    InitializationVectorFactory initializationVectorFactory = new DummyInitializationVectorFactory(
        "JPsMcIDBGACHUveT");
    EncryptionService encryptionService = new EncryptionService(keyProvider,
        initializationVectorFactory);

    AesEncryptedPayload result = encryptionService.encryptPayloadWithAes("someTopic",
        "Hello World!");
    assertThat(result).isNotNull();
    assertThat(result.keyVersion()).isEqualTo(3);
    assertThat(result.initializationVectorBase64()).isEqualTo("JPsMcIDBGACHUveT");
    assertThat(result.encryptedPayload()).isNotEmpty();
    assertThat(Base64.getEncoder().encodeToString(result.encryptedPayload()))
        .isEqualTo("Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==");
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("encryption_key");
  }

  @Test
  void shouldEncryptMessageAndSetEncryptionKeyAttributeName() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(
        new KeyVersion(3, "aes", "gZvWT1IN0mM5sK3sK0V2Wfzo9Jmk4tUPt7gxRsuN3LY="));
    InitializationVectorFactory initializationVectorFactory = new DummyInitializationVectorFactory(
        "JPsMcIDBGACHUveT");
    EncryptionService encryptionService = new EncryptionService(keyProvider,
        initializationVectorFactory);

    AesEncryptedPayload result = encryptionService.encryptPayloadWithAes("someTopic",
        "Hello World!");
    assertThat(result).isNotNull();
    assertThat(result.keyVersion()).isEqualTo(3);
    assertThat(result.initializationVectorBase64()).isEqualTo("JPsMcIDBGACHUveT");
    assertThat(result.encryptedPayload()).isNotEmpty();
    assertThat(Base64.getEncoder().encodeToString(result.encryptedPayload()))
        .isEqualTo("Y89d2GZR/Dpg8BXlzAkJB9SqazBGwXJ+4PLoCQ==");
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
  }

  @Test
  void shouldNotEncryptMessageWhenNoEncryptionKeyIsProvided() {
    EncryptionKeyProvider keyProvider = new DummyEncryptionKeyProvider(null);
    EncryptionService encryptionService = new EncryptionService(keyProvider);

    AesEncryptedPayload result = encryptionService.encryptPayloadWithAes("someTopic",
        "Hello World!");
    assertThat(result).isNotNull();
    assertThat(result.isEncrypted()).isFalse();
    assertThat(result.encryptedPayload()).isNotEmpty();
    assertThat(Base64.getEncoder().encodeToString(result.encryptedPayload()))
        .isEqualTo("SGVsbG8gV29ybGQh");
    assertThat(Base64.getEncoder().encodeToString(result.encryptedPayload()))
        .isEqualTo(
            Base64.getEncoder().encodeToString("Hello World!".getBytes(StandardCharsets.UTF_8)));
  }
}