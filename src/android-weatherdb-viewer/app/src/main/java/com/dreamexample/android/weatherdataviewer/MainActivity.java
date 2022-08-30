package com.dreamexample.android.weatherdataviewer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import androidx.viewpager2.widget.ViewPager2;

//import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.dreamexample.android.weatherdataviewer.ui.main.MultiScreenFragmentAdapter;
import com.dreamexample.android.weatherdataviewer.ui.main.TodayDataFragment;
import com.dreamexample.android.weatherdataviewer.ui.main.TodayGraphFragment;

import java.net.ConnectException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    MultiScreenFragmentAdapter mFragmentAdapter;
    ViewPager2 mViewPager2;
    ViewPager2.OnPageChangeCallback mOnPageChangeCallback;

    ConnectivityManager.NetworkCallback mNetCallback;

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
        mFragmentAdapter.addFragment(TodayDataFragment.newInstance());
        // 当日グラフ取得・表示フラグメント
        mFragmentAdapter.addFragment(TodayGraphFragment.newInstance());
        mViewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mViewPager2.setAdapter(mFragmentAdapter);

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

        // ページャの切り替わりをモニタするコールバック
        mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "position: " + position);
            }
        };
        // ページャにコールバックを登録
        mViewPager2.registerOnPageChangeCallback(mOnPageChangeCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");

        // ページャコールバックの登録解除
        if (mOnPageChangeCallback != null) {
            mViewPager2.unregisterOnPageChangeCallback(mOnPageChangeCallback);
            mOnPageChangeCallback = null;
        }
    }

}
