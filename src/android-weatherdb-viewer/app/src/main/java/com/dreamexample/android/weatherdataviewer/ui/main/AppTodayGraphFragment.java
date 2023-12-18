package com.dreamexample.android.weatherdataviewer.ui.main;

import static com.dreamexample.android.weatherdataviewer.functions.MyLogging.DEBUG_OUT;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dreamexample.android.weatherdataviewer.MainActivity;
import com.dreamexample.android.weatherdataviewer.MainActivity.SettingsChangeCallback;
import com.dreamexample.android.weatherdataviewer.R;
import com.dreamexample.android.weatherdataviewer.WeatherApplication;
import com.dreamexample.android.weatherdataviewer.constants.RequestDevice;
import com.dreamexample.android.weatherdataviewer.data.ResponseImageData;
import com.dreamexample.android.weatherdataviewer.data.ResponseImageDataResult;
import com.dreamexample.android.weatherdataviewer.data.ResponseStatus;
import com.dreamexample.android.weatherdataviewer.dialogs.PickerDialogs;
import com.dreamexample.android.weatherdataviewer.functions.AppFragmentUtil;
import com.dreamexample.android.weatherdataviewer.functions.ImageUtil;
import com.dreamexample.android.weatherdataviewer.tasks.NetworkUtil;
import com.dreamexample.android.weatherdataviewer.tasks.RequestParamBuilder;
import com.dreamexample.android.weatherdataviewer.tasks.Result;
import com.dreamexample.android.weatherdataviewer.tasks.WeatherGraphRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 本日データ画像取得・表示フラグメント
 */
public class AppTodayGraphFragment extends AppBaseFragment {
    private static final String TAG = AppTodayGraphFragment.class.getSimpleName();

    // 画像表示ビュー
    private ImageView mImageView;
    // ウォーニング表示用ビュー ※非表示
    private TextView mWarningView;
    // 下部コンテナーウィジット
    private Button mBtnUpdate;
    // カレンダー選択ビュー ※リクエストパラメータは"startDay"
    private TextView mInpStartDay;
    // 本日/終了日指定(カレンダー選択)
    private RadioGroup mRadioDayGroup;
    private RadioButton mRadioToday;
    private RadioButton mRadioChangeToday;
    // 検索日からN日前のスピナー
    private Spinner mSpinnerBeforeDay;
    // 測定日付の比較用基準オブジェクト(本日)
    private final LocalDate mTodayLocalDate = LocalDate.now();
    // DatePickerDialogに連動するカレンダーオブジェクト
    private final Calendar mMeasurementDayCal = Calendar.getInstance();
    // 画像の保存ファイル名配列
    private String[] mSaveImageNames;
    // 画像のプリファレンス保存ファイルへのキー配列
    private String[] mPrefSaveImagePathKeys;
    // ストップ時保存済みプリファレンスキー
    private String mPrefKeyStopSaved;
    // pref_screen_main_activityで定義されている画像データ保存スイッチへのキー
    private String mSaveDataInPrefScreenKey;

