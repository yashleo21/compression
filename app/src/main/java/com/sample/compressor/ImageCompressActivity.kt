package com.sample.compressor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.loadBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class ImageCompressActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private var actualImage: File? = null
    private var compressedImage: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actualImageView.setBackgroundColor(getRandomColor())
        clearImage()
        setupClickListener()
    }

    private fun setupClickListener() {
        chooseImageButton.setOnClickListener { chooseImage() }
        compressImageButton.setOnClickListener { compressImage() }
        customCompressImageButton.setOnClickListener { customCompressImage() }
        customCompressImageButtonUpload.setOnClickListener { upload() }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun compressImage() {
        actualImage?.let { imageFile ->
            lifecycleScope.launch {
                // Default compression
                compressedImage = Compressor.compress(this@ImageCompressActivity, imageFile)
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }

    private fun customCompressImage() {
        actualImage?.let { imageFile ->
            lifecycleScope.launch {
                // Default compression with custom destination file
                /*  compressedImage = Compressor.compress(this@MainActivity, imageFile) {
                      default()
                      getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also {
                          val file = File("${it.absolutePath}${File.separator}my_image.${imageFile.extension}")
                          destination(file)
                      }
                  }*/

                // Full custom
               /* compressedImage = Compressor.compress(this@MainActivity, imageFile) {
                    quality(75)
                    format(Bitmap.CompressFormat.JPEG)
                }
*/
                val compressedImageFile = File(this@ImageCompressActivity.getExternalFilesDir("lbb"), "test.jpg") // to keep images in folder
                compressedImage = Compressor.compress(this@ImageCompressActivity, imageFile) {
                    quality(75)
                    format(Bitmap.CompressFormat.JPEG)
                    destination(compressedImageFile)
                }
                Constant.destinationPath = compressedImage.toString()
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }

    private fun setCompressedImage() {
        compressedImage?.let {
            compressedImageView.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))

            compressedSizeTextView.text = String.format("Size : %s", getReadableFileSize(it.length()))
            Toast.makeText(this, "Compressed image save in " + it.path, Toast.LENGTH_LONG).show()
            Log.d("Compressor", "Compressed image save in " + it.path)
        }
    }

    private fun clearImage() {
        actualImageView.setBackgroundColor(getRandomColor())
        compressedImageView.setImageDrawable(null)
        compressedImageView.setBackgroundColor(getRandomColor())
        compressedSizeTextView.text = "Size : -"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data == null) {
                showError("Failed to open picture!")
                return
            }
            try {
                actualImage = FileUtil.from(this, data.data)?.also {
                    actualImageView.setImageBitmap(loadBitmap(it))
                    actualSizeTextView.text = String.format("Size : %s", getReadableFileSize(it.length()))
                    clearImage()
                }
            } catch (e: IOException) {
                showError("Failed to read picture data!")
                e.printStackTrace()
            }
        }
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun getRandomColor() = Random().run {
        Color.argb(100, nextInt(256), nextInt(256), nextInt(256))
    }

    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) {
            return "0"
        }
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun upload(){
        val uploadIntent =
            Intent(this@ImageCompressActivity, UploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(uploadIntent)
        } else {
            startService(uploadIntent)
        }

    }
}
