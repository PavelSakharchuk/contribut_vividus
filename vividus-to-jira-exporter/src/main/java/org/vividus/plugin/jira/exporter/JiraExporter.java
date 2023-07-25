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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.output.OutputReader;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exception.JiraSkipExportMetaException;
import org.vividus.plugin.jira.exception.NonTestCaseIdException;
import org.vividus.plugin.jira.exporter.Constants.Meta;
import org.vividus.plugin.jira.model.VividusScenarioInfo;

@Component
public class JiraExporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporter.class);

    @Autowired private JiraExporterOptions jiraExporterOptions;
    @Autowired private JiraInfoExporter jiraInfoExporter;
    @Autowired private JiraStatusExporter jiraStatusExporter;
    @Autowired private JiraExporterErrorCollection jiraExporterErrorCollection;

    public void exportResults() throws IOException
    {
        Set<Entry<String, Scenario>> testCases = new HashSet<>();

        Map<String, List<VividusScenarioInfo>> multiTestCaseMap =
            OutputReader.readStoriesFromJsons(jiraExporterOptions.getJsonResultsDirectory()).stream()
                .flatMap(this::getMultiTestCaseStream)
                .filter(vividusScenarioInfo -> Objects.nonNull(vividusScenarioInfo.getTestCaseId()))
                .filter(vividusScenarioInfo -> !checkIfJiraSkipExportMeta(
                    vividusScenarioInfo.getTestCaseId(),
                    vividusScenarioInfo.getStory(),
                    vividusScenarioInfo.getScenario()))
                .collect(Collectors.groupingBy(VividusScenarioInfo::getTestCaseId));

        jiraInfoExporter.exportInfoTestCases(multiTestCaseMap).ifPresent(testCases::addAll);
        jiraStatusExporter.exportStatusTestCases(multiTestCaseMap).ifPresent(testCases::addAll);

        jiraExporterErrorCollection.publishErrors();
    }

    private Stream<VividusScenarioInfo> getMultiTestCaseStream(Story story)
    {
        return story.getFoldedScenarios().stream()
            .flatMap(scenario -> getMultiTestCaseStream(story, scenario));
    }

    private Stream<VividusScenarioInfo> getMultiTestCaseStream(Story story, Scenario scenario)
    {
        return scenario.getMetaValues(Constants.Meta.TEST_CASE_ID).isEmpty() ?
            Stream.of(getMultiTestCaseWithoutTestCaseId(story, scenario)) :
            scenario.getMetaValues(Meta.TEST_CASE_ID).stream()
                .map(testCaseId -> new VividusScenarioInfo(testCaseId, story, scenario));
    }

    private VividusScenarioInfo getMultiTestCaseWithoutTestCaseId(Story story, Scenario scenario)
    {
        jiraExporterErrorCollection.addLogReaderError(new NonTestCaseIdException(), null, story, scenario);
        return new VividusScenarioInfo(null, story, scenario);
    }

    private boolean checkIfJiraSkipExportMeta(String testCaseId, Story story, Scenario scenario)
    {
        if (scenario.hasMetaWithName(Constants.Meta.JIRA_SKIP_EXPORT)){
            jiraExporterErrorCollection.addLogReaderError(
                new JiraSkipExportMetaException(testCaseId, story, scenario),
                testCaseId, story, scenario);
            return true;
        }
        return false;
    }
}
