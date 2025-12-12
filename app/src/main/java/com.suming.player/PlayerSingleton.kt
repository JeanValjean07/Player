package com.suming.player

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.ScrubbingModeParameters
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
@Suppress("unused")
object PlayerSingleton {

    //播放器参数
    var _player: ExoPlayer? = null
    @SuppressLint("StaticFieldLeak")
    private var _trackSelector: DefaultTrackSelector? = null
    @SuppressLint("StaticFieldLeak")
    private var _rendererFactory: RenderersFactory? = null

    val player: ExoPlayer
        get() = _player ?: throw IllegalStateException("发生错误")

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
                playWhenReady = true
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
    var singleton_media_url = ""
    var singleton_media_cover_path = ""

    var singleton_player_built = false





    fun getPlayerState(): Int {
        if (singleton_player_built){
            return 1
        }else{
            return 0
        }
    }
    fun getCurrentMediaItem(): MediaItem? {
        return _player?.currentMediaItem
    }


    //功能1
    fun setMediaUri(uri: Uri) {
        _player?.setMediaItem(MediaItem.fromUri(uri))
    }
    fun setMediaItem(item: MediaItem) {
        _player?.setMediaItem(item)
    }
    fun setMediaInfo(type: String, title: String, url: String) {
        singleton_media_type = type
        singleton_media_title = title
        singleton_media_url = url
    }
    fun getMediaInfo(): Triple<String, String, String> {
        return Triple(singleton_media_type, singleton_media_title, singleton_media_url)
    }
    fun playPlayer() {
        _player?.play()
    }
    fun pausePlayer() {
        _player?.pause()
    }
    fun clearMediaItem() {
        _player?.clearMediaItems()
    }
    fun clearMediaInfo() {
        singleton_media_type = ""
        singleton_media_title = ""
        singleton_media_url = ""
    }


    //功能2
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


    //功能3
    fun getPlayer(app: Application): ExoPlayer = _player ?: synchronized(this) {
            _player ?: buildPlayer(app).also { _player = it }
        }.also {
            isPlayerInitialized = true
            initializationCallbacks.forEach { callback -> callback.invoke() }
            initializationCallbacks.clear()
        }

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