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
  void shouldWriteAndReadKeyVersion() {
    // given: a encrypted payload
    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED, IV_TEXT,
        43);
    // when: ciphers is mapped
    String headerText = KafkaEncryptionHelper.mapToCipherHeaderValueText(encryptedPayload);
    // then: header should be as expected
    assertThat(headerText).isEqualTo(
        "[{\"encryption_key\":{\"cipherVersion\":43,\"cipherVersionString\":null,\"cipherName\":\"encryption_key\"}}]");
    // when: ciphers is mapped back to
    int keyVersion = KafkaEncryptionHelper.extractKeyVersion(headerText);
    // then: key version should be correct
    assertThat(keyVersion).isEqualTo(43);
  }

  @Test
  void shouldWriteAndReadKeyVersionWithEncryptionKeyAttributeName() {
    // given: a encrypted payload
    AesEncryptedPayload encryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED, IV_TEXT,
        43, "aes");
    // when: ciphers is mapped
    String headerText = KafkaEncryptionHelper.mapToCipherHeaderValueText(encryptedPayload);
    // then: header should be as expected
    assertThat(headerText).isEqualTo(
        "[{\"aes\":{\"cipherVersion\":43,\"cipherVersionString\":null,\"cipherName\":\"aes\"}}]");
    // when: ciphers is mapped back to
    int keyVersion = KafkaEncryptionHelper.extractKeyVersion(headerText);
    String encryptionKeyAttributeName = KafkaEncryptionHelper.extractEncryptionKeyAttributeName(headerText);
    // then: key version should be correct
    assertThat(keyVersion).isEqualTo(43);
    assertThat(encryptionKeyAttributeName).isEqualTo("aes");
  }

  @Test
  void shouldWriteAndReaCipherSpec() {
    // given: a Cipher Spec
    EncryptionCipherSpec encryptionCipherSpec = EncryptionCipherSpec.builder()
        .keyVersion(43)
        .cipherName("aes")
        .build();
    // when: ciphers is mapped
    String headerText = KafkaEncryptionHelper.mapToCipherHeaderValueText(encryptionCipherSpec);
    // then: header should be as expected
    assertThat(headerText).isEqualTo(
        "[{\"aes\":{\"cipherVersion\":43,\"cipherVersionString\":null,\"cipherName\":\"aes\"}}]");
    // when: ciphers is mapped back to
    EncryptionCipherSpec result = KafkaEncryptionHelper.extractCipherSpec(headerText);
    // then: cipher name and key version should be correct
    assertThat(result.cipherName()).isEqualTo("aes");
    assertThat(result.keyVersion()).isEqualTo(43);
  }

  @Test
  void shouldExtractKeyVersionWhenCipherVersionIsAtTheEndOfJsonPayload() {
    // given: a valid header text
    String headerText = "[{\"encryption_key\":{\"cipherVersionString\":null,\"cipherName\":\"encryption_key\",\"cipherVersion\":43}}]";
    // when: ciphers is mapped back to
    int keyVersion = KafkaEncryptionHelper.extractKeyVersion(headerText);
    // then: key version should be correct
    assertThat(keyVersion).isEqualTo(43);
  }

  @Test
  void shouldExtractKeyVersionWhenCipherNameIsNotTheDefaultOne() {
    // given: a valid header text
    String headerText = "[{\"aes\":{\"cipherVersion\":43,\"cipherVersionString\":null,\"cipherName\":\"aes\"}}]";
    // when: ciphers is mapped back to
    int keyVersion = KafkaEncryptionHelper.extractKeyVersion(headerText);
    // then: key version should be correct
    assertThat(keyVersion).isEqualTo(43);
  }

  @Test
  void shouldExtractEncryptionKeyName() {
    // given: a valid header text
    String headerText = "[{\"aes\":{\"cipherVersion\":43}}]";
    // when: ciphers is mapped back to
    String encryptionKeyAttributeNamen  = KafkaEncryptionHelper.extractEncryptionKeyAttributeName(headerText);
    // then: encryption key name should be correct
    assertThat(encryptionKeyAttributeNamen).isEqualTo("aes");
  }

  @Test
  void shouldWriteAndReadAesEncryptionPayload() {
    // given: a valid AesEncryptedPayload (with default cipher name)
    AesEncryptedPayload aesEncryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43);
    // when: headers are created
    byte[] encryptedBytes = aesEncryptedPayload.encryptedPayload();
    byte[] ivHeaderValue = KafkaEncryptionHelper.mapToIvHeaderValue(aesEncryptedPayload);
    byte[] ciphersHeaderValue = KafkaEncryptionHelper.mapToCipherHeaderValue(aesEncryptedPayload);
    // when: converted back to AesEncryptedPayload
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafka(
        encryptedBytes, ivHeaderValue, ciphersHeaderValue);
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
  }

  @Test
  void shouldCreateAesEncryptedPayloadForKey() {
    // given: some kafka headers
    AesEncryptedPayload orgAesEncryptedPayload = AesEncryptedPayload.ofEncryptedPayload(ENCRYPTED,
        IV_TEXT, 43);
    Map<String, ?> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForKey(
        orgAesEncryptedPayload);
    // when: method is called
    AesEncryptedPayload result = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForKey(
        orgAesEncryptedPayload.encryptedPayload(), kafkaHeaders);
    // then: result should be valid
    assertThat(result.isEncrypted()).isTrue();
    assertThat(result.encryptedPayload()).isEqualTo(ENCRYPTED);
    assertThat(result.initializationVectorBase64()).isEqualTo(IV_TEXT);
    assertThat(result.keyVersion()).isEqualTo(43);
  }
}