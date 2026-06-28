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
import androidx.core.graphics.scale
import androidx.databinding.ObservableList
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.FuncionalPack.ScrollerHelper
import com.suming.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@UnstableApi
class PlayerScrollerAdapter(
    private val context: Context,
    private val mediaDuration: Long,
    private val absolutePath: String,
) : RecyclerView.Adapter<PlayerScrollerAdapter.scrollerViewHolder>() {

    //协程
    private val coroutine_capture = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_save = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //缩略图内存缓存
    private val BitmapCache = LruCache<Int, Bitmap>(10 * 1024 * 1024)
    //占位图
    private var placeholderBitmap : Bitmap? = null
    //viewHolder
    class scrollerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemFrame_Job_capture: Job? = null
        var itemFrame_Job_load: Job? = null
        val itemFrame: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }




    init {
        //consoleLog("init")
        //列表监听器
        /*
        frames.addOnListChangedCallback(object : ObservableList.OnListChangedCallback<ObservableList<PlayerScrollerViewModel.scrollerItem>>() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChanged(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>) =
                notifyDataSetChanged()
            override fun onItemRangeChanged(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
                                            positionStart: Int,
                                            itemCount: Int) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    notifyItemRangeChanged(positionStart, itemCount)
                } else {
                    recyclerView?.post {
                        notifyItemRangeChanged(positionStart, itemCount)
                    }
                }
            }
            override fun onItemRangeInserted(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
                                             positionStart: Int,
                                             itemCount: Int) {
                recyclerView?.post {
                    if (itemCount == 1) notifyItemInserted(positionStart)
                    else notifyItemRangeInserted(positionStart, itemCount)
                }
            }
            override fun onItemRangeMoved(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
                                          fromPosition: Int,
                                          toPosition: Int,
                                          itemCount: Int) {
                recyclerView?.post {
                    for (i in 0 until itemCount) {
                        notifyItemMoved(fromPosition + i, toPosition + i)
                    }
                }
            }
            override fun onItemRangeRemoved(sender: ObservableList<PlayerScrollerViewModel.scrollerItem>,
                                            positionStart: Int,
                                            itemCount: Int) {
                recyclerView?.post {
                    notifyItemRangeRemoved(positionStart, itemCount)
                }
            }
        })

         */

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
        //consoleLog("onBindViewHolder: position $position 触发绑定")
        //取出缓存中的图片
        val frame = BitmapCache.get(position)
        if (frame == null){
            consoleLog("onBindViewHolder: position $position 缓存中没有图片")
            holder.itemFrame.setImageBitmap(placeholderBitmap)

            //启动协程
            coroutine_capture.launch {
                //开始截取图片,计算次位置的视频时间ms
                val time = position * ScrollerHelper.singleFrame_durationMs
                //开始截取图片
                val bitmap = ScrollerHelper.captureFrameInVideo(
                    context = context,
                    absolutePath = absolutePath,
                    videoDurationUs = mediaDuration * 1000,
                    timeUs = time * 1000,
                    option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    needCheckDark = true,
                    needCompress = true,
                )
                if (bitmap != null){
                    consoleLog("onBindViewHolder: position $position 截取到图片")
                    //尝试设置为占位图
                    setPlaceholderBitmap(bitmap)
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
            consoleLog("onBindViewHolder: position $position 缓存中有图片,直接上屏")
            holder.itemFrame.setImageBitmap(frame)
        }

    }






    //放置占位图
    private fun setPlaceholderBitmap(bitmap: Bitmap){
        if (placeholderBitmap != null) return

        placeholderBitmap = bitmap
    }

    //一次性加载已有缩略图到缓存池
    private fun loadFrameFolder(){
        //加载目标位置的缩略图
        fun loadBitmapTargetPosition(position: Int): Bitmap? {
            return try {
                val thumbPath = ScrollerHelper.getScrollerFramePath(context)
                //consoleLog("loadFrameFolder: 缩略图路径:$thumbPath")
                if (thumbPath.exists()) {
                    //consoleLog("loadFrameFolder: 缩略图路径存在")
                    //加载该文件夹下文件名为position.webp的图片
                    BitmapFactory.decodeFile(File(thumbPath, "${position}.webp").absolutePath)
                }
                else {
                    null
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        //加载所有缩略图到缓存池
        val positions = (0 until ScrollerHelper.allFrame_totalFrameNumber).toList()
        positions.map { position ->
            val bitmap = loadBitmapTargetPosition(position)
            //consoleLog("loadFrameFolder: position $position 是否为空:${bitmap == null}")
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