/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.plugin.jira.exporter.model;

import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.exception.ExceptionType;

public class VividusScenarioInfo {

  private TestCaseInfo testCase;
  private Story story;
  private Scenario scenario;
  private TestCaseStatus scenarioStatus;
  private ExceptionType exceptionType;

  public VividusScenarioInfo(String testCaseId, Story story, Scenario scenario) {
    this.testCase = new TestCaseInfo(testCaseId);
    this.story = story;
    this.scenario = scenario;
  }

  public TestCaseInfo getTestCase() {
    return testCase;
  }

  public void setTestCase(TestCaseInfo testCase) {
    this.testCase = testCase;
  }

  public String getTestCaseId() {
    return testCase.getTestCaseId();
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

  public TestCaseStatus getScenarioStatus() {
    return scenarioStatus;
  }

  public void setScenarioStatus(TestCaseStatus scenarioStatus) {
    this.scenarioStatus = scenarioStatus;
  }

  public ExceptionType getExceptionType() {
    return exceptionType;
  }

  public void setExceptionType(ExceptionType exceptionType) {
    this.exceptionType = exceptionType;
  }
}
