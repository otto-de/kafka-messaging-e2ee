package de.otto.messaging.kafka.e2ee;

import de.otto.messaging.kafka.e2ee.vault.VaultEncryptionKeyProviderConfig;
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
   * Name of Kafka Header for the cipher metadata for the partition key
   */
  String KAFKA_HEADER_CIPHER_KEY = "encryption/key/ciphers";
  /**
   * Name of Kafka Header for the cipher metadata for the payload (or value)
   */
  String KAFKA_HEADER_CIPHER_VALUE = "encryption/ciphers";

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
    String kafkaHeaderInitializationVector = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameIv(false));
    String kafkaHeaderCiphersText = extractKafkaHeaderValueText(kafkaHeaders,
        headerNameCiphers(false));
    return aesEncryptedPayloadOfKafka(encryptedPayload, kafkaHeaderInitializationVector,
        kafkaHeaderCiphersText);
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
          headerNameIv(false), mapToIvHeaderValue(encryptedPayload),
          headerNameCiphers(false), mapToCipherHeaderValue(encryptedPayload));
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

    return extractIv(new String(ivRaw, StandardCharsets.UTF_8));
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

    String ciphersText = new String(cipherHeaderValue, StandardCharsets.UTF_8);
    return extractKeyVersion(ciphersText);
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

    String ciphersText = new String(cipherHeaderValue, StandardCharsets.UTF_8);
    return extractCipherSpec(ciphersText);
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
