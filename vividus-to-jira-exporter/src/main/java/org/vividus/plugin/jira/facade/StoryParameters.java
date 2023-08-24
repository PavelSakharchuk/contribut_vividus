package org.vividus.plugin.jira.facade;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.vividus.model.MetaWrapper;
import org.vividus.model.jbehave.Meta;
import org.vividus.model.jbehave.NotUniqueMetaValueException;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;

public class StoryParameters {

  private String path;
  private List<StoryParameters> givenStories;
  private List<AbstractScenarioParameters> scenarios;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<StoryParameters> getGivenStories() {
    return givenStories;
  }

  public void setGivenStories(List<StoryParameters> givenStories) {
    this.givenStories = givenStories;
  }

  public List<AbstractScenarioParameters> getScenarios() {
    return scenarios;
  }

  public void setScenarios(List<AbstractScenarioParameters> scenarios) {
    this.scenarios = scenarios;
  }
}
