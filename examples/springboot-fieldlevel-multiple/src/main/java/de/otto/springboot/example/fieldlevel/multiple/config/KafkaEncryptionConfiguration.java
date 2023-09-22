package de.otto.springboot.example.fieldlevel.multiple.config;

import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import de.otto.kafka.messaging.e2ee.fieldlevel.FieldLevelDecryptionService;
import de.otto.kafka.messaging.e2ee.fieldlevel.FieldLevelEncryptionService;
import de.otto.kafka.messaging.e2ee.vault.CachedEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.vault.MultiTopicVaultEncryptionKeyProviderConfig;
import de.otto.kafka.messaging.e2ee.vault.MultiTopicVaultEncryptionKeyProviderConfig.KafkaTopicConfigEntry;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig;
import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KafkaEncryptionConfiguration {

  @Bean
  public VaultEncryptionKeyProviderConfig vaultEncryptionKeyProviderConfig(
      KafkaEncryptionProperties kafkaEncryptionProperties) {
    VaultConnectionConfig vaultConnectionConfig = kafkaEncryptionProperties.vaultConnectionConfig();
    List<KafkaTopicConfigEntry> configEntries = kafkaEncryptionProperties.kafkaTopicConfigEntries();
    return new MultiTopicVaultEncryptionKeyProviderConfig(vaultConnectionConfig, configEntries);
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

  @Bean
  public FieldLevelEncryptionService fieldLevelEncryptionService(
      EncryptionService encryptionService) {
    return new FieldLevelEncryptionService(encryptionService);
  }

  @Bean
  public FieldLevelDecryptionService fieldLevelDecryptionService(
      DecryptionService decryptionService) {
    return new FieldLevelDecryptionService(decryptionService);
  }
}
