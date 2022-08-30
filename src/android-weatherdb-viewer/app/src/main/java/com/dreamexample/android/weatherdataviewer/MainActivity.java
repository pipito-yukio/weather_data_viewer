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
//    // FAB オブジェクト: 2つのフラグメントをコードにより切り替えるためのボタン
//    FloatingActionButton mFloatingNav;
    // 次フラグメントインデックス用配列
//    int[] itemIndexes = {1, 0};
    // フラグメントに対応する FABオブジェクトのアイコン (arrow next, arror back)
//    Drawable[] itemIcons = new Drawable[2];

    ConnectivityManager.NetworkCallback mNetCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 画面に矢印アイコンがないと初見ユーザーが次画面が有るかどうかわからないため設けた
//        mFloatingNav = findViewById(R.id.floatingNav);
//        itemIcons[0] = mFloatingNav.getDrawable();
//        itemIcons[1] = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back);
//        // コードで次のフラグメントを選択 ※左右のフリック(スライド)操作でも画面を切り替え可能
//        mFloatingNav.setOnClickListener((view)-> {
//            int currentItem = mViewPager2.getCurrentItem();
//            Log.d(TAG, "currentItem: " + currentItem);
//            mViewPager2.setCurrentItem(itemIndexes[currentItem]);
//        });

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
                // FABアイコンを表示ページ(フラグメント)に対応するアイコンを切り替える
//                mFloatingNav.setImageDrawable(itemIcons[position]);
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
