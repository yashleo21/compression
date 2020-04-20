package com.sample.compressor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.filestack.Client;
import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.Policy;
import com.filestack.Progress;
import com.filestack.android.internal.Util;
import com.vincent.videocompressor.VideoCompress;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Flowable;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

import static com.sample.compressor.VideoCompressActivity.CHANNEL_NAME;
import static com.sample.compressor.VideoCompressActivity.NOTIFICATION_CHANNEL_ID;


public class UploadService extends Service {
    private Executor executor = Executors.newSingleThreadExecutor();
    private NotificationManager notificationManager;
    private int notificationId;
    private int errorNotificationId;

    private static final DecimalFormat format = new DecimalFormat("#.##");
    private static final long MiB = 1024 * 1024;
    private static final long KiB = 1024;
    Client client;


    @Override
    public void onCreate() {
        super.onCreate();
        notificationId = UUID.randomUUID().hashCode();
        errorNotificationId = notificationId + 1;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }



    }


    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        CharSequence name =getString(R.string.notify_channel_upload_name);;
        String description = getString(R.string.notify_channel_upload_description);;
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel =
                new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     Log.d("compressor","on start command called");
        Notification serviceNotification =
                progressNotification(0, 430, "").build();
        startForeground(notificationId, serviceNotification);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                // function for uploading
                Log.d("compressor","compress called");
                compressVideo();
                stopSelf();
            }
        });
        return START_STICKY;
    }


    private void sendProgressNotification(int done, int total, String name) {
        if (total == 0) {
            notificationManager.cancel(notificationId);
            return;
        }
        NotificationCompat.Builder builder;
        if (total == done) {
            builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(String.format(Locale.getDefault(), "Uploaded %d files", done));
            builder.setSmallIcon(R.drawable.ic_launcher_background);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        } else {
            builder = progressNotification(done, total, name);
        }
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(notificationId, builder.build());
    }

    private NotificationCompat.Builder progressNotification(int done, int total, String currentFileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(String.format(Locale.getDefault(), "Uploading %d/%d files", done, total));
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setContentText(currentFileName);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setProgress(total, done, false);
        return builder;
    }

    private void sendNotification(String title, String name) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentTitle(title);
        builder.setContentText(name);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_launcher_background);

        notificationManager.notify(errorNotificationId, builder.build());
    }


    private void compressVideo(){
           VideoCompress.compressVideoLow(Constant.Companion.getSourcePath(),Constant.Companion.getDestinationPath(), new VideoCompress.CompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess() {
                        sendBroadcast();
                        Log.d("compressor","compressor succeded");
                       // Util.getClient().uploadAsync(Constant.Companion.getDestinationPath(),false);

                    }

                    @Override
                    public void onFail() {
                        sendNotification("Compress failed","failed to compress");
                       /* tv_indicator.setText("Compress Failed!");
                        pb_compress.setVisibility(View.INVISIBLE);
                        endTime = System.currentTimeMillis();*/
                        //Util.writeFile(getApplicationContext(), "Failed Compress!!!" + new SimpleDateFormat("HH:mm:ss", getLocale()).format(new Date()));
                    }

                    @Override
                    public void onProgress(float percent) {
                      sendProgressNotification(100,Math.round(percent),"");
                      Log.d("compress","completed" + percent);

                    }
                });
    }


    @SuppressLint("CheckResult")
    private void uploadFiles(){
        String apiKey = getString(R.string.filestack_api_key);
        Config config = new Config(apiKey);
        client = new Client(config);
        // Set options and metadata for upload
        /*StorageOptions options = new StorageOptions.Builder()
                .mimeType("text/plain")
                .filename("hello.txt")
                .build();*/

        // Perform an asynchronous, non-blocking upload
        Flowable<Progress<FileLink>> upload = client.uploadAsync(Constant.Companion.getDestinationPath(), false);
        upload.doOnNext(new Consumer<Progress<FileLink>>() {
            @Override
            public void accept(Progress<FileLink> progress) throws Exception {
                System.out.printf("%f%% file uploaded\n", progress.getPercent());
                if (progress.getData() != null) {
                    FileLink file = progress.getData();
                    Log.d("compressor","uploading done" + file.toString());
                    //uploadBroadcast(file);
                }
            }
        });

    }


 /*   private void uploadBroadcast(FileLink link){
        Intent intent = new Intent(FsConstants.BROADCAST_UPLOAD);
        if(link == null){
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_FAILED);
        }else{
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_COMPLETE);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }*/

    private void sendBroadcast() {
        Intent intent = new Intent(CompressorConstant.BROADCAST_COMPRESS_UPLOAD);
        if (TextUtils.isEmpty(Constant.Companion.getDestinationPath())) {
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_FAILED);
        } else {
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_COMPLETE);
        }
        sendNotification("Compress success","File compressed succesfully");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    public String getFileSize() {

        File file = new File(Constant.Companion.getSourcePath());

        if (!file.isFile()) {
            throw new IllegalArgumentException("Expected a file");
        }
        final double length = file.length();

        if (length > MiB) {
            return format.format(length / MiB) + " MiB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KiB";
        }
        return format.format(length) + " B";
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d("compressor","task removed");
        Intent intent = new Intent(CompressorConstant.BROADCAST_STOPPED);
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("compressor","service stopped called");
    }


}
