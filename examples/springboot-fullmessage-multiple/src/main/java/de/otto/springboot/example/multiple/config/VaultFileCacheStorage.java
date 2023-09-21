package de.otto.springboot.example.multiple.config;

import de.otto.messaging.kafka.e2ee.vault.SecondLevelCacheStorage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VaultFileCacheStorage implements SecondLevelCacheStorage {

  private static final String fileName = "vaultCache.json";

  @Override
  public void storeEntry(String payload) {
    log.debug("storeEntry(): payload: {}", payload);

    try {
      FileWriter fw = new FileWriter(fileName);
      fw.write(payload);
      fw.close();
    } catch (IOException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  @Override
  public String retrieveEntry() {
    try {
      StringBuilder resultStringBuilder = new StringBuilder();
      try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
        String line;
        while ((line = br.readLine()) != null) {
          resultStringBuilder.append(line).append("\n");
        }
      }

      log.debug("retrieveEntry(): cache result: {}", resultStringBuilder);
      return resultStringBuilder.toString();
    } catch (FileNotFoundException ex) {
      log.warn(ex.getMessage());
      return null;
    } catch (IOException ex) {
      log.error(ex.getMessage(), ex);
      return null;
    }
  }
}
