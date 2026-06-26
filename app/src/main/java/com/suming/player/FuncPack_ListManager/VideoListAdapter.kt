package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.FuncionalPack.ArtworkFrameManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("unused")
class VideoListAdapter(
    private val context: Context,
    private val onAddToListClick: (String) -> Unit,
    private val onPlayClick: (String) -> Unit
):PagingDataAdapter<MediaItemForVideo, VideoListAdapter.viewHolder>(Differ) {
    companion object {
        //比较器
        val Differ = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }
            override fun areContentsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem == newItem
            }
        }
        //viewType
        const val item_NORMAL = 0
        const val item_NORMAL_ONGOING = 1
        const val item_footer = 2

    }


    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        var itemFrameJob: Job? = null
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //协程
    private val coroutine_loadArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork_in = CoroutineScope(Dispatchers.IO + SupervisorJob())



    override fun getItemViewType(position: Int): Int {
        return when (position) {
            item_footer -> item_footer
            else -> item_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_play_list_live_item, parent, false)

        return when (viewType) {
            item_NORMAL -> viewHolder(view)
            else -> viewHolder(view)
        }
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int){
        val item = getItem(position) ?: return
        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = "未知艺术家"
        //取图任务
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutine_loadArtwork.launch {
            loadArtworkFrame(item, holder)
        }
        //点击事件设定
        holder.ButtonAddToList.setOnClickListener { onAddToListClick(item.uriString) }
        holder.ButtonPlay.setOnClickListener { onPlayClick(item.uriString) }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onViewAttachedToWindow(holder: viewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

    }

    override fun onViewDetachedFromWindow(holder: viewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        holder.itemFrameJob?.cancel()
    }




    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemForVideo, holder: viewHolder)  {
        //记录holder的tag
        val imageTag = item.uriNumOnly.toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch(Dispatchers.IO){
            //从ArtworkFrameManager要图片
            val Frame = ArtworkFrameManager.get_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_video, item.uriNumOnly)
            //检查图片是否有效
            if (Frame != null){
                consoleLog("RecyclerAdapterVideo: 加载图片成功, 位置：${item.uriNumOnly},名称：${item.filename}")
                //推送到图片ImageView
                if (holder.itemFrame.tag == imageTag) {
                    withContext(Dispatchers.Main){
                        submitToImageView(holder,Frame)
                    }
                }else{ Frame.recycle() }

            }

        }

    }


    //推送到ImageView
    private fun submitToImageView(holder: viewHolder, Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)
    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterVideo: $msg")
        }
    }


}