package de.otto.kafka.messaging.e2ee;

import java.util.Objects;

/**
 * Simple record to hold the version of the key and the cipher name. It's a helper class for Kafka.
 */
public record EncryptionCipherSpec(
    int keyVersion,
    String cipherName
) {

  public EncryptionCipherSpec {
    Objects.requireNonNull(cipherName, "cipherName must not be null");
  }

  /**
   * @return a EncryptionCipherSpec builder object
   */
  public static EncryptionCipherSpecBuilder builder() {
    return new EncryptionCipherSpecBuilder();
  }

  public static class EncryptionCipherSpecBuilder {

    private int keyVersion;
    private String cipherName;

    /**
     * @param keyVersion the version of the key within vault
     * @return the builder
     */
    public EncryptionCipherSpecBuilder keyVersion(int keyVersion) {
      this.keyVersion = keyVersion;
      return this;
    }

    /**
     * @param cipherName JSON property name of the key within Vault.
     * @return the builder
     */
    public EncryptionCipherSpecBuilder cipherName(String cipherName) {
      this.cipherName = cipherName;
      return this;
    }

    /**
     * @return the EncryptionCipherSpec
     */
    public EncryptionCipherSpec build() {
      return new EncryptionCipherSpec(keyVersion, cipherName);
    }
  }
}
