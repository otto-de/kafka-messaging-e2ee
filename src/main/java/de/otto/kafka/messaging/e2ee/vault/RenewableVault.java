package de.otto.kafka.messaging.e2ee.vault;

import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig.VaultAppRole;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.AuthResponse;
import io.github.jopenlibs.vault.response.LogicalResponse;
import java.time.LocalDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ReadonlyVaultApi which can reconnect to the vault. This is handy when the credentials are
 * expiring.
 */
public final class RenewableVault implements ReadonlyVaultApi {

  private static final Logger log = LoggerFactory.getLogger(RenewableVault.class);

  private final VaultAppRole appRoleConfig;
  private Vault vault;
  private VaultConfig configAuth;
  private boolean isAuthRenewable;
  private LocalDateTime authLeaseValidUntil;

  /**
   * Constructor using App-Role authentication.
   *
   * @param configAuth the configuration
   * @param appRole    the app role name
   */
  public RenewableVault(VaultConfig configAuth, VaultAppRole appRole) {
    Objects.requireNonNull(configAuth, "configAuth is required");
    Objects.requireNonNull(appRole, "appRole is required");
    this.configAuth = configAuth;
    this.vault = Vault.create(configAuth);
    this.isAuthRenewable = true;
    // auth token is not valid and must be renewed at the first usage
    this.authLeaseValidUntil = LocalDateTime.now().minusSeconds(5);
    this.appRoleConfig = appRole;
  }

  /**
   * Constructor using token based authentication.
   *
   * @param configAuth the configuration
   */
  public RenewableVault(VaultConfig configAuth) {
    Objects.requireNonNull(configAuth, "configAuth is required");
    this.configAuth = configAuth;
    this.vault = Vault.create(configAuth);
    this.isAuthRenewable = false;
    // basically forever
    this.authLeaseValidUntil = LocalDateTime.now().plusYears(10);
    this.appRoleConfig = null;
  }

  @Override
  public LogicalResponse read(String path) throws VaultException {
    renewAuthTokenIfNeeded();
    return vault.logical().read(path);
  }

  @Override
  public LogicalResponse read(String path, int version) throws VaultException {
    renewAuthTokenIfNeeded();
    return vault.logical().read(path, true, version);
  }

  private void renewAuthTokenIfNeeded() throws VaultException {
    if (isAuthRenewable && LocalDateTime.now().isAfter(authLeaseValidUntil)) {
      log.debug("Try to renew vault auth token ..");
      // renew vault
      AuthResponse authResponse = vault.auth()
          .loginByAppRole(appRoleConfig.path(), appRoleConfig.roleid(), appRoleConfig.secretid());
      configAuth = configAuth.token(authResponse.getAuthClientToken());
      vault = Vault.create(configAuth);
      isAuthRenewable = authResponse.isAuthRenewable();
      authLeaseValidUntil = LocalDateTime.now().plusSeconds(authResponse.getAuthLeaseDuration());
      log.debug("new vault auth token is valid until {}", authLeaseValidUntil);
    }
  }
}
