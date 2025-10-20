package com.suming.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

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



    var onOrientationChanging: Boolean = false



    var controllerHided = false

    var closeVideoTrackJobRunning = false

    var statusBarHeight = 0



    var PREFS_LoopPlay: Boolean = false
    var PREFS_AlwaysSeek: Boolean = false
    var PREFS_BackgroundPlay: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = false
    var PREFS_SealOEL: Boolean = false


    var PREFS_GenerateThumbSYNC : Boolean = false
    var PREFS_ExitWhenEnd : Boolean = false
    var PREFS_UseLongScroller : Boolean = false
    var PREFS_UseLongSeekGap : Boolean = false
    var PREFS_UseBlackBackground : Boolean = false
    var PREFS_UseHighRefreshRate : Boolean = false
    var PREFS_UseCompatScroller : Boolean = false
    var PREFS_CloseVideoTrack : Boolean = false


    var ShouldUseBlackBackground : Boolean = false



    //以下开关不固化
    var PREFS_OnlyAudio: Boolean = false
    var PREFS_OnlyVideo: Boolean = false






    override fun onCleared() {  }
}