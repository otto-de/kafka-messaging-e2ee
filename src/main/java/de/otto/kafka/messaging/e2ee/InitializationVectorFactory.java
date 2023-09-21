package de.otto.kafka.messaging.e2ee;

public interface InitializationVectorFactory {

  /**
   * @return a new random initialization vector
   */
  byte[] generateInitializationVector();
}
