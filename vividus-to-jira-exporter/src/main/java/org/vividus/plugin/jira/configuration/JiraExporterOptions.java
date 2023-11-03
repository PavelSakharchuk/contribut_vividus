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

package org.vividus.plugin.jira.configuration;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.vividus.plugin.jira.exporter.Constants;
import org.vividus.plugin.jira.exporter.Constants.PropertyPrefix;

@ConfigurationProperties(Constants.PropertyPrefix.JIRA_EXPORTER)
@PropertySource("file:over.properties")
public class JiraExporterOptions
{
    private Path jsonResultsDirectory;
    private String projectKey;
    private String testRunId;
    private String assigneeId;
    // TODO: Delete
    private String testSetKey;
    private String testExecutionKey;
    // TODO: Delete
    private String testExecutionSummary;
    private List<Path> testExecutionAttachments;
    private boolean testCaseInfoUpdatesEnabled;
    private boolean testCaseStatusUpdatesEnabled;
    private String testIssueType;

    public Path getJsonResultsDirectory()
    {
        return jsonResultsDirectory;
    }

    public void setJsonResultsDirectory(Path jsonResultsDirectory)
    {
        this.jsonResultsDirectory = jsonResultsDirectory;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public void setTestRunId(String testRunId) {
        this.testRunId = testRunId;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getTestSetKey()
    {
        return testSetKey;
    }

    public void setTestSetKey(String testSetKey)
    {
        this.testSetKey = testSetKey;
    }

    public String getTestExecutionKey()
    {
        return testExecutionKey;
    }

    public void setTestExecutionKey(String testExecutionKey)
    {
        this.testExecutionKey = testExecutionKey;
    }

    public String getTestExecutionSummary()
    {
        return testExecutionSummary;
    }

    public void setTestExecutionSummary(String testExecutionSummary)
    {
        this.testExecutionSummary = testExecutionSummary;
    }

    public List<Path> getTestExecutionAttachments()
    {
        return testExecutionAttachments;
    }

    public void setTestExecutionAttachments(List<Path> testExecutionAttachments)
    {
        this.testExecutionAttachments = testExecutionAttachments;
    }

    public boolean isTestCaseInfoUpdatesEnabled() {
        return testCaseInfoUpdatesEnabled;
    }

    public void setTestCaseInfoUpdatesEnabled(boolean testCaseInfoUpdatesEnabled) {
        this.testCaseInfoUpdatesEnabled = testCaseInfoUpdatesEnabled;
    }

    public boolean isTestCaseStatusUpdatesEnabled() {
        return testCaseStatusUpdatesEnabled;
    }

    public void setTestCaseStatusUpdatesEnabled(boolean testCaseStatusUpdatesEnabled) {
        this.testCaseStatusUpdatesEnabled = testCaseStatusUpdatesEnabled;
    }

    public String getTestIssueType()
    {
        return testIssueType;
    }

    public void setTestIssueType(String testIssueType)
    {
        this.testIssueType = testIssueType;
    }
}
