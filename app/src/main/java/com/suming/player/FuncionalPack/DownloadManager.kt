package com.suming.player.FuncionalPack

import android.content.Context
import android.os.Environment
import android.util.Log
import com.suming.player.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object DownloadManager {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "DownloadManager: $msg")
        }
    }



    //下载Apk
    suspend fun downloadApk(context:Context,url:String,version:String,onProgress:(Float)->Unit):Result<File> =withContext(Dispatchers.IO) {
        try {
            //TODO 复用到独立客户端里
            val client = NetworkClient.client

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "identity") //禁用压缩,确保能获取 Content-Length
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("下载失败: ${response.code} ${response.message}"))
            }

            val body = response.body

            //获取文件大小
            val contentLength = body.contentLength()
            if (contentLength <= 0) {
                //可能仍可下载
                consoleLog("无法获取Content-Length")
            }

            //在目标路径准备文件占位
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            //获取App名称
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val appName = context.packageManager.getApplicationLabel(applicationInfo)
            val fileName = "${appName}_${version}.apk"
            val destFile = File(downloadDir, fileName)
            destFile.parentFile?.mkdirs()

            //开始下载
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destFile)  //存在同一个已存在文件时,覆盖
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                //回调进度
                if (contentLength > 0) {
                    val progress = totalBytesRead.toFloat() / contentLength
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            //清理资源
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            response.close()

            Result.success(destFile)
        }catch(e: Exception){
            consoleLog("下载失败: ${e.message}")
            Result.failure(e)
        }
    }
}