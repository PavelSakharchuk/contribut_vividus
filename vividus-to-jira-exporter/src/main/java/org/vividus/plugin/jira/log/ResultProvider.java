/*
 * Copyright 2019-2022 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.vividus.plugin.jira.exception.ExceptionType;
import org.vividus.plugin.jira.exporter.model.TestCaseInfo;
import org.vividus.plugin.jira.exporter.model.TestCaseStatus;
import org.vividus.plugin.jira.exporter.model.VividusScenarioInfo;
import org.vividus.plugin.jira.log.ExporterStatisticLogger.ExportStatus;

public class ResultProvider {

  private final Map<TestCaseInfo, List<VividusScenarioInfo>> vividusScenarioMap;
  private final List<VividusScenarioInfo> vividusScenarioWithoutTestCaseIdList;

  public ResultProvider(List<VividusScenarioInfo> vividusScenarioInfoList) {
    this.vividusScenarioWithoutTestCaseIdList = vividusScenarioInfoList.stream()
        .filter(vividusScenarioInfo -> Objects.isNull(vividusScenarioInfo.getTestCaseId()))
        .collect(Collectors.toList());
    this.vividusScenarioMap = vividusScenarioInfoList.stream()
        .filter(vividusScenarioInfo -> Objects.nonNull(vividusScenarioInfo.getTestCaseId()))
        .collect(Collectors.groupingBy(VividusScenarioInfo::getTestCase));
  }

  public Map<TestCaseInfo, List<VividusScenarioInfo>> getVividusScenarioMap() {
    return vividusScenarioMap;
  }

  public List<VividusScenarioInfo> getVividusScenarioWithoutTestCaseIdList() {
    return vividusScenarioWithoutTestCaseIdList;
  }

  public int calculateVividusScenarioTotalSize() {
    return vividusScenarioMap.values().stream()
        .mapToInt(List::size).sum() + vividusScenarioWithoutTestCaseIdList.size();
  }

  public long calculateVividusTotalTestCaseUpdatedSize() {
    return vividusScenarioMap.keySet().stream()
        .map(this::getUpdatedInfoStatus)
        .filter(ExportStatus.YES::equals)
        .count();
  }

  public long calculateVividusTotalTestCaseNotUpdatedSize() {
    return vividusScenarioMap.keySet().stream()
        .map(this::getUpdatedInfoStatus)
        .filter(status -> !ExportStatus.YES.equals(status))
        .count();
  }

  public long calculateVividusTotalScenariosUpdatedSize() {
    return vividusScenarioMap.values().stream()
        .flatMap(List::stream)
        .map(VividusScenarioInfo::getTestCase)
        .filter(TestCaseInfo::isUpdatedInfo)
        .count();
  }

  public long calculateVividusTotalScenariosNotUpdatedSize() {
    return vividusScenarioMap.values().stream()
        .flatMap(List::stream)
        .map(VividusScenarioInfo::getTestCase)
        .filter(testCaseInfo -> !testCaseInfo.isUpdatedInfo())
        .count();
  }

  public long calculateVividusTotalTestCaseStatusSize(TestCaseStatus status) {
    Predicate<TestCaseStatus> statusPredicate = Objects.isNull(status) ? Objects::isNull : status::equals;
    return vividusScenarioMap.keySet().stream()
        .map(TestCaseInfo::getTestCaseStatus)
        .filter(statusPredicate)
        .count();
  }

  public ExportStatus getUpdatedInfoStatus(TestCaseInfo testCaseInfo) {
    return Objects.isNull(testCaseInfo) ?
        vividusScenarioWithoutTestCaseIdList.isEmpty() ? ExportStatus.UNKNOWN : ExportStatus.SKIPPED :
        testCaseInfo.isUpdatedInfo() ? ExportStatus.YES : ExportStatus.SKIPPED;
  }

  public TestCaseStatus getTestCaseStatus(TestCaseInfo testCaseInfo) {
    TestCaseStatus testCaseStatus = testCaseInfo.getTestCaseStatus();
    return Objects.isNull(testCaseStatus) ? TestCaseStatus.SKIPPED : testCaseStatus;
  }

  public long getScenariosStatus(List<VividusScenarioInfo> vividusScenarioInfoList, TestCaseStatus status) {
    Predicate<TestCaseStatus> statusPredicate = Objects.isNull(status) ? Objects::isNull : status::equals;
    return vividusScenarioInfoList.stream()
        .map(VividusScenarioInfo::getScenarioStatus)
        .filter(statusPredicate)
        .count();
  }

  public String getErrorStatus(List<VividusScenarioInfo> vividusScenarioList) {
    Set<ExceptionType> values = vividusScenarioList.stream()
        .map(VividusScenarioInfo::getExceptionType)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (values.size() > 1) {
      return values.stream()
          .map(ExceptionType::getComment)
          .collect(Collectors.joining("; "));
    }
    if (values.isEmpty()) {
      return ExportStatus.UNKNOWN.getValue();
    }
    ExceptionType singleUniqueValue = values.iterator().next();
    return Objects.isNull(singleUniqueValue) ? ExportStatus.UNKNOWN.getValue() : singleUniqueValue.getComment();
  }
}
