package com.hugoruiz.models;

public class VariableDetail {
  private String name;
  private String value;
  private String method;

  public VariableDetail(String name, String value, String method) {
    this.name = name;
    this.value = value;
    this.method = method;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getMethod() {
    return method;
  }
}
