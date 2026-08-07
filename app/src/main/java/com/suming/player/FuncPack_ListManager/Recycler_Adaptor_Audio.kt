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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.R
import com.suming.player.DataPack.MediaModel.MediaItemForMusic
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("NewApi,unused")
class Recycler_Adaptor_Audio(
    private val context: Context,
    private val onAddToListClick: (MediaItemForMusic) -> Unit,
    private val onPlayClick: (MediaItemForMusic) -> Unit
): PagingDataAdapter<MediaItemForMusic, Recycler_Adaptor_Audio.viewHolder>(diffCallback) {
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
        val item_root_card: CardView = itemView.findViewById(R.id.item_root_card)

        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)

        var itemFrameJob: Job? = null

        //是否正在播放
        fun setItemPlayingCard(isItemOn: Boolean){
            //写入状态
            itemName.isSelected = isItemOn
            //修改背景颜色
            item_root_card.backgroundTintList = if (isItemOn){
                ContextCompat.getColorStateList(itemView.context, R.color.SecondaryColorPack_ContentCardBackground_Theme)
            }else{
                ContextCompat.getColorStateList(itemView.context, R.color.SecondaryColorPack_ContentCardBackground)
            }
            //把按钮一并至于暂停状态
            if (!isItemOn){
                setItemPlayingButton(false)
            }


        }
        fun setItemPlayingButton(isPlaying: Boolean){
            ButtonPlay.setImageResource(if (isPlaying) R.drawable.ic_frag_list_item_pause else R.drawable.ic_frag_list_item_play)
        }
    }
    //协程
    private val coroutine_loadArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork_in = CoroutineScope(Dispatchers.IO + SupervisorJob())



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_play_list_audio_item, parent, false)
        return viewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int)  {
        val item = getItem(position) ?: return

        //检查是不是当前媒体
        if (item.uriString == PlayerInfoCenter.getMediaUriString()){
            holder.setItemPlayingCard(true)
            currentItemUri = item.uriString
            //检查并设置播放状态
            holder.setItemPlayingButton(PlayerInfoCenter.isPlaying.value)
        }else{
            holder.setItemPlayingCard(false)
        }

        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = if (item.artist == "<unknown>" || item.artist == "") { "未知艺术家" } else { item.artist }
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutine_loadArtwork.launch {
            loadArtworkFrame(item, holder)
        }
        //点击事件设定
        holder.ButtonAddToList.setOnClickListener {
            ToolVibrate().vibrate(context)

            onAddToListClick(item)
        }
        holder.ButtonPlay.setOnClickListener {
            ToolVibrate().vibrate(context)

            onPlayClick(item)
        }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int, payloads: List<Any>){
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            when (payloads.firstOrNull()) {
                ListManagerHelper.payload_event_item_update -> {
                    if (item?.uriString == PlayerInfoCenter.getMediaUriString()){
                        holder.setItemPlayingCard(true)
                    }else{
                        holder.setItemPlayingCard(false)
                    }
                }
                ListManagerHelper.payload_event_item_state_update -> {
                    holder.setItemPlayingButton(PlayerInfoCenter.isPlaying.value)
                }
                ListManagerHelper.payload_event_item_clear_playing_mark -> {
                    holder.setItemPlayingCard(false)
                    holder.setItemPlayingButton(false)
                }
            }
        }else{
            super.onBindViewHolder(holder, position, payloads)
        }
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
            val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Audio, item.uriNumOnly)
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


    //外部控制
    //重置可见项
    fun refreshVisibleItems(layoutManager: LinearLayoutManager) {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        //刷新所有可见项
        if (firstVisible >= 0 && lastVisible >= 0) {
            notifyItemRangeChanged(firstVisible, lastVisible - firstVisible + 1)
        }
    }
    //切换当前播放项指示器
    private var currentItemUri = ""
    fun updateCurrentMediaItem(targetItemUri: String, payloads: Any){
        if (targetItemUri == currentItemUri) return

        val cache = currentItemUri

        currentItemUri = targetItemUri

        snapshot().forEachIndexed { index, item ->
            if (item?.uriString == targetItemUri || item?.uriString == cache) {

                notifyItemChanged(index, payloads)
            }
        }

    }
    //切换当前播放状态
    fun updateCurrentIsPlayingState(targetItemUri: String,newIsPlaying: Boolean, payloads: Any){
        snapshot().forEachIndexed { index, item ->
            if (item?.uriString == targetItemUri) {

                notifyItemChanged(index, payloads)
            }
        }
    }
    //清理播放标记
    fun clearPlayingItem(payloads: Any){
        //consoleLog("清理播放标记 clearPlayingItem")
        snapshot().forEachIndexed { index, item ->
            if (item?.uriString == currentItemUri){
                notifyItemChanged(index, payloads)
            }
        }
        currentItemUri = ""
    }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterAudio: $msg")
        }
    }


}