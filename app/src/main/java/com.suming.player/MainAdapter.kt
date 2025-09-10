package com.suming.player

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import data.model.VideoItem

class MainAdapter(private val onItemClick: (VideoItem) -> Unit):
    PagingDataAdapter<VideoItem, MainAdapter.ViewHolder>(diffCallback) {

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
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_items, parent, false)
        return ViewHolder(view)
    }


    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name
        holder.tvDuration.text = "${item.durationMs / 1000} 秒"
        holder.tvThumb.load(item.thumbnailUri)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }





}//class END