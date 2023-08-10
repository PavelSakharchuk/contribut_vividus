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
import static java.util.Map.entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.function.FailableBiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.model.jbehave.NotUniqueMetaValueException;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.output.ManualStepConverter;
import org.vividus.output.OutputReader;
import org.vividus.output.SyntaxException;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter.CucumberScenario;
import org.vividus.plugin.jira.facade.AbstractTestCaseParameters;
import org.vividus.plugin.jira.facade.CucumberTestCaseParameters;
import org.vividus.plugin.jira.facade.ManualTestCaseParameters;
import org.vividus.plugin.jira.facade.JiraExporterFacade;
import org.vividus.plugin.jira.facade.JiraExporterFacade.NonEditableIssueStatusException;
import org.vividus.plugin.jira.factory.TestCaseFactory;
import org.vividus.plugin.jira.factory.TestExecutionFactory;
import org.vividus.plugin.jira.model.AbstractTestCase;
import org.vividus.plugin.jira.model.TestCaseType;
import org.vividus.plugin.jira.model.TestExecution;

@Component
public class JiraExporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporter.class);

    @Autowired private JiraExporterOptions jiraExporterOptions;
    @Autowired private JiraExporterFacade jiraExporterFacade;
    @Autowired private TestCaseFactory testCaseFactory;
    @Autowired private TestExecutionFactory testExecutionFactory;

    private final List<String> errors = new ArrayList<>();

    private final Map<TestCaseType, Function<AbstractTestCaseParameters, AbstractTestCase>> testCaseFactories = Map.of(
        TestCaseType.MANUAL, p -> testCaseFactory.createManualTestCase((ManualTestCaseParameters) p),
        TestCaseType.CUCUMBER, p -> testCaseFactory.createCucumberTestCase((CucumberTestCaseParameters) p)
    );

    private final Map<TestCaseType, CreateParametersFunction> parameterFactories = Map.of(
        TestCaseType.MANUAL, this::createManualTestCaseParameters,
        TestCaseType.CUCUMBER, (title, scenario) -> createCucumberTestCaseParameters(scenario)
    );

    // TODO: ok in general: Look to exportScenario method
    public void exportResults() throws IOException
    {
        List<Entry<String, Scenario>> testCases = new ArrayList<>();
        for (Story story : OutputReader.readStoriesFromJsons(jiraExporterOptions.getJsonResultsDirectory()))
        {
            LOGGER.atInfo().addArgument(story::getPath).log("Exporting scenarios from {} story");

            for (Scenario scenario : story.getFoldedScenarios())
            {
                /**
                 *  TODO:
                 *      - createTestCase:
                 *          - createandlink: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario","labels":[],"components":[]}}
                 *              - ok. It is extra I think, we need only to update status in the TestReport
                 *          - componentslabelsupdatabletci: skip
                 *          - createcucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario Outline"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment\r\nExamples:\r\n|parameter-key|\r\n|parameter-value-1|\r\n|parameter-value-2|\r\n|parameter-value-3|\r\n"}}
                 *          - updatecucumber: skip
                 *          - continueiferror: skip
                 *          - skipped: skip
                 *          - morethanoneid: skip
                 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
                 *      - updateTestCase -
                 *          - createandlink: skip
                 *          - componentslabelsupdatabletci: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: componentslabelsupdatabletci","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}]}}
                 *          - createcucumber: skip
                 *          - updatecucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario: updatecucumber","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment"}}
                 *          - continueiferror: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: continueiferror 2","labels":[],"components":[]}}
                 *          - skipped: skip
                 *          - morethanoneid: skip
                 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
                 *      - createTestsLink -
                 *          - createandlink: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-25"},"outwardIssue":{"key":"VIVIT-1"}}
                 *          - componentslabelsupdatabletci: ok (updated): {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-2"},"outwardIssue":{"key":"VIVIT-1"}}
                 *          - createcucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-32"},"outwardIssue":{"key":"VIVIT-1"}}
                 *          - updatecucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-3"},"outwardIssue":{"key":"VIVIT-1"}}
                 *          - continueiferror: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-5"},"outwardIssue":{"key":"VIVIT-1"}}
                 *          - skipped: skip
                 *          - morethanoneid: skip
                 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
                 */
                exportScenario(story.getPath(), scenario).ifPresent(testCases::add);
            }
        }

        // TODO: Looks like It is needed to skip now because we don't have Sets in our approach
//        addTestCasesToTestSet(testCases);
        // TODO: Looks like It is needed to skip now because cloning of TestPlan to TestReport is simpler
        // TODO: We need to update only result/ status
        // TODO: From this Part: Need to investigate adding of attachments
//        addTestCasesToTestExecution(testCases);

        publishErrors();
    }

    // TODO: Looks like It is needed to skip now because we don't have Sets in our approach
