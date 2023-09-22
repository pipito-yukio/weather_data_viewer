package com.dreamexample.android.weatherdataviewer.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.dreamexample.android.weatherdataviewer.data.ResponseDataResult;

/**
 * MainFragment用データ取得リポジトリ
 */
public class WeatherDataRepository extends WeatherRepository<ResponseDataResult> {
    // GETリクエストパス
    private static final String URL_PATH = "/getlastdataforphone";

    public WeatherDataRepository() {}

    @Override
    public String getRequestPath(int pathIdx) {
        return URL_PATH;
    }

    @Override
    public ResponseDataResult parseResultJson(String jsonText) throws JsonParseException {
        Gson gson = new Gson();
        return gson.fromJson(jsonText, ResponseDataResult.class);
    }

}
