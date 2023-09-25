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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.output.ManualStepConverter;
import org.vividus.output.SyntaxException;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter.CucumberScenario;
import org.vividus.plugin.jira.converter.GivenStoriesConverter;
import org.vividus.plugin.jira.exception.NonCucumberTypesException;
import org.vividus.plugin.jira.exception.NonEditableIssueStatusException;
import org.vividus.plugin.jira.exception.NonEditableTestRunException;
import org.vividus.plugin.jira.exception.NonScenariosException;
import org.vividus.plugin.jira.exception.NonTestCaseWithinRunException;
import org.vividus.plugin.jira.exception.NotSingleUniqueValueException;
import org.vividus.plugin.jira.exporter.Constants.Meta;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;
import org.vividus.plugin.jira.facade.AbstractScenarioParameters;
import org.vividus.plugin.jira.facade.CucumberScenarioParameters;
import org.vividus.plugin.jira.facade.JiraExporterFacade;
import org.vividus.plugin.jira.facade.ManualScenarioParameters;
import org.vividus.plugin.jira.facade.StoryParameters;
import org.vividus.plugin.jira.facade.TestCaseParameters;
import org.vividus.plugin.jira.factory.TestCaseFactory;
import org.vividus.plugin.jira.model.TestCaseType;
import org.vividus.plugin.jira.model.jira.AbstractTestCase;
import org.vividus.plugin.jira.model.jira.TestCase;

@Component
public class JiraInfoExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiraInfoExporter.class);

  @Autowired
  private JiraExporterOptions jiraExporterOptions;
  @Autowired
  private JiraExporterFacade jiraExporterFacade;
  @Autowired
  private TestCaseFactory testCaseFactory;
  @Autowired
  private JiraExporterErrorCollection jiraExporterErrorCollection;

  private final Map<TestCaseType, CreateParametersFunction> parameterFactories = Map.of(
      TestCaseType.MANUAL, this::createManualScenarioParameters,
      TestCaseType.AUTOMATED, this::createCucumberScenarioParameters
  );

  public void exportInfoTestCases(Map<TestCaseInfo, List<VividusScenarioInfo>> vividusScenarioInfoMap) {
    if (jiraExporterOptions.isTestCaseInfoUpdatesEnabled()) {
      vividusScenarioInfoMap.entrySet().parallelStream()
          .forEach(entry -> exportTestCaseInfo(entry.getKey(), entry.getValue()));
    } else {
      LOGGER.atInfo().log("Test Case Information Exporting is switched off");
    }
  }

  private void exportTestCaseInfo(
      TestCaseInfo testCaseInfo, List<VividusScenarioInfo> vividusScenarioInfoList) {
    String testCaseId = testCaseInfo.getTestCaseId();
    LOGGER.atInfo().addArgument(testCaseId).log("Exporting Test Case: {}");

    try {
      TestCase testCase = createTestCaseParameters(testCaseId, vividusScenarioInfoList);
      exportTestCase(testCase);
      vividusScenarioInfoList.forEach(vividusScenarioInfo -> vividusScenarioInfo.getTestCase().setUpdatedInfo(true));
    } catch (IOException | SyntaxException | NonCucumberTypesException | NonScenariosException
             | NotSingleUniqueValueException
             | NonEditableTestRunException | NonEditableIssueStatusException | NonTestCaseWithinRunException
             | JiraConfigurationException e) {
      vividusScenarioInfoList.forEach(vividusScenarioInfo -> jiraExporterErrorCollection
          .addLogTestCaseInfoExportError(e, testCaseId, vividusScenarioInfo));
    }
  }

  private TestCase createTestCaseParameters(
      String testCaseId, List<VividusScenarioInfo> vividusScenarioInfoList)
      throws SyntaxException {
    Map<Story, List<Scenario>> testCaseStoryMap = vividusScenarioInfoList.stream()
        .collect(Collectors.groupingBy(
            VividusScenarioInfo::getStory,
            Collectors.mapping(VividusScenarioInfo::getScenario, Collectors.toList())
        ));

    List<StoryParameters> storyParametersList = new ArrayList<>();
    for (Entry<Story, List<Scenario>> storyEntry : testCaseStoryMap.entrySet()) {
      Story story = storyEntry.getKey();
      List<Scenario> scenarioInfoList = storyEntry.getValue();
      LOGGER.atInfo().addArgument(story.getPath()).log("Processing Story: {}");

      StoryParameters storyParameters = new StoryParameters();
      storyParameters.setPath(story.getPath());
      storyParameters.setGivenStories(GivenStoriesConverter.convert(story.getGivenStories()));
      storyParameters.setScenarios(createScenarioParametersList(story, scenarioInfoList));

      storyParametersList.add(storyParameters);
    }

    TestCaseParameters testCaseParameters = new TestCaseParameters(testCaseId);
    testCaseParameters.setStories(storyParametersList);

    return testCaseFactory.createTestCase(testCaseParameters);
  }

  private void exportTestCase(TestCase testCase)
      throws IOException, NonCucumberTypesException, NonScenariosException, NotSingleUniqueValueException,
      NonEditableIssueStatusException, NonTestCaseWithinRunException, JiraConfigurationException,
      NonEditableTestRunException {
    jiraExporterFacade.updateTestCase(testCase);
  }

  public List<AbstractScenarioParameters> createScenarioParametersList(Story story, List<Scenario> scenarioInfoList)
      throws SyntaxException {
    List<AbstractScenarioParameters> scenarioParametersList = new ArrayList<>();
    for (Scenario scenario : scenarioInfoList) {
      LOGGER.atInfo().addArgument(scenario.getTitle()).log("Processing Scenario: {}");
      scenarioParametersList.add(createScenarioParameters(story, scenario));
    }
    return scenarioParametersList;
  }

  public AbstractScenarioParameters createScenarioParameters(Story story, Scenario scenario)
      throws SyntaxException {
    return parameterFactories.get(TestCaseType.AUTOMATED).apply(story, scenario);
  }

  private ManualScenarioParameters createManualScenarioParameters(Story story, Scenario scenario)
      throws SyntaxException {
    ManualScenarioParameters parameters = new ManualScenarioParameters();
    fillTestCaseParameters(parameters, TestCaseType.MANUAL, scenario);
    parameters.setSteps(ManualStepConverter.convert(story.getPath(), scenario.getTitle(), scenario.collectSteps()));
    return parameters;
  }

  private CucumberScenarioParameters createCucumberScenarioParameters(Story story, Scenario scenario) {
    CucumberScenarioParameters scenarioParameters = new CucumberScenarioParameters();
    fillTestCaseParameters(scenarioParameters, TestCaseType.AUTOMATED, scenario);
    CucumberScenario cucumberScenario = CucumberScenarioConverter.convert(scenario);
    scenarioParameters.setScenarioType(cucumberScenario.getType());
    scenarioParameters.setScenario(cucumberScenario.getScenario());
    scenarioParameters.setGivenStories(GivenStoriesConverter.convert(scenario.getGivenStories()));
    return scenarioParameters;
  }

  private <T extends AbstractScenarioParameters> void fillTestCaseParameters(T parameters, TestCaseType type,
      Scenario scenario) {
    parameters.setType(type);
    parameters.setLabels(scenario.getMetaValues(Meta.JIRA_LABELS));
    parameters.setComponents(scenario.getMetaValues(Meta.JIRA_COMPONENTS));
    parameters.setSummary(scenario.getTitle());
  }

  @FunctionalInterface
  private interface CreateParametersFunction
      extends FailableBiFunction<Story, Scenario, AbstractScenarioParameters, SyntaxException> {

  }
}
