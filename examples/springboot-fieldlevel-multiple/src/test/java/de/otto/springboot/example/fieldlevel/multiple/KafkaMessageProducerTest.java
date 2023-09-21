package de.otto.springboot.example.fieldlevel.multiple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider;
import de.otto.messaging.kafka.e2ee.EncryptionKeyProvider.KeyVersion;
import de.otto.messaging.kafka.e2ee.EncryptionService;
import de.otto.messaging.kafka.e2ee.InitializationVectorFactory;
import de.otto.messaging.kafka.e2ee.fieldlevel.FieldLevelEncryptionService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaMessageProducerTest {

  private static final String TOPIC_NAME = "someTopic";

  private KafkaMessageProducer messageProducer;
  @Mock
  private EncryptionKeyProvider encryptionKeyProvider;
  @Mock
  private InitializationVectorFactory initializationVectorFactory;
  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @BeforeEach
  void setup() {
    Clock clock = Clock.fixed(OffsetDateTime.parse("2023-09-12T18:02:37.668221Z").toInstant(),
        ZoneId.of("UTC"));
    ObjectMapper objectMapper = new ObjectMapper();
    FieldLevelEncryptionService fieldLevelEncryptionService = new FieldLevelEncryptionService(
        new EncryptionService(encryptionKeyProvider, initializationVectorFactory));
    messageProducer = new KafkaMessageProducer(clock, kafkaTemplate, fieldLevelEncryptionService,
        TOPIC_NAME, objectMapper);
  }

  @Test
  void shouldCreateEncryptedData() throws JsonProcessingException {
    // given: a valid encryption key and initialization vector
    when(encryptionKeyProvider.retrieveKeyForEncryption(eq(TOPIC_NAME)))
        .thenReturn(new KeyVersion(37, "fYpQkfYkdgLBpbMAqfoHxzFvB03Liy4XpvWznmCgmSg%3D%0A"));
    when(initializationVectorFactory.generateInitializationVector())
        .thenReturn(Base64.getDecoder().decode("5/8tG8K/WNMEcjy9"));
    // when: message producer is triggered
    messageProducer.onTickTopicThree();
    // then: expected message should have been created
    ArgumentCaptor<String> topicArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> partionKeyArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> dataArg = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate).send(topicArg.capture(), partionKeyArg.capture(), dataArg.capture());
    assertThat(topicArg.getValue()).describedAs("topic").isEqualTo(TOPIC_NAME);
    assertThat(partionKeyArg.getValue()).describedAs("partionKeyArg").isNotBlank();
    String expectedPayload = """
        {"data":"encAesV1.37.5/8tG8K/WNMEcjy9.wXOiUOdUOS6Lbb6xpMnn6s4d30Rl9jcXN5fb+Y28dSlwjZRDXK9IrgBZOKG9e9mzGVOFktMR2a3gGZ8="}""";
    assertThat(dataArg.getValue()).describedAs("dataArg").isEqualTo(expectedPayload);
  }
}