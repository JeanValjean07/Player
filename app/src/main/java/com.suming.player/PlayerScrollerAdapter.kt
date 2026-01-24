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
import androidx.core.graphics.get
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
class PlayerScrollerAdapter(
    private val context: Context,
    private val MediaInfo_AbsolutePath: String,
    private val MediaInfo_FileName: String,
    private val thumbItems: ObservableList<PlayerScrollerViewModel.scrollerItem>,
    private val scrollerParam_EachPicWidth: Int,
    private val scrollerParam_PicNumber: Int,
    private val scrollerParam_EachPicDuration: Int,
    private val PREFS_GenerateThumbSYNC: Boolean,
    private var recyclerView: RecyclerView? = null,
    private var PlayerScrollerVM: PlayerScrollerViewModel
) : RecyclerView.Adapter<PlayerScrollerAdapter.scrollerViewHolder>() {

    //协程作用域
    private val coroutineScopeGenerateFrame = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //缩略图内存缓存
    private val BitmapCache = LruCache<Int, Bitmap>(10 * 1024 * 1024)
    private lateinit var HolderBitmap : Bitmap
    //viewHolder
    class scrollerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var generateThumbJob: Job? = null
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }



    //主线程初始化操作
    init {
        //列表监听器(必选)
        thumbItems.addOnListChangedCallback(object : ObservableList.OnListChangedCallback<ObservableList<PlayerScrollerViewModel.scrollerItem>>() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChanged(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>) =
                notifyDataSetChanged()

            override fun onItemRangeChanged(
                sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
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
                sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
                positionStart: Int,
                itemCount: Int
            ) {
                recyclerView?.post {
                    if (itemCount == 1) notifyItemInserted(positionStart)
                    else notifyItemRangeInserted(positionStart, itemCount)
                }
            }

            override fun onItemRangeMoved(
                sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
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
                sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
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

    override fun getItemCount() = (scrollerParam_PicNumber)

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): scrollerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_adapter_scroller_item, parent, false)

        //媒体文件更新,需要更换数据源
        if (MediaInfo_FileName != PlayerScrollerVM.last_MediaInfo_FileName){
            PlayerScrollerVM.last_MediaInfo_FileName = MediaInfo_FileName

            val newList = List(scrollerParam_PicNumber) {
                PlayerScrollerViewModel.scrollerItem(
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


        return scrollerViewHolder(view)
    }

    override fun onBindViewHolder(holder: scrollerViewHolder, position: Int) {
        //指定单图宽度
        holder.itemView.updateLayoutParams<ViewGroup.LayoutParams> { this.width = scrollerParam_EachPicWidth }
        //绑定图片
        val frame = BitmapCache.get(position)
        if (frame == null){
            holder.ivThumbnail.setImageBitmap(HolderBitmap)
        }else{
            holder.ivThumbnail.setImageBitmap(frame)
        }

    }

    override fun onViewAttachedToWindow(holder: scrollerViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition

        if (BitmapCache.get(position) == null){
            holder.generateThumbJob = coroutineScopeGenerateFrame.launch(Dispatchers.IO) { generateFrame(position) }
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
        loadBitmapAll_single_thread((0 until scrollerParam_PicNumber).toList())

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
    private suspend fun generateFrame(position: Int) {
        val item = thumbItems[position]
        if (item.thumbGeneratingRunning) return
        item.thumbGeneratingRunning = true
        retrieverMap[position] = MediaMetadataRetriever()
        try {
            retrieverMap[position]?.setDataSource(MediaInfo_AbsolutePath)
            currentCoroutineContext().ensureActive()
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
            currentCoroutineContext().ensureActive()
            //使用关键帧缩略图
            if (PREFS_GenerateThumbSYNC){
                //首张需检查是否纯黑
                if (position == 0){
                    var frame = retrieverMap[position]?.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame != null){
                        if (isDarkFrame(frame)){
                            frame = retrieverMap[position]?.getFrameAtTime(
                                (scrollerParam_EachPicDuration / 2 * 1000L),
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                            saveFrame(ratio, position, frame)
                        }
                    }
                    saveFrame(ratio, position, frame)
                }
                //非首张图
                else{
                    val frame = retrieverMap[position]?.getFrameAtTime((position * scrollerParam_EachPicDuration * 1000L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    saveFrame(ratio, position, frame)
                }
                currentCoroutineContext().ensureActive()
                retrieverMap[position]?.release()
                currentCoroutineContext().ensureActive()
            }
            //使用精确帧缩略图
            else{
                val frame = retrieverMap[position]?.getFrameAtTime(
                    (position * scrollerParam_EachPicDuration * 1000L),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                currentCoroutineContext().ensureActive()
                retrieverMap[position]?.release()
                saveFrame(ratio, position, frame)
                currentCoroutineContext().ensureActive()
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
    private suspend fun saveFrame(ratio: Float, position: Int, frame: Bitmap?) {
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
        try {
            retriever.setDataSource(MediaInfo_AbsolutePath)
        }catch (_: Exception) {

            return
        }



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
            val duration = scrollerParam_EachPicDuration * scrollerParam_PicNumber
            val frame = retriever.getFrameAtTime((duration / 2) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
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