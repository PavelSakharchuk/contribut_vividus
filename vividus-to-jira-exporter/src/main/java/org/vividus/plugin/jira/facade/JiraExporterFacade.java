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

package org.vividus.plugin.jira.facade;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.jira.JiraClient;
import org.vividus.jira.JiraClientProvider;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.jira.JiraFacade;
import org.vividus.jira.model.Attachment;
import org.vividus.jira.model.JiraEntity;
import org.vividus.plugin.jira.databind.CucumberTestCaseSerializer;
import org.vividus.plugin.jira.databind.ManualTestCaseSerializer;
import org.vividus.plugin.jira.model.AbstractTestCase;
import org.vividus.plugin.jira.model.AddOperationRequest;
import org.vividus.plugin.jira.model.CucumberTestCase;
import org.vividus.plugin.jira.model.ManualTestCase;
import org.vividus.plugin.jira.model.TestExecution;
import org.vividus.util.json.JsonPathUtils;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

public class JiraExporterFacade
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporterFacade.class);

    private final List<String> editableStatuses;
    private final Optional<String> jiraInstanceKey;
    private final JiraFacade jiraFacade;
    private final JiraClientProvider jiraClientProvider;
    private final ObjectMapper objectMapper;

    public JiraExporterFacade(Optional<String> jiraInstanceKey, List<String> editableStatuses, JiraFacade jiraFacade,
            JiraClientProvider jiraClientProvider, ManualTestCaseSerializer manualTestSerializer,
            CucumberTestCaseSerializer cucumberTestSerializer)
    {
        this.jiraInstanceKey = jiraInstanceKey;
        this.editableStatuses = editableStatuses;
        this.jiraFacade = jiraFacade;
        this.jiraClientProvider = jiraClientProvider;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(Include.NON_NULL)
                .registerModule(new SimpleModule().addSerializer(ManualTestCase.class, manualTestSerializer)
                                                  .addSerializer(CucumberTestCase.class, cucumberTestSerializer));
    }

    /**
     *  TODO:
     *      - createandlink: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario","labels":[],"components":[]}}
     *          - ok. It is extra I think, we need only to update status in the TestReport
     *      - componentslabelsupdatabletci: skip
     *      - createcucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario Outline"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment\r\nExamples:\r\n|parameter-key|\r\n|parameter-value-1|\r\n|parameter-value-2|\r\n|parameter-value-3|\r\n"}}
     *      - updatecucumber: skip
     *      - continueiferror: skip
     *      - skipped: skip
     *      - morethanoneid: skip
     *      - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     */
    public <T extends AbstractTestCase> String createTestCase(T testCase) throws IOException, JiraConfigurationException
    {
        String createTestRequest = objectMapper.writeValueAsString(testCase);
        LOGGER.atInfo().addArgument(testCase::getType).addArgument(createTestRequest).log("Creating {} Test Case: {}");
        String response = jiraFacade.createIssue(createTestRequest, jiraInstanceKey);
        String issueKey = JsonPathUtils.getData(response, "$.key");
        LOGGER.atInfo().addArgument(testCase::getType)
                       .addArgument(issueKey)
                       .log("{} Test with key {} has been created");
        return issueKey;
    }

    /**
     * TODO:
     *          - createandlink: skip
     *          - componentslabelsupdatabletci: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: componentslabelsupdatabletci","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}]}}
     *          - createcucumber: skip
     *          - updatecucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario: updatecucumber","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment"}}
     *          - continueiferror: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: continueiferror 2","labels":[],"components":[]}}
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     */
    public <T extends AbstractTestCase> void updateTestCase(String testCaseKey, T testCase)
            throws IOException, NonEditableIssueStatusException, JiraConfigurationException
    {
        checkIfIssueEditable(testCaseKey);
        String updateTestRequest = objectMapper.writeValueAsString(testCase);
        LOGGER.atInfo().addArgument(testCase::getType)
                       .addArgument(testCaseKey)
                       .addArgument(updateTestRequest)
                       .log("Updating {} Test Case with ID {}: {}");
        jiraFacade.updateIssue(testCaseKey, updateTestRequest);
        LOGGER.atInfo().addArgument(testCase::getType)
                       .addArgument(testCaseKey)
                       .log("{} Test with key {} has been updated");
    }

    // TODO
    public void importTestExecution(TestExecution testExecution, List<Path> attachments)
            throws IOException, JiraConfigurationException
    {
        String testExecutionRequest = objectMapper.writeValueAsString(testExecution);
        FailableFunction<JiraClient, String, IOException> importFunction = client -> client
                .executePost("/rest/raven/1.0/import/execution", testExecutionRequest);

        String testExecutionKey = testExecution.getTestExecutionKey();
        if (testExecutionKey != null)
        {
            LOGGER.atInfo()
                  .addArgument(testExecutionKey)
                  .addArgument(testExecutionRequest)
                  .log("Updating Test Execution with ID {}: {}");
            importFunction.apply(jiraClientProvider.getByIssueKey(testExecutionKey));
            addAttachments(testExecutionKey, attachments);
            LOGGER.atInfo().addArgument(testExecutionKey).log("Test Execution with key {} has been updated");
        }
        else
        {
            LOGGER.atInfo().addArgument(testExecutionRequest).log("Creating Test Execution: {}");
            String response = importFunction.apply(jiraClientProvider.getByJiraConfigurationKey(jiraInstanceKey));
            testExecutionKey = JsonPathUtils.getData(response, "$.testExecIssue.key");
            addAttachments(testExecutionKey, attachments);
            LOGGER.atInfo().addArgument(testExecutionKey).log("Test Execution with key {} has been created");
        }
    }

    // TODO
    public void addAttachments(String testExecutionKey, List<Path> attachmentPaths)
            throws IOException, JiraConfigurationException
    {
        if (attachmentPaths.isEmpty())
        {
            return;
        }

        try
        {
            jiraFacade.addAttachments(testExecutionKey, asAttachments(attachmentPaths));

            LOGGER.atInfo().addArgument(attachmentPaths)
                           .addArgument(testExecutionKey)
                           .log("Successfully attached files and folders at {} to test execution with key {}");
        }
        catch (IOException | ZipException | JiraConfigurationException e)
        {
            LOGGER.atError().setCause(e)
                            .addArgument(attachmentPaths)
                            .addArgument(testExecutionKey)
                            .log("Failed to attach files and folders at {} to test execution with key {}");
        }
    }

    private List<Attachment> asAttachments(List<Path> attachmentPaths) throws IOException
    {
        List<Attachment> attachments = new ArrayList<>(attachmentPaths.size());

        for (Path attachmentPath : attachmentPaths)
        {
            String name = FilenameUtils.getName(attachmentPath.toString());
            byte[] body;

            if (Files.isDirectory(attachmentPath))
            {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipUtil.pack(attachmentPath.toFile(), output, ZipUtil.DEFAULT_COMPRESSION_LEVEL);
                name += ".zip";
                body = output.toByteArray();
            }
            else
            {
                body = Files.readAllBytes(attachmentPath);
            }

            attachments.add(new Attachment(name, body));
        }

        return attachments;
    }

    // TODO: Looks like It is needed to skip now
