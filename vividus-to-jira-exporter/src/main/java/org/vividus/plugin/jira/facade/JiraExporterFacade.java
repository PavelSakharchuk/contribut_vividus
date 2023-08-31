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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.jira.JiraClientProvider;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.jira.JiraConfigurationProvider;
import org.vividus.jira.JiraFacade;
import org.vividus.jira.model.Attachment;
import org.vividus.jira.model.IssueTransitionStatus;
import org.vividus.jira.model.JiraEntity;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.databind.CucumberTestCaseSerializer;
import org.vividus.plugin.jira.databind.ManualTestCaseSerializer;
import org.vividus.plugin.jira.databind.TestCaseSerializer;
import org.vividus.plugin.jira.exception.NonAccessTestCaseStatusException;
import org.vividus.plugin.jira.exception.NonEditableIssueStatusException;
import org.vividus.plugin.jira.exception.NonEditableTestRunException;
import org.vividus.plugin.jira.exception.NonTestCaseWithinRunException;
import org.vividus.plugin.jira.exporter.Constants;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.TestCaseStatus;
import org.vividus.plugin.jira.model.jira.AbstractTestCase;
import org.vividus.plugin.jira.model.jira.CucumberTestCase;
import org.vividus.plugin.jira.model.jira.ManualTestCase;
import org.vividus.plugin.jira.model.jira.TestCase;
import org.vividus.plugin.jira.model.rest.IssueTransitionUpdateRequest;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

