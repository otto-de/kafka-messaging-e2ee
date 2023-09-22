package de.otto.kafka.messaging.e2ee.helper;

import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig;

public class DummyVaultEncryptionKeyProviderConfig implements VaultEncryptionKeyProviderConfig {

  private final DummyVault dummyVault;

  public DummyVaultEncryptionKeyProviderConfig() {
    this.dummyVault = new DummyVault();
  }

  @Override
  public DummyVault createReadonlyVault() {
    return dummyVault;
  }

  public DummyVault getDummyVault() {
    return dummyVault;
  }

  @Override
  public boolean isEncryptedTopic(String kafkaTopicName) {
    return true;
  }

  @Override
  public String vaultPath(String kafkaTopicName) {
    return "/galaogos_" + kafkaTopicName;
  }
}
