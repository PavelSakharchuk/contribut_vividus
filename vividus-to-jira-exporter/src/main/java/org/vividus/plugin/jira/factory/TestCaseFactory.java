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

package org.vividus.plugin.jira.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.model.AbstractTestCase;
import org.vividus.plugin.jira.model.CucumberTestCase;
import org.vividus.plugin.jira.model.ManualTestCase;
import org.vividus.plugin.jira.facade.AbstractTestCaseParameters;
import org.vividus.plugin.jira.facade.CucumberTestCaseParameters;
import org.vividus.plugin.jira.facade.ManualTestCaseParameters;

@Component
public class TestCaseFactory
{
    @Autowired
    private JiraExporterOptions jiraExporterOptions;

    public ManualTestCase createManualTestCase(ManualTestCaseParameters parameters)
    {
        ManualTestCase testCase = new ManualTestCase();
        fillTestCase(parameters, testCase);
        testCase.setManualTestSteps(parameters.getSteps());
        return testCase;
    }

    public CucumberTestCase createCucumberTestCase(CucumberTestCaseParameters parameters)
    {
        CucumberTestCase testCase = new CucumberTestCase();
        fillTestCase(parameters, testCase);
        testCase.setScenarioType(parameters.getScenarioType());
        testCase.setScenario(parameters.getScenario());
        return testCase;
    }

    private void fillTestCase(AbstractTestCaseParameters parameters, AbstractTestCase testCase)
    {
        testCase.setType(parameters.getType().getValue());
        testCase.setProjectKey(jiraExporterOptions.getProjectKey());
        testCase.setAssigneeId(jiraExporterOptions.getAssigneeId());
        testCase.setSummary(parameters.getSummary());
        testCase.setLabels(parameters.getLabels());
        testCase.setComponents(parameters.getComponents());
    }
}
