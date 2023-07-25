package org.vividus.plugin.jira.exception;

import org.vividus.plugin.jira.exporter.Constants;

public final class NonTestCaseIdException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public NonTestCaseIdException() {
    super(String.format("Skip export of scenario without '@%s' Meta", Constants.Meta.TEST_CASE_ID));
  }
}
