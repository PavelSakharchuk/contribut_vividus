/*
 * Copyright 2019-2021 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.vividus.output.ManualTestStep;
import org.vividus.plugin.jira.exporter.Constants;
import org.vividus.plugin.jira.facade.AbstractScenarioParameters;
import org.vividus.plugin.jira.facade.CucumberScenarioParameters;
import org.vividus.plugin.jira.facade.ManualScenarioParameters;
import org.vividus.plugin.jira.model.jira.AbstractTestCase;
import org.vividus.plugin.jira.model.jira.CucumberTestCase;
import org.vividus.plugin.jira.model.jira.ManualTestCase;
import org.vividus.plugin.jira.model.TestCaseType;

class TestCaseFactoryTests
{
    private static final String PROJECT_KEY = Constants.JiraExporterProperties.PROJECT_KEY;
    private static final String ASSIGNEE_ID = Constants.JiraExporterProperties.ASSIGNEE_ID;

    @Mock private TestCaseFactory factory;

    @Test
    void shouldCreateManualTestCase()
    {
        ManualTestStep step = Mockito.mock(ManualTestStep.class);
        ManualScenarioParameters parameters = createTestCaseParameters(TestCaseType.MANUAL,
                ManualScenarioParameters::new);
        parameters.setSteps(List.of(step));

        ManualTestCase testCase = factory.createManualTestCase(parameters);

        assertEquals(List.of(step), testCase.getManualTestSteps());
        verifyTestCase(parameters, testCase);
    }

    @Test
    void shouldCreateCucumberTestCase()
    {
        CucumberScenarioParameters parameters = createTestCaseParameters(TestCaseType.AUTOMATED,
                CucumberScenarioParameters::new);
        parameters.setScenarioType("scenario-type");
        parameters.setScenario("scenario");

        CucumberTestCase testCase = factory.createCucumberTestCase(parameters);

        assertEquals(parameters.getScenarioType(), testCase.getScenarioType());
        assertEquals(parameters.getScenario(), testCase.getScenario());
        verifyTestCase(parameters, testCase);
    }

    private void verifyTestCase(AbstractScenarioParameters parameters, AbstractTestCase testCase)
    {
        assertEquals(PROJECT_KEY, testCase.getProjectKey());
        assertEquals(ASSIGNEE_ID, testCase.getAssigneeId());
        assertEquals(parameters.getLabels(), testCase.getLabels());
        assertEquals(parameters.getComponents(), testCase.getComponents());
        assertEquals(parameters.getSummary(), testCase.getSummary());
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractScenarioParameters> T createTestCaseParameters(TestCaseType type,
            Supplier<T> factory)
    {
        AbstractScenarioParameters testCase = factory.get();
        testCase.setType(type);
        testCase.setSummary("summary");
        testCase.setLabels(Set.of("labels-1"));
        testCase.setComponents(Set.of("components-1"));
        return (T) testCase;
    }
}