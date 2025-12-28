package com.suming.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

@SuppressLint("StaticFieldLeak")
@UnstableApi
@Suppress("unused")
object PlayerSingleton {
    //播放器参数
    var _player: ExoPlayer? = null
    @SuppressLint("StaticFieldLeak")
    private var _trackSelector: DefaultTrackSelector? = null
    @SuppressLint("StaticFieldLeak")
    private var _rendererFactory: RenderersFactory? = null
    //获取播放器实例
    val player: ExoPlayer get() = _player ?: throw IllegalStateException("发生错误")
    //加载控制(未启用)
    /*
    val loadControl = DefaultLoadControl.Builder()
        .setBackBuffer(1500, true)
        .setBufferDurationsMs(
            1000,  // minBufferMs - 减少最小缓冲
            3000,  // maxBufferMs - 减少最大缓冲
            500,   // bufferForPlaybackMs
            500    // bufferForPlaybackAfterRebufferMs
        )
        .setTargetBufferBytes(-1)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

     */
    //创建播放器实例
    private fun buildPlayer(app: Application): ExoPlayer {
        val trackSelector = getTrackSelector(app)
        val rendererFactory = getRendererFactory(app)

        singleton_player_built = true

        return ExoPlayer.Builder(app)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setMaxSeekToPreviousPositionMs(1_000_000L)
            //.setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .build()
            .apply {
                prepare()
                playWhenReady = false
            }
    }
    //何意味？
    fun JustBuildPlayer(app: Application){
        val trackSelector = getTrackSelector(app)
        val rendererFactory = getRendererFactory(app)
        _player = ExoPlayer.Builder(app)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setMaxSeekToPreviousPositionMs(1_000_000L)
            //.setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .build()
    }
    fun isPlayerBuilt(): Boolean{
        if (_player == null){
            return false
        }else{
            return true
        }
    }
    //播放器状态监听
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> { playerReady() }
                Player.STATE_ENDED -> {
                    playEnd()
                }
                Player.STATE_IDLE -> {

                }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            ToolEventBus.sendEvent("PlayerSingleton_PlaybackStateChanged")
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onMediaItemChanged(mediaItem)
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            state_onPlayError = true
            state_onPlayError_ErrorDuration = _player?.contentPosition ?: -1L
            state_onPlayError_NeedSeekToLastPosition = true
            ReStartPlayer()
            Log.d("SuMing", "onPlayerError: ${error}")
            Log.d("SuMing", "onPlayerError: ${error.errorCode}")
            Log.d("SuMing", "onPlayerError: ${error.cause}")
        }
    }
    private var state_PlayerStateListenerAdded = false
    fun addPlayerStateListener(){
        if (state_PlayerStateListenerAdded){
            return
        }
        player.addListener(PlayerStateListener)
        state_PlayerStateListenerAdded = true
    }
    fun removePlayerStateListener(){
        player.removeListener(PlayerStateListener)
        state_PlayerStateListenerAdded = false
    }

    //播放器初始化回调
    private val initializationCallbacks = mutableListOf<() -> Unit>()
    private var isPlayerInitialized = false
    fun addInitializationCallback(callback: () -> Unit) {
        synchronized(initializationCallbacks) {
            if (isPlayerInitialized && _player != null) {
                callback.invoke()
            } else {
                initializationCallbacks.add(callback)
            }
        }
    }

    //播放器状态变量
    var singleton_player_built = false
    var singleton_exit_before_read = false
    //媒体信息：!!!修改后必须记得更新这些信息
    var MediaInfo_MediaType = ""
    var MediaInfo_MediaTitle = ""
    var MediaInfo_MediaArtist = ""
    var MediaInfo_FileName = ""
    var MediaInfo_AbsolutePath = ""
    var MediaInfo_MediaUri = Uri.EMPTY!!
    var MediaInfo_MediaUriString = ""

    //错误状态
    var state_onPlayError = false
    var state_onPlayError_ErrorDuration: Long? = null
    var state_onPlayError_NeedSeekToLastPosition = false
    fun isPlayerError(): Boolean{
        if (state_onPlayError){
            state_onPlayError = false
            return true
        }else{
            return false
        }
    }
    //媒体项变更的后续操作
    private fun setServiceSetting(){
        val INFO_PlayerSingleton = singletonContext.getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
        INFO_PlayerSingleton.edit{ putInt("state_PlayerType", 1 ).apply() }
        INFO_PlayerSingleton.edit{ putString("MediaInfo_MediaType", MediaInfo_MediaType).apply() }
        INFO_PlayerSingleton.edit{ putString("MediaInfo_MediaUriString", MediaInfo_MediaUriString).apply() }
        INFO_PlayerSingleton.edit{ putString("MediaInfo_FileName", MediaInfo_FileName).apply() }
        INFO_PlayerSingleton.edit{ putString("MediaInfo_MediaArtist", MediaInfo_MediaArtist).apply() }
    }
    private fun saveLastMediaItemInfo(type: String, fileName: String, artist: String, uri: String){
        //Log.d("SuMing", "单例 saveLastMediaItemInfo : $type, $fileName, $artist, $uri")
        val INFO_PlayerSingleton = singletonContext.getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
        INFO_PlayerSingleton.edit {
            putString("MediaInfo_MediaType", type)
            putString("MediaInfo_FileName", fileName)
            putString("MediaInfo_MediaArtist", artist)
            putString("MediaInfo_MediaUriString", uri)
        }
    }  //播放信息保存到上次播放记录
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        //Log.d("SuMing", "单例 onMediaItemChanged : $mediaItem")
        if (mediaItem == null){ return }
        //播放信息保存到上次播放记录
        saveLastMediaItemInfo(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist, MediaInfo_MediaUriString)
        //写入服务信息
        setServiceSetting()
        //通告主界面
        ToolEventBus.sendEvent("PlayerSingleton_MediaItemChanged")
        //读取循环模式
        getRepeatMode()
        //注册事件总线
        registerEventBus(singletonContext)
        //读取单个媒体播放设置
        loadPlayParametersFromRoom()
        //读取媒体列表
        getMediaListFromDataBase(singletonContext)
        //更新当前媒体index
        updateMediaIndex(MediaInfo_MediaUriString)
        //链接媒体会话
        Handler(Looper.getMainLooper()).postDelayed({
            connectToMediaSession(singletonContext)
        }, 1000)
        //请求音频焦点和注册设备监听
        requestAudioFocus(singletonContext)
        startAudioDeviceCallback(singletonContext)

    }
    //媒体信息
    private lateinit var retriever: MediaMetadataRetriever
    private fun getMediaInfo(context: Context, uri: Uri): Boolean{
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(context, uri) }
        catch (_: Exception) { return false }
        //更新单例环境中的媒体信息变量
        MediaInfo_MediaUri = uri
        MediaInfo_MediaUriString = uri.toString()
        val MediaInfo_NewMediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "error"
        if (MediaInfo_NewMediaType != MediaInfo_MediaType){
            ToolEventBus.sendEvent("PlayerSingleton_MediaTypeChanged")
        }
        MediaInfo_MediaType = MediaInfo_NewMediaType
        MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "error"
        MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "error"
        MediaInfo_AbsolutePath = getFilePath(context, uri).toString()
        MediaInfo_FileName = (File(MediaInfo_AbsolutePath)).name ?: "error"
        //处理值
        if (MediaInfo_MediaType.contains("video")){
            MediaInfo_MediaType = "video"
        }
        else if (MediaInfo_MediaType.contains("audio")){
            MediaInfo_MediaType = "music"
        }
        if (MediaInfo_FileName == "error"){
            MediaInfo_FileName = "未知媒体标题"
        }
        if (MediaInfo_MediaArtist == "error"){
            MediaInfo_MediaArtist = "未知艺术家"
        }
        retriever.release()
        return true
    } //内部:从uri获取媒体信息,并覆写本地信息变量
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
    } //根据uri合成绝对路径
    fun getMediaInfoFromSingleton(){
        getMediaInfo(singletonContext, MediaInfo_MediaUri)
    } //外部作用域获取当前媒体信息
    fun getMediaType(): String{
        return MediaInfo_MediaType
    }
    //播放列表
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
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
            val sortOrder = PREFS_MediaStore.getString("PREFS_SortOrder", "info_title") ?: "info_title"
            val sortOrientation = PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") ?: "DESC"
            //读取所有媒体
            val mediaStoreRepo = MediaStoreRepo.get(singletonContext)
            val mediaStoreSettings = mediaStoreRepo.getAllVideosSorted(sortOrder, sortOrientation)
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_ID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_ID.toLongOrNull() ?: 0,
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
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
            val sortOrder = PREFS_MediaStore.getString("PREFS_SortOrder", "info_title") ?: "info_title"
            val sortOrientation = PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") ?: "DESC"
            //读取所有媒体
            val mediaStoreRepo = MediaStoreRepo.get(singletonContext)
            val mediaStoreSettings = mediaStoreRepo.getAllVideosSorted(sortOrder, sortOrientation)
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_ID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_ID.toLongOrNull() ?: 0,
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
    fun getMediaList(context: Context): SnapshotStateList<MediaItemForVideo>{
        //未完成读取,返回空列表
        if (!state_MediaListProcess_complete){
            context.showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
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
    private fun isNewUriValid(uri: Uri): Boolean{
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(singletonContext, uri) }
        catch (e: Exception){
            ToolEventBus.sendEvent("ExistInvalidMediaItem")
            //Log.e("SuMing", "checkNewUri: $e")
            return false
        }
        return true
    }
    private fun showNotification_MediaListNotPrepared(text: String) {
        val channelId = "toast_replace"
        val nm = singletonContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(singletonContext, channelId)
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
    private fun getTargetMediaUri(flag_next_or_previous: String): String{
        if (!state_MediaListProcess_complete){
            singletonContext.showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
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
                val newUriValid = isNewUriValid(targetUriString.toUri())
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
                val newUriValid = isNewUriValid(targetUriString.toUri())
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
            singletonContext.showCustomToast("未传入有效的上下参数",Toast.LENGTH_SHORT, 3)
            return "error"
        }
    }
    fun switchToNextMediaItem(){
        //尝试获取目标uri
        val targetUriString = getTargetMediaUri("next")
        //检查uri是否有效
        if (targetUriString == "error"){ return }
        //获取目标uri
        val targetUri = targetUriString.toUri()
        //解码目标媒体信息
        val getMediaInfoResult = getMediaInfo(singletonContext,targetUri)
        if (!getMediaInfoResult){
            singletonContext.showCustomToast("出错了",Toast.LENGTH_SHORT, 3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true)


    }
    fun switchToPreviousMediaItem(){
        //尝试获取目标uri
        val targetUriString = getTargetMediaUri("previous")
        //检查uri是否有效,若有效,刷新index
        if (targetUriString == "error"){ return }
        //获取目标uri
        val targetUri = targetUriString.toUri()
        //解码目标媒体信息
        val getMediaInfoResult = getMediaInfo(singletonContext,targetUri)
        if (!getMediaInfoResult){
            singletonContext.showCustomToast("出错了",Toast.LENGTH_SHORT, 3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true)

    }
    //通用:设置媒体项
    private fun setNewMediaItem(itemUri: Uri, playWhenReady: Boolean){
        //Log.d("SuMing", "单例 setNewMediaItem: $itemUri  $playWhenReady")
        //先保存上一个媒体的需要保存的信息
        savePositionToRoom()


        state_currentMediaReady = false

        //更新单例环境媒体信息
        val originMediaType = MediaInfo_MediaType
        getMediaInfo(singletonContext, itemUri)
        if (originMediaType != MediaInfo_MediaType){
            //先销毁原本的播放器
            ReleaseSingletonPlayer(singletonContext)
            //重建播放器并添加监听器
            BuildPlayer(singletonContext)
            addPlayerStateListener()
        }

        //设置播放状态
        _player?.playWhenReady = playWhenReady
        //合成并设置媒体项
        val covers_path_music = File(singletonContext.filesDir, "miniature/music_cover")
        val covers_path_video = File(singletonContext.filesDir, "miniature/video_cover")
        val cover_img_path = if (MediaInfo_MediaType == "video"){
            File(covers_path_video, "${MediaInfo_FileName.hashCode()}.webp")
        } else {
            File(covers_path_music, "${MediaInfo_FileName.hashCode()}.webp")
        }
        val cover_img_uri = if (cover_img_path.exists()) {
            try {
                FileProvider.getUriForFile(singletonContext, "${singletonContext.packageName}.provider", cover_img_path)
            }
            catch (e: Exception) {
                if (cover_img_path.canRead()) {
                    cover_img_path.toUri()
                } else {
                    null
                }
            }
        } else {
            null
        }
        val mediaItem = MediaItem.Builder()
            .setUri(MediaInfo_MediaUri)
            .setMediaId(MediaInfo_MediaUriString)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(MediaInfo_FileName)
                    .setArtist(MediaInfo_MediaArtist)
                    .setArtworkUri( cover_img_uri )
                    .build()
            )
            .build()
        _player?.setMediaItem(mediaItem)

    }
    //重启播放器
    private fun ReStartPlayer(){
        releasePlayer()
        Handler(Looper.getMainLooper()).postDelayed({

            releasePlayer()

            BuildPlayer(singletonContext)
            addPlayerStateListener()


            setNewMediaItem(MediaInfo_MediaUri, true)

        }, 500)

    }


    //循环模式:在playEnd()函数中处理
    private lateinit var PREFS: SharedPreferences
    var singleton_repeat_mode = ""
    fun setRepeatMode(mode: String) {
        singleton_repeat_mode = mode
        PREFS.edit{ putString("PREFS_RepeatMode", singleton_repeat_mode).apply() }
    }
    fun getRepeatMode(): String{
        getRepeatModeFromPreference(singletonContext)
        if (singleton_repeat_mode == ""){
            singleton_repeat_mode = "OFF"
        }
        else if (singleton_repeat_mode != "OFF" && singleton_repeat_mode != "ALL" && singleton_repeat_mode != "ONE"){
            singleton_repeat_mode = "OFF"
        }
        return singleton_repeat_mode
    }
    private fun getRepeatModeFromPreference(context: Context){
        if (singleton_repeat_mode != ""){ return }

        PREFS = context.getSharedPreferences("PREFS", MODE_PRIVATE)
        if (PREFS.contains("PREFS_RepeatMode")){
            singleton_repeat_mode = PREFS.getString("PREFS_RepeatMode", "OFF") ?: "error"
            if (singleton_repeat_mode == "error"){
                singleton_repeat_mode = "OFF"
                PREFS.edit{ putString("PREFS_RepeatMode", singleton_repeat_mode).apply() }
            }
            else if (singleton_repeat_mode != "OFF" && singleton_repeat_mode != "ALL" && singleton_repeat_mode != "ONE"){
                singleton_repeat_mode = "OFF"
                PREFS.edit{ putString("PREFS_RepeatMode", singleton_repeat_mode).apply() }
            }

        }else{
            singleton_repeat_mode = "OFF"
            PREFS.edit{ putString("PREFS_RepeatMode", singleton_repeat_mode).apply() }
        }

        singleton_repeat_mode = PREFS.getString("PREFS_RepeatMode", "OFF") ?: "error"
        if (singleton_repeat_mode == "error"){
            singleton_repeat_mode = "OFF"
        }
        else if (singleton_repeat_mode != "OFF" && singleton_repeat_mode != "ALL" && singleton_repeat_mode != "ONE"){
            singleton_repeat_mode = "OFF"
            PREFS.edit{ putString("PREFS_RepeatMode", singleton_repeat_mode).apply() }
        }
    }



    //音频设备监听
    private lateinit var audioManager: AudioManager
    private val DeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val relevant = removedDevices.filter {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (relevant.isNotEmpty()) {
                state_HeadSetInserted = false
                _player?.pause()
            }
        }
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val relevant = addedDevices.filter {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            if (relevant.isNotEmpty()) {
                state_HeadSetInserted = true
                checkVolumeLimit(singletonContext)
            }
        }
    }
    private var state_AudioManager_Initialized = false
    private var state_DeviceCallback_Registered = false
    private var state_HeadSetInserted = false
    fun initAudioManager(context: Context){
        audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        state_AudioManager_Initialized = true
    }
    fun startAudioDeviceCallback(context: Context){
        if (!state_AudioManager_Initialized){ initAudioManager(context) }
        if (state_DeviceCallback_Registered) return
        state_DeviceCallback_Registered = true
        audioManager.registerAudioDeviceCallback(DeviceCallback, null)
    }
    fun stopAudioDeviceCallback(context: Context){
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        audioManager.unregisterAudioDeviceCallback(DeviceCallback)
    }
    //音频焦点监听
    private lateinit var focusRequest: AudioFocusRequest
    private var state_focusRequest_Initialized = false
    fun initFocusRequest(context: Context){
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            //音频焦点变化监听
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {

                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        _player?.pause()

                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playState_wasPlaying){
                            _player?.play()
                        }

                    }
                }
            }
            .build()
        state_focusRequest_Initialized = true
    }
    fun requestAudioFocus(context: Context){
        if (!state_focusRequest_Initialized){
            initFocusRequest(context)
        }
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        if (_player?.isPlaying == false) return
        audioManager.requestAudioFocus(focusRequest)
    }
    fun releaseAudioFocus(context: Context){
        if (!state_focusRequest_Initialized){
            initFocusRequest(context)
        }
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }


    //事件总线
    private var state_EventBus_Registered = false
    fun registerEventBus(context: Context){
        if (state_EventBus_Registered) return
        setupEventBus(context)
        state_EventBus_Registered = true
    }
    fun unregisterEventBus(){
        disposable?.dispose()
        state_EventBus_Registered = false
    }
    private var disposable: io.reactivex.rxjava3.disposables.Disposable? = null
    private fun setupEventBus(context: Context) {
        disposable = ToolEventBus.events
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({
                HandlePlayerEvent(it)
            }, {
                context.showCustomToast("singleton事件总线注册失败:${it.message}", Toast.LENGTH_SHORT,3)
            })
    }
    private fun HandlePlayerEvent(event: String) {
        when (event) {
            "SessionController_Next" -> {
                //Log.d("SuMing", "SessionController_Next")
                switchToNextMediaItem()
            }
            "SessionController_Previous" -> {
                //Log.d("SuMing", "SessionController_Previous")
                switchToPreviousMediaItem()
            }
            "SessionController_Play" -> {
                playState_wasPlaying = true
            }
            "SessionController_Pause" -> {
                playState_wasPlaying = false
            }
        }
    }


    //限制音量上限
    private fun checkVolumeLimit(context: Context){
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume >= (maxVolume*0.6).toInt()){
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume*0.6).toInt(), 0)
            //发布通告
            ToolEventBus.sendEvent("PlayerSingleton_VolumeLimited")
            context.showCustomToast("检测到耳机插入,音量已限制为${(maxVolume*0.6).toInt()}", Toast.LENGTH_SHORT, 3)
        }
    }
    //耳机状态获取:是否插入耳机
    fun getHeadsetPlugged(context: Context): Boolean {
        return state_HeadSetInserted
    }

    //外部发起创建播放器
    fun BuildPlayer(context: Context){
        _player = buildPlayer(context as Application)
    }
    //媒体会话控制器
    var controller: MediaController? = null
    var MediaSessionController: ListenableFuture<MediaController>? = null
    var state_MediaSessionConnected = false
    //连接到媒体会话控制器
    fun connectToMediaSession(context: Context){
        if (state_MediaSessionConnected) return
        val SessionToken = SessionToken(context as Application, ComponentName(context, PlayerService::class.java))
        MediaSessionController = MediaController.Builder(context, SessionToken).buildAsync()
        MediaSessionController?.addListener({
            controller = MediaSessionController?.get()
            state_MediaSessionConnected = true
        }, MoreExecutors.directExecutor())
    }
    //关闭媒体会话控制器:同时在活动关闭服务和在单例断开控制器,才能确保播控中心消失
    fun stopMediaSessionController(context: Context){
        MediaSessionController?.get()?.run { release() }
    }
    fun stopBackgroundServices(){
        singletonContext.stopService(Intent(singletonContext, PlayerService::class.java))
    }
    private fun stopMediaSession(context: Context){
        stopBackgroundServices()
        stopMediaSessionController(context)
        state_MediaSessionConnected = false
    }


    //获取播放器存在状态
    fun getIsPlayerBuilt(): Int {
        if (singleton_player_built){
            return 1
        }else{
            return 0
        }
    }
    //在页面未打开完成就退出了
    fun getExitBeforeRead(): Boolean {
        return singleton_exit_before_read
    }
    fun setExitBeforeRead(exit: Boolean = true) {
        singleton_exit_before_read = exit
        Handler(Looper.getMainLooper()).postDelayed({
            singleton_exit_before_read = false
        }, 500)
    }
    //获取播放器媒体状态
    fun getIsPlaying(): Boolean {
        return _player?.isPlaying ?: false
    }
    fun getCurrentMediaItem(): MediaItem? {
        return _player?.currentMediaItem
    }
    fun getMediaInfoUri(): String {
        return MediaInfo_MediaUriString
    }
    fun getMediaInfoFileName(): String {
        return MediaInfo_FileName
    }
    fun getMediaInfoForMain(): Triple<String, String, String> {
        return Triple(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist)
    }
    fun getMediaInfoType(): String {
        return MediaInfo_MediaType
    }
    fun getMediaCurrentPosition(): Long {
        return _player?.currentPosition ?: -1
    }
    //主动设置媒体自定义状态
    fun setMediaInfo(type: String, filename: String, artist: String, url: String) {
        MediaInfo_MediaType = type
        MediaInfo_FileName = filename
        MediaInfo_MediaArtist = artist
        MediaInfo_MediaUriString = url
        MediaInfo_MediaUri = url.toUri()
    }
    fun setMediaInfoUri(uri: String) {
        MediaInfo_MediaUriString = uri
        MediaInfo_MediaUri = uri.toUri()
    }
    //设置媒体项
    fun setMediaItem_dont_use(item: MediaItem) {
        _player?.setMediaItem(item)
    }
    fun setMediaItem(itemUri: Uri, playWhenReady: Boolean) {
        setNewMediaItem(itemUri, playWhenReady)
    }
    //播放和暂停:禁止在播放器用户端调用,只能在主页面等地方使用
    private var playState_playEnd = false
    private var playState_wasPlaying = false
    fun playPlayer() {
        if (playState_playEnd){
            playState_playEnd = false
            _player?.seekTo(0)
        }
        playState_wasPlaying = true

        //保险：重置音量
        if (!PREFS_onlyVideoTrack) _player?.volume = 1f
        //保险：重置倍速
        player.setPlaybackSpeed(value_originalPlaySpeed)

        //开始播放
        _player?.play()
    }
    fun pausePlayer() {
        playState_wasPlaying = false
        _player?.pause()
    }
    fun setWasPlaying(wasPlaying: Boolean){
        this.playState_wasPlaying = wasPlaying
    }
    private fun playEnd(){
        if (singleton_repeat_mode == ""){
            singleton_repeat_mode = "OFF"
        }
        if (state_autoShutDown_Reach){
            state_autoShutDown_Reach = false
            countDownDuration_Ms = 0
            _player?.stop()
            onTaskRemoved()
            ReleaseSingletonPlayer(singletonContext)
            //结束进程
            val pid = Process.myPid()
            Process.killProcess(pid)
            exitProcess(0)
        }
        //判断
        when (singleton_repeat_mode) {
            "ONE" -> {
                _player?.seekTo(0)
                _player?.play()
            }
            "ALL" -> {
                switchToNextMediaItem()
            }
            "OFF" -> {
                playState_playEnd = true
                _player?.pause()
                ToolEventBus.sendEvent("PlayerSingleton_PlaybackStateChanged")
            }
        }
    }
    //清除媒体项和自定义信息
    fun clearMediaItem() {
        _player?.clearMediaItems()
    }
    fun clearMediaInfo() {
        MediaInfo_MediaType = ""
        MediaInfo_MediaTitle = ""
        MediaInfo_MediaArtist = ""
        MediaInfo_MediaUriString = ""
        MediaInfo_MediaUri = Uri.EMPTY
        //写入配置
        saveLastMediaItemInfo(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist, MediaInfo_MediaUriString)
    }
    //挂起和释放播放器
    fun stopPlayer() {
        singleton_player_built = false
        _player?.stop()
    }
    fun releasePlayer() {
        singleton_player_built = false
        _player?.release()
        _player = null
        _trackSelector = null
    }
    //销毁播放器并关闭媒体会话和服务
    fun ReleaseSingletonPlayer(context: Context){
        savePositionToRoom()
        state_PlayerStateListenerAdded = false
        stopMediaSession(context)
        releasePlayer()
    }
    //关闭所有监听器
    fun onTaskRemoved(){
        unregisterEventBus()
        stopAudioDeviceCallback(singletonContext)
        releaseAudioFocus(singletonContext)
    }
    //定时关闭倒计时：time：分钟
    private var autoShutDown_Timer: CountDownTimer? = null
    private var countDownDuration_Ms = 0
    private var shutDownMoment = ""
    private var state_autoShutDown_Reach = false
    private var state_autoShutDown_PrefsReaded = false
    private var PREFS_ShutDownWhenMediaEnd = false
    private fun showNotification_AboutToShutDown() {
        val channelId = "toast_replace"
        val nm = singletonContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(singletonContext, channelId)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .setContentTitle(null)
            .setContentText("本次播放完毕后将自动关闭")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)

    }
    private fun clearTimerShutDown(){
        countDownDuration_Ms = 0
        shutDownMoment = ""
        state_autoShutDown_Reach = false
        autoShutDown_Timer?.cancel()
    }
    private fun startTimerShutDown(countDownDuration_Ms: Int){
        autoShutDown_Timer?.cancel()
        autoShutDown_Timer = object : CountDownTimer(countDownDuration_Ms.toLong(), 1000000L) {
            override fun onTick( millisUntilFinished: Long) {}
            override fun onFinish() { autoShutDown_Reach() }
        }.start()
    }
    private fun autoShutDown_Reach() {
        if (PREFS_ShutDownWhenMediaEnd) {
            countDownDuration_Ms = 0
            shutDownMoment = "shutdown_when_end"
            state_autoShutDown_Reach = true
            showNotification_AboutToShutDown()
        }
        //直接关闭
        else{
            countDownDuration_Ms = 0
            _player?.stop()
            onTaskRemoved()
            ReleaseSingletonPlayer(singletonContext)
            //结束进程
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
    fun setCountDownTimer(CountDownDuration_Min: Int){
        //传入0即为关闭
        if (CountDownDuration_Min == 0){
            clearTimerShutDown()
            return
        }
        //记录倒计时时长,单位：毫秒
        countDownDuration_Ms = (CountDownDuration_Min * 60_000L).toInt()
        //计算关闭时间
        //val nowDateTime: String = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val nowMillis = System.currentTimeMillis()
        val shutDownMillis = nowMillis + countDownDuration_Ms.toLong()  //分钟转毫秒
        val pattern = java.text.SimpleDateFormat("HH时mm分ss秒", java.util.Locale.getDefault())
        shutDownMoment = pattern.format(java.util.Date(shutDownMillis))
        //启动倒计时
        startTimerShutDown(countDownDuration_Ms)
    }
    fun getShutDownMoment(): String{
        return shutDownMoment
    }
    fun get_PREFS_ShutDownWhenMediaEnd(): Boolean{
        return PREFS_ShutDownWhenMediaEnd
    }
    fun set_PREFS_ShutDownWhenMediaEnd(isChecked: Boolean){
        PREFS_ShutDownWhenMediaEnd = isChecked
        state_autoShutDown_PrefsReaded = true
        PREFS.edit{ putBoolean("PREFS_ShutDownWhenMediaEnd", isChecked) }
    }


    //重置播放参数
    private fun resetPlayParameters(){
        //确保播放倍速为1.0f
        setPlaySpeed(1.0f)
        //确保视频和音频都在播放
        if (!state_videoTrackWorking){ recoverVideoTrack() }
        if (!state_audioTrackWorking){ recoverAudioTrack() }
    }
    //播放参数一次性读取:只播音频/只播视频/上次进度等
    private var state_NeedSeekToLastPosition = false
    private var value_lastPosition = 0L
    private fun loadPlayParametersFromRoom(){
        //读取视频和音频轨道状态
        coroutineScope_saveRoom.launch {
            PREFS_onlyVideoTrack = MediaItemRepo.get(singletonContext).get_PREFS_VideoOnly(MediaInfo_FileName)
            PREFS_onlyAudioTrack = MediaItemRepo.get(singletonContext).get_PREFS_SoundOnly(MediaInfo_FileName)
            PREFS_saveLastPosition = MediaItemRepo.get(singletonContext).get_PREFS_saveLastPosition(MediaInfo_FileName)


            //根据状态设置播放参数
            if (PREFS_saveLastPosition){
                value_lastPosition = MediaItemRepo.get(singletonContext).get_value_LastPosition(MediaInfo_FileName)
                if (value_lastPosition >= 10_000L){
                    withContext(Dispatchers.Main){
                        if (state_currentMediaReady){
                            _player?.seekTo(value_lastPosition)
                            state_NeedSeekToLastPosition = false
                        }else{
                            state_NeedSeekToLastPosition = true
                        }
                    }
                }
            }
            if (PREFS_onlyVideoTrack){ closeVideoTrack() }
            if (PREFS_onlyAudioTrack){ closeAudioTrack() }


        }
    }
    //关闭和开启视频轨道
    private var coroutineScope_saveRoom = CoroutineScope(Dispatchers.IO)
    private var PREFS_onlyVideoTrack = false
    private var PREFS_onlyAudioTrack = false
    private var state_videoTrackWorking = true
    private var state_audioTrackWorking = true
    private fun saveTrackStateToRoom(type: String, flag: Boolean){
        when(type){
            "video" -> {
                coroutineScope_saveRoom.launch {
                    MediaItemRepo.get(singletonContext).update_PREFS_VideoOnly(MediaInfo_FileName,flag)
                }
            }
            "audio" -> {
                coroutineScope_saveRoom.launch {
                    MediaItemRepo.get(singletonContext).update_PREFS_SoundOnly(MediaInfo_FileName,flag)
                }
            }
        }

    }
    fun closeVideoTrack() {
        if (!state_videoTrackWorking) return
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        state_videoTrackWorking = false
        //关闭视频时确保音频在播放
        if (!state_audioTrackWorking){ recoverAudioTrack() }
        //设置保存到数据库
        saveTrackStateToRoom("video", true)
    }
    fun recoverVideoTrack() {
        if (state_videoTrackWorking) return
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
        state_videoTrackWorking = true
        //设置保存到数据库
        saveTrackStateToRoom("video", false)
    }
    fun getState_isVideoTrackWorking(): Boolean{
        return state_videoTrackWorking
    }
    fun getState_trackWorkingState(): Pair<Boolean, Boolean>{
        return Pair(state_audioTrackWorking, state_videoTrackWorking)
    }
    //关闭和开启音频轨道
    fun closeAudioTrack() {
        if (!state_audioTrackWorking) return
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        state_audioTrackWorking = false
        //关闭音频时确保视频在播放
        if (!state_videoTrackWorking){ recoverVideoTrack() }
        //设置保存到数据库
        saveTrackStateToRoom("audio", true)
    }
    fun recoverAudioTrack() {
        if (state_audioTrackWorking) return
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
        state_audioTrackWorking = true
        //设置保存到数据库
        saveTrackStateToRoom("audio", false)
    }
    fun getState_isAudioTrackWorking(): Boolean{
        return state_audioTrackWorking
    }
    //倍速管理
    private var value_originalPlaySpeed = 1.0f  //设定的倍速,避免返回长按快进倍速
    fun setPlaySpeed(speed: Float){
        player.setPlaybackSpeed(speed)
        value_originalPlaySpeed = speed
    }
    fun setPlaySpeedByLongPress(speed: Float){
        player.setPlaybackSpeed(speed)
    }
    fun getPlaySpeed(): Pair<Float, Float>{
        return Pair(player.playbackParameters.speed, value_originalPlaySpeed)
    }
    //保存播放进度
    private var PREFS_saveLastPosition = false
    private fun savePositionToRoom(){
        if (!PREFS_saveLastPosition) return
        val currentPosition = _player?.currentPosition
        if (currentPosition == 0L) return
        Log.d("SuMing", "savePositionToRoom: value_lastPosition = $currentPosition ${MediaInfo_FileName}")
        coroutineScope_saveRoom.launch {
            MediaItemRepo.get(singletonContext).update_value_LastPosition(MediaInfo_FileName, currentPosition?:114514L)
        }
    }
    fun get_PREFS_saveLastPosition(): Boolean{
        return PREFS_saveLastPosition
    }
    fun set_PREFS_saveLastPosition(flag: Boolean){
        PREFS_saveLastPosition = flag
        coroutineScope_saveRoom.launch {
            MediaItemRepo.get(singletonContext).update_PREFS_saveLastPosition(MediaInfo_FileName, flag)
        }
    }
    //后台播放设置：loadSettings()
    private var PREFS_BackgroundPlay = false
    private var PREFS_closeVideoTrackOnBackground = false
    fun set_PREFS_BackgroundPlay(flag: Boolean){
        PREFS_BackgroundPlay = flag
        PREFS.edit { putBoolean("PREFS_BackgroundPlay", flag) }
    }
    fun get_PREFS_BackgroundPlay(): Boolean{
        return PREFS_BackgroundPlay
    }

    private var state_currentMediaReady = false
    private fun playerReady(){
        state_currentMediaReady = true
        if (state_onPlayError_NeedSeekToLastPosition){
            state_onPlayError_NeedSeekToLastPosition = false
            if (state_onPlayError_ErrorDuration != -1L){
                player.seekTo(state_onPlayError_ErrorDuration!!)
            }
        }
        if (state_NeedSeekToLastPosition){
            Log.d("SuMing", "playerReady: seekTo value_lastPosition = $value_lastPosition")
            state_NeedSeekToLastPosition = false
            player.seekTo(value_lastPosition)
        }

    }

    //启动播放器单例 + 存入上下文引用
    lateinit var singletonContext: Context
    private var state_ContextSet = false
    private fun setContext(ctx: Context) {
        singletonContext = ctx.applicationContext
        state_ContextSet = true
    }
    private fun loadSettings(){
        PREFS = singletonContext.getSharedPreferences("PREFS", MODE_PRIVATE)
        if (PREFS.contains("PREFS_BackgroundPlay")) {
            PREFS_BackgroundPlay = PREFS.getBoolean("PREFS_BackgroundPlay", false)
        } else {
            PREFS.edit { putBoolean("PREFS_BackgroundPlay", false) }
        }
        if (PREFS.contains("PREFS_closeVideoTrackOnBackground")) {
            PREFS_closeVideoTrackOnBackground = PREFS.getBoolean("PREFS_closeVideoTrackOnBackground", false)
        } else {
            PREFS.edit { putBoolean("PREFS_closeVideoTrackOnBackground", false) }
        }
        if (PREFS.contains("PREFS_ShutDownWhenMediaEnd")){
            PREFS_ShutDownWhenMediaEnd = PREFS.getBoolean("PREFS_ShutDownWhenMediaEnd", false)
        }else{
            PREFS_ShutDownWhenMediaEnd = false
            PREFS.edit{ putBoolean("PREFS_ShutDownWhenMediaEnd", PREFS_ShutDownWhenMediaEnd).apply() }
        }

    }
    fun startPlayerSingleton(app: Application){
        //设置上下文
        setContext(app)
        //启动事件总线
        registerEventBus(app)
        //启动音频设备监听器
        startAudioDeviceCallback(app)
        //启动音频焦点监听器：未实际播放时，不申请焦点
        requestAudioFocus(app)


    }
    fun settingsUpdate(){
        loadSettings()
    }
    //全局onResume/onStop信息收集
    fun ActivityOnResume(){
        stopBackgroundPlay()
    }
    fun ActivityOnStop(){
        startBackgroundPlay()
    }
    //开始/结束后台播放
    private fun startBackgroundPlay(){
        //开启后台播放
        if (PREFS_BackgroundPlay){
            if (PREFS_closeVideoTrackOnBackground){
                closeVideoTrackJob()
                state_closeVideoTrackJob_Running = true
            }
        }
        //关闭后台播放
        else{
            pausePlayer()
        }
    }
    private fun stopBackgroundPlay(){
        //开启后台播放
        if (PREFS_BackgroundPlay){
            if (PREFS_closeVideoTrackOnBackground){
                closeVideoTrackJob?.cancel()
                recoverVideoTrack()
            }
        }
        //关闭后台播放
        else{
            playPlayer()
        }
    }
    //关闭视频轨道倒计时
    private var coroutineScope_closeVideoTrackJob: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var state_closeVideoTrackJob_Running = false
    private var closeVideoTrackJob: Job? = null
    private fun closeVideoTrackJob() {
        closeVideoTrackJob?.cancel()
        closeVideoTrackJob = coroutineScope_closeVideoTrackJob.launch {
            delay(60_000)
            closeVideoTrack()
            state_closeVideoTrackJob_Running = false
        }
    }

    //获取播放器实例
    fun getPlayer(app: Application): ExoPlayer = _player ?: synchronized(this) {
            _player ?: buildPlayer(app).also { _player = it }
        }.also {
            isPlayerInitialized = true
            initializationCallbacks.forEach { callback -> callback.invoke() }
            initializationCallbacks.clear()
        }
    //播放器底层组件
    fun getTrackSelector(app: Application): DefaultTrackSelector =
        _trackSelector ?: synchronized(this) {
            _trackSelector ?: DefaultTrackSelector(app)
                    .also { _trackSelector = it }

        }
    fun getRendererFactory(app: Application): RenderersFactory =
        _rendererFactory ?: synchronized(this) {
            _rendererFactory ?: DefaultRenderersFactory(app)
                //.setEnableDecoderFallback(true)
                .also { _rendererFactory = it }
        }
    fun createCustomCodecFactory(): MediaCodecAdapter.Factory {
        return MediaCodecAdapter.Factory.DEFAULT
    }



//singleton object END
}