package de.otto.messaging.kafka.e2ee.helper;

import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.vault.VaultRuntimeException;
import java.util.ArrayList;
import java.util.List;

public class EncryptionKeyProviderMock implements EncryptionKeyProvider {

  private final List<String> methodCalls = new ArrayList<>();
  private boolean throwException = false;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  @Override
  public KeyVersion retrieveKeyForEncryption(String topic) {
    methodCalls.add("retrieveKeyForEncryption(" + topic + ")");

    if (throwException) {
      throw new VaultRuntimeException("Some Vault Exception");
    }
    return new KeyVersion(3, "aes", "someSecret");
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version) {
    methodCalls.add("retrieveKeyForDecryption(" + topic + ", " + version + ")");

    if (throwException) {
      throw new VaultRuntimeException("Some Vault Exception");
    }
    return "someSecret2";
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version,
      String encryptionKeyAttributeName) {
    methodCalls.add(
        "retrieveKeyForDecryption(" + topic + ", " + version + ", " + encryptionKeyAttributeName
            + ")");

    if (throwException) {
      throw new VaultRuntimeException("Some Vault Exception");
    }
    return "someSecret3";
  }

  public List<String> getMethodCalls() {
    return methodCalls;
  }
}
