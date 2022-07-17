package com.dreamexample.android.weatherdataviewer;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.os.HandlerCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherApplication extends Application {
    private static final String LOG_TAG = "WeatherApplication";
    // Json file in assts for request.
    private static final String REQUEST_INFO_FILE = "request_info.json";
    private Map<String, String> mRequestUrls;
    private Map<String, String> mRequestHeaders;

    public static final String REQUEST_IMAGE_SIZE_KEY = "X-Request-Image-Size";

    // リクエストは10分おきに制限: センサーが10分間隔で測定しているため値が変わらない
    public ExecutorService mEexecutor = Executors.newFixedThreadPool(1);
    public Handler mdHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            loadRequestConf();
        } catch (Exception e) {
            // ここには来ない想定
            Log.e(LOG_TAG, e.getLocalizedMessage());
        }
    }

    public Map<String, String> getmRequestUrls() {
        return mRequestUrls;
    }

    public Map<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    private void loadRequestConf() throws IOException {
        AssetManager am = getApplicationContext().getAssets();
        Gson gson = new Gson();
        Type typedMap = new TypeToken<Map<String, Map<String, String>>>() {
        }.getType();
        Log.d(LOG_TAG, "typedMap: " + typedMap);
        // gson.fromJson() thorows JsonSyntaxException, JsonIOException
        Map<String, Map<String, String>> map = gson.fromJson(
                new JsonReader(new InputStreamReader(am.open(REQUEST_INFO_FILE))), typedMap);
        mRequestUrls = map.get("urls");
        mRequestHeaders = map.get("headers");
        Log.d(LOG_TAG, "RequestUrls: " + mRequestUrls);
        Log.d(LOG_TAG, "RequestHeaders: " + mRequestHeaders);
    }
}
