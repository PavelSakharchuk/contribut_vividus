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

import static java.lang.System.lineSeparator;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.model.jbehave.Scenario;
import org.vividus.output.ManualStepConverter;
import org.vividus.output.SyntaxException;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter.CucumberScenario;
import org.vividus.plugin.jira.exception.NonCucumberTypesException;
import org.vividus.plugin.jira.exception.NonEditableIssueStatusException;
import org.vividus.plugin.jira.exception.NonEditableTestRunException;
import org.vividus.plugin.jira.exception.NonTestCaseWithinRunException;
import org.vividus.plugin.jira.exception.NotSingleUniqueValueException;
import org.vividus.plugin.jira.exporter.Constants.Meta;
import org.vividus.plugin.jira.facade.AbstractTestCaseParameters;
import org.vividus.plugin.jira.facade.CucumberTestCaseParameters;
import org.vividus.plugin.jira.facade.JiraExporterFacade;
import org.vividus.plugin.jira.facade.ManualTestCaseParameters;
import org.vividus.plugin.jira.factory.TestCaseFactory;
import org.vividus.plugin.jira.model.AbstractTestCase;
import org.vividus.plugin.jira.model.CucumberTestCase;
import org.vividus.plugin.jira.model.MultiTestCase;
import org.vividus.plugin.jira.model.TestCaseType;
import org.vividus.plugin.jira.model.VividusScenarioInfo;

