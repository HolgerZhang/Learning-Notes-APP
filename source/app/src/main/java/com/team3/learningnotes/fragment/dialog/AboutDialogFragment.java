package com.team3.learningnotes.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.team3.learningnotes.R;
import com.team3.learningnotes.util.SignatureUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AboutDialogFragment extends DialogFragment {

    TextView textView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_dialogs, null);

        builder.setView(view).setTitle(R.string.dialog_about_title).setPositiveButton(R.string.action_close, null);
        SignatureUtils.ReleaseType releaseType = SignatureUtils.getReleaseType(getActivity());

        textView = view.findViewById(R.id.dialogMessage);
        textView.setText(R.string.dialog_about_message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        return builder.create();
    }
}
