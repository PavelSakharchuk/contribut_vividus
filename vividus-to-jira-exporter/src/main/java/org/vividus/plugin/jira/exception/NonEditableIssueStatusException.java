package org.vividus.plugin.jira.exception;

public final class NonEditableIssueStatusException extends Exception {

  private static final long serialVersionUID = -5547086076322794984L;

  public NonEditableIssueStatusException(String testCaseId, String status) {
    super("Issue " + testCaseId + " is in non-editable '" + status + "' status");
  }
}
