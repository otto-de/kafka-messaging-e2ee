package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultException;
import java.util.Objects;

/**
 * Implementation of VaultEncryptionKeyProviderConfig which can handle one topic only.
 */
public final class SingleTopicVaultEncryptionKeyProviderConfig
    implements VaultEncryptionKeyProviderConfig {

  private final boolean isEncryptedTopic;
  private final String kafkaTopicName;
  private final VaultConnectionConfig vaultConnectionConfig;
  private final String vaultPath;
  private final String encryptionKeyAttributeName;

  /**
   * Constructor using the default encryptionKeyAttributeName.
   *
   * @param isEncryptedTopic      <code>true</code> the topic shall be encrypted.
   *                              <code>false</code>
   *                              the topic will not be encrypted.
   * @param kafkaTopicName        the kafka topic name
   * @param vaultConnectionConfig the vault connection configuration
   * @param vaultPath             the path within the vault where the different versions of
   *                              encryption keys are stored
   */
  public SingleTopicVaultEncryptionKeyProviderConfig(
      boolean isEncryptedTopic,
      String kafkaTopicName,
      VaultConnectionConfig vaultConnectionConfig,
      String vaultPath) {
    this(isEncryptedTopic, kafkaTopicName, vaultConnectionConfig, vaultPath,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
  }

  /**
   * Constructor
   *
   * @param isEncryptedTopic           <code>true</code> the topic shall be encrypted.
   *                                   <code>false</code> the topic will not be encrypted.
   * @param kafkaTopicName             the kafka topic name
   * @param vaultConnectionConfig      the vault connection configuration
   * @param vaultPath                  the path within the vault where the different versions of
   *                                   encryption keys are stored
   * @param encryptionKeyAttributeName name of the vault property that contains the encryption key
   */
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

  /**
   * Builder pattern
   *
   * @return a builder
   */
  public static SingleTopicVaultEncryptionKeyProviderConfigBuilder builder() {
    return new SingleTopicVaultEncryptionKeyProviderConfigBuilder();
  }

  @Override
  public RenewableVault createReadonlyVault() {
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

  /**
   * Builder for SingleTopicVaultEncryptionKeyProviderConfig
   */
  public static class SingleTopicVaultEncryptionKeyProviderConfigBuilder {

    private boolean isEncryptedTopic = false;
    private String kafkaTopicName;
    private VaultConnectionConfig vaultConnectionConfig;
    private String vaultPath;
    private String encryptionKeyAttributeName = DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;

    /**
     * Default constructor
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder() {
    }

    /**
     * Sets the kafkaTopicName.
     *
     * @param kafkaTopicName the kafka topic name
     * @return this builder
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder kafkaTopicName(
        String kafkaTopicName) {
      this.kafkaTopicName = kafkaTopicName;
      return this;
    }

    /**
     * Sets the isEncryptedTopic property. Default value is <code>false</code>
     *
     * @param isEncryptedTopic <code>true</code> the topic shall be encrypted. <code>false</code>
     *                         the topic will not be encrypted.
     * @return this builder
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder isEncryptedTopic(
        boolean isEncryptedTopic) {
      this.isEncryptedTopic = isEncryptedTopic;
      return this;
    }

    /**
     * Sets the vaultConnectionConfig.
     *
     * @param vaultConnectionConfig the vault connection configuration
     * @return this builder
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder vaultConnectionConfig(
        VaultConnectionConfig vaultConnectionConfig) {
      this.vaultConnectionConfig = vaultConnectionConfig;
      return this;
    }

    /**
     * Sets the vault path where the encryption keys are stored.
     *
     * @param vaultPath the path within the vault where the different versions of encryption keys
     *                  are stored
     * @return this builder
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder vaultPath(String vaultPath) {
      this.vaultPath = vaultPath;
      return this;
    }

    /**
     * Sets the encryptionKeyAttributeName property.
     *
     * @param encryptionKeyAttributeName name of the vault property that contains the encryption
     *                                   key
     * @return this builder
     */
    public SingleTopicVaultEncryptionKeyProviderConfigBuilder encryptionKeyAttributeName(
        String encryptionKeyAttributeName) {
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
      return this;
    }

    /**
     * Creates the SingleTopicVaultEncryptionKeyProviderConfig
     *
     * @return the configuration
     */
    public SingleTopicVaultEncryptionKeyProviderConfig build() {
      return new SingleTopicVaultEncryptionKeyProviderConfig(isEncryptedTopic,
          kafkaTopicName,
          vaultConnectionConfig,
          vaultPath,
          encryptionKeyAttributeName);
    }
  }
}
