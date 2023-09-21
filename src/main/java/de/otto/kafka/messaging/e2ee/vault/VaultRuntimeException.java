package de.otto.kafka.messaging.e2ee.vault;

public class VaultRuntimeException extends RuntimeException {

  public VaultRuntimeException(Exception ex) {
    super(ex);
  }

  public VaultRuntimeException(String msg) {
    super(msg);
  }
}