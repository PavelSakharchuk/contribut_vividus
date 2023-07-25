package org.vividus.plugin.jira.exception;

import org.vividus.jira.model.JiraEntity;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.exporter.Constants;

public final class JiraSkipExportMetaException extends Exception {

  private static final long serialVersionUID = -7652198889958024012L;

  public JiraSkipExportMetaException(String testCaseId, Story story, Scenario scenario) {
    super(String.format("Skip export of scenario ['%s' Initial Test Case] with '@%s' Meta: %s [%s]",
        testCaseId,
        Constants.Meta.JIRA_SKIP_EXPORT,
        scenario.getTitle(),
        story.getPath()));
  }
}
