package de.otto.springboot.example.cloudevent;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class CloudEventV1Builder {

  private String id;
  private URI source;
  private String type;
  private OffsetDateTime time;
  private Map<String, Object> extensions;
  private String dataAsJson;

  public CloudEventV1 build() {
    return new CloudEventV1(id, source, type, time, extensions, dataAsJson);
  }

  public CloudEventV1Builder withId(String id) {
    this.id = id;
    return this;
  }

  public CloudEventV1Builder withType(String type) {
    this.type = type;
    return this;
  }

  public CloudEventV1Builder withSource(URI source) {
    this.source = source;
    return this;
  }

  public CloudEventV1Builder withTime(OffsetDateTime time) {
    this.time = time;
    return this;
  }

  public CloudEventV1Builder withExtension(String name, Object value) {
    if (this.extensions == null) {
      this.extensions = new HashMap<>();
    }
    this.extensions.put(name, value);
    return this;
  }

  public CloudEventV1Builder withData(String dataAsJson) {
    this.dataAsJson = dataAsJson;
    return this;
  }
}
