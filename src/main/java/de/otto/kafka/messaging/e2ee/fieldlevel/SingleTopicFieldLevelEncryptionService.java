package de.otto.kafka.messaging.e2ee.fieldlevel;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import java.util.Objects;

public final class SingleTopicFieldLevelEncryptionService {

  private final FieldLevelEncryptionService fieldLevelEncryptionService;
  private final String kafkaTopicName;

  public SingleTopicFieldLevelEncryptionService(
      FieldLevelEncryptionService fieldLevelEncryptionService, String kafkaTopicName) {
    Objects.requireNonNull(fieldLevelEncryptionService, "fieldLevelEncryptionService");
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName");
    this.fieldLevelEncryptionService = fieldLevelEncryptionService;
    this.kafkaTopicName = kafkaTopicName;
  }

  public SingleTopicFieldLevelEncryptionService(EncryptionService encryptionService,
      String kafkaTopicName) {
    this(new FieldLevelEncryptionService(encryptionService), kafkaTopicName);
  }

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
