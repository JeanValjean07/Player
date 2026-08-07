package com.suming.player.FuncionalPack

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.suming.player.DataPack.DataClassForPlay.MediaItemForPlay
import com.suming.player.DataPack.MediaInfo
import java.io.File
import kotlin.String

object MediaInfoRetriever {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaInfoRetriever: $msg")
        }
    }


    //解码器
    private var retriever: MediaMetadataRetriever? = null







    //解码一个媒体
    fun retrieveMediaInfo(file_path: String,uriString: String): Pair<Boolean, MediaItemForPlay> {
        //解码器初始化
        if(retriever == null) retriever = MediaMetadataRetriever()
        //设置数据源
        try{
            retriever?.setDataSource(file_path)

        }catch(e: Exception){
            consoleLog("retrieveMediaInfo-setMediaUri-setDataSource发生错误: $e")
            return Pair(false, MediaItemForPlay())
        }

        //解码
        try{
            val Media_UriString = uriString
            //确保链接标准

            //获取媒体类型
            var MediaInfo_MediaType = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            if (MediaInfo_MediaType.contains("video")){
                MediaInfo_MediaType = MediaType.Video
            }else if(MediaInfo_MediaType.contains("audio")){
                MediaInfo_MediaType = MediaType.Audio
            }else{

                return Pair(false, MediaItemForPlay())
            }

            //获取通用信息
            val MediaInfo_FileName = (File(file_path)).name ?: ""
            var MediaInfo_MediaTitle = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            var MediaInfo_MediaArtist = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            val MediaInfo_Duration = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L


            //视频专属
            val MediaInfo_VideoWidth = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val MediaInfo_VideoHeight = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0



            //过滤获取的信息

            if (MediaInfo_MediaTitle == ""){ MediaInfo_MediaTitle = "未知媒体标题" }
            if (MediaInfo_MediaArtist == "" || MediaInfo_MediaArtist == "<unknown>"){ MediaInfo_MediaArtist = "未知艺术家" }


            //合成数据包
            val MediaInfoPack = MediaItemForPlay(
                media_api_SPECIFIC_ID = "",
                media_api_NUM_ID = 0,
                media_api_dateAdded = 0,
                media_SPECIFIC_MediaType = "",
                content_uriString = "",
                content_uriStandard = "",
                file_path = "",
                file_name = "",
                file_size = 0L,
                media_title = "",
                media_artist = "",
                media_durationMs = 0L,
                media_format = "",

                //类型专属
                video_videoHeight = 0L,
                video_videoWidth = 0L,
                video_actualFPS = 0f,

            )

            return Pair(true, MediaInfoPack)
        }catch(e: Exception){
            consoleLog("retrieveMediaInfo-setMediaUri-解码发生错误: $e")

            return Pair(false, MediaItemForPlay())
        }finally{
            retriever = null
        }
    }


    //检查链接是否能解码
    fun isUriStringValid(context: Context,uriString: String): Boolean{
        val retriever = MediaMetadataRetriever()
        //尝试解码
        try{
            val uri = uriString.toUri()
            retriever.setDataSource(context,uri)

            return true
        }catch(_: Exception){

            return false
        }finally{
            retriever.release()
        }
    }

    //快速检查链接是否有效并返回媒体类型
    fun getUriValidAndMediaType(context: Context,uriString: String): Pair<Boolean,String>{
        val (success, MediaInfoPack) = retrieveMediaInfo(context,uriString.toUri())
        if (!success){
            consoleLog("解码失败")
            return Pair(false,"")
        }
        //获取媒体类型
        val MediaInfo_MediaType = MediaInfoPack.MediaInfo_MediaType
        //过滤获取的信息
        if (MediaInfo_MediaType.contains("video")){
            consoleLog("获取到媒体类型 video")
            return Pair(true,MediaType.Video)
        }else if(MediaInfo_MediaType.contains("audio")){
            consoleLog("获取到媒体类型 audio")
            return Pair(true,MediaType.Audio)
        }else{
            consoleLog("获取到非法媒体类型")
            return Pair(false,"")
        }
    }



    //SPECIFIC_ID 计算器
    const val SPECIFIC_ID_SEPARATOR = "_"
    fun calculate_SPECIFIC_ID(mediaType: String, mediaNUMID: String): String{

        return "${mediaType}${SPECIFIC_ID_SEPARATOR}${mediaNUMID}"
    }


    //工具函数-根据uri获得绝对路径
    private fun GET_FilePath(context: Context, uri: Uri): String? {
        val cleanUri = if (uri.scheme == null || uri.scheme == "file") {
            Uri.fromFile(File(uri.path?.substringBefore("?") ?: return null))
        } else {
            uri
        }
        val absolutePath: String? = when (cleanUri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                context.contentResolver.query(cleanUri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) else null
                }
            }
            ContentResolver.SCHEME_FILE    -> cleanUri.path
            else                           -> cleanUri.path
        }

        return absolutePath?.takeIf { File(it).exists() }
    }


}