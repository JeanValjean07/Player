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
import java.net.URLDecoder
import kotlin.math.absoluteValue

@Suppress("/unused")
class MediaInfoRetriever {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaInfoRetriever: $msg")
        }
    }
    //空字段
    private val Undefined = ""




    //解码 URI (自动检查 URI 类型,返回值在ActivityResultConnector查阅)(传入file_path时就不再获取)
    //传入(URI_S_FP:打算用于播放的链接,仅写入用作后期对照)
    fun retrieveMediaInfo(context: Context, URI_S_FR:String, file_path:String, UriTypeMode_e:String, URI_S_FP:String): Triple<String, MediaItemForPlay, String> {
        var retriever: MediaMetadataRetriever? = null
        //初始化retriever
        fun init_retriever(){
            retriever = MediaMetadataRetriever()
        }
        //释放retriever
        fun release_retriever(){
            retriever?.release()
            retriever = null
        }



        //缓存URI_S_FR
        val URI_U_FR = URI_S_FR.toUri()

        //启动retriever
        init_retriever()

        //尝试设置数据源
        try{
            //检查URI_S是否实际上是一个文件路径
            val is_file_path = MediaUriManager.spy_is_string_actually_a_file_path(URI_S_FR)
            if (is_file_path){
                val file_path = URI_S_FR
                val URI_File = Uri.fromFile(File(file_path))

                val pfd = context.contentResolver.openFileDescriptor(URI_File, "r")
                if (pfd == null) {
                    consoleLog("无法获取文件描述符")
                    return Triple(ActivityResultConnector.retriever_error ,MediaItemForPlay(),Undefined)
                }else{
                    consoleLog("获取文件描述符成功")
                }
                //设置路径为数据源
                pfd.use { fd -> retriever?.setDataSource(fd.fileDescriptor) }

            }else{
                //设置URI为数据源
                retriever?.setDataSource(context,URI_U_FR)
            }

        }catch(e: Exception){
            consoleLog("retrieveMediaInfo -发生错误:e:$e,message:${e.message}")

            return Triple(ActivityResultConnector.retriever_error ,MediaItemForPlay(),Undefined)
        }

        //尝试解码
        try{
            //获取媒体类型
            val MediaInfo_MediaType_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: Undefined
            val MediaInfo_MediaType = when {
                MediaInfo_MediaType_Original.contains("video") -> {
                    MediaType.Video
                }
                MediaInfo_MediaType_Original.contains("audio") -> {
                    MediaType.Audio
                }
                MediaInfo_MediaType_Original.isEmpty() -> Undefined
                else -> MediaInfo_MediaType_Original

            }
            if (MediaInfo_MediaType == Undefined){
                consoleLog("retrieveMediaInfo -使用 URI 解码时 发生错误: 格式获取失败")
                return Triple(ActivityResultConnector.retriever_get_type_failed ,MediaItemForPlay(),Undefined)
            }
            if (MediaInfo_MediaType != MediaType.Video && MediaInfo_MediaType != MediaType.Audio){
                consoleLog("retrieveMediaInfo -使用 URI 解码时 发生错误: 格式获取成功但不支持")
                return Triple(ActivityResultConnector.retriever_type_not_support,MediaItemForPlay(),Undefined)
            }
            //获取 URI 类型
            val UriTypeMode = if (UriTypeMode_e == Undefined){
                //consoleLog("retrieveMediaInfo -未传入 UriTypeMode, 本地尝试获取 UriTypeMode")
                MediaUriManager.detectMediaUriTypeMode(URI_U_FR)
            }else{
                UriTypeMode_e
            }
            //获取绝对路径
            val file_path =  if (file_path == Undefined){
                when(UriTypeMode){
                    MediaUriManager.uriType_media_store_detail -> {
                        GET_FilePath_From_MediaUri_SC1(context,URI_U_FR)
                    }
                    MediaUriManager.uriType_contain_file_path -> {
                        GET_FilePath_From_FileProviderURI(URI_U_FR)
                    }
                    else -> Undefined

                }
            }else{
                file_path
            }


            val MediaInfo_FileName = if((File(file_path)).name.isEmpty()){
                "未知媒体文件名"
            } else {
                (File(file_path)).name ?: "未知媒体文件名"
            }
            val MediaInfo_MediaTitle_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: Undefined
            val MediaInfo_MediaArtist_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: Undefined
            val MediaInfo_Duration = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
            //过滤获取的信息
            val MediaInfo_MediaTitle = if (MediaInfo_MediaTitle_Original == Undefined){
                "未知媒体标题"
            }else{
                MediaInfo_MediaTitle_Original
            }
            val MediaInfo_MediaArtist = if (MediaInfo_MediaArtist_Original == Undefined){
                "未知艺术家"
            }else{
                MediaInfo_MediaArtist_Original
            }
            //视频专属
            val MediaInfo_VideoWidth = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong() ?: 0L
            val MediaInfo_VideoHeight = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong() ?: 0L


            //检查链接格式(必须是标准 MediaStore URI 时才启用 NUM_ID 和 SPECIFIC_ID )
            val URI_STD = if (UriTypeMode == MediaUriManager.uriType_media_store_detail){
                MediaUriManager.GET_STD_MediaStoreURI_from_Any_URI(URI_S_FR,context)
            }else{
                //万一传入链接是个标准链接
                val is_URI_S_MS = MediaUriManager.spy_is_string_matches_a_MediaStore_S_URI(URI_S_FR)
                if (is_URI_S_MS){
                    URI_S_FR
                }else{
                    Undefined
                }
            }
            val NUM_ID = if (URI_STD != Undefined){
                URI_STD.split("/").last().toLong()
            }else{
                if (file_path != Undefined){
                    //TODO 这里有可能跟MediaStore自增ID冲突,只是概率极小
                    file_path.hashCode().absoluteValue.toLong()
                }else{
                    URI_S_FP.hashCode().absoluteValue.toLong()
                }
            }
            val SPECIFIC_ID = if (NUM_ID > 0){
                calculate_SPECIFIC_ID(MediaInfo_MediaType,NUM_ID.toString())
            }else{
                Undefined
            }

            //日志
             // /*
            consoleLog("retrieveMediaInfo -使用 URI 解码 -结果：" +
                    "MediaInfo_MediaType: $MediaInfo_MediaType , " +
                    "file_path: $file_path , " +
                    "MediaInfo_FileName: $MediaInfo_FileName , " +
                    "MediaInfo_Duration: $MediaInfo_Duration , " +
                    "MediaInfo_MediaTitle: $MediaInfo_MediaTitle , " +
                    "MediaInfo_MediaArtist: $MediaInfo_MediaArtist , " +
                    "MediaInfo_VideoWidth: $MediaInfo_VideoWidth , " +
                    "MediaInfo_VideoHeight: $MediaInfo_VideoHeight , " +
                    "URI_STD: $URI_STD , " +
                    "NUM_ID: $NUM_ID , " +
                    "SPECIFIC_ID: $SPECIFIC_ID"
            )
            //   */

            //合成数据包
            val MediaInfoPack = MediaItemForPlay(
                media_api_SPECIFIC_ID = SPECIFIC_ID,
                media_api_NUM_ID = NUM_ID,
                media_api_dateAdded = 0,
                media_SPECIFIC_MediaType = MediaInfo_MediaType,
                URI_S_FP = URI_S_FP,
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

            return Triple(ActivityResultConnector.retriever_complete, MediaInfoPack,UriTypeMode)
        }catch(e: Exception){
            consoleLog("retrieveMediaInfo -使用 URI 解码 发生错误 $e")

            return Triple(ActivityResultConnector.retriever_error, MediaItemForPlay(),Undefined)
        }finally{
            release_retriever()
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
    private val SPECIFIC_ID_SEPARATOR = "_"
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


    //获取文件路径
    //从URI获取文件路径(必须是详情URI 2种方案 1基于contentResolver 2基于)
    fun GET_FilePath_From_MediaUri_SC1(context: Context, uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            val path = cursor?.let {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                it.moveToFirst()
                it.getString(columnIndex)
            }
            return path ?: Undefined
        }catch(e: Exception){
            consoleLog("GET_FilePath_From_MediaUri_SC1 -获取文件路径发生错误: $e")
            Undefined
        }finally{
            cursor?.close()
        }
    }
    fun GET_FilePath_From_MediaUri_SC2(context: Context, uri: Uri): String {
        val cleanUri = if (uri.scheme == null || uri.scheme == "file"){
            Uri.fromFile(File(uri.path?.substringBefore("?") ?: return Undefined))
        }else{
            uri
        }
        val absolutePath: String? = when (cleanUri.scheme){
            ContentResolver.SCHEME_CONTENT -> {
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                context.contentResolver.query(cleanUri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) else null
                }
            }
            ContentResolver.SCHEME_FILE    -> cleanUri.path
            else                           -> cleanUri.path
        }

        return absolutePath?.takeIf { File(it).exists() } ?: Undefined
    }
    //从FileProviderURI获取文件路径
    fun GET_FilePath_From_FileProviderURI(URI: Uri): String {
        try{
            val path = URI.path ?: return MediaUriManager.Undefined

            val file_path = path.replace("/root", "") // 移除 /root 前缀
            val storageIndex = path.indexOf("/storage/emulated/")
            if (storageIndex != -1) {
                return path.substring(storageIndex)
            }

            return try {
                URLDecoder.decode(path, "UTF-8")
            }catch(e: Exception){
                consoleLog("GET_FilePath_From_FileProviderURI -解码URL编码的路径失败: $e")
                path
            }

        }catch(e: Exception){
            consoleLog("GET_FilePath_From_FileProviderURI -获取文件路径失败: $e")

            return MediaUriManager.Undefined
        }
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