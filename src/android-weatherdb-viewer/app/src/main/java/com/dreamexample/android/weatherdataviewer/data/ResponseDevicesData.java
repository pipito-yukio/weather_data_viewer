package com.dreamexample.android.weatherdataviewer.data;

import java.util.List;

/**
 * センサーデバイスリストを格納するコンテナクラス
 */
public class ResponseDevicesData {
    // センサーディバイスリスト ("devices":[デバイスリスト])
    private final List<DeviceItem> devices;

    public ResponseDevicesData(List<DeviceItem> devices) {
        this.devices = devices;
    }

    public List<DeviceItem> getDevices() {
        return devices;
    }

    @Override
    public String toString() {
        return "ResponseDevicesData{" +
                "devices=" + devices +
                '}';
    }
}
