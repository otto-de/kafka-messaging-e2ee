package de.otto.kafka.messaging.e2ee.fieldlevel;

import static de.otto.kafka.messaging.e2ee.fieldlevel.DefaultFieldLevelEncryptionConfiguration.AES_V1_PREFIX;
import static de.otto.kafka.messaging.e2ee.fieldlevel.DefaultFieldLevelEncryptionConfiguration.FIELD_DELIMITER;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import java.util.Base64;
import java.util.Objects;

/**
 * Encryption service. Used to encrypt fields.
 */
public final class FieldLevelEncryptionService {

  private final EncryptionService encryptionService;

  /**
   * Constructor for that class.
   *
   * @param encryptionService the EncryptionService
   */
  public FieldLevelEncryptionService(EncryptionService encryptionService) {
    Objects.requireNonNull(encryptionService, "encryptionService");
    this.encryptionService = encryptionService;
  }

  /**
   * Constructor for that class.
   *
   * @param encryptionKeyProvider the EncryptionKeyProvider
   */
  public FieldLevelEncryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    this(new EncryptionService(encryptionKeyProvider));
  }

  /**
   * encrypts the given text (depending on the topic related configuration).
   *
   * @param kafkaTopicName name of the Kafka Topic the field value is for.
   * @param plainText      the plain text value. Can be <code>null</code>.
   * @return The encrypted string or <code>null</code>
   */
  public String encryptFieldValueToString(String kafkaTopicName, String plainText) {
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName");
    if (plainText == null) {
      return null;
    }

    AesEncryptedPayload aesEncryptedPayload = encryptionService.encryptPayloadWithAes(
        kafkaTopicName, plainText);
    if (!aesEncryptedPayload.isEncrypted()) {
      return plainText;
    }

    String encryptedPayloadBase64 = Base64.getEncoder()
        .encodeToString(aesEncryptedPayload.encryptedPayload());
    return AES_V1_PREFIX + FIELD_DELIMITER
        + aesEncryptedPayload.keyVersion() + FIELD_DELIMITER
        + aesEncryptedPayload.initializationVectorBase64() + FIELD_DELIMITER
        + encryptedPayloadBase64;
  }

  /**
   * encrypts the given text (depending on the topic related configuration).
   *
   * @param kafkaTopicName name of the Kafka Topic the field value is for.
   * @param plainText      the plain text value. Can be <code>null</code>.
   * @return The encrypted string or <code>null</code>
   */
  public EncryptedString encryptFieldValueToEncryptedString(String kafkaTopicName,
      String plainText) {
    return EncryptedString.of(encryptFieldValueToString(kafkaTopicName, plainText));
  }
}
