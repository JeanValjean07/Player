package com.suming.player.ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import data.MediaModel.MiniMediaItemForList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FragmentPlayListCustomAdapter(
    private val context: Context,
    private val onDeleteClick: (Long) -> Unit,
    private val onPlayClick: (Uri) -> Unit
):PagingDataAdapter<MiniMediaItemForList, FragmentPlayListCustomAdapter.ViewHolder>(DiffUtil)  {
    //条目比较器 DiffUtil
    companion object {
        val DiffUtil = object : DiffUtil.ItemCallback<MiniMediaItemForList>() {
            override fun areItemsTheSame(oldItem: MiniMediaItemForList, newItem: MiniMediaItemForList): Boolean {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }
            override fun areContentsTheSame(oldItem: MiniMediaItemForList, newItem: MiniMediaItemForList): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        var itemFrameJob: Job? = null
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonDelete: ImageView = itemView.findViewById(R.id.ButtonDelete)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //协程作用域
    private val coroutineScope_LoadFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_fragment_play_list_custom_adapter_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.itemName.isSelected = true
        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = if (item.artist == "<unknown>" || item.artist == "") { "未知艺术家" } else { item.artist }
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutineScope_LoadFrame.launch { setHolderFrame(item, holder) }
        //点击事件设定
        holder.ButtonDelete.setOnClickListener { onDeleteClick(item.uriNumOnly) }
        holder.ButtonPlay.setOnClickListener { onPlayClick(item.uri) }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
    }


    //Functions

    //内部Functions
    //检查缩略图
    private suspend fun setHolderFrame(item: MiniMediaItemForList, holder: ViewHolder) {
        //设置文件
        var covers_path = File(context.filesDir, "miniature/music_cover")
        if (item.type == "video"){
            covers_path = File(context.filesDir, "miniature/video_cover")
        }
        else if (item.type == "music"){
            covers_path = File(context.filesDir, "miniature/music_cover")
        }
        val cover_item_file = File(covers_path, "${item.uriNumOnly}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main) { holder.itemFrame.setImageBitmap(frame) }
        }
    }


}