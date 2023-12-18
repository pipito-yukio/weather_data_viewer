package com.dreamexample.android.weatherdataviewer.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class TempOutStat {
    private final TempOutStatItem min;
    private final TempOutStatItem max;
    @SerializedName("measurement_date")
    private final String measurementDate;

    public TempOutStat(TempOutStatItem min, TempOutStatItem max,
                       String measurementDate) {
        this.min = min;
        this.max = max;
        this.measurementDate = measurementDate;
    }

    public TempOutStatItem getMin() {
        return min;
    }

    public TempOutStatItem getMax() {
        return max;
    }

    public String getMeasurementDate() {
        return measurementDate;
    }

    @NonNull
    @Override
    public String toString() {
        return "TempOutStat{" +
                "min=" + min +
                ", max=" + max +
                ", measurementDate='" + measurementDate + '\'' +
                '}';
    }
}
