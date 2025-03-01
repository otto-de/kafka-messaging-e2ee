# Kafka Messaging End-To-End-Encryption library

## Status
[![Library](https://github.com/otto-de/kafka-messaging-e2ee/actions/workflows/application.yml/badge.svg)](https://github.com/otto-de/kafka-messaging-e2ee/actions/workflows/application.yml)
[![Maven Central](https://img.shields.io/maven-central/v/de.otto/kafka-messaging-e2ee?label=maven-central)](https://central.sonatype.com/artifact/de.otto/kafka-messaging-e2ee)
![OSS Lifecycle](https://img.shields.io/osslifecycle?file_url=https%3A%2F%2Fraw.githubusercontent.com%2Fotto-de%2Fkafka-messaging-e2ee%2Fmain%2FOSSMETADATA)

## About

You can use this library to make client side End-To-End-Encryption (E2EE).

For the encryption mechanism we have selected AES256 encryption with GCM mode. A 96 bits (12 bytes)
initialization vector and 128 bits (16 bytes) authorization code.

The shared encryption key (256 bits, 32 bytes) is stored in [HashiCorp Vault](https://www.vaultproject.io/) secret. The key is
subject of rotation and each encrypted message carries the version number of the key used to encrypt the data.


## Features

* compatible with the [internal otto kafka end-to-end encryption specification](https://og2.me/Ckoh1n)
* encryption can be unit tested
* supports multiple encrypted and/or unencrypted kafka topics with one single serializer and/or deserializer configuration
* refreshes the vault authentication token when using app-role authentication
* supports caching of the shared secrets (stored in the vault)
* when the shared secrets are rotated they will be used with some delay (cache delay)


## Usage
- see: [kafka-messaging-e2ee at Maven Central](https://central.sonatype.com/artifact/de.otto/kafka-messaging-e2ee)
- see: [USAGE.md](docs/USAGE.md)
- see: [Some examples](/examples)


## Changelog
- see: [CHANGELOG.md](CHANGELOG.md)


## Third Party Libraries

##### jopenlibs.github.io Vault Java Driver

* The [Vault Java Driver](https://jopenlibs.github.io/vault-java-driver) is licensed under the [MIT License](https://jopenlibs.github.io/vault-java-driver/#license).

##### Logback

* The [SLF4J API](http://www.slf4j.org) is licensed under the [MIT License](http://www.slf4j.org/license.html).


## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/otto-de/kafka-messaging-e2ee/tags).

### Commit style

We are using the [semantic release plugin](https://github.com/marketplace/actions/action-for-semantic-release) that automatically create tags based on the commit message.
Therefor you should follow the [Angular Commit Message Conventions](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#-commit-message-format)

TL;DR
```text
## Commit Message styles
fix(<something>): <fix a bug commit message>
feat(<something>): <build a new feature commit message>
docs(<something>): <add documentation commit message (will not create a new tag)>
refactor(<something>): <add refactoring commit message (will not create a new tag)>
```

## Authors

See also the list of [contributors](CONTRIBUTORS.md) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details
