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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.exception.ExceptionType;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;

@Component
public class JiraExporterErrorCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporter.class);
  private final List<String> errors = new ArrayList<>();

  public void addLogReaderError(Exception e, String testCaseId, VividusScenarioInfo vividusScenarioInfo) {
    String errorMessage = "TestCaseId: " + testCaseId + lineSeparator()
        + "Story: " + vividusScenarioInfo.getStory().getPath() + lineSeparator()
        + "Scenario: " + vividusScenarioInfo.getScenario().getTitle() + lineSeparator()
        + "Error [Reader]: " + e.getMessage();
    errors.add(errorMessage);
    vividusScenarioInfo.setExceptionType(getExceptionType(e));
    LOGGER.atError().setCause(e).log("Got an error while reading results");
  }

  public void addLogTestCaseInfoExportError(Exception e, String testCaseId, VividusScenarioInfo vividusScenarioInfo) {
    String errorMessage = "TestCaseId: " + testCaseId + lineSeparator()
        + "Story: " + vividusScenarioInfo.getStory().getPath() + lineSeparator()
        + "Scenario: " + vividusScenarioInfo.getScenario().getTitle() + lineSeparator()
        + "Error [Test Case Info Export]: " + e.getMessage();
    errors.add(errorMessage);
    vividusScenarioInfo.setExceptionType(getExceptionType(e));
    LOGGER.atError().setCause(e).log("Got an error while exporting of Test Case Information");
  }

  public void addLogTestCaseStatusExportError(Exception e, String testCaseId, Story story, Scenario scenario) {
    String errorMessage = "TestCaseId: " + testCaseId + lineSeparator()
        + "Story: " + story.getPath() + lineSeparator()
        + "Scenario: " + scenario.getTitle() + lineSeparator()
        + "Error [Test Case Status Export]: " + e.getMessage();
    errors.add(errorMessage);
    LOGGER.atError().setCause(e).log("Got an error while exporting of Test Case Status");
  }

  public void publishErrors() {
    if (!errors.isEmpty()) {
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

  public ExceptionType getExceptionType(Exception e) {
    return Arrays.stream(ExceptionType.values())
        .filter(type -> e.getClass().equals(type.getExceptionClass()))
        .findFirst()
        .orElse(ExceptionType.UNKNOWN);
  }
}
