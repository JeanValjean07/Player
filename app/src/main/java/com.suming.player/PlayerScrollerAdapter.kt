package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class PlayerScrollerAdapter(
    private val context: Context,
    private val videoPath: String,
    private val thumbItems: MutableList<PlayerScrollerViewModel.ThumbScrollerItem>,
    private val eachPicWidth: Int,
    private val picNumber: Int,
    private val eachPicDuration: Int,
    private val generateThumbSYNC: Int,
) : RecyclerView.Adapter<PlayerScrollerAdapter.ThumbViewHolder>() {

    //初始化—协程作用域
    private val coroutineScopeGenerateThumb = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile
    private var generateCoverWorking = false   //防止重复发起多次占位图生成任务


    inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var generateThumbJob: Job? = null
        var onBindViewHolderJob: Job? = null
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }


    override fun getItemCount() = (picNumber)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_bar, parent, false)
        return ThumbViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        val item = thumbItems[position]
        holder.itemView.updateLayoutParams<ViewGroup.LayoutParams> { this.width = eachPicWidth }
        item.thumbPath?.let { file ->
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            holder.ivThumbnail.setImageBitmap(bmp)
        }
    }

    override fun onViewAttachedToWindow(holder: ThumbViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = thumbItems[position]

        if (item.isCoverPlaced){
            if (item.currentThumbType){
                return
            }
            else{
                holder.generateThumbJob?.cancel()
                holder.generateThumbJob = coroutineScopeGenerateThumb.launch(Dispatchers.IO) { generateThumb(position) }
            }
        }
        else{
            generateCover()
        }
    }

    override fun onViewDetachedFromWindow(holder: ThumbViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return
        val item = thumbItems[holder.bindingAdapterPosition]
        if (item.thumbGeneratingRunning) {
            holder.generateThumbJob?.cancel()
            item.thumbGeneratingRunning = false
        }
    }




    //Functions
    //截取实际缩略图
    val retrieverMap = mutableMapOf<Int, MediaMetadataRetriever>()
    private suspend fun generateThumb(position: Int) {
        val item = thumbItems[position]
        if (item.thumbGeneratingRunning) return
        item.thumbGeneratingRunning = true
        retrieverMap[position] = MediaMetadataRetriever()
        try {
            retrieverMap[position]?.setDataSource(videoPath)
            coroutineContext.ensureActive()
            var wStr = retrieverMap[position]?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            var hStr = retrieverMap[position]?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotateStr = retrieverMap[position]?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (rotateStr == "90"){
                val temp = wStr
                wStr = hStr
                hStr = temp
            }
            val videoWidth = wStr?.toFloat() ?: 0f
            val videoHeight = hStr?.toFloat() ?: 0f
            val ratio = videoHeight.div(videoWidth)
            coroutineContext.ensureActive()
            if (generateThumbSYNC == 1){
                val frame = retrieverMap[position]?.getFrameAtTime(
                    (position * eachPicDuration * 1000L),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                coroutineContext.ensureActive()
                retrieverMap[position]?.release()
                saveThumb(ratio, position, frame)
                coroutineContext.ensureActive()
            }
            else{
                val frame = retrieverMap[position]?.getFrameAtTime(
                    (position * eachPicDuration * 1000L),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                coroutineContext.ensureActive()
                retrieverMap[position]?.release()
                saveThumb(ratio, position, frame)
                coroutineContext.ensureActive()
            }
            item.thumbGeneratingRunning = false
        }
        catch (_: Exception) {
        } finally {
            item.thumbGeneratingRunning = false
            retrieverMap[position]?.release()
        }
    }
    //压缩和保存缩略图
    private suspend fun saveThumb(ratio: Float, position: Int, frame: Bitmap?) {
        val item = thumbItems[position]
        if (frame != null) {
            val outFile = File(context.cacheDir, "thumb_${videoPath.hashCode()}_${position}.jpg")
            outFile.outputStream().use {
                val targetCoverWidth = 200
                val targetCoverHeight = (200 * ratio).toInt()
                val scaledBitmap = frame.scale(targetCoverWidth, targetCoverHeight)
                val success = scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
                if (!success) { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 15, it) }
                scaledBitmap.recycle()
                frame.recycle()
            }
            //修改item中的缩略图链接
            item.thumbPath=outFile
            item.thumbGeneratingRunning = false
            item.currentThumbType = true
            withContext(Dispatchers.Main) { notifyItemChanged(position) }
        }
    }
    //截取占位缩略图
    private fun generateCover() {
        if (generateCoverWorking) return
        generateCoverWorking = true
        CoroutineScope(Dispatchers.IO).launch {
            val item = thumbItems[0]
            val retriever = MediaMetadataRetriever().apply { setDataSource(videoPath) }
            var wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            var hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (rotateStr == "90"){
                val temp = wStr
                wStr = hStr
                hStr = temp
            }
            val videoWidth = wStr?.toFloat() ?: 0f
            val videoHeight = hStr?.toFloat() ?: 0f
            val ratio = videoHeight.div(videoWidth)
            val frame = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            if (frame != null) {
                val outFile = File(context.cacheDir, "thumb_${videoPath.hashCode()}_cover.jpg")
                outFile.outputStream().use {
                    val targetCoverWidth = 200
                    val targetCoverHeight = (200 * ratio).toInt()
                    val scaledBitmap = frame.scale(targetCoverWidth, targetCoverHeight)
                    val success = scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
                    if (!success) { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 15, it) }
                    scaledBitmap.recycle()
                    frame.recycle()
                }
                val newItem = item.copy(thumbPath = outFile)
                thumbItems[0] = newItem
                placeCover()
            }
        }
    }
    //放置占位缩略图链接
    @SuppressLint("NotifyDataSetChanged")
    private suspend fun placeCover(){
        val cover = File(context.cacheDir, "thumb_${videoPath.hashCode()}_cover.jpg")
        thumbItems.replaceAll { it.copy(thumbPath = cover) }
        thumbItems.replaceAll { it.copy(isCoverPlaced = true) }
        withContext(Dispatchers.Main) { notifyDataSetChanged() }
        return
    }


}//class END