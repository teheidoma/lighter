package com.teheidom.lighter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teheidom.lighter.provider.KiroeProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MainController {
  private final KiroeProvider kiroeProvider;

  @GetMapping(value = "/", produces = "application/json")
  public Object home() {
    return kiroeProvider.getData();
  }

}
