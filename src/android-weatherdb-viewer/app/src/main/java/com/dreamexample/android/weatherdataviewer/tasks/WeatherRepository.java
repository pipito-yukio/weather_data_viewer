package com.dreamexample.android.weatherdataviewer.tasks;

import android.os.Handler;
import android.util.Log;

import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class WeatherRepository<T> {
    private static final String TAG = "WeatherRepository";

    public WeatherRepository() {}

    public void makeCurrentTimeDataRequest(
            int urlIdx,
            String baseUrl,
            String requestParameter,
            Map<String, String> headers,
            ExecutorService executor,
            Handler handler,
            final RepositoryCallback<T> callback) {
        executor.execute(() -> {
            try {
                String requestUrl = baseUrl + getRequestPath(urlIdx) + requestParameter;
                Log.d(TAG, "requestUrl:" + requestUrl);
                Result<T> result =
                        getSynchronousCurrentTimeDataRequest(requestUrl, headers);
                notifyResult(result, callback, handler);
            } catch (Exception e) {
                Result<T> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback, handler);
            }
        });
    }

    private void notifyResult(final Result<T> result,
                              final RepositoryCallback<T> callback,
                              final Handler handler) {
        handler.post(() -> callback.onComplete(result));
    }

    private Result<T> getSynchronousCurrentTimeDataRequest(
            String requestUrl, Map<String, String> requestHeaders) {
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf8");
            conn.setRequestProperty("Accept", "application/json;");
            for (String key : requestHeaders.keySet()) {
                conn.setRequestProperty(key, requestHeaders.get(key));
            }
            // Check response code: allow 200 only.
            Log.d(TAG, "ResponseCode: " + conn.getResponseCode());
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                T result = parseInputStream(conn.getInputStream());
                Log.d(TAG, "ResponseResult: " + result);
                return new Result.Success<>(result);
            } else {
                ResponseStatus status = new ResponseStatus(
                        conn.getResponseCode(),
                        conn.getResponseMessage());
                Log.d(TAG, "Warning::ResponseStatus: " + status);
                return new Result.Warning<>(status);
            }
        } catch (IOException ie) {
            Log.w(TAG, ie.getLocalizedMessage());
            return new Result.Error<>(ie);
        }
    }

    public abstract String getRequestPath(int urlIdx);

    public abstract T parseInputStream(InputStream is) throws IOException;
}
