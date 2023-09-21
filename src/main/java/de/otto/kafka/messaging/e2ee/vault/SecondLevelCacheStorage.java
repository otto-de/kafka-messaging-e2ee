package de.otto.kafka.messaging.e2ee.vault;

/**
 * Interface for any backend that can store secrets. E.g. a File or AWS Parameter Store or GCP
 * Secret Manager
 */
public interface SecondLevelCacheStorage {

  /**
   * stores the given payload somewere locally. E.g. in a File or AWS Parameter Store or GCP Secret
   * Manager
   *
   * @param payload the payload to be stored. Is never <code>null</code>
   */
  void storeEntry(String payload);

  /**
   * @return the previously stored payload or <code>null</code>
   */
  String retrieveEntry();
}
