package com.suming.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import data.model.VideoItem

class VideoAdapter(
    private val data: List<VideoItem>
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    // ViewHolder：缓存 item 里的子 View
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
    }

    //onCreateViewHolder：创建 ViewHolder（第一次需要显示 item 时调用）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_adapter_items, parent, false)
        return VH(view)
    }

    //onBindViewHolder：把数据绑定到 ViewHolder
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.tvName.text = item.name
        holder.tvDuration.text = "${item.durationMs / 1000} 秒"
        holder.ivThumb.setImageURI(item.thumbnailUri)
    }

    // 总条数
    override fun getItemCount(): Int = data.size
}