//    private void addTestCasesToTestSet(List<Entry<String, Scenario>> testCases)
//    {
//        String testSetKey = jiraExporterOptions.getTestSetKey();
//        if (testSetKey != null)
//        {
//            List<String> testCaseIds = testCases.stream()
//                                                .map(Entry::getKey)
//                                                .collect(Collectors.toList());
//
//            executeSafely(() -> jiraExporterFacade.updateTestSet(testSetKey, testCaseIds), "test set", testSetKey);
//        }
//    }

    // TODO: Looks like It is needed to skip now because cloning of TestPlan to TestReport is simpler
//    private void addTestCasesToTestExecution(List<Entry<String, Scenario>> testCases)
//    {
//        String testExecutionKey = jiraExporterOptions.getTestExecutionKey();
//
//        if (testExecutionKey != null || jiraExporterOptions.getTestExecutionSummary() != null)
//        {
//            TestExecution testExecution = testExecutionFactory.create(testCases);
//            executeSafely(() -> jiraExporterFacade.importTestExecution(testExecution,
//                    jiraExporterOptions.getTestExecutionAttachments()), "test execution", testExecutionKey);
//        }
//    }

//    private void executeSafely(FailableRunnable runnable, String type, String key)
//    {
//        try
//        {
//            runnable.run();
//        }
//        catch (IOException | JiraConfigurationException thrown)
//        {
//            String errorMessage = key == null
//                    ? String.format("Failed to create %s: %s", type, thrown.getMessage())
//                    : String.format("Failed to update %s with the key %s: %s", type, key, thrown.getMessage());
//            errors.add(errorMessage);
//        }
//    }

    /**
     *  TODO:
     *      - createTestCase:
     *          - createandlink: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario","labels":[],"components":[]}}
     *              - ok. It is extra I think, we need only to update status in the TestReport
     *          - componentslabelsupdatabletci: skip
     *          - createcucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario Outline"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment\r\nExamples:\r\n|parameter-key|\r\n|parameter-value-1|\r\n|parameter-value-2|\r\n|parameter-value-3|\r\n"}}
     *          - updatecucumber: skip
     *          - continueiferror: skip
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     *      - updateTestCase -
     *          - createandlink: skip
     *          - componentslabelsupdatabletci: ok:  {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: componentslabelsupdatabletci","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}]}}
     *          - createcucumber: skip
     *          - updatecucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario: updatecucumber","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment"}}
     *          - continueiferror: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: continueiferror 2","labels":[],"components":[]}}
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     *      - createTestsLink -
     *          - createandlink: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-25"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - componentslabelsupdatabletci: ok (updated): {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-2"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - createcucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-32"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - updatecucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-3"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - continueiferror: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-5"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     */
    private Optional<Entry<String, Scenario>> exportScenario(String storyTitle, Scenario scenario)
    {
        String scenarioTitle = scenario.getTitle();

        if (scenario.hasMetaWithName("jira.skip-export"))
        {
            LOGGER.atInfo().addArgument(scenarioTitle).log("Skip export of {} scenario");
            return Optional.empty();
        }
        LOGGER.atInfo().addArgument(scenarioTitle).log("Exporting {} scenario");

        try
        {
            TestCaseType testCaseType = scenario.isManual() ? TestCaseType.MANUAL : TestCaseType.CUCUMBER;

            String testCaseId = scenario.getUniqueMetaValue("testCaseId").orElse(null);

            AbstractTestCaseParameters parameters = parameterFactories.get(testCaseType).apply(scenarioTitle, scenario);
            AbstractTestCase testCase = testCaseFactories.get(testCaseType).apply(parameters);
            if (testCaseId == null)
            {
                /**
                 *  TODO:
                 *      - createTestCase: I think we need only update
                 *          - createandlink: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario","labels":[],"components":[]}}
                 *              - It is extra I think, we need only to update status in the TestReport
                 *          - componentslabelsupdatabletci: skip
                 *          - createcucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario Outline"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment\r\nExamples:\r\n|parameter-key|\r\n|parameter-value-1|\r\n|parameter-value-2|\r\n|parameter-value-3|\r\n"}}
                 *          - updatecucumber: skip
                 *          - continueiferror: skip
                 *          - skipped: skip
                 *          - morethanoneid: skip
                 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
                 */
                testCaseId = jiraExporterFacade.createTestCase(testCase);
            }
            else if (jiraExporterOptions.isTestCaseUpdatesEnabled())
            {
                /**
                 *  TODO:
                 *      - updateTestCase -
                 *          - createandlink: skip
                 *          - componentslabelsupdatabletci: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: componentslabelsupdatabletci","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}]}}
                 *          - createcucumber: skip
                 *          - updatecucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario: updatecucumber","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment"}}
                 *          - continueiferror: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: continueiferror 2","labels":[],"components":[]}}
                 *          - skipped: skip
                 *          - morethanoneid: skip
                 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
                 */
                jiraExporterFacade.updateTestCase(testCaseId, testCase);
            }
            else
            {
                LOGGER.atInfo().addArgument(testCase::getType)
                               .addArgument(testCaseId)
                               .log("Skipping update of {} Test Case with ID {}");
            }
            /**
             *  TODO:
             *      - createTestsLink -
             *          - createandlink: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-25"},"outwardIssue":{"key":"VIVIT-1"}}
             *          - componentslabelsupdatabletci: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-2"},"outwardIssue":{"key":"VIVIT-1"}}
             *          - createcucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-32"},"outwardIssue":{"key":"VIVIT-1"}}
             *          - updatecucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-3"},"outwardIssue":{"key":"VIVIT-1"}}
             *          - continueiferror: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-5"},"outwardIssue":{"key":"VIVIT-1"}}
             *          - skipped: skip
             *          - morethanoneid: skip
             *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
             */
            createTestsLink(testCaseId, scenario);
            return Optional.of(entry(testCaseId, scenario));
        }
        catch (IOException | SyntaxException | NonEditableIssueStatusException | NotUniqueMetaValueException
                | JiraConfigurationException e)
        {
            String errorMessage = "Story: " + storyTitle + lineSeparator() + "Scenario: " + scenarioTitle
                    + lineSeparator() + "Error: " + e.getMessage();
            errors.add(errorMessage);
            LOGGER.atError().setCause(e).log("Got an error while exporting");
        }
        return Optional.empty();
    }

    // TODO: ok
    private ManualTestCaseParameters createManualTestCaseParameters(String storyTitle, Scenario scenario)
            throws SyntaxException
    {
        ManualTestCaseParameters parameters = new ManualTestCaseParameters();
        fillTestCaseParameters(parameters, TestCaseType.MANUAL, scenario);
        /**
         * TODO:
         *                  - Field Type (Jira): Jira does not have supported JSON field. I didn't find.
         *                  - Notes: I think manual steps can be skipped because we will be update tasks ???
         */
        parameters.setSteps(ManualStepConverter.convert(storyTitle, scenario.getTitle(), scenario.collectSteps()));
        return parameters;
    }

    // TODO: ok
    private CucumberTestCaseParameters createCucumberTestCaseParameters(Scenario scenario)
    {
        CucumberTestCaseParameters parameters = new CucumberTestCaseParameters();
        fillTestCaseParameters(parameters, TestCaseType.CUCUMBER, scenario);
        CucumberScenario cucumberScenario = CucumberScenarioConverter.convert(scenario);
        parameters.setScenarioType(cucumberScenario.getType());
        parameters.setScenario(cucumberScenario.getScenario());
        return parameters;
    }

    // TODO: ok
    private <T extends AbstractTestCaseParameters> void fillTestCaseParameters(T parameters, TestCaseType type,
            Scenario scenario)
    {
        parameters.setType(type);
        parameters.setLabels(scenario.getMetaValues("jira.labels"));
        parameters.setComponents(scenario.getMetaValues("jira.components"));
        parameters.setSummary(scenario.getTitle());
    }

    // TODO: ok
    private void publishErrors()
    {
        if (!errors.isEmpty())
        {
            LOGGER.atError().addArgument(System::lineSeparator).addArgument(() ->
            {
                StringBuilder errorBuilder = new StringBuilder();
                IntStream.range(0, errors.size()).forEach(index ->
                {
                    String errorMessage = errors.get(index);
                    errorBuilder.append("Error #").append(index + 1).append(lineSeparator())
                                .append(errorMessage).append(lineSeparator());
                });
                return errorBuilder.toString();
            }).log("Export failed:{}{}");
            return;
        }
        LOGGER.atInfo().log("Export successful");
    }

    /**
     *  TODO:
     *      - createTestsLink -
     *          - createandlink: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-25"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - componentslabelsupdatabletci: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-2"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - createcucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-32"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - updatecucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-3"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - continueiferror: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-5"},"outwardIssue":{"key":"VIVIT-1"}}
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     */
    private void createTestsLink(String testCaseId, Scenario scenario)
            throws IOException, NotUniqueMetaValueException, JiraConfigurationException
    {
        Optional<String> requirementId = scenario.getUniqueMetaValue("requirementId");
        if (requirementId.isPresent())
        {
            jiraExporterFacade.createTestsLink(testCaseId, requirementId.get());
        }
    }

    @FunctionalInterface
    private interface CreateParametersFunction
            extends FailableBiFunction<String, Scenario, AbstractTestCaseParameters, SyntaxException>
    {
    }

    @FunctionalInterface
    private interface FailableRunnable
    {
        void run() throws IOException, JiraConfigurationException;
    }
}
