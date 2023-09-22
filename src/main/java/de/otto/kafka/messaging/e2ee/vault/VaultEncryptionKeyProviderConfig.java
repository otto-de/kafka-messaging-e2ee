package de.otto.kafka.messaging.e2ee.vault;

public interface VaultEncryptionKeyProviderConfig {

  String DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME = "encryption_key";

  ReadonlyVaultApi createReadonlyVault();

  boolean isEncryptedTopic(String kafkaTopicName);

  String vaultPath(String kafkaTopicName);

  default String encryptionKeyAttributeName(String kafkaTopicName) {
    return DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;
  }
}
