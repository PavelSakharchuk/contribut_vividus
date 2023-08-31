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

package org.vividus.plugin.jira.log;

//import de.vandermeer.asciitable.AT_Context;
//import de.vandermeer.asciitable.AT_Renderer;
//import de.vandermeer.asciitable.AsciiTable;
//import de.vandermeer.asciitable.CWC_LongestLine;
//import de.vandermeer.asciithemes.a7.A7_Grids;
//import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.TestCaseStatus;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;

public final class ExporterStatisticLogger {


  private static final int REGULAR_COLUMN_MAX_WIDTH = 36;
  private static final int ERROR_COLUMN_MAX_WIDTH = 50;

  private static final int MARGIN = 1;
  private static final String HYPHEN = "-";
  private static final int HEADER_SIZE = 90;
  private static final String CATEGORY_FORMAT = "%s%n %s:%n";
  private static final String NEW_LINE = System.lineSeparator();
  private static final Logger LOGGER = LoggerFactory.getLogger(ExporterStatisticLogger.class);
  private static final Pattern SECURE_KEY_PATTERN = Pattern.compile(
      "password|secret|token|key([^s]|$)", Pattern.CASE_INSENSITIVE);
  private static final int HORIZONTAL_RULE_LENGTH = 80;
  private static final String HORIZONTAL_RULE = HYPHEN.repeat(HORIZONTAL_RULE_LENGTH);
  private static final String CONFIGURATION_SET = "Set";

  private final ResultProvider resultProvider;
  private final JiraExporterOptions jiraExporterOptions;

  public ExporterStatisticLogger(
      JiraExporterOptions jiraExporterOptions, List<VividusScenarioInfo> vividusScenarioInfoList) {
    this.jiraExporterOptions = jiraExporterOptions;
    this.resultProvider = new ResultProvider(vividusScenarioInfoList);
  }

  public void logTestExecutionResults() {
    logInfoMessage(() ->
    {
      try (Formatter message = new Formatter()) {
        message.format(NEW_LINE);
        logExecutionExportInfoDetailsStatistics(message);
        logExecutionExportInfoTotalStatistics(message);
        logExecutionExportStatusTotalStatistics(message);
        return message.toString();
      }
    });
  }

  private static void logInfoMessage(Supplier<String> messageSupplier) {
    LOGGER.atInfo().log(messageSupplier);
  }

  private void logExecutionExportInfoDetailsStatistics(Formatter message) {
    String row = "%n %-10s %-10s %-10s %-10s %25s %-25s";
    message.format(System.lineSeparator());
    message.format("%n Execution Export Info: Details:");
    String rowsSeparator = "%n " + HYPHEN.repeat(HEADER_SIZE);
    message.format(rowsSeparator);
    message.format(row, "TC id", "Scenarios", "TC info is", "Status TC", "Status Scenarios", "Reason");
    message.format(row, "", "", "Updated", "(Exported)", "(Passed/Failed/Skipped)", "");
    message.format(rowsSeparator);

    resultProvider.getVividusScenarioMap().forEach((key, valueList) -> message.format(row,
        key.getTestCaseId(), valueList.size(),
        isTestCaseInfoUpdatesSwitchOn(resultProvider.getUpdatedInfoStatus(key).getValue()),
        isTestCaseStatusUpdatesSwitchOn(WordUtils.capitalizeFully(resultProvider.getTestCaseStatus(key).name())),
        isTestCaseStatusUpdatesSwitchOn(generateScenariosStatus(key, valueList)),
        generateFailReason(valueList))
    );
    message.format(row, "null", resultProvider.getVividusScenarioWithoutTestCaseIdList().size(),
        isTestCaseInfoUpdatesSwitchOn(resultProvider.getUpdatedInfoStatus(null).getValue()),
        isTestCaseStatusUpdatesSwitchOn(resultProvider.getVividusScenarioWithoutTestCaseIdList().size() > 0 ?
            WordUtils.capitalizeFully(TestCaseStatus.SKIPPED.name()) : ExportStatus.UNKNOWN.getValue()),
        isTestCaseStatusUpdatesSwitchOn(String.join("/ ", String.valueOf(0), String.valueOf(0),
            String.valueOf(resultProvider.getVividusScenarioWithoutTestCaseIdList().size()))),
        generateFailReason(resultProvider.getVividusScenarioWithoutTestCaseIdList()));

    message.format(rowsSeparator);
  }

