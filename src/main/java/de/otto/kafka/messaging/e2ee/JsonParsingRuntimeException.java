package de.otto.kafka.messaging.e2ee;

/**
 * Some JSON parsing exception when reading the ciphersText from the Kafka header.
 */
public class JsonParsingRuntimeException extends RuntimeException {

  /**
   * @param msg an error message
   */
  public JsonParsingRuntimeException(String msg) {
    super(msg);
  }
}
