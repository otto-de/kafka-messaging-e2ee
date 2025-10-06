package de.otto.kafka.messaging.e2ee.vault;

/**
 * API for encryption keys using hashicorp vault
 */
public interface VaultEncryptionKeyProviderConfig {

  /**
   * The default name of the JSON property within a vault secret that contains the encryption key
   */
  String DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME = "encryption_key";

  /**
   * creates a vault API which only supports read operations.
   *
   * @return a read only vault API
   */
  ReadonlyVaultApi createReadonlyVault();

  /**
   * Determines if according to the configuration the topic is encrypted or contains encrypted
   * data.
   *
   * @param kafkaTopicName a topic name
   * @return <code>true</code> when according to the configuration the topic should be encrypted.
   * <code>false</code> when according to the configuration the topic should not be encrypted.
   */
  boolean isEncryptedTopic(String kafkaTopicName);

  /**
   * Calculates the vault path for a kafka topic.
   *
   * @param kafkaTopicName a topic name
   * @return the vault path of that topic
   */
  String vaultPath(String kafkaTopicName);

  /**
   * Gets the name of the JSON attribute in the vault payload that contains the encryption key.
   *
   * @param kafkaTopicName a topic name
   * @return the encryption key JSON attribute name
   */
  default String encryptionKeyAttributeName(String kafkaTopicName) {
    return DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;
  }
}
