package de.otto.kafka.messaging.e2ee.vault;

/**
 * Some vault configuration error occurred.
 */
public class VaultConfigException extends RuntimeException {

  /**
   * Constructs a new vault configuration runtime exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *                {@link #getMessage()} method.
   */
  public VaultConfigException(String message) {
    super(message);
  }
}
