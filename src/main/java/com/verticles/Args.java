package com.verticles;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parameters(separators = "=")
public class Args {
  @Parameter(names = "-api-keys", description = "Comma-separated list of API keys")
  private String apiKeys;

  @Parameter(names = "-users", description = "Comma-separated list of user credentials in the format username:password")
  private String users;

  // TODO: add custom parser
  public List<String> getApiKeys() {
    return List.of(this.apiKeys.split(","));
  }

  // TODO: add custom parser
  public Map<String, String> getUsers() {
    List<String> users = List.of(this.users.split(","));

    return users.stream()
      .map(user -> user.split(":"))
      .collect(
        java.util.stream.Collectors.toMap(
          user -> user[0],
          user -> user[1]
        )
      );
  }
}
