package com.suming.player.ActivityComponent.MainActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
import com.suming.player.FuncionalPack.Animations
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
class RecyclerAdapterVideo(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onClick_Duration: (MediaItemFullForVideo) -> Unit,
    private val onClick_tvFormat: (MediaItemFullForVideo) -> Unit,
    private val onClick_Options: (MediaItemFullForVideo, ViewHolder) -> Unit,
): PagingDataAdapter<MediaItemFullForVideo, RecyclerAdapterVideo.ViewHolder>(diffCallback) {
    companion object {
        //比较器
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemFullForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemFullForVideo, newItem: MediaItemFullForVideo): Boolean {
                return oldItem.media_api_SPECIFIC_ID == newItem.media_api_SPECIFIC_ID
            }
            override fun areContentsTheSame(oldItem: MediaItemFullForVideo, newItem: MediaItemFullForVideo): Boolean {
                return oldItem == newItem
            }
        }
    }

    //viewHolder 1 - 普通视频项卡片
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //基本视图
        val touchLayer: View = itemView.findViewById(R.id.TouchPad)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvFormat: TextView = itemView.findViewById(R.id.tvFormat)
        val tvFrame: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvOption: CardView = itemView.findViewById(R.id.options)
        //可控制任务
        var tvFrameLoadingJob: Job? = null
        //标识
        var isAnimShowed: Boolean = false   //是否显示过动画
    }
    //协程
    private val coroutine_capArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork_in = CoroutineScope(Dispatchers.IO + SupervisorJob())


    //init(未使用)
    /*
    init {
    }
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_list_item_video, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        bindBasicVideoCard(holder, position)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        /*
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

         */

    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        holder.tvFrameLoadingJob?.cancel()

    }



    //Functions
    //绑定基本视频卡片
    private fun bindBasicVideoCard(holder: ViewHolder, position: Int){
        val item = getItem(position) ?: return
        //填充基本信息
        holder.tvName.text = item.file_name.substringBeforeLast(".")
        holder.tvDuration.text = FormatTime_numOnly(item.media_durationMs)
        holder.tvFormat.text = item.media_format.ifEmpty { "未知" }
        //加载艺术图
        holder.tvFrameLoadingJob?.cancel()
        holder.tvFrameLoadingJob = coroutine_loadArtwork.launch {
            loadArtworkFrame(item, holder)
        }
        //点击事件设定
        holder.touchLayer.setOnClickListener {
            ToolVibrate().vibrate(context)

            onClickFunc_touchLayer(item)
        }
        holder.tvDuration.setOnClickListener {
            ToolVibrate().vibrate(context)

            onClickFunc_tvDuration(item)
        }
        holder.tvOption.setOnClickListener {
            ToolVibrate().vibrate(context)
            //显示菜单
            onClickFunc_tvOption(item,holder)
        }
        holder.tvFormat.setOnClickListener {
            ToolVibrate().vibrate(context)
            //
            onClickFunc_tvFormat(item)
        }
    }

    //点击事件
    private fun onClickFunc_touchLayer(item: MediaItemFullForVideo){
        onItemClick(item.content_uriString.toUri())
    }
    private fun onClickFunc_tvDuration(item: MediaItemFullForVideo){
        onClick_Duration(item)
    }
    private fun onClickFunc_tvOption(item: MediaItemFullForVideo, holder: ViewHolder){
        onClick_Options(item,holder)
    }
    private fun onClickFunc_tvFormat(item: MediaItemFullForVideo){
        onClick_tvFormat(item)
    }


    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemFullForVideo, holder: ViewHolder)  {
        //记录holder的tag
        val imageTag = item.media_api_NUM_ID
        holder.tvFrame.tag = imageTag
        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch(Dispatchers.IO){
            //从ArtworkFrameManager要图片
            val Frame = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Video, item.media_api_NUM_ID)
            //检查图片是否有效
            if (Frame != null){
                //推送到图片ImageView
                if (holder.tvFrame.tag == imageTag) {
                    withContext(Dispatchers.Main){ submitToImageView(holder,Frame) }
                }else{ Frame.recycle() }

            }else{
                //截取图片
                capArtworkFrame(item, holder)
            }
        }
    }

    //截取缩略图
    private fun capArtworkFrame(item: MediaItemFullForVideo, holder: ViewHolder){
        coroutine_capArtwork.launch(Dispatchers.IO){
            //截取图片(让ArtworkCapturer承担截图任务)
            val Bitmap = ArtworkCapturer.captureFrameInVideo(
                context = context,
                uri = item.content_uriString.toUri(),
                videoDurationUs = item.media_durationMs * 1_000L,
                timeUs = 0L,
                option = ArtworkCapturer.OPTION_CLOSEST_SYNC,
                needCheckDark = true,
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                consoleLog("截取视频封面失败: file_name=${item.file_name}")
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
    private fun submitToImageView(holder: ViewHolder, Bitmap : Bitmap){
        holder.tvFrame.setImageBitmap(Bitmap)
        if (!holder.isAnimShowed){
            holder.tvFrame.startAnimation(Animations.FadeIn)
            holder.isAnimShowed = true
        }
    }
    private fun submitToImageViewNoAnim(holder: ViewHolder, Bitmap : Bitmap){
        holder.tvFrame.setImageBitmap(Bitmap)
    }



    //外部控制函数
    //更新指定位置的封面
    fun updateCoverForItem(targetFilePath: String, mediaId: Long)  {
        //拿新图
        val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Video, mediaId)
        //检查是否取图成功
        if (Bitmap == null){
            consoleLog("刷新新视频封面失败: targetFilePath: ${targetFilePath}, mediaId: $mediaId")
            return
        }

        //遍历列表并换图
        snapshot().forEachIndexed { index, mediaItem ->
            if (mediaItem?.file_path == targetFilePath) {
                notifyItemChanged(index)
            }
        }
    }





    //工具函数
    //格式化时间
    @SuppressLint("DefaultLocale")
    private fun FormatTime_numOnly(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        //不显示时
        return if (hours == 0L){
            String.format("%02d:%02d",  minutes, seconds)
        }
        //显示时
        else{
            String.format("%02d:%02d:%02d",  hours, minutes, seconds)
        }
    }
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterVideo: $msg")
        }
    }

}