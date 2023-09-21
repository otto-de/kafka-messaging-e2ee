package de.otto.messaging.kafka.e2ee;

public interface InitializationVectorFactory {

  /**
   * @return a new random initialization vector
   */
  byte[] generateInitializationVector();
}
