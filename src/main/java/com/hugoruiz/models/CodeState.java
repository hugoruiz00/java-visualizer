package com.hugoruiz.models;

import java.util.Map;

public class CodeState {
  private String method;
  private int lineNumber;
  private String changeDescription;
  private Map<String, VariableDetail> variables;

  public CodeState(String method, int lineNumber, String changeDescription, Map<String, VariableDetail> variables) {
    this.method = method;
    this.lineNumber = lineNumber;
    this.changeDescription = changeDescription;
    this.variables = variables;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getChangeDescription() {
    return changeDescription;
  }

  public void setChangeDescription(String changeDescription) {
    this.changeDescription = changeDescription;
  }

  public Map<String, VariableDetail> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, VariableDetail> variables) {
    this.variables = variables;
  }
}
