package com.sample.compressor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class VideoCompressActivity extends AppCompatActivity {

    public static final String NOTIFICATION_CHANNEL_ID = "compressorChannel";
    //User visible Channel Name
    public static final String CHANNEL_NAME = "uploadsChannel";
    // Importance applicable to all the notifications in this Channel
    private static final int REQUEST_FOR_VIDEO_FILE = 1000;
    private TextView tv_input, tv_output, tv_indicator, tv_progress;
    private String outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    public static final int STORAGE_REQ_CODE = 113;

    private String inputPath;
    private String outputPath;

    private ProgressBar pb_compress;
    private UploadReceiver uploadReceiver;
    private RestartReceiver restartService;
    NotificationManager notificationManager;

    private long startTime, endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_layout);

        IntentFilter filter = new IntentFilter(CompressorConstant.BROADCAST_UPLOAD);
        IntentFilter filters = new IntentFilter(CompressorConstant.BROADCAST_STOPPED);

        uploadReceiver = new UploadReceiver();
        restartService = new RestartReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(restartService, filters);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        Button btn_select = (Button) findViewById(R.id.btn_select);
        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Util.hasStoragePermission(STORAGE_REQ_CODE,VideoCompressActivity.this)){
                    Intent intent = new Intent();
                    /* 开启Pictures画面Type设定为image */
                    //intent.setType("video/*;image/*");
                    //intent.setType("audio/*"); //选择音频
                    intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, REQUEST_FOR_VIDEO_FILE);
                }else {
                    checkStoragePermissionAndCompress();
                }
            }
        });

        Button btn_compress = (Button) findViewById(R.id.btn_compress);
        btn_compress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String destPath = tv_output.getText().toString() + File.separator + "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss", getLocale()).format(new Date()) + ".mp4";
                Constant.Companion.setDestinationPath(destPath);
                Constant.Companion.setSourcePath(tv_input.getText().toString());

                //with foreground service
                if(Util.hasStoragePermission(STORAGE_REQ_CODE,VideoCompressActivity.this)){
                Intent uploadIntent = new Intent(VideoCompressActivity.this, UploadService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(uploadIntent);
                } else {
                    startService(uploadIntent);
                }
                }else {
                    checkStoragePermissionAndCompress();
                }

                //work manager
                //startManager();
            }
        });

        tv_input = (TextView) findViewById(R.id.tv_input);
        tv_output = (TextView) findViewById(R.id.tv_output);
        tv_output.setText(outputDir);
        tv_indicator = (TextView) findViewById(R.id.tv_indicator);
        tv_progress = (TextView) findViewById(R.id.tv_progress);

        pb_compress = (ProgressBar) findViewById(R.id.pb_compress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOR_VIDEO_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
//                inputPath = data.getData().getPath();
//                tv_input.setText(inputPath);

                try {
                    inputPath = Util.getFilePath(this, data.getData());
                    tv_input.setText(inputPath);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

//                inputPath = "/storage/emulated/0/DCIM/Camera/VID_20170522_172417.mp4"; // 图片文件路径
//                tv_input.setText(inputPath);// /storage/emulated/0/DCIM/Camera/VID_20170522_172417.mp4
            }
        }
    }

    private Locale getLocale() {
        Configuration config = getResources().getConfiguration();
        Locale sysLocale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sysLocale = getSystemLocale(config);
        } else {
            sysLocale = getSystemLocaleLegacy(config);
        }

        return sysLocale;
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config){
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config){
        return config.getLocales().get(0);
    }

    private void startManager(){
        WorkManager mWorkManager = WorkManager.getInstance();
        OneTimeWorkRequest mRequest = new OneTimeWorkRequest.Builder(UploadWorker.class).build();
        mWorkManager.enqueue(mRequest);


        WorkManager.getInstance().getWorkInfoByIdLiveData(mRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                if (workInfo != null) {
                    WorkInfo.State state = workInfo.getState();
                    tv_indicator.append(state.toString() + "\n");
                    if(workInfo.getState() == WorkInfo.State.SUCCEEDED){
                        Log.d("livedata status","success");
                        createNotification("success");
                    }else if(workInfo.getState() == WorkInfo.State.FAILED){
                        createNotification("failed");
                        Log.d("livedata status","failed");
                    }

                }
            }
        });
    }


 public void createNotification(String status){

     int importance = NotificationManager.IMPORTANCE_DEFAULT;
     //Notification channel should only be created for devices running Android 26
     NotificationChannel notificationChannel = null;
     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
         notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
     }
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

         //Boolean value to set if lights are enabled for Notifications from this Channel
         notificationChannel.enableLights(true);
         //Boolean value to set if vibration are enabled for Notifications from this Channel
         notificationChannel.enableVibration(true);
         //Sets the color of Notification Light
         notificationChannel.setLightColor(Color.GREEN);
         //Set the vibration pattern for notifications. Pattern is in milliseconds with the format {delay,play,sleep,play,sleep...}
         notificationChannel.setVibrationPattern(new long[] {
                 500,
                 500,
                 500,
                 500,
                 500
         });
         //Sets whether notifications from these Channel should be visible on Lockscreen or not
         notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
     }
     // notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         notificationManager.createNotificationChannel(notificationChannel);
     }

     NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
     builder.setContentTitle("Compressor");
     builder.setContentText(status);
     builder.setSmallIcon(R.mipmap.ic_launcher);


 }


 private void checkStoragePermissionAndCompress(){
     Dexter.withActivity((this))
             .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
             .withListener(new BasePermissionListener() {
                 @Override
                 public void onPermissionGranted(PermissionGrantedResponse response) {

                     Intent intent = new Intent();
                     /* 开启Pictures画面Type设定为image */
                     //intent.setType("video/*;image/*");
                     //intent.setType("audio/*"); //选择音频
                     intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
                     intent.setAction(Intent.ACTION_GET_CONTENT);
                     startActivityForResult(intent, REQUEST_FOR_VIDEO_FILE);
                 }

                 @Override
                 public void onPermissionDenied(PermissionDeniedResponse response) {
                     if (response.isPermanentlyDenied()) {
                         Toast.makeText(VideoCompressActivity.this,"Permission permanently denied.",Toast.LENGTH_LONG).show();
                     }
                 }
             }).check();
 }


 private void compressVideoDefault(){

              /*  VideoCompress.compressVideoMedium(tv_input.getText().toString(), destPath, new VideoCompress.CompressListener() {
                    @Override
                    public void onStart() {
                        tv_indicator.setText("Compressing..." + "\n"
                                + "Start at: " + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()));
                        pb_compress.setVisibility(View.VISIBLE);
                        startTime = System.currentTimeMillis();
                        Util.writeFile(VideoCompressActivity.this, "Start at: " + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()) + "\n");
                    }

                    @Override
                    public void onSuccess() {
                        String previous = tv_indicator.getText().toString();
                        tv_indicator.setText(previous + "\n"
                                + "Compress Success!" + "\n"
                                + "End at: " + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()));
                        pb_compress.setVisibility(View.INVISIBLE);
                        endTime = System.currentTimeMillis();
                        Util.writeFile(VideoCompressActivity.this, "End at: " + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()) + "\n");
                        Util.writeFile(VideoCompressActivity.this, "Total: " + ((endTime - startTime)/1000) + "s" + "\n");
                        Util.writeFile(VideoCompressActivity.this);
                    }

                    @Override
                    public void onFail() {
                        tv_indicator.setText("Compress Failed!");
                        pb_compress.setVisibility(View.INVISIBLE);
                        endTime = System.currentTimeMillis();
                        Util.writeFile(VideoCompressActivity.this, "Failed Compress!!!" + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()));
                    }

                    @Override
                    public void onProgress(float percent) {
                        tv_progress.setText(String.valueOf(percent) + "%");
                    }
                });*/
 }


    public class UploadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("fileLink","compressed from service");
            String status = intent.getStringExtra(CompressorConstant.EXTRA_STATUS);
            if(!TextUtils.isEmpty(status)){
                if(status.equalsIgnoreCase(CompressorConstant.STATUS_COMPLETE)){
                    createNotification("file compressed successfully");
                }else{
                    createNotification("file compression failed");
                }
            }
            tv_indicator.setText(status);
        }
    }


    public static class RestartReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast Listened", "Service tried to stop");
            Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, UploadService.class));
            } else {
                context.startService(new Intent(context, UploadService.class));
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(restartService);
    }
}
