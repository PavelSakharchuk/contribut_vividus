/*
 * Copyright 2019-2020 the original author or authors.
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

package org.vividus.plugin.jira.factory;

import static java.lang.System.lineSeparator;

import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.facade.AbstractScenarioParameters;
import org.vividus.plugin.jira.facade.CucumberScenarioParameters;
import org.vividus.plugin.jira.facade.ManualScenarioParameters;
import org.vividus.plugin.jira.facade.StoryParameters;
import org.vividus.plugin.jira.facade.TestCaseParameters;
import org.vividus.plugin.jira.model.TestCaseType;
import org.vividus.plugin.jira.model.jira.AbstractTestCase;
import org.vividus.plugin.jira.model.jira.CucumberTestCase;
import org.vividus.plugin.jira.model.jira.ManualTestCase;
import org.vividus.plugin.jira.model.jira.TestCase;

@Component
public class TestCaseFactory {

  @Autowired
  private JiraExporterOptions jiraExporterOptions;

  public TestCase createTestCase(TestCaseParameters parameters) {
    TestCase testCase = new TestCase();
    // TODO: Add to TestCaseParameters and other classes isManual field
    testCase.setTestCaseId(parameters.getTestCaseId());
    testCase.setType(TestCaseType.AUTOMATED.getValue());
    testCase.setProjectKey(jiraExporterOptions.getProjectKey());
    testCase.setAssigneeId(jiraExporterOptions.getAssigneeId());
    // TODO: Need to think about Labels
    // TODO: Need to think about Components
    // TODO: Summary will be skipped
//        testCase.setSummary(parameters.getSummary());
//        testCase.setLabels(parameters.getLabels());
//        testCase.setComponents(parameters.getComponents());
    testCase.setDescription(generateTestCaseDescription(parameters));
    return testCase;
  }

  public ManualTestCase createManualTestCase(ManualScenarioParameters parameters) {
    ManualTestCase testCase = new ManualTestCase();
    fillTestCase(parameters, testCase);
    testCase.setManualTestSteps(parameters.getSteps());
    return testCase;
  }

  public CucumberTestCase createCucumberTestCase(CucumberScenarioParameters parameters) {
    CucumberTestCase testCase = new CucumberTestCase();
    fillTestCase(parameters, testCase);
    testCase.setScenarioType(parameters.getScenarioType());
    testCase.setScenario(parameters.getScenario());
    return testCase;
  }

  private void fillTestCase(AbstractScenarioParameters parameters, AbstractTestCase testCase) {
    testCase.setType(parameters.getType().getValue());
    testCase.setProjectKey(jiraExporterOptions.getProjectKey());
    testCase.setAssigneeId(jiraExporterOptions.getAssigneeId());
    testCase.setSummary(parameters.getSummary());
    testCase.setLabels(parameters.getLabels());
    testCase.setComponents(parameters.getComponents());
  }

  private String generateTestCaseDescription(TestCaseParameters testCaseParameters) {
    return testCaseParameters.getStories().stream()
        .map(this::generateStoryDescription)
        .collect(Collectors.joining(System.lineSeparator() + "----" + System.lineSeparator()));
  }

  private String generateStoryDescription(StoryParameters storyParameters) {
    String givenStoryDescription = storyParameters.getGivenStories().stream()
        .map(this::generateGivenStoryDescription)
        .collect(Collectors.joining(lineSeparator()));

    String scenariosDescription = storyParameters.getScenarios().stream()
        .map(scenario -> generateScenarioDescription((CucumberScenarioParameters) scenario))
        .collect(Collectors.joining(lineSeparator()));

    StringJoiner stringJoiner = new StringJoiner(lineSeparator())
        .add(String.format("*Story: %s*", storyParameters.getPath()));
    if (StringUtils.isNotBlank(givenStoryDescription)) {
      stringJoiner
          .add(givenStoryDescription);
    }
    stringJoiner
        .add(System.lineSeparator())
        .add(scenariosDescription)
        .add(System.lineSeparator());
    return stringJoiner.toString();
  }

  private String generateGivenStoryDescription(StoryParameters givenStoryParameters) {
    String givenScenarioDescription = givenStoryParameters.getScenarios().stream()
        .map(givenScenario -> generateGivenScenarioDescription(givenStoryParameters, (CucumberScenarioParameters) givenScenario))
        .collect(Collectors.joining(lineSeparator()));

    return new StringJoiner(System.lineSeparator())
        .add(givenScenarioDescription)
        .add(System.lineSeparator())
        .toString();
  }

  private String generateGivenScenarioDescription(StoryParameters givenStoryParameters, CucumberScenarioParameters givenScenarioParameters) {
    return new StringJoiner(System.lineSeparator())
        .add(String.format("*Given Story: %s*", givenStoryParameters.getPath()))
        .add("*Scenario:* " + givenScenarioParameters.getSummary())
        .add("{noformat}")
        .add(givenScenarioParameters.getScenario())
        .add("{noformat}")
        .toString();
  }

  private String generateScenarioDescription(CucumberScenarioParameters scenarioParameters) {
    String givenStoryDescription = scenarioParameters.getGivenStories().stream()
        .map(this::generateGivenStoryDescription)
        .collect(Collectors.joining(lineSeparator()));

    StringJoiner stringJoiner = new StringJoiner(lineSeparator());
    if (StringUtils.isNotBlank(givenStoryDescription)) {
      stringJoiner
          .add(givenStoryDescription);
    }
    stringJoiner
        .add("*Scenario:* " + scenarioParameters.getSummary())
        .add("{noformat}")
        .add(scenarioParameters.getScenario())
        .add("{noformat}")
        .add(System.lineSeparator());
    return stringJoiner.toString();
  }
}
