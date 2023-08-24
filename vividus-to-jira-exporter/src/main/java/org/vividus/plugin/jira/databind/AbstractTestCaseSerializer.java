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

package org.vividus.plugin.jira.databind;

import static org.apache.commons.lang3.Validate.isTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.vividus.jira.JiraConfigurationException;
import org.vividus.jira.JiraConfigurationProvider;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exporter.Constants;
import org.vividus.plugin.jira.model.jira.AbstractTestCase;

public abstract class AbstractTestCaseSerializer<T extends AbstractTestCase> extends JsonSerializer<T>
{
    private static final String NAME = "name";

    private static final String TEST_CASE_TYPE_FIELD_KEY = Constants.JiraMappingProperties.TEST_CASE_TYPE;

    @Autowired protected JiraConfigurationProvider jiraConfigurationProvider;
    @Autowired private JiraExporterOptions jiraExporterOptions;

    @Override
    public void serialize(T testCase, JsonGenerator generator, SerializerProvider serializers) throws IOException
    {
        generator.writeStartObject();
        generator.writeObjectFieldStart("fields");

        String projectKey = testCase.getProjectKey();
        writeObjectWithField(generator, "project", "key", projectKey);

        String assigneeId = testCase.getAssigneeId();
        if (StringUtils.isNotBlank(assigneeId))
        {
            writeObjectWithField(generator, "assignee", "id", assigneeId);
        }

        String summary = testCase.getSummary();
        if (summary != null)
        {
            generator.writeStringField("summary", summary);
        }

        writeObjectWithField(generator, "issuetype", NAME, jiraExporterOptions.getTestIssueType());

        try
        {
            Map<String, String> mapping = jiraConfigurationProvider.getFieldsMappingByProjectKey(projectKey);

            writeObjectWithValueField(
                generator,
                jiraConfigurationProvider.getMappedFieldSafely(TEST_CASE_TYPE_FIELD_KEY, mapping),
                testCase.getType());

            writeJsonArray(generator, "labels", testCase.getLabels(), false);

            writeJsonArray(generator, "components", testCase.getComponents(), true);

            serializeCustomFields(testCase, mapping, generator);

            generator.writeEndObject();
            generator.writeEndObject();
        }
        catch (JiraConfigurationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    protected void writeObjectWithValueField(JsonGenerator generator, String objectKey, String fieldValue)
            throws IOException
    {
        writeObjectWithField(generator, objectKey, "value", fieldValue);
    }

    protected abstract void serializeCustomFields(T testCase, Map<String, String> mapping, JsonGenerator generator)
            throws IOException;

    private static void writeJsonArray(JsonGenerator generator, String startField, Collection<String> values,
            boolean wrapValuesAsObjects) throws IOException
    {
        if (values != null)
        {
            generator.writeArrayFieldStart(startField);
            for (String value : values)
            {
                if (wrapValuesAsObjects)
                {
                    generator.writeStartObject();
                    generator.writeStringField(NAME, value);
                    generator.writeEndObject();
                }
                else
                {
                    generator.writeString(value);
                }
            }
            generator.writeEndArray();
        }
    }

    private static void writeObjectWithField(JsonGenerator generator, String objectKey, String fieldName,
            String fieldValue) throws IOException
    {
        generator.writeObjectFieldStart(objectKey);
        generator.writeStringField(fieldName, fieldValue);
        generator.writeEndObject();
    }
}
