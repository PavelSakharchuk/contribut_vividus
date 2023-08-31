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

package org.vividus.plugin.jira.exporter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.jira.model.IssueTransitionStatus;
import org.vividus.model.jbehave.AbstractStepsContainer;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.StepStatus;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exception.NonAccessTestCaseStatusException;
import org.vividus.plugin.jira.exception.NonEditableIssueStatusException;
import org.vividus.plugin.jira.exception.NonEditableTestRunException;
import org.vividus.plugin.jira.exception.NonTestCaseWithinRunException;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;
import org.vividus.plugin.jira.facade.JiraExporterFacade;
import org.vividus.plugin.jira.exporter.model.TestCaseStatus;

@Component
public class JiraStatusExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiraStatusExporter.class);

  @Autowired
  private JiraExporterOptions jiraExporterOptions;
  @Autowired
  private JiraExporterFacade jiraExporterFacade;
  @Autowired
  private JiraExporterErrorCollection jiraExporterErrorCollection;

  public void exportStatusTestCases(
      Map<TestCaseInfo, List<VividusScenarioInfo>> multiTestCaseMap) {
    if (jiraExporterOptions.isTestCaseStatusUpdatesEnabled()) {
      multiTestCaseMap.entrySet().parallelStream()
          .forEach(entry -> exportTestCaseStatus(entry.getKey(), entry.getValue()));
    } else {
      LOGGER.atInfo().log("Test Case Statuses Exporting is switched off");
    }
  }

  private void exportTestCaseStatus(
      TestCaseInfo testCaseInfo, List<VividusScenarioInfo> vividusScenarioInfoList) {
    String testCaseId = testCaseInfo.getTestCaseId();
    LOGGER.atInfo().addArgument(testCaseId).log("Exporting Status: {}");
    try {
      TestCaseStatus testCaseStatus = calculateTestCaseStatus(vividusScenarioInfoList);
      IssueTransitionStatus jiraTransitionStatus = jiraExporterFacade.getTransitionStatus(testCaseInfo, testCaseStatus);
      jiraExporterFacade.updateStatus(testCaseId, jiraTransitionStatus);
      vividusScenarioInfoList
          .forEach(vividusScenarioInfo -> vividusScenarioInfo.getTestCase().setTestCaseStatus(testCaseStatus));
    } catch (IOException | JiraConfigurationException | NonEditableTestRunException | NonTestCaseWithinRunException
             | NonEditableIssueStatusException | NonAccessTestCaseStatusException e) {
      vividusScenarioInfoList.forEach(multiTestCase -> jiraExporterErrorCollection
          .addLogTestCaseStatusExportError(e, testCaseId, multiTestCase.getStory(), multiTestCase.getScenario()));
    }
  }

  private TestCaseStatus calculateTestCaseStatus(List<VividusScenarioInfo> vividusScenarioInfoList) {
    List<TestCaseStatus> scenarioStatuses = vividusScenarioInfoList.stream()
        .map(vividusScenarioInfo -> {
          TestCaseStatus scenarioStatus = calculateScenarioStatus(vividusScenarioInfo.getScenario());
          vividusScenarioInfo.setScenarioStatus(scenarioStatus);
          return scenarioStatus;
        })
        .collect(Collectors.toList());

    return scenarioStatuses.stream()
        .filter(scenarioStatus -> scenarioStatus == TestCaseStatus.FAILED)
        .findFirst()
        .orElse(TestCaseStatus.PASSED);
  }

  private TestCaseStatus calculateScenarioStatus(Scenario scenario) {
    if (scenario.getExamples() == null) {
      return calculateStatus(scenario);
    } else {
      List<TestCaseStatus> exampleStatuses =
          scenario.getExamples().getExamples().stream()
              .map(JiraStatusExporter::calculateStatus)
              .collect(Collectors.toList());

      return exampleStatuses.stream()
          .filter(ts -> ts == TestCaseStatus.FAILED)
          .findFirst()
          .orElse(TestCaseStatus.PASSED);
    }
  }

  private static TestCaseStatus calculateStatus(AbstractStepsContainer scenario) {
    return scenario.createStreamOfAllSteps().anyMatch(step ->
        Arrays.asList(StepStatus.FAILED.getName(), StepStatus.NOT_PERFORMED.getName())
            .contains(step.getOutcome()))
        ? TestCaseStatus.FAILED
        : TestCaseStatus.PASSED;
  }
}
