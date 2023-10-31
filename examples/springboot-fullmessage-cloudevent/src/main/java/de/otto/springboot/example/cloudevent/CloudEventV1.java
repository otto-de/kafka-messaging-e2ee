package de.otto.springboot.example.cloudevent;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

public record CloudEventV1(
    String id,
    URI source,
    String type,
    OffsetDateTime time,
    Map<String, Object> extensions,
    String dataAsJson
) {

  /**
   * The name of the <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#id">id</a>
   * attribute
   */
  public final static String ID = "id";
  /**
   * The name of the <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#source">source</a>
   * attribute
   */
  public final static String SOURCE = "source";
  /**
   * The name of the <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#specversion">specversion</a>
   * attribute
   */
  public final static String SPECVERSION = "specversion";
  /**
   * The name of the <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#type">type</a>
   * attribute
   */
  public final static String TYPE = "type";
  /**
   * The name of the <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#time">time</a>
   * attribute
   */
  public final static String TIME = "time";

  public CloudEventV1 {
    Objects.requireNonNull(id, () -> "Attribute '" + ID + "' cannot be null");
    Objects.requireNonNull(source, () -> "Attribute '" + SOURCE + "' cannot be null");
    Objects.requireNonNull(type, () -> "Attribute '" + TYPE + "' cannot be null");
    if (extensions != null) {
      // check extensions
      for (String extensionName : extensions.keySet()) {
        if (!isValidExtensionName(extensionName)) {
          throw new IllegalArgumentException("Invalid extensions name: " + extensionName);
        }
      }
    }
  }

  public static CloudEventV1Builder builder() {
    return new CloudEventV1Builder();
  }

  /**
   * Validates the extension name as defined in  CloudEvents spec.
   *
   * @param name the extension name
   * @return true if extension name is valid, false otherwise
   * @see <a href="https://github.com/cloudevents/spec/blob/v1.0/spec.md#attribute-naming-convention">attribute-naming-convention</a>
   */
  public static boolean isValidExtensionName(String name) {
    for (char ch : name.toCharArray()) {
      if (!isValidChar(ch)) {
        return false;
      }
    }

    return true;
  }

  private static boolean isValidChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
  }
}
