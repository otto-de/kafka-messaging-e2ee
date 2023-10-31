package de.otto.springboot.example.cloudevent;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.topic.one.listener.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerTopicOne {

  private final DecryptionService decryptionService;

  public KafkaListenerTopicOne(DecryptionService decryptionService) {
    this.decryptionService = decryptionService;
  }

  @KafkaListener(topics = {"${app.topic.one.name}"})
  public void onMessage(
      @Payload(required = false) byte[] payload,
      @Header(name = "kafka_receivedTopic") String kafkaTopicName,
      @Headers Map<String, ?> kafkaHeaders) {

    Map<String, Object> readableKafkaHeaders = new HashMap<>();
    for (Entry<String, ?> entry : kafkaHeaders.entrySet()) {
      if (entry.getValue() instanceof byte[] bytes) {
        readableKafkaHeaders.put(entry.getKey(), "(byte[]) " + new String(bytes, StandardCharsets.UTF_8));
      } else {
        readableKafkaHeaders.put(entry.getKey(), entry.getValue());
      }
    }
    log.info("Headers: {}", readableKafkaHeaders);

    // decrypt incoming event
    AesEncryptedPayload encryptedPayload = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        payload, kafkaHeaders);
    String plainEvent = decryptionService.decryptToString(kafkaTopicName, encryptedPayload);

    CloudEventV1 cloudEvent = CloudEventDeserializer.deserializeToCloudEventV1(kafkaHeaders,
        plainEvent);
    log.info("received TopicOne event: {}", cloudEvent);
  }

}