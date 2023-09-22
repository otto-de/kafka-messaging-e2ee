package de.otto.kafka.messaging.e2ee.fieldlevel;

import static de.otto.kafka.messaging.e2ee.fieldlevel.DefaultFieldLevelEncryptionConfiguration.AES_V1_PREFIX;
import static de.otto.kafka.messaging.e2ee.fieldlevel.DefaultFieldLevelEncryptionConfiguration.FIELD_DELIMITER;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;

public final class FieldLevelDecryptionService {

  private final DecryptionService decryptionService;

  public FieldLevelDecryptionService(DecryptionService decryptionService) {
    Objects.requireNonNull(decryptionService, "decryptionService");
    this.decryptionService = decryptionService;
  }

  public FieldLevelDecryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    this(new DecryptionService(encryptionKeyProvider));
  }

  /**
   * decrypts the given text (depending on the content).
   *
   * @param kafkaTopicName  name of the Kafka Topic the field value is from.
   * @param encryptedString the (potentially) encrypted text value. Can be <code>null</code>.
   * @return The plain text or <code>null</code>
   */
  public String decryptFieldValue(String kafkaTopicName, String encryptedString) {
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName");
    if (encryptedString == null) {
      return null;
    }

    if (!encryptedString.startsWith(AES_V1_PREFIX + FIELD_DELIMITER)) {
      return encryptedString;
    }

    String[] cryptoParts = encryptedString.split(Pattern.quote(FIELD_DELIMITER));
    if (cryptoParts.length != 4) {
      throw new IllegalArgumentException("encrypted string format is invalid");
    }

    int keyVersion;
    try {
      keyVersion = Integer.parseInt(cryptoParts[1]);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("keyVersion must be a number", ex);
    }
    String initializationVectorBase64 = cryptoParts[2];
    byte[] encryptedPayload = Base64.getDecoder().decode(cryptoParts[3]);

    AesEncryptedPayload aesEncryptedPayload = new AesEncryptedPayload(encryptedPayload,
        initializationVectorBase64, keyVersion);

    return decryptionService.decryptToString(kafkaTopicName, aesEncryptedPayload);
  }

  /**
   * decrypts the given text (depending on the content).
   *
   * @param kafkaTopicName  name of the Kafka Topic the field value is from.
   * @param encryptedString the (potentially) encrypted text value. Can be <code>null</code>.
   * @return The plain text or <code>null</code>
   */
  public String decryptFieldValue(String kafkaTopicName, EncryptedString encryptedString) {
    if (encryptedString == null || encryptedString.value() == null) {
      return null;
    }
    return decryptFieldValue(kafkaTopicName, encryptedString.value());
  }
}
