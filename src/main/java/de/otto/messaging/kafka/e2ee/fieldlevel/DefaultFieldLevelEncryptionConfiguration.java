package de.otto.messaging.kafka.e2ee.fieldlevel;

interface DefaultFieldLevelEncryptionConfiguration {

  /**
   * field delimiter
   */
  String FIELD_DELIMITER = ".";
  /**
   * default prefix
   */
  String AES_V1_PREFIX = "encAesV1";
}
