package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LogicalResponse;

/**
 * Vault API which supports read operations only
 */
public interface ReadonlyVaultApi {

  /**
   * <p>Basic read operation to retrieve a secret.  A single secret key can map to multiple
   * name-value pairs, which can be retrieved from the response object.  E.g.:</p>
   *
   * <blockquote>
   * <pre>{@code
   * final LogicalResponse response = vault.read("secret/hello");
   *
   * final String value = response.getData().get("value");
   * final String otherValue = response.getData().get("other_value");
   * }</pre>
   * </blockquote>
   *
   * @param path The Vault key value from which to read (e.g. <code>secret/hello</code>)
   * @return The response information returned from Vault
   * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code,
   *                        invalid JSON payload, etc), and the maximum number of retries is
   *                        exceeded.
   */
  LogicalResponse read(String path) throws VaultException;

  /**
   * <p>Basic read operation to retrieve a specified secret version for KV engine version 2. A
   * single secret key version can map to multiple name-value pairs, which can be retrieved from the
   * response object. E.g.:</p>
   *
   * <blockquote>
   * <pre>{@code
   * final LogicalResponse response = vault.read("secret/hello", 21);
   *
   * final String value = response.getData().get("value");
   * final String otherValue = response.getData().get("other_value");
   * }</pre>
   * </blockquote>
   *
   * @param path    The Vault key value from which to read (e.g. <code>secret/hello</code>
   * @param version The Integer version number of the secret to read, e.g. "21"
   * @return The response information returned from Vault
   * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code,
   *                        invalid JSON payload, etc), and the maximum number of retries is
   *                        exceeded.
   */
  LogicalResponse read(String path, int version) throws VaultException;
}
