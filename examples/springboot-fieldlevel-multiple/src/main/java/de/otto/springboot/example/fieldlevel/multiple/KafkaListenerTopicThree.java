package de.otto.springboot.example.fieldlevel.multiple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import de.otto.kafka.messaging.e2ee.fieldlevel.FieldLevelDecryptionService;
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

  private final FieldLevelDecryptionService decryptionService;
  private final SomeBusinessService someBusinessService;
  private final JsonMapper objectMapper;

  public KafkaListenerTopicThree(
      FieldLevelDecryptionService decryptionService,
      SomeBusinessService someBusinessService,
      JsonMapper objectMapper) {
    this.decryptionService = decryptionService;
    this.someBusinessService = someBusinessService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = {"${app.topic.three.name}"})
  public void onMessage(
      @Payload(required = false) String payload,
      @Header(name = "kafka_receivedTopic") String kafkaTopicName) throws JacksonException {

    // parse payload
    DataJsonDto dataJsonDto = objectMapper.readValue(payload, DataJsonDto.class);

    // decrypt fields
    String plainData = decryptionService.decryptFieldValue(kafkaTopicName, dataJsonDto.data());

    // call some business service or use-case
    someBusinessService.doSomething(plainData);

    log.info("received TopicThree event: {} / realValue: {}", payload, plainData);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DataJsonDto(@JsonProperty("data") String data) {

  }
}
