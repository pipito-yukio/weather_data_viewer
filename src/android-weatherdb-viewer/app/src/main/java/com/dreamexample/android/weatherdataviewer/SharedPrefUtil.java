package com.dreamexample.android.weatherdataviewer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * SharedPreferencesユーティリィティクラス
 * Activity, Fragmentで共通でアクセスするメソッド定義
 */
public class SharedPrefUtil {

    /**
     * コンテキストに属するSharedPreferencesを取得する
     * @param context Application | Activity
     * @param prefKey preference key
     * @return SharedPreferences
     */
    public static SharedPreferences getSharedPrefWithKey(Context context, String prefKey) {
        return context.getSharedPreferences(prefKey, Context.MODE_PRIVATE);
    }

    /**
     * AppTopFragmentが属するアクティビィティのプリファレンスを取得する
     * @param context Activityコンテキスト
     * @return プリファレンス
     */
    public static SharedPreferences getSharedPrefInMainActivity(Context context) {
        return getSharedPrefWithKey(context,
                context.getString(R.string.pref_app_top_fragment));
    }

    /**
     * 選択されたセンサーデバイス名を取得する
     * @param context Activityコンテキスト
     * @return 選択されたセンサーデバイス名
     */
    public static String getSelectedDeviceName(Context context) {
        SharedPreferences sharedPref = getSharedPrefInMainActivity(context);
        String key = context.getString(R.string.pref_selected_device_name);
        return sharedPref.getString(key, null);
    }

    /**
     * 選択されたセンサーデバイス名をプリファレンスに保存する
     * @param context Activityコンテキスト
     * @param value 選択されたセンサーデバイス名を
     */
    public static void saveSelectedDeviceName(Context context, String value) {
        SharedPreferences sharedPref = getSharedPrefInMainActivity(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.pref_selected_device_name), value);
        editor.apply();
    }

    //** For SettingsActivity ************************************************************
     /**
     * 気象データ表示版データの保存設定を取得する
     * @param context Activityコンテキスト
     * @return 保存するが設定されていたらtrue
     */
    public static boolean isSaveLatestDataInSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context);
        return prefs.getBoolean(context.getString(R.string.pref_screen_key_save_latest_data),
                false);
    }

    /**
     * 取得した画像の保存設定を取得する
     * @param context Activityコンテキスト
     * @return 保存するが設定されていたらtrue
     */
    public static boolean isSaveGraphImageInSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context);
        return prefs.getBoolean(context.getString(R.string.pref_screen_key_save_today_image),
                false);
    }

}
