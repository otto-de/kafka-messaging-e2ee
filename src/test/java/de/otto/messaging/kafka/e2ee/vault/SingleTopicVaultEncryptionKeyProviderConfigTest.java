package de.otto.messaging.kafka.e2ee.vault;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.messaging.kafka.e2ee.vault.VaultConnectionConfig.VaultAppRole;
import org.junit.jupiter.api.Test;

class SingleTopicVaultEncryptionKeyProviderConfigTest {

  @Test
  void shouldBuildConfig1() {
    SingleTopicVaultEncryptionKeyProviderConfig config = SingleTopicVaultEncryptionKeyProviderConfig.builder()
        .isEncryptedTopic(true)
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .vaultPath("galapagos/some-topic")
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.isEncryptedTopic("some-other-topic")).isTrue();
    assertThat(config.vaultPath("some-topic")).isEqualTo(
        "galapagos/some-topic");
    assertThat(config.vaultPath("some-other-topic")).isEqualTo(
        "galapagos/some-topic");
    assertThat(config.encryptionKeyAttributeName("some-topic"))
        .isEqualTo("encryption_key");
  }

  @Test
  void shouldBuildConfig2() {
    SingleTopicVaultEncryptionKeyProviderConfig config = SingleTopicVaultEncryptionKeyProviderConfig.builder()
        .isEncryptedTopic(true)
        .kafkaTopicName("some-topic")
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .appRole(VaultAppRole.builder()
                .path("app-role-path")
                .roleid("app-role-id")
                .secretid("app-role-secret-id")
                .build())
            .build())
        .vaultPath("galapagos/some-topic")
        .build();

    assertThat(config.isEncryptedTopic("some-topic")).isTrue();
    assertThat(config.isEncryptedTopic("some-other-topic")).isFalse();
    assertThat(config.vaultPath("some-topic")).isEqualTo(
        "galapagos/some-topic");
  }

  @Test
  void shouldBuildConfigWithEncryptionKeyAttribute() {
    SingleTopicVaultEncryptionKeyProviderConfig config = SingleTopicVaultEncryptionKeyProviderConfig.builder()
        .isEncryptedTopic(true)
        .vaultConnectionConfig(VaultConnectionConfig.builder()
            .token("dev-token")
            .build())
        .vaultPath("galapagos/some-topic")
        .encryptionKeyAttributeName("my_secret")
        .build();

    assertThat(config.encryptionKeyAttributeName("some-topic"))
        .isEqualTo("my_secret");
  }
}