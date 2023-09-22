package com.dreamexample.android.weatherdataviewer.data;

public class DeviceItem {
    private final String name;
    private final String description;

    public DeviceItem(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "DeviceItem{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
