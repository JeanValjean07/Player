package com.suming.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {
    //本项目仓库路径
    const val githubRepository = "JeanValjean07/Player"

    //OkHttpClient实例
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    //发送请求
    fun request(request: Request): Response {
        return client.newCall(request).execute()
    }



    //下载文件核心函数
    suspend fun download_File_Core(url: String, destFile: File, onProgress: (Float) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        try {

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "identity") // 禁用压缩，确保 Content-Length
                .build()

            val response = client.newCall(request).execute()

            //检查响应
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("下载失败: ${response.code} ${response.message}"))
            }

            val body = response.body

            //获取文件大小 contentLength 可能为 -1,此时无法支持进度显示
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()

            //确保目录存在
            destFile.parentFile?.mkdirs()
            val outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val progress = totalBytesRead.toFloat() / contentLength
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            //关闭流
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            response.close()

            Result.success(destFile)
        }catch(e: Exception){
            Result.failure(e)
        }
    }


    //下载Apk
    suspend fun download_S_Apk(context: Context, applicationPackage: ApplicationPackage, onProgress: (Float) -> Unit = {}): Result<File> {
        //文件名
        val fileName = "${applicationPackage.version}.apk"

        //目标下载路径
        val downloadDirPath = applicationPackage.downloadDir.toPath()
        //检查目录是否可访问



        //目标文件
        val destFile = File(context.getExternalFilesDir(downloadDirPath), fileName)
        //检查目标文件夹的可读性






        //下载地址
        val url = applicationPackage.url


        return download_File_Core(url, destFile, onProgress)
    }

    data class ApplicationPackage(
        val url: String,
        val version: String,
        val md5: String,
        //期望的下载目录
        val downloadDir: DownloadDirType,

    )

    //目标下载路径
    enum class DownloadDirType {
        APP_INTERNAL,     //App内部私有目录 (//data/data/com.suming.player)
        APP_EXTERNAL,     //App外部私有目录 (emulated/0/Android/data/com.suming.player/files)
        PUBLIC_DOWNLOADS,  //公共Downloads目录
        PUBLIC_DOCUMENTS,  //公共Documents目录
        PUBLIC_MEDIA,      //公共媒体目录
    }
    fun String.toDownloadDirType(): DownloadDirType {
        return when (this.lowercase()) {
            "app_internal" -> DownloadDirType.APP_INTERNAL
            "app_external" -> DownloadDirType.APP_EXTERNAL
            "public_downloads" -> DownloadDirType.PUBLIC_DOWNLOADS
            "public_documents" -> DownloadDirType.PUBLIC_DOCUMENTS
            "public_media" -> DownloadDirType.PUBLIC_MEDIA
            else -> DownloadDirType.PUBLIC_DOWNLOADS
        }
    }
    fun DownloadDirType.toPath(): String {
        return when (this) {
            DownloadDirType.APP_INTERNAL -> "downloads"
            DownloadDirType.APP_EXTERNAL -> "downloads"
            DownloadDirType.PUBLIC_DOWNLOADS -> "downloads"
            DownloadDirType.PUBLIC_DOCUMENTS -> "documents"
            DownloadDirType.PUBLIC_MEDIA -> "media"
        }
    }





}