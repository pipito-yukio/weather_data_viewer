package com.dreamexample.android.weatherdataviewer.ui.main;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.dreamexample.android.weatherdataviewer.R;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;
import com.dreamexample.android.weatherdataviewer.data.ResponseGraphResult;
import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;
import com.dreamexample.android.weatherdataviewer.tasks.NetworkUtil;
import com.dreamexample.android.weatherdataviewer.tasks.Result;
import com.dreamexample.android.weatherdataviewer.tasks.WeatherGraphRepository;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/*
当日データ取得(Matplotlibで出力したグラフ画像)・表示フラグメント
*/
public class TodayGraphFragment extends Fragment {
    private static final String TAG = "TodayDataFragment";

    private final String NO_IMAGE_FILE = "NoImage_500x700.png";
    private final String SPINNER_DEFAULT_ITEM_IDEX = "0";
    private ImageView mImageView;
    private TextView mValResponseStatus;
    private Bitmap mNoImageBitmap;
    private DisplayMetrics mMetrics;
    private RadioButton mRadioToday;
    private RadioGroup mDayGroup;
    private ViewGroup mSpinnerFrame;
    private Spinner mCboBeforeDays;
    private int mImageWd;
    private int mImageHt;

    public static TodayGraphFragment newInstance() {
        return new TodayGraphFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        Log.d(TAG, "" + mMetrics);

        View mainView = inflater.inflate(R.layout.fragment_today_graph, container,false);
        mImageView = mainView.findViewById(R.id.currentTimeDataGraph);
        mValResponseStatus = mainView.findViewById(R.id.valGraphResponseStatus);
        Button btnUpdate = mainView.findViewById(R.id.btnImageUpdate);
        mSpinnerFrame = mainView.findViewById(R.id.SpinnerFrame);
        // 前日指定のスピナーはDisabled
        mCboBeforeDays = mainView.findViewById(R.id.cboBoforeDays);
        mCboBeforeDays.setSelection(0);
        mCboBeforeDays.setEnabled(false);
        mRadioToday = mainView.findViewById(R.id.radioToday);
        mDayGroup = mainView.findViewById(R.id.dayGroup);
        // デフォルト: "本日"
        mDayGroup.check(R.id.radioToday);
        mDayGroup.setOnCheckedChangeListener(
                (group, checkedId) -> mCboBeforeDays.setEnabled(!(checkedId == R.id.radioToday))
        );
        initButton(btnUpdate);
        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // 初期イメージ設定
        if (mNoImageBitmap == null) {
            AssetManager am = getContext().getAssets();
            try {
                mNoImageBitmap = BitmapFactory.decodeStream(am.open(NO_IMAGE_FILE));
                Log.w(TAG, "mNoImageBitmap: " + mNoImageBitmap);
                if (mNoImageBitmap != null) {
                    mImageView.setImageBitmap(mNoImageBitmap);
                    mImageWd = mImageView.getWidth();
                    mImageHt = mImageView.getHeight();
                    Log.d(TAG, "ImageView.width: " + mImageWd + ",height: " + mImageHt);
                }
                // Spinner set dropdownWidth
                Log.d(TAG, "SpinnerFrame width: " + mSpinnerFrame.getWidth());
                mCboBeforeDays.setDropDownWidth(mSpinnerFrame.getWidth());
            }catch (IOException iex) {
                // 通常ここには来ない
                Log.w(TAG, iex.getLocalizedMessage());
            }
        }
    }

