package de.otto.kafka.messaging.e2ee.fieldlevel;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import java.util.Objects;

/**
 * Single topic field level encryption service. It's a wrapper for FieldLevelEncryptionService.
 */
public final class SingleTopicFieldLevelEncryptionService {

  private final FieldLevelEncryptionService fieldLevelEncryptionService;
  private final String kafkaTopicName;

  /**
   * The constructor using a FieldLevelEncryptionService.
   *
   * @param fieldLevelEncryptionService the FieldLevelEncryptionService
   * @param kafkaTopicName              the name of the kafka topic which contains the
   *                                    to-be-encrypted fields
   */
  public SingleTopicFieldLevelEncryptionService(
      FieldLevelEncryptionService fieldLevelEncryptionService, String kafkaTopicName) {
    Objects.requireNonNull(fieldLevelEncryptionService, "fieldLevelEncryptionService");
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName");
    this.fieldLevelEncryptionService = fieldLevelEncryptionService;
    this.kafkaTopicName = kafkaTopicName;
  }

  /**
   * The constructor using a EncryptionService.
   *
   * @param encryptionService the EncryptionService
   * @param kafkaTopicName    the name of the kafka topic which contains the to-be-encrypted fields
   */
  public SingleTopicFieldLevelEncryptionService(EncryptionService encryptionService,
      String kafkaTopicName) {
    this(new FieldLevelEncryptionService(encryptionService), kafkaTopicName);
  }

  /**
   * The constructor using a EncryptionKeyProvider.
   *
   * @param encryptionKeyProvider the EncryptionKeyProvider
   * @param kafkaTopicName        the name of the kafka topic which contains the to-be-encrypted
   *                              fields
   */
  public SingleTopicFieldLevelEncryptionService(EncryptionKeyProvider encryptionKeyProvider,
      String kafkaTopicName) {
    this(new EncryptionService(encryptionKeyProvider), kafkaTopicName);
  }

  /**
   * encrypts the given text (depending on the topic related configuration).
   *
   * @param plainText the plain text value. Can be <code>null</code>.
   * @return The encrypted string or <code>null</code>
   */
  public String encryptFieldValueToString(String plainText) {
    return fieldLevelEncryptionService.encryptFieldValueToString(kafkaTopicName, plainText);
  }

  /**
   * encrypts the given text (depending on the topic related configuration).
   *
   * @param plainText the plain text value. Can be <code>null</code>.
   * @return The encrypted string or <code>null</code>
   */
  public EncryptedString encryptToEncryptedString(String plainText) {
    return fieldLevelEncryptionService.encryptFieldValueToEncryptedString(kafkaTopicName,
        plainText);
  }
}
