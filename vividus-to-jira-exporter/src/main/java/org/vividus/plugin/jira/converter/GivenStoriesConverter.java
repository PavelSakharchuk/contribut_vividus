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

package org.vividus.plugin.jira.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.vividus.model.jbehave.GivenStories;
import org.vividus.model.jbehave.Scenario;
import org.vividus.model.jbehave.Story;
import org.vividus.plugin.jira.converter.CucumberScenarioConverter.CucumberScenario;
import org.vividus.plugin.jira.exporter.Constants.Meta;
import org.vividus.plugin.jira.facade.AbstractScenarioParameters;
import org.vividus.plugin.jira.facade.CucumberScenarioParameters;
import org.vividus.plugin.jira.facade.StoryParameters;
import org.vividus.plugin.jira.model.TestCaseType;

public final class GivenStoriesConverter {

  private GivenStoriesConverter() {
  }

  public static List<StoryParameters> convert(GivenStories givenStories) {
    return fetchGivenStories(givenStories).stream()
        .map(GivenStoriesConverter::convertGivenStory)
        .toList();
  }

  private static List<Story> fetchGivenStories(GivenStories givenStories) {
    if (Objects.isNull(givenStories)) {
      return Collections.emptyList();
    }

    List<Story> givenStoriesList = new ArrayList<>();
    for (Story story : givenStories.getStories()) {
      List<Story> internalGivenStories = new ArrayList<>();
      fetchInternalGivenStory(internalGivenStories, story);
      Collections.reverse(internalGivenStories);
      givenStoriesList.addAll(internalGivenStories);
    }
    return givenStoriesList;
  }

  private static void fetchInternalGivenStory(List<Story> givenStoriesList, Story givenStory) {
    if (Objects.isNull(givenStory)) {
      return;
    }

    givenStoriesList.add(givenStory);
    GivenStories internalGivenStories = givenStory.getGivenStories();
    if (Objects.nonNull(internalGivenStories)) {
      internalGivenStories.getStories().forEach(story -> fetchInternalGivenStory(givenStoriesList, story));
    }
  }

  private static StoryParameters convertGivenStory(Story givenStory) {
    List<AbstractScenarioParameters> givenScenarioParametersList = new ArrayList<>();
    for (Scenario givenScenario : givenStory.getScenarios()) {
      givenScenarioParametersList.add(createGivenScenariosParameters(givenScenario));
    }

    StoryParameters parameters = new StoryParameters();
    parameters.setPath(givenStory.getPath());
    parameters.setScenarios(givenScenarioParametersList);
    return parameters;
  }

  private static CucumberScenarioParameters createGivenScenariosParameters(Scenario givenScenario) {
    CucumberScenario cucumberScenario = CucumberScenarioConverter.convert(givenScenario);

    CucumberScenarioParameters parameters = new CucumberScenarioParameters();
    parameters.setType(TestCaseType.AUTOMATED);
    parameters.setLabels(givenScenario.getMetaValues(Meta.JIRA_LABELS));
    parameters.setComponents(givenScenario.getMetaValues(Meta.JIRA_COMPONENTS));
    parameters.setSummary(givenScenario.getTitle());
    parameters.setScenarioType(cucumberScenario.getType());
    parameters.setScenario(cucumberScenario.getScenario());
    return parameters;
  }
}
