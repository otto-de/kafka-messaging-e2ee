package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import java.util.Objects;

/**
 * Basic vault connection configuration.
 *
 * @param address the URL of the vault
 * @param token   authentification token or <code>null</code>
 * @param appRole app role configuration or <code>null</code>
 */
public record VaultConnectionConfig(
    String address,
    String token,
    VaultAppRole appRole
) {

  private static final String DEFAULT_VAULT_URL = "http://localhost:8200";

  /**
   * Constructor with all fields.
   *
   * @param address the URL of the vault
   * @param token   authentification token or <code>null</code>
   * @param appRole app role configuration or <code>null</code>
   * @see #builder()
   */
  public VaultConnectionConfig(String address, String token, VaultAppRole appRole) {
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

  /**
   * a builder
   *
   * @return a new builder instance
   */
  public static VaultConnectionConfigBuilder builder() {
    return new VaultConnectionConfigBuilder();
  }

  /**
   * Creates a RenewableVault instance based on the current vault configuration.
   *
   * @return the Vault API
   * @throws VaultException on errors related to vault
   */
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

  /**
   * Builder for VaultConnectionConfig
   */
  public static class VaultConnectionConfigBuilder {

    private String address;
    private String token;
    private VaultAppRole appRole;

    /**
     * Default constructor
     */
    public VaultConnectionConfigBuilder() {
    }

    /**
     * Sets the address property.
     *
     * @param address the URL of the vault
     * @return this builder
     */
    public VaultConnectionConfigBuilder address(String address) {
      this.address = address;
      return this;
    }

    /**
     * Sets the token property. Either a token or an appRole must be present.
     *
     * @param token authentification token
     * @return this builder
     */
    public VaultConnectionConfigBuilder token(String token) {
      this.token = token;
      return this;
    }

    /**
     * Sets the appRole property. Either a token or an appRole must be present.
     *
     * @param appRole app role configuration
     * @return this builder
     */
    public VaultConnectionConfigBuilder appRole(VaultAppRole appRole) {
      this.appRole = appRole;
      return this;
    }

    /**
     * Creates a config object.
     *
     * @return the VaultConnectionConfig
     */
    public VaultConnectionConfig build() {
      return new VaultConnectionConfig(address, token, appRole);
    }
  }

  /**
   * Vault app role configuration.
   *
   * @param path     the app role path
   * @param roleid   the app role id
   * @param secretid the app role secret / password
   */
  public record VaultAppRole(
      String path,
      String roleid,
      String secretid
  ) {

    /**
     * Default constructor
     *
     * @param path     the app role path
     * @param roleid   the app role id
     * @param secretid the app role secret / password
     */
    public VaultAppRole {
      Objects.requireNonNull(path, "path is required");
      Objects.requireNonNull(roleid, "roleid is required");
      Objects.requireNonNull(secretid, "secretid is required");
    }

    /**
     * a builder
     *
     * @return a new builder instance
     */
    public static AppRoleBuilder builder() {
      return new AppRoleBuilder();
    }

    /**
     * Builder for VaultAppRole
     */
    public static class AppRoleBuilder {

      private String path;
      private String roleid;
      private String secretid;

      /**
       * Default constructor
       */
      public AppRoleBuilder() {
      }

      /**
       * Sets the path property.
       *
       * @param path the app role path
       * @return this builder
       */
      public AppRoleBuilder path(String path) {
        this.path = path;
        return this;
      }

      /**
       * Sets the roleid property.
       *
       * @param roleid the app role id
       * @return this builder
       */
      public AppRoleBuilder roleid(String roleid) {
        this.roleid = roleid;
        return this;
      }

      /**
       * Sets the secretid property.
       *
       * @param secretid the app role secret / password
       * @return this builder
       */
      public AppRoleBuilder secretid(String secretid) {
        this.secretid = secretid;
        return this;
      }

      /**
       * Creates a app role configuration object.
       *
       * @return the VaultAppRole
       */
      public VaultAppRole build() {
        return new VaultAppRole(path, roleid, secretid);
      }
    }
  }
}
