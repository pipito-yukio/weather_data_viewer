package com.dreamexample.android.weatherdataviewer.tasks;

import com.google.gson.Gson;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.data.ResponseDataResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * MainFragment用データ取得リポジトリ
 */
public class WeatherDataRepository extends WeatherRepository<ResponseDataResult> {
    private static final String URL_PATH = "/getlastdataforphone";

    public WeatherDataRepository() {}

    @Override
    public String getRequestPath(int urlIdx) {
        return URL_PATH;
    }

    @Override
    public ResponseDataResult parseInputStream(InputStream is) throws IOException {
        StringBuilder sb;
        try (BufferedReader bf = new BufferedReader
                (new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            sb = new StringBuilder();
            while ((line = bf.readLine()) != null) {
                sb.append(line);
            }
        }
        Gson gson = new Gson();
        return gson.fromJson(sb.toString(), ResponseDataResult.class);
    }
}
