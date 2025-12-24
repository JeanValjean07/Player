package com.suming.player

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.net.toUri

class MainVideoAdapter(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onDurationClick: (MediaItemForVideo) -> Unit,
    private val onOptionClick: (MediaItemForVideo) -> Unit,
    private val onFormatClick: (MediaItemForVideo, String) -> Unit,
    private val onSmallCardPlay: (String, String) -> Unit,
):PagingDataAdapter<MediaItemForVideo, MainVideoAdapter.ViewHolder>(diffCallback) {
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
        val TouchPad: View = itemView.findViewById(R.id.TouchPad)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvFormat: TextView = itemView.findViewById(R.id.tvFormat)
        val tvFrame: ImageView = itemView.findViewById(R.id.ivThumb)
        var tvFrameLoadingJob: Job? = null
        val tvOption: CardView = itemView.findViewById(R.id.options)
    }
    //协程作用域
    private val CoroutineScope_GenerateCover = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CoroutineScope_LoadCoverFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //图片池和加载动画
    private var FadeInAnimation: AlphaAnimation = AlphaAnimation(0.0f, 1.0f)
    private val covers_path = File(context.filesDir, "miniature/video_cover")



    init {
        FadeInAnimation.duration = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_adapter_video_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.tvName.text = item.filename.substringBeforeLast(".")
        holder.tvDuration.text = FormatTime_numOnly(item.durationMs)
        holder.tvFormat.text = item.format.ifEmpty { "未知" }
        holder.tvFrameLoadingJob?.cancel()
        holder.tvFrameLoadingJob = CoroutineScope_LoadCoverFrame.launch(Dispatchers.IO) { setHolderFrame(item, holder) }
        //点击事件设定
        holder.TouchPad.setOnClickListener { onItemClick(item.uriString.toUri()) }
        holder.tvDuration.setOnClickListener { onDurationClick(item) }
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
                context.showCustomToast("进入视频后,可在更多选项面板更新封面", Toast.LENGTH_SHORT, 3)
                true
            }
            popup_hide_item.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                context.showCustomToast("已停止对隐藏视频功能的支持", Toast.LENGTH_SHORT, 3)
                true
            }
            popup_onSmallCardPlay.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onSmallCardPlay(item.uriString, item.filename)
                true
            }
        }
        holder.tvFormat.setOnClickListener { onFormatClick(item, item.format) }
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
    //更新指定位置的封面
    fun updateCoverForVideo(uriNumOnly: Long) {
        //先检查新图是否存在
        val covers_path = File(context.filesDir, "miniature/cover")
        val cover_file = File(covers_path, "${uriNumOnly}.webp")
        if (cover_file.exists()) {
            val bitmap = BitmapFactory.decodeFile(cover_file.absolutePath)
        }
        //遍历列表并换图
        snapshot().forEachIndexed { index, mediaItem ->
            if (mediaItem?.uriNumOnly == uriNumOnly) {
                notifyItemChanged(index)
            }
        }
    }
    //内部Functions
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
    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForVideo, holder: ViewHolder)  {
        val imageTag = item.uriNumOnly.toString()
        //记录holder的tag
        withContext(Dispatchers.Main) {
            holder.tvFrame.tag = imageTag
        }
        //设置文件
        val cover_item_file = File(covers_path, "${item.uriNumOnly}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main){
                if (holder.tvFrame.tag == imageTag) {
                    holder.tvFrame.setImageBitmap(frame)
                    //holder.tvFrame.startAnimation(FadeInAnimation)
                } else {
                    frame?.recycle()
                }
            }
        }
        //不存在,生成图片
        else{
            generateCoverFrame(item, holder)
        }

    }
    //生成缩略图
    private fun generateCoverFrame(item: MediaItemForVideo, holder: ViewHolder){
        CoroutineScope_GenerateCover.launch(Dispatchers.IO){
            val retriever = MediaMetadataRetriever()
            try {
                //需要使用数据源作为参数,不能使用绝对路径,否则会因为安卓13及以上权限限制而无法访问除.mp4之外的视频文件
                context.contentResolver.openFileDescriptor(item.uriString.toUri(), "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }
                //获取视频封面帧
                var bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                //检查是否为黑屏
                if (bitmap != null){
                    if (isDarkFrame(bitmap)) {
                        bitmap = retriever.getFrameAtTime(item.durationMs * 1000 / 2, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    if (bitmap != null) {
                        //创建目录
                        if (!covers_path.exists()) {
                            covers_path.mkdirs()
                        }
                        //图片裁剪：最好裁剪成ImageView一样的比例
                        val targetWidth = 400
                        val targetHeight = (targetWidth * 9 / 10)
                        val processedBitmap = processCenterCrop(bitmap)
                        //保存图片
                        val cover_item_file = File(covers_path, "${item.uriNumOnly}.webp")
                        cover_item_file.outputStream().use {
                            processedBitmap.compress(Bitmap.CompressFormat.WEBP, 50, it)
                        }
                        //刷新页面
                        withContext(Dispatchers.Main) {
                            holder.tvFrame.setImageBitmap(processedBitmap)
                            //holder.tvFrame.startAnimation(FadeInAnimation)
                        }

                        if (bitmap != processedBitmap) {
                            bitmap.recycle()
                        }
                    }
                }
                //使用默认占位图
                else{

                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            finally {
                retriever.release()
            }
        }
    }
    private fun processCenterCrop(src: Bitmap): Bitmap {
        //以后可以添加为传入量
        //processCenterCrop(src: Bitmap, targetWidth: Int = 300, targetHeight: Int): Bitmap {
        val targetWidth = 400
        val targetHeight = (targetWidth * 9 / 10)

        val srcWidth = src.width
        val srcHeight = src.height

        val scale = (targetWidth.toFloat() / srcWidth).coerceAtLeast(targetHeight.toFloat() / srcHeight)

        val scaledWidth = scale * srcWidth
        val scaledHeight = scale * srcHeight

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val targetBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(targetBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(src, null, destRect, paint)

        return targetBitmap
    }

    //黑屏评估
    private fun isDarkFrame(bmp: Bitmap, darkThreshold: Int = 20, ratioThreshold: Float = 0.9f, sampleStep: Int = 4): Boolean {
        val w = bmp.width
        val h = bmp.height
        //
        val left = (w / 3)
        val right = 2 * w / 3
        val top = h / 3
        val bottom = 2 * h / 3

        var darkCount = 0
        var totalCount = 0

        for (y in top until bottom step sampleStep) {
            for (x in left until right step sampleStep) {
                val pixel = bmp[x, y]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (brightness <= darkThreshold) darkCount++
                totalCount++
            }
        }
        return darkCount.toFloat() / totalCount >= ratioThreshold
    }


}