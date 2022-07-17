package com.dreamexample.android.weatherdataviewer.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.dreamexample.android.weatherdataviewer.R;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;
import com.dreamexample.android.weatherdataviewer.data.ResponseData;
import com.dreamexample.android.weatherdataviewer.data.ResponseDataResult;
import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;
import com.dreamexample.android.weatherdataviewer.tasks.NetworkUtil;
import com.dreamexample.android.weatherdataviewer.tasks.Result;
import com.dreamexample.android.weatherdataviewer.tasks.WeatherDataRepository;

import java.util.Map;

/*
最新データ取得・表示フラグメント
*/
public class TodayDataFragment extends Fragment {
    private static final String TAG = "TodayDataFragment";

    private TextView mValMeasurementDatetime;
    private TextView mValTempOut;
    private TextView nValTempIn;
    private TextView mValHumid;
    private TextView mValPressure;
    private TableRow mMessageRow;
    private TextView mValResponseStatus;

    public static TodayDataFragment newInstance() {
        return new TodayDataFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_today_data, container, false);
        mMessageRow = mainView.findViewById(R.id.messageRow);
        mValResponseStatus = mainView.findViewById(R.id.valResponseStatus);
        mValMeasurementDatetime = mainView.findViewById(R.id.valMeasurementDatetime);
        mValTempOut = mainView.findViewById(R.id.valTempOut);
        nValTempIn = mainView.findViewById(R.id.valTempin);
        mValHumid = mainView.findViewById(R.id.valHumid);
        mValPressure = mainView.findViewById(R.id.valPressure);
        Button btnUpdate = mainView.findViewById(R.id.btnUpdate);
        initButton(btnUpdate);
        return mainView;
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
            WeatherDataRepository repository = new WeatherDataRepository();
            // アプリケーションに保持しているリクエスト情報からネットワーク種別に応じたリクエストURLを取得
            String requestUrl = app.getmRequestUrls().get(device.toString());
            Map<String, String> headers = app.getRequestHeaders();
            // イメージ表示フラグメントが追加したヘッダを取り除く
            headers.remove(WeatherApplication.REQUEST_IMAGE_SIZE_KEY);
            String requestUrlWithPath = requestUrl + repository.getRequestPath();
            repository.makeCurrentTimeDataRequest(
                    requestUrl, headers, app.mEexecutor, app.mdHandler, (result) -> {
                // ボタン状態を戻す
                btnUpdate.setEnabled(true);
                // リクエストURLをAppBarに表示
                showActionBarResult(requestUrlWithPath);
                if (result instanceof Result.Success) {
                    showSuccess((Result.Success<ResponseDataResult>) result);
                } else if (result instanceof Result.Warning) {
                    showWarning((Result.Warning<ResponseDataResult>) result);
                } else if (result instanceof Result.Error) {
                    showError((Result.Error<ResponseDataResult>) result);
                }
            });
        });
    }

    private void showNetworkUnavailable() {
        mValResponseStatus.setText(
                getResources().getString(R.string.msg_network_not_available)
        );

        mMessageRow.setVisibility(View.VISIBLE);
    }

    private void showActionBarGetting(RequestDevice device) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        // AppBarタイトル: ネットワーク接続種別
        if (device == RequestDevice.MOBILE) {
            TelephonyManager manager =
                    (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            // モバイル接続の場合はキャリア名にかっこ付きで表示
            String operatorName = manager.getNetworkOperatorName();
            bar.setTitle(device.getMessage() + " (" + operatorName +")");
        } else {
            bar.setTitle(device.getMessage());
        }
        // AppBarサブタイトル: 取得中
        bar.setSubtitle(getResources().getString(R.string.msg_gettting_data));
    }

    private void showActionBarResult(String reqUrlWithPath) {
        ActionBar bar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        // AppBarサブタイトル: リクエストURL
        bar.setSubtitle(reqUrlWithPath);
    }

    private void showSuccess(Result.Success<ResponseDataResult> result) {
        if (mValResponseStatus.getVisibility() == View.VISIBLE) {
            mValResponseStatus.setText("");
            mValResponseStatus.setVisibility(View.GONE);
        }
        ResponseData data = result.get().getData();
        mValMeasurementDatetime.setText(
                (data.getMeasurementTime() != null ? data.getMeasurementTime() :
                        getResources().getString(R.string.init_measurement_time))
        );
        mValTempOut.setText(String.valueOf(data.getTempOut()));
        nValTempIn.setText(String.valueOf(data.getTempIn()));
        mValHumid.setText(String.valueOf(data.getHumid()));
        long pressure = Math.round(data.getPressure());
        mValPressure.setText(String.valueOf(pressure));
    }

    private void showWarning(Result.Warning<ResponseDataResult> result) {
        ResponseStatus status = result.getResponseStatus();
        mValResponseStatus.setText(
                String.format(getResources().getString(R.string.format_warning),
                        status.getCode(), status.getMessage())
        );
        mMessageRow.setVisibility(View.VISIBLE);
    }

    private void showError(Result.Error<ResponseDataResult> result) {
        Exception exception = result.getException();
        mValResponseStatus.setText(exception.getLocalizedMessage());
        mMessageRow.setVisibility(View.VISIBLE);
    }
}
