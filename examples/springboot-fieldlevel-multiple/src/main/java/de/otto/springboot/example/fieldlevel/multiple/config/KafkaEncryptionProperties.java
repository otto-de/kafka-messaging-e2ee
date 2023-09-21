package de.otto.springboot.example.fieldlevel.multiple.config;

import de.otto.kafka.messaging.e2ee.vault.MultiTopicVaultEncryptionKeyProviderConfig.KafkaTopicConfigEntry;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig.VaultAppRole;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "hashicorp", ignoreInvalidFields = true)
public class KafkaEncryptionProperties {

  private String address;
  private String token;
  private AppRoleProperties approle;
  private List<ConfigRuleEntry> rules;

  public VaultConnectionConfig vaultConnectionConfig() {
    return VaultConnectionConfig.builder()
        .address(address)
        .token(token)
        .appRole(approle == null ? null : approle.mapToVaultAppRole())
        .build();
  }

  public List<KafkaTopicConfigEntry> kafkaTopicConfigEntries() {
    if (rules == null) {
      return List.of();
    }
    return rules.stream()
        .map(ConfigRuleEntry::mapToKafkaTopicConfigEntry)
        .toList();
  }

  @Setter
  @Getter
  public static class AppRoleProperties {

    private String roleid;
    private String secretid;
    private String path;

    public VaultAppRole mapToVaultAppRole() {
      return VaultAppRole.builder()
          .roleid(roleid)
          .secretid(secretid)
          .path(path)
          .build();
    }
  }

  @Setter
  @Getter
  public static class ConfigRuleEntry {

    private Boolean defaultRule;
    private Boolean encryptionEnabled;
    private String pathPattern;
    private String topicname;
    private String teamname;

    public KafkaTopicConfigEntry mapToKafkaTopicConfigEntry() {
      return KafkaTopicConfigEntry.builder()
          .isDefault(defaultRule)
          .kafkaTopicName(topicname)
          .vaultPathTemplate(pathPattern)
          .encryptionEnabled(encryptionEnabled)
          .teamName(teamname)
          .build();
    }
  }
}
