package org.vividus.plugin.jira.exporter;

import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;

@Component
public class JiraExporterErrorCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiraExporter.class);
  private final List<String> errors = new ArrayList<>();

  public void addLogReaderError(Exception e, String testCaseId, Story story, Scenario scenario) {
    String errorMessage = "TestCaseId: " + testCaseId + lineSeparator()
        + "Story: " + story.getPath() + lineSeparator()
        + "Scenario: " + scenario.getTitle() + lineSeparator()
        + "Error [Reader]: " + e.getMessage();
    errors.add(errorMessage);
    LOGGER.atError().setCause(e).log("Got an error while reading results");
  }

  public void addLogTestCaseInfoExportError(Exception e, String testCaseId, Story story, Scenario scenario) {
    String errorMessage = "TestCaseId: " + testCaseId + lineSeparator()
        + "Story: " + story.getPath() + lineSeparator()
        + "Scenario: " + scenario.getTitle() + lineSeparator()
        + "Error [Test Case Info Export]: " + e.getMessage();
    errors.add(errorMessage);
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

  public void publishErrors()
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
}
