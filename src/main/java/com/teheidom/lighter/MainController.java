package com.teheidom.lighter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.teheidom.lighter.Schedule.Queue;

import com.teheidom.lighter.provider.Provider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MainController {
  private final List<Provider> providers;

  @GetMapping(value = "/list", produces = "application/json")
  public List<String> home() {
    return providers.stream()
        .map(Provider::getName)
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/{id}", produces = "application/json")
  public Object home(@PathVariable String id) {
    return providers.stream()
        .filter(provider -> provider.getName().equals(id))
        .findFirst()
        .map(Provider::getData)
        .orElseThrow();
  }

  @GetMapping(value = "/remaining/{id}/{group}")
  public Object getRemaining(@PathVariable String id, @PathVariable String group) {
    return providers.stream()
        .filter(provider -> provider.getName().equals(id))
        .findFirst()
        .map(provider -> (Schedule) provider.getData())
        .orElseThrow();
  }

  @GetMapping(value = "/countdown/{providerId}/{queueNumber}", produces = "application/json")
  public CountdownResponse getCountdown(@PathVariable String providerId, @PathVariable String queueNumber) {
    Object data = providers.stream()
        .filter(provider -> provider.getName().equals(providerId))
        .findFirst()
        .map(Provider::getData)
        .orElseThrow(() -> new RuntimeException("Provider not found"));

    List<Queue> queues;
    if (data instanceof List<?>) {
      queues = ((List<?>) data).stream()
          .filter(item -> item instanceof Queue)
          .map(item -> (Queue) item)
          .toList();
    } else if (data instanceof Schedule) {
      queues = ((Schedule) data).queues();
    } else {
      throw new RuntimeException("Unexpected data format");
    }

    Queue targetQueue = queues.stream()
        .filter(q -> q.queue().equals(queueNumber))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Queue not found: " + queueNumber));

    LocalTime now = LocalTime.now();
    boolean isPowerOff = false;
    LocalTime nextEventTime = null;
    String nextEvent = null;

    List<TimeInterval> intervals = targetQueue.intervals().stream()
        .map(TimeInterval::parse)
        .sorted((a, b) -> a.start().compareTo(b.start()))
        .toList();

    for (TimeInterval interval : intervals) {
      if (now.isBefore(interval.start())) {
        if (nextEventTime == null) {
          nextEventTime = interval.start();
          nextEvent = isPowerOff ? "POWER_ON" : "BLACKOUT_START";
        }
        break;
      } else if (now.isBefore(interval.end()) || (interval.end().isBefore(interval.start()) && (now.isAfter(interval.start()) || now.isBefore(interval.end())))) {
        isPowerOff = true;
        if (interval.end().isBefore(interval.start())) {
          nextEventTime = interval.end();
        } else {
          nextEventTime = interval.end();
        }
        nextEvent = "POWER_ON";
        break;
      }
    }

    if (nextEventTime == null && !intervals.isEmpty()) {
      nextEventTime = intervals.get(0).start();
      nextEvent = "BLACKOUT_START";
    }

    long secondsUntil = 0;
    if (nextEventTime != null) {
      if (nextEventTime.isAfter(now)) {
        secondsUntil = java.time.Duration.between(now, nextEventTime).getSeconds();
      } else {
        secondsUntil = java.time.Duration.between(now, nextEventTime.plusHours(24)).getSeconds();
      }
    }

    return new CountdownResponse(
        queueNumber,
        isPowerOff ? "POWER_OFF" : "POWER_ON",
        nextEvent,
        secondsUntil,
        nextEventTime != null ? nextEventTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "N/A"
    );
  }

  private record TimeInterval(LocalTime start, LocalTime end) {
    static TimeInterval parse(String interval) {
      String[] parts = interval.split("-");
      LocalTime start = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH"));
      LocalTime end = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH"));
      return new TimeInterval(start, end);
    }
  }

}
