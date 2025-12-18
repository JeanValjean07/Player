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

class MainVideoAdapter(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onDurationClick: (MediaItemForVideo) -> Unit,
    private val onOptionClick: (MediaItemForVideo) -> Unit,
    private val onItemHideClick: (Uri, Boolean) -> Unit,
    private val onFormatClick: (MediaItemForVideo, String) -> Unit,
    private val onSmallCardPlay: (Uri, String) -> Unit
):PagingDataAdapter<MediaItemForVideo, MainVideoAdapter.ViewHolder>(diffCallback) {
    //条目比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.name == newItem.name
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
    private val covers_path = File(context.filesDir, "miniature/cover")



    init {
        FadeInAnimation.duration = 300
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_adapter_video_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name.substringBeforeLast(".")
        holder.tvDuration.text = FormatTime_numOnly(item.durationMs)
        holder.tvFormat.text = item.format.ifEmpty { "未知" }
        holder.tvFrameLoadingJob?.cancel()
        holder.tvFrameLoadingJob = CoroutineScope_LoadCoverFrame.launch(Dispatchers.IO) { setHolderFrame(item, holder) }
        //点击事件设定
        holder.TouchPad.setOnClickListener { onItemClick(item.uri) }
        holder.tvDuration.setOnClickListener { onDurationClick(item) }
        holder.tvOption.setOnClickListener {
            ToolVibrate().vibrate(context)
            //显示菜单
            val popup = PopupMenu(holder.itemView.context, holder.tvOption)
            popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
            val popup_update_cover = popup.menu.findItem(R.id.MenuAction_Repic)
            val popup_hide_text = popup.menu.findItem(R.id.MenuAction_Hide)
            val popup_onSmallCardPlay = popup.menu.findItem(R.id.MenuAction_onSmallCardPlay)
            val local_isHidden = item.isHidden
            popup_hide_text.title = if (local_isHidden) "取消隐藏" else "隐藏"
            popup.show()
            //注册点击
            popup_hide_text.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onItemHideClick(item.uri, !local_isHidden)
                item.isHidden = !local_isHidden
                true
            }
            popup_update_cover.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                context.showCustomToast("进入视频后,可在更多选项面板更新封面", Toast.LENGTH_SHORT, 3)
                true
            }
            popup_onSmallCardPlay.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onSmallCardPlay(item.uri, item.name)
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
        val item = getItem(position) ?: return
        holder.tvFrameLoadingJob?.cancel()

    }



    //Functions
    //更新指定位置的封面
    fun updateCoverForVideo(videoName: String) {
        //先检查新图是否存在
        val covers_path = File(context.filesDir, "miniature/cover")
        val cover_file = File(covers_path, "${videoName.hashCode()}.webp")
        if (cover_file.exists()) {
            val bitmap = BitmapFactory.decodeFile(cover_file.absolutePath)
        }
        //遍历列表并换图
        snapshot().forEachIndexed { index, mediaItem ->
            if (mediaItem?.name == videoName) {
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
    //uri转绝对路径
    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
        var absolutePath: String? = null

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    absolutePath = it.getString(columnIndex)
                }
            }
        }
        else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            absolutePath = uri.path
        }

        if (absolutePath != null && File(absolutePath).exists()) {
            return absolutePath
        }
        return null
    }
    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForVideo, holder: ViewHolder) {
        val imageTag = item.name.hashCode().toString()
        //在主线程记录当前ViewHolder预期的图片标识
        withContext(Dispatchers.Main) {
            holder.tvFrame.tag = imageTag
        }
        val covers_path = File(context.filesDir, "miniature/cover")
        val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main){
                if (holder.tvFrame.tag == imageTag) {
                    holder.tvFrame.setImageBitmap(frame)
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
                retriever.setDataSource(getAbsoluteFilePath(context, item.uri) ?: item.uri.toString())
                val bitmap = retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                //生成成功
                if (bitmap != null){
                    //创建目录
                    val covers_path = File(context.filesDir, "miniature/cover")
                    if (!covers_path.exists()) {
                        covers_path.mkdirs()
                    }

                    val targetWidth = 400  // 示例值，根据你的 UI 需求设定
                    val targetHeight = (targetWidth * 9 / 10)

                    // 3. 执行 CenterCrop 裁剪与缩放
                    val processedBitmap = processCenterCrop(bitmap, targetWidth, targetHeight)
                    //保存图片
                    val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
                    cover_item_file.outputStream().use {
                        processedBitmap.compress(Bitmap.CompressFormat.WEBP, 50, it)
                    }
                    //刷新页面
                    withContext(Dispatchers.Main){
                        holder.tvFrame.setImageBitmap(processedBitmap)
                        //holder.tvFrame.startAnimation(FadeInAnimation)
                    }

                    if (bitmap != processedBitmap) {
                        bitmap.recycle()
                    }
                }
                //生成失败
                else{
                    Toast.makeText(context, "存在无法生成缩略图的视频,请手动为其截取", Toast.LENGTH_SHORT).show()
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
    private fun processCenterCrop(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height

        // 计算缩放比例
        val scale = (targetWidth.toFloat() / srcWidth).coerceAtLeast(targetHeight.toFloat() / srcHeight)

        // 计算缩放后的中间尺寸
        val scaledWidth = scale * srcWidth
        val scaledHeight = scale * srcHeight

        // 计算裁剪起始点 (居中)
        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        // 创建目标画布
        val targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565) // 进一步节省内存
        val canvas = Canvas(targetBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(src, null, destRect, paint)

        return targetBitmap
    }


}