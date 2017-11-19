package ru.dasha.koshka.model;

import java.util.List;

/**
 * Created by Daria on 08.11.2017.
 */
public class FestSection {
    private String name;
    private List<FestActivity> activities;

    public FestSection(String name, List<FestActivity> activities) {
        this.name = name;
        this.activities = activities;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FestActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<FestActivity> activities) {
        this.activities = activities;
    }
}
