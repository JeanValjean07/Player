package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.DataPack.DataClassForPlay.MediaItemForList
import com.suming.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("unused")
class Recycler_Adaptor_CustomList(
    private val context: Context,
    private val onDeleteClick: (Long) -> Unit,
    private val onPlayClick: (Uri) -> Unit
):PagingDataAdapter<MediaItemForList, Recycler_Adaptor_CustomList.viewHolder>(DiffUtil)  {
    companion object {
        //比较器
        val DiffUtil = object : DiffUtil.ItemCallback<MediaItemForList>() {
            override fun areItemsTheSame(oldItem: MediaItemForList, newItem: MediaItemForList): Boolean {
                return oldItem.media_api_NUM_ID == newItem.media_api_NUM_ID
            }
            override fun areContentsTheSame(oldItem: MediaItemForList, newItem: MediaItemForList): Boolean {
                return oldItem == newItem
            }
        }


    }
    //viewHolder
    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        var itemFrameJob: Job? = null
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonDelete: ImageView = itemView.findViewById(R.id.ButtonDelete)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //普通卡片view
    private lateinit var view : View
    //协程
    private val coroutineScope_LoadFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())






    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_play_list_custom_item, parent, false)

        return viewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.itemName.text = item.file_name.substringBeforeLast(".")
        holder.itemArtist.text = if (item.media_artist == "<unknown>" || item.media_artist == "") { "未知艺术家" } else { item.media_artist }
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutineScope_LoadFrame.launch { setHolderFrame(item, holder) }
        //点击事件设定
        holder.ButtonDelete.setOnClickListener { onDeleteClick(item.media_api_NUM_ID) }
        holder.ButtonPlay.setOnClickListener { onPlayClick(item.content_uriStandard.toUri()) }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onViewAttachedToWindow(holder: viewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
    }

    override fun onViewDetachedFromWindow(holder: viewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
    }





    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForList, holder: viewHolder) {
        //设置文件
        var covers_path = File(context.filesDir, "miniature/music_cover")
        if (item.media_SPECIFIC_MediaType == "video"){
            covers_path = File(context.filesDir, "miniature/video_cover")
        }else if(item.media_SPECIFIC_MediaType == "music"){
            covers_path = File(context.filesDir, "miniature/music_cover")
        }
        val cover_item_file = File(covers_path, "${item.media_api_NUM_ID}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main) { holder.itemFrame.setImageBitmap(frame) }
        }
    }

//adapter END
}
