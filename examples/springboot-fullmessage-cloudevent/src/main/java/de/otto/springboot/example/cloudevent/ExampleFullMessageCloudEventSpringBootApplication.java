package de.otto.springboot.example.cloudevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExampleFullMessageCloudEventSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleFullMessageCloudEventSpringBootApplication.class, args);
  }
}
