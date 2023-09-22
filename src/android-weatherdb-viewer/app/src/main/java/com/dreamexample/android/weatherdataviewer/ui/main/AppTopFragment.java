package com.dreamexample.android.weatherdataviewer.ui.main;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dreamexample.android.weatherdataviewer.MainActivity;
import com.dreamexample.android.weatherdataviewer.MainActivity.SettingsChangeCallback;
import com.dreamexample.android.weatherdataviewer.R;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;
import com.dreamexample.android.weatherdataviewer.data.DeviceItem;
import com.dreamexample.android.weatherdataviewer.data.ResponseData;
import com.dreamexample.android.weatherdataviewer.data.ResponseDataResult;
import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;
import com.dreamexample.android.weatherdataviewer.functions.AppFragmentUtil;
import com.dreamexample.android.weatherdataviewer.functions.FileManager;
import com.dreamexample.android.weatherdataviewer.tasks.NetworkUtil;
import com.dreamexample.android.weatherdataviewer.tasks.RequestParamBuilder;
import com.dreamexample.android.weatherdataviewer.tasks.Result;
import com.dreamexample.android.weatherdataviewer.tasks.WeatherDataRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 最新データ取得・表示フラグメント
 */
public class AppTopFragment extends AppBaseFragment {
    private static final String TAG = AppTopFragment.class.getSimpleName();

    // 最新データ表示ビュー
    private TextView mValMeasurementDatetime;
    private TextView mValTempOut;
    private TextView mValTempIn;
    private TextView mValHumid;
    private TextView mValPressure;
    // ウォーニング表示用ビュー ※非表示
    private TextView mWarningView;
    // 下部コンテナーウィジット
    private Button mBtnUpdate;
    private Spinner mSpinnerSensor;
    // スピナーアダブター
    private ArrayAdapter<String> mSpinnerAdapter;
    private CheckBox mChkGetLatest;
    private AppFragmentUtil.SpinnerSelected mSpinnerSelected;
    // リクエストバラメータ用デバイス名
    private String mRequestDeviceName;
    // 最新データファイル保存パスへのキー
    private String mPrefLatestDataPathKey;
    // ストップ時保存済みプリファレンスキー
    private String mPrefKeyStopSaved;
    // pref_screen_main_activityで定義されている最新データ保存スイッチへのキー
    private String mSaveDataInPrefScreenKey;

