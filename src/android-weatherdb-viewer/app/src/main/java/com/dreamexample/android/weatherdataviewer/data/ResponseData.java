package com.dreamexample.android.weatherdataviewer.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class ResponseData {

    @SerializedName("measurement_time")
    private final String measurementTime;
    @SerializedName("temp_out")
    private final double tempOut;
    @SerializedName("temp_in")
    private final double tempIn;
    private final double humid;
    private final double pressure;
    @SerializedName("rec_count")
    private final int recCount;
    @SerializedName("temp_out_stat_today")
    private final TempOutStat tempOutStatToday;
    @SerializedName("temp_out_stat_before")
    private final TempOutStat tempOutStatBefore;

    public ResponseData(String measurementTime, double tempOut, double tempIn,
                        double humid, double pressure, int recCount,
                        TempOutStat statToday, TempOutStat statBefore) {
        this.measurementTime = measurementTime;
        this.tempOut = tempOut;
        this.tempIn = tempIn;
        this.humid = humid;
        this.pressure = pressure;
        this.recCount = recCount;
        this.tempOutStatToday = statToday;
        this.tempOutStatBefore = statBefore;
    }

    public String getMeasurementTime() {
        return measurementTime;
    }

    public double getTempOut() {
        return tempOut;
    }

    public double getTempIn() {
        return tempIn;
    }

    public double getHumid() {
        return humid;
    }

    public double getPressure() {
        return pressure;
    }

    public int getRecCount() {
        return recCount;
    }

    public TempOutStat getTempOutStatToday() {
        return tempOutStatToday;
    }

    public TempOutStat getTempOutStatBefore() {
        return tempOutStatBefore;
    }

    @NonNull
    @Override
    public String toString() {
        return "ResponseData{" +
                "measurementTime='" + measurementTime + '\'' +
                ", tempOut=" + tempOut +
                ", tempIn=" + tempIn +
                ", humid=" + humid +
                ", pressure=" + pressure +
                ", recCount=" + recCount +
                ", tempOutStatToday=" + tempOutStatToday +
                ", tempOutStatBefore=" + tempOutStatBefore +
                '}';
    }

}
