package de.otto.kafka.messaging.e2ee;

import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonArray;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.WriterConfig;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public interface KafkaEncryptionHelper {

  /**
   * Name of Kafka Header for the initialization vector for the partition key
   */
  String KAFKA_HEADER_IV_KEY = "encryption/key/iv";
  /**
   * Name of Kafka Header for the initialization vector for the payload (or value)
   */
  String KAFKA_HEADER_IV_VALUE = "encryption/iv";
  /**
   * Name of Kafka CloudEvent Header for the initialization vector for the payload (or value)
   */
  String KAFKA_CE_HEADER_IV_VALUE = "ce_encryption.ref.iv";

  /**
   * Name of Kafka Header for the cipher metadata for the partition key
   */
  String KAFKA_HEADER_CIPHER_KEY = "encryption/key/ciphers";
  /**
   * Name of Kafka Header for the cipher metadata for the payload (or value)
   */
  String KAFKA_HEADER_CIPHER_VALUE = "encryption/ciphers";
  /**
   * Name of Kafka CloudEvent Header for the cipher version for the payload (or value)
   */
  String KAFKA_CE_HEADER_CIPHER_VERSION_VALUE = "ce_encryption.ref.cipher.version";
  /**
   * Name of Kafka CloudEvent Header for the cipher name for the payload (or value)
   */
  String KAFKA_CE_HEADER_CIPHER_NAME_VALUE = "ce_encryption.ref.cipher.name";

  static String headerNameIv(boolean isForKey) {
    if (isForKey) {
      return KafkaEncryptionHelper.KAFKA_HEADER_IV_KEY;
    } else {
      return KafkaEncryptionHelper.KAFKA_HEADER_IV_VALUE;
    }
  }

  static String headerNameCiphers(boolean isForKey) {
    if (isForKey) {
      return KafkaEncryptionHelper.KAFKA_HEADER_CIPHER_KEY;
    } else {
      return KafkaEncryptionHelper.KAFKA_HEADER_CIPHER_VALUE;
    }
  }

  /**
   * @param encryptedPayload                the encrypted payload
   * @param kafkaHeaderInitializationVector value of kafka header of "initialization vector"
   * @param kafkaHeaderCiphersText          value of kafka header of "ciphers"
   * @return a AesEncryptedPayload instance with the given values
   * @see #aesEncryptedPayloadOfKafka(byte[], String, String, String, String, String)
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      String kafkaHeaderInitializationVector,
      String kafkaHeaderCiphersText) {
    if (kafkaHeaderInitializationVector == null || kafkaHeaderCiphersText == null) {
      return AesEncryptedPayload.ofUnencryptedPayload(encryptedPayload);
    }

    EncryptionCipherSpec cipherSpec = extractCipherSpec(kafkaHeaderCiphersText);
    return AesEncryptedPayload.ofEncryptedPayload(encryptedPayload,
        kafkaHeaderInitializationVector, cipherSpec);
  }

  /**
   * @param encryptedPayload                  the encrypted payload
   * @param kafkaHeaderInitializationVector   value of deprecated kafka header of "initialization
   *                                          vector"
   * @param kafkaHeaderCiphersText            value of deprecated kafka header of "ciphers"
   * @param kafkaCeHeaderInitializationVector value of cloud event kafka header of "initialization
   *                                          vector"
   * @param kafkaCeHeaderCipherVersion        value of cloud event kafka header of "cipher version"
   * @param kafkaCeHeaderCipherName           value of cloud event kafka header of "cipher name"
   * @return a AesEncryptedPayload instance with the given values
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      String kafkaHeaderInitializationVector,
      String kafkaHeaderCiphersText,
      String kafkaCeHeaderInitializationVector,
      String kafkaCeHeaderCipherVersion,
      String kafkaCeHeaderCipherName) {
    if (kafkaCeHeaderInitializationVector != null
        && kafkaCeHeaderCipherVersion != null
        && kafkaCeHeaderCipherName != null) {
      // prefer new cloud event kafka headers for decryption
      int cipherVersion = extractCipherVersion(kafkaCeHeaderCipherVersion);
      return AesEncryptedPayload.ofEncryptedPayload(encryptedPayload,
          kafkaHeaderInitializationVector, cipherVersion, kafkaCeHeaderCipherName);
    }

    if (kafkaHeaderInitializationVector != null
        && kafkaHeaderCiphersText != null) {
      // use deprecated kafka headers for decryption
      EncryptionCipherSpec cipherSpec = extractCipherSpec(kafkaHeaderCiphersText);
      return AesEncryptedPayload.ofEncryptedPayload(encryptedPayload,
          kafkaHeaderInitializationVector, cipherSpec);
    }

    return AesEncryptedPayload.ofUnencryptedPayload(encryptedPayload);
  }

  /**
   * @param encryptedPayload                the encrypted payload
   * @param kafkaHeaderInitializationVector value of kafka header of "initialization vector"
   * @param kafkaHeaderCiphersText          value of kafka header of "ciphers"
   * @return a AesEncryptedPayload instance with the given values
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      byte[] kafkaHeaderInitializationVector,
      byte[] kafkaHeaderCiphersText) {
    return aesEncryptedPayloadOfKafka(encryptedPayload,
        byteArrayToUtf8String(kafkaHeaderInitializationVector),
        byteArrayToUtf8String(kafkaHeaderCiphersText));
  }

  /**
   * @param encryptedPayload                the encrypted payload
   * @param kafkaHeaderInitializationVector value of kafka header of "initialization vector"
   * @param kafkaHeaderCiphersText          value of kafka header of "ciphers"
   * @return a AesEncryptedPayload instance with the given values
   * @see #aesEncryptedPayloadOfKafka(byte[], byte[], byte[], byte[], byte[], byte[])
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafka(
      byte[] encryptedPayload,
      byte[] kafkaHeaderInitializationVector,
      byte[] kafkaHeaderCiphersText,
      byte[] kafkaCeHeaderInitializationVector,
      byte[] kafkaCeHeaderCipherVersion,
      byte[] kafkaCeHeaderCipherName) {
    return aesEncryptedPayloadOfKafka(encryptedPayload,
        byteArrayToUtf8String(kafkaHeaderInitializationVector),
        byteArrayToUtf8String(kafkaHeaderCiphersText),
        byteArrayToUtf8String(kafkaCeHeaderInitializationVector),
        byteArrayToUtf8String(kafkaCeHeaderCipherVersion),
        byteArrayToUtf8String(kafkaCeHeaderCipherName));
  }

  /**
   * @param encryptedPayload the encrypted payload
   * @param kafkaHeaders     all kafka headers including "initialization vector" and "ciphers"
   * @return a AesEncryptedPayload instance with the given values to represent an encrypted kafka
   * key
   * @see #aesEncryptedPayloadOfKafkaForValue(byte[], Map)
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafkaForKey(
      byte[] encryptedPayload,
      Map<String, ?> kafkaHeaders) {
    String kafkaHeaderInitializationVector = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameIv(true));
    String kafkaHeaderCiphersText = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameCiphers(true));
    return aesEncryptedPayloadOfKafka(encryptedPayload, kafkaHeaderInitializationVector,
        kafkaHeaderCiphersText);
  }

  /**
   * @param encryptedPayload the encrypted payload
   * @param kafkaHeaders     all kafka headers including "initialization vector" and "ciphers"
   * @return a AesEncryptedPayload instance with the given values to represent an encrypted kafka
   * value a.k.a. payload
   * @see #aesEncryptedPayloadOfKafkaForKey(byte[], Map)
   */
  static AesEncryptedPayload aesEncryptedPayloadOfKafkaForValue(
      byte[] encryptedPayload,
      Map<String, ?> kafkaHeaders) {
    // read deprecated kafka headers
    String kafkaHeaderInitializationVector = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameIv(false));
    String kafkaHeaderCiphersText = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameCiphers(false));

    // read CloudEvent kafka headers
    String kafkaCeHeaderInitializationVector = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_IV_VALUE);
    String kafkaCeHeaderCipherVersion = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_CIPHER_VERSION_VALUE);
    String kafkaCeHeaderCipherName = extractKafkaHeaderValueText(kafkaHeaders,
        KAFKA_CE_HEADER_CIPHER_NAME_VALUE);

    return aesEncryptedPayloadOfKafka(encryptedPayload, kafkaHeaderInitializationVector,
        kafkaHeaderCiphersText, kafkaCeHeaderInitializationVector, kafkaCeHeaderCipherVersion,
        kafkaCeHeaderCipherName);
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
   * @param encryptedPayload a AesEncryptedPayload object for a Kafka key
   * @return the kafka headers needed for given AesEncryptedPayload
   * @see #mapToKafkaHeadersForValue(AesEncryptedPayload)
   */
  static Map<String, byte[]> mapToKafkaHeadersForKey(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return Map.of(
          headerNameIv(true), mapToIvHeaderValue(encryptedPayload),
          headerNameCiphers(true), mapToCipherHeaderValue(encryptedPayload));
    }
    return Map.of();
  }

  /**
   * @param encryptedPayload a AesEncryptedPayload object for a Kafka value a.k.a. payload
   * @return the kafka headers needed for given AesEncryptedPayload
   * @see #mapToKafkaHeadersForValue(AesEncryptedPayload)
   */
  static Map<String, byte[]> mapToKafkaHeadersForValue(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return Map.of(
          // add deprecated kafka headers
          headerNameIv(false), mapToIvHeaderValue(encryptedPayload),
          headerNameCiphers(false), mapToCipherHeaderValue(encryptedPayload),
          // add CloudEvent kafka headers
          KAFKA_CE_HEADER_IV_VALUE, mapToIvHeaderValue(encryptedPayload),
          KAFKA_CE_HEADER_CIPHER_VERSION_VALUE, mapToCipherVersionHeaderValue(encryptedPayload),
          KAFKA_CE_HEADER_CIPHER_NAME_VALUE, mapToCipherNameHeaderValue(encryptedPayload)
      );
    }
    return Map.of();
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the initialization vector. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameIv(boolean)
   */
  static String mapToIvHeaderValueText(AesEncryptedPayload encryptedPayload) {
    return encryptedPayload.initializationVectorBase64();
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the initialization vector. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameIv(boolean)
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
   * @param encryptedPayload the payload
   * @return the value for the cipher metadata. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
   */
  static String mapToCipherHeaderValueText(AesEncryptedPayload encryptedPayload) {
    String encryptionKeyName = encryptedPayload.encryptionKeyAttributeName();
    if (encryptionKeyName == null) {
      encryptionKeyName = VaultEncryptionKeyProviderConfig.DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;
    }

    JsonObject jsonObjectCipher = new JsonObject();
    jsonObjectCipher.add("cipherVersion", Json.value(encryptedPayload.keyVersion()));
    jsonObjectCipher.add("cipherVersionString", Json.NULL);
    jsonObjectCipher.add("cipherName", Json.value(encryptionKeyName));
    JsonObject jsonObjectEncryption = new JsonObject();
    jsonObjectEncryption.add(encryptionKeyName, jsonObjectCipher);
    JsonArray jsonArrayRoot = new JsonArray();
    jsonArrayRoot.add(jsonObjectEncryption);

    return jsonArrayRoot.toString(WriterConfig.MINIMAL);
  }

  /**
   * @param cipherSpec the cipher spec
   * @return the value for the cipher metadata.
   * @see #headerNameCiphers(boolean)
   */
  static String mapToCipherHeaderValueText(EncryptionCipherSpec cipherSpec) {
    String encryptionKeyName = cipherSpec.cipherName();
    if (encryptionKeyName == null) {
      encryptionKeyName = VaultEncryptionKeyProviderConfig.DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;
    }

    JsonObject jsonObjectCipher = new JsonObject();
    jsonObjectCipher.add("cipherVersion", Json.value(cipherSpec.keyVersion()));
    jsonObjectCipher.add("cipherVersionString", Json.NULL);
    jsonObjectCipher.add("cipherName", Json.value(encryptionKeyName));
    JsonObject jsonObjectEncryption = new JsonObject();
    jsonObjectEncryption.add(encryptionKeyName, jsonObjectCipher);
    JsonArray jsonArrayRoot = new JsonArray();
    jsonArrayRoot.add(jsonObjectEncryption);

    return jsonArrayRoot.toString(WriterConfig.MINIMAL);
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the cipher name. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
   */
  static byte[] mapToCipherNameHeaderValue(AesEncryptedPayload encryptedPayload) {
    String cipherNameHeaderText = mapToCipherNameHeaderText(encryptedPayload);
    return cipherNameHeaderText.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the cipher name. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
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

  /**
   * @param encryptedPayload the payload
   * @return the value for the cipher version. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
   */
  static byte[] mapToCipherVersionHeaderValue(AesEncryptedPayload encryptedPayload) {
    String cipherVersionHeaderText = mapToCipherVersionHeaderText(encryptedPayload);
    return cipherVersionHeaderText.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the cipher version. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
   */
  static String mapToCipherVersionHeaderText(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return Integer.toString(encryptedPayload.keyVersion());
    }
    throw new IllegalArgumentException(
        "Cannot call 'mapToCipherVersionHeaderText' for unencrypted payloads.");
  }

  /**
   * @param encryptedPayload the payload
   * @return the value for the cipher metadata. Note: you should check {@link
   * AesEncryptedPayload#isEncrypted()} before calling this method.
   * @see #headerNameCiphers(boolean)
   */
  static byte[] mapToCipherHeaderValue(AesEncryptedPayload encryptedPayload) {
    if (encryptedPayload.isEncrypted()) {
      return mapToCipherHeaderValueText(encryptedPayload)
          .getBytes(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException(
        "Cannot call 'mapToCipherHeaderValue' for unencrypted payloads.");
  }

  /**
   * @param ivRaw the raw kafka header value of the initialization vector
   * @return the initialization vector
   * @see #mapToIvHeaderValue(AesEncryptedPayload)
   * @see #headerNameIv(boolean)
   */
  static byte[] extractIv(byte[] ivRaw) {
    if (ivRaw == null) {
      return new byte[]{};
    }

    return extractIv(byteArrayToUtf8String(ivRaw));
  }

  /**
   * @param ivText the kafka header value of the initialization vector
   * @return the initialization vector
   * @see #mapToIvHeaderValue(AesEncryptedPayload)
   * @see #headerNameIv(boolean)
   */
  static byte[] extractIv(String ivText) {
    if (ivText == null) {
      return new byte[]{};
    }

    return Base64.getDecoder().decode(ivText);
  }

  /**
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
   * @param cipherNameCeHeaderValue the raw CloudEvent kafka header value of the cipher name.
   * @return the key version used to encrypt the payload
   */
  static String extractCipherName(byte[] cipherNameCeHeaderValue) {
    if (cipherNameCeHeaderValue == null) {
      // not encrypted
      return null;
    }
    return byteArrayToUtf8String(cipherNameCeHeaderValue);
  }

  /**
   * @param cipherNameCeHeaderValue the raw CloudEvent kafka header value of the cipher name.
   * @return the key version used to encrypt the payload
   */
  static String extractCipherName(String cipherNameCeHeaderValue) {
    return cipherNameCeHeaderValue;
  }

  /**
   * @param cipherHeaderValue the raw kafka header value of the cipher metadata.
   * @return the key version used to encrypt the payload
   * @see #mapToCipherHeaderValue(AesEncryptedPayload)
   * @see #headerNameCiphers(boolean)
   */
  static int extractKeyVersion(byte[] cipherHeaderValue) {
    if (cipherHeaderValue == null) {
      // not encrypted
      return 0;
    }

    return extractKeyVersion(byteArrayToUtf8String(cipherHeaderValue));
  }

  /**
   * @param ciphersText the kafka header value of the cipher metadata.
   * @return the key version used to encrypt the payload
   * @see #mapToCipherHeaderValue(AesEncryptedPayload)
   * @see #headerNameCiphers(boolean)
   * @see #extractCipherSpec(String)
   */
  static int extractKeyVersion(String ciphersText) {
    EncryptionCipherSpec cipherSpec = extractCipherSpec(ciphersText);
    if (cipherSpec == null) {
      // not encrypted
      return 0;
    }

    return cipherSpec.keyVersion();
  }

  /**
   * @param ciphersText the kafka header value of the cipher metadata.
   * @return the encryptionKeyAttributeName used to fetch the key from the vault
   * @see #mapToCipherHeaderValue(AesEncryptedPayload)
   * @see #headerNameCiphers(boolean)
   * @see #extractCipherSpec(String)
   */
  static String extractEncryptionKeyAttributeName(String ciphersText) {
    EncryptionCipherSpec cipherSpec = extractCipherSpec(ciphersText);
    if (cipherSpec == null) {
      // not encrypted
      return null;
    }

    return cipherSpec.cipherName();
  }

  /**
   * @param cipherHeaderValue the kafka header value of the cipher metadata.
   * @return the EncryptionCipherSpec used to fetch the key from the vault
   * @see #mapToCipherHeaderValue(AesEncryptedPayload)
   * @see #headerNameCiphers(boolean)
   */
  static EncryptionCipherSpec extractCipherSpec(byte[] cipherHeaderValue) {
    if (cipherHeaderValue == null) {
      // not encrypted
      return null;
    }

    return extractCipherSpec(byteArrayToUtf8String(cipherHeaderValue));
  }

  /**
   * @param ciphersText the kafka header value of the cipher metadata.
   * @return the EncryptionCipherSpec used to fetch the key from the vault
   * @see #mapToCipherHeaderValue(AesEncryptedPayload)
   * @see #headerNameCiphers(boolean)
   */
  static EncryptionCipherSpec extractCipherSpec(String ciphersText) {
    if (ciphersText == null) {
      // not encrypted
      return null;
    }

    try {
      JsonArray jsonArrayRoot = Json.parse(ciphersText).asArray();
      if (jsonArrayRoot.size() != 1) {
        throw new JsonParsingRuntimeException(
            "Cannot parse cipher. Error=CipherText has not exactly one element. Cipher="
                + ciphersText);
      }

      JsonObject jsonObjectEncryption = jsonArrayRoot.get(0).asObject();
      List<String> jsonElementNames = jsonObjectEncryption.names();
      if (jsonElementNames.size() != 1) {
        throw new JsonParsingRuntimeException(
            "Cannot parse cipher. Error=Root object has not exactly one element. Cipher="
                + ciphersText);
      }

      String cipherName = jsonElementNames.get(0);
      JsonObject jsonAes = jsonObjectEncryption.get(cipherName).asObject();
      int keyVersion = jsonAes.getInt("cipherVersion");

      return new EncryptionCipherSpec(keyVersion, cipherName);
    } catch (JsonParsingRuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new JsonParsingRuntimeException(
          "Cannot parse cipher. Error=" + ex.getMessage() + " Cipher=" + ciphersText);
    }
  }
}
