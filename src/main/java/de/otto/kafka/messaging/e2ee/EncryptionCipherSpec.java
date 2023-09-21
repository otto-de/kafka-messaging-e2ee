package de.otto.kafka.messaging.e2ee;

import java.util.Objects;

public record EncryptionCipherSpec(
    int keyVersion,
    String cipherName
) {

  public EncryptionCipherSpec {
    Objects.requireNonNull(cipherName, "cipherName must not be null");
  }

  public static EncryptionCipherSpecBuilder builder() {
    return new EncryptionCipherSpecBuilder();
  }

  public static class EncryptionCipherSpecBuilder {

    private int keyVersion;
    private String cipherName;

    public EncryptionCipherSpecBuilder keyVersion(int keyVersion) {
      this.keyVersion = keyVersion;
      return this;
    }

    public EncryptionCipherSpecBuilder cipherName(String cipherName) {
      this.cipherName = cipherName;
      return this;
    }

    public EncryptionCipherSpec build() {
      return new EncryptionCipherSpec(keyVersion, cipherName);
    }
  }
}
