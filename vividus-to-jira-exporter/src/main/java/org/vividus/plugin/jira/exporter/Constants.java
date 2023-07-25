package org.vividus.plugin.jira.exporter;

public class Constants {

  public static class PropertyPrefix {
    public static final String JIRA = "jira";
    public static final String JIRA_EXPORTER = "jira-exporter";
  }

  public static class JiraExporterProperties {
    public static final String JIRA_INSTANCE_KEY = "jira-instance-key";
    public static final String EDITABLE_STATUSES = "editable-statuses";
    public static final String PROJECT_KEY = "project-key";
    public static final String ASSIGNEE_ID = "assignee-id";
  }

  public static class JiraProperties {
    public static final String FIELDS_MAPPING = "fields-mapping";
  }

  public static class JiraMappingProperties {
    public static final String INITIAL_TEST_CASE = "initial-test-case";
    public static final String TEST_CASE_TYPE = "test-case-type";
    public static final String MANUAL_STEPS = "manual-steps";
    public static final String CUCUMBER_SCENARIO_TYPE = "cucumber-scenario-type";
    public static final String CUCUMBER_SCENARIO = "cucumber-scenario";
  }

  public static class Meta {
    public static final String TEST_CASE_ID = "testCaseId";
    public static final String JIRA_LABELS = "jira.labels";
    public static final String JIRA_COMPONENTS = "jira.components";
    public static final String JIRA_SKIP_EXPORT = "jira.skip-export";
  }
}
