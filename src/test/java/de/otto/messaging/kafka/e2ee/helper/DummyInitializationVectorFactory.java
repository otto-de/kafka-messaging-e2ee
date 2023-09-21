package de.otto.messaging.kafka.e2ee.helper;

import de.otto.messaging.kafka.e2ee.InitializationVectorFactory;
import java.util.Base64;

public class DummyInitializationVectorFactory implements InitializationVectorFactory {

  private final byte[] initializationVector;

  public DummyInitializationVectorFactory(byte[] initializationVector) {
    this.initializationVector = initializationVector;
  }

  public DummyInitializationVectorFactory(String initializationVectorBase64) {
    this(Base64.getDecoder().decode(initializationVectorBase64));
  }

  @Override
  public byte[] generateInitializationVector() {
    return initializationVector;
  }
}
