package com.teheidom.lighter.provider;

import org.jsoup.Jsoup;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
public class KiroeProvider extends TelegramProvider {
  private static final String CHANNEL_NAME = "SvitloKropyvnytskyiMisto";

  public KiroeProvider(OllamaChatModel chat) {
    super(chat, CHANNEL_NAME);
  }

}
