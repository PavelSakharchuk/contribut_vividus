package org.vividus.plugin.jira.facade;

import java.util.List;
import java.util.stream.Collectors;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Step;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.model.TestCaseType;

public class TestCaseParameters {
  private String testCaseId;
  private List<StoryParameters> stories;

  public TestCaseParameters(String testCaseId) {
    this.testCaseId = testCaseId;
  }

  public String getTestCaseId() {
    return testCaseId;
  }

  public void setTestCaseId(String testCaseId) {
    this.testCaseId = testCaseId;
  }

  public List<StoryParameters> getStories() {
    return stories;
  }

  public void setStories(List<StoryParameters> stories) {
    this.stories = stories;
  }
}
