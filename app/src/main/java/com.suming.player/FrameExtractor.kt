package com.suming.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


interface FrameListener {
    //完成一帧
    fun onFrameExtracted(bitmap: Bitmap, presentationTimeUs: Long)
    //完成全部
    fun onExtractionFinished()
    //发生错误
    fun onExtractionError(message: String)
}


class FrameExtractor(private val listener: FrameListener) {

    //统一日志标签
    private val TAG = "SuMing"
    //超时时间
    private val TIMEOUT_US = 100_000_000L
    private val TARGET_FPS = 60
    //协程作用域
    private val coroutineScopeExtraction = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val imageAvailable = AtomicBoolean(false)
    private var currentImage: Image? = null


    //开始提取入口
    fun startExtraction(videoPath: String) {
        coroutineScopeExtraction.launch {
            extractFramesInternal(videoPath)
        }
    }



    //提取帧方法
    private fun extractFramesInternal(videoPath: String) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        try {

            //Extractor初始化+设置数据源
            extractor = MediaExtractor()
            extractor.setDataSource(videoPath)

            //搜索视频轨道
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    videoFormat = format
                    Log.d("SuMing", "搜索 视频轨道已找到：轨道${i}是视频轨道,格式$format")
                    break
                }
            }
            Log.d("SuMing", "搜索 文件内轨道数量： ${extractor.trackCount}")

            if (videoTrackIndex == -1 || videoFormat == null) {
                Log.d("SuMing", "文件内没找到视频轨道")
                listener.onExtractionError("未找到视频轨道")
                return
            }

            Log.d("SuMing", "搜索 视频轨道 完成 已找到视频轨道：$videoTrackIndex, 格式：$videoFormat")

            //Extractor选择视频轨道
            extractor.selectTrack(videoTrackIndex)

            // 获取视频尺寸
            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

            Log.d("SuMing", "视频轨道：视频尺寸 $width x $height")

            //初始化ImageReader
            initImageReader(width, height, videoFormat)

            Log.d("SuMing", "初始化ImageReader 完成")

            //初始化MediaCodec
            val mimeType = videoFormat.getString(MediaFormat.KEY_MIME)!!
            decoder = MediaCodec.createDecoderByType(mimeType)
            decoder.configure(videoFormat, imageReader?.surface, null, 0)
            decoder.start()

            Log.d("SuMing", "初始化MediaCodec 完成")


            //解码循环
            val bufferInfo = MediaCodec.BufferInfo()
            var isExtractorEOS = false // 提取器是否到达文件末尾
            var isDecoderEOS = false   // 解码器是否到达文件末尾
            var frameCount = 0
            val desiredIntervalUs = 1_000_000L / TARGET_FPS
            var nextFrameTimeUs: Long = 0
            //解码循环
            while (!isDecoderEOS) {

                if (!isExtractorEOS) {

                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)

                    if (inputBufferIndex >= 0) {

                        val inputBuffer: ByteBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        val presentationTimeUs = extractor.sampleTime

                        if (sampleSize < 0) {
                            //标记输入流结束
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isExtractorEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when (outputBufIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed: " + decoder.outputFormat)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {  }
                    else -> {
                        if (outputBufIndex >= 0) {
                            //检查末尾帧信号
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                isDecoderEOS = true
                                Log.d("SuMing", "解码器：已运行到末尾帧")
                            }
                            //帧操作
                            //当前帧的时间戳
                            val presentationTimeUs = bufferInfo.presentationTimeUs
                            Log.d("SuMing", "解码器：取出了一帧，时间戳：${presentationTimeUs/1_000_000.0}秒")
                            //帧抽取逻辑

                                //ImageReader抽取Bitmap
                                val bitmap = getBitmapFromImage()
                                //处理Bitmap
                                if (bitmap != null) {
                                   // Log.d("SuMing", "目标时间：$nextFrameTimeUs  抽帧成功")
                                    listener.onFrameExtracted(bitmap, presentationTimeUs)
                                    frameCount++
                                    // 更新下一个抽取时间
                                    // 优化后
                                    nextFrameTimeUs = presentationTimeUs + desiredIntervalUs
                                    
                                }else{
                                    Log.d("SuMing", "$presentationTimeUs  抽帧失败")
                                }

                            //必须释放解码器的输出缓冲区
                            decoder.releaseOutputBuffer(outputBufIndex, true)
                        }
                    }
                }

            }

            //完成循环
            listener.onExtractionFinished()
        }
        catch (e: Exception) {
            Log.w("SuMing", "catch: 捕获错误信息：${e.message}")
            listener.onExtractionError("帧提取发生错误: ${e.message}")
            e.printStackTrace()
        }
        finally {
            Log.w("SuMing", "finally: 释放资源")
            releaseImageResources()
            decoder?.stop()
            decoder?.release()
            extractor?.release()
        }
    }

    //初始化ImageReader
    private fun initImageReader(width: Int, height: Int , videoFormat: MediaFormat) {
        //maxImages探测
        var maxImages = 12
        var imageReaderTest: ImageReader? = null
        for (i in maxImages downTo 1) {
            try {
                imageReaderTest = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, maxImages)
                val mimeType = videoFormat.getString(MediaFormat.KEY_MIME)!!
                val decoder = MediaCodec.createDecoderByType(mimeType)
                decoder.configure(videoFormat, imageReaderTest.surface, null, 0)
                decoder.start()
                //Log.w("SuMing", "maxImages=$maxImages 测试成功")
                break
            }
            catch (_: Exception) {
                //Log.w("SuMing", "maxImages=$maxImages 测试失败")
                maxImages = maxImages - 1
            }
            finally {
                imageReaderTest?.close()
            }
        }

        Log.w("SuMing", "最终创建ImageReader: maxImages=$maxImages")
        //创建ImageReader
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, maxImages)


        // 创建HandlerThread来处理ImageReader的回调
        handlerThread = HandlerThread("ImageReaderThread")
        handlerThread?.start()
        handler = Handler(handlerThread?.looper!!)
        // 设置ImageReader的回调
        imageReader?.setOnImageAvailableListener({ reader ->
            try {
                currentImage?.close()
                currentImage = reader.acquireLatestImage()
                imageAvailable.set(true)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }, handler)
    }


    private fun getBitmapFromImage(): Bitmap? {
        if (!imageAvailable.get()) {
            val startTime = System.currentTimeMillis()
            // 恢复等待机制并添加适当的超时处理
            while (!imageAvailable.get() && System.currentTimeMillis() - startTime < 50) {
                Thread.sleep(5)
            }
            if (!imageAvailable.get()) return null
        }

        val image = currentImage ?: return null

        try { return yuv420888ToBitmap(image) }
        catch (e: Exception) {
            Log.d("SuMing", "getBitmapFromImage: 捕获错误信息：${e.message}")
            e.printStackTrace()
            return null
        }
        finally {
            image.close()
            imageAvailable.set(false)
            currentImage = null
        }
    }

    private fun yuv420888ToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val yuv = out.toByteArray()

        return BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
    }



    private fun releaseImageResources() {
        currentImage?.close()
        currentImage = null

        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null

        handlerThread?.quitSafely()
        try {
            handlerThread?.join(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        handlerThread = null
        handler = null
    }


}