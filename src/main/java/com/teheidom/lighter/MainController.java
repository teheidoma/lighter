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

  @GetMapping(value = "/health", produces = "application/json")
  public String health() {
    return "{\"status\":\"UP\"}";
  }

  @GetMapping(value = "/list", produces = "application/json")
  public List<String> list() {
    return providers.stream()
        .map(Provider::getName)
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/providers/{id}", produces = "application/json")
  public Object getProvider(@PathVariable String id) {
    return providers.stream()
        .filter(provider -> provider.getName().equals(id))
        .findFirst()
        .map(Provider::getData)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Provider not found: " + id));
  }

  @GetMapping(value = "/remaining/{id}/{group}")
  public Object getRemaining(@PathVariable String id, @PathVariable String group) {
    return providers.stream()
        .filter(provider -> provider.getName().equals(id))
        .findFirst()
        .map(provider -> (Schedule) provider.getData())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Provider not found: " + id));
  }

  @GetMapping(value = "/countdown/{providerId}/{queueNumber}", produces = "application/json")
  public CountdownResponse getCountdown(@PathVariable String providerId, @PathVariable String queueNumber) {
    Object data = providers.stream()
        .filter(provider -> provider.getName().equals(providerId))
        .findFirst()
        .map(Provider::getData)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Provider not found: " + providerId));

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
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Queue not found: " + queueNumber));

    LocalTime now = LocalTime.now();
    
    List<TimeInterval> intervals = targetQueue.intervals().stream()
        .map(TimeInterval::parse)
        .sorted((a, b) -> a.start().compareTo(b.start()))
        .toList();

    boolean isPowerOff = false;
    LocalTime nextEventTime = null;
    String nextEvent = null;

    for (TimeInterval interval : intervals) {
      boolean isInInterval;
      if (interval.end().isBefore(interval.start())) {
        isInInterval = now.isAfter(interval.start()) || now.isBefore(interval.end());
      } else {
        isInInterval = !now.isBefore(interval.start()) && now.isBefore(interval.end());
      }

      if (isInInterval) {
        isPowerOff = true;
        nextEventTime = interval.end();
        nextEvent = "POWER_ON";
        break;
      } else if (now.isBefore(interval.start())) {
        nextEventTime = interval.start();
        nextEvent = "BLACKOUT_START";
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
      try {
        String[] parts = interval.split("-");
        if (parts.length != 2) {
          throw new IllegalArgumentException("Invalid interval format: " + interval);
        }
        
        LocalTime start = parseTime(parts[0].trim());
        LocalTime end = parseTime(parts[1].trim());
        return new TimeInterval(start, end);
      } catch (Exception e) {
        throw new IllegalArgumentException("Failed to parse interval: " + interval, e);
      }
    }
    
    private static LocalTime parseTime(String time) {
      try {
        return LocalTime.parse(time + ":00", DateTimeFormatter.ofPattern("HH:mm"));
      } catch (Exception e) {
        return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
      }
    }
  }

}
