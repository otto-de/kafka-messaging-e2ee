package de.otto.messaging.kafka.e2ee;

import static de.otto.messaging.kafka.e2ee.DefaultAesEncryptionConfiguration.GCM_IV_SIZE;

import java.security.SecureRandom;
import java.util.Random;

public class SecureRandomInitializationVectorFactory implements InitializationVectorFactory {

  @Override
  public byte[] generateInitializationVector() {
    byte[] iv = new byte[GCM_IV_SIZE];
    Random random = new SecureRandom();
    random.nextBytes(iv);
    return iv;
  }
}
