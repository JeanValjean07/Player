package com.suming.player.ListManager

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import data.MediaModel.MediaItemForMusic
import data.MediaModel.MediaItemForVideo
import data.MediaModel.MiniMediaItemForList
import java.io.File
import androidx.core.content.edit

@SuppressLint("StaticFieldLeak")
@Suppress("unused")
object PlayerListManager {

    //列表管理器设置
    private lateinit var PREFS_List: SharedPreferences
    private var state_PREFS_List_Initialized = false
    //当前列表
    private var currentList = -1


    //自定义列表实例
    var customList = mutableListOf<MiniMediaItemForList>()
    //实时视频列表
    var liveVideoList = mutableListOf<MediaItemForVideo>()
    //实时音乐列表
    var liveMusicList = mutableListOf<MediaItemForMusic>()


    //列表操作
    //向自定义列表中插入一项
    fun InsertItemToCustomList(uriString: String, context: Context){
        val item = getMediaInfo(uriString, context)

        if (item == MiniMediaItemForList.EMPTY) return

        if (customList.any { it.uriNumOnly == item.uriNumOnly }) return
        customList.add(item)

    }
    //从自定义列表中删除一项
    fun DeleteItemFromCustomList(uriNumOnly: Long){
        customList.removeAll { it.uriNumOnly == uriNumOnly }

    }
    //整表替换自定义列表
    fun updateCustomList(newList: List<MiniMediaItemForList>) {
        customList.clear()
        customList.addAll(newList)
    }
    //清空自定义列表
    fun clearCustomList(){
        customList.clear()
    }




    //获取下一个或上一个媒体项
    fun getMediaItemByOrder(flag_prev_or_next: String, current_uri_num: Long): Triple<String, Uri, String>    {


        return Triple("", Uri.EMPTY, "")
    }





    //查询某个特定uri是否存在于当前播放列表中
    fun getMediaItemByUri(uriNumOnly: Long): Boolean {
        return when(currentList){
            0 -> {
                customList.any { it.uriNumOnly == uriNumOnly }
            }
            1 -> {
                liveVideoList.any { it.uriNumOnly == uriNumOnly }
            }
            2 -> {
                liveMusicList.any { it.uriNumOnly == uriNumOnly }
            }
            else -> {
                false
            }
        }
    }



