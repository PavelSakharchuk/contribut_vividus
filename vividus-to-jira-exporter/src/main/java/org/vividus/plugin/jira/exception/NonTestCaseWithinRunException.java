package org.vividus.plugin.jira.exception;

import org.vividus.jira.model.JiraEntity;
import org.vividus.plugin.jira.exporter.Constants;

public final class NonTestCaseWithinRunException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public NonTestCaseWithinRunException(JiraEntity testRun, String targetInitialTestCaseId) {
    super(String.format("%s Test Run does not have TC with '%s' = %s",
        testRun.getKey(), Constants.JiraMappingProperties.INITIAL_TEST_CASE, targetInitialTestCaseId));
  }
}
