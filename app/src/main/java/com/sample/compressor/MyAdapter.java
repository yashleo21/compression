package com.sample.compressor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sample.compressor.helper.ItemTouchHelperAdapter;
import com.sample.compressor.helper.ItemTouchHelperViewHolder;
import com.sample.compressor.helper.OnStartDragListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Oclemy on 8/4/2016 for ProgrammingWizards Channel and http://www.camposha.com.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> implements ItemTouchHelperAdapter {

    Context c;
    ArrayList<Uri> spacecrafts;
    private final OnStartDragListener mDragStartListener;

    public MyAdapter(Context c, ArrayList<Uri> spacecrafts,OnStartDragListener dragStartListener) {
        this.c = c;
        this.spacecrafts = spacecrafts;
        mDragStartListener = dragStartListener;
    }


    public void moveItem(int from, int to){
        Uri m = spacecrafts.get(from);
        spacecrafts.remove(from);
        if(to < from){
            spacecrafts.add(to, m);
        }else{
            spacecrafts.add(to-1,m);
        }
    }



    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(c).inflate(R.layout.model,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        Uri s=spacecrafts.get(position);
        Picasso.with(c).load(s).placeholder(R.mipmap.ic_launcher).into(holder.img);

        // Start a drag whenever the handle view it touched
        holder.img.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return spacecrafts.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(spacecrafts, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        spacecrafts.remove(position);
        notifyItemRemoved(position);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {

        TextView nameTxt;
        ImageView img;

        public MyViewHolder(View itemView) {
            super(itemView);
            img= (ImageView) itemView.findViewById(R.id.chotuimage);

        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}
