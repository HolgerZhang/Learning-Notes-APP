package com.team3.learningnotes.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.team3.learningnotes.R;
import com.team3.learningnotes.recordtool.AudioRecorder;
import com.team3.learningnotes.recordtool.FileUtil;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener,
        View.OnClickListener, EventListener {
    private String noteFileName;       // 文档ID，也是文档储存的文件名

    protected TextView txtResult;       //识别结果
    protected Button startBtn;          //开始识别  一直不说话会自动停止，需要再次打开
    protected Button stopBtn;           //停止识别

    private EventManager asr;           //语音识别核心库

    private Button recordBtn, playBtn, deleteThisBtn;
    private String thisSoundPath = null;
    private Spinner soundSpinner;

    private AudioRecorder audioRecorder;

    // 初始化控件
    private void initView() {
        txtResult = (TextView) findViewById(R.id.tv_txt);
        startBtn = (Button) findViewById(R.id.btn_start);
        stopBtn = (Button) findViewById(R.id.btn_stop);

        startBtn.setOnClickListener(new View.OnClickListener() {  // 开始
            @Override
            public void onClick(View v) {
                asr.send(SpeechConstant.ASR_START, null, null, 0, 0);
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {  // 停止
            @Override
            public void onClick(View v) {
                asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
            }
        });
    }

    //申请录音权限
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] PERMISSION_ALL = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    public RecordActivity() {
    }

    // 自定义输出事件类 EventListener 回调方法
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            // 识别相关的结果都在这里
            if (params == null || params.isEmpty()) {
                return;
            }
            if (params.contains("\"final_result\"")) {
                // 一句话的最终识别结果
                Pattern pat = Pattern.compile("\"best_result\":\"(.*)\",");
                Matcher match = pat.matcher(params);
                if (match.find()) {
                    txtResult.setText(match.group(1));
                    // 获取剪贴板管理器
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    // 创建普通字符型ClipData
                    ClipData mClipData = ClipData.newPlainText("Label", match.group(1));
                    // 将ClipData内容放到系统贴板里。
                    cm.setPrimaryClip(mClipData);

                    Toast.makeText(RecordActivity.this, "结果已存入系统剪切板", Toast.LENGTH_SHORT).show();

                }
            }
        }

    }

    // 申请录音权限
    public void verifyPermissions(Activity activity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                    || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                    || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                // 检查权限状态
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                } else {
                    //  用户未彻底拒绝授予权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteFileName = getIntent().getStringExtra("filename").toString();
        setTitle(noteFileName);
        verifyPermissions(this);
        setContentView(R.layout.activity_record);

        initView();

        recordBtn = findViewById(R.id.record);
        playBtn = findViewById(R.id.play);
        deleteThisBtn = findViewById(R.id.delete_sound);
        soundSpinner = findViewById(R.id.record_files);

        recordBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        deleteThisBtn.setOnClickListener(this);

        audioRecorder = AudioRecorder.getInstance();

        //初始化EventManager对象
        asr = EventManagerFactory.create(this, "asr");
        //注册自己的输出事件类
        asr.registerListener(this); //  EventListener 中 onEvent方法

        resetRecordList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 发送取消事件
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        // 退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
        asr.unregisterListener(this);
    }

    @Override
    public void onClick(View v) {
        verifyPermissions(this);

        switch (v.getId()) {
            case R.id.record:
                try {
                    if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_NO_READY) {
                        // 初始化录音
                        String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                        audioRecorder.createDefaultAudio(fileName);
                        audioRecorder.startRecord(null, this, noteFileName);
                        recordBtn.setText("停止");
                    } else {
                        // 停止录音
                        audioRecorder.stopRecord(this, noteFileName);
                        recordBtn.setText("录音");
                        resetRecordList();
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(RecordActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.play:
                if (thisSoundPath == null) {
                    Toast.makeText(RecordActivity.this, "当前未选定或无录音",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // 调用系统自带播放器
                Toast.makeText(this, "播放", Toast.LENGTH_SHORT).show();
                Intent playIntent = new Intent("android.intent.action.VIEW");
                playIntent.setAction(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(thisSoundPath));
                playIntent.setDataAndType(uri, "audio/*");
                startActivity(playIntent);
                break;

            case R.id.delete_sound:
                if (thisSoundPath == null) {
                    Toast.makeText(RecordActivity.this, "当前未选定或无录音",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                File file = new File(thisSoundPath);
                if (file.delete()) {
                    Toast.makeText(RecordActivity.this, "删除当前录音成功",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RecordActivity.this, "删除录音失败，未知错误",
                            Toast.LENGTH_SHORT).show();
                }
                resetRecordList();
                break;

        }

    }


    private void resetRecordList() {
        String[] recordsName = FileUtil.getWavFileNameList(this, noteFileName);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, recordsName);
        soundSpinner.setAdapter(adapter);
        soundSpinner.setOnItemSelectedListener(RecordActivity.this);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String content = parent.getItemAtPosition(position).toString();
        if (parent.getId() == R.id.record_files) {
            thisSoundPath = FileUtil.getWavFileDir(this, noteFileName) + content;
            Toast.makeText(RecordActivity.this, "选定录音" + content,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        thisSoundPath = null;
    }
}
