package de.otto.kafka.messaging.e2ee;

import static de.otto.kafka.messaging.e2ee.DefaultAesEncryptionConfiguration.GCM_IV_SIZE;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Standard implementation for InitializationVectorFactory
 */
public final class SecureRandomInitializationVectorFactory implements InitializationVectorFactory {

  /**
   * Default constructor
   */
  public SecureRandomInitializationVectorFactory() {
  }

  @Override
  public byte[] generateInitializationVector() {
    byte[] iv = new byte[GCM_IV_SIZE];
    Random random = new SecureRandom();
    random.nextBytes(iv);
    return iv;
  }
}
