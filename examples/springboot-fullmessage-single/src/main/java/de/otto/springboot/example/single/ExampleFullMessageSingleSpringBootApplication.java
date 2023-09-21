package de.otto.springboot.example.single;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExampleFullMessageSingleSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleFullMessageSingleSpringBootApplication.class, args);
  }
}
