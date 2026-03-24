# Caching for the End-To-End-Encryption Library

The encryption keys stored in the vault do not change that often, so they can be cached within the
local services.
So in case of a downtime of the central vault the operation of your service is not discontinued.


## Example for a 2nd-Level Vault Cache based on AWS Parameter Store

```java
import de.otto.kafka.messaging.e2ee.vault.SecondLevelCacheStorage;

class SsmVaultCacheStorage implements SecondLevelCacheStorage {

  private final String ssmPath = "/app/app.kafka.vault.cache";
  private final SsmClient ssmClient;

  public SsmVaultCacheStorage(SsmClient ssmClient) {
    this.ssmClient = ssmClient;
  }

  @Override
  public void storeEntry(String payload) {
    ssmClient.putParameter(PutParameterRequest.builder()
            .name(ssmPath)
            .value(payload)
            .overwrite(true)
        .build());
  }

  @Override
  public String retrieveEntry() {
    var ssmParameterResult = ssmClient.getParameter(GetParameterRequest.builder()
        .name(ssmPath)
        .withDecryption(true)
        .build());
    return ssmParameterResult.parameter().value();
  }
}
```

```java
class TheApp {
  public static void main(String[] args) {
    // [...]
    SsmClient ssmClient = SsmClient.create();
    // [...]
    EncryptionKeyProvider vaultEncryptionKeyProvider = new VaultEncryptionKeyProvider(
        vaultEncryptionKeyProviderConfig);
    SecondLevelCacheStorage cacheStorage = new SsmVaultCacheStorage(ssmClient);
    Duration secondLevelCacheDuration = Duration.ofHours(48);
    EncryptionKeyProvider encryptionKeyProvider = CachedEncryptionKeyProvider.builder()
        .realEncryptionKeyProvider(vaultEncryptionKeyProvider)
        .cacheStorage(cacheStorage)
        .cachingDuration(secondLevelCacheDuration)
        .maxCacheSize(4096) // AWS SSM has a limit of 4096 characters
        .build();
    EncryptionService encryptionService = new EncryptionService(encryptionKeyProvider);
    DecryptionService decryptionService = new DecryptionService(encryptionKeyProvider);
    // [...]
  }
}
```


## Example for a 2nd-Level Vault Cache based on AWS Secret Manager

```java
import de.otto.kafka.messaging.e2ee.vault.SecondLevelCacheStorage;

class SecretManagerVaultCacheStorage implements SecondLevelCacheStorage {

  private final String secretName = "app.kafka.vault.cache";
  private final SecretsManagerClient secretsClient;

  public SecretManagerVaultCacheStorage(SecretsManagerClient secretsClient) {
    this.secretsClient = secretsClient;
  }

  @Override
  public void storeEntry(String payload) {
    secretsClient.putSecretValue(PutSecretValueRequest.builder()
        .secretId(secretName)
        .secretString(payload)
        .build());
  }

  @Override
  public String retrieveEntry() {
    var valueResponse = secretsClient.getSecretValue(GetSecretValueRequest.builder()
        .secretId(secretName)
        .build());
    return valueResponse.secretString();
  }
}
```


## Example for a 2nd-Level Vault Cache based on GCP Secrets Manager
```java
import de.otto.kafka.messaging.e2ee.vault.SecondLevelCacheStorage;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;


@Component
class SecretManagerVaultCacheStorage implements SecondLevelCacheStorage {

  private final SecretManagerServiceClient secretManagerServiceClient;
  private final String secretId;
  private final String gcpProjectNumber;

  public SecretManagerVaultCacheStorage(
      SecretManagerServiceClient secretManagerServiceClient,
      @Value("${app.e2ee.vault.cache.secret.id}") String secretId,
      @Value("${app.gcp.project.number}") String gcpProjectNumber
  ) {
    this.secretManagerServiceClient = secretManagerServiceClient;
    this.secretId = secretId;
    this.gcpProjectNumber = gcpProjectNumber;
  }

  @Override
  public void storeEntry(String payload) {
    SecretName secretName = SecretName.ofProjectSecretName(gcpProjectNumber, secretId);
    SecretPayload secretPayload = SecretPayload.newBuilder()
        .setData(ByteString.copyFrom(payload, StandardCharsets.UTF_8))
        .build();
    secretManagerServiceClient.addSecretVersion(secretName, secretPayload);
  }

  @Override
  public String retrieveEntry() {
    SecretVersionName secretVersionName = SecretVersionName.ofProjectSecretSecretVersionName(
        gcpProjectNumber, secretId, "latest");
    var response = secretManagerServiceClient.accessSecretVersion(secretVersionName);
    return response.getPayload().getData().toStringUtf8();
  }
}
```
