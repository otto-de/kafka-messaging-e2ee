package de.otto.springboot.example.single.config;

import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import de.otto.kafka.messaging.e2ee.vault.CachedEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.vault.SingleTopicVaultEncryptionKeyProviderConfig;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig;
import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KafkaEncryptionConfiguration {

  @Bean
  public VaultEncryptionKeyProviderConfig vaultEncryptionKeyProviderConfig(
      KafkaEncryptionProperties kafkaEncryptionProperties,
      @Value("${app.topic.one.name}") String kafkaTopicName,
      @Value("${app.topic.one.encrypted}") boolean encryptedTopic,
      @Value("${app.topic.one.vaultPath}") String vaultPath,
      @Value("${app.topic.one.encryptionKeyAttributeName}") String encryptionKeyAttributeName
  ) {
    VaultConnectionConfig vaultConnectionConfig = kafkaEncryptionProperties.vaultConnectionConfig();
    return SingleTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(vaultConnectionConfig)
        .kafkaTopicName(kafkaTopicName)
        .isEncryptedTopic(encryptedTopic)
        .vaultPath(vaultPath)
        .encryptionKeyAttributeName(encryptionKeyAttributeName)
        .build();
  }

  @Bean
  public EncryptionKeyProvider encryptionKeyProvider(
      VaultEncryptionKeyProviderConfig vaultEncryptionKeyProviderConfig,
      VaultFileCacheStorage vaultFileCacheStorage) {
    VaultEncryptionKeyProvider encryptionKeyProvider = new VaultEncryptionKeyProvider(
        vaultEncryptionKeyProviderConfig);
    Duration cachingDuration = Duration.ofHours(8);
    return new CachedEncryptionKeyProvider(encryptionKeyProvider, vaultFileCacheStorage, cachingDuration);
  }

  @Bean
  public EncryptionService encryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    return new EncryptionService(encryptionKeyProvider);
  }

  @Bean
  public DecryptionService decryptionService(EncryptionKeyProvider encryptionKeyProvider) {
    return new DecryptionService(encryptionKeyProvider);
  }
}
