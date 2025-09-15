# [2.3.0](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.7...v2.3.0) (2025-09-15)


### Features

* **caching:** add a size limit to the 2nd-level-cache - AWS SSM has a limit of 4096 characters, also introduce builder pattern for CachedEncryptionKeyProvider ([5ab499d](https://github.com/otto-de/kafka-messaging-e2ee/commit/5ab499d1fffc86b03ea11051b24e97458866b8bc))

## [2.2.7](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.6...v2.2.7) (2025-02-26)


### Bug Fixes

* **deps:** bump slf4jVersion from 2.0.16 to 2.0.17 ([#35](https://github.com/otto-de/kafka-messaging-e2ee/issues/35)) ([677bce0](https://github.com/otto-de/kafka-messaging-e2ee/commit/677bce073f41f12d444c42581ce0ec45d31e75d9))

## [2.2.6](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.5...v2.2.6) (2024-05-27)


### Bug Fixes

* **deps:** bump slf4jVersion from 2.0.12 to 2.0.13 ([#16](https://github.com/otto-de/kafka-messaging-e2ee/issues/16)) ([f8efcca](https://github.com/otto-de/kafka-messaging-e2ee/commit/f8efccadb9711c47cdbbb95e79c8231bb1b4a3c2))

## [2.2.5](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.4...v2.2.5) (2024-04-08)


### Bug Fixes

* **version:** [CORMORANT-1629] update to gradle 8.7 ([983b54f](https://github.com/otto-de/kafka-messaging-e2ee/commit/983b54f11d332deca9e39f1ca5ea4a3ec743c8a4))

## [2.2.4](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.3...v2.2.4) (2024-03-26)


### Bug Fixes

* **deps:** bump io.github.gradle-nexus.publish-plugin ([#12](https://github.com/otto-de/kafka-messaging-e2ee/issues/12)) ([8953578](https://github.com/otto-de/kafka-messaging-e2ee/commit/8953578a10ec0e231c5feb93bda8a47b537155ef))
* **deps:** bump org.assertj:assertj-core from 3.25.1 to 3.25.3 ([#13](https://github.com/otto-de/kafka-messaging-e2ee/issues/13)) ([6f38bf5](https://github.com/otto-de/kafka-messaging-e2ee/commit/6f38bf56c8f977c6cdb36ba09c5bdf73aacf885d))

## [2.2.3](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.2...v2.2.3) (2024-03-26)


### Bug Fixes

* **deps:** bump slf4jVersion from 2.0.9 to 2.0.12 ([#10](https://github.com/otto-de/kafka-messaging-e2ee/issues/10)) ([3320737](https://github.com/otto-de/kafka-messaging-e2ee/commit/332073776d23f79375ef3ca027ffc4f470801026))

## [2.2.2](https://github.com/otto-de/kafka-messaging-e2ee/compare/v2.2.1...v2.2.2) (2024-03-26)


### Bug Fixes

* **deps:** bump io.github.jopenlibs:vault-java-driver ([#1](https://github.com/otto-de/kafka-messaging-e2ee/issues/1)) ([55839ec](https://github.com/otto-de/kafka-messaging-e2ee/commit/55839ec3c4fb9aadcfb6742fed888acd38f5c858))
* **deps:** bump org.junit.jupiter:junit-jupiter from 5.10.0 to 5.10.2 ([#11](https://github.com/otto-de/kafka-messaging-e2ee/issues/11)) ([4d419e2](https://github.com/otto-de/kafka-messaging-e2ee/commit/4d419e27d5475f4415e8f313bb0694c31b0903df))

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
