package com.dreamexample.android.weatherdataviewer.data;

import android.util.Log;
import java.util.Base64;

public class ResponseGraphResult {
    private final ResponseGraph data;
    private final ResponseStatus status;

    public ResponseGraphResult(ResponseGraph data, ResponseStatus status) {
        this.data = data;
        this.status = status;
    }

    public byte[] getImageBytes() {
        if (data != null) {
            // img_src = "data:image/png;base64, ..base64string..."
            String[] datas = data.getImgSrc().split(",");
            Log.d("ResponseGraphResult", "datas[0]" + datas[0]);
            return Base64.getDecoder().decode(datas[1]);
        } else {
            return null;
        }
    }

    public ResponseStatus getStatus() {
        return status;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        int imgLength = 0;
        if (data != null && data.getImgSrc() != null) {
            imgLength = data.getImgSrc().length();
        }
        return "ResponseGraphResult{" +
                "imageBase64String.length=" + imgLength +
                ", status=" + (status != null ? status : "NULL") +
                '}';
    }
}
