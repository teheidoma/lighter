package com.teheidom.lighter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
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

}
