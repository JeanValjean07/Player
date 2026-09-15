package com.suming.player.PlayerImplements.MediaPlayer

import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
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
    override fun build_player() {
        TODO("Not yet implemented")
    }

    override fun attach_addons() {
        TODO("Not yet implemented")
    }

    override fun attach_more_addons() {
        TODO("Not yet implemented")
    }


    init {
        mediaPlayer.setOnPreparedListener {
            listener?.onPrepared()
        }
        mediaPlayer.setOnCompletionListener {
            listener?.onMediaEnd()
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

    override fun attachSurfaceView(context: Context, view: FrameLayout) {
        TODO("Not yet implemented")
    }

    override fun attachTextureView(context: Context, view: FrameLayout) {
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