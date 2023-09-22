package com.dreamexample.android.weatherdataviewer.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.dreamexample.android.weatherdataviewer.R;

/**
 * メッセージダイアログクラス
 */
public class CustomDialogs {
    /**
     * OKボタンイベント処理の不要なメッセージ表示のみのフラグメントダイアログクラス
     */
    public static class MessageOkDialogFragment extends DialogFragment {
        public static MessageOkDialogFragment newInstance(String title, String message) {
            MessageOkDialogFragment frag = new MessageOkDialogFragment();
            Bundle args = new Bundle();
            // タイトルは任意
            if (!TextUtils.isEmpty(title)) {
                args.putString("title", title);
            }
            // メッセージは文字列指定
            args.putString("message", message);
            frag.setArguments(args);
            return frag;
        }

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            assert getArguments() != null;
            String title = getArguments().getString("title");
            String message = getArguments().getString("message");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            return builder
                    .setMessage(message)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            (dialog, whichButton) -> {
                            })
                    .create();
        }
    }

    /**
     * OK/CANCEL ボタンイベント処理が必要なフラグメントダイアログクラス
     */
    public static class ConfirmDialogFragment extends DialogFragment {
        public interface ConfirmOkCancelListener {
            void onOk();
            void onCancel();
        }

        private final ConfirmOkCancelListener mListener;
        public ConfirmDialogFragment(ConfirmOkCancelListener listener) {
            mListener = listener;
        }

        public static ConfirmDialogFragment newInstance(String title, String message,
                                                          ConfirmOkCancelListener listener) {
            ConfirmDialogFragment frag = new ConfirmDialogFragment(listener);
            Bundle args = new Bundle();
            // タイトルは任意
            if (!TextUtils.isEmpty(title)) {
                args.putString("title", title);
            }
            // メッセージは文字列指定
            args.putString("message", message);
            frag.setArguments(args);
            return frag;
        }

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            assert getArguments() != null;
            String  title = getArguments().getString("title");
            String message = getArguments().getString("message");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            return builder
                    .setMessage(message)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            (dialog, whichButton) -> mListener.onOk())
                    .setNegativeButton(R.string.alert_dialog_cancel,
                            (dialog, whichButton) -> mListener.onCancel())
                    .create();
        }
    }

}
