package de.otto.kafka.messaging.e2ee;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.Duration;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

interface DefaultAesEncryptionConfiguration {

  String CIPHER_TYPE = "AES/GCM/NoPadding";
  /**
   * length of the AES initialization vector in bytes
   */
  int GCM_IV_SIZE = 12;
  /**
   * the authentication tag length (in bits)
   */
  int GCM_TAG_SIZE = 128;

  /**
   * duration of the cache content
   */
  Duration CACHING_DURATION = Duration.ofMinutes(60);

  static byte[] encrypt(byte[] plainValue, Key aesKey, byte[] iv) {
    try {
      var cipher = Cipher.getInstance(CIPHER_TYPE);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_SIZE, iv));
      return cipher.doFinal(plainValue);
    } catch (GeneralSecurityException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  static byte[] decrypt(byte[] encryptedValue, Key aesKey, byte[] iv) {
    try {
      var cipher = Cipher.getInstance(CIPHER_TYPE);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_SIZE, iv));
      return cipher.doFinal(encryptedValue);
    } catch (GeneralSecurityException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}