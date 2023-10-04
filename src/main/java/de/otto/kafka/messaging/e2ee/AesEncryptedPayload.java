package de.otto.kafka.messaging.e2ee;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider.KeyVersion;
import java.util.Base64;
import java.util.Objects;

/**
 * record to hold all the data needed for an encrypted payload. But it can also hold an unencrypted
 * payload.
 *
 * @see #isEncrypted()
 */
public final class AesEncryptedPayload {

  private final byte[] encryptedPayload;
  private final byte[] initializationVector;
  private final int keyVersion;
  private final String encryptionKeyAttributeName;

  /**
   * @param plainPayload the plain text as byte array.
   */
  public AesEncryptedPayload(byte[] plainPayload) {
    this.encryptedPayload = plainPayload;
    this.initializationVector = null;
    this.keyVersion = 0;
    this.encryptionKeyAttributeName = null;
  }

  /**
   * @param encryptedPayload     an encrypted payload as byte array
   * @param initializationVector the raw initialization vector
   * @param keyVersion           the vault version of the encryption key entry
   */
  public AesEncryptedPayload(byte[] encryptedPayload, byte[] initializationVector, int keyVersion) {
    this(encryptedPayload, initializationVector, keyVersion, null);
  }

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVector       the raw initialization vector
   * @param keyVersion                 the vault version of the encryption key entry
   * @param encryptionKeyAttributeName JSON property name of the key within Vault. Can be
   *                                   <code>null</code> for Field-Level-Encryption.
   */
  public AesEncryptedPayload(byte[] encryptedPayload, byte[] initializationVector, int keyVersion,
      String encryptionKeyAttributeName) {
    Objects.requireNonNull(encryptedPayload, "encryptedPayload must not be null");
    Objects.requireNonNull(initializationVector, "initializationVector must not be null");
    this.encryptedPayload = encryptedPayload;
    this.initializationVector = initializationVector;
    this.keyVersion = keyVersion;
    this.encryptionKeyAttributeName = encryptionKeyAttributeName;
  }

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVectorBase64 the initialization vector base64 encoded
   * @param keyVersion                 the vault version of the encryption key entry
   */
  public AesEncryptedPayload(byte[] encryptedPayload, String initializationVectorBase64,
      int keyVersion) {
    this(encryptedPayload, initializationVectorBase64, keyVersion, null);
  }

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVectorBase64 the initialization vector base64 encoded
   * @param keyVersion                 the vault version of the encryption key entry
   * @param encryptionKeyAttributeName JSON property name of the key within Vault. Can be
   *                                   <code>null</code> for Field-Level-Encryption.
   */
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

  /**
   * @param plainPayload the plain payload as byte array
   * @return an AesEncryptedPayload of an unencrypted payload
   */
  public static AesEncryptedPayload ofUnencryptedPayload(byte[] plainPayload) {
    return new AesEncryptedPayload(plainPayload);
  }

  /**
   * @param encryptedPayload     an encrypted payload as byte array
   * @param initializationVector the raw initialization vector
   * @param keyVersion           the vault data for the encryption key
   * @return an AesEncryptedPayload of an encrypted payload
   */
  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      byte[] initializationVector,
      KeyVersion keyVersion) {
    return new AesEncryptedPayload(encryptedPayload, initializationVector,
        keyVersion.version(),
        keyVersion.encryptionKeyAttributeName());
  }

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVectorBase64 the initialization vector base64 encoded
   * @param keyVersion                 the vault metadata for the encryption key
   * @return an AesEncryptedPayload of an encrypted payload
   */
  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      String initializationVectorBase64,
      int keyVersion) {
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64, keyVersion);
  }

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVectorBase64 the initialization vector base64 encoded
   * @param keyVersion                 the vault version of the encryption key entry
   * @param encryptionKeyAttributeName JSON property name of the key within Vault. Can be
   *                                   <code>null</code> for Field-Level-Encryption.
   * @return an AesEncryptedPayload of an encrypted payload
   */
  public static AesEncryptedPayload ofEncryptedPayload(
      byte[] encryptedPayload,
      String initializationVectorBase64,
      int keyVersion,
      String encryptionKeyAttributeName) {
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64, keyVersion,
        encryptionKeyAttributeName);
  }

  /**
   * @param encryptedPayload     an encrypted payload as byte array
   * @param initializationVector the raw initialization vector
   * @param cipherSpec           the vault metadata for the encryption key
   * @return an AesEncryptedPayload of an encrypted payload
   */
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

  /**
   * @param encryptedPayload           an encrypted payload as byte array
   * @param initializationVectorBase64 the initialization vector base64 encoded
   * @param cipherSpec                 the vault metadata for the encryption key
   * @return an AesEncryptedPayload of an encrypted payload
   */
  public static AesEncryptedPayload ofEncryptedPayload(byte[] encryptedPayload,
      String initializationVectorBase64, EncryptionCipherSpec cipherSpec) {
    if (cipherSpec == null) {
      return ofUnencryptedPayload(encryptedPayload);
    }
    return new AesEncryptedPayload(encryptedPayload, initializationVectorBase64,
        cipherSpec.keyVersion(), cipherSpec.cipherName());
  }

  /**
   * @return <code>true</code> when this object holds an encrypted value. <code>false</code> when
   * this object hold an unencrypted value.
   */
  public boolean isEncrypted() {
    return initializationVector != null
        && initializationVector.length > 0
        && keyVersion > 0;
  }

  /**
   * @return the value - which might is encrypted
   * @see #isEncrypted()
   */
  public byte[] encryptedPayload() {
    return encryptedPayload;
  }

  /**
   * @return the raw initialization vector or <code>null</code> when the value is encrypted
   * @see #isEncrypted()
   */
  public byte[] initializationVector() {
    return initializationVector;
  }

  /**
   * @return the initialization vector base64 encoded or <code>null</code> when the value is
   * encrypted
   * @see #isEncrypted()
   */
  public String initializationVectorBase64() {
    if (initializationVector == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(initializationVector);
  }

  /**
   * @return the vault version of the encryption key entry
   */
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
