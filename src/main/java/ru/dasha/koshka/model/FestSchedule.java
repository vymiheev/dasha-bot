package ru.dasha.koshka.model;

import java.util.List;

/**
 * Created by Daria on 08.11.2017.
 */
public class FestSchedule {
    private List<FestActivity> activities;

    public FestSchedule(List<FestActivity> activities) {
        this.activities = activities;
    }

    public List<FestActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<FestActivity> activities) {
        this.activities = activities;
    }
}
