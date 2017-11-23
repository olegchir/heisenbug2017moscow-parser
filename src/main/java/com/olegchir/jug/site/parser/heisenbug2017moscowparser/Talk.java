package com.olegchir.jug.site.parser.heisenbug2017moscowparser;

import java.util.ArrayList;
import java.util.List;

public class Talk {
    private String id;
    private String name;
    private List<Speaker> speakers = new ArrayList<>();
    private String url;
    private List<String> description;

    public void replaceWith(Talk talk) {
        this.name = talk.name;
        this.speakers = talk.speakers;
        this.url = talk.url;
        this.description = talk.description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Speaker> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