    //设置当前播放列表 返回值:是否设置成功
    fun setPlayList(targetList: String): Boolean{
        when(targetList){
            "custom" -> {
                currentList = 0
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            "video" -> {
                currentList = 1
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            "music" -> {
                currentList = 2
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            else -> {
                return false
            }
        }

    }
    fun setPlayList(targetList: Int): Boolean{
        when(targetList){
            0 -> {
                currentList = 0
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            1 -> {
                currentList = 1
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            2 -> {
                currentList = 2
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
                return true
            }
            else -> {
                return false
            }
        }

    }
    fun getCurrentList(context: Context): Int{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //确保数值有效
        if (PREFS_List.contains("PREFS_currentList")){
            currentList = PREFS_List.getInt("PREFS_currentList", 0)
            if (currentList !in 0..2) {
                currentList = 0
                PREFS_List.edit { putInt("PREFS_currentList", currentList) }
            }
        }else{
            PREFS_List.edit { putInt("PREFS_currentList", currentList) }
            currentList = 0
        }

        return currentList
    }

    //向实时视频列表和音乐列表传入内容
    fun InfuseLiveVideoList(newList: List<MediaItemForVideo>){
        liveVideoList.clear()
        liveVideoList.addAll(newList)
    }
    fun InfuseLiveMusicList(newList: List<MediaItemForMusic>){
        liveMusicList.clear()
        liveMusicList.addAll(newList)
    }


    //启动播放列表管理器
    fun startPlayListManage(context: Context){
        //设置上下文
        setContext(context)
        //读取设置
        loadSettings(context)


        state_PlayListManage_started = true
    }
    lateinit var singletonContext: Context
    private var state_PlayListManage_started = false
    private fun setContext(context: Context){
        singletonContext = context
    }
    private fun loadSettings(context: Context){
        PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
        state_PREFS_List_Initialized = true

        if (PREFS_List.contains("PREFS_LoopMode")){
            PREFS_LoopMode = PREFS_List.getString("PREFS_LoopMode", "OFF") ?: "error"
            if (PREFS_LoopMode != "OFF" && PREFS_LoopMode != "ONE" && PREFS_LoopMode != "ALL"){
                PREFS_LoopMode = "OFF"
                PREFS_List.edit{ putString("PREFS_LoopMode", "OFF").apply() }
            }
        }else{
            PREFS_LoopMode = "OFF"
            PREFS_List.edit{ putString("PREFS_LoopMode", "OFF").apply() }
        }

    }


    //循环模式
    var PREFS_LoopMode = ""
    @SuppressLint("StaticFieldLeak")
    fun setLoopMode(mode: String, context: Context) {
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }

        PREFS_LoopMode  = mode
        PREFS_List.edit{ putString("PREFS_LoopMode", PREFS_LoopMode ).apply() }
    }
    fun getLoopMode(context: Context): String{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //过滤无效值
        PREFS_LoopMode  = PREFS_List.getString("PREFS_LoopMode", "OFF") ?: "error"
        if (PREFS_LoopMode != "OFF" && PREFS_LoopMode != "ONE" && PREFS_LoopMode != "ALL"){ PREFS_LoopMode = "OFF" }

        return PREFS_LoopMode
    }


    //媒体信息读取
    private lateinit var retriever: MediaMetadataRetriever
    private var state_MediaMetadataRetriever_Running = false
    private fun getMediaInfo(uriString: String, context: Context): MiniMediaItemForList {
        if (!state_MediaMetadataRetriever_Running) {
            retriever = MediaMetadataRetriever()
            state_MediaMetadataRetriever_Running = true
        }
        //获得基本信息
        val MediaInfo_MediaUri = uriString.toUri()
        val MediaInfo_MediaID = MediaInfo_MediaUri.lastPathSegment?.toLongOrNull() ?: 0
        val MediaInfo_MediaUriNumOnly = MediaInfo_MediaUri.lastPathSegment?.toLongOrNull() ?: 0
        //设置解码器数据源
        try { retriever.setDataSource(context, MediaInfo_MediaUri) }
        catch (e: Exception) {
            Log.e("SuMing", "getMediaInfo: 解码媒体信息失败 $e")
            return MiniMediaItemForList.CREATOR.EMPTY
        }
        //解码更多信息
        val MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "error"
        var MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "error"
        val MediaInfo_MediaPath = getFilePath(context, MediaInfo_MediaUri).toString()
        var MediaInfo_MediaFileName = (File(MediaInfo_MediaPath)).name ?: "error"
        var MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "error"
        //处理和过滤取得值
        if (MediaInfo_MediaType.contains("video")) { MediaInfo_MediaType = "video" }
        else if (MediaInfo_MediaType.contains("audio")) { MediaInfo_MediaType = "music" }
        if (MediaInfo_MediaFileName == "error"){ MediaInfo_MediaFileName = "未知媒体标题" }
        if (MediaInfo_MediaArtist == "error") { MediaInfo_MediaArtist = "未知艺术家" }

        return MiniMediaItemForList(
            id = MediaInfo_MediaID,
            uri = MediaInfo_MediaUri,
            uriNumOnly = MediaInfo_MediaUriNumOnly,
            filename = MediaInfo_MediaFileName,
            title = MediaInfo_MediaTitle,
            artist = MediaInfo_MediaArtist,
            type = MediaInfo_MediaType

        )

    }
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
    private fun stopMediaMetadataRetriever() {
        if (!state_MediaMetadataRetriever_Running) {
            return
        }
        retriever.release()
        state_MediaMetadataRetriever_Running = false


        //MiniMediaItemForList需要包含：
        //val id: Long = 0,
        //val uri: Uri,
        //val uriNumOnly: Long = 0,
        //val filename: String,
        //val title: String = "",
        //val artist: String = "",
        //val type: String = ""


    }

}

//MiniMediaItemForList包含：
//id uri uriNumOnly filename title artist type