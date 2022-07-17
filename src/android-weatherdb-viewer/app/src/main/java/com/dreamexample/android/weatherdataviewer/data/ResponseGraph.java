package com.dreamexample.android.weatherdataviewer.data;

import com.google.gson.annotations.SerializedName;

public class ResponseGraph {
    @SerializedName("img_src")
    private final String imgSrc;

    public ResponseGraph(String imgSrc) {
        this.imgSrc = imgSrc;
    }

    public String getImgSrc() {
        return imgSrc;
    }

    @Override
    public String toString() {
        return "ResponseGraph: imgSrc.length: " + (imgSrc != null ? imgSrc.length() : 0);
    }
}
