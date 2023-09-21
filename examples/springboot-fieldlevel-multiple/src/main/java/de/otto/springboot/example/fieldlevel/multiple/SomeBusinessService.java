package de.otto.springboot.example.fieldlevel.multiple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SomeBusinessService {

  void doSomething(String data) {
    log.debug("data: {}", data);
  }
}
