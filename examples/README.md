# Example usage of the Kafka Messaging End-To-End-Encryption library

## Initialize local environment

### Start services as Docker containers

```bash
## start the docker containers
cd /repos/kafka-messaging-e2ee/examples
./gradlew composeUp
```

- see vault: http://localhost:8200  (using token ```dev-only-token```)

### Build the libraries locally

```bash
## (optional) clean old versions of this libraries
rm -rfv $HOME/.m2/repository/de/otto/kafka-messaging-e2ee

## update the libraries in local maven repository
cd /repos/kafka-messaging-e2ee
./gradlew clean publishToMavenLocal -x signMavenJavaPublication --info
```

### Start one of the SpringBoot services

```bash
cd /repos/kafka-messaging-e2ee/examples

## start the "springboot-fullmessage-multiple" example
./gradlew clean :springboot-fullmessage-multiple:bootRun

## or start the "springboot-fieldlevel-multiple" example
./gradlew clean :springboot-fieldlevel-multiple:bootRun
```


## Examples

* [springboot-fullmessage-multiple](springboot-fullmessage-multiple) .. Example which uses SpringBoot and full-message encryption and decryption. It has multiple encrypted Kafka Topics configured.
* [springboot-fullmessage-single](springboot-fullmessage-single) .. Example which uses SpringBoot and full-message encryption and decryption. It has one encrypted Kafka Topic configured.
* [springboot-fieldlevel-multiple](springboot-fieldlevel-multiple) .. Example which uses SpringBoot and field-level encryption and decryption. It uses the Multi-Topic-Configuration.

