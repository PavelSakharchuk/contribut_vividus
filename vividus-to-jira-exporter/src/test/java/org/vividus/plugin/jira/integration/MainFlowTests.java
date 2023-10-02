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
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.vividus.plugin.jira.VividusToJiraExporterApplication;
import org.vividus.plugin.jira.configuration.JiraExporterOptions;
import org.vividus.plugin.jira.exporter.JiraExporter;
import org.vividus.plugin.jira.facade.JiraExporterFacade;
import org.vividus.plugin.jira.factory.TestCaseFactory;
import org.vividus.util.ResourceUtils;



/**
 * {{Jira: Project type}}: 'company-managed project' (because 'components' is skiped in the 'team-managed project')
 * TODO: After updating: Scenario without TestCaseId in the @Meta will be skipped
 * TODO: MANUAL Scenarios will be skipped - Need to Update
 * TODO: Markdown: https://support.atlassian.com/jira-software-cloud/docs/markdown-and-keyboard-shortcuts/
 *
 *
 * TODO: Need to investigate setting up of 'jiraExporterOptions'
 * TODO: Add Attachments - I didn't checked
 * TODO Notes: Test is:
 *      - Manual: if all "scenarios"."steps"."outcome" == comment
 *      - Cucumber: if one "scenarios"."steps"."outcome" != "comment". Can be "successful"
 *      -   Exemple:  "steps": [{"outcome": "successful","value": "Given I setup test environment"},{"outcome": "successful","value": "When I perform action on test environment"},
 *
 *      * createTestCase:
 *          TODO Notes: It is extra I think, we need only to update status in the TestReport
 *              - customfield_** (request): "test-case-type" (property) (Required):
 *                  - Details: https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html#_manual_test_case_properties
 *                  - Create in the Jira: https://vivi-test.atlassian.net/secure/admin/ViewCustomFields.jspa
 *                  - Field Type (Jira): 'Select List (single choice)'
 *                  - Options (Required setting by Jira): Manual/ Cucumber
 *                  - 'Cucumber' from: https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html: Create and update manual and cucumber test cases
 *                  - 'Cucumber' from: https://docs.getxray.app/display/XRAY/Test#Test-AutomatedTests
 *                  - Options ???: I think need to update to 'Manual'/ 'Automated'
 *                  - property: jira.vivi.fields-mapping.test-case-type
 *                  - 'id'/ 'key' from: https://vivi-test.atlassian.net/rest/api/3/field
 *              - customfield_** (request): "manual-steps" (property) (Required):
 *                  - Details: https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html#_manual_test_case_properties
 *                  - Create in the Jira: https://vivi-test.atlassian.net/secure/admin/ViewCustomFields.jspa
 *                  - Field Type (Jira): JSON
 *                  - Field Type (Jira): Jira didn't find supported JSON field.
 *                  - Notes: I think manual steps can be skipped because we will be update tasks ???
 *                  - Notes: I saved steps feature but I skip it on the serialization level ???
 *                  - property: jira.vivi.fields-mapping.manual-steps
 *                  - 'id'/ 'key' from: https://vivi-test.atlassian.net/rest/api/3/field
 *              - customfield_** (request): "cucumber-scenario-type" (property) (Required):
 *                  - Details: https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html#_cucumber_test_case_properties
 *                  - Field Type (Jira): 'Select List (single choice)'
 *                  - Options (Required setting by Jira): Scenario (without Examples)/ Scenario Outline (with Examples)
 *                  - Create in the Jira: https://vivi-test.atlassian.net/secure/admin/ViewCustomFields.jspa
 *                  - property: jira.vivi.fields-mapping.cucumber-scenario-type
 *                  - 'id'/ 'key' from: https://vivi-test.atlassian.net/rest/api/3/field
 *              - customfield_** (request): "cucumber-scenario" (property) (Required):
 *                  - Details: https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html#_cucumber_test_case_properties
 *                  - Create in the Jira: https://vivi-test.atlassian.net/secure/admin/ViewCustomFields.jspa
 *                  - Field Type (Jira): 'Paragraph (supports rich text)'
 *                  - property: jira.vivi.fields-mapping.cucumber-scenario
 *                  - 'id'/ 'key' from: https://vivi-test.atlassian.net/rest/api/3/field
 *              - issuetype (request): [Test]
 *              - summary (request): {{text}}
 *              - labels (request): []
 *              - components (request): []
 *          - createandlink: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario","labels":[],"components":[]}}
 *              - ok. It is extra I think, we need only to update status in the TestReport
 *              - Notes: Skipped generator of step JSON within ManualTestCaseSerializer#serializeCustomFields
 *              -   because: Field Type (Jira): Jira does not have supported JSON field. I didn't find.
 *              - Notes: I think manual steps can be skipped because we will be update tasks ???
 *          - componentslabelsupdatabletci: skip
 *          - createcucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario Outline"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment\r\nExamples:\r\n|parameter-key|\r\n|parameter-value-1|\r\n|parameter-value-2|\r\n|parameter-value-3|\r\n"}}
 *          - updatecucumber: skip
 *          - continueiferror: skip
 *          - skipped: skip
 *          - morethanoneid: skip
 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
 *      * updateTestCase
 *          TODO Notes:
 *              - Status: property: jira-exporter.editable-statuses=To Do
 *              - labels (request): Can be created
 *              - components (request): Can be created
 *              - description (Jira): Didn't face
 *          - createandlink: skip
 *          - componentslabelsupdatabletci: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: componentslabelsupdatabletci","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}]}}
 *          - createcucumber: skip
 *          - updatecucumber: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Cucumber"},"summary":"Dummy scenario: updatecucumber","labels":["dummy-label-1","dummy-label-2"],"components":[{"name":"dummy-component-1"},{"name":"dummy-component-2"}],"customfield_10057":{"value":"Scenario"},"customfield_10058":"Given I setup test environment\r\nWhen I perform action on test environment\r\nThen I verify changes on test environment"}}
 *          - continueiferror: ok: {"fields":{"project":{"key":"VIVIT"},"issuetype":{"name":"Test"},"customfield_10055":{"value":"Manual"},"summary":"Dummy scenario: continueiferror 2","labels":[],"components":[]}}
 *          - skipped: skip
 *          - morethanoneid: skip
 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
 *      * createTestsLink:
 *          TODO Notes: It is extra I think, we need only to update status in the TestReport
 *              - Add link from 'requirementId' (JSON) field as a 'Test' in the Jira
 *              - "type"."name" (request):
 *                  -   from https://vivi-test.atlassian.net/rest/api/3/issueLinkType
 *                  -   Need to update to optional (to property): String linkType = "Test"; (Java Code)
 *          - createandlink: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-25"},"outwardIssue":{"key":"VIVIT-1"}}
 *          - componentslabelsupdatabletci: ok (updated): {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-2"},"outwardIssue":{"key":"VIVIT-1"}}
 *          - createcucumber: ok (updated): {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-32"},"outwardIssue":{"key":"VIVIT-1"}}
 *          - updatecucumber: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-3"},"outwardIssue":{"key":"VIVIT-1"}}
 *          - continueiferror: ok: {"type":{"name":"Test"},"inwardIssue":{"key":"VIVIT-5"},"outwardIssue":{"key":"VIVIT-1"}}
 *          - skipped: skip
 *          - morethanoneid: skip
 *          - empty Folder: java.lang.IllegalArgumentException: The directory 'C:\Users\PAVEL_~1\AppData\Local\Temp\junit8253403924913869911' does not contain needed JSON files
 *
 * *addTestCasesToTestSet: Looks like It is needed to skip now because we don't have Sets in our approach
 * *addTestCasesToTestExecution: Looks like It is needed to skip now because cloning of TestPlan to TestReport is simpler
 *                               We need to update only result/ status
 */
