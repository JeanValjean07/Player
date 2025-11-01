package com.suming.player

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import data.MediaModel.MediaItem_video

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

    var state_FromSysStart: Boolean = false


    //设置项
    var PREFS_LoopPlay: Boolean = false
    var PREFS_AlwaysSeek: Boolean = true
    var PREFS_BackgroundPlay: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = true
    var PREFS_SealOEL: Boolean = false

    var PREFS_GenerateThumbSYNC : Boolean = false
    var PREFS_ExitWhenEnd : Boolean = false
    var PREFS_UseLongScroller : Boolean = false
    var PREFS_UseLongSeekGap : Boolean = false
    var PREFS_UseBlackBackground : Boolean = false
    var PREFS_UseHighRefreshRate : Boolean = false
    var PREFS_UseCompatScroller : Boolean = false
    var PREFS_CloseVideoTrack : Boolean = false
    var PREFS_EnableRoomDatabase : Boolean = false
    var PREFS_CloseFragmentGesture : Boolean = false
    var PREFS_UseOnlySyncFrame : Boolean = false
    var PREFS_RaiseProgressBarInLandscape : Boolean = false


    var PREFS_VibrateMillis: Long = 10L


    var PREFS_ShutDownWhenMediaEnd: Boolean = false

    var PREFS_TimeUpdateGap: Long = 20L

    var PREFS_SavePositionWhenExit: Boolean = false
    var PREFS_SwitchPortraitWhenExit: Boolean = true


    var state_firstReadyReached: Boolean = false

    var allowRecord_wasPlaying: Boolean = true

    var wasPlaying: Boolean = false

    var Flag_SavedThumbFlag: String = ""

    var String_SavedCoverPath: String = ""




    //以下开关不固化
    var PREFS_OnlyAudio: Boolean = false
    var PREFS_OnlyVideo: Boolean = false

    //Intent保存
    var originIntent : Intent? = null
    fun saveIntent(intent: Intent){
        if (originIntent == null) {
            originIntent = intent
        }
    }
    //文件名保存
    var fileName : String = ""
    fun saveFileName(name: String){
        if (fileName == "") {
            fileName = name
        }
    }
    //倍速
    var PREFS_PlaySpeed: Float = 1.0f
    fun setSpeed(speed: Float){
        PREFS_PlaySpeed = speed
        player.setPlaybackSpeed(speed)
    }
    //定时关闭
    var PREFS_TimerShutDown: Boolean = false
    var shutDownTime = ""


    var PREFS_SeekHandlerGap: Long = 0


    var MediaInfo_VideoItem_Saved: Boolean = false


    lateinit var MediaInfo_VideoItem: MediaItem_video







    override fun onCleared() {  }
}