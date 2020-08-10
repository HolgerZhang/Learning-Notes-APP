package com.team3.learningnotes.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.team3.learningnotes.activity.MainActivity;
import com.team3.learningnotes.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NoteEditFragment extends Fragment {

    private EditText noteContents;
    String filename = String.valueOf(System.currentTimeMillis());
    String contentsOnLoad = "";
    int length = 0;
    long draftName;
    boolean isSavedNote = false;
    String contents;
    boolean directEdit = false;

    // 接收方用于在删除札记时关闭片段
    public class DeleteNotesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] filesToDelete = intent.getStringArrayExtra("files");

            for (Object file : filesToDelete) {
                if (filename.equals(file)) {
                    // 隐藏软键盘
                    EditText editText = getActivity().findViewById(R.id.editText1);
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    Fragment fragment;
                    if (getActivity().findViewById(R.id.layoutMain).getTag().equals("main-layout-normal"))
                        fragment = new NoteListFragment();
                    else
                        fragment = new WelcomeFragment();

                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.noteViewEdit, fragment, "NoteListFragment")
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                }
            }
        }
    }

    IntentFilter filter = new IntentFilter("com.team3.learningnotes.DELETE_NOTES");
    DeleteNotesReceiver receiver = new DeleteNotesReceiver();

    // 创建此片段实例的活动必须实现此接口才能接收事件回调
    public interface Listener {
        void showBackButtonDialog(String filename);

        void showDeleteDialog();

        void showSaveButtonDialog();

        boolean isShareIntent();

        String loadNote(String filename) throws IOException;

        String loadNoteTitle(String filename) throws IOException;

        void exportNote(String filename);

        void printNote(String contentToPrint);

        void mediaNote(String filename);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_edit, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 动画高度变化
        if (getActivity() instanceof MainActivity
                && getActivity().findViewById(R.id.layoutMain).getTag().equals("main-layout-large")
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LinearLayout noteViewEdit = getActivity().findViewById(R.id.noteViewEdit);
            LinearLayout noteList = getActivity().findViewById(R.id.noteList);

            noteList.animate().z(0f);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                noteViewEdit.animate().z(getResources().getDimensionPixelSize(R.dimen.note_view_edit_elevation_land));
            else
                noteViewEdit.animate().z(getResources().getDimensionPixelSize(R.dimen.note_view_edit_elevation));
        }
        // 设置内容视图
        noteContents = getActivity().findViewById(R.id.editText1);

        // 应用主题
        SharedPreferences pref = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        ScrollView scrollView = getActivity().findViewById(R.id.scrollView1);
        String theme = pref.getString("theme", "light-sans");
        if (theme.contains("light")) {
            noteContents.setTextColor(ContextCompat.getColor(getActivity(), R.color.text_color_primary));
            noteContents.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.window_background));
            scrollView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.window_background));
        }
        if (theme.contains("dark")) {
            noteContents.setTextColor(ContextCompat.getColor(getActivity(), R.color.text_color_primary_dark));
            noteContents.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.window_background_dark));
            scrollView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.window_background_dark));
        }
        if (theme.contains("sans"))
            noteContents.setTypeface(Typeface.SANS_SERIF);
        if (theme.contains("serif"))
            noteContents.setTypeface(Typeface.SERIF);
        if (theme.contains("monospace"))
            noteContents.setTypeface(Typeface.MONOSPACE);

        switch (pref.getString("font_size", "normal")) {
            case "smallest":
                noteContents.setTextSize(12);
                break;
            case "small":
                noteContents.setTextSize(14);
                break;
            case "normal":
                noteContents.setTextSize(16);
                break;
            case "large":
                noteContents.setTextSize(18);
                break;
            case "largest":
                noteContents.setTextSize(20);
                break;
        }

        // 获取文件名
        try {
            if (!getArguments().getString("filename").equals("new")) {
                filename = getArguments().getString("filename");
                if (!filename.equals("draft"))
                    isSavedNote = true;
            }
        } catch (NullPointerException e) {
            filename = "new";
        }

        // 从文件加载札记
        if (isSavedNote) {
            try {
                contentsOnLoad = listener.loadNote(filename);
            } catch (IOException e) {
                showToast(R.string.error_loading_note);
                finish(null);
            }

            length = contentsOnLoad.length();
            noteContents.setText(contentsOnLoad);

            if (!pref.getBoolean("direct_edit", false))
                noteContents.setSelection(length, length);
        } else if (filename.equals("draft")) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            String draftContents = sharedPref.getString("draft-contents", null);
            length = draftContents.length();
            noteContents.setText(draftContents);

            if (!pref.getBoolean("direct_edit", false))
                noteContents.setSelection(length, length);
        }

        // 显示软键盘
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(noteContents, InputMethodManager.SHOW_IMPLICIT);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (!listener.isShareIntent() && !isRemoving()) {
            // 将当前札记内容设置为字符串
            noteContents = getActivity().findViewById(R.id.editText1);
            contents = noteContents.getText().toString();

            if (!contents.equals("")) {
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong("draft-name", Long.parseLong(filename));
                editor.putBoolean("is-saved-note", isSavedNote);
                editor.putString("draft-contents", contents);
                editor.apply();
                showToast(R.string.draft_saved);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (!listener.isShareIntent()) {
            if (filename.equals("draft")) {
                draftName = sharedPref.getLong("draft-name", 0);
                isSavedNote = sharedPref.getBoolean("is-saved-note", false);
                filename = Long.toString(draftName);
                // 重新加载札记
                if (isSavedNote) {
                    try {
                        contentsOnLoad = listener.loadNote(filename);
                    } catch (IOException e) {
                        showToast(R.string.error_loading_note);
                        finish(null);
                    }
                } else
                    contentsOnLoad = "";

                showToast(R.string.draft_restored);
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove("draft-name");
            editor.remove("is-saved-note");
            editor.remove("draft-contents");
            editor.apply();
        }


        String title;   // 修改标题

        if (isSavedNote)
            try {
                title = listener.loadNoteTitle(filename);
            } catch (IOException e) {
                title = getResources().getString(R.string.edit_note);
            }
        else
            title = getResources().getString(R.string.action_new);

        getActivity().setTitle(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.ic_recents_logo)).getBitmap();

            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(title, bitmap, ContextCompat.getColor(getActivity(), R.color.primary));
            getActivity().setTaskDescription(taskDescription);
        }

        SharedPreferences pref = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        directEdit = pref.getBoolean("direct_edit", false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!listener.isShareIntent())
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!listener.isShareIntent())
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_edit, menu);
        if (listener.isShareIntent() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            menu.removeItem(R.id.action_export);
            menu.removeItem(R.id.action_print);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().findViewById(R.id.editText1).getWindowToken(), 0);

        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;

            case R.id.action_save:
                noteContents = getActivity().findViewById(R.id.editText1);
                contents = noteContents.getText().toString();

                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else if (directEdit)
                    getActivity().onBackPressed();
                else {
                    if (contentsOnLoad.equals(noteContents.getText().toString())) {
                        Bundle bundle = new Bundle();
                        bundle.putString("filename", filename);

                        Fragment fragment = new NoteViewFragment();
                        fragment.setArguments(bundle);

                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.noteViewEdit, fragment, "NoteViewFragment")
                                .commit();
                    } else {
                        SharedPreferences pref = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
                        if (pref.getBoolean("show_dialogs", true)) {
                            listener.showSaveButtonDialog();
                        } else {
                            try {
                                Intent intent = new Intent();
                                intent.putExtra(Intent.EXTRA_TEXT, noteContents.getText().toString());
                                this.getActivity().setResult(Activity.RESULT_OK, intent);
                                saveNote();

                                if (listener.isShareIntent())
                                    getActivity().finish();
                                else {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("filename", filename);

                                    Fragment fragment = new NoteViewFragment();
                                    fragment.setArguments(bundle);

                                    getFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.noteViewEdit, fragment, "NoteViewFragment")
                                            .commit();
                                }
                            } catch (IOException e) {
                                showToast(R.string.failed_to_save);
                            }
                        }
                    }
                }
                return true;

            case R.id.action_delete:
                listener.showDeleteDialog();
                return true;

            case R.id.action_share:
                contents = noteContents.getText().toString();
                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, contents);
                    intent.setType("text/plain");
                    // 验证意图可以解决活动，然后发送
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null)
                        startActivity(Intent.createChooser(intent, getResources().getText(R.string.send_to)));
                }
                return true;

            case R.id.action_export:
                contents = noteContents.getText().toString();
                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else {
                    String currentFilename = filename;
                    filename = "exported_note";

                    try {
                        saveNote();
                    } catch (IOException ignored) {
                    }

                    filename = currentFilename;

                    listener.exportNote("exported_note");
                }

                return true;

            case R.id.action_print:
                contents = noteContents.getText().toString();

                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else
                    listener.printNote(contents);

                return true;

            case R.id.action_media:
                contents = noteContents.getText().toString();

                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else
                    listener.mediaNote(filename);   // 转跳页面

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteNote(String filename) {
        // 建立路径名称以删除文件，然后执行删除操作
        File fileToDelete = new File(getActivity().getFilesDir() + File.separator + filename);
        fileToDelete.delete();
    }

    // 将札记保存到/data/data/com.team3.learningnotes/files
    private void saveNote() throws IOException {
        noteContents = getActivity().findViewById(R.id.editText1);
        contents = noteContents.getText().toString();

        if (contents.equals("") && filename.equals("draft"))
            finish(null);
        else {
            String newFilename;
            if (filename.equals("draft") || filename.equals("exported_note"))
                newFilename = filename;
            else
                newFilename = String.valueOf(System.currentTimeMillis());

            FileOutputStream output = getActivity().openFileOutput(newFilename, Context.MODE_PRIVATE);
            output.write(contents.getBytes());
            output.close();
            if (!(filename.equals("draft") || filename.equals("exported_note")))
                deleteNote(filename);

            if (filename.equals("draft"))
                showToast(R.string.draft_saved);
            else if (!filename.equals("exported_note"))
                showToast(R.string.note_saved);

            if (!(filename.equals("draft") || filename.equals("exported_note"))) {
                filename = newFilename;
                contentsOnLoad = contents;
                length = contentsOnLoad.length();
            }

            // 向MainActivity方式广播通知刷新列表
            Intent listNotesIntent = new Intent();
            listNotesIntent.setAction("com.team3.learningnotes.LIST_NOTES");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(listNotesIntent);
        }
    }

    private void showToast(int message) {
        Toast toast = Toast.makeText(getActivity(), getResources().getString(message), Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onBackDialogNegativeClick(String filename) {
        // 用户点击了对话框的取消按钮
        showToast(R.string.changes_discarded);
        finish(filename);
    }

    public void onBackDialogPositiveClick(String filename) {
        // 用户点击了对话框的确定按钮
        try {
            saveNote();
            finish(filename);
        } catch (IOException e) {
            showToast(R.string.failed_to_save);
        }
    }

    public void onDeleteDialogPositiveClick() {
        // 用户点击了对话框的确定按钮
        deleteNote(filename);
        showToast(R.string.note_deleted);

        if (getActivity().getComponentName().getClassName().equals("com.team3.learningnotes.activity.MainActivity")
                && getActivity().findViewById(R.id.layoutMain).getTag().equals("main-layout-large")) {
            Intent listNotesIntent = new Intent();
            listNotesIntent.setAction("com.team3.learningnotes.LIST_NOTES");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(listNotesIntent);
        }

        finish(null);
    }

    public void onSaveDialogNegativeClick() {
        // 用户点击了对话框的取消按钮
        if (isSavedNote) {
            showToast(R.string.changes_discarded);

            Bundle bundle = new Bundle();
            bundle.putString("filename", filename);

            Fragment fragment = new NoteViewFragment();
            fragment.setArguments(bundle);

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.noteViewEdit, fragment, "NoteViewFragment")
                    .commit();
        } else {
            showToast(R.string.changes_discarded);
            finish(null);
        }
    }

    public void onSaveDialogPositiveClick() {
        // 用户点击了对话框的确定按钮
        try {
            saveNote();

            if (listener.isShareIntent())
                finish(null);
            else {
                Bundle bundle = new Bundle();
                bundle.putString("filename", filename);

                Fragment fragment = new NoteViewFragment();
                fragment.setArguments(bundle);

                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.noteViewEdit, fragment, "NoteViewFragment")
                        .commit();
            }
        } catch (IOException e) {
            showToast(R.string.failed_to_save);
        }
    }

    public void onBackPressed(String filename) {
        if (contentsOnLoad.equals(noteContents.getText().toString())) {
            finish(filename);
        } else {
            if (noteContents.getText().toString().isEmpty())
                finish(filename);
            else {
                SharedPreferences pref = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
                if (pref.getBoolean("show_dialogs", true)) {
                    listener.showBackButtonDialog(filename);
                } else {
                    try {
                        saveNote();
                        finish(filename);
                    } catch (IOException e) {
                        showToast(R.string.failed_to_save);
                    }
                }
            }
        }
    }

    public void dispatchKeyShortcutEvent(int keyCode) {
        switch (keyCode) {

            // CTRL+S
            case KeyEvent.KEYCODE_S:
                contents = noteContents.getText().toString();

                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else {
                    try {
                        saveNote();
                        isSavedNote = true;
                        String title;
                        try {
                            title = listener.loadNoteTitle(filename);
                        } catch (IOException e) {
                            title = getResources().getString(R.string.edit_note);
                        }

                        getActivity().setTitle(title);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.ic_recents_logo)).getBitmap();

                            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(title, bitmap, ContextCompat.getColor(getActivity(), R.color.primary));
                            getActivity().setTaskDescription(taskDescription);
                        }
                    } catch (IOException e) {
                        showToast(R.string.failed_to_save);
                    }
                }
                break;

            // CTRL+D
            case KeyEvent.KEYCODE_D:
                listener.showDeleteDialog();
                break;

            // CTRL+H
            case KeyEvent.KEYCODE_H:
                contents = noteContents.getText().toString();

                // 如果EditText为空，则通知用户输入文本
                if (contents.equals(""))
                    showToast(R.string.empty_note);
                else {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, contents);
                    shareIntent.setType("text/plain");
                    if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null)
                        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
                }
                break;
        }
    }

    private void finish(String filename) {
        if (listener.isShareIntent())
            getActivity().finish();
        else if (filename == null) {
            Fragment fragment;
            if (getActivity().findViewById(R.id.layoutMain).getTag().equals("main-layout-normal"))
                fragment = new NoteListFragment();
            else
                fragment = new WelcomeFragment();

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.noteViewEdit, fragment, "NoteListFragment")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("filename", filename);

            Fragment fragment;
            String tag;

            if (directEdit) {
                fragment = new NoteEditFragment();
                tag = "NoteEditFragment";
            } else {
                fragment = new NoteViewFragment();
                tag = "NoteViewFragment";
            }

            fragment.setArguments(bundle);

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.noteViewEdit, fragment, tag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }

    public void switchNotes(String filename) {
        EditText editText = getActivity().findViewById(R.id.editText1);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        onBackPressed(filename);
    }

    public String getFilename() {
        return filename;
    }
}
