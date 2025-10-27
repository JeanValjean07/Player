package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.view.updateLayoutParams
import androidx.databinding.ObservableList
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.getValue

@UnstableApi
class PlayerScrollerAdapter(
    private val context: Context,
    private val MediaInfo_AbsolutePath: String,
    private val MediaInfo_FileName: String,
    private val thumbItems: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>,
    private val eachPicWidth: Int,
    private val picNumber: Int,
    private val eachPicDuration: Int,
    private val PREFS_GenerateThumbSYNC: Boolean,
    private var recyclerView: RecyclerView? = null,
    private var PlayerScrollerVM: PlayerScrollerViewModel
) : RecyclerView.Adapter<PlayerScrollerAdapter.ThumbViewHolder>() {

    init {
        thumbItems.addOnListChangedCallback(
            object : ObservableList.OnListChangedCallback<ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>>() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onChanged(sender: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>) =
                    notifyDataSetChanged()

                override fun onItemRangeChanged(
                    sender: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>,
                    positionStart: Int,
                    itemCount: Int
                ) {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        notifyItemRangeChanged(positionStart, itemCount)
                    } else {
                        recyclerView?.post {
                            notifyItemRangeChanged(positionStart, itemCount)
                        }
                    }
                }

                override fun onItemRangeInserted(
                    sender: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>,
                    positionStart: Int,
                    itemCount: Int
                ) {
                    recyclerView?.post {
                        if (itemCount == 1) notifyItemInserted(positionStart)
                        else notifyItemRangeInserted(positionStart, itemCount)
                    }
                }

                override fun onItemRangeMoved(
                    sender: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>,
                    fromPosition: Int,
                    toPosition: Int,
                    itemCount: Int
                ) {
                    recyclerView?.post {
                        for (i in 0 until itemCount) {
                            notifyItemMoved(fromPosition + i, toPosition + i)
                        }
                    }
                }


                override fun onItemRangeRemoved(
                    sender: ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>,
                    positionStart: Int,
                    itemCount: Int
                ) {
                    recyclerView?.post {
                        notifyItemRangeRemoved(positionStart, itemCount)
                    }
                }
            })
    }



    //初始化—协程作用域
    private val coroutineScopeGenerateThumb = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile
    private var generateCoverWorking = false   //防止重复发起多次占位图生成任务



    inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var generateThumbJob: Job? = null
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }

    override fun getItemCount() = (picNumber)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_adapter_scroller_item, parent, false)

        if (MediaInfo_FileName != PlayerScrollerVM.last_MediaInfo_FileName){
            PlayerScrollerVM.last_MediaInfo_FileName = MediaInfo_FileName

            val newList = List(picNumber) {
                PlayerScrollerViewModel.ThumbScrollerItem(
                    thumbPath = null,
                    isCoverPlaced = false,
                    currentThumbType = false,
                    thumbGeneratingRunning = false
                )
            }
            thumbItems.clear()
            thumbItems.addAll(newList)
            recyclerView?.post {
                notifyDataSetChanged()
            }



        }
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
            retrieverMap[position]?.setDataSource(MediaInfo_AbsolutePath)
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
            if (PREFS_GenerateThumbSYNC){
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
            val SaveAs = File(context.cacheDir, "Media/${MediaInfo_FileName.hashCode()}/scroller/${position}.jpg")
            SaveAs.outputStream().use {
                val targetCoverWidth = 200
                val targetCoverHeight = (200 * ratio).toInt()
                val scaledBitmap = frame.scale(targetCoverWidth, targetCoverHeight)
                val success = scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
                if (!success) { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 15, it) }
                scaledBitmap.recycle()
                frame.recycle()
            }
            //修改item中的缩略图链接
            item.thumbPath = SaveAs
            item.thumbGeneratingRunning = false
            item.currentThumbType = true
            withContext(Dispatchers.Main) { notifyItemChanged(position) }
            //保存状态到数据库

        }
    }
    //截取占位缩略图
    private fun generateCover() {
        if (generateCoverWorking) return
        generateCoverWorking = true
        CoroutineScope(Dispatchers.IO).launch {
            val item = thumbItems[0]
            val retriever = MediaMetadataRetriever().apply { setDataSource(MediaInfo_AbsolutePath) }
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
                val SaveAs = File(context.cacheDir, "Media/${MediaInfo_FileName.hashCode()}/scroller/cover.jpg")
                SaveAs.outputStream().use {
                    val targetCoverWidth = 200
                    val targetCoverHeight = (200 * ratio).toInt()
                    val scaledBitmap = frame.scale(targetCoverWidth, targetCoverHeight)
                    val success = scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
                    if (!success) { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 15, it) }
                    scaledBitmap.recycle()
                    frame.recycle()
                }
                val newItem = item.copy(thumbPath = SaveAs)
                thumbItems[0] = newItem
                placeCover()
            }
        }
    }
    //放置占位缩略图链接
    @SuppressLint("NotifyDataSetChanged")
    private suspend fun placeCover(){
        val cover = File(context.cacheDir, "Media/${MediaInfo_FileName.hashCode()}/scroller/cover.jpg")
        thumbItems.replaceAll { it.copy(thumbPath = cover) }
        thumbItems.replaceAll { it.copy(isCoverPlaced = true) }
        withContext(Dispatchers.Main) { notifyDataSetChanged() }
        return
    }


}