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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GivenStories
{
    private String keyword;
    @JsonProperty("givenStories")
    private List<GivenStoriesPath> givenStoriesPathList;
    private List<Story> stories;

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public List<GivenStoriesPath> getGivenStoriesPathList()
    {
        return givenStoriesPathList;
    }

    public void setGivenStoriesPathList(List<GivenStoriesPath> givenStoriesPathList)
    {
        this.givenStoriesPathList = givenStoriesPathList;
    }

    public List<Story> getStories()
    {
        return stories;
    }

    public void setStories(List<Story> stories)
    {
        this.stories = stories;
    }
}
