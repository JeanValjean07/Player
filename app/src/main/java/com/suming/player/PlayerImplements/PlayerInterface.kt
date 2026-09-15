package com.suming.player.PlayerImplements

import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.media3.ui.PlayerView

interface PlayerInterface {


    var listener : PlayerCallBack?


    fun prepare()


    fun stop()


    fun play()


    fun pause()


    fun attachSurfaceView(surfaceView: SurfaceView)
    fun attachTextureView(textureView: TextureView)


    fun seekTo(positionMs: Long)


    fun clearMediaItem()

    fun setPlayWhenReady(playWhenReady: Boolean)


    fun setMediaItem(URI: Uri)


    fun release()


    val currentPosition: Long
    val duration: Long
    val isPlaying: Boolean


    fun <T> getEngine(): T?


}