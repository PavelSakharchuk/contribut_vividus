package org.vividus.plugin.jira.exporter.model;

import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;

public class VividusScenarioInfo {
  private String testCaseId;
  private Story story;
  private Scenario scenario;

  public VividusScenarioInfo(String testCaseId, Story story, Scenario scenario) {
    this.testCaseId = testCaseId;
    this.story = story;
    this.scenario = scenario;
  }

  public String getTestCaseId() {
    return testCaseId;
  }

  public void setTestCaseId(String testCaseId) {
    this.testCaseId = testCaseId;
  }

  public Story getStory() {
    return story;
  }

  public void setStory(Story story) {
    this.story = story;
  }

  public Scenario getScenario() {
    return scenario;
  }

  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }
}
