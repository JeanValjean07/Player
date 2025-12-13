package com.suming.player

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
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

    //播放器回调接口
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
    var singleton_media_type = ""
    var singleton_media_title = ""
    var singleton_media_artist = ""
    var singleton_media_url = ""
    var singleton_media_cover_path = ""
    var singleton_player_built = false
    var singleton_exit_before_read = false

    //播放列表

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
        return singleton_media_url
    }
    fun getMediaInfoForMain(): Triple<String, String, String> {
        return Triple(singleton_media_type, singleton_media_title, singleton_media_artist)
    }
    //主动设置媒体自定义状态
    fun setMediaInfo(type: String, title: String, artist: String, url: String) {
        singleton_media_type = type
        singleton_media_title = title
        singleton_media_artist = artist
        singleton_media_url = url
    }
    fun setMediaInfoUri(uri: String) {
        singleton_media_url = uri
    }
    //设置媒体项
    fun setMediaUri(uri: Uri) {
        _player?.setMediaItem(MediaItem.fromUri(uri))
    }
    fun setMediaItem(item: MediaItem) {
        _player?.setMediaItem(item)
    }
    //播放和暂停
    fun playPlayer() {
        _player?.play()
    }
    fun pausePlayer() {
        _player?.pause()
    }
    //清除媒体项和自定义信息
    fun clearMediaItem() {
        _player?.clearMediaItems()
    }
    fun clearMediaInfo() {
        singleton_media_type = ""
        singleton_media_title = ""
        singleton_media_url = ""
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