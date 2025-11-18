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

      You are an expert in Python text parsing.

      Your task: generate a complete Python 3.11 script that extracts schedule information from a Telegram text message.

      The script will be evaluated inside GraalPython, so it must obey the following strict rules:

      REQUIREMENTS:
      1. The script must use ONLY built-in modules: `re`, `json`.
      2. The script must NOT import anything else.
      3. The script must NOT print anything.
      4. The script must NOT define a main() or call functions.
      5. The script must NOT use "return" at top level.
      6. The script must create a variable named RESULT containing a JSON string.
      7. The script must assume the variable `text` already exists and contains the message to parse.
      8. The script must extract all schedule entries of the form:
           Черга X.Y: HH-HH, HH-HH, ...
         Where:
           - X.Y is the queue number (e.g. "1.1", "2.2")
           - Intervals are a list of "HH-HH" items
      9. The script must produce Python data in this form (full JSON example):

      [
        {"queue": "1.1", "intervals": ["00-02", "04-06", "08-10"]},
        {"queue": "2.2", "intervals": ["01-03", "05-07"]},
        {"queue": "3.1", "intervals": ["02-04", "06-08", "10-12"]}
      ]

      10. Finally, the script must assign:
           RESULT = json.dumps(parsed_data, ensure_ascii=False)

      OUTPUT RULES:
      - Respond with ONLY the Python script.
      - Do NOT wrap it in Markdown.
      - Do NOT include explanations.
      - Do NOT include backticks.
      - Do NOT include comments.
      - The output must be a valid script ready for execution.

      Here is the text that will be available in a variable named `text`:
                                    """;

  @Setter
  private Function<String, String> extractor = Function.identity();

  @Override
  @SneakyThrows
  public Object getData() {
    var doc = Jsoup.connect(BASE_URL + channelName).get();

    String text = doc.select(".js-widget_message").last().text();
    var rawResponse = ollama.call(new SystemMessage(basePrompt),
        new UserMessage(text));
    System.out.println("Raw response: " + rawResponse);
    try (Context context = Context.newBuilder("python")
        .allowAllAccess(true)
        .build()) {
      context.getBindings("python").putMember("text", text);
      context.eval("python", rawResponse);
      String asString = context.getBindings("python").getMember("RESULT").asString();
      System.out.println("Extracted JSON: " + asString);
      var response = mapper.readValue(asString, Queue[].class);
      return new Schedule(channelName, ZonedDateTime.now(), List.of(response));
    }
  }

  @Override
  public String getName() {
    return channelName;
  }

}
