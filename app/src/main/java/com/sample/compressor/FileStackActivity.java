
package com.sample.compressor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.android.FsActivity;
import com.filestack.android.FsConstants;
import com.filestack.transforms.tasks.ResizeTask;
import com.squareup.picasso.Picasso;

public class FileStackActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView nameView;
    private Button selectImageBtn;

    private UploadReceiver uploadReceiver;
    private RestartReceiver restartService;



    private FileLink fileLink;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filestack_layout);
        setViews();


        // Register the receiver for upload broadcasts
        IntentFilter filter = new IntentFilter(FsConstants.BROADCAST_UPLOAD);
        uploadReceiver = new UploadReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, filter);

        restartService = new RestartReceiver();
        IntentFilter filters = new IntentFilter(CompressorConstant.BROADCAST_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(restartService, filters);
    }


    private void setViews(){
        imageView = (ImageView) findViewById(R.id.img);
        nameView =(TextView) findViewById(R.id.name);
        selectImageBtn = (Button) findViewById(R.id.sel_img_btn);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }


    public FileLink getFileLink() {
        return fileLink;
    }

    @Override
    protected void onResume() {
        super.onResume();

        FileLink fileLink = getFileLink();
        if (fileLink != null) {
            int dimen = getResources().getDimensionPixelSize(R.dimen.form_image);
            String url = getAdaptiveUrl(fileLink, dimen);
            Picasso.with(this).load(url).into(imageView);
        }
    }

    // Handles user clicking select button, launches the picker UI
    private void selectImage() {
        // Start picker activity

        // For simplicity we're loading credentials from a string res, don't do this in production
        String apiKey = getString(R.string.filestack_api_key);
        if (apiKey.equals("")) {
            throw new RuntimeException("Create a string res value for \"filestack_api_key\"");
        }

        Config config = new Config(apiKey, "https://form.samples.android.filestack.com");
        Intent pickerIntent = new Intent(this, FsActivity.class);
        pickerIntent.putExtra(FsConstants.EXTRA_CONFIG, config);
        // Restrict file selections to just images
        //String[] mimeTypes = {"image/*"};
        String[] mimeTypes = {"image/*","video/*"};
        pickerIntent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypes);
        startActivity(pickerIntent);

        // Show loading progress spinner
       // ((MainActivity) getActivity()).setLoading(true);
    }


    // Creates a URL to an image sized appropriately for the form ImageView
    private String getAdaptiveUrl(FileLink fileLink, int dimen) {
        ResizeTask resizeTask = new ResizeTask.Builder()
                .fit("crop")
                .align("center")
                .width(dimen)
                .height(dimen)
                .build();

        return fileLink.imageTransform().addTask(resizeTask).url();
    }



    // Receives upload broadcasts from SDK service
    public class UploadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            fileLink = (FileLink) intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK);
            Log.d("fileLink",fileLink.toString());
            //setLoading(false);
        }
    }


    public static class RestartReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast Listened", "Service tried to stop");
            Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, com.filestack.android.internal.UploadService.class));
            } else {
                context.startService(new Intent(context, com.filestack.android.internal.UploadService.class));
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

