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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.appyvet.materialrangebar.RangeBar;
import com.filestack.Client;
import com.filestack.Config;
import com.filestack.FileLink;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class VideoCompressActivity extends AppCompatActivity {

    public static final String TAG = VideoCompressActivity.class.getSimpleName();
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
    private CompressReceiver compressReceiver;
    //
    private TextView mediaInfo;
    private RecyclerView timelineView;

    private RestartReceiver restartService;
    NotificationManager notificationManager;
    //
    private TimelineAdapter timelineAdapter;
    //
    private ProgressBar pbTimeline;

    private Runnable myRunnable;
    //
    private Future longRunningTaskFuture;
    //
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private ProgressiveMediaSource dataSource;
    //
    private RangeBar rangeBar;
    private ImageView iv_play;


    private long startTime, endTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_layout);


        IntentFilter filter = new IntentFilter(CompressorConstant.BROADCAST_COMPRESS_UPLOAD);
        IntentFilter filters = new IntentFilter(CompressorConstant.BROADCAST_STOPPED);

        compressReceiver = new CompressReceiver();
        restartService = new RestartReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(compressReceiver, filter);
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
                     intent.setType("video/*;image/*");
                    //intent.setType("audio/*"); //选择音频
                    //intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
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

        Button btn_trim = findViewById(R.id.btn_trim);
        btn_trim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Trim clicked");
                startTime = System.currentTimeMillis();
                try {
                    String destPath = tv_output.getText().toString() + File.separator + "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss", getLocale()).format(new Date()) + ".mp4";
                    Constant.Companion.setDestinationPath(destPath);
                    Constant.Companion.setSourcePath(tv_input.getText().toString());
                    TrimVideoUtils.startTrim(Constant.Companion.getSourcePath(), Constant.Companion.getDestinationPath(), 0L, 10000L, new OnTrimVideoListener() {
                        @Override
                        public void onTrimStarted() {
                        Log.d(TAG, "onTrimStarted");
                        }

                        @Override
                        public void getResult(Uri uri) {
                            long endTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                            Log.d(TAG, "getResult " + uri+ " TOTAL TIME: " + endTime);
                        }

                        @Override
                        public void cancelAction() {
                            Log.d(TAG, "cancelAction");
                        }

                        @Override
                        public void onError(String message) {

                            Log.d(TAG, "onError :" + message);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        tv_input = (TextView) findViewById(R.id.tv_input);
        tv_output = (TextView) findViewById(R.id.tv_output);
        tv_output.setText(outputDir);
        tv_indicator = (TextView) findViewById(R.id.tv_indicator);
        tv_progress = (TextView) findViewById(R.id.tv_progress);

        pb_compress = (ProgressBar) findViewById(R.id.pb_compress);

        mediaInfo = (TextView) findViewById(R.id.tv_mediaInfo);
        timelineView = findViewById(R.id.rv_timeline);
        pbTimeline = findViewById(R.id.pb_timeline);
        playerView = findViewById(R.id.pv_video);

        timelineAdapter = new TimelineAdapter(this);
        timelineView.setAdapter(timelineAdapter);
        //
        rangeBar = findViewById(R.id.rangebar);
        //
        iv_play = findViewById(R.id.iv_play);
        exoPlayer = new SimpleExoPlayer.Builder(this).build();
        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        playerView.setPlayer(exoPlayer);


        //
        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex, String leftPinValue,
                                              String rightPinValue) {
                Log.d(TAG, "Rangebar changed: leftPinIndex "
                        + leftPinIndex + " rightPinIndex" + rightPinIndex + " leftPinValue"
                        + leftPinValue + " rightPinValue "+ rightPinValue);
                if (exoPlayer != null) {
                    exoPlayer.seekTo(leftPinIndex);
                }
            }

            @Override
            public void onTouchStarted(RangeBar rangeBar) {

            }

            @Override
            public void onTouchEnded(RangeBar rangeBar) {

            }
        });

        iv_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (exoPlayer != null && dataSource != null) {
                    exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
                }
            }
        });
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

                    /*Intent intent = new Intent(this, TimelineActivity.class);
                    intent.putExtra("videoPath", inputPath);
                    startActivity(intent);*/
                    //Set Media meta data info here
                    final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    File file = new File(inputPath);
                    FileInputStream fileInputStream = new FileInputStream(inputPath);
                    mediaMetadataRetriever.setDataSource(fileInputStream.getFD());
                    Log.d(TAG, "media meta data received: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
                    //
                    timelineAdapter.clearCurrentList();
                    extractMediaFrames(mediaMetadataRetriever);
                    //
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(VideoCompressActivity.this, "agent");
                    dataSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(inputPath));
                    exoPlayer.prepare(dataSource);
                    exoPlayer.seekTo(0);
                    exoPlayer.setPlayWhenReady(false);


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                inputPath = "/storage/emulated/0/DCIM/Camera/VID_20170522_172417.mp4"; // 图片文件路径
//                tv_input.setText(inputPath);// /storage/emulated/0/DCIM/Camera/VID_20170522_172417.mp4
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }*/
    }

    private void extractMediaFrames(final MediaMetadataRetriever mediaMetadataRetriever) {
        String result = "";
        result += "MimeType: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) + "\n";
        result += "Duration: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) + "ms\n";
        result += "Bitrate: " + mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) + "bits/s\n";

        mediaInfo.setText(result);
        long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        //
        rangeBar.setTickStart(0f);
        rangeBar.setTickEnd(duration);
        rangeBar.setTickInterval(1f);

        /*pbTimeline.setVisibility(View.VISIBLE);
        timelineView.setVisibility(View.GONE);*/

        //Set an executor for background thread op
        ExecutorService executor = Executors.newSingleThreadExecutor();
        myRunnable = new Runnable() {
            @Override
            public void run() {
//Prepare data source
                long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                long currentDuration = 0L;
                final ArrayList<Bitmap> frames = new ArrayList<>();
                while (currentDuration <= duration) {
                    //Will update every 5s
                    frames.add(mediaMetadataRetriever.getFrameAtTime(currentDuration * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC));
                    VideoCompressActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /*pbTimeline.setVisibility(View.GONE);
                            timelineView.setVisibility(View.VISIBLE);*/
                            timelineAdapter.updateList(frames);
                            Log.d(TAG, "Bitmap list size: " + frames.size());
                        }
                    });
                    currentDuration += 1000;
                }
                mediaMetadataRetriever.close();
            }
        };

        longRunningTaskFuture = executor.submit(myRunnable);

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
                      intent.setType("video/*;image/*");
                     //intent.setType("audio/*"); //选择音频
                     //intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
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


    public class CompressReceiver extends BroadcastReceiver {

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(compressReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(restartService);

    }
}