@Component
public class JiraInfoExporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraInfoExporter.class);

    @Autowired private JiraExporterOptions jiraExporterOptions;
    @Autowired private JiraExporterFacade jiraExporterFacade;
    @Autowired private TestCaseFactory testCaseFactory;
    @Autowired private JiraExporterErrorCollection jiraExporterErrorCollection;

    private final Map<TestCaseType, Function<AbstractTestCaseParameters, AbstractTestCase>> testCaseFactories = Map.of(
        TestCaseType.MANUAL, p -> testCaseFactory.createManualTestCase((ManualTestCaseParameters) p),
        TestCaseType.AUTOMATED, p -> testCaseFactory.createCucumberTestCase((CucumberTestCaseParameters) p)
    );

    private final Map<TestCaseType, CreateParametersFunction> parameterFactories = Map.of(
        TestCaseType.MANUAL, this::createManualTestCaseParameters,
        TestCaseType.AUTOMATED, (title, scenario) -> createCucumberTestCaseParameters(scenario)
    );

    public Optional<List<Entry<String, Scenario>>> exportInfoTestCases(
        Map<String, List<VividusScenarioInfo>> multiTestCaseMap)
    {
        List<Entry<String, Scenario>> testCases = new ArrayList<>();
        if (jiraExporterOptions.isTestCaseInfoUpdatesEnabled())
        {
            for (Map.Entry<String,List<VividusScenarioInfo>> testCaseEntry : multiTestCaseMap.entrySet())
            {
                LOGGER.atInfo().addArgument(testCaseEntry.getKey()).log("Exporting Test Case: {}");
                this.exportTestCaseInfo(testCaseEntry.getKey(), testCaseEntry.getValue()).ifPresent(testCases::addAll);
            }
        }
        else
        {
            LOGGER.atInfo().log("Test Case Information Exporting is switched off");
        }
        return Optional.of(testCases);
    }

    private Optional<List<Entry<String, Scenario>>> exportTestCaseInfo(
        String testCaseId, List<VividusScenarioInfo> multiTestCaseList)
    {
        List<AbstractTestCase> testCaseScenarioList = new ArrayList<>();
        for (VividusScenarioInfo multiTestCase : multiTestCaseList)
        {
            Scenario scenario = multiTestCase.getScenario();
            try
            {
                getTestCaseScenario(scenario).ifPresent(testCaseScenarioList::add);
            } catch (SyntaxException e) {
                jiraExporterErrorCollection
                    .addLogTestCaseInfoExportError(e, testCaseId, multiTestCase.getStory(), scenario);
            }
        }

        try
        {
            exportMultiTestCase(testCaseId, testCaseScenarioList);

            return Optional.of(multiTestCaseList.stream()
                .map(VividusScenarioInfo::getScenario)
                .map(scenario -> new SimpleEntry<>(testCaseId, scenario))
                .collect(Collectors.toList()));
        }
        catch (IOException | NonCucumberTypesException | NotSingleUniqueValueException
               | NonEditableTestRunException | NonEditableIssueStatusException | NonTestCaseWithinRunException
               | JiraConfigurationException e)
        {
            multiTestCaseList.forEach(multiTestCase -> jiraExporterErrorCollection
                .addLogTestCaseInfoExportError(e, testCaseId, multiTestCase.getStory(), multiTestCase.getScenario()));
        }
        return Optional.empty();
    }

    private Optional<AbstractTestCase> getTestCaseScenario(Scenario scenario) throws SyntaxException
    {
        String scenarioTitle = scenario.getTitle();

        LOGGER.atInfo().addArgument(scenarioTitle).log("Processing scenario: {}");
        TestCaseType testCaseType = scenario.isManual() ? TestCaseType.MANUAL : TestCaseType.AUTOMATED;
        AbstractTestCaseParameters parameters = parameterFactories.get(testCaseType).apply(scenarioTitle, scenario);
        return Optional.of(testCaseFactories.get(testCaseType).apply(parameters));
    }

    private void exportMultiTestCase(String testCaseId, List<AbstractTestCase> testScenarioList)
        throws IOException, NonCucumberTypesException, NotSingleUniqueValueException, NonEditableIssueStatusException,
        NonTestCaseWithinRunException, JiraConfigurationException, NonEditableTestRunException
    {
        if (CollectionUtils.isNotEmpty(testScenarioList))
        {
            checkIfAutomatedType(testScenarioList);
            MultiTestCase multiTestCase = generateMultiTestCase(testScenarioList);
            jiraExporterFacade.updateTestCase(testCaseId, multiTestCase);
        }
    }

    private MultiTestCase generateMultiTestCase(List<AbstractTestCase> testScenarioList)
        throws NotSingleUniqueValueException
    {
        String description = testScenarioList.stream()
            .map(testScenario -> generateScenarioDescription((CucumberTestCase) testScenario))
            .collect(Collectors.joining(lineSeparator() + "----" + lineSeparator()));

        MultiTestCase multiTestCase = new MultiTestCase();
        multiTestCase.setType(getSingleUniqueValue(testScenarioList, AbstractTestCase::getType).orElse(null));
        multiTestCase.setProjectKey(getSingleUniqueValue(testScenarioList, AbstractTestCase::getProjectKey).orElse(null));
        multiTestCase.setAssigneeId(getSingleUniqueValue(testScenarioList, AbstractTestCase::getAssigneeId).orElse(null));
        multiTestCase.setDescription(description);

        return multiTestCase;
    }

    private String generateScenarioDescription(CucumberTestCase cucumberTestCase)
    {
        return new StringJoiner(System.lineSeparator())
            .add("*Scenario:* " + cucumberTestCase.getSummary())
            .add(cucumberTestCase.getScenario())
            .toString();
    }

    private void checkIfAutomatedType(List<AbstractTestCase> testScenarioList) throws NonCucumberTypesException
    {
        if (testScenarioList.stream()
            .noneMatch(testScenario -> TestCaseType.AUTOMATED.getValue().equals(testScenario.getType())))
        {
            throw new NonCucumberTypesException();
        }
    }

    private Optional<String> getSingleUniqueValue(
        List<AbstractTestCase> testScenarioList, Function<AbstractTestCase, String> valueFunc)
        throws NotSingleUniqueValueException
    {
        Set<String> values = testScenarioList.stream()
            .map(valueFunc)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (values.size() > 1)
        {
            throw new NotSingleUniqueValueException(values);
        }
        return values.isEmpty() ? Optional.empty() : Optional.of(values.iterator().next());
    }

    private ManualTestCaseParameters createManualTestCaseParameters(String storyTitle, Scenario scenario)
            throws SyntaxException
    {
        ManualTestCaseParameters parameters = new ManualTestCaseParameters();
        fillTestCaseParameters(parameters, TestCaseType.MANUAL, scenario);
        parameters.setSteps(ManualStepConverter.convert(storyTitle, scenario.getTitle(), scenario.collectSteps()));
        return parameters;
    }

    private CucumberTestCaseParameters createCucumberTestCaseParameters(Scenario scenario)
    {
        CucumberTestCaseParameters parameters = new CucumberTestCaseParameters();
        fillTestCaseParameters(parameters, TestCaseType.AUTOMATED, scenario);
        CucumberScenario cucumberScenario = CucumberScenarioConverter.convert(scenario);
        parameters.setScenarioType(cucumberScenario.getType());
        parameters.setScenario(cucumberScenario.getScenario());
        return parameters;
    }

    private <T extends AbstractTestCaseParameters> void fillTestCaseParameters(T parameters, TestCaseType type,
            Scenario scenario)
    {
        parameters.setType(type);
        parameters.setLabels(scenario.getMetaValues(Meta.JIRA_LABELS));
        parameters.setComponents(scenario.getMetaValues(Meta.JIRA_COMPONENTS));
        parameters.setSummary(scenario.getTitle());
    }

    @FunctionalInterface
    private interface CreateParametersFunction
            extends FailableBiFunction<String, Scenario, AbstractTestCaseParameters, SyntaxException>
    {
    }
}
