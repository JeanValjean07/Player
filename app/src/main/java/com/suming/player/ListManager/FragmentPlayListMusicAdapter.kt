package com.suming.player.ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import data.MediaModel.MediaItemForMusic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("unused")
class FragmentPlayListMusicAdapter(
    context: Context,
    private val onAddToListClick: (String) -> Unit,
    private val onPlayClick: (String) -> Unit
): PagingDataAdapter<MediaItemForMusic, FragmentPlayListMusicAdapter.viewHolder>(diffCallback) {
    companion object {
        //比较器
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForMusic>() {
            override fun areItemsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }
            override fun areContentsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean {
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
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //协程作用域
    private val coroutineScope_LoadFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val covers_path = File(context.filesDir, "miniature/music_cover")



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_fragment_play_list_live_adapter_item, parent, false)
        return viewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = if (item.artist == "<unknown>" || item.artist == "") { "未知艺术家" } else { item.artist }
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutineScope_LoadFrame.launch { setHolderFrame(item, holder) }
        //点击事件设定
        holder.ButtonAddToList.setOnClickListener { onAddToListClick(item.uriString) }
        holder.ButtonPlay.setOnClickListener { onPlayClick(item.uriString) }
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
        holder.itemFrameJob?.cancel()
    }



    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForMusic, holder: viewHolder) {
        val imageTag = item.filename.hashCode().toString()
        //记录holder的tag
        withContext(Dispatchers.Main) {
            holder.itemFrame.tag = imageTag
        }
        //设置文件
        val cover_item_file = File(covers_path, "${item.uriNumOnly}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main) {
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


//adapter END
}