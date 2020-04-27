package com.sample.compressor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var bitmapList = ArrayList<Bitmap>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return TimelineViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TimelineViewHolder) holder.bindView(position)
    }

    override fun getItemCount(): Int {
        return bitmapList.size
    }

    inner class TimelineViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val timelineFrame = view.findViewById<ImageView>(R.id.iv_video_frame)

        fun bindView(pos: Int) {
            timelineFrame.setImageBitmap(bitmapList[pos])
        }
    }

    class TimelineDiffUtil(val oldList: ArrayList<Bitmap>, val newList: ArrayList<Bitmap>): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    fun updateList(data: ArrayList<Bitmap>) {
        /*bitmapList.clear()
        bitmapList.addAll(data)
        notifyDataSetChanged()*/
        val diffResult = DiffUtil.calculateDiff(TimelineDiffUtil(bitmapList, data))
        diffResult.dispatchUpdatesTo(this)
        bitmapList.clear()
        bitmapList.addAll(data)
        //notifyItemInserted(data.size - 1)
    }

    fun clearCurrentList() {
        bitmapList.clear()
        Log.d("TimelineAdapter", "List cleared")
        notifyDataSetChanged()
    }
}