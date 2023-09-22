package de.otto.kafka.messaging.e2ee;

import static de.otto.kafka.messaging.e2ee.DefaultAesEncryptionConfiguration.CACHING_DURATION;
import static de.otto.kafka.messaging.e2ee.DefaultAesEncryptionConfiguration.encrypt;
import static de.otto.kafka.messaging.e2ee.vault.VaultHelper.decodeBase64Key;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider.KeyVersion;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptionService {

  private final EncryptionKeyProvider encryptionKeyProvider;
  private final InitializationVectorFactory initializationVectorFactory;
  private final Cache<String, EncryptionKeyData> encryptionKeyDataCache;

  /**
   * This constructor should be used in production.
   *
   * @param encryptionKeyProvider a EncryptionKeyProvider
   */
  public EncryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    this(encryptionKeyProvider, new SecureRandomInitializationVectorFactory());
  }

  /**
   * This constructor should be used in unit tests only.
   *
   * @param encryptionKeyProvider       a EncryptionKeyProvider
   * @param initializationVectorFactory a test InitializationVectorFactory
   */
  public EncryptionService(EncryptionKeyProvider encryptionKeyProvider,
      InitializationVectorFactory initializationVectorFactory) {
    Objects.requireNonNull(encryptionKeyProvider, "encryptionKeyProvider");
    Objects.requireNonNull(initializationVectorFactory, "initializationVectorFactory");
    this.encryptionKeyProvider = encryptionKeyProvider;
    this.initializationVectorFactory = initializationVectorFactory;
    this.encryptionKeyDataCache = new Cache<>(CACHING_DURATION);
  }

  /**
   * encrypts the given payload (depending on the topic related configuration).
   *
   * @param kafkaTopicName name of the Kafka Topic the message is for.
   * @param plainPayload   the plain text payload.
   * @return The encrypted payload
   */
  public AesEncryptedPayload encryptPayloadWithAes(String kafkaTopicName, byte[] plainPayload) {
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName must not be null");
    Objects.requireNonNull(plainPayload, "plainPayload must not be null");

    // fetch encryption key (from cache or via EncryptionKeyProvider)
    EncryptionKeyData encryptionKeyData = encryptionKeyDataCache.getOrRetrieve(kafkaTopicName,
        this::retrieveKeyData);
    if (encryptionKeyData == null) {
      // skip encryption when no encryption key is given
      return AesEncryptedPayload.ofUnencryptedPayload(plainPayload);
    }

    // create AES key
    Key aesKey = encryptionKeyData.aesKey();

    // create new Initialization Vector for each payload
    byte[] iv = initializationVectorFactory.generateInitializationVector();

    // run encryption
    byte[] encryptedData = encrypt(plainPayload, aesKey, iv);

    return AesEncryptedPayload.ofEncryptedPayload(encryptedData, iv,
        encryptionKeyData.keyVersion());
  }

  /**
   * encrypts the given payload (depending on the topic related configuration).
   *
   * @param kafkaTopicName name of the Kafka Topic the message is for.
   * @param plainText      the plain text payload.
   * @return The encrypted payload
   */
  public AesEncryptedPayload encryptPayloadWithAes(String kafkaTopicName, String plainText) {
    Objects.requireNonNull(kafkaTopicName, "kafkaTopicName must not be null");
    Objects.requireNonNull(plainText, "plainText must not be null");
    return encryptPayloadWithAes(kafkaTopicName, plainText.getBytes(StandardCharsets.UTF_8));
  }

  private EncryptionKeyData retrieveKeyData(String topic) {
    EncryptionKeyProvider.KeyVersion keyVersion = encryptionKeyProvider.retrieveKeyForEncryption(
        topic);
    if (keyVersion == null) {
      return null;
    }
    Key aesKey = createAesKey(keyVersion);
    return new EncryptionKeyData(aesKey, keyVersion);
  }

  private SecretKeySpec createAesKey(KeyVersion keyVersion) {
    String base64Key = keyVersion.encodedKey();
    byte[] key = decodeBase64Key(base64Key);
    return new SecretKeySpec(key, "AES");
  }

  private record EncryptionKeyData(
      Key aesKey,
      EncryptionKeyProvider.KeyVersion keyVersion
  ) {

    private EncryptionKeyData {
      Objects.requireNonNull(aesKey, "aesKey must not be null");
      Objects.requireNonNull(keyVersion, "keyVersion must not be null");
    }
  }
}
