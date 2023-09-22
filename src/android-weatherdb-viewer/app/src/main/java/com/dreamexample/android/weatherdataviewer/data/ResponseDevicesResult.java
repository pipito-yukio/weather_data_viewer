package com.dreamexample.android.weatherdataviewer.data;

/**
 * センサーデバイスリスト取得リクエスト用レスポンス結果クラス
 */
public class ResponseDevicesResult {
    // センサーデバイスリストを格納するコンテナ ("data")
    private final ResponseDevicesData data;
    // レスポンスステータス
    private final ResponseStatus status;

    public ResponseDevicesResult(ResponseDevicesData data, ResponseStatus status) {
        this.data = data;
        this.status = status;
    }

    public ResponseDevicesData getData() {
        return data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ResponseDevicesResult{" +
                "data=" + data +
                ", status=" + status +
                '}';
    }

}
