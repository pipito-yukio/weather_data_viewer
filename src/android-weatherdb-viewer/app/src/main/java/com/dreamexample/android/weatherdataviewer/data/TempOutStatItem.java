package com.dreamexample.android.weatherdataviewer.data;

import com.google.gson.annotations.SerializedName;

public class TempOutStatItem {

    @SerializedName("appear_time")
    private final String appearTime;
    private final Double temper;

    public TempOutStatItem(String appearTime, Double temper) {
        this.appearTime = appearTime;
        this.temper = temper;
    }

    public String getAppearTime() {
        return appearTime;
    }

    public Double getTemper() {
        return temper;
    }

    @Override
    public String toString() {
        return "TempOutStatItem{" +
                "appearTime='" + appearTime + '\'' +
                ", temper=" + temper +
                '}';
    }
}
