package de.otto.kafka.messaging.e2ee;

import java.util.Objects;

/**
 * This is the central interface for the vault access.
 */
public interface EncryptionKeyProvider {

  /**
   * Retrieves a key for encrypting a new message.
   *
   * @param topic the name of the topic to encrypt
   * @return a key for encryption or <code>null</code> if encryption is not needed
   */
  KeyVersion retrieveKeyForEncryption(String topic);

  /**
   * Retrieves the key for decrypting an encrypted message. The encryption key name is unknown, so
   * the method will try to figure that out by itself.
   *
   * @param topic   the name of the topic to decrypt
   * @param version the version of the key
   * @return base64 and URL-Encoded encoded key
   */
  String retrieveKeyForDecryption(String topic, int version);

  /**
   * Retrieves the key for decrypting an encrypted message.
   *
   * @param topic                      the name of the topic to decrypt
   * @param version                    the version of the key
   * @param encryptionKeyAttributeName the name of the encryption key within the vault. When
   *                                   <code>null</code> then the default value must be used.
   * @return base64 and URL-Encoded encoded key
   */
  String retrieveKeyForDecryption(String topic, int version, String encryptionKeyAttributeName);

  /**
   * Checks if the given topic shall be encrypted or not.
   *
   * @param kafkaTopicName the name of the topic
   * @return <code>true</code> when the topic can contain encrypted payloads
   */
  default boolean isEncryptedTopic(String kafkaTopicName) {
    throw new UnsupportedOperationException(
        "This method is not implemented by " + getClass().getName());
  }

  /**
   * base64 and URL-Encoded encoded AES key
   */
  final class KeyVersion {

    private final int version;
    private final String encryptionKeyAttributeName;
    private final String encodedKey;

    /**
     * Creates a key version object without the encryptionKeyAttributeName. This is used for
     * Field-Level-Encryption.
     *
     * @param version    the version of the Vault entry
     * @param encodedKey the value of the key within Vault. The format is base64 with probably some
     *                   CR and/or LF characters at the end.
     */
    public KeyVersion(int version, String encodedKey) {
      Objects.requireNonNull(encodedKey);
      this.version = version;
      this.encryptionKeyAttributeName = null;
      this.encodedKey = encodedKey;
    }

    /**
     * Constructor for that class.
     *
     * @param version                    the version of the Vault entry
     * @param encryptionKeyAttributeName JSON property name of the key within Vault.
     * @param encodedKey                 the value of the key within Vault. The format is base64
     *                                   with probably some CR and/or LF characters at the end.
     */
    public KeyVersion(int version, String encryptionKeyAttributeName, String encodedKey) {
      Objects.requireNonNull(encodedKey);
      this.version = version;
      this.encryptionKeyAttributeName = encryptionKeyAttributeName;
      this.encodedKey = encodedKey;
    }

    /**
     * Gets the key version.
     *
     * @return the version of the Vault entry
     */
    public int version() {
      return version;
    }

    /**
     * Gets the encryption key name.
     *
     * @return JSON property name of the key within Vault. It can be <code>null</code> for
     * Field-Level-Encryption.
     */
    public String encryptionKeyAttributeName() {
      return encryptionKeyAttributeName;
    }

    /**
     * Gets the encoded key base64 and URL encoded.
     *
     * @return the value of the key within Vault. The format is base64 with probably some CR and/or
     * LF characters at the end.
     */
    public String encodedKey() {
      return encodedKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      KeyVersion that = (KeyVersion) o;
      return version == that.version
          && Objects.equals(encryptionKeyAttributeName, that.encryptionKeyAttributeName)
          && Objects.equals(encodedKey, that.encodedKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(version, encryptionKeyAttributeName, encodedKey);
    }
  }
}
