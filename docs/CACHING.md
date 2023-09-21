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
    EncryptionKeyProvider encryptionKeyProvider = new CachedEncryptionKeyProvider(
        vaultEncryptionKeyProvider, cacheStorage, secondLevelCacheDuration
    );
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


## Example for a 2nd-Level Vault Cache based on GCP Secret Manager

see: https://cloud.google.com/secret-manager/docs/create-secret-quickstart