package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.suming.player.DataPack.MediaModel.MediaItemForMusic
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.DataPack.MediaModel.MiniMediaItemForList
import java.io.File

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



    //默认页签
    private var PREFS_AcquiescePage = -2
    fun get_PREFS_AcquiescePage(context: Context): Int{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //过滤无效值
        PREFS_AcquiescePage = PREFS_List.getInt("PREFS_AcquiescePage", -2)
        if (PREFS_AcquiescePage == -2){
            PREFS_List.edit{ putInt("PREFS_AcquiescePage", 0) }
            PREFS_AcquiescePage = 0
        }

        return PREFS_AcquiescePage
    }
    fun set_PREFS_AcquiescePage(context: Context, page: Int): Boolean{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //过滤无效值
        PREFS_AcquiescePage = page
        PREFS_List.edit{ putInt("PREFS_AcquiescePage", page) }

        return true
    }
    //上一次的页面
    private var state_LastPageSign = -1
    fun get_state_LastPageSign(context: Context): Int{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //过滤无效值
        state_LastPageSign = PREFS_List.getInt("state_LastPageSign", -1)

        if (state_LastPageSign == -1){
            PREFS_List.edit{ putInt("state_LastPageSign", 0) }
            state_LastPageSign = 0
        }

        return state_LastPageSign
    }
    fun set_state_LastPageSign(context: Context, page: Int): Boolean{
        //确保已初始化设置清单
        if (!state_PREFS_List_Initialized){
            PREFS_List = context.getSharedPreferences("PREFS_List", MODE_PRIVATE)
            state_PREFS_List_Initialized = true
        }
        //过滤无效值
        state_LastPageSign = page
        PREFS_List.edit{ putInt("state_LastPageSign", page) }

        return true
    }








    //播放列表
    /*
    private val coroutineScope_getPlayList = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var PREFS_MediaStore: SharedPreferences
    private lateinit var mediaItemsMutableSnapshot: SnapshotStateList<MediaItemForVideo>
    private var emptyList = emptyList<MediaItemForVideo>().toMutableStateList()
    private var currentMediaIndex = 0
    private var maxMediaIndex = 0
    private var state_MediaListProcess_complete = false
    private var MediaInfo_VideoUri: Uri = Uri.EMPTY
    private fun getMediaListFromDataBase(context: Context){
        //已读取列表,不再重复读取
        if (state_MediaListProcess_complete){ return }
        //读取播放列表
        coroutineScope_getPlayList.launch(Dispatchers.IO) {
            //读取设置
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore",
                Context.MODE_PRIVATE
            )
            val sortOrder = PREFS_MediaStore.getString("PREFS_SortOrder", "info_title") ?: "info_title"
            val sortOrientation = PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") ?: "DESC"
            //读取所有媒体
            val mediaStoreRepo = MediaStoreRepo.Companion.get(context)
            val mediaStoreSettings = mediaStoreRepo.getAllVideosSorted(sortOrder, sortOrientation)
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_MediaUniqueID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_MediaUniqueID.toLongOrNull() ?: 0,
                        filename = setting.info_filename,
                        title = setting.info_title,
                        artist = setting.info_artist,
                        durationMs = setting.info_duration,
                        //视频专属
                        res = setting.info_resolution,
                        //其他
                        path = setting.info_path,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
                    )
                }

            //转换为可观察列表
            mediaItemsMutableSnapshot = mediaItems.toMutableStateList()

            //反定位当前媒体index
            currentMediaIndex = mediaItemsMutableSnapshot.indexOfFirst { it.uriString == MediaInfo_MediaUriString }
            maxMediaIndex = mediaItemsMutableSnapshot.size - 1

            //保存完后公布状态
            state_MediaListProcess_complete = true
        }

    } //内部:从数据库读取播放列表
    fun getMediaListByDataBaseChange(context: Context){
        state_MediaListProcess_complete = false
        coroutineScope_getPlayList.launch(Dispatchers.IO) {
            //Log.d("SuMing", "getMediaListByDataBaseChange")
            //读取设置
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore",
                Context.MODE_PRIVATE
            )
            val sortOrder = PREFS_MediaStore.getString("PREFS_SortOrder", "info_title") ?: "info_title"
            val sortOrientation = PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") ?: "DESC"
            //读取所有媒体
            val mediaStoreRepo = MediaStoreRepo.Companion.get(context)
            val mediaStoreSettings = mediaStoreRepo.getAllVideosSorted(sortOrder, sortOrientation)
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_MediaUniqueID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_MediaUniqueID.toLongOrNull() ?: 0,
                        filename = setting.info_filename,
                        title = setting.info_title,
                        artist = setting.info_artist,
                        durationMs = setting.info_duration,
                        //视频专属
                        res = setting.info_resolution,
                        //其他
                        path = setting.info_path,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
                    )
                }

            //转换为可观察列表
            mediaItemsMutableSnapshot = mediaItems.toMutableStateList()
            //反定位当前媒体index
            currentMediaIndex = mediaItemsMutableSnapshot.indexOfFirst { it.uriString == MediaInfo_MediaUriString }
            maxMediaIndex = mediaItemsMutableSnapshot.size - 1

            //保存完后公布状态
            state_MediaListProcess_complete = true
        }
    }
    fun getMediaList(context: Context): SnapshotStateList<MediaItemForVideo> {
        //未完成读取,返回空列表
        if (!state_MediaListProcess_complete){
            context.showCustomToast("播放列表未加载完成",3)
            return emptyList
        }
        //已完成读取,返回播放列表
        return mediaItemsMutableSnapshot
    } //外部作用域获取列表
    fun isMediaListProcessComplete(): Boolean{
        return state_MediaListProcess_complete
    } //播放列表是否已完成读取
    fun updateMediaList(context: Context){
        getMediaListFromDataBase(context)
    } //更新播放列表
    fun deleteMediaItem(uriString: String){
        mediaItemsMutableSnapshot.removeIf { it.uriString == uriString }

    } //删除播放列表中的项
    private fun updateMediaIndex(itemUriString: String){
        if (!state_MediaListProcess_complete) return
        currentMediaIndex = mediaItemsMutableSnapshot.indexOfFirst { it.uriString == itemUriString }
    } //内部:更新当前媒体index
    private fun isNewUriValid(uri: Uri, context: Context): Boolean{
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(context, uri) }
        catch (e: Exception){
            ToolEventBus.sendEvent("ExistInvalidMediaItem")
            //Log.e("SuMing", "checkNewUri: $e")
            return false
        }
        return true
    }
    private fun showNotification_MediaListNotPrepared(text: String, context: Context) {
        val channelId = "toast_replace"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .setContentTitle(null)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)

    }  //显示未准备通知
    //播放列表:切换媒体
    private fun getTargetMediaUri(flag_next_or_previous: String, context: Context): String{
        if (!state_MediaListProcess_complete){
            context.showCustomToast("播放列表未加载完成",3)
            return "error"
        }
        var indexCursor = currentMediaIndex
        var indexTryCount = 0
        val maxCursorCount = maxMediaIndex
        var targetUriString = ""
        if (flag_next_or_previous == "next"){
            while (targetUriString == "" || targetUriString == "error"){
                indexCursor++
                //Log.d("SuMing", "indexCursor: $indexCursor  maxCursorCount: $maxCursorCount")
                if (indexCursor > maxCursorCount){
                    indexCursor = 0
                }
                targetUriString = mediaItemsMutableSnapshot.getOrNull(indexCursor)?.uriString ?: ""
                //Log.d("SuMing", "indexCursor: $indexCursor  targetUriString: $targetUriString")
                indexTryCount++
                val newUriValid = isNewUriValid(targetUriString.toUri(),context)
                //Log.d("SuMing", "检查newUriValid: $newUriValid")
                if (!newUriValid){ targetUriString = "error" }
                //Log.d("SuMing", "变更后的uri targetUriString: $targetUriString")
                if (indexTryCount > maxCursorCount){
                    //Log.d("SuMing", "indexTryCount: $indexTryCount  maxCursorCount: $maxCursorCount")
                    targetUriString = "error"
                    break
                }
                //Log.d("SuMing", "检查末尾 targetUriString: $targetUriString")
            }
            currentMediaIndex = mediaItemsMutableSnapshot.indexOfFirst { it.uriString == targetUriString }
            return targetUriString
        }
        else if (flag_next_or_previous == "previous"){
            //Log.d("SuMing", "切换上一曲")
            while (targetUriString == "" || targetUriString == "error"){
                indexCursor--
                if (indexCursor < 0){
                    indexCursor = maxCursorCount
                }
                targetUriString = mediaItemsMutableSnapshot.getOrNull(indexCursor)?.uriString ?: ""
                //Log.d("SuMing", "indexCursor: $indexCursor  targetUriString: $targetUriString")
                indexTryCount++
                val newUriValid = isNewUriValid(targetUriString.toUri(),context)
                if (!newUriValid){ targetUriString = "error" }
                if (indexTryCount > maxCursorCount){
                    targetUriString = "error"
                    break
                }
            }
            currentMediaIndex = mediaItemsMutableSnapshot.indexOfFirst { it.uriString == targetUriString }
            return targetUriString
        }
        else{
            context.showCustomToast("未传入有效的上下参数",3)
            return "error"
        }
    }
    fun switchToNextMediaItem(context: Context){
        //尝试获取目标uri
        val targetUriString = getTargetMediaUri("next",context)
        //检查uri是否有效
        if (targetUriString == "error"){ return }
        //获取目标uri
        val targetUri = targetUriString.toUri()
        //解码目标媒体信息
        val getMediaInfoResult = getMediaInfo(context,targetUri)
        if (!getMediaInfoResult){
            context.showCustomToast("出错了",3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true, context)


    }
    fun switchToPreviousMediaItem(context: Context){
        //尝试获取目标uri
        val targetUriString = getTargetMediaUri("previous",context)
        //检查uri是否有效,若有效,刷新index
        if (targetUriString == "error"){ return }
        //获取目标uri
        val targetUri = targetUriString.toUri()
        //解码目标媒体信息
        val getMediaInfoResult = getMediaInfo(context,targetUri)
        if (!getMediaInfoResult){
            context.showCustomToast("出错了",3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true, context)

    }
    //读取媒体列表
    //getMediaListFromDataBase(context)
    //更新当前媒体index
    //updateMediaIndex(MediaInfo_MediaUriString)

     */







}

//MiniMediaItemForList包含：
//id uri uriNumOnly filename title artist type