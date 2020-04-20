package com.sample.compressor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.filestack.FileLink
import com.filestack.android.FsConstants
//import com.filestack.android.FsConstants

import kotlinx.android.synthetic.main.start_activity.*

class StartActivity:AppCompatActivity() {

   private var uploadReceiver: UploadReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_activity)

       val filterr = IntentFilter(FsConstants.BROADCAST_UPLOAD)
        uploadReceiver = UploadReceiver()
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver!!, filterr)
        clickListeners()
    }


    private fun clickListeners(){
        imageCompression.setOnClickListener { openImageCompressionView()}
        videoCompression.setOnClickListener { openVideoCompressionView() }
        videoUpload.setOnClickListener { openVideoToFilestack() }

    }

    private fun openImageCompressionView(){
        val `in` = Intent(this, ImageCompressActivity::class.java)
        startActivity(`in`)
    }

    private fun openVideoCompressionView(){
        val `in` = Intent(this, VideoCompressActivity::class.java)
        startActivity(`in`)
    }

    private fun openVideoToFilestack(){
        val `in` = Intent(this, FileStackActivity::class.java)
        startActivity(`in`)
    }

    // Receives upload broadcasts from SDK service
    class UploadReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            var fileLink = intent.getSerializableExtra(FsConstants.EXTRA_FILE_LINK) as FileLink
            Log.d("fileLink", fileLink.toString())
            Log.d("compressor", "file uploaded")
            //setLoading(false);
        }
    }


    override fun onDestroy() {
        super.onDestroy()
       LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver!!)
    }

}