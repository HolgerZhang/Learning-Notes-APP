package com.team3.learningnotes.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.team3.learningnotes.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DeleteDialogFragment extends DialogFragment {

    // 创建此片段实例的活动必须实现此接口才能接收事件回调
    public interface Listener {
        void onDeleteDialogPositiveClick();
    }

    Listener listener;  // 使用此接口实例传递操作事件

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // 验证主机活动是否实现了回调接口
        try {
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 使用Builder类进行便捷的对话框构建
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_are_you_sure).setTitle(getArguments().getInt("dialog_title"))
                .setPositiveButton(R.string.action_delete, (dialog, id) -> listener.onDeleteDialogPositiveClick())
                .setNegativeButton(R.string.action_cancel, null);

        return builder.create();
    }
}
