package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultException;
import java.util.Objects;

public class SingleTopicVaultEncryptionKeyProviderConfig
    implements VaultEncryptionKeyProviderConfig {

  private final boolean isEncryptedTopic;
  private final String kafkaTopicName;
  private final VaultConnectionConfig vaultConnectionConfig;
  private final String vaultPath;
  private final String encryptionKeyAttributeName;

  public SingleTopicVaultEncryptionKeyProviderConfig(
      boolean isEncryptedTopic,
      String kafkaTopicName,
      VaultConnectionConfig vaultConnectionConfig,
      String vaultPath) {
    this(isEncryptedTopic, kafkaTopicName, vaultConnectionConfig, vaultPath,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
  }

  public SingleTopicVaultEncryptionKeyProviderConfig(
      boolean isEncryptedTopic,
      String kafkaTopicName,
      VaultConnectionConfig vaultConnectionConfig,
      String vaultPath, String encryptionKeyAttributeName) {
    if (isEncryptedTopic) {
      Objects.requireNonNull(vaultConnectionConfig, "vaultConnectionConfig is required");
      Objects.requireNonNull(vaultPath, "vaultPath is required");
    }
    this.isEncryptedTopic = isEncryptedTopic;
    this.kafkaTopicName = kafkaTopicName;
    this.vaultConnectionConfig = vaultConnectionConfig;
    this.vaultPath = vaultPath;
    this.encryptionKeyAttributeName = encryptionKeyAttributeName;
  }

  public static SingleTopicVaultEncryptionKeyProviderConfigBuilder builder() {
    return new SingleTopicVaultEncryptionKeyProviderConfigBuilder();
  }

  @Override
  public RenewableVault createRenewableVault() {
    try {
      return vaultConnectionConfig.createRenewableVault();
    } catch (VaultException e) {
      throw new VaultRuntimeException(e);
    }
  }

  @Override
  public boolean isEncryptedTopic(String kafkaTopicName) {
    if (this.kafkaTopicName == null) {
      return isEncryptedTopic;
    }
    if (Objects.equals(this.kafkaTopicName, kafkaTopicName)) {
      return isEncryptedTopic;
    }
    return false;
  }

  @Override
  public String vaultPath(String kafkaTopicName) {
    return vaultPath;
  }

  @Override
  public String encryptionKeyAttributeName(String kafkaTopicName) {
    return Objects.requireNonNullElse(encryptionKeyAttributeName,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
  }

  public static class SingleTopicVaultEncryptionKeyProviderConfigBuilder {

    private boolean isEncryptedTopic;
    private String kafkaTopicName;
    private VaultConnectionConfig vaultConnectionConfig;
    private String vaultPath;
    private String encryptionKeyAttributeName;

    public SingleTopicVaultEncryptionKeyProviderConfigBuilder kafkaTopicName(
        String kafkaTopicName) {
      this.kafkaTopicName = kafkaTopicName;
      return this;
    }

    public SingleTopicVaultEncryptionKeyProviderConfigBuilder isEncryptedTopic(
        boolean isEncryptedTopic) {
      this.isEncryptedTopic = isEncryptedTopic;
      return this;
    }

    public SingleTopicVaultEncryptionKeyProviderConfigBuilder vaultConnectionConfig(
        VaultConnectionConfig vaultConnectionConfig) {
      this.vaultConnectionConfig = vaultConnectionConfig;
      return this;
    }

    public SingleTopicVaultEncryptionKeyProviderConfigBuilder vaultPath(String vaultPath) {
      this.vaultPath = vaultPath;
      return this;
    }

    public SingleTopicVaultEncryptionKeyProviderConfigBuilder encryptionKeyAttributeName(
        String encryptionKeyAttributeName) {
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
      return this;
    }

    public SingleTopicVaultEncryptionKeyProviderConfig build() {
      return new SingleTopicVaultEncryptionKeyProviderConfig(isEncryptedTopic,
          kafkaTopicName,
          vaultConnectionConfig,
          vaultPath,
          encryptionKeyAttributeName);
    }
  }
}
