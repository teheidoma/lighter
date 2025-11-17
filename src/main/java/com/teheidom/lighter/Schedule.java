package com.teheidom.lighter;

import java.time.ZonedDateTime;
import java.util.List;

public record Schedule(
    String city,
    ZonedDateTime updatedAt,
    List<Queue> queues) {

  public record Queue(
      String queue,
      List<String> intervals) {
  }
}
