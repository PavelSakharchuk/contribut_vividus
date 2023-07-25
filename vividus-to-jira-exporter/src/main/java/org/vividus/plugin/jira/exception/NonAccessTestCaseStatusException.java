package org.vividus.plugin.jira.exception;

import org.vividus.plugin.jira.model.TestCaseStatus;

public final class NonAccessTestCaseStatusException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public NonAccessTestCaseStatusException(String testCaseId, TestCaseStatus status) {
    super(String.format("'%s' status can not be applied for %s Test Case", status, testCaseId));
  }
}
