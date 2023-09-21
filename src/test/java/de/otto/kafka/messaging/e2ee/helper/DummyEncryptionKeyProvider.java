package de.otto.kafka.messaging.e2ee.helper;


import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.vault.VaultRuntimeException;
import java.util.Objects;

public class DummyEncryptionKeyProvider implements EncryptionKeyProvider {

  private final KeyVersion keyVersion;

  public DummyEncryptionKeyProvider(String encodedKey, String encryptionKeyAttributeName,
      int keyVersion) {
    this(new KeyVersion(keyVersion, encryptionKeyAttributeName, encodedKey));
  }

  public DummyEncryptionKeyProvider(KeyVersion keyVersion) {
    this.keyVersion = keyVersion;
  }

  @Override
  public KeyVersion retrieveKeyForEncryption(String topic) {
    return keyVersion;
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version,
      String encryptionKeyAttributeName) {
    if (encryptionKeyAttributeName == null) {
      return retrieveKeyForDecryption(topic, version);
    }

    if (keyVersion.version() == version
        && Objects.equals(keyVersion.encryptionKeyAttributeName(), encryptionKeyAttributeName)) {
      return keyVersion.encodedKey();
    }

    throw new VaultRuntimeException("Wrong call arguments: 'version'=" + version
        + " / 'encryptionKeyAttributeName'=" + encryptionKeyAttributeName);
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version) {
    return keyVersion.encodedKey();
  }
}
