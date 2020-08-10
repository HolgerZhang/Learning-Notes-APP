package com.team3.learningnotes.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;

import com.team3.learningnotes.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FirstViewDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 使用Builder类进行便捷的对话框构建
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.first_view)
                .setTitle(R.string.app_name)
                .setPositiveButton(R.string.action_close, null);

        return builder.create();
    }
}