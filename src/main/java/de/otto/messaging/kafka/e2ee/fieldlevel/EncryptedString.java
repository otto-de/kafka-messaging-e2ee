package de.otto.messaging.kafka.e2ee.fieldlevel;

/**
 * A simple wrapper class that marks a string as (potentially) encrypted.
 */
public record EncryptedString(String value) {

  /**
   * @param value the (potentially) encrypted text. It can be <code>null</code>.
   * @return an instance of <code>EncryptedString</code> or <code>null</code>.
   */
  public static EncryptedString of(String value) {
    if (value == null) {
      return null;
    }
    return new EncryptedString(value);
  }
}