    /**
     * コンストラクタ
     * @param fragPosIdx フラグメント位置インデックス
     * @return このフラグメント
     */
    public static AppTopFragment newInstance(int fragPosIdx) {
        AppTopFragment frag = new AppTopFragment();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_POS_KEY, fragPosIdx);
        frag.setArguments(args);
        return frag;
    }

    //** START implements abstract methods **************************
    @Override
    public int getFragmentPosition() {
        assert getArguments() != null;
        return getArguments().getInt(FRAGMENT_POS_KEY, 0);
    }

    @Override
    public String getFragmentTitle() { return getString(R.string.app_title_frag_top); }

    public ImageView getImageView() {
        // ImageView none
        return null;
    }

    @Override
    public TextView getWaringView() {
        assert mWarningView != null;
        return mWarningView;
    }

    /**
     * センサーデバイススピナーで選択されているセンサーデバイス名(主キー)を取得
     * @return センサーデバイス名(主キー)
     */
    private String getSelectedDeviceName() {
        int selectedPos = mSpinnerSelected.getPosition();
        // リクエストパラメータに必要なデバイス名をTAGから取得
        Object obj = mSpinnerSensor.getTag();
        assert obj != null;
        // プログラムで設定しているのが明白なのでウォーニングを抑制
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>)obj;
        String result = names.get(selectedPos);
        DEBUG_OUT.accept(TAG, "getSelectedDeviceName.names[" + selectedPos + "]: " + result);
        return result;
    }

    // 更新ボタンクリックリスナー
    private final  View.OnClickListener mButtonClickListener = (view) -> performRequest();

    // センサーデバイス選択イベントリスナー
    private final AdapterView.OnItemSelectedListener mSpinnerListener
            = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String value = (String) parent.getItemAtPosition(position);
            mSpinnerSelected.setPosition(position);
            mSpinnerSelected.setValue(value);
            DEBUG_OUT.accept(TAG, "onItemSelected[" + mSpinnerSelected + "]");
            mRequestDeviceName = getSelectedDeviceName();
            saveSelectedDeviceNameInPref(mRequestDeviceName);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // プログラムで操作しないとここには来ない
            mSpinnerSelected.setPosition(AppFragmentUtil.SpinnerSelected.UNSELECTED);
            mSpinnerSelected.setValue(null);
        }
    };

    /**
     * SettingsActivityで保存設定の変更を受け取るコールバック
     * <p>MainActivityでSettingsActivityでの保存設定の変更をモニターしている</p>
     */
    private final SettingsChangeCallback mSettingsChangeCallback = (pref, key) -> {
        if (key.equals(mSaveDataInPrefScreenKey)) {
            if (!pref.getBoolean(key, false)) {
                // 保存なしに変更されたら保存済みファイルとプリファレンスをクリーンアップする
                cleanupSavedData();
                DEBUG_OUT.accept(TAG, key + "Cleanup　complete " + mSaveDataInPrefScreenKey + "");
            }
        }
    };

    /**
     * 暗黙的ネットワークリクエストによるファイル保存結果コールバック
     * <p>AppBaseFragmentクラスで暗黙的ネットワークリクエストが完了したら呼び出される</p>
     */
    private final ImplicitlyRequestCallback mSavedCallback = new ImplicitlyRequestCallback() {
        @Override
        public void onComplete(String callbackKey) {
            DEBUG_OUT.accept(TAG, "onComplete(" + callbackKey + ")");
            if (!callbackKey.equals(getString(R.string.devices_json_file))) {
                // ファイル名が対象のもの以外なら無視
                return;
            }

            loadDevicesFromFile();
            // サプタイトルに交信完了を表示
            setRequestMessage(getString(R.string.msg_implicitly_completed));
        }

        @Override
        public void onFailure(String callbackKey, String errorMessage) {
            DEBUG_OUT.accept(TAG, "onFailure(" + errorMessage + ")");
            if (!callbackKey.equals(getString(R.string.devices_json_file))) {
                // ファイル名が対象のもの以外なら無視
                return;
            }

            showWarningInWarningView(mWarningView, errorMessage);
        }
    };

    private void loadDevicesFromFile() {
        getHandler().post(()-> {
            try {
                List<String> lines = FileManager.readLines(requireContext(),
                        getString(R.string.devices_json_file));
                String jsonText = String.join("", lines);
                DeviceItem[] devices = getGson().fromJson(jsonText, DeviceItem[].class);
                // デバイスリストがあればスピナー有効
                boolean isEnable = devices.length > 0;
                mSpinnerSensor.setEnabled(isEnable);
                mBtnUpdate.setEnabled(isEnable);
                mChkGetLatest.setEnabled(isEnable);
                if (isEnable) {
                    List<DeviceItem> deviceList = Arrays.asList(devices);
                    // スピナーの更新
                    setDevicesToSpinnerAdapter(deviceList);
                    DEBUG_OUT.accept(TAG, "device.names: " + mSpinnerSensor.getTag());
                }
            } catch (IOException e) {
                // 想定しない
                String errMsg = String.format(getString(R.string.warning_save_with_2reason),
                        getString(R.string.warning_load_devices_error),
                        e.getLocalizedMessage());
                // ウォーニングステータスに表示
                showWarningInWarningView(mWarningView, errMsg);
            }
        });
    }

    /**
     * センサーデバイスリストをスピナーアダブターに反映する
     * @param devices センサーデバイスリスト
     */
    private void setDevicesToSpinnerAdapter(List<DeviceItem> devices) {
        DEBUG_OUT.accept(TAG, "setDevicesToSpinnerAdapter: " + devices);
        List<String> descriptions = devices.stream()
                .map(DeviceItem::getDescription)
                .collect(Collectors.toList());
        List<String> names = devices.stream()
                .map(DeviceItem::getName)
                .collect(Collectors.toList());
        if (!mSpinnerAdapter.isEmpty()) {
            mSpinnerAdapter.clear();
        }
        mSpinnerAdapter.addAll(descriptions);
        // デバイス名は必須リクエストパラメータのためスピナーのTAGに格納する
        mSpinnerSensor.setTag(names);
    }

    /**
     * スピナーを構成する
     * @param mainView View
     */
    private void initSpinnerView(View mainView) {
        //  スピナー
        mSpinnerSensor = mainView.findViewById(R.id.spinnerSensor);
        mSpinnerSensor.setEnabled(false);
        // 空リスト(文字列)のアダブターを設定する
        mSpinnerAdapter = new ArrayAdapter<>(getContext(), R.layout.custom_spinner_item);
        // アダプターがスピナー選択リストに表示するために使用するレイアウト
        mSpinnerAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mSpinnerSensor.setAdapter(mSpinnerAdapter);
        // 初期画面ではスピナー不可
        mSpinnerSensor.setEnabled(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 最新データ保存設定キー
        mSaveDataInPrefScreenKey = getString(R.string.pref_screen_key_save_latest_data);
        // 画像保存設定の変更を通知してくれるコールバック
        Map<String, MainActivity.SettingsChangeCallback> callbackMap = getSettingsChangeCallbacks();
        callbackMap.put(mSaveDataInPrefScreenKey, mSettingsChangeCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        DEBUG_OUT.accept(TAG, "onCreateView()");

        View mainView = inflater.inflate(R.layout.fragment_top_main, container, false);
        mWarningView = mainView.findViewById(R.id.tvWearningStatusView);
        mValMeasurementDatetime = mainView.findViewById(R.id.valMeasurementDatetime);
        mValTempOut = mainView.findViewById(R.id.valTempOut);
        mValTempIn = mainView.findViewById(R.id.valTempin);
        mValHumid = mainView.findViewById(R.id.valHumid);
        mValPressure = mainView.findViewById(R.id.valPressure);
        mBtnUpdate = mainView.findViewById(R.id.btnTopFragUpdate);
        mBtnUpdate.setOnClickListener(mButtonClickListener);
        // センサー関連ウィジット
        // センサーデバイススピナー
        initSpinnerView(mainView);
        mChkGetLatest = mainView.findViewById(R.id.chkGetLatest);
        // 空のスピナー選択オブジェクト生成
        mSpinnerSelected = new AppFragmentUtil.SpinnerSelected();

        // プリファレンスキー名
        mPrefLatestDataPathKey = getString(R.string.pref_frag_top_latest_data_path);
        mPrefKeyStopSaved = getString(R.string.pref_frag_top_stop_saved);

        return mainView;
    }

    @Override
    public void onResume() {
        DEBUG_OUT.accept(TAG, "onResume(before)");
        // センサーデバイスリストファイルが存在しなければリクエスト結果通知コールバックを登録
        if (!isExistsSensorDevices()) {
            getSavedCallback().add(mSavedCallback);
        }
        // ベースクラスのonResume
        super.onResume();

        DEBUG_OUT.accept(TAG, "onResume(after)");
        if (isExistsSensorDevices()) {
            loadDevicesFromFile();
        }

        // onStop時に保存した最新データが存在すればリストア
        restoreLatestData();
        // スピナーリスナー登録
        mSpinnerSensor.setOnItemSelectedListener(mSpinnerListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        DEBUG_OUT.accept(TAG, "onStart()");
    }

    @Override
    public void onPause() {
        // リクエスト結果通知コールバックのクリアはベースクラスで実行
        super.onPause();
        DEBUG_OUT.accept(TAG, "onPause()");

        // スピナーリスナー解除 ※null可
        mSpinnerSensor.setOnItemSelectedListener(null);
   }

    @Override
    public void onStop() {
        super.onStop();
        DEBUG_OUT.accept(TAG, "onStop()");

        // 取得データを保存する
        saveLatestDataOnStop();
    }
    //** END life cycle events *****************************

    private void performRequest() {
        RequestDevice device = NetworkUtil.getActiveNetworkDevice(requireContext());
        if (device == RequestDevice.NONE) {
            showDialogNetworkUnavailable();
            return;
        }

        mBtnUpdate.setEnabled(false);
        // リクエスト開始メッセージを設定する
        setRequestMessage(getString(R.string.msg_getting_data));

        WeatherApplication app = (WeatherApplication) requireActivity().getApplication();
        WeatherDataRepository repos = new WeatherDataRepository();
        // アプリケーションに保持しているリクエスト情報からネットワーク種別に応じたリクエストURLを取得
        String requestUrl = app.getRequestUrls().get(device.toString());
        assert requestUrl != null;
        // リクエストパラメータ生成: センサーディバイス名 ※必須
        RequestParamBuilder builder = new RequestParamBuilder(mRequestDeviceName);
        String requestParam = builder.build();
        Map<String, String> headers = app.getRequestHeaders();
        // イメージ表示フラグメントが追加したヘッダを取り除く
        headers.remove(WeatherApplication.REQUEST_IMAGE_SIZE_KEY);

        repos.makeGetRequest(0, requestUrl, requestParam, headers,
                app.mEexecutor, app.mdHandler, (result) -> {
                    // ボタン状態を戻す
                    mBtnUpdate.setEnabled(true);
                    // リクエスト完了時にネットワーク種別を表示
                    showRequestComplete(device);

                    if (result instanceof Result.Success) {
                        ResponseDataResult dataResult = ((Result.Success<ResponseDataResult>) result)
                                .get();
                        ResponseData data = dataResult.getData();
                        if (data.getRecCount() > 0) {
                            showSuccess(data);
                        } else {
                            // 指定されたセンサーデバイス名の観測データ無し
                            showWarningInWarningView(mWarningView,
                                    getString(R.string.warning_data_not_found_with_device));
                            // 画面を初期値に戻す
                            resetDataViews();
                        }
                    } else if (result instanceof Result.Warning) {
                        ResponseStatus status =
                                ((Result.Warning<?>) result).getResponseStatus();
                        showWarningWithResponseStatus(status);
                    } else if (result instanceof Result.Error) {
                        // 例外メッセージをダイアログに表示
                        Exception exception = ((Result.Error<?>) result).getException();
                        Log.w(TAG, "Error:" + exception);
                        showDialogExceptionMessage(exception);
                    }
                });
    }

    private void showSuccess(ResponseData data) {
        // ウォーニングが表示されていたら閉じる
        hideWarningView();
        showDataViews(data);
    }

    private void showDataViews(ResponseData data) {
        mValMeasurementDatetime.setText(
                (data.getMeasurementTime() != null ? data.getMeasurementTime() :
                        getResources().getString(R.string.init_measurement_time))
        );
        mValTempOut.setText(String.valueOf(data.getTempOut()));
        mValTempIn.setText(String.valueOf(data.getTempIn()));
        mValHumid.setText(String.valueOf(data.getHumid()));
        long pressure = Math.round(data.getPressure());
        mValPressure.setText(String.valueOf(pressure));
    }

    private void resetDataViews() {
        String initVal = getString(R.string.init_measurement_value);
        mValMeasurementDatetime.setText(getString(R.string.init_measurement_time));
        mValTempOut.setText(initVal);
        mValTempIn.setText(initVal);
        mValHumid.setText(initVal);
        mValPressure.setText(initVal);
    }

    private ResponseData getLatestDataFromDataViews() {
        String measurementTime = mValMeasurementDatetime.getText().toString();
        DEBUG_OUT.accept(TAG, "getLatestDataFromDataViews.measurementTime: " + measurementTime);
        if (!measurementTime.equals(getString(R.string.init_measurement_time))) {
            // 測定時刻ウィジットに取得した測定時刻が設定されていたらウィジットからデータを復元
            String strTempOut = mValTempOut.getText().toString();
            String strTempIn = mValTempIn.getText().toString();
            String strHumid = mValHumid.getText().toString();
            String strPressure = mValPressure.getText().toString();
            try {
                double tempOut = Double.parseDouble(strTempOut);
                double tempIn = Double.parseDouble(strTempIn);
                double humid = Double.parseDouble(strHumid);
                double pressure = Double.parseDouble(strPressure);
                return new ResponseData(measurementTime,
                    tempOut, tempIn, humid, pressure, 1
                );
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "getLatestDataFromDataViews.error: " + nfe.getLocalizedMessage());
                return null;
            }
        }

        // 測定時刻が初期値と同じならnull
        return null;
    }

    /**
     * 最新データ表示の状態をファイルとプリファレンスに保存
     * <p>実行タイミング: onStop()</p>
     */
    private void saveLatestDataOnStop() {
        if (!isSaveLatestData()) {
            // 保存無し設定
            return;
        }

        // 最新データの保存が設定されている場合は表示データをファイルに保存
        ResponseData data = getLatestDataFromDataViews();
        DEBUG_OUT.accept(TAG, "saveLatestDataOnStop.data: " + data);
        if (data == null) {
            // 測定日時が初期値
            return;
        }

        String fileName = getString(R.string.file_name_latest_data);
        // JSON文字列
        String json = getGson().toJson(data);
        DEBUG_OUT.accept(TAG, "saveLatestData: " + json);
        try {
            String absSavePath = FileManager.saveText(requireContext(), fileName, json);
            DEBUG_OUT.accept(TAG, "absSavePath: " + absSavePath);
            // 保存されたJSONファイルパスをプリファレンスに設定
            SharedPreferences sharedPref = getSharedPreferences();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(mPrefLatestDataPathKey, absSavePath);
            // onStop時保存の事実自体の保存 ※復元時にキーを削除
            editor.putString(mPrefKeyStopSaved, getString(R.string.pref_value_stop_saved));
            // アプリ終了につきコミット
            editor.commit();
        } catch (IOException e) {
            // アプリ終了なのでログにウォーニングを出力
            Log.w(TAG, "saveLatestDataOnStop.error: " + e.getLocalizedMessage());
        }
    }

    /**
     * onStop時に保存した最新データが存在すれば対応するウィジットに復元する
     * <p>実行タイミング: onResume()</p>
     */
    private void restoreLatestData() {
        // onStop時の保存キーの存在チェック
        SharedPreferences sharedPref = getSharedPreferences();
        String stopSaved = sharedPref.getString(mPrefKeyStopSaved,null);
        DEBUG_OUT.accept(TAG, "restoreLatestData.stopSaved: " + stopSaved);
        if (stopSaved != null) {
            // 保存されたJSONファイルパスへのキー
            String savedFileName = sharedPref.getString(mPrefLatestDataPathKey, null);
            DEBUG_OUT.accept(TAG, "restoreLatestData.savedFileName: " + savedFileName);
            if (savedFileName != null) {
                try {
                    // 保存パスからJSONを取得
                    String json = FileManager.readTextFromFilePath(savedFileName);
                    DEBUG_OUT.accept(TAG, "restoreLatestData.json: " + json);
                    ResponseData data = getGson().fromJson(json, ResponseData.class);
                    showDataViews(data);
                    // 復元完了でキーを削除する
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove(mPrefKeyStopSaved);
                    editor.apply();
                } catch (IOException e) {
                    Log.w(TAG, "restoreJsonFromFile: " + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * 保存済みファイルと対応するファイルパスキーをプリファレンスから削除する
     * <p>※設定変更コールバックのみから実行される</p>
     */
    public void cleanupSavedData() {
        SharedPreferences sharedPerf = getSharedPreferences();
        String filePath = hasPathInSharedPreference(sharedPerf, mPrefLatestDataPathKey);
        SharedPreferences.Editor editor = sharedPerf.edit();
        if (filePath != null) {
            // 保存ファイル削除
            deleteFileIfExist(filePath);
        }
        // 関連キーを纏めて削除
        editor.remove(mPrefLatestDataPathKey);
        editor.remove(mPrefKeyStopSaved);
        editor.apply();
    }

}
