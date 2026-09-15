package com.suming.player.PlayerImplements.ExoPlayer

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player as Media3Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.suming.player.PlayerImplements.PlayerInterface
import com.suming.player.PlayerImplements.PlayerCallBack
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@UnstableApi
class ExoPlayerImpl(context: Context) : PlayerInterface {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(
        context,
        NextRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
    ).build()

    override var listener: PlayerCallBack? = null


    init {
        exoPlayer.addListener(object : Media3Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                when (state){
                    Media3Player.STATE_READY -> listener?.onPrepared()
                    Media3Player.STATE_ENDED -> listener?.onCompletion()
                    Media3Player.STATE_BUFFERING -> listener?.onBuffering(0)
                    Media3Player.STATE_IDLE -> listener?.onIdle()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener?.onPlayingChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                listener?.onError(error)
            }
        })
    }

    override fun setMediaItem(URI: Uri)  {
        exoPlayer.setMediaItem(MediaItem.fromUri(URI))
    }

    override fun prepare() {
        exoPlayer.prepare()
    }

    override fun stop() {

    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        //exoPlayer.setVideoSurface(surfaceView.surfaceHolder.surface)
    }

    override fun attachTextureView(textureView: TextureView) {
       // exoPlayer.setVideoSurface(textureView.texture)
    }



    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun clearMediaItem() {

    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        exoPlayer.playWhenReady = playWhenReady
    }



    override fun release() {
        exoPlayer.release()
    }

    override val currentPosition: Long
        get() = exoPlayer.currentPosition

    override val duration: Long
        get() = exoPlayer.duration.coerceAtLeast(0L)

    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying


    @Suppress("UNCHECKED_CAST")
    override fun <T> getEngine(): T? = exoPlayer as T


}