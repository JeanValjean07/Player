package com.suming.player.FuncionalPack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.net.toUri
import com.suming.player.R
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

@Suppress("unused")
object ArtworkCapturer {

    //视频---------------------------------------------------------------
    //注意：对于视频,需要获取的内容是各个时间戳的截图
    //截取策略
    const val OPTION_CLOSEST_SYNC = MediaMetadataRetriever.OPTION_CLOSEST_SYNC    //就近关键帧
    const val OPTION_CLOSEST = MediaMetadataRetriever.OPTION_CLOSEST              //(就近)精确帧
    const val OPTION_NEXT_SYNC = MediaMetadataRetriever.OPTION_NEXT_SYNC          //下一个关键帧
    const val OPTION_PREVIOUS_SYNC = MediaMetadataRetriever.OPTION_PREVIOUS_SYNC  //上一个关键帧
    //retriever
    private val retriever_video = MediaMetadataRetriever()
    private var current_video_uriString = ""
    //串行截取
    private val mutex_video = Mutex()

    //retriever初始化
    fun initRetrieverVideo(context: Context, uri: Uri){
        //检查是否是同一个数据源
        if (uri.toString() == current_video_uriString){
            return
        }
        //设置新的数据源
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever_video.setDataSource(pfd.fileDescriptor)
        }

    }

    // timeUs: Long 微秒时间 / option: 截取策略 / needCheckDark: 是否检查黑屏 / needCompress: 是否需要压缩优化图片
    suspend fun captureFrameInVideo( context: Context,
        uri: Uri,
        videoDurationUs: Long,
        timeUs: Long,
        option: Int,
        needCheckDark: Boolean = false,
        needCompress: Boolean = false ): Bitmap? {
        return mutex_video.withLock {
            try {
                //检查是否需要替换数据源
                if (uri.toString() != current_video_uriString) {
                    initRetrieverVideo(context, uri)
                }

                //截取帧
                var bitmap = retriever_video.getFrameAtTime(timeUs, option)

                //检查图片是否有效
                if (bitmap != null) {
                    //检查黑屏
                    if (needCheckDark && isDarkFrame(bitmap)) {
                        val randomTime = (videoDurationUs * (0.2f + 0.6f * Random.nextFloat())).toLong()
                        consoleLog("ArtworkCapturerForVideo: 重新截取一次,本次随机得到的时间为(从微秒转为秒为): ${randomTime / 1_000_000}")
                        bitmap = retriever_video.getFrameAtTime(randomTime, option)
                    }
                    if (bitmap == null) {
                        consoleLog("ArtworkCapturerForVideo: 图片失效：可能因黑屏而再次截取,但再次截取失败了")
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
                    consoleLog("ArtworkCapturerForVideo: 图片无效：初次截取失败了")
                    return@withLock null
                }
            }catch(e: Exception){
                consoleLog("ArtworkCapturer: 截取帧时发生错误: ${e.message}")
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
    //获取占位图
    fun getDefaultVideoCoverFrame(context: Context): Bitmap? {
        val DefaultBitmap = vectorToBitmapForVideo(context, R.drawable.ic_album_video_album)

        return DefaultBitmap
    }
    //矢量图转Bitmap
    private fun vectorToBitmapForVideo(context: Context, @DrawableRes resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null

        val bitmap = createBitmap(400, 600)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    //音乐---------------------------------------------------------------
    //注意：对于音乐,需要获取的内容是专辑封面

    //retriever
    private val retriever_music = MediaMetadataRetriever()
    private var current_music_uriString = ""
    //串行截取
    private val mutex_music = Mutex()





    //retriever初始化
    fun initRetrieverMusic(context: Context, uri: Uri){
        //检查是否是同一个数据源
        if (uri.toString() == current_music_uriString){
            return
        }
        //设置新的数据源
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever_music.setDataSource(pfd.fileDescriptor)
        }

    }

    // needCompress: 是否需要压缩优化图片
    suspend fun captureAlbumInMusic( context: Context,
                                     uri: Uri,
                                     needCompress: Boolean = false ): Bitmap? {
        return mutex_music.withLock {
            try {
                //检查是否需要替换数据源
                if (uri.toString() != current_music_uriString) {
                    initRetrieverMusic(context, uri)
                }

                //截取专辑封面(ByteArray)
                val ByteArray = retriever_music.embeddedPicture

                //检查是否有效
                if (ByteArray == null) {
                    consoleLog("获取专辑封面失败")
                    return@withLock null
                }

                //转换为Bitmap
                var Bitmap = BitmapFactory.decodeByteArray(ByteArray, 0, ByteArray.size)
                //检查图片是否有效
                if (Bitmap != null) {
                    //压缩图片
                    if (needCompress) {
                        val oldBitmap = Bitmap
                        Bitmap = processCenterCropForMusic(Bitmap)
                        if (oldBitmap !== Bitmap) {
                            oldBitmap.recycle()
                        }
                    }
                    /*
                    if (Bitmap == null) {
                        consoleLog("ArtworkCapturerForMusic: 压缩后图片失效,需检查压缩算法")
                        return@withLock null
                    }

                     */

                    return@withLock Bitmap
                } else {
                    consoleLog("ArtworkCapturerForMusic: 初次获取专辑失败了")
                    return@withLock null
                }
            }catch(e: Exception){
                consoleLog("ArtworkCapturer: 截取专辑封面时发生错误: ${e.message}")
                return@withLock null
            }
        }
    }

    //获取占位图
    fun getDefaultAlbumFrame(context: Context): Bitmap? {
        val DefaultBitmap = vectorToBitmapForMusic(context, R.drawable.ic_album_music_album)

        return DefaultBitmap
    }

    //压缩专辑图
    private fun processCenterCropForMusic(src: Bitmap): Bitmap {
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
    private fun vectorToBitmapForMusic(context: Context, @DrawableRes resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null

        val bitmap = createBitmap(100, 100)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", msg)
        }
    }

}