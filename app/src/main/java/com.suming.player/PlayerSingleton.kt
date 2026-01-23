package com.suming.player

import android.annotation.SuppressLint
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
import com.suming.player.ListManager.PlayerListManager
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
    private val player: ExoPlayer get() = _player ?: throw IllegalStateException("发生错误")
    private var _trackSelector: DefaultTrackSelector? = null
    private var _rendererFactory: RenderersFactory? = null
    //获取播放器实例
    fun getPlayer(app: Application): ExoPlayer = _player ?: synchronized(this) {
        _player ?: buildPlayer(app).also { _player = it }
    }.also {
        stateLock_isPlayerInitialized = true
        initializationCallbacks.forEach { callback -> callback.invoke() }
        initializationCallbacks.clear()
    }
    //创建播放器实例
    private fun buildPlayer(app: Application): ExoPlayer {
        val trackSelector = getTrackSelector(app)
        val rendererFactory = getRendererFactory(app)
        //创建播放器
        val ExoPlayer = ExoPlayer.Builder(app)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setMaxSeekToPreviousPositionMs(1_000_000L)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .build()
            .apply {
                prepare()
                playWhenReady = false
            }

        //清除随单个播放器实例的状态
        playerState_PlayerStateListenerAdded = false


        return ExoPlayer
    }
    //其他
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
    @Suppress("DEPRECATION")
    fun createCustomCodecFactory(): MediaCodecAdapter.Factory {
        return MediaCodecAdapter.Factory.DEFAULT
    }
    //检查播放器是否为null
    fun isPlayerBuilt(): Boolean{
        if (_player == null){
            return false
        }else{
            return true
        }
    }
    //播放器初始化监听
    private val initializationCallbacks = mutableListOf<() -> Unit>()
    private var stateLock_isPlayerInitialized = false
    fun addInitializationCallback(callback: () -> Unit) {
        synchronized(initializationCallbacks) {
            if (stateLock_isPlayerInitialized && _player != null) {
                callback.invoke()
            } else {
                initializationCallbacks.add(callback)
            }
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

        }
    }
    private var playerState_PlayerStateListenerAdded = false
    fun addPlayerStateListener(){
        if (playerState_PlayerStateListenerAdded){
            return
        }
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = true
    }
    fun removePlayerStateListener(){
        player.removeListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = false
    }
    //快速创建播放器并包含后续必要操作
    fun startSingletonExoPlayer(context: Context){
        //确保播放器在线
        getPlayer(context as Application)
        //添加监听器
        addPlayerStateListener()
    }

    //播放器错误处理
    private fun escapePlayerError(){
        //缓存原本的媒体uri
        val currentMediaUri = MediaInfo_MediaUri
        //清除
        _player?.clearMediaItems()
        //重新设置媒体
        _player?.playWhenReady = true
        //合成并设置媒体项
        val mediaItem = MediaItem.Builder()
            .setUri(MediaInfo_MediaUri)
            .setMediaId(MediaInfo_MediaUriString)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(MediaInfo_FileName)
                    .setArtist(MediaInfo_MediaArtist)
                    .build()
            )
            .build()
        _player?.setMediaItem(mediaItem)


        _player?.prepare()


    }


    //媒体信息
    private var MediaInfo_MediaType = ""
    private var MediaInfo_MediaTitle = ""
    private var MediaInfo_MediaArtist = ""
    private var MediaInfo_FileName = ""
    private var MediaInfo_Duration = 0L
    private var MediaInfo_AbsolutePath = ""
    private var MediaInfo_MediaUri = Uri.EMPTY!!
    private var MediaInfo_MediaUriString = ""
    //媒体信息解码器
    private lateinit var retriever: MediaMetadataRetriever
    private fun getMediaInfo(context: Context, uri: Uri): Boolean{
        retriever = MediaMetadataRetriever()
        //测试是否能正常读取
        try { retriever.setDataSource(context, uri) }
        catch (_: Exception) { return false }
        //获取新的媒体信息
        val NEW_MediaInfo_MediaUri = uri
        val NEW_MediaInfo_MediaUriString = uri.toString()
        var NEW_MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
        var NEW_MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        var NEW_MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val NEW_MediaInfo_Duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
        val NEW_MediaInfo_AbsolutePath = getFilePath(context, uri).toString()
        val NEW_MediaInfo_FileName = (File(NEW_MediaInfo_AbsolutePath)).name ?: ""
        //处理值
        if (NEW_MediaInfo_MediaType.contains("video")){
            NEW_MediaInfo_MediaType = "video"
        }else if(NEW_MediaInfo_MediaType.contains("audio")){
            NEW_MediaInfo_MediaType = "music"
        }
        if (NEW_MediaInfo_MediaTitle == ""){ NEW_MediaInfo_MediaTitle = "未知媒体标题" }
        if (NEW_MediaInfo_MediaArtist == "" || NEW_MediaInfo_MediaArtist == "<unknown>"){ NEW_MediaInfo_MediaArtist = "未知艺术家" }

        //刷新本地媒体信息变量
        updateMediaInfoValues(
            NEW_MediaInfo_MediaType,
            NEW_MediaInfo_MediaTitle,
            NEW_MediaInfo_MediaArtist,
            NEW_MediaInfo_FileName,
            NEW_MediaInfo_Duration,
            NEW_MediaInfo_AbsolutePath,
            NEW_MediaInfo_MediaUri,
            NEW_MediaInfo_MediaUriString,
        )

        //
        retriever.release()

        return true
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
    } //根据uri合成绝对路径
    private fun updateMediaInfoValues(NEW_MediaInfo_MediaType: String,
        NEW_MediaInfo_MediaTitle: String,
        NEW_MediaInfo_MediaArtist: String,
        NEW_MediaInfo_FileName: String,
        NEW_MediaInfo_Duration: Long,
        NEW_MediaInfo_AbsolutePath: String,
        NEW_MediaInfo_MediaUri: Uri,
        NEW_MediaInfo_MediaUriString: String,){
        MediaInfo_MediaType = NEW_MediaInfo_MediaType
        MediaInfo_MediaTitle = NEW_MediaInfo_MediaTitle
        MediaInfo_MediaArtist = NEW_MediaInfo_MediaArtist
        MediaInfo_FileName = NEW_MediaInfo_FileName
        MediaInfo_Duration = NEW_MediaInfo_Duration
        MediaInfo_AbsolutePath = NEW_MediaInfo_AbsolutePath
        MediaInfo_MediaUri = NEW_MediaInfo_MediaUri
        MediaInfo_MediaUriString = NEW_MediaInfo_MediaUriString
    }
    //获取媒体信息丨公共函数
    fun getMediaInfoUri(): Uri {
        return MediaInfo_MediaUri
    }
    fun getMediaInfoUriString(): String {
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
    fun clearMediaInfo(context: Context) {
        MediaInfo_MediaType = ""
        MediaInfo_MediaTitle = ""
        MediaInfo_MediaArtist = ""
        MediaInfo_MediaUriString = ""
        MediaInfo_MediaUri = Uri.EMPTY
        //写入配置
        clearLastMediaRecord(context)
    }



    //👀媒体项变更
    //确认设置新媒体项丨私有
    private fun setNewMediaItem(itemUri: Uri, playWhenReady: Boolean, context: Context): Boolean {
        //保存上个媒体的信息
        val oldItemName = MediaInfo_FileName
        val oldItemDuration = MediaInfo_Duration
        val currentPosition = getMediaCurrentPosition()
        coroutine_saveOldItemData.launch {

            if (MediaInfo_FileName.isEmpty()) return@launch
            if (currentPosition == -1L) return@launch

            withContext(Dispatchers.Main){
                saveOldItemData(oldItemName,currentPosition, oldItemDuration)
            }
        }


        //👻丨正式开始设置新媒体项的流程
        //解码新媒体信息丨确认媒体有效前不会刷新本地媒体信息
        val success = getMediaInfo(context, itemUri)
        if (!success) return false


        //重置单个媒体状态
        clearItemState()
        //设置播放状态
        _player?.playWhenReady = playWhenReady

        //合成并设置媒体项
        val cover_img_uri = getCoverImgUri(context)

        //开始构建mediaItem
        val mediaItem = MediaItem.Builder()
            .setUri(MediaInfo_MediaUri)
            .setMediaId(MediaInfo_MediaUriString)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(MediaInfo_FileName)
                    .setArtist(MediaInfo_MediaArtist)
                    .setArtworkUri(cover_img_uri)
                    .build()
            )
            .build()
        _player?.setMediaItem(mediaItem)


        return true
    }
    //设置媒体项丨公共函数丨需要带一层过滤
    fun setMediaItem(itemUri: Uri, playWhenReady: Boolean, context: Context): Boolean {

        val success = setNewMediaItem(itemUri, playWhenReady, context)

        return success
    }
    //保存上个媒体的需保存内容
    private var coroutine_saveOldItemData = CoroutineScope(Dispatchers.IO)
    private fun saveOldItemData(fileName: String, currentPosition: Long, duration: Long){

        saveParaToDataBase(fileName, currentPosition, duration)

    }
    //完成媒体项变更丨后续操作
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null){ return }


        //启动服务
        startService()
        //记录到上次播放清单
        coroutine_saveLastMediaRecord.launch { saveLastMediaRecord() }
        //读取单个媒体播放设置
        coroutine_saveOrFetchDataBase.launch {
            FetchDataBaseForItem(MediaInfo_FileName)

        }


        //发布通告
        ToolEventBus.sendEvent("PlayerSingleton_MediaItemChanged")

        //请求音频焦点
        requestAudioFocus(objectContext, force_request = false)


    }
    //启动服务和媒体会话
    private var coroutine_startService = CoroutineScope(Dispatchers.IO)
    private fun startService(){
        //写入服务配置
        setServiceLinker()
        //链接媒体会话
        startMediaSession()

    }
    private fun setServiceLinker(newPageType: Int = -1){
        //写入媒体类型
        PlayerServiceLinker.setMediaInfo_MediaType(MediaInfo_MediaType)
        //
        PlayerServiceLinker.setMediaBasicInfo(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist)


    }
    private fun startMediaSession(){
        connectToMediaSession(objectContext)
    }
    //写入上次播放记录丨私有函数丨可作为一条单独线程
    private var coroutine_saveLastMediaRecord = CoroutineScope(Dispatchers.IO)
    private fun saveLastMediaRecord(){
        val lastRecord = objectContext.getSharedPreferences("lastRecord", MODE_PRIVATE)
        lastRecord.edit {
            putString("MediaInfo_MediaType", MediaInfo_MediaType)
            putString("MediaInfo_FileName", MediaInfo_FileName)
            putString("MediaInfo_MediaArtist", MediaInfo_MediaArtist)
            putString("MediaInfo_MediaUriString", MediaInfo_MediaUriString)
        }
    }
    private fun clearLastMediaRecord(context: Context){
        val lastRecord = context.getSharedPreferences("lastRecord", MODE_PRIVATE)
        lastRecord.edit {
            putString("MediaInfo_MediaType", "")
            putString("MediaInfo_FileName", "")
            putString("MediaInfo_MediaArtist", "")
            putString("MediaInfo_MediaUriString", "")
        }
    }
    //其他工具函数
    private fun getCoverImgUri(context: Context): Uri?{
        val covers_path_music = File(context.filesDir, "miniature/music_cover")
        val covers_path_video = File(context.filesDir, "miniature/video_cover")
        val MediaInfo_uriNumOnly = MediaInfo_MediaUri.lastPathSegment
        val cover_img_path = when (MediaInfo_MediaType) {
            "video" -> {
                File(covers_path_video, "${MediaInfo_uriNumOnly}.webp")
            }
            "music" -> {
                File(covers_path_music, "${MediaInfo_uriNumOnly}.webp")
            }
            else -> {
                File(covers_path_video, "${MediaInfo_uriNumOnly}.webp")
            }
        }
        val cover_img_uri = if(SettingsRequestCenter.get_PREFS_DisableMediaArtWork(context)){
            null
        }else if(cover_img_path.exists()) {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", cover_img_path)
            }
            catch (e: Exception) {
                if (cover_img_path.canRead()) {
                    cover_img_path.toUri()
                } else {
                    null
                }
            }
        }else{ null }

        return cover_img_uri
    }


    //👻丨媒体会话
    private var controller: MediaController? = null
    private var MediaSessionController: ListenableFuture<MediaController>? = null
    private var sessionState_MediaSession_connected = false
    //连接到媒体会话控制器
    private fun connectToMediaSession(context: Context){
        if (sessionState_MediaSession_connected) return
        Log.d("SuMing","connectToMediaSession MediaInfo_MediaType = $MediaInfo_MediaType")
        val SessionToken = SessionToken(context as Application, ComponentName(context, PlayerService::class.java))
        MediaSessionController = MediaController.Builder(context, SessionToken).buildAsync()
        MediaSessionController?.addListener({
            controller = MediaSessionController?.get()
            sessionState_MediaSession_connected = true
        }, MoreExecutors.directExecutor())
    }
    //关闭媒体会话控制器
    private fun stopMediaSessionController(){
        MediaSessionController?.get()?.run { release() }
        controller = null
        sessionState_MediaSession_connected = false
    }
    private fun stopServices(){
        objectContext.stopService(Intent(objectContext, PlayerService::class.java))
        sessionState_MediaSession_connected = false
    }
    //清除媒体会话
    private fun stopMediaSession(){
        stopMediaSessionController()
        stopServices()
        sessionState_MediaSession_connected = false
    }


    //👀丨关闭各种组件
    //关闭播放器核心
    private fun DevastatePlayEnginBundle(context: Context){
        //执行播放器释放
        releasePlayer()
        //播放器监听器跟随销毁,重置状态
        playerState_PlayerStateListenerAdded = false
    }
    //完全清除媒体会话
    private fun DevastateMediaSessionBundle(){
        stopMediaSession()
    }
    //关闭监听器
    private fun DevastateListener(){
        stopListener()
    }
    //公共函数
    fun stopPlayBundle(need_clear_record: Boolean, context: Context){
        //清除播放记录
        if (need_clear_record){ clearLastMediaRecord(context) }
        //关闭媒体会话
        DevastateMediaSessionBundle()
        //关闭播放器
        DevastatePlayEnginBundle(context)
        //关闭监听器
        DevastateListener()
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
            val mediaStoreRepo = MediaStoreRepo.get(objectContext)
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
            val mediaStoreRepo = MediaStoreRepo.get(objectContext)
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
        try { retriever.setDataSource(objectContext, uri) }
        catch (e: Exception){
            ToolEventBus.sendEvent("ExistInvalidMediaItem")
            //Log.e("SuMing", "checkNewUri: $e")
            return false
        }
        return true
    }
    private fun showNotification_MediaListNotPrepared(text: String) {
        val channelId = "toast_replace"
        val nm = objectContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(objectContext, channelId)
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
            objectContext.showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
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
            objectContext.showCustomToast("未传入有效的上下参数",Toast.LENGTH_SHORT, 3)
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
        val getMediaInfoResult = getMediaInfo(objectContext,targetUri)
        if (!getMediaInfoResult){
            objectContext.showCustomToast("出错了",Toast.LENGTH_SHORT, 3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true, objectContext)


    }
    fun switchToPreviousMediaItem(){
        //尝试获取目标uri
        val targetUriString = getTargetMediaUri("previous")
        //检查uri是否有效,若有效,刷新index
        if (targetUriString == "error"){ return }
        //获取目标uri
        val targetUri = targetUriString.toUri()
        //解码目标媒体信息
        val getMediaInfoResult = getMediaInfo(objectContext,targetUri)
        if (!getMediaInfoResult){
            objectContext.showCustomToast("出错了",Toast.LENGTH_SHORT, 3)
            return
        }
        //切换至目标媒体项
        setNewMediaItem(targetUri, true, objectContext)

    }
    //读取媒体列表
    //getMediaListFromDataBase(objectContext)
    //更新当前媒体index
    //updateMediaIndex(MediaInfo_MediaUriString)





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
                recessPlay(need_fadeOut = false)
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
                setVolumeLimit(objectContext)
            }
        }
    }
    private var state_AudioManager_Initialized = false
    private var state_DeviceCallback_Registered = false
    private var state_HeadSetInserted = false
    private fun initAudioManager(context: Context){
        audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        state_AudioManager_Initialized = true
    }
    private fun startAudioDeviceCallback(context: Context){
        if (!state_AudioManager_Initialized){ initAudioManager(context) }
        if (state_DeviceCallback_Registered) return
        state_DeviceCallback_Registered = true
        audioManager.registerAudioDeviceCallback(DeviceCallback, null)
    }
    private fun stopAudioDeviceCallback(context: Context){
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        audioManager.unregisterAudioDeviceCallback(DeviceCallback)
    }
    fun getState_isHeadsetPlugged(context: Context): Boolean {
        return state_HeadSetInserted
    }
    fun setVolumeLimit(context: Context){
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume >= (maxVolume*0.6).toInt()){
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume*0.6).toInt(), 0)
        }
    }
    //音频焦点监听
    private lateinit var focusRequest: AudioFocusRequest
    private var state_focusRequest_Initialized = false
    private fun initFocusRequest(context: Context){
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
                        recessPlay(need_fadeOut = true)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playState_wasPlaying){
                            continuePlay(need_requestFocus = true, force_request = true, need_fadeIn = true)
                        }

                    }
                }
            }
            .build()
        state_focusRequest_Initialized = true
    }
    private fun requestAudioFocus(context: Context, force_request: Boolean){
        if (!state_focusRequest_Initialized){
            initFocusRequest(context)
        }
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
        if (force_request){
            audioManager.requestAudioFocus(focusRequest)
        }else if(_player?.isPlaying != true ) {
            audioManager.requestAudioFocus(focusRequest)
        }

    }
    private fun releaseAudioFocus(context: Context){
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
    private fun registerEventBus(context: Context){
        if (state_EventBus_Registered) return
        setupEventBus(context)
        state_EventBus_Registered = true
    }
    private fun unregisterEventBus(){
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
                switchToNextMediaItem()
            }
            "SessionController_Previous" -> {
                switchToPreviousMediaItem()
            }
            "SessionController_Play" -> {
                setWasPlaying(true)
                requestAudioFocus(objectContext, force_request = false)
            }
            "SessionController_Pause" -> {
                setWasPlaying(false)
            }
        }
    }
    //开启/关闭所有监听器
    fun startListener(){
        registerEventBus(objectContext)
        startAudioDeviceCallback(objectContext)
        initFocusRequest(objectContext)
    }
    fun stopListener(){
        unregisterEventBus()
        stopAudioDeviceCallback(objectContext)
        releaseAudioFocus(objectContext)
    }



    //播放页样式切换，重启服务
    fun updatedPlayStyle(context: Context, newType: Int){
        //关闭媒体会话和服务
        DevastateMediaSessionBundle()
        //未播放时不执行
        if (_player?.currentMediaItem == null) return
        //写入新服务配置并启动媒体会话
        setServiceLinker(newPageType = newType)
        Handler(Looper.getMainLooper()).postDelayed({ connectToMediaSession(context) }, 2000)

    }



    //获取播放器播放状态
    fun getPlayState(uri_need_compare: Uri): Triple<Boolean, Boolean, Uri> {
        if (_player?.currentMediaItem == null){
            return Triple(false, false, MediaInfo_MediaUri)
        }else{
            //Log.d("SuMing", "uri_need_compare:${uri_need_compare},MediaInfo_MediaUri:${MediaInfo_MediaUri}")
            if (uri_need_compare == MediaInfo_MediaUri){
                return Triple(true, true, MediaInfo_MediaUri)
            }else{
                return Triple(true, false, MediaInfo_MediaUri)
            }
        }
    } //获取当前播放状态
    fun getIsPlaying(): Boolean {
        return _player?.isPlaying ?: false
    } //是否正在播放
    fun getCurrentMediaItem(): MediaItem? {
        return _player?.currentMediaItem
    } //获取当前媒体项
    //播放和暂停
    private var playState_playEnd = false
    private var playState_wasPlaying = false
    fun continuePlay(need_requestFocus: Boolean, force_request: Boolean, need_fadeIn: Boolean) {
        if (playState_playEnd){
            playState_playEnd = false
            _player?.seekTo(0)
        }
        playState_wasPlaying = true


        //请求音频焦点
        if (need_requestFocus) requestAudioFocus(objectContext, force_request)

        //保险：重置倍速
        if (_player != null && _player?.playbackParameters?.speed != Para_OriginalPlaySpeed){
            player.setPlaybackSpeed(Para_OriginalPlaySpeed)
        }


        //开始播放
        _player?.play()
    } //开始/继续播放
    fun recessPlay(need_fadeOut: Boolean) {
        if (_player?.isPlaying == true){
            setWasPlaying(true)
        }else{
            setWasPlaying(false)
        }
        _player?.pause()

    } //暂停播放
    fun setWasPlaying(wasPlaying: Boolean){
        playState_wasPlaying = wasPlaying
    }
    fun cancelPlayEnd(){
        playState_playEnd = false
    }
    //清除媒体项
    fun clearMediaItem() {
        _player?.clearMediaItems()
    }
    //挂起和释放播放器
    fun stopPlayer() {
        _player?.stop()
    }
    fun releasePlayer() {
        _player?.release()
        _player = null
        playerState_PlayerStateListenerAdded = false
    }





    //👀丨单个媒体的播放状态
    private var itemState_firstExoReady = false
    private var itemState_firstStartExecuted = false
    //重置单个媒体播放状态
    private fun clearItemState(){
        itemState_firstExoReady = false
        itemState_firstStartExecuted = false

    }
    //播放状态
    private fun playerReady(){
        itemState_firstExoReady = true
        //是否需要应用独立的项参数
        if (paraApply){ ExecuteApplyPara() }

    }
    private fun playEnd(){
        //本次播放完成后关闭
        if (timerState_autoShut_Reach){
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //关闭
            stopPlayBundle(false,objectContext)
        }
        //从列表管理器获取循环模式
        val currentLoopMode = PlayerListManager.getLoopMode(objectContext)
        //根据循环模式执行不同操作
        when (currentLoopMode) {
            "ONE" -> {
                _player?.seekTo(0)
                continuePlay(need_requestFocus = false, force_request = false, need_fadeIn = false)
            }
            "ALL" -> {
                switchToNextMediaItem()
            }
            "OFF" -> {
                playState_playEnd = true
                recessPlay(need_fadeOut = false)
                ToolEventBus.sendEvent("PlayerSingleton_PlaybackStateChanged")
            }
        }
    }



    //👀丨独立播放参数丨指以para开头的变量
    private var coroutine_saveOrFetchDataBase = CoroutineScope(Dispatchers.IO)
    //公共函数丨从外部读取和修改独立播放参数丨注意：设置清单中的参数和当前实际运行参数不是同一个值
    fun get_Para_saveLastProgress(): Boolean{
        return Para_saveLastProgress
    }
    fun set_Para_saveLastProgress(boolean: Boolean){
        Para_saveLastProgress = boolean

        //保存到数据库
        coroutine_saveOrFetchDataBase.launch {
            MediaItemRepo.get(objectContext).update_PREFS_saveLastPosition(MediaInfo_FileName,boolean)
        }
        //开启保存进度循环
        if (boolean){ startSaveProgressHandler() }else{ stopSaveProgressHandler() }

    }
    fun get_Para_DisableAudioTrack(): Boolean{
        return Para_DisableAudioTrack
    }
    fun set_Para_DisableAudioTrack(boolean: Boolean, immediateApply: Boolean){
        Para_DisableAudioTrack = boolean
        //是否需要立即执行
        if (immediateApply){
            if (Para_DisableAudioTrack){
                DisableAudioTrack()
            }else{
                EnableAudioTrack()
            }
        }
        //保存到数据库
        coroutine_saveOrFetchDataBase.launch {
            MediaItemRepo.get(objectContext).update_PREFS_VideoOnly(MediaInfo_FileName,boolean)
        }
    }
    fun get_Para_DisableVideoTrack(): Boolean{
        return Para_DisableVideoTrack
    }
    fun set_Para_DisableVideoTrack(boolean: Boolean, immediateApply: Boolean){
        Para_DisableVideoTrack = boolean
        //是否需要立即执行
        if (immediateApply){
            if (Para_DisableVideoTrack){
                DisableVideoTrack()
            }else{
                EnableVideoTrack()
            }
        }
        //保存到数据库
        coroutine_saveOrFetchDataBase.launch {
            MediaItemRepo.get(objectContext).update_PREFS_SoundOnly(MediaInfo_FileName,boolean)
        }
    }
    //重置独立播放参数
    private fun clearItemPara(){
        paraApply = false
        Para_saveLastProgress = false
        Para_DisableAudioTrack = false
        Para_DisableVideoTrack = false
    }
    //独立播放参数合集
    private var Para_saveLastProgress = false
    private var Para_DisableAudioTrack = false
    private var Para_DisableVideoTrack = false
    //独立播放参数读取和应用
    private var paraApply = false
    private var paraApply_lastProgress = 0L
    private fun FetchDataBaseForItem(itemName: String){
        coroutine_saveOrFetchDataBase.launch {
            //读取保存的进度
            Para_saveLastProgress = MediaItemRepo.get(objectContext).get_PREFS_saveLastPosition(MediaInfo_FileName)
            paraApply_lastProgress = if (Para_saveLastProgress){
                MediaItemRepo.get(objectContext).get_value_LastPosition(MediaInfo_FileName)
            }else{
                0L
            }
            if (paraApply_lastProgress <= 20_000L || paraApply_lastProgress >= MediaInfo_Duration - 20_000L){
                paraApply_lastProgress = 0L
            }


            //应用独立设置项
            withContext(Dispatchers.Main){
                ExecuteApplyPara()
            }

        }
    }
    //应用播放参数
    private fun ExecuteApplyPara(){
        //已准备好：立即执行参数设定
        if (itemState_firstExoReady){
            //执行后关闭标记
            paraApply = false
            //判断时候需要恢复上次的进度
            if (paraApply_lastProgress != 0L){
                _player?.seekTo(paraApply_lastProgress)
            }
            //开启保存进度循环丨注意：必须在媒体准备好后开启
            if (Para_saveLastProgress){ startSaveProgressHandler() }else{ stopSaveProgressHandler() }


        }
        //未准备好：设置paraApply标记供首次准备完成时调用
        else{ paraApply = true }
    }
    //轨道启用和禁用
    private var Para_state_videoTrack_Disabled = true
    private var Para_state_audioTrack_Disabled = true
    fun DisableVideoTrack(){
        //防止重复执行
        if (Para_state_videoTrack_Disabled) return
        //执行禁用视频轨道
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()

        Para_state_videoTrack_Disabled = true

    }
    fun EnableVideoTrack(){
        //
        if (Para_state_videoTrack_Disabled){
            _trackSelector?.parameters = _trackSelector!!
                .buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .build()

            Para_state_videoTrack_Disabled = false
        }
    }
    fun DisableAudioTrack(){
        //
        if (Para_state_audioTrack_Disabled) return
        //
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()

        Para_state_audioTrack_Disabled = true

    }
    fun EnableAudioTrack(){
        //
        if (Para_state_audioTrack_Disabled){
            _trackSelector?.parameters = _trackSelector!!
                .buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        Para_state_audioTrack_Disabled = false

    }
    //倍速管理
    private var Para_OriginalPlaySpeed = 1.0f
    fun setPlaySpeed(speed: Float){
        player.setPlaybackSpeed(speed)
        Para_OriginalPlaySpeed = speed
    }
    fun setPlaySpeedByLongPress(speed: Float){
        player.setPlaybackSpeed(speed)
    }
    fun getPlaySpeed(): Pair<Float, Float>{
        return Pair(player.playbackParameters.speed, Para_OriginalPlaySpeed)
    }
    //保存独立播放参数
    private fun saveParaToDataBase(fileName: String,currentPosition:Long, duration: Long){
        //1.保存播放进度
        saveProgress()
        //2.



    }




    //初始化播放器单例
    lateinit var objectContext: Context
    private var objectState_contextSet = false
    private fun setContext(context: Context) {
        if (objectState_contextSet) return
        objectContext = context.applicationContext
        objectState_contextSet = true
    }
    fun setupPlayerSingleton(app: Application){
        //设置上下文
        setContext(app)

        //启动监听器


    }



    //其他播放器功能
    //后台播放时关闭视频轨道
    fun ActivityOnResume(context: Context){
        stopBackgroundPlay(context)
    }
    fun ActivityOnStop(context: Context){
        startBackgroundPlay(context)
    }
    //开始/结束后台播放状态
    private fun startBackgroundPlay(context: Context){
        //开启后台播放功能：开始关闭视频轨道倒计时
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){
            if (SettingsRequestCenter.get_PREFS_DisableVideoTrackOnBack(context)){
                if (_player?.currentMediaItem != null && _player?.isPlaying == true){
                    closeVideoTrackJob()
                }
            }
        }
        //关闭后台播放功能：直接暂停
        else{ recessPlay(true) }
    }
    private fun stopBackgroundPlay(context: Context){
        //开启后台播放功能：关闭视频轨道倒计时 + 恢复视频轨道
        closeVideoTrackJob?.cancel()
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){
            if (Para_state_videoTrack_Disabled){ EnableVideoTrack() }
        }
        //关闭后台播放功能：开始继续播放
        else{
            if(playState_wasPlaying){
                continuePlay(need_requestFocus = false, force_request = true, need_fadeIn = true)
            }
        }
    }
    //关闭视频轨道倒计时
    private var coroutineScope_closeVideoTrackJob: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var closeVideoTrackJob: Job? = null
    private fun closeVideoTrackJob() {
        closeVideoTrackJob?.cancel()
        closeVideoTrackJob = coroutineScope_closeVideoTrackJob.launch {
            delay(60_000)
            DisableVideoTrack()
        }
    }
    //Runnable:保存播放进度
    private var coroutine_saveProgress = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var state_saveProgress_Running = false
    private val saveProgressHandler = Handler(Looper.getMainLooper())
    private var saveProgress = object : Runnable{
        override fun run() {

            saveProgress()

            saveProgressHandler.postDelayed(this, 20_000)
        }
    }
    private fun saveProgress(){
        val currentProgress = _player?.currentPosition?: -1L

        if (currentProgress == -1L) return
        if (!Para_saveLastProgress) return

        coroutine_saveProgress.launch {
            MediaItemRepo.get(objectContext).update_value_LastPosition(MediaInfo_FileName,currentProgress)
        }

    }
    private fun startSaveProgressHandler() {
        if (state_saveProgress_Running) return
        saveProgressHandler.post(saveProgress)
        state_saveProgress_Running = true
    }
    private fun stopSaveProgressHandler() {
        saveProgressHandler.removeCallbacks(saveProgress)
        state_saveProgress_Running = false
    }
    //定时关闭倒计时器
    private var timer_autoShut: CountDownTimer? = null
    private var countDownDuration_Ms = 0
    private var shutDownMoment = ""
    private var timerState_autoShut_Reach = false
    private fun timer_notification() {
        val channelId = "toast_replace"
        val nm = objectContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(objectContext, channelId)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .setContentTitle(null)
            .setContentText("本次播放完毕后将自动关闭")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)

    }
    private fun timer_DisableAutoShut(){
        countDownDuration_Ms = 0
        shutDownMoment = ""
        timerState_autoShut_Reach = false
        timer_autoShut?.cancel()
    }
    private fun timer_startAutoShut(countDownDuration_Ms: Int){
        timer_autoShut?.cancel()
        timer_autoShut = object : CountDownTimer(countDownDuration_Ms.toLong(), 1000000L) {
            override fun onTick( millisUntilFinished: Long) {}
            override fun onFinish() { timerState_autoShut_Reach = true }
        }.start()
    }
    private fun timer_autoShut_Reach() {
        //需等待当前媒体结束后关闭
        if (SettingsRequestCenter.get_PREFS_OnlyStopUnMediaEnd(objectContext)) {
            countDownDuration_Ms = 0
            shutDownMoment = "shutdown_when_end"
            timerState_autoShut_Reach = true
            timer_notification()
        }
        //直接关闭
        else{
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //关闭播放器
            stopPlayBundle(false,objectContext)
        }
    }
    fun set_timer_autoShut(CountDownDuration_Min: Int){
        //传入0即为关闭
        if (CountDownDuration_Min == 0){
            timer_DisableAutoShut()
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
        timer_startAutoShut(countDownDuration_Ms)
    }
    fun get_timer_autoShut(): String{
        return shutDownMoment
    }


//object END
}

