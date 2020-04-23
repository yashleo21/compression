package com.sample.compressor;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.filestack.Client;
import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.Policy;
import com.filestack.Progress;

import org.reactivestreams.Subscription;

import java.security.Security;

import io.reactivex.CompletableObserver;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class TestFileStack extends AppCompatActivity {
    private static final int REQUEST_PICK = 1;

    private static final String API_KEY = "";
    private static final String APP_SECRET = "";


    Client client;
    private TextView textView;

    private String handle;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            uploadFile(uri);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.testsactivity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        textView = (TextView) findViewById(R.id.text);
    }

    private String getPathFromMediaUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(uri,  projection, null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void onClickDelete(View view) {
      /*  textView.append("DELETING " + handle + " START\n");
        FileLink fileLink = new FileLink(API_KEY, handle, security);
        fileLink.deleteAsync()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        textView.append("DELETING " + handle + " FINISHED\n");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        textView.append("DELETING " + handle + " ERROR\n");
                    }
                });*/
    }

    public void onClickUpload(View view) {
        textView.append("UPLOAD START\n");

        Intent mediaPickerIntent = new Intent(Intent.ACTION_PICK);
        mediaPickerIntent.setType("*/*");
        if (mediaPickerIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(mediaPickerIntent, REQUEST_PICK);
        } else {
            textView.append("UPLOAD ERROR\n");
        }
    }

    private void uploadFile(Uri uri) {
        final Context context = getApplicationContext();
        String path = getPathFromMediaUri(context, uri);
        String apiKey = getString(R.string.filestack_api_key);
        Config config = new Config(apiKey);
        client = new Client(config);
        client.uploadAsync(path,false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FlowableSubscriber<Progress<FileLink>>() {
                    @Override
                    public void onSubscribe(Subscription s) {

                    }

                    @Override
                    public void onNext(Progress<FileLink> fileLinkProgress) {
                        FileLink hr = fileLinkProgress.getData();
                        Log.d("new filestack","on next called" + hr.getHandle().toString());
                        textView.append("UPLOAD FINISHED\n");
                    }



                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        Log.d("new filestack","errrrrorrrrr");
                        textView.append("UPLOAD ERROR\n");
                    }

                    @Override
                    public void onComplete() {
                        Log.d("new filestack","on complete called");
                    }
                });



    }
}
