package com.dreamexample.android.weatherdataviewer.tasks;

import com.google.gson.Gson;
import com.dreamexample.android.weatherdataviewer.data.ResponseGraphResult;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * GraphFragment用データ取得リポジトリ
 */
public class WeatherGraphRepository extends WeatherRepository<ResponseGraphResult> {
    // 本日気象データグラフ
    private static final String URL_PATH = "/gettodayimageforphone";

    public WeatherGraphRepository() {}

    @Override
    public String getRequestPath() {
        return URL_PATH;
    }

    @Override
    public ResponseGraphResult parseInputStream(InputStream is) throws IOException {
        String strJson = null;
        try(BufferedInputStream bufferedInput = new BufferedInputStream(is);
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 画像データは8KB単位でバイトストリーム追加する
            byte[] buff = new byte[8 * 1024];
            int length;
            while ((length = bufferedInput.read(buff)) != -1) {
                out.write(buff, 0, length);
            }
            byte[] imgArray = out.toByteArray();
            if (imgArray.length > 0) {
                // 文字列に復元
                strJson = new String(imgArray, StandardCharsets.US_ASCII);
            }
        }
        if (strJson != null) {
            Gson gson = new Gson();
            return gson.fromJson(strJson, ResponseGraphResult.class);
        } else {
            return null;
        }
    }
}
