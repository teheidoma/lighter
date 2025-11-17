package com.teheidom.lighter;

import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LighterApplication {

  public static void main(String[] args) {
    SpringApplication.run(LighterApplication.class, args);
  }

}