//    public void updateTestSet(String testSetKey, List<String> testCaseKeys)
//            throws IOException, JiraConfigurationException
//    {
//        AddOperationRequest request = new AddOperationRequest(testCaseKeys);
//        String requestBody = objectMapper.writeValueAsString(request);
//        LOGGER.atInfo()
//              .addArgument(() -> StringUtils.join(testCaseKeys, ", "))
//              .addArgument(testSetKey)
//              .log("Add {} test cases to Test Set with ID {}");
//        jiraClientProvider.getByIssueKey(testSetKey).executePost("/rest/raven/1.0/api/testset/" + testSetKey + "/test",
//                requestBody);
//    }

    /**
     *  TODO:
     *          - createandlink: skip
     *          - componentslabelsupdatabletci: ok
     *          - createcucumber: skip
     *          - updatecucumber: ok
     *          - continueiferror: ok
     *          - skipped: skip
     *          - morethanoneid: skip
     *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
     */
    private void checkIfIssueEditable(String issueKey)
            throws IOException, NonEditableIssueStatusException, JiraConfigurationException
    {
        String status = jiraFacade.getIssueStatus(issueKey);

        if (editableStatuses.stream().noneMatch(s -> StringUtils.equalsIgnoreCase(s, status)))
        {
            throw new NonEditableIssueStatusException(issueKey, status);
        }
    }

    // TODO: ok
    public void createTestsLink(String testCaseId, String requirementId) throws IOException, JiraConfigurationException
    {
        String linkType = "Test";
        JiraEntity issue = jiraFacade.getIssue(testCaseId);
        boolean linkExists = issue.getIssueLinks().stream()
                .anyMatch(link -> linkType.equals(link.getType()) && requirementId.equals(link.getOutwardIssueKey()));

        if (linkExists)
        {
            LOGGER.atInfo().addArgument(testCaseId)
                           .addArgument(linkType)
                           .addArgument(requirementId)
                           .log("Skipping create of {} {} {} link as it already exists");
            return;
        }

        LOGGER.atInfo().addArgument(linkType)
                       .addArgument(testCaseId)
                       .addArgument(requirementId)
                       .log("Create '{}' link from {} to {}");
        jiraFacade.createIssueLink(testCaseId, requirementId, linkType);
    }

    public static final class NonEditableIssueStatusException extends Exception
    {
        private static final long serialVersionUID = -5547086076322794984L;

        public NonEditableIssueStatusException(String testCaseId, String status)
        {
            super("Issue " + testCaseId + " is in non-editable '" + status + "' status");
        }
    }
}
