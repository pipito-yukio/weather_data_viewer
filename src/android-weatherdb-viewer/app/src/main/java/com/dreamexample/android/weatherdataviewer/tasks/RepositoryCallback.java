package com.dreamexample.android.weatherdataviewer.tasks;

public interface RepositoryCallback<T> {
    void onComplete(Result<T> result);
}
