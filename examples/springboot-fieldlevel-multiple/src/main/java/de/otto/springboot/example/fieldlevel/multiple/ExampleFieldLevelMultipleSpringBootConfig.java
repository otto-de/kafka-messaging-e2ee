package de.otto.springboot.example.fieldlevel.multiple;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExampleFieldLevelMultipleSpringBootConfig {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
