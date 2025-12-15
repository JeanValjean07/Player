package com.suming.player

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
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
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

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
                Player.STATE_READY -> {

                }
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
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)

        }
        override fun onTracksChanged(tracks: Tracks) {

        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            ToolEventBus.sendEvent("PlayerSingleton_MediaItemChanged")
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
    //媒体信息
    var singleton_media_type = ""
    var singleton_media_title = ""
    var singleton_media_file_name = ""
    var singleton_media_artist = ""
    var singleton_media_uri = ""
    var singleton_media_absolute_path = ""
    var singleton_media_duration = 0

    //媒体信息
    private lateinit var retriever: MediaMetadataRetriever
    private fun getMediaInfo(context: Context, uri: Uri){
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(context, uri) }
        catch (_: Exception) {
            return
        }
        singleton_media_absolute_path = getAbsoluteFilePath(context, uri).toString()
        singleton_media_title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "error"
        singleton_media_artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "error"
        singleton_media_duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
        singleton_media_file_name = (File(singleton_media_absolute_path)).name ?: "error"
        if (singleton_media_file_name == "error"){
            singleton_media_title = "未知媒体标题"
        }
        if (singleton_media_artist == "error"){
            singleton_media_artist = "未知艺术家"
        }
        retriever.release()

    }
    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
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
    //播放列表
    private val coroutineScope_getPlayList = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var PREFS_MediaStore: SharedPreferences
    private lateinit var mediaItems: List<MediaItemForVideo>
    private var currentMediaIndex = 0
    private var maxMediaIndex = 0
    private var state_PlayListProcess_Complete = false
    private var MediaInfo_VideoUri: Uri = Uri.EMPTY
    private var PREFS_InsertPreviewInMediaSession = true
    private var switchMedia_source: String = ""
    fun readDataBaseThenGatherPlayList(context: Context){

        if (state_PlayListProcess_Complete){ return }

        coroutineScope_getPlayList.launch(Dispatchers.IO) {
            //读取设置
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
            val sortOrder = PREFS_MediaStore.getString("PREFS_SortOrder", "info_title") ?: "info_title"
            val sortOrientation = PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") ?: "DESC"
            //读取所有媒体
            val mediaStoreRepo = MediaStoreRepo.get(singletonContext)
            val mediaStoreSettings = mediaStoreRepo.getAllVideosSorted(sortOrder, sortOrientation)
            mediaItems = mediaStoreSettings
                .filter { setting -> !setting.info_is_hidden }
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_Uri_numOnly.toLongOrNull() ?: 0,
                        uri = setting.info_uri_full.toUri(),
                        name = setting.info_title,
                        durationMs = setting.info_duration,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
                        isHidden = setting.info_is_hidden
                    )
                }
            //反定位当前媒体index
            currentMediaIndex = mediaItems.indexOfFirst { it.uri.toString() == singleton_media_uri }
            maxMediaIndex = mediaItems.size - 1

            //保存完后公布状态
            state_PlayListProcess_Complete = true
        }


    }
    fun getPlayList(context: Context): List<MediaItemForVideo>{
        if (!state_PlayListProcess_Complete){
            context.showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
            return emptyList()
        }

        return mediaItems
    }
    fun getPlayListProcessComplete(): Boolean{
        return state_PlayListProcess_Complete
    }
    private fun confirmSwitchMediaItem(itemUri: Uri){
        //保存新视频链接
        MediaInfo_VideoUri = itemUri
        singleton_media_uri = itemUri.toString()
        //获取新视频信息
        getMediaInfo(singletonContext, MediaInfo_VideoUri)

        //合成并设置媒体项
        val covers_path = File(singletonContext.filesDir, "miniature/cover")
        val cover_img_path = File(covers_path, "${singleton_media_file_name.hashCode()}.webp")
        val cover_img_uri = if (PREFS_InsertPreviewInMediaSession && cover_img_path.exists()) {
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
            .setUri(MediaInfo_VideoUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(singleton_media_title)
                    .setArtist(singleton_media_artist)
                    .setArtworkUri( cover_img_uri )
                    .build()
            )
            .build()
        _player?.setMediaItem(mediaItem)
        _player?.play()

        //反定位当前媒体index
        currentMediaIndex = mediaItems.indexOfFirst { it.uri == itemUri }
    }
    private fun prepareSwitchMediaItem(itemUri: Uri, itemName: String){
        //检测是否是当前媒体
        if (itemName == singleton_media_file_name){
            singletonContext.showCustomToast("您已在播放此视频",Toast.LENGTH_SHORT, 3)
            ToolEventBus.sendEvent("restart_PlayerListFragment_From")
            return
        }
        else{
            confirmSwitchMediaItem(itemUri)
        }
    }
    private fun preparePreviousMediaItem(context: Context){
        if (currentMediaIndex == 0) {
            context.showCustomToast("已在列表起始，将切换至最后一曲",Toast.LENGTH_SHORT, 3)
            val newUri = mediaItems.getOrNull(maxMediaIndex)?.uri
            if (newUri != null) {
                confirmSwitchMediaItem(newUri)
            }
            else{
                context.showCustomToast("上一曲获取失败",Toast.LENGTH_SHORT, 3)
            }
        }
        else{
            val newUri = mediaItems.getOrNull(currentMediaIndex - 1)?.uri
            if (newUri != null) {
                confirmSwitchMediaItem(newUri)
            }
            else{
                context.showCustomToast("上一曲获取失败",Toast.LENGTH_SHORT, 3)
            }
        }
    }
    private fun prepareNextMediaItem(context: Context){
        if (currentMediaIndex == maxMediaIndex) {
            context.showCustomToast("已在列表末尾，将切换至第一曲",Toast.LENGTH_SHORT, 3)
            val newUri = mediaItems.getOrNull(0)?.uri
            if (newUri != null) {
                confirmSwitchMediaItem(newUri)
            }
            else{
                context.showCustomToast("下一曲获取失败",Toast.LENGTH_SHORT, 3)
            }
        }
        else{
            val newUri = mediaItems.getOrNull(currentMediaIndex + 1)?.uri
            if (newUri != null) {
                confirmSwitchMediaItem(newUri)
            }
            else{
                context.showCustomToast("下一曲获取失败",Toast.LENGTH_SHORT, 3)
            }
        }
    }
    private fun checkSource(source_activity_name: String){
        switchMedia_source = source_activity_name
        if (switchMedia_source != "MainActivity" && switchMedia_source != "PlayerActivity" && switchMedia_source != "PlayerActivitySeekBar"){
            singletonContext.showCustomToast("切换媒体命令来源无法验证,已设为默认来源",Toast.LENGTH_SHORT, 3)
            switchMedia_source = "PlayerActivity"
        }
    }
    fun switchToPreviousMediaItem(source: String){
        checkSource(source)
        preparePreviousMediaItem(singletonContext)
    }
    fun switchToNextMediaItem(source: String){
        checkSource(source)
        prepareNextMediaItem(singletonContext)
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

    //初始化上下文
    private lateinit var singletonContext: Context
    private var state_ContextSet = false
    fun setContext(ctx: Context) {
        singletonContext = ctx.applicationContext
        state_ContextSet = true
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
        if (!state_AudioManager_Initialized){
            initAudioManager(context)
        }
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
        audioManager.requestAudioFocus(focusRequest)
    }
    fun releaseAudioFocus(context: Context){
        if (!state_focusRequest_Initialized){
            initFocusRequest(context)
        }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
    //事件总线
    //RxJava事件总线
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
                prepareNextMediaItem(singletonContext)
            }
            "SessionController_Previous" -> {
                preparePreviousMediaItem(singletonContext)
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
    //连接到媒体会话控制器
    fun connectToMediaSession(context: Context){
        val SessionToken = SessionToken(context as Application, ComponentName(context, PlayerService::class.java))
        MediaSessionController = MediaController.Builder(context, SessionToken).buildAsync()
        MediaSessionController?.addListener({
            controller = MediaSessionController?.get()
        }, MoreExecutors.directExecutor())
    }
    //关闭媒体会话控制器:同时在活动关闭服务和在单例断开控制器,才能确保播控中心消失
    fun stopMediaSessionController(context: Context){
        MediaSessionController?.get()?.run { release() }
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
        return singleton_media_uri
    }
    fun getMediaInfoForMain(): Triple<String, String, String> {
        return Triple(singleton_media_type, singleton_media_file_name, singleton_media_artist)
    }
    //主动设置媒体自定义状态
    fun setMediaInfo(type: String, title: String, artist: String, url: String) {
        singleton_media_type = type
        singleton_media_file_name = title
        singleton_media_artist = artist
        singleton_media_uri = url
    }
    fun setMediaInfoUri(uri: String) {
        singleton_media_uri = uri
    }
    //设置媒体项
    fun setMediaUri(uri: Uri) {
        _player?.setMediaItem(MediaItem.fromUri(uri))
    }
    fun setMediaItem(item: MediaItem) {
        _player?.setMediaItem(item)
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
        //判断
        when (singleton_repeat_mode) {
            "ONE" -> {
                _player?.seekTo(0)
                _player?.play()
            }
            "ALL" -> {
                prepareNextMediaItem(singletonContext)
            }
            "OFF" -> {
                playState_playEnd = true
                _player?.pause()
            }
        }
    }
    //清除媒体项和自定义信息
    fun clearMediaItem() {
        _player?.clearMediaItems()
    }
    fun clearMediaInfo() {
        singleton_media_type = ""
        singleton_media_title = ""
        singleton_media_uri = ""
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
                .also { _rendererFactory = it }
        }
    fun createCustomCodecFactory(): MediaCodecAdapter.Factory {
        return MediaCodecAdapter.Factory.DEFAULT
    }




//singleton object END
}