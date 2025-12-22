package com.suming.player

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
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlayerFragmentPlayListVideoAdapter(
    private val context: Context,
    private val onAddToListClick: (String) -> Unit,
    private val onPlayClick: (String) -> Unit
):PagingDataAdapter<MediaItemForVideo, PlayerFragmentPlayListVideoAdapter.ViewHolder>(diffCallback) {
    //条目比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }

            override fun areContentsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemTouchPad: View = itemView.findViewById(R.id.TouchPad)
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        var itemFrameJob: Job? = null
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //协程作用域
    private val coroutineScope_LoadFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val covers_path = File(context.filesDir, "miniature/video_cover")



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_fragment_play_list_adapter_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.itemName.isSelected = true
        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = "未知艺术家"
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutineScope_LoadFrame.launch { setHolderFrame(item, holder) }
        //点击事件设定
        holder.ButtonAddToList.setOnClickListener { onAddToListClick(item.uriString) }
        holder.ButtonPlay.setOnClickListener { onPlayClick(item.uriString) }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        holder.itemFrameJob?.cancel()
    }



    //Functions
    //暂无
    //内部Functions
    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForVideo, holder: ViewHolder) {
        val imageTag = item.uriNumOnly.toString()
        //记录holder的tag
        withContext(Dispatchers.Main) {
            holder.itemFrame.tag = imageTag
        }
        //设置文件
        val cover_item_file = File(covers_path, "${item.uriNumOnly}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main){
                if (holder.itemFrame.tag == imageTag) {
                    holder.itemFrame.setImageBitmap(frame)
                    //holder.itemFrame.startAnimation(FadeInAnimation)
                } else {
                    frame?.recycle()
                }
            }
        }
        //不存在,生成图片
        else{
            //generateCoverFrame(item, holder)
        }

    }




}