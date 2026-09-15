package com.suming.player.PlayerImplements

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.media3.ui.PlayerView

interface PlayerInterface {


    var listener : PlayerCallBack?


    //创建播放器实例
    fun build_player()
    //附加各自工具(监听器等)
    fun attach_addons()
    //附加其他(备用)
    fun attach_more_addons()



    //调用播放器准备 //TODO 了解一下哪些播放器需要这一步
    fun prepare()

    //调用播放器停止/挂起 //TODO 了解一下哪些播放器需要这一步
    fun stop()


    //调用播放器开始播放
    fun play()
    //调用播放器暂停播放
    fun pause()

    //动态附加视图
    fun attachSurfaceView(context: Context, view: FrameLayout)
    fun attachTextureView(context: Context, view: FrameLayout)


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