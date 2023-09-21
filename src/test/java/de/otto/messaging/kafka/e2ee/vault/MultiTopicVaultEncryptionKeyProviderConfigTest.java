package de.otto.messaging.kafka.e2ee.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.otto.messaging.kafka.e2ee.vault.MultiTopicVaultEncryptionKeyProviderConfig.KafkaTopicConfigEntry;
import org.junit.jupiter.api.Test;

class MultiTopicVaultEncryptionKeyProviderConfigTest {

  @Test
  void shouldCreateConfig1() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(false)
            .kafkaTopicName("some-topic")
            .vaultPath("galapagos/some-topic")
            .build())
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.vaultPath("some-topic"))
        .isEqualTo("galapagos/some-topic");
    assertThat(config.encryptionKeyAttributeName("some-topic"))
        .isEqualTo("encryption_key");
    assertThat(config.isEncryptedTopic("some-other-topic")).isTrue();
    assertThrows(Exception.class, () -> config.vaultPath("some-other-topic"));
  }

  @Test
  void shouldCreateConfig2() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(true)
            .vaultPath("galapagos/static-path")
            .build())
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.vaultPath("some-topic"))
        .isEqualTo("galapagos/static-path");
    assertThat(config.isEncryptedTopic("some-other-topic")).isTrue();
    assertThat(config.vaultPath("some-other-topic"))
        .isEqualTo("galapagos/static-path");
  }

  @Test
  void shouldCreateConfig3() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(false)
            .kafkaTopicName("some-topic")
            .vaultPath("galapagos/some-topic")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(false)
            .kafkaTopicName("some-other-topic")
            .encryptionEnabled(false)
            .build())
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.vaultPath("some-topic"))
        .isEqualTo("galapagos/some-topic");
    assertThat(config.isEncryptedTopic("some-other-topic")).isFalse();
    assertThrows(Exception.class, () -> config.vaultPath("some-other-topic"));
  }

  @Test
  void shouldCreateConfigWithTemplate1() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(true)
            .vaultPathTemplate("galapagos/%TOPICNAME%")
            .build())
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.vaultPath("some-topic"))
        .isEqualTo("galapagos/some-topic");
    assertThat(config.isEncryptedTopic("some-other-topic")).isTrue();
    assertThat(config.vaultPath("some-other-topic"))
        .isEqualTo("galapagos/some-other-topic");
  }

  @Test
  void shouldCreateConfigWithTemplate2() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(true)
            .vaultPathTemplate("galapagos/%TEAMNAME%/%TOPICNAME%")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .teamName("teamOne")
            .kafkaTopicName("some-topic")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .teamName("theOtherTeam")
            .kafkaTopicName("some-other-topic")
            .build())
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.vaultPath("some-topic"))
        .isEqualTo("galapagos/teamOne/some-topic");
    assertThat(config.isEncryptedTopic("some-other-topic")).isTrue();
    assertThat(config.vaultPath("some-other-topic"))
        .isEqualTo("galapagos/theOtherTeam/some-other-topic");
    assertThat(config.isEncryptedTopic("some-unknown-topic")).isTrue();
    assertThrows(Exception.class, () -> config.vaultPath("some-unknown-topic"));
  }

  @Test
  void shouldCreateConfigWithEncryptionKeyProperty() {
    MultiTopicVaultEncryptionKeyProviderConfig config = MultiTopicVaultEncryptionKeyProviderConfig.builder()
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .configEntry(KafkaTopicConfigEntry.builder()
            .isDefault(false)
            .kafkaTopicName("some-topic")
            .vaultPath("galapagos/some-topic")
            .encryptionKeyAttributeName("my_secret")
            .build())
        .build();

    assertThat(config.encryptionKeyAttributeName("some-topic"))
        .isEqualTo("my_secret");
  }

}