package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class MultiTopicVaultEncryptionKeyProviderConfig implements
    VaultEncryptionKeyProviderConfig {

  private final VaultConnectionConfig vaultConnectionConfig;
  private final List<KafkaTopicConfigEntry> configEntries;

  public MultiTopicVaultEncryptionKeyProviderConfig(
      VaultConnectionConfig vaultConnectionConfig,
      List<KafkaTopicConfigEntry> configEntries) {
    Objects.requireNonNull(vaultConnectionConfig, "vaultConnectionConfig is required");
    Objects.requireNonNull(configEntries, "configEntries is required");
    this.vaultConnectionConfig = vaultConnectionConfig;
    this.configEntries = configEntries;
  }

  public static MultiTopicVaultEncryptionKeyProviderConfigBuilder builder() {
    return new MultiTopicVaultEncryptionKeyProviderConfigBuilder();
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

  public static class MultiTopicVaultEncryptionKeyProviderConfigBuilder {

    private final List<KafkaTopicConfigEntry> configEntries = new ArrayList<>();
    private VaultConnectionConfig vaultConnectionConfig;

    public MultiTopicVaultEncryptionKeyProviderConfigBuilder vaultConnectionConfig(
        VaultConnectionConfig vaultConnectionConfig) {
      this.vaultConnectionConfig = vaultConnectionConfig;
      return this;
    }

    public MultiTopicVaultEncryptionKeyProviderConfigBuilder configEntries(
        Collection<KafkaTopicConfigEntry> configEntries) {
      this.configEntries.addAll(configEntries);
      return this;
    }

    public MultiTopicVaultEncryptionKeyProviderConfigBuilder configEntry(
        KafkaTopicConfigEntry configEntry) {
      this.configEntries.add(configEntry);
      return this;
    }

    public MultiTopicVaultEncryptionKeyProviderConfig build() {
      return new MultiTopicVaultEncryptionKeyProviderConfig(vaultConnectionConfig, configEntries);
    }
  }

  public static class KafkaTopicConfigEntry {

    private final boolean isDefault;
    private final Boolean encryptionEnabled;
    private final String kafkaTopicName;
    private final String kafkaTopicNamePrefix;
    private final VaultPathExpression pathExpression;
    private final String teamName;
    private final String encryptionKeyAttributeName;

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

    public static KafkaTopicConfigEntryBuilder builder() {
      return new KafkaTopicConfigEntryBuilder();
    }

    public boolean isDefault() {
      return isDefault;
    }

    public Boolean encryptionEnabled() {
      return encryptionEnabled;
    }

    public String kafkaTopicName() {
      return kafkaTopicName;
    }

    public String kafkaTopicNamePrefix() {
      return kafkaTopicNamePrefix;
    }

    public VaultPathExpression pathExpression() {
      return pathExpression;
    }

    public String teamName() {
      return teamName;
    }

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

  public static final class KafkaTopicConfigEntryBuilder {

    private boolean isDefault = false;
    private Boolean encryptionEnabled = true;
    private String kafkaTopicName;
    private String kafkaTopicNamePrefix;
    private String vaultPath;
    private String vaultPathTemplate;
    private String teamName;
    private String encryptionKeyAttributeName;

    public KafkaTopicConfigEntryBuilder isDefault(Boolean isDefault) {
      this.isDefault = Objects.requireNonNullElse(isDefault, false);
      return this;
    }

    public KafkaTopicConfigEntryBuilder encryptionEnabled(Boolean encryptionEnabled) {
      this.encryptionEnabled = encryptionEnabled;
      return this;
    }

    public KafkaTopicConfigEntryBuilder kafkaTopicName(String kafkaTopicName) {
      this.kafkaTopicName = kafkaTopicName;
      return this;
    }

    public KafkaTopicConfigEntryBuilder kafkaTopicNamePrefix(String kafkaTopicNamePrefix) {
      this.kafkaTopicNamePrefix = kafkaTopicNamePrefix;
      return this;
    }

    public KafkaTopicConfigEntryBuilder vaultPath(String vaultPath) {
      this.vaultPath = vaultPath;
      return this;
    }

    /**
     * @param vaultPathTemplate the template may contain <code>%TOPICNAME%</code> and/or
     *                          <code>%TEAMNAME%</code>
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder vaultPathTemplate(String vaultPathTemplate) {
      this.vaultPathTemplate = vaultPathTemplate;
      return this;
    }

    public KafkaTopicConfigEntryBuilder teamName(String teamName) {
      this.teamName = teamName;
      return this;
    }

    /**
     * @param encryptionKeyAttributeName name of the encryption key attribute within the vault
     *                                   secret. The default value is <code>encryption_key</code>
     * @return the builder
     */
    public KafkaTopicConfigEntryBuilder encryptionKeyAttributeName(
        String encryptionKeyAttributeName) {
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
      return this;
    }

    public KafkaTopicConfigEntry build() {
      return new KafkaTopicConfigEntry(isDefault, encryptionEnabled, kafkaTopicName,
          kafkaTopicNamePrefix, vaultPath, vaultPathTemplate, teamName, encryptionKeyAttributeName);
    }
  }

  public static final class VaultPathExpression {

    private final String vaultPath;
    private final String vaultPathTemplate;

    public VaultPathExpression(String vaultPath, String vaultPathTemplate) {
      this.vaultPath = vaultPath;
      this.vaultPathTemplate = vaultPathTemplate;
    }

    public String vaultPath() {
      return vaultPath;
    }

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
