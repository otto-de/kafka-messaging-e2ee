package de.otto.springboot.example.single;

import static de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper.KAFKA_HEADER_CIPHER_VALUE;
import static de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper.KAFKA_HEADER_IV_VALUE;

import de.otto.kafka.messaging.e2ee.AesEncryptedPayload;
import de.otto.kafka.messaging.e2ee.DecryptionService;
import de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.topic.three.listener.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerTopicThree {

  private final DecryptionService decryptionService;

  public KafkaListenerTopicThree(DecryptionService decryptionService) {
    this.decryptionService = decryptionService;
  }

  @KafkaListener(topics = {"${app.topic.three.name}"})
  public void onMessage(
      @Payload(required = false) byte[] payload,
      @Header(name = "kafka_receivedTopic") String kafkaTopicName,
      @Header(required = false, name = KAFKA_HEADER_IV_VALUE) byte[] ivRaw,
      @Header(required = false, name = KAFKA_HEADER_CIPHER_VALUE) byte[] cipherConfigRaw) {

    // decrypt incoming event
    String plainEvent = decryptPayload(payload, kafkaTopicName, ivRaw, cipherConfigRaw);

    String messageWasEncryptedTxt;
    if (ivRaw != null && cipherConfigRaw != null) {
      messageWasEncryptedTxt = "encrypted";
    } else {
      messageWasEncryptedTxt = "plain";
    }
    log.info("received {} TopicThree event: {}", messageWasEncryptedTxt, plainEvent);
  }

  private String decryptPayload(byte[] payload, String kafkaTopicName, byte[] ivRaw,
      byte[] cipherConfigRaw) {
    AesEncryptedPayload encryptedPayload = KafkaEncryptionHelper.aesEncryptedPayloadOfKafka(payload,
        ivRaw, cipherConfigRaw);
    return decryptionService.decryptToString(kafkaTopicName, encryptedPayload);
  }
}