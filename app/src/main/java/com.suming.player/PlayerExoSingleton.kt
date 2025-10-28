package com.suming.player

import android.annotation.SuppressLint
import android.app.Application
import androidx.media3.common.C
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.ScrubbingModeParameters
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.internal.http2.Http2Reader
import java.util.logging.Handler

@UnstableApi
@Suppress("unused")
object PlayerExoSingleton {

    var _player: ExoPlayer? = null
    @SuppressLint("StaticFieldLeak")
    private var _trackSelector: DefaultTrackSelector? = null
    @SuppressLint("StaticFieldLeak")
    private var _rendererFactory: RenderersFactory? = null

    val player: ExoPlayer
        get() = _player ?: throw IllegalStateException("Player not initialized")

    //创建播放器实例
    private fun buildPlayer(app: Application): ExoPlayer {
        val trackSelector = getTrackSelector(app)
        val rendererFactory = getRendererFactory(app)

        val scrubbingParams = ScrubbingModeParameters.Builder()
            .setAllowSkippingMediaCodecFlush(true)
            .setShouldEnableDynamicScheduling(true)
            .build()


        return ExoPlayer.Builder(app)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .setScrubbingModeParameters(scrubbingParams)
            .build()
            .apply {
                prepare()
                playWhenReady = true
            }
    }

    //功能
    fun getPlayer(app: Application): ExoPlayer = _player ?: synchronized(this) {
            _player ?: buildPlayer(app).also { _player = it }
        }

    fun getTrackSelector(app: Application): DefaultTrackSelector =
        _trackSelector ?: synchronized(this) {
            _trackSelector ?: DefaultTrackSelector(app).also { _trackSelector = it }
        }

    fun getRendererFactory(app: Application): RenderersFactory =
        _rendererFactory ?: synchronized(this) {
            _rendererFactory ?: DefaultRenderersFactory(app).also { _rendererFactory = it }
        }

    fun releasePlayer() {
        _player?.release()
        _player = null
        _trackSelector = null
    }

    fun stopPlayer() {
        _player?.stop()
    }

    fun pausePlayer() {
        _player?.pause()
    }
}