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

package org.vividus.plugin.jira.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.vividus.plugin.jira.VividusToXrayExporterApplication;
import org.vividus.plugin.jira.configuration.XrayExporterOptions;
import org.vividus.plugin.jira.exporter.XrayExporter;
import org.vividus.plugin.jira.facade.XrayFacade;
import org.vividus.plugin.jira.factory.TestCaseFactory;
import org.vividus.util.ResourceUtils;

@SpringBootTest(classes = VividusToXrayExporterApplication.class, properties = {
    "jira.endpoint=https://jira.vividus.com/",
    "xray-exporter.project-key=VIVIDUS"
})
class MainFlowTests
{
    @MockBean private XrayExporterOptions xrayExporterOptions;
    @SpyBean private XrayFacade xrayFacade;
    @SpyBean private TestCaseFactory testCaseFactory;
    @Autowired private XrayExporter xrayExporter;

    @Test
    void mainFlowTest(@TempDir Path tempDirectory) throws IOException, URISyntaxException {

        URI jsonResultsUri = getJsonResultsUri("createandlink");

        when(xrayExporterOptions.getJsonResultsDirectory()).thenReturn(Paths.get(jsonResultsUri));

        xrayExporter.exportResults();
    }

    private URI getJsonResultsUri(String resource) throws URISyntaxException
    {
        return ResourceUtils.findResource(getClass(), resource).toURI();
    }
}
