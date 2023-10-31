package de.otto.springboot.example.cloudevent;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Map.Entry;

public interface CloudEventDeserializer {

  static CloudEventV1 deserializeToCloudEventV1(Map<String, ?> kafkaHeaders, byte[] payload,
      String kafkaTopicName, DecryptionService decryptionService) {
    // decrypt incoming event
    AesEncryptedPayload encryptedPayload = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        payload, kafkaHeaders);
    String plainEvent = decryptionService.decryptToString(kafkaTopicName, encryptedPayload);

    return CloudEventDeserializer.deserializeToCloudEventV1(kafkaHeaders, plainEvent);
  }

  static CloudEventV1 deserializeToCloudEventV1(Map<String, ?> kafkaHeaders, String dataAsJson) {
    // verify correct spec version
    String specVersion = stringValue(kafkaHeaders.get("ce_" + CloudEventV1.SPECVERSION));
    if (!"1.0".equals(specVersion)) {
      throw new IllegalArgumentException("Invalid SpecVersion: " + specVersion);
    }

    CloudEventV1Builder builder = CloudEventV1.builder()
        .withData(dataAsJson);

    for (Entry<String, ?> entry : kafkaHeaders.entrySet()) {
      if (!entry.getKey().startsWith("ce_")) {
        // ignore kafka headers that are not cloud event headers
        continue;
      }
      String name = entry.getKey().substring(3);

      switch (name) {
        case CloudEventV1.SPECVERSION:
          break;
        case CloudEventV1.ID:
          builder.withId(stringValue(entry.getValue()));
          break;
        case CloudEventV1.TYPE:
          builder.withType(stringValue(entry.getValue()));
          break;
        case CloudEventV1.SOURCE:
          builder.withSource(uriValue(entry.getValue()));
          break;
        case CloudEventV1.TIME:
          builder.withTime(offsetDateTimeValue(entry.getValue()));
          break;
        default:
          if (CloudEventV1.isValidExtensionName(name)) {
            builder.withExtension(name, stringValue(entry.getValue()));
          }
          break;
      }
    }

    return builder.build();
  }

  private static String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof CharSequence) {
      return value.toString();
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException(
        "Cannot deserialize extension of type " + value.getClass().getName());
  }

  private static URI uriValue(Object value) {
    if (value == null) {
      return null;
    }
    return URI.create(stringValue(value));
  }

  private static OffsetDateTime offsetDateTimeValue(Object value) {
    if (value == null) {
      return null;
    }
    return OffsetDateTime.parse(stringValue(value), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
