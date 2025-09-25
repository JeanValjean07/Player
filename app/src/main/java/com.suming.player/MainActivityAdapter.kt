package com.suming.player

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import data.model.VideoItem

class MainActivityAdapter(private val onItemClick: (VideoItem) -> Unit):
    PagingDataAdapter<VideoItem, MainActivityAdapter.ViewHolder>(diffCallback) {

    //DiffUtil
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<VideoItem>() {
            override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvOption: CardView = itemView.findViewById(R.id.options)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_items, parent, false)
        return ViewHolder(view)
    }
    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name
        holder.tvDuration.text = formatTime1(item.durationMs)
        holder.tvThumb.load(item.thumbnailUri)
        //点击事件
        holder.tvThumb.setOnClickListener { onItemClick(item) }
        holder.tvDuration.setOnClickListener {
            Toast.makeText(holder.itemView.context, "视频时长: ${holder.tvDuration.text}", Toast.LENGTH_SHORT).show()
        }
        holder.tvOption.setOnClickListener {
            Toast.makeText(holder.itemView.context, "功能开发中", Toast.LENGTH_SHORT).show()
        }



    }


    //Functions
    @SuppressLint("DefaultLocale")
    private fun formatTime1(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours == 0L){
            String.format("%02d:%02d",  minutes, seconds)
        }else{
            String.format("%02d:%02d:%02d",  hours, minutes, seconds)
        }
    }





}//class END