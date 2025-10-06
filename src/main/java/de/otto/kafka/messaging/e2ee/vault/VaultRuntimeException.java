package de.otto.kafka.messaging.e2ee.vault;

/**
 * General vault exception
 */
public class VaultRuntimeException extends RuntimeException {

  /**
   * Constructs a new vault runtime exception with the specified cause.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *              (A {@code null} value is permitted, and indicates that the cause is nonexistent or
   *              unknown.)
   */
  public VaultRuntimeException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs a new vault runtime exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *                {@link #getMessage()} method.
   */
  public VaultRuntimeException(String message) {
    super(message);
  }
}