/*
 * Copyright 2019-2020 the original author or authors.
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

package org.vividus.plugin.jira.exception;

import org.vividus.plugin.jira.exporter.Constants;

public enum ExceptionType {
  UNKNOWN("Unknown error", null),
  TEST_RUN_IS_NOT_EDITABLE("Test Run is not editable", NonEditableTestRunException.class),
  TEST_CASE_IS_NULL(String.format("'%s' is null", Constants.Meta.TEST_CASE_ID), NonTestCaseIdException.class),
  TEST_CASE_IS_MISSED("Test Case does not exist within Test Run", NonTestCaseWithinRunException.class),
  TEST_CASE_HAS_META_SKIP_EXPORT(
      "Test Case has Meta: " + Constants.Meta.JIRA_SKIP_EXPORT, JiraSkipExportMetaException.class);

  private final String comment;
  private final Class<?> exceptionClass;

  ExceptionType(String comment, Class<?> exceptionClass) {
    this.comment = comment;
    this.exceptionClass = exceptionClass;
  }

  public String getComment() {
    return comment;
  }

  public Class<?> getExceptionClass() {
    return exceptionClass;
  }
}