  private void logExecutionExportInfoTotalStatistics(Formatter message) {
    String row = "%n %-20s %-10s %-10s";
    message.format(System.lineSeparator());
    message.format("%n Execution Export Info: Total: " + (jiraExporterOptions.isTestCaseInfoUpdatesEnabled() ?
        Toggle.SWITCH_ON.getValue() : Toggle.SWITCH_OFF.getValue()));
    String rowsSeparator = "%n " + HYPHEN.repeat(HEADER_SIZE);

      message.format(rowsSeparator);
    if(jiraExporterOptions.isTestCaseInfoUpdatesEnabled()) {
      message.format(row, "", "TCs", "Scenarios");
      message.format(rowsSeparator);
      message.format(row, "Updated",
          resultProvider.calculateVividusTotalTestCaseUpdatedSize(),
          resultProvider.calculateVividusTotalScenariosUpdatedSize());
      message.format(row, "Skipped",
          resultProvider.calculateVividusTotalTestCaseNotUpdatedSize(),
          resultProvider.calculateVividusTotalScenariosNotUpdatedSize());
      message.format(row, "Without 'testCaseId'",
          ExportStatus.UNKNOWN.getValue(), resultProvider.getVividusScenarioWithoutTestCaseIdList().size());
      message.format(rowsSeparator);
      message.format(row, "TOTAL",
          resultProvider.getVividusScenarioMap().keySet().size(), resultProvider.calculateVividusScenarioTotalSize());
      message.format(rowsSeparator);
    }
  }

  private void logExecutionExportStatusTotalStatistics(Formatter message) {
    String row = "%n %-10s %-10s";
    message.format(System.lineSeparator());
    message.format("%n Execution Export Status: Total: " + (jiraExporterOptions.isTestCaseStatusUpdatesEnabled() ?
        Toggle.SWITCH_ON.getValue() : Toggle.SWITCH_OFF.getValue()));
    String rowsSeparator = "%n " + HYPHEN.repeat(HEADER_SIZE);
    message.format(rowsSeparator);

    if(jiraExporterOptions.isTestCaseStatusUpdatesEnabled()) {
      message.format(row, "Status", "TCs");
      message.format(rowsSeparator);
      message.format(row, "Passed",
          resultProvider.calculateVividusTotalTestCaseStatusSize(TestCaseStatus.PASSED));
      message.format(row, "Failed",
          resultProvider.calculateVividusTotalTestCaseStatusSize(TestCaseStatus.FAILED));
      message.format(row, "Skipped",
          resultProvider.calculateVividusTotalTestCaseStatusSize(null));
      message.format(rowsSeparator);
      message.format(row, "TOTAL", resultProvider.getVividusScenarioMap().keySet().size());
      message.format(rowsSeparator);
    }
  }

  public static void logPropertiesSecurely(Properties properties) {
    try (Formatter message = new Formatter()) {
      properties.entrySet()
          .stream()
          .map(e -> Map.entry((String) e.getKey(), e.getValue()))
          .sorted(Entry.comparingByKey())
          .forEach(property -> {
            String key = property.getKey();
            Object value = SECURE_KEY_PATTERN.matcher(key).find() ? "****" : property.getValue();
            message.format("%n%s=%s", key, value);
          });
      LOGGER.atInfo().addArgument(message::toString).log("Properties and environment variables:{}");
    }
  }

  private String isTestCaseInfoUpdatesSwitchOn(String value) {
    return jiraExporterOptions.isTestCaseInfoUpdatesEnabled() ? value : Toggle.SWITCH_OFF.getValue();
  }

  private String isTestCaseStatusUpdatesSwitchOn(String value) {
    return jiraExporterOptions.isTestCaseStatusUpdatesEnabled() ? value : Toggle.SWITCH_OFF.getValue();
  }

  private String generateScenariosStatus(
      TestCaseInfo testCaseInfo, List<VividusScenarioInfo> vividusScenarioInfoList) {
    return String.join("/ ",
        String.valueOf(resultProvider.getScenariosStatus(vividusScenarioInfoList, TestCaseStatus.PASSED)),
        String.valueOf(resultProvider.getScenariosStatus(vividusScenarioInfoList, TestCaseStatus.FAILED)),
        String.valueOf(resultProvider.getScenariosStatus(vividusScenarioInfoList, null)));
  }

  private String generateFailReason(List<VividusScenarioInfo> vividusScenarioInfoList) {
    return jiraExporterOptions.isTestCaseInfoUpdatesEnabled() && jiraExporterOptions.isTestCaseStatusUpdatesEnabled() ?
        resultProvider.getErrorStatus(vividusScenarioInfoList) : Toggle.SWITCH_OFF.getValue();
  }

  public enum ExportStatus {
    ERROR("ERROR"),
    UNKNOWN("-"),
    YES("Yes"),
    SKIPPED("Skipped");

    private final String value;

    ExportStatus(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public enum Toggle {
    SWITCH_ON("Switch on"),
    SWITCH_OFF("Switch off");

    private final String value;

    Toggle(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
