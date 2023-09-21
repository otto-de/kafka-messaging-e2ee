package de.otto.springboot.example.single;

import de.otto.messaging.kafka.e2ee.AesEncryptedPayload;
import de.otto.messaging.kafka.e2ee.EncryptionService;
import de.otto.messaging.kafka.e2ee.KafkaEncryptionHelper;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
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
    publishKafkaMessage(topicOneName, data);
  }

  @Scheduled(fixedDelay = 13, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
  public void onTickTopicThree() {
    String data =
        "{\"data\":\"Some TopicThree my data at " + LocalTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_TIME)
            + ".\"}";
    publishKafkaMessage(topicThreeName, data);
  }

  private void publishKafkaMessage(String topic, String data) {
    String partitionKey = UUID.randomUUID().toString();

    // encrypt payload (if needed)
    AesEncryptedPayload aesEncryptedPayload = encryptionService.encryptPayloadWithAes(
        topic, data);

    // fetch kafka headers
    ArrayList<Header> headers = new ArrayList<>();
    if (aesEncryptedPayload.isEncrypted()) {
      Map<String, byte[]> kafkaHeaders = KafkaEncryptionHelper.mapToKafkaHeadersForValue(
          aesEncryptedPayload);
      kafkaHeaders.forEach((key, value) -> headers.add(new KafkaHeader(key, value)));
    }

    // publish kafka event
    kafkaTemplate.send(
        new ProducerRecord<>(topic, null, partitionKey, aesEncryptedPayload.encryptedPayload(), headers));
    log.info("created event for {}: {}", topic, data);
  }

  private record KafkaHeader(String key, byte[] value) implements Header {

  }
}
