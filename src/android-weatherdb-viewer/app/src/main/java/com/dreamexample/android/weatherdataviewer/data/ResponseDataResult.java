package com.dreamexample.android.weatherdataviewer.data;

public class ResponseDataResult {
    private final ResponseData data;
    private final ResponseStatus status;

    public ResponseDataResult(ResponseData data, ResponseStatus status) {
        this.data = data;
        this.status = status;
    }

    public ResponseData getData() {
        return data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ResponseResult{" +
                "data=" + data +
                ", status=" + status +
                '}';
    }
}
