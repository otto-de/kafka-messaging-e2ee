package de.otto.kafka.messaging.e2ee.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider.KeyVersion;
import de.otto.kafka.messaging.e2ee.helper.DummyVaultEncryptionKeyProviderConfig;
import org.junit.jupiter.api.Test;

class VaultEncryptionKeyProviderTest {

  @Test
  void shouldReturnRequestedKeyForEncryption() {
    // given: a test setup with the correct encryption key
    DummyVaultEncryptionKeyProviderConfig config = new DummyVaultEncryptionKeyProviderConfig();
    assertThat(config.vaultPath("someTopic"))
        .describedAs("vaultPath").isEqualTo("/galaogos_someTopic");
    assertThat(config.encryptionKeyAttributeName("someTopic"))
        .describedAs("encryptionKeyAttributeName").isEqualTo("encryption_key");
    config.getDummyVault()
        .setPathValue("/galaogos_someTopic", "{\"encryption_key\": \"someEncodedKey\"}", 3);
    VaultEncryptionKeyProvider encryptionKeyProvider = new VaultEncryptionKeyProvider(config);
    // when: method is called
    KeyVersion keyVersion = encryptionKeyProvider.retrieveKeyForEncryption(
        "someTopic");
    // then: expected key version should have been returned
    KeyVersion expecetdKeyVersion = new KeyVersion(3, "encryption_key", "someEncodedKey");
    assertThat(keyVersion).usingRecursiveComparison().isEqualTo(expecetdKeyVersion);
  }

  @Test
  void shouldThrowExceptionWhenRequestedKeyForEncryptionIsNotPresent() {
    // given: a test setup with the incorrect encryption key name
    DummyVaultEncryptionKeyProviderConfig config = new DummyVaultEncryptionKeyProviderConfig();
    assertThat(config.vaultPath("someTopic"))
        .describedAs("vaultPath").isEqualTo("/galaogos_someTopic");
    assertThat(config.encryptionKeyAttributeName("someTopic"))
        .describedAs("encryptionKeyAttributeName").isEqualTo("encryption_key");
    config.getDummyVault()
        .setPathValue("/galaogos_someTopic", "{\"aes\": \"someEncodedKey\"}", 3);
    VaultEncryptionKeyProvider encryptionKeyProvider = new VaultEncryptionKeyProvider(config);
    // when: method is called
    Exception thrownException = assertThrows(VaultRuntimeException.class,
        () -> encryptionKeyProvider.retrieveKeyForEncryption("someTopic"));
    // then: exception should have a useful message
    assertThat(thrownException).hasMessage("Secret does not contain 'encryption_key'.");
  }

  @Test
  void shouldReturnRequestedKeyVersionForDecryption() {
    // given: a test setup with the correct encryption key
    DummyVaultEncryptionKeyProviderConfig config = new DummyVaultEncryptionKeyProviderConfig();
    assertThat(config.vaultPath("someTopic"))
        .describedAs("vaultPath").isEqualTo("/galaogos_someTopic");
    assertThat(config.encryptionKeyAttributeName("someTopic"))
        .describedAs("encryptionKeyAttributeName").isEqualTo("encryption_key");
    config.getDummyVault()
        .setPathValue("/galaogos_someTopic", "{\"encryption_key\": \"someEncodedKey\"}", 3);
    VaultEncryptionKeyProvider encryptionKeyProvider = new VaultEncryptionKeyProvider(config);
    // when: method is called
    String result = encryptionKeyProvider.retrieveKeyForDecryption("someTopic", 3,
        "encryption_key");
    // then: expected key should have been returned
    assertThat(result).isEqualTo("someEncodedKey");
  }

  @Test
  void shouldReturnRequestedKeyVersionWithDefaultNameForDecryption() {
    // given: a test setup with the correct encryption key
    DummyVaultEncryptionKeyProviderConfig config = new DummyVaultEncryptionKeyProviderConfig();
    assertThat(config.vaultPath("someTopic"))
        .describedAs("vaultPath").isEqualTo("/galaogos_someTopic");
    assertThat(config.encryptionKeyAttributeName("someTopic"))
        .describedAs("encryptionKeyAttributeName").isEqualTo("encryption_key");
    config.getDummyVault()
        .setPathValue("/galaogos_someTopic", "{\"encryption_key\": \"someEncodedKey\"}", 3);
    VaultEncryptionKeyProvider encryptionKeyProvider = new VaultEncryptionKeyProvider(config);
    // when: method is called
    String result = encryptionKeyProvider.retrieveKeyForDecryption("someTopic", 3);
    // then: expected key should have been returned
    assertThat(result).isEqualTo("someEncodedKey");
  }

}