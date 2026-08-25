package com.suming.player.FuncionalPack

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.suming.player.DataPack.DataClassForPlay.MediaItemForPlay
import java.io.File

@Suppress() //"unused",
object MediaInfoRetriever {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaInfoRetriever: $msg")
        }
    }

    const val Undefined = ""

    //解码器
    private var retriever: MediaMetadataRetriever? = null







    //解码一个媒体
    fun retrieveMediaInfo(file_path:String = Undefined,uriString:String = Undefined,context: Context): Pair<Boolean, MediaItemForPlay> {
        //解码器初始化
        if(retriever == null) retriever = MediaMetadataRetriever()
        //数据补全
        var Media_UriString = uriString
        var file_path = file_path
        if (file_path == Undefined && uriString == Undefined){

            return Pair(false, MediaItemForPlay())
        }else if (file_path == Undefined){

            //根据uriString获取文件路径
            file_path = GET_FilePath_From_MediaUri(context, Media_UriString.toUri()) ?: return Pair(false, MediaItemForPlay())
            consoleLog("retrieveMediaInfo-根据uriString获取文件路径: $file_path")
            //字段示例
            //1./storage/emulated/0/Pictures/音乐视频/Dancin.mp4
            //2./storage/emulated/0/Pictures/音乐视频/Dancin.mp4

        }else if(uriString == Undefined){

            //根据文件路径获取uriString
            Media_UriString = GET_MediaUri_From_FilePath(context, file_path) ?: return Pair(false, MediaItemForPlay())
            consoleLog("retrieveMediaInfo-根据文件路径获取uriString: $Media_UriString")

        }


        //设置数据源
        var use_file_path_success = false
        try{
            retriever?.setDataSource(file_path)

            use_file_path_success = true

        }catch(e: Exception){
            consoleLog("retrieveMediaInfo-setMediaUri-setDataSource -file_path 发生错误: $e  , ${e.message} ${e.cause} ${e.stackTrace} ")

            use_file_path_success = false
        }
        if (!use_file_path_success){
            try{
                retriever?.setDataSource(context,Media_UriString.toUri())


            }catch(e: Exception){
                consoleLog("retrieveMediaInfo-setMediaUri-setDataSource - uriString 发生错误: $e , ${e.message} ${e.cause} ${e.stackTrace} ")

                return Pair(false, MediaItemForPlay())
            }
        }


        //解码
        try{
            val Media_UriString = uriString
            //确保链接标准
            val Media_UriStandard = MediaUriManager.GET_StandardMediaUri(Media_UriString,context)
            //截取NUM_ID
            val Media_NUM_ID = Media_UriStandard.split("/").last().toLong()
            //日志
            /*consoleLog("retrieveMediaInfo-截取" +
                    "Media_UriString: $Media_UriString" +
                    "Media_UriStandard: $Media_UriStandard" +
                    "Media_NUM_ID: $Media_NUM_ID"
            )

             */

            //获取媒体类型
            var MediaInfo_MediaType = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            if (MediaInfo_MediaType.contains("video")){
                MediaInfo_MediaType = MediaType.Video
            }else if(MediaInfo_MediaType.contains("audio")){
                MediaInfo_MediaType = MediaType.Audio
            }else{

                return Pair(false, MediaItemForPlay())
            }
            //合成SPECIFIC_ID
            val MediaInfo_SPECIFIC_ID = MediaInfo_MediaType + "_" + Media_NUM_ID

            //获取通用信息
            val MediaInfo_FileName = (File(file_path)).name ?: ""
            var MediaInfo_MediaTitle = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            var MediaInfo_MediaArtist = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            val MediaInfo_Duration = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L

            //过滤获取的信息
            if (MediaInfo_MediaTitle == ""){ MediaInfo_MediaTitle = "未知媒体标题" }
            if (MediaInfo_MediaArtist == "" || MediaInfo_MediaArtist == "<unknown>"){ MediaInfo_MediaArtist = "未知艺术家" }

            //视频专属
            val MediaInfo_VideoWidth = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong() ?: 0L
            val MediaInfo_VideoHeight = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong() ?: 0L





            //合成数据包
            val MediaInfoPack = MediaItemForPlay(
                media_api_SPECIFIC_ID = MediaInfo_SPECIFIC_ID,
                media_api_NUM_ID = Media_NUM_ID,
                media_api_dateAdded = 0,
                media_SPECIFIC_MediaType = MediaInfo_MediaType,
                content_uriString = Media_UriString,
                content_uriStandard = Media_UriStandard,
                file_path = file_path,
                file_name = MediaInfo_FileName,
                file_size = 0L,
                media_title = MediaInfo_MediaTitle,
                media_artist = MediaInfo_MediaArtist,
                media_durationMs = MediaInfo_Duration,
                media_format = "",

                //类型专属
                video_videoHeight = MediaInfo_VideoHeight,
                video_videoWidth = MediaInfo_VideoWidth,
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


    //快速检查链接是否能解码并返回媒体类型
    fun getUriValidAndMediaType(context: Context,uriString: String): Pair<Boolean,String>{
        val retriever = MediaMetadataRetriever()
        //尝试解码
        try{
            val uri = uriString.toUri()
            retriever.setDataSource(context,uri)

            var MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

            if (MediaInfo_MediaType.contains("video")){
                MediaInfo_MediaType = MediaType.Video
            }else if(MediaInfo_MediaType.contains("audio")){
                MediaInfo_MediaType = MediaType.Audio
            }else{

                return Pair(false,"")
            }


            return Pair(true, MediaInfo_MediaType)
        }catch(_: Exception){

            return Pair(false,"")
        }finally{
            retriever.release()
        }
    }
    fun getUriValidAndMediaType(file_path: String): Pair<Boolean,String>{
        val retriever = MediaMetadataRetriever()
        //尝试解码
        try{
            retriever.setDataSource(file_path)

            var MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

            if (MediaInfo_MediaType.contains("video")){
                MediaInfo_MediaType = MediaType.Video
            }else if(MediaInfo_MediaType.contains("audio")){
                MediaInfo_MediaType = MediaType.Audio
            }else{

                return Pair(false,"")
            }


            return Pair(true, MediaInfo_MediaType)
        }catch(_: Exception){

            return Pair(false,"")
        }finally{
            retriever.release()
        }
    }

    //检查链接对应媒体是否还存在(MediaStore)
    fun isMediaExist(context: Context,uri: Uri): Boolean{
        var cursor: Cursor? = null
        return try {

            val projection = arrayOf(MediaStore.MediaColumns._ID)
            cursor = context.contentResolver.query(uri, projection, null, null, null)

            //返回查询结果是否为空
            (cursor?.count ?: 0) > 0

        }catch(e: Exception){
            consoleLog("isMediaExist-查询媒体ID发生错误: $e")

            false
        }finally{
            cursor?.close()
        }
    }
    //检查对应文件是否还存在
    fun isFileExist(file_path: String): Boolean{
        val file = File(file_path)

        return file.exists() && file.isFile
    }




    //SPECIFIC_ID 计算器
    const val SPECIFIC_ID_SEPARATOR = "_"
    fun calculate_SPECIFIC_ID(mediaType: String, mediaNUMID: String): String{

        return "${mediaType}${SPECIFIC_ID_SEPARATOR}${mediaNUMID}"
    }
    //SPECIFIC_ID 拆解器
    fun split_SPECIFIC_ID(SPECIFIC_ID: String): Pair<String,Long>{
        try {
            val mediaType = SPECIFIC_ID.substringBefore(SPECIFIC_ID_SEPARATOR)
            val mediaNUMID = SPECIFIC_ID.substringAfter(SPECIFIC_ID_SEPARATOR).toLong()

            return Pair(mediaType,mediaNUMID)
        }catch(e: Exception){
            consoleLog("split_SPECIFIC_ID-拆解SPECIFIC_ID发生错误: $e")
            return Pair("",0)
        }
    }


    //获取文件路径(基于contentResolver)
    fun GET_FilePath_From_MediaUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.let {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                it.moveToFirst()
                it.getString(columnIndex)
            }
        }catch(e: Exception){
            consoleLog("GET_FilePath_From_MediaUri-获取文件路径发生错误: $e")
            null
        }finally{
            cursor?.close()
        }
    }
    private fun GET_FilePath_From_MediaUri_Absolute(context: Context, uri: Uri): String? {
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
    //获取Uri
    private fun GET_MediaUri_From_FilePath(context: Context, filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null

        //从MediaStore中查找
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = MediaStore.MediaColumns.DATA + " = ?"
        val selectionArgs = arrayOf(filePath)

        //图片
        var uri = queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs)
        if (uri != null) return uri

        //视频
        uri = queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs)
        if (uri != null) return uri

        //音频
        uri = queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs)

        return uri
    }
    private fun queryMediaStore(context: Context, contentUri: Uri, projection: Array<String>, selection: String, selectionArgs: Array<String>): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(contentUri, projection,
                selection, selectionArgs, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(contentUri, id).toString()
                }
            }
            null
        }catch(e: Exception){
            consoleLog("queryMediaStore-查询MediaStore发生错误: $e")
            null
        }finally{
            cursor?.close()
        }
    }


}