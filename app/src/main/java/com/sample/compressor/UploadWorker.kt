package com.sample.compressor

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vincent.videocompressor.VideoController


class UploadWorker (appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            val compressStatus = compress()
            Log.d("compress staus",compressStatus.toString())
            return Result.success()

        } catch (e: Exception) {
            return Result.failure()
        }
    }

    fun compress(): Boolean {
        //compress image
        return VideoController.getInstance()
            .convertVideo(Constant.sourcePath, Constant.destinationPath, VideoController.COMPRESS_QUALITY_LOW) {
                Log.d("progress", it.toString())
            }
    }


    private fun showNotification(task: String, desc: String) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_channel"
        val channelName = "compressorChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(task)
            .setContentText(desc)
            .setSmallIcon(R.mipmap.sym_def_app_icon)
        manager.notify(1, builder.build())
    }
}
