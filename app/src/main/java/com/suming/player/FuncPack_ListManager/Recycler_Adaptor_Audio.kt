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
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterMusic.ViewHolder
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.R
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForAudio
import com.suming.player.FuncionalPack.ArtworkCapturer
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
    private val onAddToListClick: (MediaItemFullForAudio) -> Unit,
    private val onPlayItemClick: (MediaItemFullForAudio) -> Unit
): PagingDataAdapter<MediaItemFullForAudio, Recycler_Adaptor_Audio.viewHolder>(diffCallback) {
    companion object {
        //比较器
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemFullForAudio>() {
            override fun areItemsTheSame(oldItem: MediaItemFullForAudio, newItem: MediaItemFullForAudio): Boolean {
                return oldItem.media_api_NUM_ID == newItem.media_api_NUM_ID
            }
            override fun areContentsTheSame(oldItem: MediaItemFullForAudio, newItem: MediaItemFullForAudio): Boolean {
                return oldItem == newItem
            }
        }


    }

    //空字段
    private val Undefined = ""

    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val item_root_card: CardView = itemView.findViewById(R.id.item_root_card)

        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
        var itemFrameJob: Job? = null

        //基础控制函数(最基础:仅控制自己这点,不联动分毫)
        fun set_item_ongoing(isItemOn: Boolean){
            //写入状态
            itemName.isSelected = isItemOn
            //修改背景颜色
            item_root_card.backgroundTintList = if (isItemOn){
                ContextCompat.getColorStateList(itemView.context, R.color.SecondaryColorPack_ContentCardBackground_Theme)
            }else{
                ContextCompat.getColorStateList(itemView.context, R.color.SecondaryColorPack_ContentCardBackground)
            }

        }  //设置进行中状态(是否选中)
        fun set_item_is_playing(isPlaying: Boolean){
            ButtonPlay.setImageResource(if (isPlaying) R.drawable.ic_frag_list_item_pause else R.drawable.ic_frag_list_item_play)
        }  //设置播放状态按钮(播放/暂停)

        //完全未在播放的项:取消主题色,按钮置于继续
        fun disable_this_item(){
            set_item_ongoing(false)
            set_item_is_playing(false)
        }
        //进行中,且正在播放的项:保持主题色,按钮置于暂停
        fun enable_this_item_with_play(){
            set_item_ongoing(true)
            set_item_is_playing(true)
        }
        //进行中,且未播放的项:保持主题色,按钮置于继续
        fun enable_this_item_without_play(){
            set_item_ongoing(true)
            set_item_is_playing(false)
        }
    }
    //协程
    private val coroutine_captureAlbum = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork_in = CoroutineScope(Dispatchers.IO + SupervisorJob())



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_play_list_audio_item, parent, false)
        return viewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: viewHolder, position: Int)  {
        val item = getItem(position) ?: return

        //卡片颜色与按钮状态
        if (item.URI_S_FP == cache_URI_S_FP){
            //是进行中媒体,检查是否正在播放
            if (cache_isPlaying){
                holder.enable_this_item_with_play()
            }else{
                holder.enable_this_item_without_play()
            }
        }else{
            //不是当前进行中的媒体项,设为disabled
            holder.disable_this_item()
        }

        holder.itemName.text = item.file_name.substringBeforeLast(".")
        holder.itemArtist.text = if (item.media_artist == "<unknown>" || item.media_artist == "") { "未知艺术家" } else { item.media_artist }
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

            onPlayItemClick(item)
        }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int, payloads: List<Any>){
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            when (payloads.firstOrNull()) {
                //不是进行中的项,设为disabled
                ListManagerHelper.payload_event_disable_this_item -> {
                    holder.disable_this_item()
                    //consoleLog("onBindViewHolder (音频列表): $position 不是进行中的项,设为disabled")
                }
                //进行中,且正在播放
                ListManagerHelper.payload_event_enable_this_item_with_play -> {
                    holder.enable_this_item_with_play()
                    //consoleLog("onBindViewHolder (音频列表): $position 进行中,且正在播放")
                }
                //进行中,但未播放
                ListManagerHelper.payload_event_enable_this_item_without_play -> {
                    holder.enable_this_item_without_play()
                    //consoleLog("onBindViewHolder (音频列表): $position 进行中,但未播放")
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
    private fun loadArtworkFrame(item: MediaItemFullForAudio, holder: viewHolder)   {
        //记录holder的tag
        val imageTag = item.media_api_NUM_ID.toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch {
            val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Audio, item.media_api_NUM_ID)
            if (Bitmap != null){
                //推到ImageView
                withContext(Dispatchers.Main) {
                    if (holder.itemFrame.tag == imageTag) {
                        submitToImageView(holder,Bitmap)
                    }else{ Bitmap.recycle() }
                }
            }else{
                //获取专辑封面
                captureAlbumFrame(item, holder)
            }
        }
    }

    //生成缩略图
    private fun captureAlbumFrame(item: MediaItemFullForAudio, holder: viewHolder){
        coroutine_captureAlbum.launch {
            //获取专辑封面(让ArtworkCapturer承担截图任务)
            //consoleLog("captureAlbumFrame: ${item.file_name} ${item.content_uriString}")
            var Bitmap = ArtworkCapturer.captureAlbumInMusic(
                context = context,
                uri = item.URI_S_FP.toUri(),
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                Bitmap = ArtworkCapturer.getDefaultAlbumFrame(context)
            }

            if (Bitmap == null){
                return@launch
            }

            //推送到ImageView
            withContext(Dispatchers.Main) { submitToImageViewNoAnim(holder,Bitmap) }

            //保存图片
            ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap(context, MediaType.Audio, item.media_api_NUM_ID, Bitmap)

        }
    }


    //推送到ImageView
    private fun submitToImageView(holder: viewHolder,Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)

    }
    private fun submitToImageViewNoAnim(holder: viewHolder,Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)
    }



    //切换当前项状态
    fun update_ongoingMediaState(URI_S_FP: String,isPlaying: Boolean,recyclerView: RecyclerView){
        //写入缓存
        cache_URI_S_FP = URI_S_FP
        cache_isPlaying = isPlaying
        //使用post保证高频时不吞更新
        recyclerView.post {
            //获取当前可见范围
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()  + 1
            if (firstVisible == RecyclerView.NO_POSITION) return@post
            //遍历检查
            for (index in firstVisible..lastVisible) {
                //consoleLog("update_ongoingMediaState (音频列表): $index")
                val item = snapshot().getOrNull(index) ?: continue
                //检查这一项是否是进行中的项
                if (item.URI_S_FP == cache_URI_S_FP && cache_URI_S_FP != Undefined) {
                    //是进行中的项,那么他是否在播放中?
                    if (cache_isPlaying){
                        //进行中,且正在播放
                        notifyItemChanged(index, ListManagerHelper.payload_event_enable_this_item_with_play)
                        //consoleLog("update_ongoingMediaState (音频列表): $index 进行中,且正在播放")
                    }else {
                        //进行中,但未播放
                        notifyItemChanged(index, ListManagerHelper.payload_event_enable_this_item_without_play)
                        //consoleLog("update_ongoingMediaState (音频列表): $index 进行中,但未播放")
                    }
                }else{
                    //不是进行中的项,设为disabled
                    notifyItemChanged(index, ListManagerHelper.payload_event_disable_this_item)
                    //consoleLog("update_ongoingMediaState (音频列表): $index 不是进行中的项,设为disabled")

                }
            }
        }
    }
    //缓存下当前 URI_S_FP
    private var cache_URI_S_FP = Undefined
    //缓存下当前 isPlaying
    private var cache_isPlaying = false


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterAudio: $msg")
        }
    }


}