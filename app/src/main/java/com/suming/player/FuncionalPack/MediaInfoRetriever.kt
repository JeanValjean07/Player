package com.suming.player.FuncionalPack

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.suming.player.DataPack.MediaInfo
import java.io.File

object MediaInfoRetriever {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaInfoRetriever: $msg")
        }
    }

    //解码器
    private var retriever: MediaMetadataRetriever? = null

    //初始化解码器
    fun initRetriever() {
        if (retriever != null) {
            return
        }
        retriever = MediaMetadataRetriever()
    }
    //释放解码器
    fun releaseRetriever() {
        if (retriever == null) {
            return
        }
        retriever?.release()
        retriever = null
    }
    //链接缓存清除(含关闭解码器)
    private var current_uriString = ""
    fun clearRetrieverUriCache(){
        releaseRetriever()
        current_uriString = ""
    }
    //为解码器设置链接
    fun setMediaUri(context: Context, uri: Uri): Boolean {
        if (retriever == null) {
            initRetriever()
        }
        if (current_uriString == uri.toString()) {
            return true
        }
        try{
            retriever?.setDataSource(getFilePath(context, uri).toString())
            current_uriString = uri.toString()

            return true
        }catch (e: Exception){
            current_uriString = ""
            consoleLog("setMediaUri - setDataSource 时发生错误: $e")
            return false
        }

    }



    //解码一个媒体
    fun retrieveMediaInfo(context: Context, MediaInfo_MediaUri: Uri): Pair<Boolean,MediaInfo> {
        consoleLog("retrieveMediaInfo - 需要解码 MediaInfo_MediaUri: $MediaInfo_MediaUri")
        //解码器初始化
        if(retriever == null) initRetriever()

        //设置数据源
        var success = false
        if(current_uriString != MediaInfo_MediaUri.toString()) {
            consoleLog("retrieveMediaInfo -成功开始解码 MediaInfo_MediaUri: $MediaInfo_MediaUri")
            success = setMediaUri(context, MediaInfo_MediaUri)
        }else{
            success = true
        }

        //开始解码信息
        if(success) {
            val (MediaInfo_MediaUniqueID,MediaInfo_MediaUriStandard) = calculateUniqueID(context,MediaInfo_MediaUri)
            val MediaInfo_MediaUriString = MediaInfo_MediaUri.toString()
            val MediaInfo_MediaUriNumOnly = MediaInfo_MediaUniqueID.toLong()
            //
            var MediaInfo_MediaType = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            val MediaInfo_AbsolutePath = getFilePath(context, MediaInfo_MediaUri).toString()
            val MediaInfo_FileName = (File(MediaInfo_AbsolutePath)).name ?: ""
            var MediaInfo_MediaTitle = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            var MediaInfo_MediaArtist = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            //
            val MediaInfo_Duration = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
            //
            val MediaInfo_VideoWidth = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val MediaInfo_VideoHeight = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            //计算数据库专用ID
            val MediaInfo_DataBaseID = BuildItemID(MediaInfo_MediaType,MediaInfo_MediaUriNumOnly.toString())


            //过滤获取的信息
            if (MediaInfo_MediaType.contains("video")){
                MediaInfo_MediaType = MediaTypeCenter.mediaType_Video
            }else if(MediaInfo_MediaType.contains("audio")){
                MediaInfo_MediaType = MediaTypeCenter.mediaType_Music
            }
            if (MediaInfo_MediaTitle == ""){ MediaInfo_MediaTitle = "未知媒体标题" }
            if (MediaInfo_MediaArtist == "" || MediaInfo_MediaArtist == "<unknown>"){ MediaInfo_MediaArtist = "未知艺术家" }


            //合成数据包
            val MediaInfoPack = MediaInfo(
                MediaInfo_MediaUniqueID = MediaInfo_MediaUniqueID,
                MediaInfo_DataBaseID = MediaInfo_DataBaseID,
                MediaInfo_MediaUri = MediaInfo_MediaUri,
                MediaInfo_MediaUriString = MediaInfo_MediaUriString,
                MediaInfo_MediaUriStandard = MediaInfo_MediaUriStandard,
                MediaInfo_MediaUriNumOnly = MediaInfo_MediaUriNumOnly,

                MediaInfo_MediaType = MediaInfo_MediaType,
                MediaInfo_FileName = MediaInfo_FileName,
                MediaInfo_AbsolutePath = MediaInfo_AbsolutePath,
                MediaInfo_MediaTitle = MediaInfo_MediaTitle,
                MediaInfo_MediaArtist = MediaInfo_MediaArtist,

                MediaInfo_Duration = MediaInfo_Duration,
                MediaInfo_Video_Width = MediaInfo_VideoWidth,
                MediaInfo_Video_Height = MediaInfo_VideoHeight,

            )

            return Pair(true, MediaInfoPack)

        }else{
            val MediaInfoEmptyPack = MediaInfo(
                MediaInfo_MediaUniqueID = "",
                MediaInfo_DataBaseID = "",
                MediaInfo_MediaUri = MediaInfo_MediaUri,
                MediaInfo_MediaUriString = "",
                MediaInfo_MediaUriStandard = "",
                MediaInfo_MediaUriNumOnly = 0L,

                MediaInfo_MediaType = "",
                MediaInfo_FileName = "",
                MediaInfo_AbsolutePath = "",
                MediaInfo_MediaTitle = "",
                MediaInfo_MediaArtist = "",

                MediaInfo_Duration = 0L,
                MediaInfo_Video_Width = 0,
                MediaInfo_Video_Height = 0,
            )
            return Pair(false, MediaInfoEmptyPack)
        }
    }


    //检查链接是否能解码
    fun isUriStringValid(context: Context,uriString: String): Boolean{
        val retriever = MediaMetadataRetriever()
        try{
            val uri = uriString.toUri()
            retriever.setDataSource(context,uri)
        }
        catch (_: Exception){
            return false
        }
        finally {
            retriever.release()
        }

        return true
    }

    //快速检查链接是否有效并返回媒体类型
    fun getUriStringMediaType(context: Context,uriString: String): Pair<Boolean,String>{
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
            return Pair(true,MediaTypeCenter.mediaType_Video)
        }else if(MediaInfo_MediaType.contains("audio")){
            consoleLog("获取到媒体类型 music")
            return Pair(true,MediaTypeCenter.mediaType_Music)
        }else{
            consoleLog("获取到非法媒体类型")
            return Pair(false,"")
        }
    }




    //工具函数 - 根据uri获得绝对路径
    private fun getFilePath(context: Context, uri: Uri): String? {
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
    //工具函数 -
    private fun calculateUniqueID(context: Context,mediaUri: Uri):Pair<String,String>{
        //计算媒体唯一识别ID
        val MediaInfo_MediaStoreID = MediaUriManager.getMediaIDByMediaUri(mediaUri,context)
        //获取标准链接
        val MediaInfo_MediaUriStandard = MediaUriManager.getStandardMediaUri(mediaUri,context)

        return Pair(MediaInfo_MediaStoreID,MediaInfo_MediaUriStandard.toString())
    }
    //合成数据库专用ID(使用mediaType_uriNumOnly,例 video114514)
    fun BuildItemID(type: String, uriNumOnly: String): String{

        return "${type}_${uriNumOnly}"
    }


}