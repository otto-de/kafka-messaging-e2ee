package de.otto.messaging.kafka.e2ee;

import static de.otto.messaging.kafka.e2ee.DefaultAesEncryptionConfiguration.CACHING_DURATION;
import static de.otto.messaging.kafka.e2ee.DefaultAesEncryptionConfiguration.decrypt;
import static de.otto.messaging.kafka.e2ee.vault.VaultHelper.decodeBase64Key;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;

public class DecryptionService {

  private final EncryptionKeyProvider encryptionKeyProvider;
  private final Cache<TopicKeyVersion, Key> aesKeyCache;

  public DecryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    Objects.requireNonNull(encryptionKeyProvider, "encryptionKeyProvider");
    this.encryptionKeyProvider = encryptionKeyProvider;
    this.aesKeyCache = new Cache<>(CACHING_DURATION);
  }

  /**
   * decrypts the given payload (depending on the content).
   *
   * @param kafkaTopicName   name of the Kafka Topic the field value is from.
   * @param encryptedPayload the (potentially) encrypted payload.
   * @return The plain text payload
   */
  public byte[] decryptToByteArray(String kafkaTopicName, AesEncryptedPayload encryptedPayload) {
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName must not be null");
    Objects.requireNonNull(encryptedPayload, "encryptedPayload must not be null");

    if (!encryptedPayload.isEncrypted()) {
      // payload is not encrypted
      return encryptedPayload.encryptedPayload();
    }

    // retrieve AES key
    TopicKeyVersion topicKeyVersion = new TopicKeyVersion(kafkaTopicName,
        encryptedPayload.keyVersion(), encryptedPayload.encryptionKeyAttributeName());
    Key aesKey = aesKeyCache.getOrRetrieve(topicKeyVersion, this::createAesKey);

    // run decryption
    byte[] iv = encryptedPayload.initializationVector();
    byte[] encryptedData = encryptedPayload.encryptedPayload();
    return decrypt(encryptedData, aesKey, iv);
  }

  /**
   * decrypts the given payload (depending on the content).
   *
   * @param kafkaTopicName   name of the Kafka Topic the field value is from.
   * @param encryptedPayload the (potentially) encrypted payload.
   * @return The plain text
   */
  public String decryptToString(String kafkaTopicName, AesEncryptedPayload encryptedPayload) {
    return new String(decryptToByteArray(kafkaTopicName, encryptedPayload), StandardCharsets.UTF_8);
  }

  private Key createAesKey(TopicKeyVersion topicKeyVersion) {
    String topic = topicKeyVersion.topic();
    int keyVersionNumber = topicKeyVersion.keyVersionNumber();
    String encryptionKeyAttributeName = topicKeyVersion.encryptionKeyAttributeName();
    String base64Key;
    if (topicKeyVersion.encryptionKeyAttributeName() == null) {
      // we don't have a encryptionKeyAttributeName, let the encryptionKeyProvider figure it out
      base64Key = encryptionKeyProvider.retrieveKeyForDecryption(topic, keyVersionNumber);
    } else {
      base64Key = encryptionKeyProvider.retrieveKeyForDecryption(topic, keyVersionNumber,
          encryptionKeyAttributeName);
    }

    byte[] key = decodeBase64Key(base64Key);
    return new SecretKeySpec(key, "AES");
  }

  private record TopicKeyVersion(
      String topic,
      int keyVersionNumber,
      /* can be null for Field-Level-Decryption */
      String encryptionKeyAttributeName
  ) {

    private TopicKeyVersion {
      Objects.requireNonNull(topic);
    }
  }
}
