package de.otto.kafka.messaging.e2ee.helper;

import de.otto.kafka.messaging.e2ee.vault.RenewableVault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.api.Logical.logicalOperations;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DummyVault extends RenewableVault {

  private final List<DataVersion> vaultData = new ArrayList<>();

  public DummyVault(VaultConfig configAuth) {
    super(configAuth);
  }

  public void setPathValue(String path, String value, int version) {
    this.vaultData.add(new DataVersion(path, value, version));
  }

  @Override
  public LogicalResponse read(String path) {
    DataVersion dataVersion = vaultData.stream()
        .filter(d -> Objects.equals(d.path(), path))
        .findFirst()
        .orElse(new DataVersion(path, "null", 0));
    String response = """
        {
          "data": {
            "metadata": {
              "created_time": "2023-09-19T15:00Z",
              "version": YYY
            },
            "data": XXX
          }
        }
        """
        .replace("XXX", dataVersion.data())
        .replace("YYY", Integer.toString(dataVersion.version()));
    return new LogicalResponse(
        new RestResponse(200, "application/json", response.getBytes(StandardCharsets.UTF_8)), 1,
        logicalOperations.readV2);
  }

  @Override
  public LogicalResponse read(String path, int version) {
    DataVersion dataVersion = vaultData.stream()
        .filter(d -> Objects.equals(d.path(), path))
        .filter(d -> Objects.equals(d.version(), version))
        .findFirst()
        .orElse(new DataVersion(path, "null", 0));
    String response = """
        {
          "data": {
            "metadata": {
              "created_time": "2023-09-19T15:00Z",
              "version": YYY
            },
            "data": XXX
          }
        }
        """
        .replace("XXX", dataVersion.data())
        .replace("YYY", Integer.toString(dataVersion.version()));
    return new LogicalResponse(
        new RestResponse(200, "application/json", response.getBytes(StandardCharsets.UTF_8)), 1,
        logicalOperations.readV2);
  }

  private record DataVersion(String path, String data, int version) {

  }
}
