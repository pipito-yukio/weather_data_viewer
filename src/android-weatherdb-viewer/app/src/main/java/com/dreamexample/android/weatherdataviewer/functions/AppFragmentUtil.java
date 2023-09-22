package com.dreamexample.android.weatherdataviewer.functions;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.Calendar;

public class AppFragmentUtil {

    /** スピナーから選択された値と選択位置を保持するクラス */
    public static class SpinnerSelected {
        public static final int UNSELECTED = -1;
        private int position;
        private String value;

        public SpinnerSelected() {}
        public int getPosition() { return position; }
        public String getValue() { return value; }
        public void setPosition(int position) { this.position = position; }
        public void setValue(String value) { this.value = value; }

        @NonNull
        @Override
        public String toString() {
            return "SpinnerSelected{position=" + position + ", value='" + value + "'}";
        }
    }

    /**
     * 日付文字列をハイフンで分割して年月日の整数配列を取得する
     * @param dateText 日付文字列 (ISO-8601拡張ローカル形式)
     * @return 年月日の整数配列[year, month, dayOfMonth]
     */
    public static int[] splitDateValue(String dateText) {
        String[] dates = dateText.split("-");
        int year = Integer.parseInt(dates[0]);
        int month = Integer.parseInt(dates[1]);
        int day = Integer.parseInt(dates[2]);
        return new int[] {year, month, day};
    }

    /**
     * JSON保存された測定日付文字列でカレンダーオブジェクトを復元する
     * @param cal カレンダーオブジェクト
     * @param iso8601text 測定日付文字列(JSON保存)
     */
    public static void restoreCalendarObject(Calendar cal, String iso8601text) {
        int[] dates = splitDateValue(iso8601text);
        cal.set(dates[0], dates[1] - 1 , dates[2]);
    }

    /**
     * 引数のカレンダーオブジェクトからLocalDateブジェクトを生成する
     * @param cal カレンダーオブジェクト
     * @return LocalDate
     */
    public static LocalDate localDateOfCalendar(Calendar cal) {
        // LocalDate.of(year, month:1 - 12, dayOfMonth)
        return LocalDate.of(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 年月日からLocalDateオブジェクトを生成する
     * @param year 年
     * @param month 月 (1-12)
     * @param day 日 (1-31)
     * @return LocalDate
     */
    public static LocalDate localDateOf(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

}
