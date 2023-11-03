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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.output.OutputReader;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exception.JiraSkipExportMetaException;
import org.vividus.plugin.jira.exception.NonTestCaseIdException;
import org.vividus.plugin.jira.exporter.Constants.Meta;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;
import org.vividus.plugin.jira.log.ExporterStatisticLogger;

@Component
public class JiraExporter {

  @Autowired
  private JiraExporterOptions jiraExporterOptions;
  @Autowired
  private JiraInfoExporter jiraInfoExporter;
  @Autowired
  private JiraStatusExporter jiraStatusExporter;
  @Autowired
  private JiraExporterErrorCollection jiraExporterErrorCollection;

  public void exportResults() throws IOException {
    System.out.println("PATH: " + jiraExporterOptions.getJsonResultsDirectory());
    List<VividusScenarioInfo> vividusScenarioInfoList =
        OutputReader.readStoriesFromJsons(jiraExporterOptions.getJsonResultsDirectory()).stream()
            .flatMap(this::getVividusScenarioInfoStream)
            .collect(Collectors.toList());

    Map<TestCaseInfo, List<VividusScenarioInfo>> vividusScenarioMap = vividusScenarioInfoList.stream()
            .filter(vividusScenarioInfo -> Objects.nonNull(vividusScenarioInfo.getTestCaseId()))
            .filter(vividusScenarioInfo ->
                !checkIfJiraSkipExportMeta(vividusScenarioInfo.getTestCaseId(), vividusScenarioInfo))
            .collect(Collectors.groupingBy(VividusScenarioInfo::getTestCase));

    jiraInfoExporter.exportInfoTestCases(vividusScenarioMap);
    jiraStatusExporter.exportStatusTestCases(vividusScenarioMap);

    jiraExporterErrorCollection.publishErrors();
    new ExporterStatisticLogger(jiraExporterOptions, vividusScenarioInfoList).logTestExecutionResults();
  }

  private Stream<VividusScenarioInfo> getVividusScenarioInfoStream(Story story) {
    story.setPath(trimPath("/story", story.getPath()));
    return story.getFoldedScenarios().stream()
        .flatMap(scenario -> getVividusScenarioInfoStream(story, scenario));
  }

  private Stream<VividusScenarioInfo> getVividusScenarioInfoStream(Story story, Scenario scenario) {
    return scenario.getMetaValues(Constants.Meta.TEST_CASE_ID).isEmpty() ?
        Stream.of(getVividusScenarioInfoWithoutTestCaseId(story, scenario)) :
        scenario.getMetaValues(Meta.TEST_CASE_ID).stream()
            .map(testCaseId -> new VividusScenarioInfo(testCaseId, story, scenario));
  }

  private VividusScenarioInfo getVividusScenarioInfoWithoutTestCaseId(Story story, Scenario scenario) {
    VividusScenarioInfo vividusScenarioInfo = new VividusScenarioInfo(null, story, scenario);
    jiraExporterErrorCollection.addLogReaderError(new NonTestCaseIdException(), null, vividusScenarioInfo);
    return vividusScenarioInfo;
  }

  private boolean checkIfJiraSkipExportMeta(String testCaseId, VividusScenarioInfo vividusScenarioInfo) {
    Story story = vividusScenarioInfo.getStory();
    Scenario scenario = vividusScenarioInfo.getScenario();
    if (scenario.hasMetaWithName(Constants.Meta.JIRA_SKIP_EXPORT)) {
      jiraExporterErrorCollection.addLogReaderError(
          new JiraSkipExportMetaException(testCaseId, story, scenario),
          testCaseId, vividusScenarioInfo);
      return true;
    }
    return false;
  }

  private static String trimPath(String startFragment, String path) {
    int startIndex = path.indexOf(startFragment);
    return startIndex < 0 ? path : path.substring(startIndex);
  }
}