    private void initButton(Button btnUpdate) {
        btnUpdate.setOnClickListener(view -> {
            RequestDevice device =  NetworkUtil.getActiveNetworkDevice(getContext());
            if (device == RequestDevice.NONE) {
                showNetworkUnavailable();
                return;
            }

            btnUpdate.setEnabled(false);
            showActionBarGetting(device);

            WeatherApplication app = (WeatherApplication) getActivity().getApplication();
            WeatherGraphRepository repository = new WeatherGraphRepository();
            String requestUrl = app.getmRequestUrls().get(device.toString());
            Map<String, String> headers = app.getRequestHeaders();
            // ImageViewサイズとDisplayMetrics.densityをリクエストヘッダに追加する
            //  (例) X-Request-Image-Size: 1064x1593x1.50
            // サイズは半角数値なのでロケールは "US"とする
            String imgSize = String.format(Locale.US, "%dx%dx%f",
                    mImageWd, mImageHt, mMetrics.density);
            Log.d(TAG, "Key: " + WeatherApplication.REQUEST_IMAGE_SIZE_KEY + "=" + imgSize);
            headers.put(WeatherApplication.REQUEST_IMAGE_SIZE_KEY, imgSize);
            // リクエストURLをAppBarに表示
            int beforeIndex = getSelectedbeforeIndex();
            String requestUrlWithPath = requestUrl + repository.getRequestPath(beforeIndex);
            String requestParameter;
            if (beforeIndex == 1) {
                Log.d(TAG, "Spinner selectItem: " + mCboBeforeDays.getSelectedItem());
                if (mCboBeforeDays.getSelectedItem() != null) {
                    requestParameter = "?before_days=" + mCboBeforeDays.getSelectedItem();
                } else {
                    requestParameter = "?before_days=" + SPINNER_DEFAULT_ITEM_IDEX;
                }
            } else {
                requestParameter = "";
            }
            repository.makeCurrentTimeDataRequest(beforeIndex, requestUrl, requestParameter,
                    headers, app.mEexecutor, app.mdHandler, (result) -> {
                // ボタン状態を戻す
                btnUpdate.setEnabled(true);
                // リクエストURLをAppBarに表示
                showActionBarResult(requestUrlWithPath);
                if (result instanceof Result.Success) {
                    showSuccess((Result.Success<ResponseGraphResult>) result);
                } else if (result instanceof Result.Warning) {
                    showWarning((Result.Warning<ResponseGraphResult>) result);
                } else if (result instanceof Result.Error) {
                    showError((Result.Error<ResponseGraphResult>) result);
                }
            });
        });
    }

    private void showNetworkUnavailable() {
        mValResponseStatus.setText(
                getResources().getString(R.string.msg_network_not_available)
        );

        mValResponseStatus.setVisibility(View.VISIBLE);
    }

    private void showActionBarGetting(RequestDevice device) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        // AppBarタイトル: ネットワーク接続種別
        if (device == RequestDevice.MOBILE) {
            TelephonyManager manager =
                    (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String operatorName = manager.getNetworkOperatorName();
            bar.setTitle(device.getMessage() + " (" + operatorName +")");
        } else {
            bar.setTitle(device.getMessage());
        }
        // AppBarサブタイトル: 取得中
        bar.setSubtitle(getResources().getString(R.string.msg_gettting_graph));
    }

    private void showActionBarResult(String reqUrlWithPath) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        // AppBarサブタイトル: リクエストURL
        bar.setSubtitle(reqUrlWithPath);
    }

    private void showSuccess(Result.Success<ResponseGraphResult> result) {
        if (mValResponseStatus.getVisibility() == View.VISIBLE) {
            mValResponseStatus.setText("");
            mValResponseStatus.setVisibility(View.GONE);
        }
        byte[] decoded = result.get().getImageBytes();
        if (decoded != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    decoded, 0, decoded.length);
            mImageView.setImageBitmap(bitmap);
        }
    }

    private void showWarning(Result.Warning<ResponseGraphResult> result) {
        ResponseStatus status = result.getResponseStatus();
        mValResponseStatus.setText(
                String.format(getResources().getString(R.string.format_warning),
                        status.getCode(), status.getMessage())
        );
        mValResponseStatus.setVisibility(View.VISIBLE);
    }

    private void showError(Result.Error<ResponseGraphResult> result) {
        Exception exception = result.getException();
        mValResponseStatus.setText(exception.getLocalizedMessage());
        mValResponseStatus.setVisibility(View.VISIBLE);
    }

    private int getSelectedbeforeIndex() {
        int radioId = mDayGroup.getCheckedRadioButtonId();
        if (mRadioToday.getId() == radioId) {
            return 0;
        }
        return 1;
    }
}
