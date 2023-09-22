package com.dreamexample.android.weatherdataviewer.tasks;

/**
 * GraphFragment用データ取得リポジトリ
 */
public class WeatherGraphRepository extends WeatherImageRepository {
    /** 本日気象データグラフ[0], 本日から指定日前[1] */
    private static final String[] URL_PATHS = {
            "/gettodayimageforphone", "/getbeforedaysimageforphone"
    };

    public WeatherGraphRepository() {}

    @Override
    public String getRequestPath(int pathIdx) {
        return URL_PATHS[pathIdx];
    }

}
