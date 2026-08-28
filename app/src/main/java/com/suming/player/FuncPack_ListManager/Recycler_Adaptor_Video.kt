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
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("NewApi")
class Recycler_Adaptor_Video(
    private val context: Context,
    private val onAddToListClick: (MediaItemFullForVideo) -> Unit,
    private val onPlayClick: (MediaItemFullForVideo, Int) -> Unit
):PagingDataAdapter<MediaItemFullForVideo, Recycler_Adaptor_Video.viewHolder>(Differ) {
    companion object {
        //比较器
        val Differ = object : DiffUtil.ItemCallback<MediaItemFullForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemFullForVideo, newItem: MediaItemFullForVideo): Boolean {
                return oldItem.media_api_NUM_ID == newItem.media_api_NUM_ID
            }
            override fun areContentsTheSame(oldItem: MediaItemFullForVideo, newItem: MediaItemFullForVideo): Boolean {
                return oldItem == newItem
            }
            override fun getChangePayload(oldItem: MediaItemFullForVideo, newItem: MediaItemFullForVideo): Any? {

                return null
            }
        }
        //viewType
        const val item_NORMAL = 0
        const val item_NORMAL_ONGOING = 1
        const val item_footer = 2

    }


    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val item_root_card: CardView = itemView.findViewById(R.id.item_root_card)

        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)

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



    override fun getItemViewType(position: Int): Int {
        return when (position) {
            item_footer -> item_footer
            else -> item_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_play_list_video_item, parent, false)

        return when (viewType) {
            item_NORMAL -> viewHolder(view)
            else -> viewHolder(view)
        }
    }
    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        val item = getItem(position) ?: return

        //检查是不是当前媒体
        if (item.content_uriString == PlayerInfoCenter.GET_Media_URI_S_FP()){
            holder.setItemPlayingCard(true)
            currentItemUri = item.content_uriString
            //检查并设置播放状态
            holder.setItemPlayingButton(PlayerInfoCenter.observableIsPlaying.value)
        }else{
            holder.setItemPlayingCard(false)
        }

        holder.itemName.text = item.file_name.substringBeforeLast(".")
        holder.itemArtist.text = "未知艺术家"
        //取图任务
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

            onPlayClick(item, position)
        }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int, payloads: List<Any>){
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            consoleLog("payloads.firstOrNull() = ${payloads.firstOrNull()}")
            when (payloads.firstOrNull()) {

                ListManagerHelper.payload_event_item_update -> {
                    if (item?.content_uriString == PlayerInfoCenter.GET_Media_URI_S_FP()){
                        holder.setItemPlayingCard(true)
                    }else{
                        holder.setItemPlayingCard(false)
                    }
                }
                ListManagerHelper.payload_event_item_state_update -> {
                    holder.setItemPlayingButton(PlayerInfoCenter.observableIsPlaying.value)
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
        val item = getItem(position) ?: return

    }

    override fun onViewDetachedFromWindow(holder: viewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        holder.itemFrameJob?.cancel()
    }




    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemFullForVideo, holder: viewHolder)  {
        //记录holder的tag
        val imageTag = item.media_api_NUM_ID.toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch(Dispatchers.IO){
            //从ArtworkFrameManager要图片
            val Frame = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Video, item.media_api_NUM_ID)
            //检查图片是否有效
            if (Frame != null){
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

        if (targetItemUri == "") {
            clearPlayingItem(ListManagerHelper.payload_event_item_clear_playing_mark)
        }

        val cache = currentItemUri

        currentItemUri = targetItemUri

        snapshot().forEachIndexed { index, item ->
            if (item?.content_uriString == targetItemUri || item?.content_uriString == cache) {

                notifyItemChanged(index, payloads)
            }
        }

    }
    //切换当前播放状态
    fun updateCurrentIsPlayingState(targetItemUri: String,newIsPlaying: Boolean, payloads: Any){
        snapshot().forEachIndexed { index, item ->
            if (item?.content_uriString == targetItemUri) {

                notifyItemChanged(index, payloads)
            }
        }
    }
    //清理播放标记
    fun clearPlayingItem(payloads: Any){
        consoleLog("清理播放标记 clearPlayingItem")
        snapshot().forEachIndexed { index, item ->
            if (item?.content_uriString == currentItemUri){
                notifyItemChanged(index, payloads)
            }
        }
        currentItemUri = ""
    }



    //日志
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterVideo: $msg")
        }
    }


}