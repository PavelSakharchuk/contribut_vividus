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

package org.vividus.plugin.jira.exception;

public final class NonEditableTestRunException extends Exception {

  private static final long serialVersionUID = -5547086076322794984L;

  public NonEditableTestRunException(String testRunId, Exception e) {
    super(String.format("%s Test Run is non-editable: %s", testRunId, e.getMessage()));
  }
}
