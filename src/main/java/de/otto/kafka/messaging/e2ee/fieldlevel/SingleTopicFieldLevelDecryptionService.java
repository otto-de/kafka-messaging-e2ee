package de.otto.kafka.messaging.e2ee.fieldlevel;

import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import java.util.Objects;

/**
 * Single topic field level decryption service. It's a wrapper for the FieldLevelDecryptionService.
 */
public final class SingleTopicFieldLevelDecryptionService {

  private final FieldLevelDecryptionService fieldLevelDecryptionService;
  private final String kafkaTopicName;

  /**
   * The constructor using a FieldLevelDecryptionService.
   *
   * @param fieldLevelDecryptionService the FieldLevelDecryptionService
   * @param kafkaTopicName              the name of the kafka topic which contains the encrypted
   *                                    fields
   */
  public SingleTopicFieldLevelDecryptionService(
      FieldLevelDecryptionService fieldLevelDecryptionService, String kafkaTopicName) {
    Objects.requireNonNull(fieldLevelDecryptionService, "fieldLevelDecryptionService");
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName");
    this.fieldLevelDecryptionService = fieldLevelDecryptionService;
    this.kafkaTopicName = kafkaTopicName;
  }

  /**
   * The constructor using a DecryptionService.
   *
   * @param decryptionService the DecryptionService
   * @param kafkaTopicName    the name of the kafka topic which contains the encrypted fields
   */
  public SingleTopicFieldLevelDecryptionService(DecryptionService decryptionService,
      String kafkaTopicName) {
    this(new FieldLevelDecryptionService(decryptionService), kafkaTopicName);
  }

  /**
   * The constructor using a EncryptionKeyProvider.
   *
   * @param encryptionKeyProvider the EncryptionKeyProvider
   * @param kafkaTopicName        the name of the kafka topic which contains the encrypted fields
   */
  public SingleTopicFieldLevelDecryptionService(EncryptionKeyProvider encryptionKeyProvider,
      String kafkaTopicName) {
    this(new DecryptionService(encryptionKeyProvider), kafkaTopicName);
  }

  /**
   * decrypts the given text (depending on the content).
   *
   * @param encryptedString the (potentially) encrypted text value. Can be <code>null</code>.
   * @return The plain text or <code>null</code>
   */
  public String decrypt(String encryptedString) {
    return fieldLevelDecryptionService.decryptFieldValue(kafkaTopicName, encryptedString);
  }

  /**
   * decrypts the given text (depending on the content).
   *
   * @param encryptedString the (potentially) encrypted text value. Can be <code>null</code>.
   * @return The plain text or <code>null</code>
   */
  public String decrypt(EncryptedString encryptedString) {
    return fieldLevelDecryptionService.decryptFieldValue(kafkaTopicName, encryptedString);
  }
}
