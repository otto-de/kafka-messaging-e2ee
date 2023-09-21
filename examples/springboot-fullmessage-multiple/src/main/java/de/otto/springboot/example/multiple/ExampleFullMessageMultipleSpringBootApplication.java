package de.otto.springboot.example.multiple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExampleFullMessageMultipleSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleFullMessageMultipleSpringBootApplication.class, args);
  }
}
