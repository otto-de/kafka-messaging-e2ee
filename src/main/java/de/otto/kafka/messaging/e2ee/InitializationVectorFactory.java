package de.otto.kafka.messaging.e2ee;

/**
 * Factory to create initialization vectors for encrypting messages or field values.
 */
public interface InitializationVectorFactory {

  /**
   * Creates a new initialization vector for encrypting a message or field.
   *
   * @return a new random initialization vector
   */
  byte[] generateInitializationVector();
}
