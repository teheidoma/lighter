package com.teheidom.lighter.provider;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.Context;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.teheidom.lighter.Schedule;
import com.teheidom.lighter.Schedule.Queue;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public abstract class TelegramProvider implements Provider {
  private static final String BASE_URL = "https://t.me/s/";
  private final OllamaChatModel ollama;
  private final String channelName;
  private final ObjectMapper mapper = new JsonMapper();

  private String basePrompt = """

      You are an expert in text parsing and data extraction.

      Your task:
      - Read the text I provide.
      - Find all schedule entries that follow the pattern: "Черга X.Y: HH-HH, HH-HH, ..."
      - Extract:
        - queue number (example: "1.1", "4.2", "6.1")
        - list of all time intervals after the colon

      Output:
      - Only a JSON array in the following format:
      [
        {
          "queue": "1.1",
          "intervals": ["00-02", "04-06", ...]
        },
        ...
      ]

      Rules:
      - Do not include explanations.
      - Do not include text outside JSON.
      - If the text contains unrelated words, emojis, links, or noise, ignore them.

      Here is the text:


                  """;

  @Setter
  private Function<String, String> extractor = Function.identity();

  @Override
  @SneakyThrows
  public Object getData() {
    var doc = Jsoup.connect(BASE_URL + channelName).get();
    Context context = Context.newBuilder("python").build();

    String text = doc.select(".js-widget_message").last().text();
    var rawResponse = ollama.call(new SystemMessage(basePrompt),
        new UserMessage(text));
    var response = mapper.readValue(rawResponse, Queue[].class);
    return new Schedule(channelName, ZonedDateTime.now(), List.of(response));
  }

  @Override
  public String getName() {
    return channelName;
  }

}
