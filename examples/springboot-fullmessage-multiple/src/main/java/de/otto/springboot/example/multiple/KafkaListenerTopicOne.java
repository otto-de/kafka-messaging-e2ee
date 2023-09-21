package de.otto.springboot.example.multiple;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper;
import java.util.Map;
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

    // decrypt incoming event
    AesEncryptedPayload encryptedPayload = KafkaEncryptionHelper.aesEncryptedPayloadOfKafkaForValue(
        payload, kafkaHeaders);
    String plainEvent = decryptionService.decryptToString(kafkaTopicName, encryptedPayload);

    String messageWasEncryptedTxt;
    if (encryptedPayload.isEncrypted()) {
      messageWasEncryptedTxt = "encrypted";
    } else {
      messageWasEncryptedTxt = "plain";
    }
    log.info("received {} TopicOne event: {}", messageWasEncryptedTxt, plainEvent);
  }
}
