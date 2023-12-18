package com.dreamexample.android.weatherdataviewer.ui.main;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dreamexample.android.weatherdataviewer.BuildConfig;
import com.dreamexample.android.weatherdataviewer.MainActivity;
import com.dreamexample.android.weatherdataviewer.MainActivity.SettingsChangeCallback;
import com.dreamexample.android.weatherdataviewer.R;
import com.dreamexample.android.weatherdataviewer.SharedPrefUtil;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;
import com.dreamexample.android.weatherdataviewer.data.DeviceItem;
import com.dreamexample.android.weatherdataviewer.data.ResponseDevicesData;
import com.dreamexample.android.weatherdataviewer.data.ResponseDevicesResult;
import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;
import com.dreamexample.android.weatherdataviewer.dialogs.CustomDialogs;
import com.dreamexample.android.weatherdataviewer.functions.FileManager;
import com.dreamexample.android.weatherdataviewer.tasks.NetworkUtil;
import com.dreamexample.android.weatherdataviewer.tasks.Result;
import com.dreamexample.android.weatherdataviewer.tasks.WeatherDevicesRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AppBaseFragment extends Fragment {
    private static final String TAG = AppBaseFragment.class.getSimpleName();

    /**
     * 暗黙的リクエスト時の通信完了コールバック
     * <p>このクラスを継承する複数のフラグメントからコールバックが呼び出される想定</p>
     */
    public interface ImplicitlyRequestCallback {
        /**
         * レスボンス受信完了
         * @param callbackKey コールバックを設定したフラグメントのキー
         */
        void onComplete(String callbackKey);

        /**
         * ウォーニング・エラーレスボンス受信
         * @param callbackKey コールバックを設定したフラグメントのキー
         * @param errorMessage ウォーニング・エラーメッセージ
         */
        void onFailure(String callbackKey, String errorMessage);
    }
    
    // フラグメント位置キー
    public static final String FRAGMENT_POS_KEY = "fragPos";
    // 初期表示用イメージ画像ファイル名
    private static final String NO_IMAGE_FILE = "NoImage_500x700.png";
    // 初期画面ビットマップ
    private Bitmap mNoImageBitmap;
    // ファイル保存、プリファレンスコミット時に利用するハンドラー
    private final Handler mHandler = new Handler();
    // 画像取得リクエスト時のヘッダー用の表示デバイス情報
    private DisplayMetrics mMetrics;

    // ファイル取得コールパックリスト
    private final List<ImplicitlyRequestCallback> mSavedCallbacks = new ArrayList<>();

    // ウォーニングメッセージ用マップ
    private final Map<Integer, String> mResponseWarningMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initResponseWarningMap();
    }

    public Gson getGson() {
        Gson gson;
        if (BuildConfig.DEBUG) {
            // 整形
            gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        } else {
            // 整形なし、nullの場合 nullを出力
            gson = new GsonBuilder().serializeNulls().create();
        }
        return gson;
    }

    public List<ImplicitlyRequestCallback> getSavedCallback() {
        return mSavedCallbacks;
    }

    /**
     * センサーデバイスリストをファイル保存する
     * @param devices センサーデバイスリスト
     */
    private void saveSensorDevices(List<DeviceItem> devices) {
        DEBUG_OUT.accept(TAG, "saveSensorDevices()");
        String fileName = getString(R.string.devices_json_file);
        // バックグラウンドスレッドで保存
        getHandler().post(() -> {
            Gson gson = getGson();
            String json = gson.toJson(devices);
            try {
                String saved = FileManager.saveText(requireContext(), fileName, json);
                // for DEBUG 保存ファイル名出力
                DEBUG_OUT.accept(TAG, "saved: " + saved);
                // 複数からのコールバックで fileName がキーになる
                DEBUG_OUT.accept(TAG, "mSavedCallbacks.size: " + mSavedCallbacks.size());
                for (ImplicitlyRequestCallback callback : mSavedCallbacks) {
                    callback.onComplete(fileName);
                }
            } catch (IOException e) {
                // これは無い想定
                String errMsg = String.format(getString(R.string.warning_save_with_2reason),
                        getString(R.string.sensor_devices),
                        e.getLocalizedMessage());
                for (ImplicitlyRequestCallback callback : mSavedCallbacks) {
                    callback.onFailure(fileName, errMsg);
                }
            }
        });
    }

    public boolean isExistsSensorDevices() {
        return FileManager.isFileExist(requireContext(), getString(R.string.devices_json_file));
    }

    /**
     * トップ画面で選択されたセンサーデバイス名をプリファレンスに保存する
     * @param selectedDeviceName 選択されたセンサーデバイス名
     */
    public void saveSelectedDeviceNameInPref(String selectedDeviceName) {
        SharedPrefUtil.saveSelectedDeviceName(requireContext(), selectedDeviceName);
    }

    /**
     * トップ画面で選択されたセンサーデバイス名を府レファレンスから取得する
     * @return トップ画面で選択されたセンサーデバイス名
     */
    public String getSelectedDeviceNameInPref() {
        return SharedPrefUtil.getSelectedDeviceName(requireContext());
    }

    /**
     * 暗黙的にセンサーデバイスリスト取得をリクエストする
     * <p>非同期で実行されるためタイミングによってはサブクラスが取得できない場合がある</p>
     */
    private void requestSensorDevicesImplicitly() {
        DEBUG_OUT.accept(TAG, "requestSensorDevicesImplicitly");
        // 暗黙的にサーバーにリクエストする
        RequestDevice device =  NetworkUtil.getActiveNetworkDevice(requireContext());
        if (device == RequestDevice.NONE) {
            // ウォーニングビューにメッセージを出力する
            showNetworkUnavailableInWarningView();
            return;
        }

        // サーバーと更新中メッセージを設定する
        setRequestMessage(getString(R.string.msg_implicitly_requesting));
        WeatherApplication app = (WeatherApplication) requireActivity().getApplication();
        String requestUrl = app.getRequestUrls().get(device.toString());
        assert requestUrl != null;
        Map<String, String> headers = app.getRequestHeaders();
        WeatherDevicesRepository repos = new WeatherDevicesRepository();
        // リクエストパラメータなし
        repos.makeGetRequest(0, requestUrl, null, headers,
            app.mEexecutor, app.mdHandler, (result) -> {
                if (result instanceof Result.Success) {
                    ResponseDevicesResult dataResult =
                            ((Result.Success<ResponseDevicesResult>) result).get();
                    ResponseDevicesData data = dataResult.getData();
                    DEBUG_OUT.accept(TAG, data.toString());
                    // ファイル保存
                    saveSensorDevices(data.getDevices());
                } else if (result instanceof Result.Warning) {
                    ResponseStatus status =
                            ((Result.Warning<?>) result).getResponseStatus();
                    DEBUG_OUT.accept(TAG, "WarningStatus: " + status);
                    showWarningInWarningView(getWaringView(),
                            getWarningFromBadRequestStatus(status));
                } else if (result instanceof Result.Error) {
                    Exception exception = ((Result.Error<?>) result).getException();
                    Log.w(TAG, "GET error:" + exception.toString());
                }
            });
    }

    @Override
    public void onResume() {
        DEBUG_OUT.accept(TAG, "onResume()");

        // サブクラスのフラグメントタイトルをアクションバータイトルに設定
        setActionBarTitle(getFragmentTitle());

        // センサーディバイスリストチェック
        if (!isExistsSensorDevices()) {
            // サーバーからセンサーディバイスリスト取得
            requestSensorDevicesImplicitly();
        }

        // 画像表示系フラグメントのみ初期イメージを取得する
        if (getFragmentPosition() > 0) {
            // 前回表示されたウォーニングビューを隠す
            hideWarningView();
            mMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
            DEBUG_OUT.accept(TAG, "" + mMetrics);

            // 初期画面ビットマップイメージ
            if (mNoImageBitmap == null) {
                AssetManager am = requireContext().getAssets();
                try {
                    mNoImageBitmap = BitmapFactory.decodeStream(am.open(NO_IMAGE_FILE));
                    DEBUG_OUT.accept(TAG, "mNoImageBitmap: " + mNoImageBitmap);
                    ImageView iv = getImageView();
                    if (iv != null) {
                        iv.setImageBitmap(mNoImageBitmap);
                    }
                }catch (IOException iex) {
                    // 通常ここには来ない
                    Log.w(TAG, iex.getLocalizedMessage());
                }
            }
        }
        // for DEBUG
        if (BuildConfig.DEBUG) {
            // 保存されているファイル
            String fileNames = FileManager.checkFileNamesInContextDir(requireContext());
            DEBUG_OUT.accept(TAG, "Context.FilesDir in [ " + fileNames + "]");
            // プリファレンスデータ
            SharedPreferences sharedPref = SharedPrefUtil.getSharedPrefInMainActivity(
                    requireContext()
            );
            Map<String, ?> prefAll = sharedPref.getAll();
            DEBUG_OUT.accept(TAG, "prefAll: " + prefAll);
        }

        // サブラクスのonResume()
        super.onResume();
    }

    @Override
    public void onPause() {
        DEBUG_OUT.accept(TAG, "onResume()");

        // 暗黙リクエスト結果通知コールバックをクリア
        if (!mSavedCallbacks.isEmpty()) {
            mSavedCallbacks.clear();
        }

        // サブラクスのonPause()
        super.onPause();
    }

    /**
     * レスポンス時のウォーニングメッセージ用マップのロード
     */
    private void initResponseWarningMap() {
        String[] warnings = getResources().getStringArray(R.array.warning_map);
        for (String item : warnings) {
            String[] items = item.split(",");
            Integer respCode = Integer.valueOf(items[0]);
            mResponseWarningMap.put(respCode, items[1]);
        }
    }

    /**
     * SettingsActivityのスイッチウィジットのON/OFFをモニタするコールバックのマップを取得する
     * @return コールバックのマップ
     */
    public Map<String, SettingsChangeCallback> getSettingsChangeCallbacks() {
        //
        MainActivity activity = (MainActivity) getActivity();
        assert activity != null;
        return activity.getSettingsChangeCallbacks();
    }

    /**
     * ファイル保存時、プリファレンスcommit()呼び出し時のHandler取得
     * @return Handler
     */
    public Handler getHandler() {
        return mHandler;
    }

    /**
     * グラフ表示系FragmentでデバイスのDisplayMetricsを取得する
     * @return デバイスのDisplayMetrics
     */
    public DisplayMetrics getDisplayMetrics() {
        assert mMetrics != null;
        return mMetrics;
    }

    /**
     * 初期画面ビットマップを取得する
     * @return 初期画面ビットマップ
     */
    public Bitmap getNoImageBitmap() {
        return mNoImageBitmap;
    }

    /**
     * ウォニング時のレスポンスステータスとメッセージ変換用マップからステータス用の文字列を取得する
     * @param responseStatus ウォニング時のレスポンスステータス
     * @return ステータス用の文字列
     */
    public String getWarningFromBadRequestStatus(ResponseStatus responseStatus) {
        String[] items = responseStatus.getMessage().split(",");
        String message;
        if (items.length > 1) {
            // コード付きメッセージ
            try {
                int warningCode = Integer.parseInt(items[0]);
                message = mResponseWarningMap.get(warningCode);
                if (message == null) {
                    // マップに未定義なら2つ目の項目 ※Androidアプリ側のBUGの可能性
                    message = items[1];
                }
            } catch (NumberFormatException e) {
                // 先頭が数値以外ならFlaskアプリ側BUGの可能性
                message = items[0];
            }
        } else if (items.length == 1) {
            // メッセージのみ ※Flask (404 URL Not found | 500 InternalError)
            message = items[0];
        } else {
            // 想定しないエラーの場合
            message = responseStatus.getMessage();
        }

        return message;
    }

    /**
     * ウォーニングメッセージを取得
     * @param status レスポンスステータス
     * @return ウォーニングメッセージ
     */
    public String getResponseWarning(ResponseStatus status) {
        String message;
        if (status.getCode() >= 400 && status.getCode() <= 403) {
            message = getWarningFromBadRequestStatus(status);
        } else if(status.getCode() == 500) {
            message = status.getMessage();
        } else {
            message = "不明エラー";
        }
        return message;
    }

    //** START ActionBar methods **************************************
    /**
     * Get ActionBar
     * @return androidx.appcompat.app.ActionBar
     */
    private ActionBar getActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        assert activity != null;
        return activity.getSupportActionBar();
    }

    /**
     * ActionBarタイトルにフラグメントタイトルを設定
     * @param title フラグメントタイトル
     */
    private void setActionBarTitle(String title) {
        ActionBar bar = getActionBar();
        // NoActionBar is null
        assert bar != null;
        bar.setTitle(title);
    }

    /**
     * ActionBarサプタイトルにメッセージを設定
     * @param message メッセージ
     */
    private void setActionBarSubTitle(String message) {
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setSubtitle(message);
    }

    /**
     * リクエスト開始メッセージを設定する
     * <p>ActionBarサブタイトルに開始メッセージを表示</p>
     * @param message リクエスト開始メッセージ
     */
    public void setRequestMessage(String message) {
        setActionBarSubTitle(message);
    }

    /**
     * リクエスト完了時にネットワーク種別を表示
     * <p>ActionBarサブタイトルにネットワーク種別を表示</p>
     * @param device RequestDevice
     */
    public void showRequestComplete(RequestDevice device) {
        String networkType;
        if (device == RequestDevice.MOBILE) {
            TelephonyManager manager =
                    (TelephonyManager) requireActivity().getSystemService(
                            Context.TELEPHONY_SERVICE);
            String operatorName = manager.getNetworkOperatorName();

            networkType = device.getMessage() + " (" + operatorName +")";
        } else {
            networkType = device.getMessage();
        }
        // AppBarサブタイトル更新
        setActionBarSubTitle(networkType);
    }
    //** END ActionBar methods **************************************

    //** START Show DialogFragment methods **************************
    /**
     * メッセージダイアログ表示 ※OKボタンのみ
     * @param title タイトル(任意)
     * @param message メッセージ
     * @param tagName FragmentTag
     */
    public void showMessageOkDialog(String title, String message, String tagName) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        DialogFragment fragment = CustomDialogs.MessageOkDialogFragment.newInstance(title, message);
        assert activity != null;
        fragment.show(activity.getSupportFragmentManager(), tagName);
    }

    /**
     * ネットワーク利用不可ダイアログ表示
     */
    public void showDialogNetworkUnavailable() {
        // タイトルなし
        showMessageOkDialog(null, getString(R.string.warning_network_not_available),
                "MessageOkDialogFragment");
    }

    /**
     * 例外メッセージ表示ダイアログ
     * @param exp Exception
     */
    public void showDialogExceptionMessage(Exception exp) {
        String errorMessage = String.format(
                getString(R.string.exception_with_reason),
                exp.getLocalizedMessage());
        showMessageOkDialog(getString(R.string.error_response_dialog_title), errorMessage,
                "ExceptionDialogFragment");
    }

    /**
     * ウォーニング時ResponseStatusのメッセージをウォーニング用ビューに表示する
     * @param status ウォーニング時ResponseStatus
     */
    public void showWarningWithResponseStatus(ResponseStatus status) {
        String message = getResponseWarning(status);
        getWaringView().setText(message);
        if (getWaringView().getVisibility() != View.VISIBLE) {
            getWaringView().setVisibility(View.VISIBLE);
        }
    }

    /**
     * ウォーニング用ビューを隠す
     */
    public void hideWarningView() {
        if (getWaringView().getVisibility() == View.VISIBLE) {
            getWaringView().setText("");
            getWaringView().setVisibility(View.GONE);
        }
    }

    /**
     * ウォーニングをウォーニング用ステータスビューに表示する ※暗黙的リクエスト用
     * @param warning ウォーニング用ステータスビュー
     */
    public void showWarningInWarningView(TextView statusView, String warning) {
        statusView.setText(warning);
        if (statusView.getVisibility() != View.VISIBLE) {
            statusView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ネットワーク利用不可メッセージをウォーニング用ステータスビューに表示する
     * <p>暗黙的なネットワークリクエスト時に利用</p>
     */
    public void showNetworkUnavailableInWarningView() {
        String warning = getString(R.string.warning_network_not_available);
        showWarningInWarningView(getWaringView(), warning);
    }
    //** END Show DialogFragment methods ****************************

    //** START プリファレンス処理
    /**
     * このフラグメントが所属するアクティビィティのプリファレンスオブジェクトを取得する<br/>
     * (SettingsActivity: pref_screen_main_activity)
     * @return アクティビィティのプリファレンスオブジェクト
     */
    public SharedPreferences getSharedPreferences () {
        return SharedPrefUtil.getSharedPrefInMainActivity(requireContext());
    }

    /**
     * データ保存設定(SettingsActivity: pref_screen_main_activity)の最新データ保存可否を取得する
     * @return 最新データ保存ならtrue
     */
    public boolean isSaveLatestData() {
        return SharedPrefUtil.isSaveLatestDataInSettings(requireContext());
    }

    /**
     * データ保存設定(SettingsActivity: pref_screen_main_activity)の画像保存可否を取得する
     * @return 画像保存ならtrue
     */
    public boolean isSaveGraphImage() {
        return SharedPrefUtil.isSaveGraphImageInSettings(requireContext());
    }

    /**
     * プリファレンスの保存済みファイルパスへのキーから保存済みファイルパスを取得する
     * @param sharedPref SharedPreferences
     * @param key 保存済みファイルパスへのキー
     * @return 保存済みファイルパスが存在すればそのファイルバス, 未存在ならnull
     */
    public String hasPathInSharedPreference(SharedPreferences sharedPref, String key) {
        return sharedPref.getString(key, null);
    }

    /**
     * ファイルが存在すれば削除をする
     * @param filePath ファイルパス (Not null)
     */
    public void deleteFileIfExist(@NonNull String filePath) {
        DEBUG_OUT.accept(TAG, "deleteFileIfExist.file: " + filePath);
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            DEBUG_OUT.accept(TAG, "deleteFileIfExist.deleted: " + deleted);
        }
    }

    /**
     * 指定したプリファレンスのクリーンアップ済みキーからクリーンアップ済みかチェックする
     * @param sharedPref SharedPreferences
     * @param key クリーンアップ済みキー
     * @return クリーンアップ済みならtrue
     */
    public boolean isAlreadyCleanupInPreference(SharedPreferences sharedPref, String key) {
        return sharedPref.getBoolean(key, false/*プリファレンスが未設定ならfalse*/);
    }
    //** END プリファレンス処理

    //** サブクラスで実装しなければならないメソッド ******
    /**
     * サブラクスのViewPager2用フラグメントインデックスを取得する
     * <p>全てのサブクラスで必須</p>
     * @return フラグメントインデックス
     */
    public abstract int getFragmentPosition();

    /**
     * サブクラスのフラグメントタイトルを取得する
     * <p>全てのサブクラスで必須</p>
     * @return フラグメントタイトル
     */
    public abstract String getFragmentTitle();

    /**
     * 画像取得系サブクラスのImageViewの参照を取得する
     * <ul>
     *     <li>画像表示系フラグメントは必須</li>
     *     <li>登録系フラグメントはnull</li>
     * </ul>
     * @return 画像表示用のImageView
     */
    public abstract ImageView getImageView();

    /**
     * サブクラスのウォーニング出力用ビューを取得する
     * @return ウォーニング出力用ビュー
     */
    public abstract TextView getWaringView();

}
