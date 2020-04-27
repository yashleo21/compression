package com.sample.compressor

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    fun updateList(data: ArrayList<Bitmap>) {
        bitmapList.clear()
        bitmapList.addAll(data)
        notifyDataSetChanged()
    }
}