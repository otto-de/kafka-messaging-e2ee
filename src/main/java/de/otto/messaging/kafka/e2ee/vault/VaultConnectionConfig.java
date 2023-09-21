package de.otto.messaging.kafka.e2ee.vault;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import java.util.Objects;

public record VaultConnectionConfig(
    String address,
    String token,
    VaultAppRole appRole
) {

  private static final String DEFAULT_VAULT_URL = "http://localhost:8200";

  public VaultConnectionConfig(String address, String token,
      VaultAppRole appRole) {
    if (appRole != null && token != null) {
      throw new VaultConfigException("Vault token must not be configured when appRole is provided");
    }
    if (appRole == null && token == null) {
      throw new VaultConfigException("Neither vault token nor appRole is provided");
    }

    this.address = Objects.requireNonNullElse(address, DEFAULT_VAULT_URL);
    this.token = token;
    this.appRole = appRole;
  }

  public static VaultConnectionConfigBuilder builder() {
    return new VaultConnectionConfigBuilder();
  }

  public RenewableVault createRenewableVault() throws VaultException {
    VaultConfig config = new VaultConfig()
        .address(address)
        .token(token)
        // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .openTimeout(5)
        // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .readTimeout(30)
        // we use KV Secrets Engine version 2 (that is the latest one)
        .engineVersion(2)
        .build();

    if (token != null) {
      return new RenewableVault(config);
    }

    return new RenewableVault(config, appRole);
  }

  public static class VaultConnectionConfigBuilder {

    private String address;
    private String token;
    private VaultAppRole appRole;

    public VaultConnectionConfigBuilder address(String address) {
      this.address = address;
      return this;
    }

    public VaultConnectionConfigBuilder token(String token) {
      this.token = token;
      return this;
    }

    public VaultConnectionConfigBuilder appRole(
        VaultAppRole appRole) {
      this.appRole = appRole;
      return this;
    }

    public VaultConnectionConfig build() {
      return new VaultConnectionConfig(address, token, appRole);
    }
  }

  public record VaultAppRole(
      String path,
      String roleid,
      String secretid
  ) {

    public VaultAppRole {
      Objects.requireNonNull(path, "path is required");
      Objects.requireNonNull(roleid, "roleid is required");
      Objects.requireNonNull(secretid, "secretid is required");
    }

    public static AppRoleBuilder builder() {
      return new AppRoleBuilder();
    }

    public static class AppRoleBuilder {

      private String path;
      private String roleid;
      private String secretid;

      public AppRoleBuilder path(String path) {
        this.path = path;
        return this;
      }

      public AppRoleBuilder roleid(String roleid) {
        this.roleid = roleid;
        return this;
      }

      public AppRoleBuilder secretid(String secretid) {
        this.secretid = secretid;
        return this;
      }

      public VaultAppRole build() {
        return new VaultAppRole(path, roleid, secretid);
      }
    }
  }
}
