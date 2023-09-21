package de.otto.messaging.kafka.e2ee.helper;

import de.otto.messaging.kafka.e2ee.vault.VaultEncryptionKeyProviderConfig;
import io.github.jopenlibs.vault.VaultConfig;

public class DummyVaultEncryptionKeyProviderConfig implements VaultEncryptionKeyProviderConfig {

  private final DummyVault dummyVault;

  public DummyVaultEncryptionKeyProviderConfig() {
    this.dummyVault = new DummyVault(new VaultConfig());
  }

  @Override
  public DummyVault createRenewableVault() {
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
