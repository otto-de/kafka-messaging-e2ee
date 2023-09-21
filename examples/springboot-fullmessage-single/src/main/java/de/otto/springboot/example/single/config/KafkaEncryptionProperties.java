package de.otto.springboot.example.single.config;

import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig.VaultAppRole;
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

  public VaultConnectionConfig vaultConnectionConfig() {
    return VaultConnectionConfig.builder()
        .address(address)
        .token(token)
        .appRole(approle == null ? null : approle.mapToVaultAppRole())
        .build();
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
}
