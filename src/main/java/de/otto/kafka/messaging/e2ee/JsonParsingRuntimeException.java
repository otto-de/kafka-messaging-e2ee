package de.otto.kafka.messaging.e2ee;

/**
 * Some JSON parsing exception when reading the ciphersText from the Kafka header.
 */
public class JsonParsingRuntimeException extends RuntimeException {

  /**
   * Constructs a JSON parsing runtime exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *                {@link #getMessage()} method.
   */
  public JsonParsingRuntimeException(String message) {
    super(message);
  }
}
