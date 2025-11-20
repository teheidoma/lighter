package com.teheidom.lighter;

public record CountdownResponse(
    String queue,
    String currentStatus,
    String nextEvent,
    long secondsUntilNextEvent,
    String nextEventTime
) {
}
