package de.otto.kafka.messaging.e2ee;

import java.util.Objects;

/**
 * Simple record to hold the version of the key and the cipher name. It's a helper class for Kafka.
 *
 * @param keyVersion the version of the key within the vault
 * @param cipherName name of the cipher/encryption key within the vault
 */
public record EncryptionCipherSpec(
    int keyVersion,
    String cipherName
) {

  /**
   * Constructor for that class.
   *
   * @param keyVersion the version of the key within the vault
   * @param cipherName name of the cipher/encryption key within the vault
   */
  public EncryptionCipherSpec {
    Objects.requireNonNull(cipherName, "cipherName must not be null");
  }

  /**
   * Creates a builder.
   *
   * @return a EncryptionCipherSpec builder object
   */
  public static EncryptionCipherSpecBuilder builder() {
    return new EncryptionCipherSpecBuilder();
  }

  /**
   * Builder for EncryptionCipherSpec.
   */
  public static class EncryptionCipherSpecBuilder {

    private int keyVersion;
    private String cipherName;

    /**
     * Standard constructor.
     */
    public EncryptionCipherSpecBuilder() {
    }

    /**
     * Sets the key version.
     *
     * @param keyVersion the version of the key within vault
     * @return the builder
     */
    public EncryptionCipherSpecBuilder keyVersion(int keyVersion) {
      this.keyVersion = keyVersion;
      return this;
    }

    /**
     * Sets the cipher name. It's also referred to as the encryption key name.
     *
     * @param cipherName JSON property name of the key within Vault.
     * @return the builder
     */
    public EncryptionCipherSpecBuilder cipherName(String cipherName) {
      this.cipherName = cipherName;
      return this;
    }

    /**
     * Creates the EncryptionCipherSpec.
     *
     * @return the EncryptionCipherSpec
     */
    public EncryptionCipherSpec build() {
      return new EncryptionCipherSpec(keyVersion, cipherName);
    }
  }
}
