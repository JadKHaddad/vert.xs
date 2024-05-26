package com.verticles;

import com.beust.jcommander.IDefaultProvider;

// TODO: use the EnvironmentVariableDefaultProvider
public class DefaultProvider implements IDefaultProvider {
  @Override
  public String getDefaultValueFor(String s) {
    if (s.equals("-api-keys")) {
      return "key1,key2";
    }

    if (s.equals("-users")) {
      return "user1:password1,user2:password2";
    }

    return null;
  }
}
