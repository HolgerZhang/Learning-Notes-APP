package com.team3.learningnotes.activity;

import com.team3.learningnotes.R;
import com.team3.learningnotes.baiduOcr.bdOcr;
import com.wildma.pictureselector.FileUtils;
import com.wildma.pictureselector.PictureBean;
import com.wildma.pictureselector.PictureSelector;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MediaActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener,
        View.OnClickListener {
    // 存储权限
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET};

    public static final String TAG = "PictureSelector";

    private ImageView mIvImage, mShowImage;
    private Spinner photosList;

    private String filename;       // 文档ID，也是文档储存的文件名
    private String photos_dir;
    private String thisPhotoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filename = getIntent().getStringExtra("filename").toString();
        setTitle(filename);
        setContentView(R.layout.activity_media);

        Button selectPhotoBtn, delPhotoBtn, recordSoundBtn, bdOcrBtn;
        selectPhotoBtn = findViewById(R.id.select_photo);
        delPhotoBtn = findViewById(R.id.delete_photo);
        recordSoundBtn = findViewById(R.id.record_sound);
        bdOcrBtn = findViewById(R.id.ocr);//*


        selectPhotoBtn.setOnClickListener(this);
        delPhotoBtn.setOnClickListener(this);
        recordSoundBtn.setOnClickListener(this);
        bdOcrBtn.setOnClickListener(this);//*

        mIvImage = (ImageView) findViewById(R.id.iv_image);
        mShowImage = (ImageView) findViewById(R.id.show_image);
        photosList = (Spinner) findViewById(R.id.photos_list);

        verifyStoragePermissions(this);

        String homePath = getExternalFilesDir(Environment.DIRECTORY_DCIM).toString();
        photos_dir = homePath + File.separator + "notes_photos" + File.separator + filename + File.separator;

        resetPhotoList();


        // 为了解决网络请求不能出现在主线程
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.select_photo:
                // 添加照片
                PictureSelector
                        .create(MediaActivity.this, PictureSelector.SELECT_REQUEST_CODE)
                        .selectPicture(true);
                break;
            case R.id.delete_photo:
                // 删除当前照片
                if (thisPhotoPath == null) {
                    Toast.makeText(MediaActivity.this, "删除文件失败，当前无照片",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(thisPhotoPath);
                if (file.delete()) {
                    Toast.makeText(MediaActivity.this, "删除当前照片成功",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MediaActivity.this, "删除文件失败，未知错误",
                            Toast.LENGTH_SHORT).show();
                }
                resetPhotoList();
                break;
            case R.id.record_sound:
                Intent intent = new Intent(MediaActivity.this, RecordActivity.class);
                intent.putExtra("filename", filename);
                startActivity(intent);
                break;


            case R.id.ocr:
                bdOcr reco_words = new bdOcr();
                String res_text = reco_words.GetText(thisPhotoPath);
                if (!res_text.equals("")) {  // 如果没找到就会返回一个空
                    // 获取剪贴板管理器
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    // 创建普通字符型ClipData
                    ClipData mClipData = ClipData.newPlainText("Label", res_text);
                    // 将ClipData内容放到系统贴板里。
                    cm.setPrimaryClip(mClipData);
                    Toast.makeText(MediaActivity.this, "结果已存入系统剪切板", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MediaActivity.this, "发生异常,请重试", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 结果回调
        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                PictureBean pictureBean = data.getParcelableExtra(PictureSelector.PICTURE_RESULT);
                Log.i(TAG, "是否裁剪: " + pictureBean.isCut());
                Log.i(TAG, "原图地址: " + pictureBean.getPath());
                Log.i(TAG, "图片 Uri: " + pictureBean.getUri());
                if (pictureBean.isCut()) {
                    mIvImage.setImageBitmap(BitmapFactory.decodeFile(pictureBean.getPath()));
                } else {
                    mIvImage.setImageURI(pictureBean.getUri());
                }

                // 获取内部存储状态  
                String state = Environment.getExternalStorageState();
                verifyStoragePermissions(this);
                // 如果状态不是mounted，无法读写
                if (!state.equals(Environment.MEDIA_MOUNTED)) {
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                }

                // 创建路径
                File createDir = new File(photos_dir);
                if (!createDir.exists()) {
                    createDir.mkdirs();
                }

                // 保存图片文件名
                Calendar now = new GregorianCalendar();
                SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                String photoName = simpleDate.format(now.getTime());

                // 写入文件
                try {
                    File file = new File(photos_dir + photoName + ".jpg");
                    verifyStoragePermissions(this);
                    FileOutputStream out = new FileOutputStream(file);
                    Bitmap bitmap = ((BitmapDrawable) mIvImage.getDrawable()).getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                resetPhotoList();

                FileUtils.deleteAllCacheImage(this);
            }
        }
    }

    // 重新加载照片下拉列表，每次进入activity、拍照、删除时调用
    private void resetPhotoList() {
        String[] photosName = getPhotosList();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, photosName);
        photosList.setAdapter(adapter);
        photosList.setOnItemSelectedListener(MediaActivity.this);
    }

    // 获取数据区有哪些照片
    public String[] getPhotosList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        File createDir = new File(photos_dir);
        if (!createDir.exists()) {
            createDir.mkdirs();
            return new String[0];
        }

        return createDir.list();

    }

    // 显示选中的照片
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        String content = parent.getItemAtPosition(position).toString();
        if (parent.getId() == R.id.photos_list) {
            try {
                thisPhotoPath = photos_dir + content;
                File file = new File(thisPhotoPath);
                FileInputStream input = new FileInputStream(file);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                mShowImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // 权限相关
    public static void verifyStoragePermissions(Activity activity) {
        if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

}
