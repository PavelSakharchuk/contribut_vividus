package org.vividus.plugin.jira.exception;

import org.vividus.plugin.jira.model.TestCaseType;

public final class NonCucumberTypesException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public NonCucumberTypesException() {
    super(String.format("Scenario is not '%s' type", TestCaseType.AUTOMATED));
  }
}
