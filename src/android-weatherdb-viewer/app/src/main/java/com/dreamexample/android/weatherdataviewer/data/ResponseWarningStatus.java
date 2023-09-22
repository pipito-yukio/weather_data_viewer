package com.dreamexample.android.weatherdataviewer.data;

public class ResponseWarningStatus {
    private ResponseStatus status;

    public ResponseWarningStatus(ResponseStatus status) {
        this.status = status;
    }
    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ResponseWarningStatus{status=" + status + '}';
    }
}
