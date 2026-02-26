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
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
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

@SuppressLint("StaticFieldLeak")
@UnstableApi
//@Suppress("unused")
object PlayerSingleton {
    //播放器实例
    private var _player: ExoPlayer? = null
    //播放器组件
    fun getTrackSelector(context: Context): DefaultTrackSelector =
        inner_trackSelector ?: synchronized(this) {
            inner_trackSelector ?: DefaultTrackSelector(context)
                .also { inner_trackSelector = it }

        }
    fun getRendererFactory(context: Context): RenderersFactory =
        inner_rendererFactory ?: synchronized(this) {
            inner_rendererFactory ?: DefaultRenderersFactory(context)
                //.setEnableDecoderFallback(true)
                .also { inner_rendererFactory = it }
        }
    fun createCustomCodecFactory(): MediaCodecAdapter.Factory {
        return MediaCodecAdapter.Factory.DEFAULT
    }
    private var inner_trackSelector: DefaultTrackSelector? = null
    private var inner_rendererFactory: RenderersFactory? = null
    //外部获取播放器&组件
    fun getPlayerFromService(): ExoPlayer?{
        return _player
    }
    //创建播放器
    private fun buildPlayer(context: Context): ExoPlayer {
        val trackSelector = getTrackSelector(context)
        val rendererFactory = getRendererFactory(context)
        //创建播放器
        val ExoPlayer = ExoPlayer.Builder(context)
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

        return ExoPlayer
    }
    fun getPlayer(context: Context): ExoPlayer = _player ?: synchronized(this) {
        _player ?: buildPlayer(context).also { _player = it }
    }.also {
        stateLock_isPlayerInitialized = true
        initializationCallbacks.forEach { callback -> callback.invoke() }
        initializationCallbacks.clear()
        //添加播放器状态监听
        addPlayerStateListener()
    }
    //播放器初始化监听
    private val initializationCallbacks = mutableListOf<() -> Unit>()
    private var stateLock_isPlayerInitialized = false
    private fun addInitializationCallback(callback: () -> Unit) {
        synchronized(initializationCallbacks) {
            if (stateLock_isPlayerInitialized && _player != null) {
                callback.invoke()
            } else {
                initializationCallbacks.add(callback)
            }
        }
    }
    //播放器回调监听
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> { playerReady() }
                Player.STATE_ENDED -> {
                    playEnd(contextApplication)
                }
                Player.STATE_IDLE -> {

                }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            ToolEventBus.sendEvent("PlayerSingleton_PlaybackStateChanged")
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onMediaItemChanged(mediaItem,contextApplication)
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

        }
    }
    private var playerState_PlayerStateListenerAdded = false
    fun addPlayerStateListener(){
        if (playerState_PlayerStateListenerAdded) return

        _player?.removeListener(PlayerStateListener)
        _player?.addListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = true
    }
    fun removePlayerStateListener(){
        _player?.removeListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = false
    }


    //
    private lateinit var contextApplication: Context
    fun setContext(context: Context){
        contextApplication = context.applicationContext
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


    //👀丨媒体信息集中管理
    //<editor-fold desc="//媒体信息变量合集">
    private var MediaInfo_MediaUniqueID = ""  //媒体唯一ID()
    private var MediaInfo_MediaType = ""
    private var MediaInfo_MediaTitle = ""
    private var MediaInfo_MediaArtist = ""
    private var MediaInfo_FileName = ""
    private var MediaInfo_Duration = 0L
    private var MediaInfo_AbsolutePath = ""
    private var MediaInfo_MediaUri = Uri.EMPTY!!  //原始获取媒体链接
    private var MediaInfo_MediaUriString = ""          //原始获取媒体链接字符串
    private var MediaInfo_MediaUriStandard = ""        //标准链接格式(content://media/external/(?video|audio)/media/114514)
    //</editor-fold>
    //媒体信息解码器
    //<editor-fold desc="//媒体信息解码工具函数&子线程">
    private lateinit var retriever: MediaMetadataRetriever
    //子线程丨计算 UniqueID & 标准链接
    private var coroutine_getMediaUniqueID = CoroutineScope(Dispatchers.IO)
    private fun calculateUniqueID(mediaUri: Uri){
        coroutine_getMediaUniqueID.launch {
            //计算媒体唯一识别ID
            val NEW_MediaInfo_MediaStoreID = MediaUriManager.getMediaIDByMediaUri(mediaUri,contextApplication)
            //获取标准链接
            val NEW_MediaInfo_MediaUriStandard = MediaUriManager.getStandardMediaUri(mediaUri,contextApplication)

            //刷新变量到本地
            MediaInfo_MediaUniqueID = NEW_MediaInfo_MediaStoreID
            MediaInfo_MediaUriStandard = NEW_MediaInfo_MediaUriStandard.toString()

            //计入播放记录
            MediaRecordManager(contextApplication).save_MediaInfo_toRecordUniqueID_main(NEW_MediaInfo_MediaStoreID)
        }
    }
    //工具函数丨根据uri获得绝对路径
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
    //</editor-fold>
    private fun getMediaInfo(context: Context, uri: Uri): Boolean{
        retriever = MediaMetadataRetriever()
        //尝试解码
        try {
            retriever.setDataSource(context, uri)
        }catch(_: Exception){
            return false
        }
        //解码获得新媒体的信息
        val NEW_MediaInfo_MediaUri = uri
        val NEW_MediaInfo_MediaUriString = uri.toString()
        var NEW_MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
        var NEW_MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        var NEW_MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val NEW_MediaInfo_Duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
        val NEW_MediaInfo_AbsolutePath = getFilePath(context, uri).toString()
        val NEW_MediaInfo_FileName = (File(NEW_MediaInfo_AbsolutePath)).name ?: ""
        val NEW_MediaInfo_VideoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val NEW_MediaInfo_VideoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        //处理值
        if (NEW_MediaInfo_MediaType.contains("video")){
            NEW_MediaInfo_MediaType = "video"
        }else if(NEW_MediaInfo_MediaType.contains("audio")){
            NEW_MediaInfo_MediaType = "music"
        }
        if (NEW_MediaInfo_MediaTitle == ""){ NEW_MediaInfo_MediaTitle = "未知媒体标题" }
        if (NEW_MediaInfo_MediaArtist == "" || NEW_MediaInfo_MediaArtist == "<unknown>"){ NEW_MediaInfo_MediaArtist = "未知艺术家" }
        //计算媒体ID
        calculateUniqueID(NEW_MediaInfo_MediaUri)

        //刷新本地媒体信息变量
        fun updateMediaInfoValues(NEW_MediaInfo_MediaType: String,NEW_MediaInfo_MediaTitle: String,
                                      NEW_MediaInfo_MediaArtist: String,
                                      NEW_MediaInfo_FileName: String,
                                      NEW_MediaInfo_Duration: Long,
                                      NEW_MediaInfo_AbsolutePath: String,
                                      NEW_MediaInfo_MediaUri: Uri,
                                      NEW_MediaInfo_MediaUriString: String,
                                      NEW_MediaInfo_VideoWidth: Int,
                                      NEW_MediaInfo_VideoHeight: Int){
            MediaInfo_MediaType = NEW_MediaInfo_MediaType
            MediaInfo_MediaTitle = NEW_MediaInfo_MediaTitle
            MediaInfo_MediaArtist = NEW_MediaInfo_MediaArtist
            MediaInfo_FileName = NEW_MediaInfo_FileName
            MediaInfo_Duration = NEW_MediaInfo_Duration
            MediaInfo_AbsolutePath = NEW_MediaInfo_AbsolutePath
            MediaInfo_MediaUri = NEW_MediaInfo_MediaUri
            MediaInfo_MediaUriString = NEW_MediaInfo_MediaUriString
            MediaInfo_VideoWidth = NEW_MediaInfo_VideoWidth
            MediaInfo_VideoHeight = NEW_MediaInfo_VideoHeight
        }
        updateMediaInfoValues(
            NEW_MediaInfo_MediaType,
            NEW_MediaInfo_MediaTitle,
            NEW_MediaInfo_MediaArtist,
            NEW_MediaInfo_FileName,
            NEW_MediaInfo_Duration,
            NEW_MediaInfo_AbsolutePath,
            NEW_MediaInfo_MediaUri,
            NEW_MediaInfo_MediaUriString,
            NEW_MediaInfo_VideoWidth,
            NEW_MediaInfo_VideoHeight,
        )

        //释放资源
        retriever.release()
        return true
    }
    //外部获取信息
    //<editor-fold desc="//外部获取当前信息接口">
    //获取当前媒体的 视频宽高值&获取比例
    fun getMediaWHratio(): Float {
        //获取视频宽高比
        val ratio_W_by_H = MediaInfo_VideoWidth.toFloat() / MediaInfo_VideoHeight.toFloat()

        return ratio_W_by_H
    }
    private var MediaInfo_VideoWidth = 0
    private var MediaInfo_VideoHeight = 0
    //获取当前媒体的 播放进度
    fun getMediaCurrentPosition(): Long {
        return _player?.currentPosition ?: -1
    }
    //获取当前媒体的 唯一身份识别(类型+ID)
    fun getCurrentMediaIdentity(): Pair<String, String> {
        return Pair(MediaInfo_MediaType, MediaInfo_MediaUniqueID)
    }
    //获取当前媒体的 唯一ID
    fun getCurrentMediaUniqueID(): String {
        return MediaInfo_MediaUniqueID
    }
    //判断传入的链接是否指向正在播放的项
    fun isthisUriOngoing(uriNeedCheck: Uri): Boolean {
        //如果传入标准链接,就直接对比标准链接
        if (MediaUriManager.isMediaUriStandard(uriNeedCheck)){

            return uriNeedCheck.toString() == MediaInfo_MediaUriStandard
        }
        //若不是标准链接,先转成标准链接,再对比
        val standardUriNeedCheck = MediaUriManager.getStandardMediaUri(uriNeedCheck,contextApplication)

        return standardUriNeedCheck.toString() == MediaInfo_MediaUriStandard
    }
    //获取当前媒体的 标准链接
    fun getCurrentMediaStandardUriString(): String {

        return MediaInfo_MediaUriStandard
    }


    //</editor-fold>



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



    //???
    fun clearMediaInfo(context: Context) {
        MediaInfo_MediaType = ""
        MediaInfo_MediaTitle = ""
        MediaInfo_MediaArtist = ""
        MediaInfo_MediaUriString = ""
        MediaInfo_MediaUri = Uri.EMPTY
        //写入配置
        clearLastMediaRecord(context)
    }



    //👀丨媒体项变更流程
    //确认设置新媒体项丨私有
    private fun setNewMediaItem(itemUri: Uri, playWhenReady: Boolean, context: Context): Boolean {
        //先判断是否是正在播放的媒体
        if (isthisUriOngoing(itemUri)) return false

        //进入设置新媒体项的流程
        //保存上个媒体的信息
        val oldItemName = MediaInfo_FileName
        val oldItemDuration = MediaInfo_Duration
        val currentPosition = getMediaCurrentPosition()
        coroutine_saveOldItemData.launch {

            if (MediaInfo_FileName.isEmpty()) return@launch
            if (currentPosition == -1L) return@launch

            withContext(Dispatchers.Main){
                saveOldItemData(oldItemName,currentPosition, oldItemDuration, context)
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
            //.setMediaId(MediaInfo_MediaUriString)
            .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(MediaInfo_FileName)
                    .setArtist(MediaInfo_MediaArtist)
                    .setArtworkUri(cover_img_uri)
                    .build())
            .build()
        _player?.setMediaItem(mediaItem)

        return true
    }
    //设置媒体项丨公共函数丨需要带一层过滤
    fun setMediaItem(itemUri: Uri, playWhenReady: Boolean, context: Context): Boolean {
        //设置新媒体项
        val success = setNewMediaItem(itemUri, playWhenReady, context)

        return success
    }
    //保存上个媒体的需保存内容
    private var coroutine_saveOldItemData = CoroutineScope(Dispatchers.IO)
    private fun saveOldItemData(fileName: String, currentPosition: Long, duration: Long, context: Context){

        saveParaToDataBase(fileName, currentPosition, duration, context)

    }
    //完成媒体项变更丨后续操作
    private fun onMediaItemChanged(mediaItem: MediaItem?, context: Context){
        if (mediaItem == null) return

        //启动服务
        startService(context)
        //记录到上次播放清单
        coroutine_saveLastMediaRecord.launch {
            saveLastMediaRecordMain(context)
        }
        //读取单个媒体播放设置
        coroutine_saveOrFetchDataBase.launch {
            FetchDataBaseForItem(MediaInfo_FileName ,context)

        }


        //发布通告
        ToolEventBus.sendEvent("PlayerSingleton_MediaItemChanged")
        //请求音频焦点
        requestAudioFocus(context, force_request = false)

    }
    //启动服务和媒体会话
    private fun startService(context: Context){
        //写入服务配置
        setServiceLinker()
        //链接媒体会话
        startMediaSession(context)
    }
    private fun setServiceLinker(){
        //写入媒体类型
        PlayerServiceLinker.setMediaInfo_MediaType(MediaInfo_MediaType)
        //写入基本信息
        PlayerServiceLinker.setMediaBasicInfo(MediaInfo_MediaUriString, MediaInfo_FileName, MediaInfo_MediaArtist)
    }
    private fun startMediaSession(context: Context){
        connectToMediaSession(context)
    }
    //写入上次播放记录丨私有函数丨可作为一条单独线程
    private var coroutine_saveLastMediaRecord = CoroutineScope(Dispatchers.IO)
    private fun saveLastMediaRecordMain(context: Context){
        //把记录保存到记录管理器
        MediaRecordManager(context).save_MediaInfo_toRecord_main(MediaInfo_FileName, MediaInfo_MediaArtist, MediaInfo_MediaType)
    }
    private fun clearLastMediaRecord(context: Context){
        //清除记录管理器中的记录
        MediaRecordManager(context).clear_MediaInfo()
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
    private fun stopServices(context: Context){
        context.stopService(Intent(context, PlayerService::class.java))
        sessionState_MediaSession_connected = false
    }
    //清除媒体会话
    private fun stopMediaSession(context: Context){
        stopMediaSessionController()
        stopServices(context)
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
    private fun DevastateMediaSessionBundle(context: Context){
        stopMediaSession(context)
    }
    //关闭监听器
    private fun DevastateListener(context: Context){
        stopListener(context = context)
    }
    //公共函数
    fun stopPlayBundle(need_clear_record: Boolean, context: Context){
        //清除播放记录
        if (need_clear_record){ clearLastMediaRecord(context) }
        //关闭媒体会话
        DevastateMediaSessionBundle(context)
        //关闭播放器
        DevastatePlayEnginBundle(context)
        //关闭监听器
        DevastateListener(context)
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
            val mediaStoreRepo = MediaStoreRepo.get(context)
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
            val mediaStoreRepo = MediaStoreRepo.get(context)
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
    private fun isNewUriValid(uri: Uri,context: Context): Boolean{
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
        val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
                setVolumeLimit(contextApplication)
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
    private var coroutine_focusRequest_wait = CoroutineScope(Dispatchers.IO)
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
                            continuePlay(need_requestFocus = true, force_request = true, need_fadeIn = true, context)
                        }

                    }
                }
            }
            .build()
        state_focusRequest_Initialized = true
    }
    private fun requestAudioFocus(context: Context, force_request: Boolean){
        coroutine_focusRequest_wait.launch {
            if (!state_focusRequest_Initialized){
                initFocusRequest(context)
            }
            if (!state_AudioManager_Initialized){
                initAudioManager(context)
            }
            delay(500)

            withContext(Dispatchers.Main){
                if (force_request){
                    audioManager.requestAudioFocus(focusRequest)
                }else{
                    if (_player == null) return@withContext
                    if (_player?.isPlaying == true ) {
                        audioManager.requestAudioFocus(focusRequest)
                    }

                }
            }

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
                HandlePlayerEvent(it,context)
            }, {
                context.showCustomToast("singleton事件总线注册失败:${it.message}",3)
            })
    }
    private fun HandlePlayerEvent(event: String, context: Context) {
        when (event) {
            "SessionController_Next" -> {
                switchToNextMediaItem(context)
            }
            "SessionController_Previous" -> {
                switchToPreviousMediaItem(context)
            }
            "SessionController_Play" -> {
                setWasPlaying(true)
                requestAudioFocus(context, force_request = false)
            }
            "SessionController_Pause" -> {
                setWasPlaying(false)
            }
        }
    }
    //开启/关闭所有监听器
    fun startListener(context: Context){
        registerEventBus(context)
        startAudioDeviceCallback(context)
        initFocusRequest(context)
    }
    fun stopListener(context: Context){
        unregisterEventBus()
        stopAudioDeviceCallback(context)
        releaseAudioFocus(context)
    }



    //获取播放器播放状态 < Boolean=是否有媒体正在播放 丨 Uri：链接 >
    fun getPlayState(): Pair<Boolean, Uri> {
        return if (_player?.currentMediaItem == null){
            Pair(false, Uri.EMPTY)
        }else{
            Pair(true, MediaInfo_MediaUri)
        }
    } //获取当前播放状态
    fun getIsPlaying(): Boolean {
        return _player?.isPlaying ?: false
    } //是否正在播放
    fun getCurrentMediaItem(): MediaItem? {
        val currentMediaItem = _player?.currentMediaItem

        return currentMediaItem
    } //获取当前媒体项
    //播放和暂停
    private var playState_playEnd = false
    private var playState_wasPlaying = false
    fun continuePlay(need_requestFocus: Boolean, force_request: Boolean, need_fadeIn: Boolean, context: Context) {
        if (playState_playEnd){
            playState_playEnd = false
            _player?.seekTo(0)
        }
        playState_wasPlaying = true


        //请求音频焦点
        if (need_requestFocus) requestAudioFocus(context, force_request)

        //保险：重置倍速
        if (_player != null && _player?.playbackParameters?.speed != Para_OriginalPlaySpeed){
            _player?.setPlaybackSpeed(Para_OriginalPlaySpeed)
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
    private fun playEnd(context: Context){
        //本次播放完成后关闭
        if (timerState_autoShut_Reach){
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //关闭
            stopPlayBundle(false,context)
        }
        //从列表管理器获取循环模式
        val currentLoopMode = PlayerListManager.getLoopMode(context)
        //根据循环模式执行不同操作
        when (currentLoopMode) {
            "ONE" -> {
                _player?.seekTo(0)
                continuePlay(need_requestFocus = false, force_request = false, need_fadeIn = false, context)
            }
            "ALL" -> {
                switchToNextMediaItem(context)
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
    fun set_Para_saveLastProgress(boolean: Boolean, context: Context){
        Para_saveLastProgress = boolean

        //保存到数据库
        coroutine_saveOrFetchDataBase.launch {
            MediaItemRepo.get(context).update_PREFS_saveLastPosition(MediaInfo_FileName,boolean)
        }
        //开启保存进度循环
        if (boolean){ startSaveProgressHandler() }else{ stopSaveProgressHandler() }

    }
    fun get_Para_DisableAudioTrack(): Boolean{
        return Para_DisableAudioTrack
    }
    fun set_Para_DisableAudioTrack(boolean: Boolean, immediateApply: Boolean, context: Context){
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
            MediaItemRepo.get(context).update_PREFS_VideoOnly(MediaInfo_FileName,boolean)
        }
    }
    fun get_Para_DisableVideoTrack(): Boolean{
        return Para_DisableVideoTrack
    }
    fun set_Para_DisableVideoTrack(boolean: Boolean, immediateApply: Boolean, context: Context){
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
            MediaItemRepo.get(context).update_PREFS_SoundOnly(MediaInfo_FileName,boolean)
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
    private fun FetchDataBaseForItem(itemName: String, context: Context){
        coroutine_saveOrFetchDataBase.launch {
            //读取保存的进度
            Para_saveLastProgress = MediaItemRepo.get(context).get_PREFS_saveLastPosition(MediaInfo_FileName)
            paraApply_lastProgress = if (Para_saveLastProgress){
                MediaItemRepo.get(context).get_value_LastPosition(MediaInfo_FileName)
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
        inner_trackSelector?.parameters = inner_trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()

        Para_state_videoTrack_Disabled = true

    }
    fun EnableVideoTrack(){
        //
        if (Para_state_videoTrack_Disabled){
            inner_trackSelector?.parameters = inner_trackSelector!!
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
        inner_trackSelector?.parameters = inner_trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()

        Para_state_audioTrack_Disabled = true

    }
    fun EnableAudioTrack(){
        //
        if (Para_state_audioTrack_Disabled){
            inner_trackSelector?.parameters = inner_trackSelector!!
                .buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        Para_state_audioTrack_Disabled = false

    }
    //倍速管理
    private var Para_OriginalPlaySpeed = 1.0f
    fun setPlaySpeed(speed: Float){
        _player?.setPlaybackSpeed(speed)
        Para_OriginalPlaySpeed = speed
    }
    fun setPlaySpeedByLongPress(speed: Float){
        _player?.setPlaybackSpeed(speed)
    }
    fun getPlaySpeed(): Pair<Float, Float>{
        return Pair(_player?.playbackParameters?.speed ?: 1.0f, Para_OriginalPlaySpeed)
    }
    //保存独立播放参数
    private fun saveParaToDataBase(fileName: String,currentPosition:Long, duration: Long, context: Context){
        //1.保存播放进度
        saveProgress(context)
        //2.



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
                continuePlay(need_requestFocus = false, force_request = true, need_fadeIn = true, context)
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

            saveProgress(contextApplication)

            saveProgressHandler.postDelayed(this, 20_000)
        }
    }
    private fun saveProgress(context: Context){
        val currentProgress = _player?.currentPosition?: -1L

        if (currentProgress == -1L) return
        if (!Para_saveLastProgress) return

        coroutine_saveProgress.launch {
            MediaItemRepo.get(context).update_value_LastPosition(MediaInfo_FileName,currentProgress)
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
    private fun timer_notification(context: Context) {
        val channelId = "toast_replace"
        val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
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
    private fun timer_autoShut_Reach(context: Context) {
        //需等待当前媒体结束后关闭
        if (SettingsRequestCenter.get_PREFS_OnlyStopUnMediaEnd(context)) {
            countDownDuration_Ms = 0
            shutDownMoment = "shutdown_when_end"
            timerState_autoShut_Reach = true
            timer_notification(context)
        }
        //直接关闭
        else{
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //关闭播放器
            stopPlayBundle(false,context)
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

