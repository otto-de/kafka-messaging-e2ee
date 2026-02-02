package de.otto.kafka.messaging.e2ee;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KafkaEncryptionHelperTest {

  private static final byte[] ENCRYPTED = new byte[]{23, 56, 74};
  private static final byte[] PLAINTEXT = "someText".getBytes(StandardCharsets.UTF_8);
  private static final String IV_TEXT = "JPsMcIDBGACHUveT";

  @Test
  void shouldWriteAndReadCloudEventKeyVersion() {
    // given: a encrypted payload
    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43);
    // when: ciphers is mapped
    String headerText = KafkaEncryptionHelper.mapToCipherVersionHeaderText(encryptedPayload);
    // then: header should be as expected
    assertThat(headerText).isEqualTo("43");
    // when: ciphers is mapped back to
    int keyVersion = KafkaEncryptionHelper.extractCipherVersion(headerText);
    // then: key version should be correct
    assertThat(keyVersion).isEqualTo(43);
  }

  @Test
  void shouldWriteAndReadCloudEventCipherName() {
    // given: a encrypted payload
    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43, "aes");
    // when: ciphers is mapped
    String headerText = KafkaEncryptionHelper.mapToCipherNameHeaderText(encryptedPayload);
    // then: header should be as expected
    assertThat(headerText).isEqualTo("aes");
  }

  @Test
  void shouldWriteAndReadCloudEventAesEncryptionPayload() {
    // given: a valid AesEncryptedPayload (with default cipher name)
    AesEncryptedPayload aesEncryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43);
    // when: headers are created
    byte[] encryptedBytes = aesEncryptedPayload.encryptedPayload();
    byte[] kafkaCeHeaderInitializationVector = KafkaEncryptionHelper.mapToIvHeaderValue(
        aesEncryptedPayload);
    byte[] kafkaCeHeaderCipherVersion = KafkaEncryptionHelper.mapToCipherVersionHeaderValue(
        aesEncryptedPayload);
    byte[] kafkaCeHeaderCipherName = KafkaEncryptionHelper.mapToCipherNameHeaderValue(
        aesEncryptedPayload);

    // when: converted back to AesEncryptedPayload
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafka(
        encryptedBytes, kafkaCeHeaderInitializationVector, kafkaCeHeaderCipherVersion,
        kafkaCeHeaderCipherName);
    // then: result should be as expected
    assertThat(result.isEncrypted()).isTrue();
    assertThat(result.encryptedPayload()).isEqualTo(ENCRYPTED);
    assertThat(result.initializationVectorBase64()).isEqualTo(IV_TEXT);
    assertThat(result.keyVersion()).isEqualTo(43);
  }

  @Test
  void shouldCreateAesEncryptedPayloadForPlaintextPayload() {
    // given: a plain text AesEncryptedPayload
    AesEncryptedPayload orgAesEncryptedPayload = AesEncryptedPayload.ofUnencryptedPayload(
        PLAINTEXT);
    // when: methods is called
    Map<String, ?> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForValue(
        orgAesEncryptedPayload);
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        orgAesEncryptedPayload.encryptedPayload(), kafkaHeaders);
    // then: result should be valid
    assertThat(result.isEncrypted()).isFalse();
    assertThat(result.encryptedPayload()).isEqualTo(PLAINTEXT);
    // then: kafka headers should be empty
    assertThat(kafkaHeaders.keySet()).isEmpty();
  }

  @Test
  void shouldCreateAesEncryptedPayloadForValue() {
    // given: some kafka headers
    AesEncryptedPayload orgAesEncryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43);
    Map<String, ?> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForValue(
        orgAesEncryptedPayload);
    // when: method is called
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        orgAesEncryptedPayload.encryptedPayload(), kafkaHeaders);
    // then: result should be valid
    assertThat(result.isEncrypted()).isTrue();
    assertThat(result.encryptedPayload()).isEqualTo(ENCRYPTED);
    assertThat(result.initializationVectorBase64()).isEqualTo(IV_TEXT);
    assertThat(result.keyVersion()).isEqualTo(43);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("encryption_key");
    // then: all expected kafka headers should have been set
    assertThat(kafkaHeaders.keySet())
        .containsExactlyInAnyOrder("ce_e2eeiv", "ce_e2eekeyversion", "ce_e2eekeyname");
  }

  @Test
  void shouldCreateAesEncryptedPayloadForValueWithGivenEncryptionKeyAttribute() {
    // given: some kafka headers
    AesEncryptedPayload orgAesEncryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43, "aes");
    Map<String, ?> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForValue(
        orgAesEncryptedPayload);
    // when: method is called
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        orgAesEncryptedPayload.encryptedPayload(), kafkaHeaders);
    // then: result should be valid
    assertThat(result.isEncrypted()).isTrue();
    assertThat(result.encryptedPayload()).isEqualTo(ENCRYPTED);
    assertThat(result.initializationVectorBase64()).isEqualTo(IV_TEXT);
    assertThat(result.keyVersion()).isEqualTo(43);
    assertThat(result.encryptionKeyAttributeName()).isEqualTo("aes");
  }
}