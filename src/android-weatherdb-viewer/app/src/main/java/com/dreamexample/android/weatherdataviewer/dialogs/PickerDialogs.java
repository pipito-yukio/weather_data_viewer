package com.dreamexample.android.weatherdataviewer.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

/**
 * 日付選択フラグメントダイアログクラスの定義
 * 
 */
public class PickerDialogs {

    public static class DatePickerFragment extends DialogFragment {
        private final Context mContext;
        private final DatePickerDialog.OnDateSetListener mListener;
        private Calendar mCalendar;

        public DatePickerFragment(@NonNull Context context,
                                  Calendar cal,
                                  @NonNull DatePickerDialog.OnDateSetListener listener) {
            mContext = context;
            mCalendar = cal;
            mListener = listener;
        }

        public DatePickerFragment(@NonNull Context context,
                                  @NonNull DatePickerDialog.OnDateSetListener listener) {
            this(context, null, listener);
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mCalendar == null) {
                mCalendar = Calendar.getInstance();
            }
            int year = mCalendar.get(Calendar.YEAR);
            int month = mCalendar.get(Calendar.MONTH); // 0-11
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(mContext, mListener, year, month, day);
        }
    }

}
