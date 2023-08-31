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

package org.vividus.plugin.jira.exporter.model;

import java.util.Objects;
import org.vividus.plugin.jira.exception.ExceptionType;

public class TestCaseInfo {

  private final String testCaseId;
  private TestCaseStatus testCaseStatus;
  private boolean isUpdatedInfo;

  public TestCaseInfo(String testCaseId) {
    this.testCaseId = testCaseId;
  }

  public String getTestCaseId() {
    return testCaseId;
  }

  public TestCaseStatus getTestCaseStatus() {
    return testCaseStatus;
  }

  public void setTestCaseStatus(TestCaseStatus testCaseStatus) {
    this.testCaseStatus = testCaseStatus;
  }

  public boolean isUpdatedInfo() {
    return isUpdatedInfo;
  }

  public void setUpdatedInfo(boolean updatedInfo) {
    isUpdatedInfo = updatedInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(testCaseId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(testCaseId, ((TestCaseInfo) obj).testCaseId);
  }
}
