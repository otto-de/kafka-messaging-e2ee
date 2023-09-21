package de.otto.messaging.kafka.e2ee.vault;

public class VaultRuntimeException extends RuntimeException {

  public VaultRuntimeException(Exception ex) {
    super(ex);
  }

  public VaultRuntimeException(String msg) {
    super(msg);
  }
}