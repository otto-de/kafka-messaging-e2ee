package de.otto.kafka.messaging.e2ee.helper;

import de.otto.kafka.messaging.e2ee.vault.SecondLevelCacheStorage;
import java.util.ArrayList;
import java.util.List;

public class CacheStorageMock implements SecondLevelCacheStorage {

  private final List<String> methodCalls = new ArrayList<>();
  private String cache;

  @Override
  public void storeEntry(String payload) {
    methodCalls.add("storeEntry(..)");
    this.cache = payload;
  }

  @Override
  public String retrieveEntry() {
    methodCalls.add("retrieveEntry()");
    return cache;
  }

  public void initEntry(String payload) {
    this.cache = payload;
  }

  public List<String> getMethodCalls() {
    return methodCalls;
  }
}
