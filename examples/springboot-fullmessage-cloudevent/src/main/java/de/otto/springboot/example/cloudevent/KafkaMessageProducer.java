package de.otto.springboot.example.cloudevent;

import de.otto.kafka.messaging.e2ee.EncryptionService;
import java.net.URI;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.message.producer.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMessageProducer {

  private final KafkaTemplate<String, byte[]> kafkaTemplate;
  private final EncryptionService encryptionService;
  private final String topicOneName;
  private final String topicThreeName;

  public KafkaMessageProducer(
      KafkaTemplate<String, byte[]> kafkaTemplate,
      EncryptionService encryptionService,
      @Value("${app.topic.one.name}") String topicOneName,
      @Value("${app.topic.three.name}") String topicThreeName) {
    this.kafkaTemplate = kafkaTemplate;
    this.encryptionService = encryptionService;
    this.topicOneName = topicOneName;
    this.topicThreeName = topicThreeName;
  }

  @Scheduled(fixedDelay = 5, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
  public void onTickTopicOne() {
    String data =
        "{\"data\":\"Some TopicOne data at " + LocalTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_TIME)
            + ".\"}";

    CloudEventV1 cloudEventV1 = CloudEventV1.builder()
        .withId(UUID.randomUUID().toString())
        .withType("de.otto.some.type.v1")
        .withSource(URI.create("//internal.otto.de/context/" + UUID.randomUUID()))
        .withTime(OffsetDateTime.parse("2022-06-15T15:03:29.749Z"))
        .withExtension("sequencetype", "Integer")
        .withExtension("sequence", "25")
        .withExtension("test", "scope.type")
        .withExtension("traceparent", "00-5ad4298a6e154128ad80d59dd724aa60-00f067aa0ba902b7-00")
        .withData(data)
        .build();
    publishKafkaMessage(topicOneName, cloudEventV1);
  }

  @Scheduled(fixedDelay = 13, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
  public void onTickTopicThree() {
    String data =
        "{\"data\":\"Some TopicThree my data at " + LocalTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_TIME)
            + ".\"}";

    CloudEventV1 cloudEventV1 = new CloudEventV1Builder()
        .withId(UUID.randomUUID().toString())
        .withType("de.otto.some.type.v3")
        .withSource(URI.create("//internal.otto.de/context/" + UUID.randomUUID()))
        .withTime(OffsetDateTime.parse("2022-06-15T15:03:29.749Z"))
        .withExtension("sequencetype", "Integer")
        .withExtension("sequence", "25")
        .withExtension("test", "scope.type")
        .withExtension("traceparent", "00-5ad4298a6e154128ad80d59dd724aa60-00f067aa0ba902b7-00")
        .withData(data)
        .build();
    publishKafkaMessage(topicThreeName, cloudEventV1);
  }

  private void publishKafkaMessage(String topic, CloudEventV1 cloudEventV1) {
    String partitionKey = UUID.randomUUID().toString();
    publishKafkaMessage(topic, partitionKey, cloudEventV1);
  }

  private void publishKafkaMessage(String topic, String partitionKey, CloudEventV1 cloudEvent) {
    log.info("publish cloudEvent {} to topic {}", cloudEvent, topic);

    // publish kafka event
    ProducerRecord<String, byte[]> record = CloudEventSerializer.serializeCloudEventV1(topic,
        partitionKey, cloudEvent, encryptionService);
    kafkaTemplate.send(record);
  }
}
