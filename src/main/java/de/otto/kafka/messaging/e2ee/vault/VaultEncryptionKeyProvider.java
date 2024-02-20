package de.otto.kafka.messaging.e2ee.vault;

import static de.otto.kafka.messaging.e2ee.vault.VaultEncryptionKeyProviderConfig.DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME;

import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.JsonValue;
import io.github.jopenlibs.vault.response.LogicalResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VaultEncryptionKeyProvider implements EncryptionKeyProvider {

  private static final Logger log = LoggerFactory.getLogger(VaultEncryptionKeyProvider.class);

  private final VaultEncryptionKeyProviderConfig config;
  private ReadonlyVaultApi vault;

  public VaultEncryptionKeyProvider(
      VaultEncryptionKeyProviderConfig config) {
    this.config = config;
    this.vault = null;
  }

  @Override
  public KeyVersion retrieveKeyForEncryption(String kafkaTopicName) {
    if (!isEncryptedTopic(kafkaTopicName)) {
      return null;
    }

    LogicalResponse response;
    String path = getPathForTopic(kafkaTopicName);
    try {
      ReadonlyVaultApi theVault = getOrCreateVault();
      response = theVault.read(path);
      validateResponse(response, () -> "path '" + path + "'");
    } catch (VaultException ex) {
      throw new VaultRuntimeException(ex);
    }

    String encryptionKeyAttributeName = config.encryptionKeyAttributeName(kafkaTopicName);
    String usedEncryptionKeyAttributeName = Objects.requireNonNullElse(encryptionKeyAttributeName,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
    String key = extractEncryptionKeyFromResponse(response, usedEncryptionKeyAttributeName);
    OffsetDateTime createdTime = extractCreationTimeFromResponse(response);
    int version = extractVersionFromResponse(response);

    if (log.isTraceEnabled()) {
      log.trace("The latest encryption key is {}, version = {}, created at {}", key, version,
          createdTime);
    } else {
      log.debug("The latest encryption key is ***, version = {}, created at {}", version,
          createdTime);
    }

    return new KeyVersion(version, usedEncryptionKeyAttributeName, key);
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version) {
    String encryptionKeyAttributeName = config.encryptionKeyAttributeName(topic);
    return retrieveKeyForDecryption(topic, version, encryptionKeyAttributeName);
  }

  @Override
  public String retrieveKeyForDecryption(String topic, int version,
      String encryptionKeyAttributeName) {
    LogicalResponse response;
    String path = getPathForTopic(topic);
    try {
      ReadonlyVaultApi theVault = getOrCreateVault();
      response = theVault.read(path, version);
      validateResponse(response, () -> "path '" + path + "' and version '" + version + "'");
    } catch (VaultException ex) {
      throw new VaultRuntimeException(ex);
    }

    String usedEncryptionKeyAttributeName = Objects.requireNonNullElse(encryptionKeyAttributeName,
        DEFAULT_ENCRYPTION_KEY_ATTRIBUTE_NAME);
    return extractEncryptionKeyFromResponse(response, usedEncryptionKeyAttributeName);
  }

  @Override
  public boolean isEncryptedTopic(String kafkaTopicName) {
    return config.isEncryptedTopic(kafkaTopicName);
  }

  private void validateResponse(LogicalResponse response, Supplier<String> errorMsgSupplier) {
    if (log.isTraceEnabled()) {
      log.trace("status = {} / body = {}", response.getRestResponse().getStatus(),
          new String(response.getRestResponse().getBody()));
    } else if (log.isDebugEnabled()) {
      log.debug("status = {} / body = ***", response.getRestResponse().getStatus());
    }

    if (response.getRestResponse().getStatus() != 200) {
      if (log.isErrorEnabled()) {
        log.error("Vault response. HttpCode={} Response={}", response.getRestResponse().getStatus(),
            new String(response.getRestResponse().getBody()));
      }

      throw new VaultRuntimeException(
          "Vault request failed with HttpCode=" + response.getRestResponse().getStatus() + " for "
              + errorMsgSupplier.get());
    }
  }

  private String extractEncryptionKeyFromResponse(LogicalResponse response,
      String encryptionKeyAttributeName) {
    JsonObject dataObject = extractDataObjectFromResponse(response);
    JsonValue key = dataObject.get(encryptionKeyAttributeName);
    if (key == null) {
      throw new VaultRuntimeException(
          "Secret does not contain '" + encryptionKeyAttributeName + "'.");
    }

    // key is bas64 and URL-encoded
    return key.asString();
  }

  private int extractVersionFromResponse(LogicalResponse response) {
    JsonObject metadata = extractMetaDataObjectFromResponse(response);
    Integer version = metadata.getInt("version");
    if (version == null) {
      throw new VaultRuntimeException("Metadata.Version is not valid.");
    }
    return version;
  }

  private OffsetDateTime extractCreationTimeFromResponse(LogicalResponse response) {
    JsonObject metadata = extractMetaDataObjectFromResponse(response);
    String secretCreatedTime = metadata.getString("created_time");
    if (secretCreatedTime == null) {
      throw new VaultRuntimeException("Metadata.CreatedTime is not valid.");
    }
    return OffsetDateTime.parse(secretCreatedTime);
  }

  private JsonObject extractDataObjectFromResponse(LogicalResponse response) {
    JsonObject dataObject = response.getDataObject();
    if (dataObject == null) {
      throw new VaultRuntimeException("Secret is not valid. KV has not version 2.");
    }
    return dataObject;
  }

  private JsonObject extractMetaDataObjectFromResponse(LogicalResponse response) {
    String responseText = new String(response.getRestResponse().getBody(),
        StandardCharsets.UTF_8);
    JsonObject responseObject = Json.parse(responseText).asObject();
    JsonValue responseData = responseObject.get("data");
    JsonValue responseMetaData = responseData.asObject().get("metadata");
    if (responseMetaData == null) {
      throw new VaultRuntimeException(
          "Secret is not valid - Missing 'metadata'. KV has not version 2.");
    }
    JsonObject metadata = responseMetaData.asObject();
    if (metadata == null) {
      throw new VaultRuntimeException("Metadata is not valid.");
    }
    return metadata;
  }

  private String getPathForTopic(String kafkaTopicName) {
    return config.vaultPath(kafkaTopicName);
  }

  private ReadonlyVaultApi getOrCreateVault() throws VaultException {
    if (vault == null) {
      this.vault = config.createReadonlyVault();
    }
    return vault;
  }
}
