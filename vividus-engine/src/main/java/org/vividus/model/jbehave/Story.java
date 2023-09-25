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

package org.vividus.model.jbehave;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.stream.Stream;
import org.vividus.model.MetaWrapper;

public class Story
{
    private String path;
    private List<Meta> meta;
    private Lifecycle lifecycle;
    private GivenStories givenStories;
    private List<Scenario> scenarios;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<Meta> getMeta()
    {
        return meta;
    }

    public void setMeta(List<Meta> meta)
    {
        this.meta = meta;
    }

    public Lifecycle getLifecycle()
    {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }

    public GivenStories getGivenStories()
    {
        return givenStories;
    }

    public void setGivenStories(GivenStories givenStories)
    {
        this.givenStories = givenStories;
    }

    public List<Scenario> getScenarios()
    {
        return scenarios;
    }

    public void setScenarios(List<Scenario> scenarios)
    {
        this.scenarios = scenarios;
    }

    /**
     * Get all <b>meta</b> values
     *
     * <p><i>Notes</i>
     * <ul>
     * <li><b>meta</b>s without value are ignored</li>
     * <li><b>meta</b> values are trimmed upon returning</li>
     * <li><i>;</i> char is used as a separator for <b>meta</b> with multiple values</li>
     * </ul>
     *
     * @param metaName the meta name
     * @return  the meta values
     */
    public Set<String> getMetaValues(String metaName)
    {
        return getMetaStream().filter(m -> metaName.equals(m.getName()))
            .map(Meta::getValue)
            .flatMap(value -> MetaWrapper.parsePropertyValues(value).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Get unique scenarios.
     *
     * All iterations over story-level and scenario-level ExamplesTables are folded into unique scenario instances
     *
     * @return list of values with {@link Scenario} type
     */
    public List<Scenario> getFoldedScenarios()
    {
        if (lifecycle == null || lifecycle.getParameters() == null)
        {
            return scenarios;
        }

        Parameters lifecycleParameters = lifecycle.getParameters();
        int partition = scenarios.size() <= 1 ? 1 : scenarios.size() / lifecycleParameters.getValues().size();
        List<List<Scenario>> scenariosPerExample = Lists.partition(scenarios, partition);

        return IntStream.range(0, partition)
                        .mapToObj(index -> scenariosPerExample.stream()
                                                              .map(l -> l.get(index))
                                                              .collect(Collectors.toList()))
                        .map(s -> s.stream()
                                   .reduce(Story::merge))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .peek(s -> adjustScenarioWithLifecycle(s.getExamples().getParameters(), lifecycleParameters))
                        .collect(Collectors.toList());
    }

    private static Scenario merge(Scenario left, Scenario right)
    {
        List<Example> examples = left.getExamples().getExamples();
        examples.addAll(right.getExamples().getExamples());
        return left;
    }

    private static void adjustScenarioWithLifecycle(Parameters scenarioParameters, Parameters lifecycleParameters)
    {
        List<List<String>> lifecycleValues = lifecycleParameters.getValues();
        scenarioParameters.getNames().addAll(lifecycleParameters.getNames());

        List<List<String>> baseValues = scenarioParameters.getValues();
        if (baseValues.isEmpty())
        {
            baseValues.addAll(lifecycleValues);
            return;
        }

        List<List<String>> adjustedValues = Lists.cartesianProduct(baseValues, lifecycleValues)
                                                 .stream().map(Iterables::concat)
                                                 .map(Lists::newArrayList)
                                                 .collect(Collectors.toList());

        scenarioParameters.setValues(adjustedValues);
    }

    private Stream<Meta> getMetaStream()
    {
        return Optional.ofNullable(getMeta()).stream().flatMap(Collection::stream);
    }
}
