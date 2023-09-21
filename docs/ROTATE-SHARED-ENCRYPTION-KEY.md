# How To rotate the encryption key using cURL


## create new encryption key via vault token

```bash
## using 'dd' and '/dev/random' to create a new random number with 32 bytes (256 bits), base 64 encoded
newRawEncryptionKey=$(dd if=/dev/urandom bs=32 count=1 status=none | base64)
## format newRawEncryptionKey to URL-Encoding (US_ASCII)
newEncryptionKey=$(echo $(echo "${newRawEncryptionKey}" | jq --slurp --raw-input --raw-output @uri))
echo "newEncryptionKey = ${newEncryptionKey}"

## store secrets at the vault
VAULT_TOKEN="dev-only-token"
E2EE_TEAMNAME="teamOne"
E2EE_TOPICNAME="teamOneTopicOne"

## put new secret via cURL to vault
url="http://localhost:8200/v1/galapagos/data/local/galapagos_${E2EE_TEAMNAME}/${E2EE_TOPICNAME}"
echo "call URL = ${url}"

curl -v \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --header "Content-Type: application/json" \
  --request POST \
  --data "{\"data\": {\"encryption_key\": \"${newEncryptionKey}\"}}" \
  ${url} | jq
```
