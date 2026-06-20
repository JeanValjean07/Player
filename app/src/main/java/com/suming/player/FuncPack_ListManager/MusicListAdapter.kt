package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterMusic.ViewHolder
import com.suming.player.R
import com.suming.player.DataPack.MediaModel.MediaItemForMusic
import com.suming.player.FuncionalPack.ArtworkFrameManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("unused")
class MusicListAdapter(
    private val context: Context,
    private val onAddToListClick: (String) -> Unit,
    private val onPlayClick: (String) -> Unit
): PagingDataAdapter<MediaItemForMusic, MusicListAdapter.viewHolder>(diffCallback) {
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
        if (position == RecyclerView.NO_POSITION) return
    }

    override fun onViewDetachedFromWindow(holder: viewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        holder.itemFrameJob?.cancel()
    }



    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemForMusic, holder: viewHolder)   {
        //记录holder的tag
        val imageTag = item.uriNumOnly.hashCode().toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch {
            val Bitmap = ArtworkFrameManager.get_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_audio, item.uriNumOnly)
            if (Bitmap != null){
                //推到ImageView
                withContext(Dispatchers.Main) {
                    if (holder.itemFrame.tag == imageTag) {
                        submitToImageView(holder,Bitmap)
                    }else{ Bitmap.recycle() }
                }
            }
        }
    }

    //推送到ImageView
    private fun submitToImageView(holder: viewHolder,Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)

    }





}