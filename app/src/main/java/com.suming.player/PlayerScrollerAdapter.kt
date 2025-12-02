package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.view.updateLayoutParams
import androidx.databinding.ObservableList
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

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

    //协程作用域
    private val coroutineScopeGenerateThumb = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineScopeLoadFrame = CoroutineScope(Dispatchers.IO + SupervisorJob()) //可选,禁止删除
    //缩略图内存缓存
    private val BitmapCache = LruCache<Int, Bitmap>(10 * 1024 * 1024)
    private lateinit var HolderBitmap : Bitmap

    inner class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var generateThumbJob: Job? = null
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }

    //主线程初始化操作
    init {
        //列表监听器(必选)
        thumbItems.addOnListChangedCallback(object : ObservableList.OnListChangedCallback<ObservableList<PlayerScrollerViewModel.ThumbScrollerItem>>() {
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
        //添加自定义的初始化时操作
        //加载已有图
        loadFrame()

    }

    override fun getItemCount() = (picNumber)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_adapter_scroller_item, parent, false)

        //媒体文件更新,需要更换数据源
        if (MediaInfo_FileName != PlayerScrollerVM.last_MediaInfo_FileName){
            PlayerScrollerVM.last_MediaInfo_FileName = MediaInfo_FileName

            val newList = List(picNumber) {
                PlayerScrollerViewModel.ThumbScrollerItem(
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
        //指定单图宽度
        holder.itemView.updateLayoutParams<ViewGroup.LayoutParams> { this.width = eachPicWidth }
        //绑定图片
        val frame = BitmapCache.get(position)
        if (frame == null){
            holder.ivThumbnail.setImageBitmap(HolderBitmap)
        }else{
            holder.ivThumbnail.setImageBitmap(frame)
        }


    }

    override fun onViewAttachedToWindow(holder: ThumbViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition

        if (BitmapCache.get(position) == null){
            holder.generateThumbJob = coroutineScopeGenerateThumb.launch(Dispatchers.IO) { generateThumb(position) }
        }

    }


    //Functions
    //一次性加载缩略图到内存
    private fun loadFrame(){
        fun preparePlaceholder(){
            if (BitmapCache[0] == null){
                generatePlaceholder()
            } else{
                HolderBitmap = BitmapCache[0]
            }
        }

        fun loadBitmapFromPosition(position: Int): Bitmap? {
            return try {
                val thumbPath = File(context.filesDir, "miniature/${MediaInfo_FileName.hashCode()}/scroller/${position}.jpg")
                if (thumbPath.exists()) {
                    //return this bitmap
                    BitmapFactory.decodeFile(thumbPath.absolutePath)
                }
                else {
                    //return null
                    null
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun loadBitmapAll_single_thread(positions: List<Int>) {
            positions.map { position ->

                val bitmap = loadBitmapFromPosition(position)
                position to bitmap

            }.forEach { (position, bitmap) ->
                bitmap?.let {
                    BitmapCache.put(position, it)
                }
            }
            preparePlaceholder()
        }
        loadBitmapAll_single_thread((0 until picNumber).toList())

        fun loadBitmapAll_multi_thread(positions: List<Int>) {
            CoroutineScope(Dispatchers.IO).launch {
                positions.map { position ->
                    async {
                        val bitmap = loadBitmapFromPosition(position)
                        position to bitmap
                    }
                }.awaitAll().forEach { (position, bitmap) ->
                    bitmap?.let {
                        withContext(Dispatchers.Main) {
                            BitmapCache.put(position, it)
                        }
                    }
                }
                preparePlaceholder()
            }
        }
        //loadBitmapAll_multi_thread((0 until picNumber).toList())

    }
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
            //使用关键帧缩略图
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
            //使用精确帧缩略图
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
        catch (_: Exception) {  }
        finally {
            item.thumbGeneratingRunning = false
            retrieverMap[position]?.release()
        }
    }
    //压缩和保存缩略图
    private suspend fun saveThumb(ratio: Float, position: Int, frame: Bitmap?) {
        val item = thumbItems[position]
        if (frame != null) {
            val save_file_path = File(context.filesDir, "miniature/${MediaInfo_FileName.hashCode()}/scroller/${position}.jpg")
            save_file_path.parentFile?.mkdirs()
            save_file_path.outputStream().use {
                val targetCoverWidth = 200
                val targetCoverHeight = (200 * ratio).toInt()
                val scaledBitmap = frame.scale(targetCoverWidth, targetCoverHeight)
                val success = scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
                if (!success) { scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 15, it) }

                BitmapCache.put(position, scaledBitmap)

            }
            //修改item中的缩略图链接
            item.thumbGeneratingRunning = false
            item.currentThumbType = true

            withContext(Dispatchers.Main) { notifyItemChanged(position) }


        }
    }
    //生成和使用占位图
    private fun generatePlaceholder() {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(MediaInfo_AbsolutePath)
        try {
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
            val frame = retriever.getFrameAtTime((0), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null) {
                HolderBitmap = frame.scale(200, (200 * ratio).toInt())
            }else{
                return
            }
        }
        catch (_: Exception) {  }
        finally {
            retriever.release()
        }

    }
}