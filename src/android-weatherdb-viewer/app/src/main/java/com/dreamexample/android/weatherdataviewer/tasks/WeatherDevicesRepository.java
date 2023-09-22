package com.dreamexample.android.weatherdataviewer.tasks;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.dreamexample.android.weatherdataviewer.data.ResponseDevicesResult;

public class WeatherDevicesRepository extends WeatherRepository<ResponseDevicesResult> {
    // GETリクエストパス
    private static final String URL_PATH = "/get_devices";

    public WeatherDevicesRepository() {}

    @Override
    public String getRequestPath(int pathIdx) {
        return URL_PATH;
    }

    @Override
    public ResponseDevicesResult parseResultJson(String jsonText) throws JsonParseException {
        Gson gson = new Gson();
        DEBUG_OUT.accept("WeatherDevicesRepository", jsonText);
        return gson.fromJson(jsonText, ResponseDevicesResult.class);
    }

}
