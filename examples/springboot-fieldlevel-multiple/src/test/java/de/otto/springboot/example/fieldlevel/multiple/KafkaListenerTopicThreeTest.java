package de.otto.springboot.example.fieldlevel.multiple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.kafka.messaging.e2ee.EncryptionKeyProvider;
import de.otto.kafka.messaging.e2ee.fieldlevel.FieldLevelDecryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaListenerTopicThreeTest {

  private KafkaListenerTopicThree kafkaListener;

  @Mock
  private SomeBusinessService someBusinessService;
  @Mock
  private EncryptionKeyProvider encryptionKeyProvider;

  @BeforeEach
  void setup() {
    FieldLevelDecryptionService fieldLevelDecryptionService = new FieldLevelDecryptionService(
        encryptionKeyProvider);
    ObjectMapper objectMapper = new ObjectMapper();
    kafkaListener = new KafkaListenerTopicThree(fieldLevelDecryptionService, someBusinessService,
        objectMapper);
  }

  @Test
  void shouldDoSomethingWhenValueIsNotEncrypted() throws JsonProcessingException {
    // given: a valid kafka event
    String event = """
        {
          "data": "foo"
        }
        """;
    // when: listener is called
    kafkaListener.onMessage(event, "someTopic");
    // then: someBusinessService should have been called
    ArgumentCaptor<String> dataArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(someBusinessService).doSomething(dataArgCaptor.capture());
    assertThat(dataArgCaptor.getValue()).isEqualTo("foo");
    // then: encryptionKeyProvider was not used
    verifyNoInteractions(encryptionKeyProvider);
  }

  @Test
  void shouldDoSomethingWhenValueIsEncrypted() throws JsonProcessingException {
    // given: a valid kafka event
    String event = """
        {
          "data": "encAesV1.37.5/8tG8K/WNMEcjy9.wXOiUOdUOS6Lbb6xpMnn6s4d30Rl9jcXN5fb+Y28dSlwjZRDXK9IrgBZOKG9e9mzGVOFktMR2a3gGZ8="
        }
        """;
    // given: a valid encryption key
    when(encryptionKeyProvider.retrieveKeyForDecryption(eq("someTopic"), eq(37)))
        .thenReturn("fYpQkfYkdgLBpbMAqfoHxzFvB03Liy4XpvWznmCgmSg%3D%0A");
    // when: listener is called
    kafkaListener.onMessage(event, "someTopic");
    // then: someBusinessService should have been called
    ArgumentCaptor<String> dataArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(someBusinessService).doSomething(dataArgCaptor.capture());
    assertThat(dataArgCaptor.getValue()).isEqualTo("Some TopicThree my data at 18:02:37.668221.");
  }
}