public class JiraExporterFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporterFacade.class);

  private final List<String> editableStatuses;
  private final Optional<String> jiraInstanceKey;
  private final JiraFacade jiraFacade;
  private final JiraConfigurationProvider jiraConfigurationProvider;
  private final JiraClientProvider jiraClientProvider;
  private final JiraExporterOptions jiraExporterOptions;
  private final ObjectMapper objectMapper;

  public JiraExporterFacade(Optional<String> jiraInstanceKey, List<String> editableStatuses, JiraFacade jiraFacade,
      JiraConfigurationProvider jiraConfigurationProvider, JiraClientProvider jiraClientProvider,
      JiraExporterOptions jiraExporterOptions, ManualTestCaseSerializer manualTestSerializer,
      CucumberTestCaseSerializer cucumberTestSerializer, TestCaseSerializer testCaseSerializer) {
    this.jiraInstanceKey = jiraInstanceKey;
    this.editableStatuses = editableStatuses;
    this.jiraFacade = jiraFacade;
    this.jiraConfigurationProvider = jiraConfigurationProvider;
    this.jiraClientProvider = jiraClientProvider;
    this.jiraExporterOptions = jiraExporterOptions;
    this.objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(Include.NON_NULL)
        .registerModule(new SimpleModule().addSerializer(ManualTestCase.class, manualTestSerializer)
            .addSerializer(CucumberTestCase.class, cucumberTestSerializer)
            .addSerializer(TestCase.class, testCaseSerializer)
        );
  }

  public <T extends AbstractTestCase> void updateTestCase(T testCase) throws IOException,
      NonEditableTestRunException, NonEditableIssueStatusException, NonTestCaseWithinRunException,
      JiraConfigurationException {

    String testCaseId = testCase.getTestCaseId();
    JiraEntity testRun = checkIfRunEditable(jiraExporterOptions.getTestRunId());
    JiraEntity testCaseOfRun = checkIfIssueEditable(testRun, testCase.getTestCaseId());

    String updateTestRequest = objectMapper.writeValueAsString(testCase);
    LOGGER.atInfo().addArgument(testCase::getType)
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseId)
        .addArgument(testRun.getKey())
        .addArgument(updateTestRequest)
        .log("Updating {} Test Case with ID {} [Initial TC: {}] for {} Test Run: {}");
    jiraFacade.updateIssue(testCaseOfRun.getKey(), updateTestRequest);
    LOGGER.atInfo().addArgument(testCase::getType)
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseId)
        .addArgument(testRun.getKey())
        .log("{} Test Case with key {} [Initial TC: {}] for {} Test Run has been updated");
  }

  public <T extends AbstractTestCase> void updateTestCase(String testCaseKey, T testCase) throws IOException,
      NonEditableTestRunException, NonEditableIssueStatusException, NonTestCaseWithinRunException,
      JiraConfigurationException {
    JiraEntity testRun = checkIfRunEditable(jiraExporterOptions.getTestRunId());
    JiraEntity testCaseOfRun = checkIfIssueEditable(testRun, testCaseKey);

    String updateTestRequest = objectMapper.writeValueAsString(testCase);
    LOGGER.atInfo().addArgument(testCase::getType)
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseKey)
        .addArgument(testRun.getKey())
        .addArgument(updateTestRequest)
        .log("Updating {} Test Case with ID {} [Initial TC: {}] for {} Test Run: {}");
    jiraFacade.updateIssue(testCaseOfRun.getKey(), updateTestRequest);
    LOGGER.atInfo().addArgument(testCase::getType)
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseKey)
        .addArgument(testRun.getKey())
        .log("{} Test Case with key {} [Initial TC: {}] for {} Test Run has been updated");
  }

  public IssueTransitionStatus getTransitionStatus(TestCaseInfo testCase, TestCaseStatus testCaseStatus)
      throws IOException, JiraConfigurationException, NonAccessTestCaseStatusException {
    return jiraFacade.getIssueTransitionStatuses(testCase.getTestCaseId()).stream()
        .filter(transitionStatus -> transitionStatus.getName().equalsIgnoreCase(testCaseStatus.name()))
        .findFirst().orElseThrow(() -> new NonAccessTestCaseStatusException(testCase.getTestCaseId(), testCaseStatus));
  }

  public void updateStatus(String testCaseId, IssueTransitionStatus testCaseStatus)
      throws IOException, JiraConfigurationException,
      NonEditableTestRunException, NonTestCaseWithinRunException, NonEditableIssueStatusException {
    JiraEntity testRun = checkIfRunEditable(jiraExporterOptions.getTestRunId());
    JiraEntity testCaseOfRun = checkIfIssueEditable(testRun, testCaseId);

    IssueTransitionUpdateRequest request = new IssueTransitionUpdateRequest(testCaseStatus);
    String updateStatusRequest = objectMapper.writeValueAsString(request);

    LOGGER.atInfo()
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseId)
        .addArgument(testRun.getKey())
        .addArgument(testCaseStatus.getName())
        .addArgument(updateStatusRequest)
        .log("Updating Test Case Status with ID {} [Initial TC: {}] for {} Test Run to '{}': {}");
    jiraFacade.updateIssueStatus(testCaseOfRun.getKey(), updateStatusRequest);
    LOGGER.atInfo()
        .addArgument(testCaseOfRun.getKey())
        .addArgument(testCaseId)
        .addArgument(testRun.getKey())
        .addArgument(testCaseStatus.getName())
        .log("Status fot Test Case with ID {} [Initial TC: {}] for {} Test Run has been updated to '{}'");
  }

  // TODO
  public void addAttachments(String testExecutionKey, List<Path> attachmentPaths)
      throws IOException, JiraConfigurationException {
    if (attachmentPaths.isEmpty()) {
      return;
    }

    try {
      jiraFacade.addAttachments(testExecutionKey, asAttachments(attachmentPaths));

      LOGGER.atInfo().addArgument(attachmentPaths)
          .addArgument(testExecutionKey)
          .log("Successfully attached files and folders at {} to test execution with key {}");
    } catch (IOException | ZipException | JiraConfigurationException e) {
      LOGGER.atError().setCause(e)
          .addArgument(attachmentPaths)
          .addArgument(testExecutionKey)
          .log("Failed to attach files and folders at {} to test execution with key {}");
    }
  }

  private List<Attachment> asAttachments(List<Path> attachmentPaths) throws IOException {
    List<Attachment> attachments = new ArrayList<>(attachmentPaths.size());

    for (Path attachmentPath : attachmentPaths) {
      String name = FilenameUtils.getName(attachmentPath.toString());
      byte[] body;

      if (Files.isDirectory(attachmentPath)) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ZipUtil.pack(attachmentPath.toFile(), output, ZipUtil.DEFAULT_COMPRESSION_LEVEL);
        name += ".zip";
        body = output.toByteArray();
      } else {
        body = Files.readAllBytes(attachmentPath);
      }

      attachments.add(new Attachment(name, body));
    }

    return attachments;
  }

  public void createTestsLink(String testCaseId, String requirementId) throws IOException, JiraConfigurationException {
    String linkType = "Test";
    JiraEntity issue = jiraFacade.getIssue(testCaseId);
    boolean linkExists = issue.getIssueLinks().stream()
        .anyMatch(link -> linkType.equals(link.getType()) && requirementId.equals(link.getOutwardIssueKey()));

    if (linkExists) {
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

  private JiraEntity checkIfRunEditable(String testRunId)
      throws NonEditableTestRunException, JiraConfigurationException {
    try {
      return jiraFacade.getIssue(testRunId);
    } catch (IOException e) {
      throw new NonEditableTestRunException(testRunId, e);
    }
  }

  private JiraEntity checkIfIssueEditable(JiraEntity testRun, String testCaseKey) throws IOException,
      NonEditableIssueStatusException, NonTestCaseWithinRunException, JiraConfigurationException {
    JiraEntity issueOfRun = getIssueOfRun(testRun.getKey(), testCaseKey)
        .orElseThrow(() -> new NonTestCaseWithinRunException(testRun, testCaseKey));

    String status = jiraFacade.getIssueStatus(issueOfRun.getKey());
    if (editableStatuses.stream().noneMatch(s -> StringUtils.equalsIgnoreCase(s, status))) {
      throw new NonEditableIssueStatusException(testCaseKey, status);
    }

    return issueOfRun;
  }

  private Optional<JiraEntity> getIssueOfRun(String runId, String targetInitialTestCaseId)
      throws IOException, JiraConfigurationException {
    String initialTestCaseCustomField = jiraConfigurationProvider.getMappedFieldSafely(
        Constants.JiraMappingProperties.INITIAL_TEST_CASE,
        jiraConfigurationProvider.getFieldsMappingByProjectKey(jiraExporterOptions.getProjectKey()));

    for (JiraEntity subtask : jiraFacade.getIssue(runId).getSubtasks()) {
      String initialTestCaseId = jiraFacade.getIssueField(subtask.getKey(), initialTestCaseCustomField).trim();
      if (initialTestCaseId.equals(targetInitialTestCaseId)) {
        return Optional.of(subtask);
      }
    }

    return Optional.empty();
  }
}