    /**
     * コンストラクタ
     *
     * @param fragPosIdx フラグメント位置インデックス
     * @return このフラグメント
     */
    public static AppTodayGraphFragment newInstance(int fragPosIdx) {
        AppTodayGraphFragment frag = new AppTodayGraphFragment();
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_POS_KEY, fragPosIdx);
        frag.setArguments(args);
        return frag;
    }

    //** START implements abstract methods **************************
    @Override
    public int getFragmentPosition() {
        assert getArguments() != null;
        return getArguments().getInt(FRAGMENT_POS_KEY, 1);
    }

    @Override
    public String getFragmentTitle() {
        return getString(R.string.app_title_frag_graph_before_days);
    }

    public ImageView getImageView() {
        assert mImageView != null;
        return mImageView;
    }

    @Override
    public TextView getWaringView() {
        assert mWarningView != null;
        return mWarningView;
    }

    //** START: 日付関連ビュー, Date Picker選択 *******************************************************
    /**
     * TextViwのTagから値を取得する
     * @return TextViewに格納しているTag値
     */
    private String getStartDayInTextView() {
        int tagId = mInpStartDay.getId();
        return (String) mInpStartDay.getTag(tagId);
    }

    /**
     * 日付入力ウィジットの表示とキー付きTAG値(自分のIDがキー)を更新する
     * カレンダーオブジェクトも更新する
     *
     * @param tv       日付入力ウィジット
     * @param tagValue TAG値
     */
    private void updateDateView(TextView tv, String tagValue) {
        // 自分のIDをキーとしてISO拡張フォーマットの日付を保持する
        tv.setTag(tv.getId(), tagValue);
        // 日本語表示用日付を表示する
        int[] dates = AppFragmentUtil.splitDateValue(tagValue);
        String showDate = String.format(getString(R.string.format_show_date),
                dates[0], dates[1], dates[2]);
        tv.setText(showDate);
    }

    /**
     * 本日日付を測定日付TextViewに設定する
     *
     * @param v 測定日付ウィジット
     */
    private void initInpTextViewToTodayValue(TextView v) {
        // TAG値用の日付生成
        updateDateView(v, mTodayLocalDate.toString());
    }

    /**
     * 日付ピッカーダイアログを表示する
     *
     * @param v 起動したウィジット
     */
    private void showDatePicker(View v) {
        DialogFragment newFragment = new PickerDialogs.DatePickerFragment(
                requireActivity(), mMeasurementDayCal, (view, year, month, dayOfMonth) -> {
            // ピッカー選択月はカレンダーと同じく: 0-11(月)
            DEBUG_OUT.accept(TAG, "showDatePicker.year: " + year +
                    ",month(0-11): " + month + ",dayOfMonth: " +  dayOfMonth);
            // 未来日は選択不可
            int localDateMonth = month + 1;
            LocalDate selectedLocal = AppFragmentUtil.localDateOf(year, localDateMonth, dayOfMonth);
            DEBUG_OUT.accept(TAG, "selected　LocalDate: " + localDateMonth);
            // selectedLocalが、指定された日付より後(未来)にあるかどうか
            if (selectedLocal.isAfter(mTodayLocalDate)) {
                showMessageOkDialog(getString(R.string.warning_title_selected_picker_day),
                        getString(R.string.warning_not_select_future_day),
                        "DatePickerNgDialogFragment");
                return;
            }

            // 測定日付ウィジットを更新するためのタグ値を生成
            String tagValue = String.format(getString(R.string.format_tag_date),
                    year, localDateMonth, dayOfMonth);
            updateDateView((TextView) v, tagValue);
            // カレンダーオブジェクトを更新
            mMeasurementDayCal.set(year, month, dayOfMonth);
            DEBUG_OUT.accept(TAG, "selected: " + selectedLocal + " ,now: " + mTodayLocalDate);
        });
        newFragment.show(requireActivity().getSupportFragmentManager(), "DatePickerFragment");
    }
    //** END *******************************************************

    // 更新ボタンクリックリスナー
    private final  View.OnClickListener mButtonClickListener = (view) -> performRequest();

    // 日付ピッカーダイアログ起動イベントリスナー
    private final View.OnClickListener mDatePickerViewClickListener = this::showDatePicker;

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

    // ラジオボタングループイベントリスナー
    private final RadioGroup.OnCheckedChangeListener mRadioGroupListener = (group, checkedId) -> {
        boolean checkedRange = (checkedId != mRadioToday.getId());
        mSpinnerBeforeDay.setEnabled(checkedRange);
        mInpStartDay.setEnabled(checkedRange);
        // 選択されたラジオボタンに対応する画像の復元
        restoreSavedImage();
    };

    private void initSpinnerView(View mainView) {
        // 前日指定のスピナー
        mSpinnerBeforeDay = mainView.findViewById(R.id.spinnerBeforeDay);
        mSpinnerBeforeDay.setSelection(0);
        mSpinnerBeforeDay.setEnabled(false);
        // https://developer.android.com/guide/topics/ui/controls/spinner?hl=ja
        // Customize spinnerItem and spinnerDropdownItem
        //  textSizeを端末サイズに応じて可変にする(values/dimens.xml, values-w600dp/dimens.xml)
        //  textColorをテーマに応じて可変にする(values/themes.xml, values-night/themes.xml)
        // android.R.layout.simple_spinner_item => R.layout.before_days_spinner_item)
        // android.R.layout.simple_spinner_dropdown_item => R.layout.before_days_spinner_dropdown_item
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.beforeDays, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mSpinnerBeforeDay.setAdapter(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画像保存設定キー
        mSaveDataInPrefScreenKey = getString(R.string.pref_screen_key_save_today_image);
        // 画像保存設定の変更を通知してくれるコールバック
        Map<String, MainActivity.SettingsChangeCallback> callbackMap = getSettingsChangeCallbacks();
        callbackMap.put(mSaveDataInPrefScreenKey, mSettingsChangeCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_today_graph, container, false);
        mImageView = mainView.findViewById(R.id.currentTimeDataGraph);
        mWarningView = mainView.findViewById(R.id.tvTgWarningStatus);
        // ラジオグループ
        mRadioToday = mainView.findViewById(R.id.radioToday);
        mRadioChangeToday = mainView.findViewById(R.id.radioRange);
        // デフォルト: 本日
        mRadioToday.setChecked(true);
        mRadioDayGroup = mainView.findViewById(R.id.radioDayGroup);
        // カレンダーを起動するためのビュー
        mInpStartDay = mainView.findViewById(R.id.inpTvStartDay);
        mInpStartDay.setOnClickListener(mDatePickerViewClickListener);
        mInpStartDay.setEnabled(false);
        // 更新ボタン
        mBtnUpdate = mainView.findViewById(R.id.btnTodayGraphUpdate);
        mBtnUpdate.setOnClickListener(mButtonClickListener);
        // スピナー初期設定
        initSpinnerView(mainView);
        // 本日設定
        initInpTextViewToTodayValue(mInpStartDay);

        // 画像の保存ファイル名配列の生成
        mSaveImageNames = new String[] {
                getString(R.string.file_name_today_image)/*本日データ画像*/,
                getString(R.string.file_mame_before_image)/*指定終了日データ画像*/,
        };
        // プリファレンス画像保存キー名配列の生成
        mPrefSaveImagePathKeys = new String[] {
                getString(R.string.pref_frag_today_today_image_path),
                getString(R.string.pref_frag_today_before_image_path),
        };
        mPrefKeyStopSaved = getString(R.string.pref_frag_today_stop_saved);

        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DEBUG_OUT.accept(TAG, "onResume()");

        // プリファレンスからラジオボタンの状態を復元する
        if (restoreControlWidgetsState()) {
            // 画像ファイル復元
            restoreSavedImage();
        } else {
            // imgTagがnullなら初回起動時
            Object imgTag = mImageView.getTag();
            DEBUG_OUT.accept(TAG, "onResume().imgTag: " + imgTag);
            if (imgTag == null) {
                // imageView未設定
                setNoImageBitmapInImageView();
            }
        }
        // ラジオグループリスナー登録
        mRadioDayGroup.setOnCheckedChangeListener(mRadioGroupListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        DEBUG_OUT.accept(TAG, "onStart()");
    }

    public void onPause() {
        // リクエスト結果通知コールバックのクリアはベースクラスで実行
        super.onPause();
        DEBUG_OUT.accept(TAG, "onPause()");

        // ラジオグループリスナー解除 ※null可
        mRadioDayGroup.setOnCheckedChangeListener(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        DEBUG_OUT.accept(TAG, "onStop()");

        // 下部リクエスト制御コンテナ内のウィジットの状態をプリファレンスに保存する
        saveControlWidgetsStateOnStop();
    }
    //** END life cycle events *****************************

    private String makeRequestParam(int selectedRadioIndex) {
        // トップ画面で選択されたデバイス名取得 ※必須
        String deviceName = getSelectedDeviceNameInPref();
        RequestParamBuilder builder = new RequestParamBuilder(deviceName);
        // 終了日指定ラジオボタンが選択されていたら追加のリクエストパラメータを設定する
        if (selectedRadioIndex == 1) {
            // 検索開始日は任意だが入力ピッカー用ビューから取得した値を送る
            String startDay = getStartDayInTextView();
            DEBUG_OUT.accept(TAG, "startDay: " + startDay);
            builder.addStartDay(startDay);
            // 検索開始日から N(1,2,3,7)日が選択された時のリクエストパラメータ
            DEBUG_OUT.accept(TAG, "Spinner selectItem: " + mSpinnerBeforeDay.getSelectedItem());
            // ※スピナー生成時にposition=0を指定しているので未選択状態はない
            builder.addBeforeDay(mSpinnerBeforeDay.getSelectedItem().toString());
        } // 本日データならリクエストパラメータ無し

        return builder.build();
    }

    private void performRequest() {
        RequestDevice device = NetworkUtil.getActiveNetworkDevice(requireContext());
        if (device == RequestDevice.NONE) {
            showDialogNetworkUnavailable();
            return;
        }

        mBtnUpdate.setEnabled(false);
        // リクエスト開始メッセージを設定する
        setRequestMessage(getString(R.string.msg_getting_graph));

        WeatherApplication app = (WeatherApplication) requireActivity().getApplication();
        WeatherGraphRepository repos = new WeatherGraphRepository();
        String requestUrl = app.getRequestUrls().get(device.toString());
        assert requestUrl != null;
        // ImageViewサイズとDisplayMetrics.densityをリクエストヘッダに追加する
        Map<String, String> headers = app.getRequestHeaders();
        ImageUtil.appendImageSizeToHeaders(headers,
                mImageView.getWidth(), mImageView.getHeight(), getDisplayMetrics().density);
        // ラジオボタン選択(本日データ:0, 指定終了日データ: 1)
        int selectedRadioIndex = getSelectedRadioIndex();
        String requestParam = makeRequestParam(selectedRadioIndex) ;

        repos.makeGetRequest(selectedRadioIndex, requestUrl, requestParam,
            headers, app.mEexecutor, app.mdHandler, (result) -> {
                // ボタン状態を戻す
                mBtnUpdate.setEnabled(true);
                // リクエスト完了時にネットワーク種別を表示
                showRequestComplete(device);

                if (result instanceof Result.Success) {
                    ResponseImageDataResult imageResult =
                            ((Result.Success<ResponseImageDataResult>) result).get();
                    ResponseImageData data = imageResult.getData();
                    // レスポンスI/F仕様変更 ※レコードなしの場合は画像はnull
                    if (data.getRecCount() > 0) {
                        showSuccess(data, selectedRadioIndex);
                    } else {
                        // 観測データなし(正常レスポンス)
                        String msgRecord0;
                        if (selectedRadioIndex == 1) {
                            // 指定終了日
                            msgRecord0 = getString(R.string.warning_before_image_record_none);
                        } else {
                            // 本日
                            msgRecord0 = getString(R.string.warning_today_image_record_none);
                        }
                        // 指定されたセンサーデバイス名、または終了日の観測データ無し
                        showWarningInWarningView(mWarningView, msgRecord0);
                        // NoImage
                        setNoImageBitmapInImageView();
                    }
                } else if (result instanceof Result.Warning) {
                    ResponseStatus status =
                            ((Result.Warning<?>) result).getResponseStatus();
                    showWarningWithResponseStatus(status);
                } else if (result instanceof Result.Error) {
                    // 例外メッセージをダイアログに表示
                    Exception exception = ((Result.Error<?>) result).getException();
                    showDialogExceptionMessage(exception);
                }
            });
    }

    private void showSuccess(ResponseImageData data, int urlPathIndex) {
        // ウォーニングが表示されていたら閉じる
        hideWarningView();
        byte[] decoded = data.getImageBytes();
        if (decoded != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    decoded, 0, decoded.length);
            mImageView.setImageBitmap(bitmap);
            // タグにイメージ有りをセット
            mImageView.setTag(Boolean.TRUE);
            // 画層ファイル保存が設定されている場合は画像をファイルに保存
            if (isSaveGraphImage()) {
                saveBitmapToFile(urlPathIndex, bitmap);
            }
        }
    }

    private void setNoImageBitmapInImageView() {
        mImageView.setImageBitmap(getNoImageBitmap());
        // タグにNoImageを設定
        mImageView.setTag(Boolean.FALSE);
    }

    /**
     * N日前指定の画像データが存在するかプリファレンスをチェックする
     * @return 存在すればtrue
     */
    private boolean existBeforeDayImage() {
        String beforeImagePath = getSharedPreferences().getString(
                getString(R.string.pref_frag_today_before_image_path), null);
        return beforeImagePath != null;
    }

    /**
     * N日前指定時の関連ウィジットを復元 ※N日前画像が存在する場合
     * @param sharedPref SharedPreferences
     */
    private void restoreBeforeDayWidgets(SharedPreferences sharedPref) {
        if (existBeforeDayImage()) {
            // N日前の画像ファイルが存在
            int beforeDayPos = sharedPref.getInt(
                    getString(R.string.pref_frag_today_selected_before_day), 0);
            mSpinnerBeforeDay.setSelection(beforeDayPos);
        }
    }

    /**
     * onStop時に画像取得に関連するウィジットの最終状態をプリファレンスに保存
     * <ul>
     *     <li>当日データの画像が存在しなくとも本日ラジオボタンのチェック状態は保存</li>
     *     <li>何日前指定の画像が存在する場合、N日前スピナー選択位置と終了日を保存</li>
     * </ul>
     */
    private void saveControlWidgetsStateOnStop() {
        if (!isSaveGraphImage()) {
            // 保存無し設定
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        // ラジオボタンは片方だけで十分
        editor.putBoolean(getString(R.string.pref_frag_today_radio_today_checked),
                mRadioToday.isChecked());
        // N日前指定の画像ファイルが存在すればプリファレンスに保存する
        if (existBeforeDayImage()) {
            // N日前の選択位置
            editor.putInt(getString(R.string.pref_frag_today_selected_before_day),
                    mSpinnerBeforeDay.getSelectedItemPosition());
        }
        // 保存の事実自体の保存 ※復元時にキーを削除
        editor.putString(mPrefKeyStopSaved, getString(R.string.pref_value_stop_saved));
        // アプリ終了につきコミット
        editor.commit();
    }

    /**
     * onResume時にリクエストに関連するウィジットの最終状態をプリファレンスから復元
     * <p>リスナー登録前 ※リスナー登録はonResume()の最後</p>
     */
    private boolean restoreControlWidgetsState() {
        SharedPreferences sharedPref = getSharedPreferences();
        // onStop時の保存キーの存在チェック
        String stopSaved = sharedPref.getString(mPrefKeyStopSaved,null);
        DEBUG_OUT.accept(TAG, "restoreControlWidgetsState.stopSaved: " + stopSaved);
        if (stopSaved != null) {
            boolean isToday = sharedPref.getBoolean(
                    getString(R.string.pref_frag_today_radio_today_checked),false);
            // 各ラジオボタン
            mRadioToday.setChecked(isToday);
            mRadioChangeToday.setChecked(!isToday);
            // 終了日指定なら付随するウィジットの可否も復元する
            mSpinnerBeforeDay.setEnabled(!isToday);
            mInpStartDay.setEnabled(!isToday);
            // N日前関連ウィジットを復元
            restoreBeforeDayWidgets(sharedPref);
            // 復元完了でキーを削除する
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(mPrefKeyStopSaved);
            editor.apply();
        }
        return stopSaved != null;
    }

    /**
     * 取得した画像イメージをファイルを保存するに対応する
     * @param selectedRadioIndex 選択されているラジオボタン (本日=0)
     * @param bitmap 画像イメージ
     */
    private void saveBitmapToFile(int selectedRadioIndex, Bitmap bitmap) {
        String fileName = mSaveImageNames[selectedRadioIndex];
        // バックグラウンドスレッドでファイル保存する
        getHandler().post(() -> {
            try {
                String absSavePath = ImageUtil.saveBitmapToPng(requireContext(),
                        bitmap, fileName);
                DEBUG_OUT.accept(TAG, "absSavePath: " + absSavePath);
                if (absSavePath != null) {
                    // ファイル名をプリファレンスに保存する
                    SharedPreferences sharedPref = getSharedPreferences();
                    // 画像ファイル名保存のプリファレンスキー
                    String prefKey = mPrefSaveImagePathKeys[selectedRadioIndex];
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(prefKey, absSavePath);
                    editor.apply();
                }
            } catch (IOException e) {
                String errMsg = String.format(getString(R.string.warning_save_with_2reason),
                        getString(R.string.warning_today_image_save_error),
                        e.getLocalizedMessage());
                // ウォーニングステータスに表示
                showWarningInWarningView(mWarningView, errMsg);
            }
        });
    }

    /**
     * 現在選択されているラジオボタンから保存済みの画像ファイルが存在すればImageViewに復元する
     * @param selectedRadioIndex 選択されているラジオボタン (本日=0)
     * @return 存在すればBitmap, 未保存ならnul
     */
    private Bitmap restoreBitmapFromFile(int selectedRadioIndex) {
        SharedPreferences sharedPref = getSharedPreferences();
        // 画像ファイル名保存のプリファレンスキー
        String prefKey = mPrefSaveImagePathKeys[selectedRadioIndex];
        String savedFileName = sharedPref.getString(prefKey, null);
        DEBUG_OUT.accept(TAG, "savedFileName: " + savedFileName);
        if (savedFileName != null) {
            try {
                return ImageUtil.readBitmapFromAbsolutePath(savedFileName);
            } catch (IOException e) {
                Log.w(TAG, "restoreBitmapFromFile: " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    private void restoreSavedImage() {
        int selectedRadioIndex = getSelectedRadioIndex();
        // 画像復元
        Bitmap savedBitmap = restoreBitmapFromFile(selectedRadioIndex);
        DEBUG_OUT.accept(TAG, "savedBitmap: " + savedBitmap);
        if (savedBitmap != null) {
            mImageView.setImageBitmap(savedBitmap);
        } else {
            // 保存された画像がなければNoImage画像
            mImageView.setImageBitmap(getNoImageBitmap());
        }
    }

    private int getSelectedRadioIndex() {
        int checkedRadioId = mRadioDayGroup.getCheckedRadioButtonId();
        if (mRadioToday.getId() == checkedRadioId) {
            return 0;
        }
        return 1;
    }

    /**
     * 保存済みファイルと対応するファイルパスキー、ウィジットの状態保存キーもプリファレンスから削除する
     * <p>※設定変更コールバックのみから実行される</p>
     */
    private void cleanupSavedData() {
        SharedPreferences sharedPerf = getSharedPreferences();
        // 複数の保存済み画像ファイルを一括削除
        List<String> pathKeyList = new ArrayList<>(mPrefSaveImagePathKeys.length);
        for (String pathKey : mPrefSaveImagePathKeys) {
            String filePath = hasPathInSharedPreference(sharedPerf, pathKey);
            if (filePath != null) {
                deleteFileIfExist(filePath);
                pathKeyList.add(pathKey);
            }
        }

        // 保存ファイルパスを保持しているキーを削除
        SharedPreferences.Editor editor = sharedPerf.edit();
        if (!pathKeyList.isEmpty()) {
            for (String key : pathKeyList) {
                editor.remove(key);
            }
        }
        // 下部制御コンテナ内のウィジットの状態保存キーも削除
        String[] prefKeys = new String[] {
                getString(R.string.pref_frag_today_radio_today_checked)/*本日ラジオボタン*/,
                getString(R.string.pref_frag_today_selected_before_day)/*N日前選択値*/,
                mPrefKeyStopSaved/*OnStop時保存キー*/
        };
        for (String key : prefKeys) {
            editor.remove(key);
        }
        editor.apply();
    }

}
