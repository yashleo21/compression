package com.sample.compressor

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sample.compressor.helper.OnStartDragListener
import com.sample.compressor.helper.SimpleItemTouchHelperCallback
import kotlinx.android.synthetic.main.mutlple_image.*
import kotlinx.android.synthetic.main.test.selected
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


class ChooseImages: AppCompatActivity(), OnStartDragListener {
    var userSelectedImageUriList:ArrayList<Uri>?=null
   // private var mItemTouchHelper: ItemTouchHelper? = null

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


        selected.setOnClickListener { chooseImage() }
    }


    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if(data!=null) {
                val fileUri: Uri? = data.data
                if (userSelectedImageUriList == null) {
                    userSelectedImageUriList = ArrayList<Uri>()
                }
                userSelectedImageUriList!!.add(fileUri!!)
                val contentResolver = contentResolver
                try {
                    // Open the file input stream by the uri.
                    val inputStream: InputStream? = contentResolver.openInputStream(fileUri)

                    // Get the bitmap.
                    val imgBitmap = BitmapFactory.decodeStream(inputStream)

                    // Show image bitmap in imageview object.
                    selectedPictureImageView.setImageBitmap(imgBitmap)
                    val adapters =  MyAdapter(this, userSelectedImageUriList,this)
                    imagesrecycler.adapter = adapters

                   /* val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(adapters)
                    mItemTouchHelper = ItemTouchHelper(callback)
                    mItemTouchHelper?.attachToRecyclerView(imagesrecycler)*/


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

    public fun startDragging(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }


}