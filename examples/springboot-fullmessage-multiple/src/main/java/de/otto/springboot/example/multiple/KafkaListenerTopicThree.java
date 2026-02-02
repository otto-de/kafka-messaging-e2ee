package de.otto.springboot.example.multiple;

import static de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper.KAFKA_CE_HEADER_CIPHER_NAME_VALUE;
import static de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper.KAFKA_CE_HEADER_CIPHER_VERSION_VALUE;
import static de.otto.kafka.messaging.e2ee.KafkaEncryptionHelper.KAFKA_CE_HEADER_IV_VALUE;

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
      @Header(required = false, name = KAFKA_CE_HEADER_IV_VALUE) byte[] ceIvRaw,
      @Header(required = false, name = KAFKA_CE_HEADER_CIPHER_VERSION_VALUE) byte[] cipherVersionRaw,
      @Header(required = false, name = KAFKA_CE_HEADER_CIPHER_NAME_VALUE) byte[] cipherNameRaw
  ) {

    // decrypt incoming event
    AesEncryptedPayload encryptedPayload = KafkaEncryptionHelper.aesEncryptedPayloadOfKafka(payload,
        ceIvRaw, cipherVersionRaw, cipherNameRaw);
    String plainEvent = decryptionService.decryptToString(kafkaTopicName, encryptedPayload);

    String messageWasEncryptedTxt;
    if (encryptedPayload.isEncrypted()) {
      messageWasEncryptedTxt = "encrypted";
    } else {
      messageWasEncryptedTxt = "plain";
    }
    log.info("received {} TopicThree event: {}", messageWasEncryptedTxt, plainEvent);
  }
}
