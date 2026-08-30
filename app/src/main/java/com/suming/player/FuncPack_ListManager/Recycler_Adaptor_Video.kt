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
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.R
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
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

@Suppress("NewApi")
class Recycler_Adaptor_Video(
    private val context: Context,
    private val onAddToListClick: (MediaItemFullForVideo) -> Unit,
    private val onPlayItemClick: (MediaItemFullForVideo) -> Unit
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

    //空字段
    private val Undefined = ""

    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val item_root_card: CardView = itemView.findViewById(R.id.item_root_card)

        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonAddToList: ImageView = itemView.findViewById(R.id.ButtonAddToList)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
        val itemFrame: ImageView = itemView.findViewById(R.id.tvThumb)
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
    private val coroutine_capArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
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

            onPlayItemClick(item)
        }
        holder.itemName.setOnClickListener { holder.itemName.isSelected = true }
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int, payloads: List<Any>){
        if (payloads.isNotEmpty()){
            val item = getItem(position)
            when (payloads.firstOrNull()) {
                //不是进行中的项,设为disabled
                ListManagerHelper.payload_event_disable_this_item -> {
                    holder.disable_this_item()
                }
                //进行中,且正在播放
                ListManagerHelper.payload_event_enable_this_item_with_play -> {
                    holder.enable_this_item_with_play()
                }
                //进行中,但未播放
                ListManagerHelper.payload_event_enable_this_item_without_play -> {
                    holder.enable_this_item_without_play()
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
    private fun loadArtworkFrame(item: MediaItemFullForVideo, holder: viewHolder) {
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

            }else{
                //截取图片
                capArtworkFrame(item, holder)
            }

        }

    }

    //截取缩略图
    private fun capArtworkFrame(item: MediaItemFullForVideo, holder: viewHolder){
        coroutine_capArtwork.launch(Dispatchers.IO){
            //截取图片(让ArtworkCapturer承担截图任务)
            val Bitmap = ArtworkCapturer.captureFrameInVideo(
                context = context,
                uri = item.URI_S_FP.toUri(),
                videoDurationUs = item.media_durationMs * 1_000L,
                timeUs = 0L,
                option = ArtworkCapturer.OPTION_CLOSEST_SYNC,
                needCheckDark = true,
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                //consoleLog("截取视频封面失败: file_name=${item.file_name}")
                return@launch
            }else{
                //consoleLog("截取视频封面成功: file_name=${item.file_name}")
            }

            //推送到ImageView
            withContext(Dispatchers.Main) { submitToImageViewNoAnim(holder,Bitmap) }

            //保存图片
            ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap(context, MediaType.Video, item.media_api_NUM_ID, Bitmap)

        }
    }

    //推送到ImageView
    private fun submitToImageView(holder: viewHolder, Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)
    }
    private fun submitToImageViewNoAnim(holder: viewHolder, Bitmap : Bitmap){
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
            val lastVisible = layoutManager.findLastVisibleItemPosition() + 1
            if (firstVisible == RecyclerView.NO_POSITION) return@post
            //遍历检查
            for (index in firstVisible..lastVisible) {
                //consoleLog("update_ongoingMediaState (视频列表): $index")
                val item = snapshot().getOrNull(index) ?: continue
                //检查这一项是否是进行中的项
                if (item.URI_S_FP == URI_S_FP && cache_URI_S_FP != Undefined) {
                    //是进行中的项,那么他是否在播放中?
                    if (isPlaying){
                        //进行中,且正在播放
                        notifyItemChanged(index, ListManagerHelper.payload_event_enable_this_item_with_play)
                    }else {
                        //进行中,但未播放
                        notifyItemChanged(index, ListManagerHelper.payload_event_enable_this_item_without_play)
                    }
                }else{
                    //不是进行中的项,设为disabled
                    notifyItemChanged(index, ListManagerHelper.payload_event_disable_this_item)

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
            Log.d("SuMing", "RecyclerAdapterVideo: $msg")
        }
    }


}