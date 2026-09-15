package com.suming.player.PlayerImplements.MediaPlayer

import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import com.suming.player.PlayerImplements.PlayerInterface
import com.suming.player.PlayerImplements.PlayerCallBack
import android.media.MediaPlayer as SystemMediaPlayer

class MediaPlayerImpl(context: Context) : PlayerInterface {


    private val mediaPlayer: SystemMediaPlayer = SystemMediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
        )
    }


    override var listener: PlayerCallBack? = null


    init {
        mediaPlayer.setOnPreparedListener {
            listener?.onPrepared()
        }
        mediaPlayer.setOnCompletionListener {
            listener?.onCompletion()
        }
        mediaPlayer.setOnErrorListener { _, what, extra ->
            listener?.onError(RuntimeException("MediaPlayer error: what=$what, extra=$extra"))
            true
        }
        mediaPlayer.setOnBufferingUpdateListener { _, percent ->
            listener?.onBuffering(percent)
        }
    }


    override fun setMediaItem(URI: Uri) {
       // mediaPlayer.setDataSource(URI)
    }

    override fun prepare() {
        mediaPlayer.prepareAsync()
    }

    override fun stop() {

    }

    override fun play() {
        mediaPlayer.start()
        listener?.onPlayingChanged(true)
    }

    override fun pause() {
        mediaPlayer.pause()
        listener?.onPlayingChanged(false)
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        TODO("Not yet implemented")
    }

    override fun attachTextureView(textureView: TextureView) {
        TODO("Not yet implemented")
    }


    override fun seekTo(positionMs: Long) {
        mediaPlayer.seekTo(positionMs.toInt())
    }

    override fun clearMediaItem() {

    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {

    }


    override fun release() {
        mediaPlayer.release()
    }


    override val currentPosition: Long
        get() = mediaPlayer.currentPosition.toLong()

    override val duration: Long
        get() = mediaPlayer.duration.toLong().coerceAtLeast(0L)

    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    @Suppress("UNCHECKED_CAST")
    override fun <T> getEngine(): T? = mediaPlayer as T
}