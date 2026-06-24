package com.suming.player.ActivityComponent.MainActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.FuncionalPack.ArtworkCapturer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

//@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
class RecyclerAdapterVideo(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onDurationClick: (MediaItemForVideo) -> Unit,
    private val onOptionClick: (MediaItemForVideo) -> Unit,
    private val onFormatClick: (MediaItemForVideo, String) -> Unit,
    private val onSmallCardPlay: (String, String) -> Unit,
): PagingDataAdapter<MediaItemForVideo, RecyclerAdapterVideo.ViewHolder>(diffCallback) {
    companion object {
        //比较器
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }

            override fun areContentsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem == newItem
            }
        }
    }


    //viewHolder 1 - 普通视频项卡片
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //基本视图
        val TouchPad: View = itemView.findViewById(R.id.TouchPad)
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



    init {
         consoleLog("init: 114514")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_list_item_video, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        bindBasicVideoCard(holder, position)
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
        holder.tvFrameLoadingJob?.cancel()

    }



    //Functions
    //绑定基本视频卡片
    private fun bindBasicVideoCard(holder: ViewHolder, position: Int){
        consoleLog("bindBasicVideoCard: $position")
        val item = getItem(position) ?: return
        //填充基本信息
        holder.tvName.text = item.filename.substringBeforeLast(".")
        holder.tvDuration.text = FormatTime_numOnly(item.durationMs)
        holder.tvFormat.text = item.format.ifEmpty { "未知" }
        //加载艺术图
        holder.tvFrameLoadingJob?.cancel()
        holder.tvFrameLoadingJob = coroutine_loadArtwork.launch {
            loadArtworkFrame(item, holder)
        }
        //点击事件设定
        holder.TouchPad.setOnClickListener {
            ToolVibrate().vibrate(context)
            //
            onItemClick(item.uriString.toUri())
        }
        holder.tvDuration.setOnClickListener {
            ToolVibrate().vibrate(context)
            //
            onDurationClick(item)
        }
        holder.tvOption.setOnClickListener {
            ToolVibrate().vibrate(context)
            //显示菜单
            val popup = PopupMenu(holder.itemView.context, holder.tvOption)
            popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
            val popup_update_cover = popup.menu.findItem(R.id.MenuAction_Repic)
            val popup_hide_item = popup.menu.findItem(R.id.MenuAction_Hide)
            val popup_onSmallCardPlay = popup.menu.findItem(R.id.MenuAction_onSmallCardPlay)
            popup.show()
            //注册点击
            popup_update_cover.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                context.showCustomToast("进入视频后,可在更多选项面板更新封面", 3)
                true
            }
            popup_hide_item.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                context.showCustomToast("已停止对隐藏视频功能的支持", 3)
                true
            }
            popup_onSmallCardPlay.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onSmallCardPlay(item.uriString, item.filename)
                true
            }
        }
        holder.tvFormat.setOnClickListener {
            ToolVibrate().vibrate(context)
            //
            onFormatClick(item, item.format)
        }
    }


    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemForVideo, holder: ViewHolder)  {
        //记录holder的tag
        val imageTag = item.uriNumOnly.toString()
        holder.tvFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch(Dispatchers.IO){
            //从ArtworkFrameManager要图片
            val Frame = ArtworkFrameManager.get_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_video, item.uriNumOnly)
            //检查图片是否有效
            if (Frame != null){
                consoleLog("RecyclerAdapterVideo: 加载图片成功, 位置：${item.uriNumOnly},名称：${item.filename}")
                //推送到图片ImageView
                if (holder.tvFrame.tag == imageTag) {
                    withContext(Dispatchers.Main){
                        submitToImageView(holder,Frame)
                    }
                }else{ Frame.recycle() }

            }
            //不存在该位置图片时立即截取
            else{
                consoleLog("RecyclerAdapterVideo: 加载图片失败, 开始截取图片, 位置：${item.uriNumOnly},名称：${item.filename}")
                //截取图片
                capArtworkFrame(item, holder)
            }
        }

    }

    //加载动画
    private var FadeInAnimation: AlphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }


    //截取缩略图
    private fun capArtworkFrame(item: MediaItemForVideo, holder: ViewHolder){
        coroutine_capArtwork.launch(Dispatchers.IO){
            //截取图片(让ArtworkCapturer承担截图任务)
            val Bitmap = ArtworkCapturer.captureFrameInVideo(
                context = context,
                uri = item.uriString.toUri(),
                videoDurationUs = item.durationMs * 1_000L,
                timeUs = 0L,
                option = ArtworkCapturer.OPTION_CLOSEST_SYNC,
                needCheckDark = true,
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                consoleLog("截取视频封面失败: uriNumOnly=${item.uriNumOnly}")
                return@launch
            }else{
                consoleLog("截取视频封面成功: uriNumOnly=${item.uriNumOnly}")
            }

            //推送到ImageView
            withContext(Dispatchers.Main) {
                submitToImageView(holder,Bitmap)
            }

            //保存图片(让ArtworkFrameManager承担保存任务)
            ArtworkFrameManager.save_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_video, item.uriNumOnly, Bitmap)

        }
    }

    //推送到ImageView
    private fun submitToImageView(holder: ViewHolder, Bitmap : Bitmap){
        holder.tvFrame.setImageBitmap(Bitmap)
        if (!holder.isAnimShowed){
            holder.tvFrame.startAnimation(FadeInAnimation)
            holder.isAnimShowed = true
        }
    }


    //📐外部控制函数 更新指定位置的封面
    fun updateCoverForVideo(uriNumOnly: Long)  {
        //拿新图
        val Bitmap = ArtworkFrameManager.get_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_video, uriNumOnly)
        //检查是否取图成功
        if (Bitmap == null){
            consoleLog("刷新新视频封面失败: uriNumOnly=${uriNumOnly}")
            return
        }

        //遍历列表并换图
        snapshot().forEachIndexed { index, mediaItem ->
            if (mediaItem?.uriNumOnly == uriNumOnly) {
                notifyItemChanged(index)
            }
        }
    }

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


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterVideo: $msg")
        }
    }

}