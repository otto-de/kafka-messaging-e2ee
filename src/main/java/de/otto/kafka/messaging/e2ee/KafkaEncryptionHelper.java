package de.otto.kafka.messaging.e2ee;

import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Helper class for dealing with the kafka library without using the kafka library in this project.
 */
public interface KafkaEncryptionHelper {

  /**
   * Name of Kafka CloudEvent Header for the initialization vector for the payload (or value)
   */
  String KAFKA_CE_HEADER_IV_VALUE = "ce_e2eeiv";
  /**
   * Name of Kafka CloudEvent Header for the cipher version for the payload (or value)
   */
  String KAFKA_CE_HEADER_CIPHER_VERSION_VALUE = "ce_e2eekeyversion";
  /**
   * Name of Kafka CloudEvent Header for the cipher name for the payload (or value)
   */
  String KAFKA_CE_HEADER_CIPHER_NAME_VALUE = "ce_e2eekeyname";

  /**
   * Create an AesEncryptedPayload using the kafka header values.
   *
   * @param encryptedPayload                  the encrypted payload
   * @param kafkaCeHeaderInitializationVector value of cloud event kafka header of "initialization
   *                                          vector"
   * @param kafkaCeHeaderCipherVersion        value of cloud event kafka header of "cipher version"
   * @param kafkaCeHeaderCipherName           value of cloud event kafka header of "cipher name"
   * @return a AesEncryptedPayload instance with the given values
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      String kafkaCeHeaderInitializationVector,
      String kafkaCeHeaderCipherVersion,
      String kafkaCeHeaderCipherName) {
    if (kafkaCeHeaderInitializationVector != null
        && kafkaCeHeaderCipherVersion != null
        && kafkaCeHeaderCipherName != null) {
      int cipherVersion = extractCipherVersion(kafkaCeHeaderCipherVersion);
      return AesEncryptedPayload.ofEncryptedPayload(encryptedPayload,
          kafkaCeHeaderInitializationVector, cipherVersion, kafkaCeHeaderCipherName);
    }

    return AesEncryptedPayload.ofUnencryptedPayload(encryptedPayload);
  }

  /**
   * Create an AesEncryptedPayload using the kafka header values.
   *
   * @param encryptedPayload                  the encrypted payload
   * @param kafkaCeHeaderInitializationVector value of kafka header of "ce_e2eeiv"
   * @param kafkaCeHeaderCipherName           value of kafka header of "ce_e2eekeyname"
   * @param kafkaCeHeaderCipherVersion        value of kafka header of "ce_e2eekeyversion"
   * @return a AesEncryptedPayload instance with the given values
   * @see #aesEncryptedPayloadOfKafka(byte[], byte[], byte[], byte[])
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      byte[] kafkaCeHeaderInitializationVector,
      byte[] kafkaCeHeaderCipherVersion,
      byte[] kafkaCeHeaderCipherName) {
    if (kafkaCeHeaderInitializationVector != null
        && kafkaCeHeaderCipherVersion != null
        && kafkaCeHeaderCipherName != null) {
      int cipherVersion = extractCipherVersion(kafkaCeHeaderCipherVersion);
      return AesEncryptedPayload.ofEncryptedPayload(encryptedPayload,
          byteArrayToUtf8String(kafkaCeHeaderInitializationVector),
          cipherVersion,
          byteArrayToUtf8String(kafkaCeHeaderCipherName));
    }

    return AesEncryptedPayload.ofUnencryptedPayload(encryptedPayload);
  }

  /**
   * Creates AesEncryptedPayload for a potentially encrypted event.
   *
   * @param encryptedPayload the encrypted payload
   * @param kafkaHeaders     all kafka headers including "ce_e2eeiv", "ce_e2eekeyversion" and
   *                         "ce_e2eekeyname"
   * @return a AesEncryptedPayload instance with the given values to represent an encrypted kafka
   * value a.k.a. payload
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafkaForValue(
      byte[] encryptedPayload,
      Map<String, ?> kafkaHeaders) {
    // read CloudEvent kafka headers
    String kafkaCeHeaderInitializationVector = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_IV_VALUE);
    String kafkaCeHeaderCipherVersion = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_CIPHER_VERSION_VALUE);
    String kafkaCeHeaderCipherName = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_CIPHER_NAME_VALUE);

    return aesEncryptedPayloadOfKafka(encryptedPayload, kafkaCeHeaderInitializationVector,
        kafkaCeHeaderCipherVersion, kafkaCeHeaderCipherName);
  }

  /**
   * find and extract the value of the specified kafka header
   *
   * @param kafkaHeaders all kafka headers
   * @param headerName   name of the kafka header that are of interest
   * @return the found header or <code>null</code>
   */
  static String extractKafkaHeaderValueText(Map<String, ?> kafkaHeaders, String headerName) {
    Object value = kafkaHeaders.get(headerName);
    if (value == null) {
      return null;
    }
    if (value instanceof byte[] bytes) {
      return byteArrayToUtf8String(bytes);
    }
    if (value instanceof String) {
      return (String) value;
    }

    return value.toString();
  }

