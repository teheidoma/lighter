package com.teheidom.lighter.provider;

import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;

import com.teheidom.lighter.Schedule.Queue;

import lombok.SneakyThrows;

@Component
public class KiroeProvider implements Provider {
  private static final String CHANNEL_NAME = "SvitloKropyvnytskyiMisto";
  private String lastHeader = "";
  private List<Queue> lastQueues = null;
  private static final Pattern MAIN_PATTERN = Pattern.compile("Черга (\\d.\\d\\:(?:\\W\\d{2}-\\d{2},?)+)");

  @Override
  public String getName() {
    return "KiroeProvider";
  }

  @Override
  @SneakyThrows
  public Object getData() {
    var doc = Jsoup.connect("https://kiroe.com.ua")
        .timeout(5_000)
        .get();
    var header = doc.select(".fancybox_header > span").text();
    if (header.equals(lastHeader)) {
      return lastQueues;
    } else {
      lastHeader = header;
      var body = doc.select(".fancybox_body_desc").text();
      var queues = MAIN_PATTERN.matcher(body).results()
          .map(match -> match.group(1))
          .map(entry -> new Queue(
              entry.split(":")[0],
              List.of(entry.substring(entry.indexOf(":") + 1).replaceAll("\\s", "").split(","))))
          .toList();

      lastQueues = queues;
      return queues;
    }
  }

}
