package de.otto.springboot.example.fieldlevel.multiple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExampleFieldLevelMultipleSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleFieldLevelMultipleSpringBootApplication.class, args);
  }
}