  /**
   * Extracts the cipher version from the given kafka header value.
   *
   * @param cipherVersionCeHeaderValue the raw CloudEvent kafka header value of the cipher version.
   * @return the key version used to encrypt the payload
   */
  static int extractCipherVersion(byte[] cipherVersionCeHeaderValue) {
    if (cipherVersionCeHeaderValue == null) {
      // not encrypted
      return 0;
    }
    return Integer.parseInt(byteArrayToUtf8String(cipherVersionCeHeaderValue));
  }

  /**
   * Extracts the cipher version from the given kafka header value.
   *
   * @param cipherVersionCeHeaderText the CloudEvent kafka header value of the cipher version.
   * @return the key version used to encrypt the payload
   */
  static int extractCipherVersion(String cipherVersionCeHeaderText) {
    if (cipherVersionCeHeaderText == null) {
      // not encrypted
      return 0;
    }
    return Integer.parseInt(cipherVersionCeHeaderText);
  }

  /**
   * Converts a byte-array to a String assuming UTF-8 encoding.
   *
   * @param kafkaHeaderValue a Kafka header value as raw byte array or <code>null</code>
   * @return the header value as String can be <code>null</code>
   */
  static String byteArrayToUtf8String(byte[] kafkaHeaderValue) {
    if (kafkaHeaderValue == null) {
      return null;
    }
    return new String(kafkaHeaderValue, StandardCharsets.UTF_8);
  }

  /**
   * Builds a map containing all required encryption headers for an encrypted event.
   *
   * @param encryptedPayload a AesEncryptedPayload object for a Kafka value a.k.a. payload
   * @return the kafka headers needed for given AesEncryptedPayload
   * @see #mapToKafkaHeadersForValue(AesEncryptedPayload)
   */
  static Map<String, byte[]> mapToKafkaHeadersForValue(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return Map.of(
          KAFKA_CE_HEADER_IV_VALUE, mapToIvHeaderValue(encryptedPayload),
          KAFKA_CE_HEADER_CIPHER_VERSION_VALUE, mapToCipherVersionHeaderValue(encryptedPayload),
          KAFKA_CE_HEADER_CIPHER_NAME_VALUE, mapToCipherNameHeaderValue(encryptedPayload)
      );
    }
    return Map.of();
  }

  /**
   * Extracts the initialization vector as byte-array to be used in
   * {@code KAFKA_CE_HEADER_IV_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the initialization vector. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #mapToIvHeaderValueText(AesEncryptedPayload)
   */
  static byte[] mapToIvHeaderValue(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return mapToIvHeaderValueText(encryptedPayload)
          .getBytes(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException(
        "Cannot call 'mapToIvHeaderValue' for unencrypted payloads.");
  }

  /**
   * Extracts the initialization vector in base64 encoding to be used in
   * {@code KAFKA_CE_HEADER_IV_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the initialization vector. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   */
  static String mapToIvHeaderValueText(AesEncryptedPayload encryptedPayload) {
    return encryptedPayload.initializationVectorBase64();
  }

  /**
   * Build the cipher version header value as byte-array to be used in
   * {@code KAFKA_CE_HEADER_CIPHER_VERSION_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the cipher version. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #mapToCipherVersionHeaderText(AesEncryptedPayload)
   */
  static byte[] mapToCipherVersionHeaderValue(AesEncryptedPayload encryptedPayload) {
    return mapToCipherVersionHeaderText(encryptedPayload)
        .getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Build the cipher version header value as String to be used in
   * {@code KAFKA_CE_HEADER_CIPHER_VERSION_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the cipher version. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   */
  static String mapToCipherVersionHeaderText(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return Integer.toString(encryptedPayload.keyVersion());
    }
    throw new IllegalArgumentException(
        "Cannot call 'mapToCipherVersionHeaderText' for unencrypted payloads.");
  }

  /**
   * Build the cipher name header value as byte-array to be used in
   * {@code KAFKA_CE_HEADER_CIPHER_NAME_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the cipher name. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #mapToCipherNameHeaderText(AesEncryptedPayload)
   */
  static byte[] mapToCipherNameHeaderValue(AesEncryptedPayload encryptedPayload) {
    return mapToCipherNameHeaderText(encryptedPayload)
        .getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Build the cipher name header value as String to be used in
   * {@code KAFKA_CE_HEADER_CIPHER_NAME_VALUE}
   *
   * @param encryptedPayload the payload
   * @return the value for the cipher name. Note: you should check
   * {@link AesEncryptedPayload#isEncrypted()} before calling this method.
   */
  static String mapToCipherNameHeaderText(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      String encryptionKeyName = encryptedPayload.encryptionKeyAttributeName();
      if (encryptionKeyName == null) {
        encryptionKeyName = VaultEncryptionKeyProviderConfig.DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;
      }
      return encryptionKeyName;
    }
    throw new IllegalArgumentException(
        "Cannot call 'mapToCipherNameHeaderText' for unencrypted payloads.");
  }
}