@SpringBootTest(classes = VividusToJiraExporterApplication.class, properties = {
    // Jira
    // Project=vivdus-test
    // To=\vividus-facade-jira/src/main/java/org/vividus/jira/model/JiraConfiguration.java
//    "jira.nucleus.project-key-regex=(VIVIT)",
//    "jira.nucleus.project-key-regex=(NUC)",
    // To=\vividus-facade-jira/src/main/java/org/vividus/jira/model/JiraConfiguration.java
//    "jira.nucleus.endpoint=https://jjmtdf.atlassian.net/",
    // To=\vividus-http-client/src/main/java/org/vividus/http/client/AuthConfig.java

//    "jira.nucleus.http.auth.username=sakharchuk.pavel@gmail.com",
    // To=\vividus-http-client/src/main/java/org/vividus/http/client/AuthConfig.java
//    "jira.nucleus.http.auth.password=ATATT3xFfGF099I2wPWDTFPb5lEJyvcwBiI1Wif531Ikos1bH7jqPjFCC4ABtH7yolnRlSLNYxshUXoak1eYfhl0wuTLJ7EshJozObSjPXcpNhid4o7U1V7iv368Z3BfExBlEbTM5H9Mxs5_cSe7v9woxyEnFBB2umxoSM033QwsI2Ye7BoVDfc=D442DAE2",
    // To=\vividus-http-client/src/main/java/org/vividus/http/client/AuthConfig.java
//    "jira.nucleus.http.auth.preemptive-auth-enabled=true",
// https://docs.vividus.dev/vividus/0.5.13/integrations/xray-exporter.html#_authentication
//    "jira.nucleus.http.headers.Cookie=ajs_anonymous_id=%221ca66645-9074-4e34-b1c2-034dd34b2d89%22; atlassian.xsrf.token=4733b3edec66b02fc99a11a3d2a83ea3783071b4_lin; SL_G_WPT_TO=ru; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1; tenant.session.token=eyJraWQiOiJzZXNzaW9uLXNlcnZpY2VcL3Byb2QtMTU5Mjg1ODM5NCIsImFsZyI6IlJTMjU2In0.eyJhc3NvY2lhdGlvbnMiOltdLCJzdWIiOiI3MTIwMjA6YzIzMTVhNjMtNWQ2OC00ZmZiLWE3ZDItNzBkZTJhNTc4OWFhIiwiZW1haWxEb21haW4iOiJlcGFtLmNvbSIsImltcGVyc29uYXRpb24iOltdLCJjcmVhdGVkIjoxNjg4OTkyODA4LCJyZWZyZXNoVGltZW91dCI6MTY5MjY5ODcxNiwidmVyaWZpZWQiOnRydWUsImlzcyI6InNlc3Npb24tc2VydmljZSIsInNlc3Npb25JZCI6IjYyOTdiMTU3LTZmOTMtNDk4NS04NDMwLWE3MDA2ZTZhZWQwZCIsInN0ZXBVcHMiOltdLCJvcmdJZCI6IjRhNjIyZmE3LTcxZDMtNGQwZC04MzJkLWFjZWE4MzE4NTJjNiIsImF1ZCI6ImF0bGFzc2lhbiIsIm5iZiI6MTY5MjY5ODExNiwiZXhwIjoxNjk1MjkwMTE2LCJpYXQiOjE2OTI2OTgxMTYsImVtYWlsIjoicGF2ZWxfc2FraGFyY2h1a0BlcGFtLmNvbSIsImp0aSI6IjYyOTdiMTU3LTZmOTMtNDk4NS04NDMwLWE3MDA2ZTZhZWQwZCJ9.rsgCHPjTAgsumtMmamywOdOQ1XEJHrbcPYGAsSlGECIba8XF4nkfwSHVti_9zkGRuu2fLcnGNDvuptVRZ_QuAc4rpl2XYmJsS7scKTa8B9HaijzXrHXQ3J3ywQPihhqefalaA_bGG5pjXpY9EmI6Wkw3hi1XRi8gBRJfO3kCrOrdnTotPxWeAmF-V2iHefv90uFK3wPfaYxlMgZDi5m6NWl9uXnZ4CVynOOOQ4FQgt0GNIE6EoLXirrPySUNSbfTE9aZKEbky1uO_8mb1hDMkZRldtC68ZGD53YnbEz0SAknI1KKdEQDao9HYFW0Iqvmj-lrsR6ZysGBgyY7MZXVkQ; JSESSIONID=6D83580E9A926EFAA08AD834323E5CD9",
//    "jira.vivi.http.socket-timeout=10000",

    // Mapping
    // Required
    // Constants
    // TO: \vividus-facade-jira/src/main/java/org/vividus/jira/JiraConfigurationProvider.java#getFieldsMappingByProjectKey
//    "jira.nucleus.fields-mapping.initial-test-case=customfield_10096",
    // Constants
    // TO: \vividus-facade-jira/src/main/java/org/vividus/jira/JiraConfigurationProvider.java#getFieldsMappingByProjectKey
//    "jira.nucleus.fields-mapping.test-case-type=customfield_10045",
//    // Constants
//    // TO: \vividus-facade-jira/src/main/java/org/vividus/jira/JiraConfigurationProvider.java#getFieldsMappingByProjectKey
//    "jira.nucleus.fields-mapping.manual-steps=customfield_10056",
//    // Constants
//    // TO: \vividus-facade-jira/src/main/java/org/vividus/jira/JiraConfigurationProvider.java#getFieldsMappingByProjectKey
//    "jira.nucleus.fields-mapping.cucumber-scenario-type=customfield_10057",
//    // Constants
//    // TO: \vividus-facade-jira/src/main/java/org/vividus/jira/JiraConfigurationProvider.java#getFieldsMappingByProjectKey
//    "jira.nucleus.fields-mapping.cucumber-scenario=customfield_10058",

    // Xray -> Jira
    // Constants
    // To: vividus-to-jira-exporter/src/main/resources/org/vividus/plugin/jira/spring.xml
//    "jira-exporter.jira-instance-key=nucleus",
    // Constants
    // To: vividus-to-jira-exporter/src/main/resources/org/vividus/plugin/jira/spring.xml
//    "jira-exporter.editable-statuses=To Do, In progress, Ready for test, Failed, Passed, Blocked",
    // Constants
    // To: \vividus-to-jira-exporter/src/main/java/org/vividus/plugin/jira/configuration/JiraExporterOptions.java
//    "jira-exporter.project-key=VIVITEST",
//    "jira-exporter.project-key=VIVIT",
//    "jira-exporter.project-key=NUC",
    // TODO: Go to profile and get Id from url: https://vivi-test.atlassian.net/jira/people/5c3c62faa217aa69bce5d9f0
    // Constants
    // To: \vividus-to-jira-exporter/src/main/java/org/vividus/plugin/jira/configuration/JiraExporterOptions.java
//    "jira-exporter.assignee-id=5c3c62faa217aa69bce5d9f0",

    // To: \vividus-to-jira-exporter/src/main/java/org/vividus/plugin/jira/configuration/JiraExporterOptions.java
    // TODO: Need to investigate setting up of 'jiraExporterOptions'
//-    "jira-exporter.json-results-directory=reports_1/jbehave/batch-1",
//    "jira-exporter.test-run-id=NUC-14",
//-    "jira-exporter.test-case-info-updates-enabled=true",
//    "jira-exporter.test-issue-type=Task",





    "jira.nucleus.project-key-regex=(NUC)",
    "jira.nucleus.endpoint=https://jjmtdf.atlassian.net/",
    "jira.nucleus.http.auth.preemptive-auth-enabled=true",
//    "jira.nucleus.http.headers.Cookie=ajs_anonymous_id=%221ca66645-9074-4e34-b1c2-034dd34b2d89%22; atlassian.xsrf.token=a9401f60b60df5baec9b870b1e9ff03ab626bae0_lin; SL_G_WPT_TO=ru; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1; JSESSIONID=BFCB1B9D665AEEC17E85E8DF459B3B02; tenant.session.token=eyJraWQiOiJzZXNzaW9uLXNlcnZpY2VcL3Byb2QtMTU5Mjg1ODM5NCIsImFsZyI6IlJTMjU2In0.eyJhc3NvY2lhdGlvbnMiOltdLCJzdWIiOiI3MTIwMjA6YzIzMTVhNjMtNWQ2OC00ZmZiLWE3ZDItNzBkZTJhNTc4OWFhIiwiZW1haWxEb21haW4iOiJlcGFtLmNvbSIsImltcGVyc29uYXRpb24iOltdLCJjcmVhdGVkIjoxNjg4OTkyODA4LCJyZWZyZXNoVGltZW91dCI6MTY5MzIyMDY0NSwidmVyaWZpZWQiOnRydWUsImlzcyI6InNlc3Npb24tc2VydmljZSIsInNlc3Npb25JZCI6IjYyOTdiMTU3LTZmOTMtNDk4NS04NDMwLWE3MDA2ZTZhZWQwZCIsInN0ZXBVcHMiOltdLCJvcmdJZCI6IjRhNjIyZmE3LTcxZDMtNGQwZC04MzJkLWFjZWE4MzE4NTJjNiIsImF1ZCI6ImF0bGFzc2lhbiIsIm5iZiI6MTY5MzIyMDA0NSwiZXhwIjoxNjk1ODEyMDQ1LCJpYXQiOjE2OTMyMjAwNDUsImVtYWlsIjoicGF2ZWxfc2FraGFyY2h1a0BlcGFtLmNvbSIsImp0aSI6IjYyOTdiMTU3LTZmOTMtNDk4NS04NDMwLWE3MDA2ZTZhZWQwZCJ9.pYPM9lHTzOYfsTOcTBV9QuYCy6nDCtYNeQ2MpouTDL3GKWc0KDIbZO1erm7_K_qcl76iwZBiBQISICdkdrQWntVSMMNcuwWI9t0CuWJ7H3vLa0t4H1Wyq98SBfES1HG4-tQ2wsa9KI-nMvlDn8oU3SKNw4NCPhJyApAOdeO3LoZJBwVAk23ODDnIPXrp286IFdqXcMmRSTWT-KI26_BBEd2fyf8a8WisaO9bxCVNigN9WW_VhJXFIkXoMzj_qOg4hdhbcTXPd8Rg4pGktQtMcfDuWPghyHDVycinX8XtLsXvKhZpgJvrP44B8ZArGHjzHiQfjBtajz-KzLJ5Z6yNGQ",
//    "jira.nucleus.http.auth.username=",
//    "jira.nucleus.http.auth.password=",
    "jira.nucleus.http.auth.username=pavel_sakharchuk@epam.com",
//    "jira.nucleus.http.auth.password=SaharSahar1",
    "jira.nucleus.http.auth.password=",

    "jira.nucleus.fields-mapping.initial-test-case=customfield_10096",
    "jira.nucleus.fields-mapping.test-case-type=customfield_10045",

    "jira-exporter.jira-instance-key=nucleus",
    "jira-exporter.project-key=NUC",
    "jira-exporter.editable-statuses=To Do, In progress, Ready for test, Failed, Passed, Blocked",

//    "jira-exporter.json-results-directory=reports_1/jbehave/batch-1",
//    "jira-exporter.test-run-id=NUC-14",
//    "jira-exporter.test-case-info-updates-enabled=true",
//    "jira-exporter.test-issue-type=Task",

    // Example
//    "jira.endpoint=https://jira.vividus.com/",
//    "xray-exporter.project-key=VIVIDUS"
})
class MainFlowTests
{
    @MockBean private JiraExporterOptions jiraExporterOptions;
    @SpyBean private JiraExporterFacade jiraExporterFacade;
    @SpyBean private TestCaseFactory testCaseFactory;
    @Autowired private JiraExporter jiraExporter;

