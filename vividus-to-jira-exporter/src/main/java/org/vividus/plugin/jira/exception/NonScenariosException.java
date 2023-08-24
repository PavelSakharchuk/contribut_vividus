package org.vividus.plugin.jira.exception;

public final class NonScenariosException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public NonScenariosException() {
    super("Scenarios are not existed");
  }
}
