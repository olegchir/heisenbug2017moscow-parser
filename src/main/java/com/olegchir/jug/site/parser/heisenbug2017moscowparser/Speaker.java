package com.olegchir.jug.site.parser.heisenbug2017moscowparser;

import java.util.ArrayList;
import java.util.List;

public class Speaker {
    private String speaker;
    private String company;
    private List<String> bio = new ArrayList<>();
    private String imageUrl;

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }



    public List<String> getBio() {
        return bio;
    }

    public void setBio(List<String> bio) {
        this.bio = bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