    @TempDir
    Path sourceDirectory;

    @BeforeEach
    void setUpJiraExporterOptions() throws IOException, URISyntaxException
    {
//          URI jsonResultsUri = getJsonResultsUri("jnj");
//          URI jsonResultsUri = getJsonResultsUri("jnj_full");
//          URI jsonResultsUri = getJsonResultsUri("jnj_one_scenario_to_few_cases");
//          URI jsonResultsUri = getJsonResultsUri("jnj_few_scenario_to_one_case");

//          URI jsonResultsUri = getJsonResultsUri("batch-1");
//          URI jsonResultsUri = getJsonResultsUri("batch-2");
//          URI jsonResultsUri = getJsonResultsUri("batch-22082023");
//          URI jsonResultsUri = getJsonResultsUri("batch-22082023_2");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_comment");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_without_GivenStories");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_givenStories_with_few_Scenarios");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_givenStories_if_one_TC-id_in_different_story_files");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_givenStories_with_parameters");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_few_givenStories_within_one_file");
//          URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_specific_GivenStory");
//        URI jsonResultsUri = getJsonResultsUri("batch_NUC-761_specific_GivenStory_second");
//        URI jsonResultsUri = getJsonResultsUri("story_meta");
//        URI jsonResultsUri = getJsonResultsUri("docker-reports_25062023_NUC-4120");

//          URI jsonResultsUri = getJsonResultsUri("createandlink");
          URI jsonResultsUri = getJsonResultsUri("createandlink2");
//        URI jsonResultsUri = getJsonResultsUri("componentslabelsupdatabletci");
//        URI jsonResultsUri = getJsonResultsUri("createcucumber");
//        URI jsonResultsUri = getJsonResultsUri("updatecucumber");
//        URI jsonResultsUri = getJsonResultsUri("continueiferror");
//        URI jsonResultsUri = getJsonResultsUri("skipped");
//        URI jsonResultsUri = getJsonResultsUri("skipped1");
//        URI jsonResultsUri = getJsonResultsUri("skipped2");
//        URI jsonResultsUri = getJsonResultsUri("morethanoneid");
//        when(jiraExporterOptions.getJsonResultsDirectory()).thenReturn(sourceDirectory);

//        URI jsonResultsUri = getJsonResultsUri("batch-1_31082023_1783");
//        URI jsonResultsUri = getJsonResultsUri("batch-1_18092023_3676");


        // TODO: How I can manage this properties by properties file
        when(jiraExporterOptions.getJsonResultsDirectory()).thenReturn(Paths.get(jsonResultsUri));
        when(jiraExporterOptions.getProjectKey()).thenReturn("NUC");
        when(jiraExporterOptions.getTestRunId()).thenReturn("NUC-2626");
        when(jiraExporterOptions.isTestCaseInfoUpdatesEnabled()).thenReturn(true);
        when(jiraExporterOptions.isTestCaseStatusUpdatesEnabled()).thenReturn(true);
        when(jiraExporterOptions.getTestIssueType()).thenReturn("Test Case");
        //        jiraExporterOptions.setTestExecutionAttachments(List.of(ROOT));
    }

    @Test
    void mainFlowTest(@TempDir Path tempDirectory) throws IOException, URISyntaxException
    {
        jiraExporter.exportResults();
    }

    private URI getJsonResultsUri(String resource) throws URISyntaxException
    {
        return ResourceUtils.findResource(getClass(), resource).toURI();
    }
}
