package com.suming.player

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import data.MediaModel.MediaItemForMusic
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri
import kotlinx.coroutines.Job
import androidx.core.graphics.createBitmap

class MainMusicAdapter(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onOptionsClick: (MediaItemForMusic, View) -> Unit,
):PagingDataAdapter<MediaItemForMusic, MainMusicAdapter.ViewHolder>(diffCallback) {
    //条目比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForMusic>() {
            override fun areItemsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mediaTouchPad: View = itemView.findViewById(R.id.TouchPad)
        val mediaThumb: ImageView = itemView.findViewById(R.id.auThumb)
        var mediaThumbJob: Job? = null
        val mediaName: TextView = itemView.findViewById(R.id.auName)
        val mediaArtist: TextView = itemView.findViewById(R.id.auArtist)
        val ButtonOptions: CardView = itemView.findViewById(R.id.ButtonOptions)
    }
    //加载动画
    private var FadeInAnimation: AlphaAnimation = AlphaAnimation(0.0f, 1.0f)
    private val covers_path = File(context.filesDir, "miniature/music_cover")
    private val CoroutineScope_GenerateCover = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CoroutineScope_LoadCoverFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())


    init {
        FadeInAnimation.duration = 100
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_adapter_music_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.mediaName.text = item.name.substringBeforeLast(".")
        holder.mediaArtist.text = if (item.artist == "<unknown>" || item.artist == "") { "未知艺术家" } else { item.artist }
        holder.mediaThumbJob?.cancel()
        holder.mediaThumbJob = CoroutineScope_LoadCoverFrame.launch { setHolderFrame(item, holder) }
        //点击事件设定
        holder.mediaTouchPad.setOnClickListener {
            onItemClick(item.uri)
        }
        holder.ButtonOptions.setOnClickListener {
            onOptionsClick(item, it)
        }
    }



    //Functions
    //检查缩略图
    private suspend fun setHolderFrame(item: MediaItemForMusic, holder: ViewHolder) {
        val imageTag = item.name.hashCode().toString()
        //记录holder的tag
        withContext(Dispatchers.Main) {
            holder.mediaThumb.tag = imageTag
        }
        //设置文件
        val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
        //检查是否存在
        if (cover_item_file.exists()){
            val frame = BitmapFactory.decodeFile(cover_item_file.absolutePath)
            withContext(Dispatchers.Main){
                if (holder.mediaThumb.tag == imageTag) {
                    holder.mediaThumb.setImageBitmap(frame)
                    holder.mediaThumb.startAnimation(FadeInAnimation)
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
    private fun generateCoverFrame(item: MediaItemForMusic, holder: ViewHolder){
        CoroutineScope_GenerateCover.launch(Dispatchers.IO){
            val retriever = MediaMetadataRetriever()
            try {
                //需要使用数据源作为参数,不能使用绝对路径,否则会因为安卓13及以上权限限制而无法访问除.mp3之外的音频文件
                context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }
                //mp3封面图片:retriever.embeddedPicture
                val bitmap = retriever.embeddedPicture
                //生成成功
                if (bitmap != null){
                    //转换为Bitmap
                    val bitmap = BitmapFactory.decodeByteArray(bitmap, 0, bitmap.size)
                    //创建目录
                    if (!covers_path.exists()) {
                        covers_path.mkdirs()
                    }
                    //图片裁剪：最好裁剪成ImageView一样的比例
                    val processedBitmap = processCenterCrop(bitmap)
                    //保存图片
                    val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
                    cover_item_file.outputStream().use {
                        processedBitmap.compress(Bitmap.CompressFormat.WEBP, 50, it)
                    }
                    //刷新页面
                    withContext(Dispatchers.Main){
                        holder.mediaThumb.setImageBitmap(processedBitmap)
                        //holder.mediaThumb.startAnimation(FadeInAnimation)
                    }

                    if (bitmap != processedBitmap) {
                        bitmap.recycle()
                    }
                }
                //生成失败:把默认占位图做成缩略图
                else{
                    val defaultBitmap = vectorToBitmap(context, R.drawable.ic_music_music_album) ?: return@launch
                    //val processedBitmap = processCenterCrop(defaultBitmap, 50, 50)
                    //创建目录
                    if (!covers_path.exists()) {
                        covers_path.mkdirs()
                    }
                    //保存图片
                    val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
                    cover_item_file.outputStream().use {
                        defaultBitmap.compress(Bitmap.CompressFormat.WEBP, 50, it)
                    }
                    //刷新页面
                    withContext(Dispatchers.Main){
                        holder.mediaThumb.setImageBitmap(defaultBitmap)
                        //holder.mediaThumb.startAnimation(FadeInAnimation)
                    }
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                Log.e("SuMing", "generateCoverFrame: ${item.name} 生成缩略图异常: ${e.message}")
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
        val targetHeight = (targetWidth * 1 / 1)

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

    //矢量图转Bitmap
    private fun vectorToBitmap(context: Context, @DrawableRes resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null

        val bitmap = createBitmap(100, 100)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


}