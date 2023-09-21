package de.otto.messaging.kafka.e2ee;

import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider.KeyVersion;
import java.util.Base64;
import java.util.Objects;

public class AesEncryptedPayload {

  private final byte[] encryptedPayload;
  private final byte[] initializationVector;
  private final int keyVersion;
  private final String encryptionKeyAttributeName;

  public AesEncryptedPayload(byte[] plainPayload) {
    this.encryptedPayload = plainPayload;
    this.initializationVector = null;
    this.keyVersion = 0;
    this.encryptionKeyAttributeName = null;
  }

  public AesEncryptedPayload(byte[] encryptedPayload, byte[] initializationVector, int keyVersion) {
    this(encryptedPayload, initializationVector, keyVersion, null);
  }

  public AesEncryptedPayload(byte[] encryptedPayload, byte[] initializationVector, int keyVersion,
      String encryptionKeyAttributeName) {
    Objects.requireNonNull(encryptedPayload, "encryptedPayload must not be null");
    Objects.requireNonNull(initializationVector, "initializationVector must not be null");
    this.encryptedPayload = encryptedPayload;
    this.initializationVector = initializationVector;
    this.keyVersion = keyVersion;
    this.encryptionKeyAttributeName = encryptionKeyAttributeName;
  }

  public AesEncryptedPayload(byte[] encryptedPayload, String initializationVectorBase64,
      int keyVersion) {
    this(encryptedPayload, initializationVectorBase64, keyVersion, null);
  }

  public AesEncryptedPayload(byte[] encryptedPayload, String initializationVectorBase64,
      int keyVersion, String encryptionKeyAttributeName) {
    Objects.requireNonNull(encryptedPayload, "encryptedPayload must not be null");
    Objects.requireNonNull(initializationVectorBase64,
        "initializationVectorBase64 must not be null");
    this.encryptedPayload = encryptedPayload;
    this.initializationVector = Base64.getDecoder().decode(initializationVectorBase64);
    this.keyVersion = keyVersion;
    this.encryptionKeyAttributeName = encryptionKeyAttributeName;
  }

  public static AesEncryptedPayload ofUnencryptedPayload(byte[] plainPayload) {
    return new AesEncryptedPayload(plainPayload);
  }

  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      byte[] initializationVector,
      KeyVersion keyVersion) {
    return new AesEncryptedPayload(encryptedPayload, initializationVector,
        keyVersion.version(),
        keyVersion.encryptionKeyAttributeName());
  }

  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      String initializationVectorBase64,
      int keyVersion) {
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64, keyVersion);
  }

  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      String initializationVectorBase64,
      int keyVersion,
      String encryptionKeyAttributeName) {
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64, keyVersion,
        encryptionKeyAttributeName);
  }

  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      byte[] initializationVector,
      EncryptionCipherSpec cipherSpec) {
    if (cipherSpec == null) {
      return ofUnencryptedPayload(encryptedPayload);
    }
    return new AesEncryptedPayload(encryptedPayload, initializationVector,
        cipherSpec.keyVersion(), cipherSpec.cipherName());
  }

  public static AesEncryptedPayload ofEncryptedPayload(byte[] encryptedPayload,
      String initializationVectorBase64, EncryptionCipherSpec cipherSpec) {
    if (cipherSpec == null) {
      return ofUnencryptedPayload(encryptedPayload);
    }
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64,
        cipherSpec.keyVersion(), cipherSpec.cipherName());
  }

  public boolean isEncrypted() {
    return initializationVector != null
        && initializationVector.length > 0
        && keyVersion > 0;
  }

  public byte[] encryptedPayload() {
    return encryptedPayload;
  }

  public byte[] initializationVector() {
    return initializationVector;
  }

  public String initializationVectorBase64() {
    if (initializationVector == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(initializationVector);
  }

  public int keyVersion() {
    return keyVersion;
  }

  /**
   * @return name of the encryption key property within the vault. When <code>null</code>, then the
   * default value must be used.
   */
  public String encryptionKeyAttributeName() {
    return encryptionKeyAttributeName;
  }

  @Override
  public String toString() {
    if (isEncrypted()) {
      return "AesEncryptedPayload{" +
          "encryptedPayload=" + Base64.getEncoder().encodeToString(encryptedPayload) +
          ", initializationVector=" + Base64.getEncoder().encodeToString(initializationVector) +
          ", keyVersion=" + keyVersion +
          ", encryptionKeyAttributeName=" + encryptionKeyAttributeName +
          '}';
    }

    return "AesEncryptedPayload{" +
        "payload=" + Base64.getEncoder().encodeToString(encryptedPayload) +
        '}';
  }
}
