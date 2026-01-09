package de.otto.springboot.example.fieldlevel.multiple;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import de.otto.kafka.messaging.e2ee.fieldlevel.FieldLevelEncryptionService;
import de.otto.kafka.messaging.e2ee.fieldlevel.SingleTopicFieldLevelEncryptionService;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.message.producer.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMessageProducer {

  private final Clock clock;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final SingleTopicFieldLevelEncryptionService encryptionService;
  private final String topicThreeName;
  private final JsonMapper objectMapper;

  public KafkaMessageProducer(
      Clock clock,
      KafkaTemplate<String, String> kafkaTemplate,
      FieldLevelEncryptionService encryptionService,
      @Value("${app.topic.three.name}") String topicThreeName,
      JsonMapper objectMapper) {
    this.clock = clock;
    this.kafkaTemplate = kafkaTemplate;
    this.encryptionService = new SingleTopicFieldLevelEncryptionService(encryptionService,
        topicThreeName);
    this.topicThreeName = topicThreeName;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelay = 13, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
  public void onTickTopicThree() throws JacksonException {
    String msgPlain =
        "Some TopicThree my data at " + LocalTime.now(clock)
            .format(DateTimeFormatter.ISO_LOCAL_TIME)
            + ".";

    DataDto dataDto = DataDto.builder()
        .data(encryptionService.encryptFieldValueToString(msgPlain))
        .build();

    String payload = objectMapper.writeValueAsString(dataDto);
    publishKafkaMessage(topicThreeName, payload);
  }

  private void publishKafkaMessage(String topic, String data) {
    String partitionKey = UUID.randomUUID().toString();

    // publish kafka event
    kafkaTemplate.send(topic, partitionKey, data);
    log.info("created event for {}: {}", topic, data);
  }

  private record DataDto(@JsonProperty("data") String data) {

    @Builder
    DataDto {
    }
  }
}
