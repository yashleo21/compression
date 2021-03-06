package com.sample.compressor

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sample.compressor.helper.OnStartDragListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.mutlple_image.*
import kotlinx.android.synthetic.main.test.selected
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


class ChooseImages: AppCompatActivity(), OnStartDragListener {
    var userSelectedImageUriList:ArrayList<Uri>?=null


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(START or END, 0) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter as MyAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                adapter.moveItem(from, to)
                adapter.notifyItemMoved(from, to)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder?.itemView?.alpha = 1.0f
            }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mutlple_image)

        val horizontalLayout = LinearLayoutManager(
            this@ChooseImages,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        imagesrecycler.layoutManager = horizontalLayout
        itemTouchHelper.attachToRecyclerView(imagesrecycler)
        setImageHeight()

        setClickListeners()

    }


    private fun setImageHeight(){
        val newHeight: Float = Util.getHeightBasedOnAspect(1.3f,this@ChooseImages)
        val height =Math.round(newHeight)

        val parms = FrameLayout.LayoutParams(Util.getScreenWidth(this@ChooseImages), height)
        selectedPictureImageView.layoutParams = parms
        cropImageView.layoutParams = parms
    }


    private fun setClickListeners(){
        selected.setOnClickListener { chooseImage() }
        crop_icon.setOnClickListener { cropImage() }
        apply.setOnClickListener { getCroppedImage() }
        fill_icon.setOnClickListener{makeImageAspectFill()}
    }


    private fun cropImage(){
        if(!userSelectedImageUriList.isNullOrEmpty()){
            selectedPictureImageView.visibility =View.GONE
            cropImageView.visibility = View.VISIBLE
            cropImageView.setImageUriAsync(userSelectedImageUriList!![userSelectedImageUriList!!.size-1])
            //cropImageView.getCroppedImageAsync();
            //cropImageView.setOnCropImageCompleteListener(CropImageView.OnCropImageCompleteListener())
        }

    }


    private fun getCroppedImage(){

        // on cropped
        val croppedImage = cropImageView.croppedImage
        cropImageView.visibility = View.GONE
        selectedPictureImageView.visibility = View.VISIBLE
        selectedPictureImageView.setImageBitmap(croppedImage)


    }


    private fun makeImageAspectFill(){
        if(selectedPictureImageView.scaleType == ImageView.ScaleType.CENTER_INSIDE) {
            selectedPictureImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }else{
            selectedPictureImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }


    private fun chooseImage() {
        //val intent = Intent()
       // intent.type = "image/*"
        //intent.type = "image/* video/*"
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        //intent.action = Intent.ACTION_GET_CONTENT
       // startActivityForResult(intent, PICK_IMAGE_REQUEST)


        //val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //intent.type = "image/* video/*"
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        //startActivityForResult(intent, PICK_IMAGE_REQUEST)


        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if(data!=null) {
                var fileUri: Uri? =null
                if(data?.clipData != null) {
                    val count = data.clipData?.itemCount; //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                    for (i in 0 until data.clipData?.itemCount!!) {
                        fileUri = data.clipData?.getItemAt(i)?.uri

                        if (userSelectedImageUriList == null) {
                            userSelectedImageUriList = ArrayList<Uri>()
                        }
                        Log.d("uri-string",fileUri.toString())
                        userSelectedImageUriList?.add(fileUri!!)
                    }
                }else {

                    fileUri = data.data
                    if (userSelectedImageUriList == null) {
                        userSelectedImageUriList = ArrayList<Uri>()
                    }
                    Log.d("uri-string",fileUri.toString())
                    userSelectedImageUriList?.add(fileUri!!)
                }
                icons.visibility = View.VISIBLE

                val contentResolver = contentResolver
                try {
                    // Open the file input stream by the uri.
                    val inputStream: InputStream? = contentResolver.openInputStream(fileUri!!)

                    // Get the bitmap.
                    val imgBitmap = BitmapFactory.decodeStream(inputStream)

                    // Show image bitmap in imageview object.
                    selectedPictureImageView.setImageBitmap(imgBitmap)
                    selectedPictureImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                   // Picasso.with(this).load(fileUri).placeholder(R.mipmap.ic_launcher).into(selectedPictureImageView)

                    val adapters =  MyAdapter(this, userSelectedImageUriList,this)
                    imagesrecycler.adapter = adapters

                    inputStream?.close()
                } catch (ex: FileNotFoundException) {
                    Log.e("multiimage", "file not found", ex)
                } catch (ex: IOException) {
                    Log.e("multiimage", "io exception", ex)
                }
            }
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
       // mItemTouchHelper!!.startDrag(viewHolder!!)
        itemTouchHelper.startDrag(viewHolder!!)
    }



}