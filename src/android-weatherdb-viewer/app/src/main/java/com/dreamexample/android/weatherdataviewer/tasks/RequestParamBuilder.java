package com.dreamexample.android.weatherdataviewer.tasks;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * リクエストパラメータビルダークラス
 */
public class RequestParamBuilder {
    private static final String FMT_PARAM = "%s=%s";
    private final List<String> params;

    /**
     * コンストラクタ
     * @param deviceName センサーディバイス名 ※必須
     */
    public RequestParamBuilder(String deviceName) {
        params = new ArrayList<>();
        String encoded = urlEncoded(deviceName);
        String reqParam = String.format(FMT_PARAM, "device_name", encoded);
        this.params.add(reqParam);
    }

    /**
     * 検索開始日(過去、または本日) ※任意
     * @param startDay 検索開始日
     * @return RequestParamBuilder
     */
    public RequestParamBuilder addStartDay(String startDay) {
        String encoded = urlEncoded(startDay);
        String reqParam = String.format(FMT_PARAM, "start_day", encoded);
        this.params.add(reqParam);
        return this;
    }

    /**
     * 検索開始日(過去、または本日)から何日前のリクエストパラメータ設定
     * @param beforeDay 本日から何日前
     * @return RequestParamBuilder
     */
    public RequestParamBuilder addBeforeDay(String beforeDay) {
        String encoded = urlEncoded(beforeDay);
        String reqParam = String.format(FMT_PARAM, "before_days", encoded);
        this.params.add(reqParam);
        return this;
    }

    /**
     * 検索年月リクエストパラメータ設定
     * @param yearMonth 検索年月 (形式) 'YYYY-mm'
     * @return RequestParamBuilder
     */
    public RequestParamBuilder addYearMonth(String yearMonth) {
        String encoded = urlEncoded(yearMonth);
        String reqParam = String.format(FMT_PARAM, "year_month", encoded);
        this.params.add(reqParam);
        return this;
    }

    /**
     * リクエストパラメータを生成する
     * @return 先頭に"?"がついたリクエストパラメータ
     */
    public String build() {
        String joinedParam = String.join("&", this.params);
        return "?" + joinedParam;
    }

    /**
     * 引数の文字列をURLエンコードする
     * @param rawParam 通常の文字列
     * @return URLエンコード済み文字列
     */
    private static String urlEncoded(String rawParam) {
        try {
            return URLEncoder.encode(rawParam, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return rawParam;
        }
    }

}
