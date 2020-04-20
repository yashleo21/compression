package com.sample.compressor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.start_activity.*

class StartActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_activity)

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

}