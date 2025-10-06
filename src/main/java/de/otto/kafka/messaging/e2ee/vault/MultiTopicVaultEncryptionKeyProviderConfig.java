package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Encryption key provider that can be configured for more than one topic. Otherwise, each topic
 * needs its own configuration.
 */
public final class MultiTopicVaultEncryptionKeyProviderConfig implements
    VaultEncryptionKeyProviderConfig {

  private final VaultConnectionConfig vaultConnectionConfig;
  private final List<KafkaTopicConfigEntry> configEntries;

  /**
   * Constructor for that class.
   *
   * @param vaultConnectionConfig the vault connection configuration
   * @param configEntries         the list of the configuration entries
   */
  public MultiTopicVaultEncryptionKeyProviderConfig(
      VaultConnectionConfig vaultConnectionConfig,
      List<KafkaTopicConfigEntry> configEntries) {
    Objects.requireNonNull(vaultConnectionConfig, "vaultConnectionConfig is required");
    Objects.requireNonNull(configEntries, "configEntries is required");
    this.vaultConnectionConfig = vaultConnectionConfig;
    this.configEntries = configEntries;
  }

  /**
   * Use the builder pattern to create instances of that class.
   *
   * @return the builder
   */
  public static MultiTopicVaultEncryptionKeyProviderConfigBuilder builder() {
    return new MultiTopicVaultEncryptionKeyProviderConfigBuilder();
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
    return getValueForTopic(kafkaTopicName, KafkaTopicConfigEntry::encryptionEnabled, true);
  }

  @Override
  public String vaultPath(String kafkaTopicName) {
    VaultPathExpression pathExpression = getValueForTopic(kafkaTopicName,
        KafkaTopicConfigEntry::pathExpression, null);
    if (pathExpression == null) {
      throw new VaultConfigException("No vault path found for topic name " + kafkaTopicName);
    }
    if (pathExpression.vaultPath() != null) {
      return pathExpression.vaultPath();
    }

    String teamName = getValueForTopic(kafkaTopicName, KafkaTopicConfigEntry::teamName,
        null);
    if (teamName == null) {
      if (pathExpression.vaultPathTemplate().contains("%TEAMNAME%")) {
        throw new VaultConfigException("No team found for topic name " + kafkaTopicName);
      }
      return pathExpression.vaultPathTemplate()
          .replace("%TOPICNAME%", kafkaTopicName);
    }

    return pathExpression.vaultPathTemplate()
        .replace("%TOPICNAME%", kafkaTopicName)
        .replace("%TEAMNAME%", teamName);
  }

  @Override
  public String encryptionKeyAttributeName(String kafkaTopicName) {
    return getValueForTopic(kafkaTopicName,
        KafkaTopicConfigEntry::encryptionKeyAttributeName,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
  }

  private <T> T getValueForTopic(String topic, Function<KafkaTopicConfigEntry, T> mapping,
      T defaultValue) {
    int bestScore = -1;
    T currentBestValue = defaultValue;
    for (KafkaTopicConfigEntry configEntry : this.configEntries) {
      T value = mapping.apply(configEntry);
      if (value == null) {
        continue;
      }
      if (configEntry.isDefault() && bestScore == -1) {
        bestScore = 0;
        currentBestValue = value;
      }
      if (configEntry.kafkaTopicName() != null
          && topic.equals(configEntry.kafkaTopicName())) {
        // best possible match
        return value;
      }
      if (configEntry.kafkaTopicNamePrefix() != null
          && topic.startsWith(configEntry.kafkaTopicNamePrefix())) {
        int score = configEntry.kafkaTopicNamePrefix().length();
        if (score > bestScore) {
          bestScore = score;
          currentBestValue = value;
        }
      }
    }

    return currentBestValue;
  }

  /**
   * Builder class for the MultiTopicVaultEncryptionKeyProviderConfig
   */
  public static class MultiTopicVaultEncryptionKeyProviderConfigBuilder {

    private final List<KafkaTopicConfigEntry> configEntries = new ArrayList<>();
    private VaultConnectionConfig vaultConnectionConfig;

    /**
     * Default constructor
     */
    public MultiTopicVaultEncryptionKeyProviderConfigBuilder() {
    }

    /**
     * Sets the "vaultConnectionConfig" value.
     *
     * @param vaultConnectionConfig the vault connection configuration
     * @return the builder
     */
    public MultiTopicVaultEncryptionKeyProviderConfigBuilder vaultConnectionConfig(
        VaultConnectionConfig vaultConnectionConfig) {
      this.vaultConnectionConfig = vaultConnectionConfig;
      return this;
    }

    /**
     * Sets the "configEntries" value.
     *
     * @param configEntries all configuration entries
     * @return the builder
     * @see #configEntry(KafkaTopicConfigEntry)
     */
    public MultiTopicVaultEncryptionKeyProviderConfigBuilder configEntries(
        Collection<KafkaTopicConfigEntry> configEntries) {
      this.configEntries.addAll(configEntries);
      return this;
    }

    /**
     * Adds another config entry to the end of the configuration entry list
     *
     * @param configEntry a new configuration entry
     * @return the builder
     */
    public MultiTopicVaultEncryptionKeyProviderConfigBuilder configEntry(
        KafkaTopicConfigEntry configEntry) {
      this.configEntries.add(configEntry);
      return this;
    }

    /**
     * Creates a MultiTopicVaultEncryptionKeyProviderConfig
     *
     * @return the MultiTopicVaultEncryptionKeyProviderConfig
     */
    public MultiTopicVaultEncryptionKeyProviderConfig build() {
      return new MultiTopicVaultEncryptionKeyProviderConfig(vaultConnectionConfig, configEntries);
    }
  }

  /**
   * Configuration entry
   */
  public static class KafkaTopicConfigEntry {

    private final boolean isDefault;
    private final Boolean encryptionEnabled;
    private final String kafkaTopicName;
    private final String kafkaTopicNamePrefix;
    private final VaultPathExpression pathExpression;
    private final String teamName;
    private final String encryptionKeyAttributeName;

    /**
     * the constructor. Just use the builder which is easier to use.
     *
     * @param isDefault                  <code>true</code> its the default/base config
     * @param encryptionEnabled          <code>true</code> encryption is enabled.
     *                                   <code>false</code>
     *                                   encryption is disabled. <code>null</code> encryption is not
     *                                   defined using that entry.
     * @param kafkaTopicName             a topic name
     * @param kafkaTopicNamePrefix       a topic name prefix
     * @param vaultPath                  a vault path
     * @param vaultPathTemplate          a vault path template. Valid placeholders are
     *                                   <code>%TEAMNAME%</code> and <code>%TOPICNAME%</code>
     * @param teamName                   a team name
     * @param encryptionKeyAttributeName JSON attribute name of the encryption key within the vault
     *                                   payload
     * @see #builder()
     */
    public KafkaTopicConfigEntry(boolean isDefault, Boolean encryptionEnabled,
        String kafkaTopicName, String kafkaTopicNamePrefix,
        String vaultPath, String vaultPathTemplate,
        String teamName, String encryptionKeyAttributeName) {
      this.isDefault = isDefault;
      this.encryptionEnabled = encryptionEnabled;
      this.kafkaTopicName = kafkaTopicName;
      this.kafkaTopicNamePrefix = kafkaTopicNamePrefix;
      if (vaultPath == null && vaultPathTemplate == null) {
        this.pathExpression = null;
      } else {
        this.pathExpression = new VaultPathExpression(vaultPath, vaultPathTemplate);
      }
      this.teamName = teamName;
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
    }

    /**
     * Use builder pattern to create entries.
     *
     * @return a builder
     */
    public static KafkaTopicConfigEntryBuilder builder() {
      return new KafkaTopicConfigEntryBuilder();
    }

    /**
     * Gets the "isDefault" property
     *
     * @return <code>true</code> its the default/base config
     */
    public boolean isDefault() {
      return isDefault;
    }

    /**
     * Gets the "encryptionEnabled" property
     *
     * @return <code>true</code> encryption is enabled. <code>false</code> encryption is disabled.
     * <code>null</code> encryption is not defined using that entry
     */
    public Boolean encryptionEnabled() {
      return encryptionEnabled;
    }

    /**
     * Gets the "kafkaTopicName" property
     *
     * @return a topic name
     */
    public String kafkaTopicName() {
      return kafkaTopicName;
    }

    /**
     * Gets the "kafkaTopicNamePrefix" property
     *
     * @return a topic name prefix
     */
    public String kafkaTopicNamePrefix() {
      return kafkaTopicNamePrefix;
    }

    /**
     * Gets the "pathExpression" property
     *
     * @return the vault path expression
     */
    public VaultPathExpression pathExpression() {
      return pathExpression;
    }

    /**
     * Gets the "teamName" property
     *
     * @return the team name
     */
    public String teamName() {
      return teamName;
    }

    /**
     * Gets the "encryptionKeyAttributeName" property
     *
     * @return JSON attribute name of the encryption key within the vault payload
     */
    public String encryptionKeyAttributeName() {
      return encryptionKeyAttributeName;
    }

    @Override
    public String toString() {
      return "KafkaTopicConfig{" +
          "isDefault=" + isDefault +
          ", encryptionEnabled=" + encryptionEnabled +
          ", kafkaTopicName='" + kafkaTopicName + '\'' +
          ", kafkaTopicNamePrefix='" + kafkaTopicNamePrefix + '\'' +
          ", pathExpression='" + pathExpression + '\'' +
          ", teamName='" + teamName + '\'' +
          ", encryptionKeyAttributeName='" + encryptionKeyAttributeName + '\'' +
          '}';
    }
  }

  /**
   * Builder class for a configuration entry.
   */
  public static final class KafkaTopicConfigEntryBuilder {

    private boolean isDefault = false;
    private Boolean encryptionEnabled = true;
    private String kafkaTopicName;
    private String kafkaTopicNamePrefix;
    private String vaultPath;
    private String vaultPathTemplate;
    private String teamName;
    private String encryptionKeyAttributeName;

    /**
     * Default constructor
     */
    public KafkaTopicConfigEntryBuilder() {
    }

    /**
     * Sets the "isDefault" value.
     *
     * @param isDefault <code>true</code> its the default/base config entry
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder isDefault(Boolean isDefault) {
      this.isDefault = Objects.requireNonNullElse(isDefault, false);
      return this;
    }

    /**
     * Sets the "encryptionEnabled" value.
     *
     * @param encryptionEnabled <code>true</code> encryption is enabled. <code>false</code>
     *                          encryption is disabled. <code>null</code> encryption is not defined
     *                          using that entry.
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder encryptionEnabled(Boolean encryptionEnabled) {
      this.encryptionEnabled = encryptionEnabled;
      return this;
    }

    /**
     * Sets the "kafkaTopicName" value.
     *
     * @param kafkaTopicName the kafka topic name which is associated with that config entry
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder kafkaTopicName(String kafkaTopicName) {
      this.kafkaTopicName = kafkaTopicName;
      return this;
    }

    /**
     * Sets the "kafkaTopicNamePrefix" value.
     *
     * @param kafkaTopicNamePrefix the kafka topic name prefix of topic that should be associated
     *                             with that config entry
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder kafkaTopicNamePrefix(String kafkaTopicNamePrefix) {
      this.kafkaTopicNamePrefix = kafkaTopicNamePrefix;
      return this;
    }

    /**
     * Sets the "vaultPath" value.
     *
     * @param vaultPath the vault path
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder vaultPath(String vaultPath) {
      this.vaultPath = vaultPath;
      return this;
    }

    /**
     * Sets the "vaultPathTemplate" value.
     *
     * @param vaultPathTemplate the template may contain <code>%TOPICNAME%</code> and/or
     *                          <code>%TEAMNAME%</code>
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder vaultPathTemplate(String vaultPathTemplate) {
      this.vaultPathTemplate = vaultPathTemplate;
      return this;
    }

    /**
     * Sets the "teamName" value.
     *
     * @param teamName th team name
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder teamName(String teamName) {
      this.teamName = teamName;
      return this;
    }

    /**
     * Sets the "encryptionKeyAttributeName" value.
     *
     * @param encryptionKeyAttributeName name of the JSON attribute within the vault secret that
     *                                   contains the encryption key. The default value is
     *                                   <code>encryption_key</code>
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder encryptionKeyAttributeName(
        String encryptionKeyAttributeName) {
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
      return this;
    }

    /**
     * Creates the entry.
     *
     * @return the config entry
     */
    public KafkaTopicConfigEntry build() {
      return new KafkaTopicConfigEntry(isDefault, encryptionEnabled, kafkaTopicName,
          kafkaTopicNamePrefix, vaultPath, vaultPathTemplate, teamName, encryptionKeyAttributeName);
    }
  }

  /**
   * Value class which holds the vault path expression.
   */
  public static final class VaultPathExpression {

    private final String vaultPath;
    private final String vaultPathTemplate;

    /**
     * Constructor for that class
     *
     * @param vaultPath         a vault path
     * @param vaultPathTemplate a vault path template
     */
    public VaultPathExpression(String vaultPath, String vaultPathTemplate) {
      this.vaultPath = vaultPath;
      this.vaultPathTemplate = vaultPathTemplate;
    }

    /**
     * Gets the vault path
     *
     * @return the vault path
     */
    public String vaultPath() {
      return vaultPath;
    }

    /**
     * Gets the vault path expression
     *
     * @return the vault path expression
     */
    public String vaultPathTemplate() {
      return vaultPathTemplate;
    }

    @Override
    public String toString() {
      return "PathExpression{" +
          "vaultPath='" + vaultPath + '\'' +
          ", vaultPathTemplate='" + vaultPathTemplate + '\'' +
          '}';
    }
  }
}
