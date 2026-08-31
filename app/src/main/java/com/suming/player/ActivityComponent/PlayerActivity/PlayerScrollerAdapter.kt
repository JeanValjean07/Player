package com.suming.player.ActivityComponent.PlayerActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.FuncionalPack.ScrollerHelper
import com.suming.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
class PlayerScrollerAdapter(
    private val context: Context,
    private val mediaDuration: Long,
    private val recyclerView: RecyclerView,
) : RecyclerView.Adapter<PlayerScrollerAdapter.scrollerViewHolder>() {

    //协程
    private val coroutine_capture = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_save = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //缩略图内存缓存
    private val BitmapCache = LruCache<Int, Bitmap>(10 * 1024 * 1024)
    //占位图
    private var BitmapHolder : Bitmap? = null

    //viewHolder
    class scrollerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemFrame_Job_capture: Job? = null
        var itemFrame_Job_load: Job? = null
        val itemFrame: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }




    init {

        //加载已有图
        loadFrameFolder()

    }

    override fun getItemCount() = (ScrollerHelper.allFrame_totalFrameNumber)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): scrollerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_player_scroller_item, parent, false)

        return scrollerViewHolder(view)
    }

    override fun onBindViewHolder(holder: scrollerViewHolder, position: Int) {
        //取出缓存中的图片
        val frame = BitmapCache.get(position)
        if (frame == null){

            //设置占位图
            holder.itemFrame.setImageBitmap(BitmapHolder)

            //启动协程获取图片
            coroutine_capture.launch {
                //开始截取图片,计算次位置的视频时间ms
                val time = position * ScrollerHelper.singleFrame_durationMs
                //开始截取图片
                val bitmap = ScrollerHelper.captureFrameInVideo(
                    videoDurationUs = mediaDuration * 1000,
                    timeUs = time * 1000,
                    option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    needCheckDark = true,
                    needCompress = true,
                )
                if (bitmap != null){
                    //尝试设置为占位图
                    if (BitmapHolder == null) {
                        BitmapHolder = bitmap
                        //首次设置时通知更新可见项
                        withContext(Dispatchers.Main) {
                            //首次设置时通知更新可见项
                            recyclerView.post {
                                for (position in 0 until ScrollerHelper.halfScreenEndIndex) {
                                    notifyItemChanged(position)
                                }
                            }
                        }

                    }
                    //将图片缓存到内存池
                    BitmapCache.put(position, bitmap)
                    //上屏
                    withContext(Dispatchers.Main) {
                        holder.itemFrame.setImageBitmap(bitmap)
                    }
                    //落盘
                    coroutine_save.launch {
                        ScrollerHelper.saveBitmapToDisk(bitmap, position, context)
                    }
                }else{
                    consoleLog("onBindViewHolder: position $position 截取到图片失败")
                }
            }
        }else{
            //直接上屏
            holder.itemFrame.setImageBitmap(frame)
            //保存占位图
            if (BitmapHolder == null) {
                BitmapHolder = frame
                //首次设置时通知更新可见项
                recyclerView.post {
                    for (position in 0 until ScrollerHelper.halfScreenEndIndex) {
                        notifyItemChanged(position)
                    }
                }

            }

        }

    }







    //一次性加载已有缩略图到缓存池
    private fun loadFrameFolder(){
        //加载目标位置的缩略图
        fun loadBitmapTargetPosition(position: Int): Bitmap? {
            return try {
                val thumbPath = ScrollerHelper.getScrollerFramePath(context)

                if (thumbPath.exists()) {

                    //加载该文件夹下文件名为position.webp的图片
                    BitmapFactory.decodeFile(File(thumbPath, "${position}.webp").absolutePath)

                }else{
                    null
                }
            }catch(e: Exception){
                consoleLog("loadFrameFolder: position $position 加载图片失败,e:${e.message}")
                null
            }
        }

        //加载所有缩略图到缓存池
        val positions = (0 until ScrollerHelper.allFrame_totalFrameNumber).toList()
        positions.map { position ->
            val bitmap = loadBitmapTargetPosition(position)

            position to bitmap
        }.forEach { (position, bitmap) ->
            bitmap?.let { BitmapCache.put(position, it) }
        }

    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerScrollerAdapter: $msg")
        }
    }
}