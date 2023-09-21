package de.otto.springboot.example.multiple;

import de.otto.messaging.kafka.e2ee.AesEncryptedPayload;
import de.otto.messaging.kafka.e2ee.DecryptionService;
import de.otto.messaging.kafka.e2ee.KafkaEncryptionHelper;
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
@ConditionalOnProperty(name = "app.topic.two.listener.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerTopicTwo {

  private final DecryptionService decryptionService;

  public KafkaListenerTopicTwo(DecryptionService decryptionService) {
    this.decryptionService = decryptionService;
  }

  @KafkaListener(topics = {"${app.topic.two.name}"})
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
    log.info("received {} TopicTwo event: {}", messageWasEncryptedTxt, plainEvent);
  }
}
