package com.sample.compressor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.filestack.Client;
import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.Progress;
import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.vincent.videocompressor.VideoCompress;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;

import static com.linkedin.android.litr.MediaTransformer.*;
import static com.sample.compressor.VideoCompressActivity.CHANNEL_NAME;
import static com.sample.compressor.VideoCompressActivity.NOTIFICATION_CHANNEL_ID;


public class UploadService extends Service implements TransferListener,TransformationListener{
    private Executor executor = Executors.newSingleThreadExecutor();
    private NotificationManager notificationManager;
    private int notificationId;
    private int errorNotificationId;
    //CognitoCachingCredentialsProvider credentialsProvider;

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

                //compressVideo();
                //compressVideoWithLitr(); // this is with litr
                transcodeVideo();
                //uploadToS3(); //for uploading to S3
                //stopSelf();
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
                        //sendBroadcast();
                        /*Config config = new Config( getString(R.string.filestack_api_key)); // the key used here is working well in IOS
                        Client client = new Client(config);*/
                        Log.d("compressor","compressor succeded");
                        try {
                            uploadToS3();
                           // client.uploadAsync(Constant.Companion.getDestinationPath(), true);
                        } catch (Exception e) {
                            Log.e("Test321", "Exception = " + e.getMessage());
                            e.printStackTrace();
                        }

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


    private void compressVideoWithLitr(){

        MediaTransformer mediaTransformer = new MediaTransformer(getApplicationContext());
        mediaTransformer.transform("video_upload",
                Uri.parse(Constant.Companion.getSourcePath()),
                Constant.Companion.getDestinationPath(),
                createMediaFormat(),
                null, this,
                GRANULARITY_DEFAULT,
                null);

       /* MediaFormat sourceMediaFormat = MediaFormat.createVideoFormat("video/mp4", 1920, 1080);
        sourceMediaFormat.setInteger();
        mediaTransformer.transform(UUID.randomUUID().toString(),
                Uri.parse(Constant.Companion.getSourcePath()),
                Constant.Companion.getDestinationPath(),
                );*/
    }



    @Nullable
    private MediaFormat createMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/mp4", 1280, 720);
        mediaFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, 1280);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, 720);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 5);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        return mediaFormat;
    }


    private void uploadToS3(){
        Log.d("compressor","upload started");
        ObjectMetadata metadata = new ObjectMetadata();
        //metadata.setContentType("video/mp4");
        metadata.setContentType("image/jpeg");

        final TransferObserver observer = getTransferUtility().upload(
                "test-media-upload",  //this is the bucket name on S3
                "UPLOAD_MEDIA_DIRECTORY", //this is the path and name
                new File(Constant.Companion.getDestinationPath()), //path to the file locally
                metadata, // metadata which stores content type and all
                CannedAccessControlList.PublicRead //to make the file public
        );
        observer.setTransferListener(this);
       /* observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state.equals(TransferState.COMPLETED)) {
                    //Success
                    Log.d("compressor","s3 upload completed");
                    Log.d("compressor","uploaded video id " + id);
                    sendBroadcast(true);
                } else if (state.equals(TransferState.FAILED)) {
                    //Failed
                    Log.d("compressor","s3 upload failed");
                    sendBroadcast(false);

                    Log.d("compressor","retrying again");
                    TransferObserver newTransferObserver = getTransferUtility().resume(id);
                    newTransferObserver.setTransferListener(this);

                }else if (state.equals(TransferState.WAITING_FOR_NETWORK)) {
                    //Failed
                    Log.d("compressor","waiting for network");
                }

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("compressor", String.format("onProgressChanged: %d, total: %d, current: %d",
                        id, bytesTotal, bytesCurrent));
                sendProgressNotification((int)bytesCurrent,(int)bytesTotal,"uploading video");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("compressor","s3 upload failed");
            }
        });*/


       /* //For pausing and resuming tasks:
        // Gets id of the transfer.
        int id = observer.getId();

        // Pauses the transfer.
        transferUtility.pause(id);

        // Pause all the transfers.
        transferUtility.pauseAllWithType(TransferType.ANY);

        // Resumes the transfer.
        transferUtility.resume(id);

        // Resume all the transfers.
        transferUtility.resumeAllWithType(TransferType.ANY);*/
    }


    private void sendBroadcast(boolean isUploadSuccess) {
        Intent intent = new Intent(CompressorConstant.BROADCAST_COMPRESS_UPLOAD);
        if (TextUtils.isEmpty(Constant.Companion.getDestinationPath())) {
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_FAILED);
        } else {
            intent.putExtra(CompressorConstant.EXTRA_STATUS, CompressorConstant.STATUS_COMPLETE);
        }
        if(isUploadSuccess) {
            sendNotification("upload ","File uploaded succesfully");
        }else{
            sendNotification("upload ","File upload failed");
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

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



    private TransferUtility getTransferUtility(){
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-south-1:df6a66ff-0658-4329-a1aa-22615d8cb5c5", // Identity pool ID
                Regions.AP_SOUTH_1 // Region
        );
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        return TransferUtility.builder().s3Client(s3).context(getApplicationContext()).build();
    }

    @Override
    public void onStateChanged(int id, TransferState state) {
        if (state.equals(TransferState.COMPLETED)) {
            //Success
            Log.d("compressor","s3 upload completed");
            Log.d("compressor","uploaded video id " + id);
            sendBroadcast(true);
        } else if (state.equals(TransferState.FAILED)) {
            //Failed
            Log.d("compressor","s3 upload failed");
            sendBroadcast(false);

           /* Log.d("compressor","retrying again");
            TransferObserver newTransferObserver = getTransferUtility().resume(id);
            newTransferObserver.setTransferListener(this);*/

        }else if (state.equals(TransferState.WAITING_FOR_NETWORK)) {
            //Failed
            Log.d("compressor","waiting for network");
        }
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        Log.d("compressor", String.format("onProgressChanged: %d, total: %d, current: %d",
                id, bytesTotal, bytesCurrent));
        sendProgressNotification((int)bytesCurrent,(int)bytesTotal,"uploading video");
    }

    @Override
    public void onError(int id, Exception ex) {
        Log.d("compressor","s3 upload failed");
    }

    @Override
    public void onStarted(@NonNull String id) {
     Log.d("litr","started");
    }

    @Override
    public void onProgress(@NonNull String id, float progress) {
        Log.d("litr",progress + "");
    }

    @Override
    public void onCompleted(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        Log.d("litr","completed");
    }

    @Override
    public void onCancelled(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        Log.d("litr","cancelled");
    }

    @Override
    public void onError(@NonNull String id, @Nullable Throwable cause, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        Log.d("litr","errrorssss");
    }


    private void transcodeVideo(){
        Transcoder.into(Constant.Companion.getDestinationPath())
                .addDataSource(Constant.Companion.getSourcePath())
                .setListener(new TranscoderListener() {
                    public void onTranscodeProgress(double progress) {
                        Log.d("transcode","transcode progress - " + progress + "");
                    }
                    public void onTranscodeCompleted(int successCode) {
                        Log.d("transcode","transcode completed");
                    }
                    public void onTranscodeCanceled() {
                        Log.d("transcode","transcode cancelled");
                    }
                    public void onTranscodeFailed(@NonNull Throwable exception) {
                        Log.d("transcode","transcode failed");
                    }
                }).transcode();
    }
}
