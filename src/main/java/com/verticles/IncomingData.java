package com.verticles;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IncomingData(
  @JsonProperty(required = true)
  String name,
  @JsonProperty(required = true)
  String email,
  @JsonProperty(required = true)
  Integer age) {
}
