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

    public ResponseData(String measurementTime, double tempOut, double tempIn,
                        double humid, double pressure, int recCount) {
        this.measurementTime = measurementTime;
        this.tempOut = tempOut;
        this.tempIn = tempIn;
        this.humid = humid;
        this.pressure = pressure;
        this.recCount = recCount;
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

    public int getRecCount() { return recCount; }

    @NonNull
    @Override
    public String toString() {
        return "ResponseData{" +
                "measurement_time='" + measurementTime + '\'' +
                ", temp_out=" + tempOut +
                ", temp_in=" + tempIn +
                ", humid=" + humid +
                ", pressure=" + pressure +
                ", rec_count=" + recCount +
                '}';
    }
}
