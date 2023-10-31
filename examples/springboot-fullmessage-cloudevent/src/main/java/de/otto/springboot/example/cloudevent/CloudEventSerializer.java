package de.otto.springboot.example.cloudevent;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.EncryptionService;
import de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

public interface CloudEventSerializer {

  static ProducerRecord<String, byte[]> serializeCloudEventV1(String topic, String partitionKey,
      CloudEventV1 cloudEvent, EncryptionService encryptionService) {
    // encrypt payload (if needed)
    AesEncryptedPayload aesEncryptedPayload = encryptionService.encryptPayloadWithAes(
        topic, cloudEvent.dataAsJson());

    // fetch kafka headers
    ArrayList<Header> headers = new ArrayList<>();
    // add cloud event headers
    addCloudEventHeaders(headers, cloudEvent);
    // add encryption headers
    if (aesEncryptedPayload.isEncrypted()) {
      Map<String, byte[]> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForValue(
          aesEncryptedPayload);
      kafkaHeaders.forEach((key, value) -> headers.add(new KafkaHeader(key, value)));
    }

    byte[] payload = aesEncryptedPayload.encryptedPayload();
    return serializeCloudEventV1(topic, partitionKey, payload, headers);
  }

  static ProducerRecord<String, byte[]> serializeCloudEventV1(String topic, String partitionKey,
      CloudEventV1 cloudEvent) {
    // fetch kafka headers
    ArrayList<Header> headers = new ArrayList<>();
    // add cloud event headers
    addCloudEventHeaders(headers, cloudEvent);

    byte[] payload = cloudEvent.dataAsJson().getBytes(StandardCharsets.UTF_8);
    return serializeCloudEventV1(topic, partitionKey, payload, headers);
  }

  static ProducerRecord<String, byte[]> serializeCloudEventV1(String topic, String partitionKey,
      byte[] payload, Iterable<Header> headers) {
    return new ProducerRecord<>(topic, null, partitionKey, payload, headers);
  }

  private static void addCloudEventHeaders(List<Header> headers, CloudEventV1 cloudEvent) {
    headers.add(CeKafkaHeader.of(CloudEventV1.SPECVERSION, "1.0"));
    headers.add(CeKafkaHeader.of(CloudEventV1.ID, cloudEvent.id()));
    headers.add(CeKafkaHeader.of(CloudEventV1.TYPE, cloudEvent.type()));
    headers.add(CeKafkaHeader.of(CloudEventV1.SOURCE, cloudEvent.source()));

    if (cloudEvent.time() != null) {
      headers.add(CeKafkaHeader.of(CloudEventV1.TIME, cloudEvent.time()));
    }

    for (Entry<String, Object> entry : cloudEvent.extensions().entrySet()) {
      if (entry.getValue() == null) {
        // skip NULL values
        continue;
      }
      if (entry.getValue() instanceof String value) {
        headers.add(CeKafkaHeader.of(entry.getKey(), value));
        continue;
      }
      throw new IllegalArgumentException(
          "Cannot serialize extension '" + entry.getKey() + "' of class " + entry.getValue()
              .getClass().getName());
    }
  }

  record KafkaHeader(String key, byte[] value) implements Header {

  }

  record CeKafkaHeader(String key, byte[] value) implements Header {

    public static CeKafkaHeader of(String name, String value) {
      return new CeKafkaHeader("ce_" + name, value.getBytes(StandardCharsets.UTF_8));
    }

    public static CeKafkaHeader of(String name, URI value) {
      return of(name, value.toString());
    }

    public static CeKafkaHeader of(String name, OffsetDateTime value) {
      return of(name, value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
  }
}
