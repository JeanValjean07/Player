package com.suming.player

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class PlayerExoViewModel(application: Application) : AndroidViewModel(application) {

    //播放器数据连接
    private val app = application

    val player: ExoPlayer
        get() = PlayerExoSingleton.getPlayer(app)

    val trackSelector: DefaultTrackSelector
        get() = PlayerExoSingleton.getTrackSelector(app)

    fun setVideoUri(videoUri: Uri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
    }

    fun selectAudioOnly() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
    }

    fun recoveryAllTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
    }


    //屏幕旋转状态
    var FromManualPortrait: Boolean = true
    var OrientationValue = 0


    //手动旋转
    var Manual: Boolean = false

    var Auto: Boolean = false

    //视频播放状态
    var playEnd: Boolean = false


    //浮窗相关
    var inFloatingWindow = false


    //屏幕旋转相关
    var currentOrientation: Int = 0
    var LastLandscapeOrientation: Int = 0
    fun setManual() {
        Manual = true
        Auto = false
    }
    fun setAuto() {
        Auto = true
        Manual = false
    }


    //音量相关
    var NOTICED_VolumeIsZero: Boolean = false
    var NOTICED_HeadSetInsert: Boolean = false
    //亮度相关
    var BrightnessChanged: Boolean = false
    var BrightnessValue: Float = 0f





    override fun onCleared() {}
}