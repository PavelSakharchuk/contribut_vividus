package org.vividus.plugin.jira.exception;

public final class NonEditableTestRunException extends Exception {

  private static final long serialVersionUID = -5547086076322794984L;

  public NonEditableTestRunException(String testRunId, Exception e) {
    super(String.format("%s Test Run is non-editable: %s", testRunId, e.getMessage()));
  }
}
