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

import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.exporter.Constants;

public final class JiraSkipExportMetaException extends VividusException {

  private static final long serialVersionUID = -7652198889958024012L;

  public JiraSkipExportMetaException(String testCaseId, Story story, Scenario scenario) {
    super(ExceptionType.TEST_CASE_HAS_META_SKIP_EXPORT,
        String.format("Skip export of scenario ['%s' Initial Test Case] with '@%s' Meta: %s [%s]",
        testCaseId,
        Constants.Meta.JIRA_SKIP_EXPORT,
        scenario.getTitle(),
        story.getPath()));
  }
}
