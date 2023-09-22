package com.dreamexample.android.weatherdataviewer;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.dreamexample.android.weatherdataviewer.ui.main.AppTodayGraphFragment;
import com.dreamexample.android.weatherdataviewer.ui.main.AppTopFragment;
import com.dreamexample.android.weatherdataviewer.ui.main.MultiScreenFragmentAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * アプリメインアクティビィティ
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // SettingsActivityのプリファレンスウィジットの変更を通知するコールバックインターフェイス
    public interface SettingsChangeCallback {
        void onChanged(SharedPreferences sharedPref, String key);
    }


    // 気象データ表示版データ画面
    private static final int FRAGMENT_APP_TOP = 0;
    // 当日・指定日前データグラフ表示画面
    private static final int FRAGMENT_GRAPH_BEFORE_DAYS = 1;

    MultiScreenFragmentAdapter mFragmentAdapter;
    ViewPager2 mViewPager2;
    ViewPager2.OnPageChangeCallback mOnPageChangeCallback;

    private SharedPreferences mDefaultPref;

    private final Map<String, SettingsChangeCallback> mSettingsChangeCallbacks = new HashMap<>();

    // SettingsActivityでの設定変更リスナー
    private final OnSharedPreferenceChangeListener mSharedPrefListener = (pref, key) -> {
        DEBUG_OUT.accept(TAG, "mSharedPrefListener.key: " + key);
        if (mSettingsChangeCallbacks.containsKey(key)) {
            // キーに一致するコールバックを呼び出す
            SettingsChangeCallback callback = mSettingsChangeCallbacks.get(key);
            assert callback != null;
            callback.onChanged(pref, key);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager2 = findViewById(R.id.pager);
        mFragmentAdapter = new MultiScreenFragmentAdapter(
                getSupportFragmentManager(),
                getLifecycle()
        );
        // 最新テータ取得・表示フラグメント
        mFragmentAdapter.addFragment(AppTopFragment.newInstance(FRAGMENT_APP_TOP));
        // 当日グラフ取得・表示フラグメント
        mFragmentAdapter.addFragment(AppTodayGraphFragment.newInstance(FRAGMENT_GRAPH_BEFORE_DAYS));
        mViewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mViewPager2.setAdapter(mFragmentAdapter);

        // SettingsActivityを起動しないとXMLでデフォルト値(true)に設定していても反映されない
        // SettingsActivityを起動することなくデフォルト値を設定する方法
        // https://coderanch.com/t/755849/Im-default-time-running-app
        //   How come Im getting the default value the first time running the app?
        // https://developer.android.com/reference/androidx/preference/PreferenceManager
        //  Sets the default values from an XML preference file
        //   by reading the values defined by each Preference item's android:defaultValue attribute.
        PreferenceManager.setDefaultValues(this, R.xml.pref_screen_main_activity,
                false/*readAgain*/);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        // for DEBUG
        DEBUG_OUT.accept(TAG, "DefaultSharedPreferences: " + sharedPref.getAll());

        // プリファレンスの変更をモニタする
        mDefaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        mDefaultPref.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        DEBUG_OUT.accept(TAG, "onStart()");

        // ページャの切り替わりをモニタするコールバック
        mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                DEBUG_OUT.accept(TAG, "position: " + position);
            }
        };
        // ページャにコールバックを登録
        mViewPager2.registerOnPageChangeCallback(mOnPageChangeCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        DEBUG_OUT.accept(TAG, "onStop()");

        // ページャコールバックの登録解除
        if (mOnPageChangeCallback != null) {
            mViewPager2.unregisterOnPageChangeCallback(mOnPageChangeCallback);
            mOnPageChangeCallback = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DEBUG_OUT.accept(TAG, "onDestroy()");

        mDefaultPref.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        mSettingsChangeCallbacks.clear();
    }

    @Override
    public void onBackPressed() {
        DEBUG_OUT.accept(TAG, "onBackPressed()");

        if (mOnPageChangeCallback != null) {
            int currentItem = mViewPager2.getCurrentItem();
            if (currentItem == FRAGMENT_APP_TOP) {
                // 登録画面でバックキー押下なら最近の画面に残さない
                DEBUG_OUT.accept(TAG, "finishAndRemoveTask()");
                finishAndRemoveTask();
            } else if (currentItem > FRAGMENT_APP_TOP){
                // 画像表示フラグメントの場合
                DEBUG_OUT.accept(TAG, "Back previous fragment.");
                // 一つ前に戻る
                mViewPager2.setCurrentItem(mViewPager2.getCurrentItem() - 1);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // id in values/menu/menu_main.xml
        if (id == R.id.actionWeatherDataViewerSettings) {
            showSettingsActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO
    public Map<String, SettingsChangeCallback> getSettingsChangeCallbacks() {
        return mSettingsChangeCallbacks;
    }

    private void showSettingsActivity() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

}
