package com.suming.player.FuncionalPack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.net.toUri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.random.Random

object ScrollerHelper {

    //当前媒体标识和设置入口
    private var current_uriNumOnly = 0L
    fun setMediaItemMark(uriNumOnly: Long){
        current_uriNumOnly = uriNumOnly
        scroller_frame_folder = "Scroller/video/${uriNumOnly}/"
    }


    //定义进度条缩略图的储存位置
    private var scroller_frame_folder = "Scroller/video/${current_uriNumOnly}/"


    //进度条参数
    const val maxPicNumber = 30
    //总的图片数量
    var allFrame_totalFrameNumber = 0
    //单张图片的像素宽度(参与offset计算)
    var singleFrame_WidthPx = 0
    //单张图片对应的视频时长
    var singleFrame_durationMs = 0L




    //完整准备进度条参数,返回值是否成功
    fun prepareForNewMedia(uriNumOnly: Long, mediaDuration: Long, absolutePath: String): Boolean{
        if (mediaDuration <= 0) return false
        if (uriNumOnly == 0L) return false

        //计算图片数
        if (mediaDuration / 1000 >= maxPicNumber){
            allFrame_totalFrameNumber = maxPicNumber
            singleFrame_durationMs = mediaDuration / maxPicNumber
        }else{
            allFrame_totalFrameNumber = (mediaDuration / 1000).toInt() + 1
            singleFrame_durationMs = mediaDuration / allFrame_totalFrameNumber
        }
        if (allFrame_totalFrameNumber < 1) allFrame_totalFrameNumber = 1
        if (singleFrame_durationMs < 1L) singleFrame_durationMs = 1L
        //consoleLog( "进度条参数计算 图片总数：${allFrame_totalFrameNumber},单张图片对应时长(毫秒)：${singleFrame_durationMs}" )
        //处理绝对路径字段
        val absolutePath = absolutePath.substringBefore("?")
        //检查字段合理性
        if (absolutePath.isEmpty()) return false


        //设置视频唯一标识
        setMediaItemMark(uriNumOnly)

        return true
    }


    fun getScrollerFramePath(context: Context): File {
        return File(context.filesDir, scroller_frame_folder)
    }






    //scroller retriever
    private var retriever: MediaMetadataRetriever? = null
    private var retriever_current_source = ""
    //初始化解码器
    fun initRetriever(){
        if (retriever == null) {
            retriever = MediaMetadataRetriever()
            retriever_current_source = ""
        }
    }
    //设置数据源
    fun setDataSource(absolutePath: String){
        if (absolutePath.isEmpty() || retriever_current_source == absolutePath) return
        try {
            retriever?.setDataSource(absolutePath)
            retriever_current_source = absolutePath
        } catch (e: Exception) {
            retriever_current_source = ""
            retriever = null
            consoleLog("setDataSource:设置数据源失败了 error: ${e.message}")
        }
    }

    //检查解码器状态
    private fun checkRetrieverState(absolutePath: String){
        if (retriever == null) initRetriever()
        //检查是否需要重设数据源
        if (absolutePath != retriever_current_source){
            consoleLog("checkRetrieverState: 数据源改变,需要重设")
            setDataSource(absolutePath)
        }else{
            consoleLog("checkRetrieverState: 数据源未改变,无需重设 absolutePath ${absolutePath} retriever_current_source $retriever_current_source")
        }
    }

    //截取视频帧
    private val mutex_scroller = Mutex()
    suspend fun captureFrameInVideo( context: Context,
                                     absolutePath: String,
                                     videoDurationUs: Long,
                                     timeUs: Long,
                                     option: Int,
                                     needCheckDark: Boolean = false,
                                     needCompress: Boolean = true ): Bitmap? {
        return mutex_scroller.withLock {
            try {
                //检查解码器状态
                checkRetrieverState(absolutePath)

                //截取帧
                var bitmap = retriever?.getFrameAtTime(timeUs, option)

                //检查图片是否有效
                if (bitmap != null) {
                    //检查黑屏
                    if (needCheckDark && isDarkFrame(bitmap)) {
                        val randomTime = (videoDurationUs * (0.2f + 0.6f * Random.nextFloat())).toLong()
                        consoleLog("ScrollerHelper: 重新截取一次,本次随机得到的时间为(从微秒转为秒为): ${randomTime / 1_000_000}")
                        bitmap = retriever?.getFrameAtTime(randomTime, option)
                    }
                    if (bitmap == null) {
                        consoleLog("ScrollerHelper: 图片失效：可能因黑屏而再次截取,但再次截取失败了")
                        return@withLock null
                    }

                    //压缩图片
                    if (needCompress) {
                        val oldBitmap = bitmap
                        bitmap = processCenterCropForVideo(bitmap)
                        if (oldBitmap !== bitmap) {
                            oldBitmap.recycle()
                        }
                    }
                    /*
                    if (bitmap == null) {
                        consoleLog("ArtworkCapturerForVideo: 图片失效：压缩后图片失效,需检查压缩算法")
                        return@withLock null
                    }

                     */

                    return@withLock bitmap
                } else {
                    consoleLog("ScrollerHelper: 图片无效：初次截取失败了")
                    return@withLock null
                }
            }catch(e: Exception){
                consoleLog("ScrollerHelper: 截取帧时发生错误: ${e.message}")
                return@withLock null
            }
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
    //压缩优化图片,保持比例并居中裁剪
    private fun processCenterCropForVideo(bitmap: Bitmap): Bitmap {
        //以后可以添加为传入量
        //processCenterCrop(src: Bitmap, targetWidth: Int = 300, targetHeight: Int): Bitmap {
        val targetWidth = 400
        val targetHeight = (targetWidth * 9 / 10)

        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        val scale = (targetWidth.toFloat() / srcWidth).coerceAtLeast(targetHeight.toFloat() / srcHeight)

        val scaledWidth = scale * srcWidth
        val scaledHeight = scale * srcHeight

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val targetBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(targetBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        return targetBitmap
    }


    //落盘保存
    fun saveBitmapToDisk(bitmap: Bitmap,position: Int, context: Context) {
        if (current_uriNumOnly == 0L) {
            consoleLog("ScrollerHelper: 未配置视频唯一标识,无法保存帧")
            return
        }
        val frame_folder = File(context.filesDir, scroller_frame_folder)
        val frame_file = File(frame_folder, "${position}.webp")
        //创建文件夹
        frame_folder.mkdirs()
        //
        frame_file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.WEBP, 20, it)
        }

    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ScrollerHelper: $msg")
        }
    }

}