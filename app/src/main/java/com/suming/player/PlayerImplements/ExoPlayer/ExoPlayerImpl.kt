package com.suming.player.PlayerImplements.ExoPlayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.PlayerImplements.PlayerCallBack
import com.suming.player.PlayerImplements.PlayerInterface
import com.suming.player.R
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@UnstableApi
class ExoPlayerImpl(context: Context) : PlayerInterface {


    //init { }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ExoPlayerImpl: $msg")
        }
    }
    //播放器实例
    private var _player_exo: ExoPlayer ?= null
    //上下文
    private val context = context.applicationContext


    //播放器回调通道
    override var listener: PlayerCallBack? = null

    //播放器组件
    @SuppressLint("StaticFieldLeak")
    private var _trackSelector: DefaultTrackSelector? = null
    private var _rendererFactory: RenderersFactory? = null
    private fun get_trackSelector(context: Context): DefaultTrackSelector = _trackSelector ?: synchronized(this) {
        _trackSelector ?: DefaultTrackSelector(context)
            .also { _trackSelector = it }

    }
    private fun get_RendererFactory(context: Context): RenderersFactory = _rendererFactory ?: synchronized(this) {
        _rendererFactory ?: NextRenderersFactory(context)
            //允许解码器回退
            .setEnableDecoderFallback(true)
            .also { _rendererFactory = it }
    }
    private fun release_trackSelector(){
        //_trackSelector?.release()  //不知道为什么这一行执行会崩溃说线程错误
        _trackSelector = null
    }
    private fun release_RendererFactory(){
        _rendererFactory = null
    }
    @Suppress("unused")
    private fun create_customCodecFactory(): MediaCodecAdapter.Factory {
        @Suppress("DEPRECATION")
        return MediaCodecAdapter.Factory.DEFAULT
    }


    //初始化播放器 Core
    private fun buildPlayer_Core(): ExoPlayer {
        //consoleLog("buildPlayer")

        val trackSelector = get_trackSelector(context)
        val rendererFactory = get_RendererFactory(context)
        //创建播放器
        val ExoPlayer = ExoPlayer.Builder(context)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMaxSeekToPreviousPositionMs(1_000_000L)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .build()
            .apply {
                //别忘了自动调prepare()
                prepare()
                playWhenReady = false
            }


        return ExoPlayer
    }
    //初始化播放器
    override fun build_player() {
        //双重检查锁定初始化
        var player = _player_exo
        //
        if (player == null) {
            synchronized(this) {
                player = _player_exo
                if (player == null) {
                    player = buildPlayer_Core()
                    _player_exo = player

                }
            }
        }

        //添加播放器状态监听
        addPlayerStateListener()

    }
    //测试数据监听器
    private val decoderListener = object : AnalyticsListener {
        //视频解码器初始化
        override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName:String, initializedTimestampMs:Long, initializationDurationMs:Long) {

            //consoleLog("Video decoder initialized: $decoderName")
        }

        //视频输入格式变化
        override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {

            //consoleLog("Video format changed: $format")
        }
        //音频解码器初始化
        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            // consoleLog("Audio decoder initialized: $decoderName")
        }
        override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
            // consoleLog("Audio format changed: $format")
        }

    }
    //播放器回调监听器
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY ->  playState_Ready()
                Player.STATE_ENDED ->  playState_End()
                //播放器进入空闲状态
                Player.STATE_IDLE ->  on_EngineIdle()

            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //修改可观察标志,触发更新
            PlayerInfoCenter.updateObservableIsPlaying(isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            //consoleLog("onMediaItemTransition mediaItem:${mediaItem} reason:${reason}")
            onMediaItemChanged(mediaItem)
        }
        override fun onTracksChanged(tracks: Tracks) {
            for (trackGroup in tracks.groups) {
                val format = trackGroup.getTrackFormat(0)
                val fps = format.frameRate
                PlayerInfoCenter.SET_Media_ActualFPS(fps)
                break
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            on_EngineErrorOccur(error)
        }
    }
    //添加监听器
    private fun addPlayerStateListener(){
        //执行监听器添加
        _player_exo?.addListener(PlayerStateListener)
        _player_exo?.addAnalyticsListener(decoderListener)

    }
    private fun removePlayerStateListener(){
        //执行移除
        _player_exo?.removeListener(PlayerStateListener)
        _player_exo?.removeAnalyticsListener(decoderListener)
    }
    //添加插件(疑似没用?)
    override fun attach_addons() {

    }
    //添加更多插件(疑似没用?)
    override fun attach_more_addons() {

    }


    //播放回调
    //Ready
    private fun playState_Ready(){
        listener?.onReady()
    }
    //END
    private fun playState_End(){
        listener?.onMediaEnd()
    }
    //idle
    private fun on_EngineIdle(){
        listener?.onIdle()
    }
    //
    private fun on_EngineErrorOccur(error: PlaybackException){
        listener?.onError(error)
    }
    //
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null){
            listener?.onMediaItemCleared()
        }else{
            listener?.onMediaItemChange()
        }


    }



    //设置媒体项
    override fun setMediaItem(URI: Uri) {
        consoleLog("setMediaItem: $URI")
        if (_player_exo == null){
            consoleLog("setMediaItem: 播放器未初始化")
        }else{
            consoleLog("setMediaItem: 播放器已状态: ${_player_exo?.playbackState}")
        }

        _player_exo?.setMediaItem(MediaItem.fromUri(URI))
    }
    //调用播放器准备
    override fun prepare() {
        _player_exo?.prepare()
    }
    //调用播放器挂起
    override fun stop() {
        _player_exo?.stop()
    }

    //调用播放器开始播放
    override fun play() {
        _player_exo?.play()
    }
    //调用播放器暂停
    override fun pause() {
        _player_exo?.pause()
    }

    //连接到SurfaceView
    override fun attachSurfaceView(context: Context, view: FrameLayout) {
        //清除当前的音乐视图
        view.removeAllViews()
        //创建视频视图
        val PlayerView = LayoutInflater.from(context).inflate(R.layout.piece_media3_player_view_surface_ver, view, false) as PlayerView
        //添加视频视图
        view.addView(PlayerView)
        //添加视频视图后,绑定到视频
        PlayerView.post {

            //绑定到视频
            PlayerView.player = _player_exo

        }
    }
    //连接到textureView
    override fun attachTextureView(context: Context, view: FrameLayout) {
        //清除当前的音乐视图
        view.removeAllViews()
        //创建视频视图
        val PlayerView = LayoutInflater.from(context).inflate(R.layout.piece_media3_player_view_texture_ver, view, false) as PlayerView
        //添加视频视图
        view.addView(PlayerView)
        //添加视频视图后,绑定到视频
        PlayerView.post {

            //绑定到视频
            PlayerView.player = _player_exo

        }
    }



    override fun seekTo(positionMs: Long) {
        _player_exo?.seekTo(positionMs)
    }

    override fun clearMediaItem() {
        _player_exo?.clearMediaItems()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        _player_exo?.playWhenReady = playWhenReady
    }



    override fun release() {
        _player_exo?.release()
        _player_exo = null
        _trackSelector = null
        _rendererFactory = null
    }

    override val currentPosition: Long get() = _player_exo?.currentPosition ?: -1L

    override val duration: Long get() = _player_exo?.duration ?: -1L

    override val isPlaying: Boolean get() = _player_exo?.isPlaying ?: false


    //非极端不要直接取引用
    @Suppress("UNCHECKED_CAST")
    override fun <T> getEngine(): T? = _player_exo as T


}