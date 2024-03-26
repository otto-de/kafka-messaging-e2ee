## [2.2.1](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.0...v2.2.1) (2024-03-26)


### Bug Fixes

* **deps:** bump actions/setup-java from 3 to 4 ([#3](https://github.com/otto-de/kafka-messaging-e2ee/issues/3)) ([b97ef42](https://github.com/otto-de/kafka-messaging-e2ee/commit/b97ef421bef409c11cc3a008c1a1426c07c1db31))
* **deps:** bump actions/upload-artifact from 3 to 4 ([#4](https://github.com/otto-de/kafka-messaging-e2ee/issues/4)) ([49dcdf8](https://github.com/otto-de/kafka-messaging-e2ee/commit/49dcdf8864e79459c23f12aaa67fd5c9e4764414))
* **deps:** bump org.assertj:assertj-core from 3.24.2 to 3.25.1 ([#7](https://github.com/otto-de/kafka-messaging-e2ee/issues/7)) ([780f52b](https://github.com/otto-de/kafka-messaging-e2ee/commit/780f52b8b00552f69cbe72f86507bffae27e21a4))

# [2.2.0](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.1.2...v2.2.0) (2024-02-20)


### Features

* **encryptionCheck:** introduce new method DecryptionService#hasSameEncryptionFlag(..) so kafka consumers can validate the kafka message producer ([39e05c2](https://github.com/otto-de/kafka-messaging-e2ee/commit/39e05c21e99740177f2cdcaa6689c85a3ff93861))

## [2.1.2](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.1.1...v2.1.2) (2023-11-03)


### Bug Fixes

* **specUpdate:** update "ce_" header names to comply with the CloudEvent specification. ([efdc140](https://github.com/otto-de/kafka-messaging-e2ee/commit/efdc140bb63646772e294682f6e824fae947e95f))

## [2.1.1](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.1.0...v2.1.1) (2023-10-17)


### Bug Fixes

* **specUpdate:** bug fix in KafkaEncryptionHelper#aesEncryptedPayloadOfKafka - it used the wrong IV variable ([94c6378](https://github.com/otto-de/kafka-messaging-e2ee/commit/94c63786643d9e187e2d8d7c26657f62623cf8e7))

# [2.1.0](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.0.1...v2.1.0) (2023-10-04)


### Features

* **specUpdate:** Updated to Kafka Headers of specification version 2. Use new "ce_encryption.ref.*" headers. ([11a9bff](https://github.com/otto-de/kafka-messaging-e2ee/commit/11a9bff9bc324a480e832d271d8af3108db9dc68))

## [2.0.1](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.0.0...v2.0.1) (2023-09-22)


### Bug Fixes

* **codeStyle:** make classes "final" so nobody can alter them. Create ReadonlyVaultApi interface so UnitTests become more easy. ([c47b918](https://github.com/otto-de/kafka-messaging-e2ee/commit/c47b9189fb7c54808b11752a5acae1cf5eb56ccf))

# [2.0.0](https://github.com/otto-de/kafka-messaging-e2ee/compare/v1.0.1...v2.0.0) (2023-09-21)


### Bug Fixes

* **init:** rename java package from "de.otto.messaging.kafka.e2ee" to "de.otto.kafka.messaging.e2ee" so it matches the artifactId ([b69f69d](https://github.com/otto-de/kafka-messaging-e2ee/commit/b69f69dcf56ae91f316337a9b42627f8d43aa5b3))


### BREAKING CHANGES

* **init:** package names have changed to "de.otto.kafka.messaging.e2ee"

## [1.0.1](https://github.com/otto-de/kafka-messaging-e2ee/compare/v1.0.0...v1.0.1) (2023-09-21)


### Bug Fixes

* **init:** remove publishing from github action pipeline - this will come later. For now we can use the release.sh script. ([86a1ba6](https://github.com/otto-de/kafka-messaging-e2ee/commit/86a1ba61f0d9810dbe1cd7c3673428403d70b959))

# 1.0.0 (2023-09-21)


### Features

* **init:** create first version of messaging.kafka.e2ee library ([4756ec2](https://github.com/otto-de/kafka-messaging-e2ee/commit/4756ec2ea036e3a43ec4dc646632dbdcc3fc8935))
