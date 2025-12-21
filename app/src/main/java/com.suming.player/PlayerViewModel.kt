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
import data.MediaModel.MediaItemForVideo

@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    //播放器数据连接
    private val app = application

    val player: ExoPlayer get() = PlayerSingleton.getPlayer(app)

    val trackSelector: DefaultTrackSelector get() = PlayerSingleton.getTrackSelector(app)

    //Functions
    //设置单链接或媒体项
    fun setMediaUri(videoUri: Uri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
    }
    fun setMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
    }
    //清除现有媒体
    fun clearMediaItem() {
        player.clearMediaItems()
    }
    //关闭和开启视频轨道
    fun close_VideoTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
    }
    fun recovery_VideoTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
    }
    //关闭和开启音频轨道
    fun close_AudioTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
    }
    fun recovery_AudioTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
    }




    var mediaItems: List<MediaItemForVideo> = emptyList()
    var currentMediaIndex = -1
    var maxMediaIndex = 0



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

    //视频时长
    var global_videoDuration = 0L


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

    var PREFS_ExitWhenEnd: Boolean = false



    //设置项
    var PREFS_AlwaysSeek: Boolean = false
    var PREFS_BackgroundPlay: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = true
    var PREFS_SealOEL: Boolean = false
    var PREFS_GenerateThumbSYNC : Boolean = false
    var PREFS_UseLongScroller : Boolean = false
    var PREFS_UseLongSeekGap : Boolean = false
    var PREFS_UseBlackBackground : Boolean = false
    var PREFS_UseHighRefreshRate : Boolean = false
    var PREFS_UseCompatScroller : Boolean = false
    var PREFS_CloseVideoTrack : Boolean = false
    var PREFS_CloseFragmentGesture : Boolean = false
    var PREFS_UseOnlySyncFrame : Boolean = false
    var PREFS_TimeUpdateGap: Long = 20L
    var PREFS_SavePositionWhenExit: Boolean = false
    var PREFS_SwitchPortraitWhenExit: Boolean = true
    var PREFS_EnablePlayAreaMove: Boolean = false
    var PREFS_UseDataBaseForScrollerSetting: Boolean = false
    var PREFS_UseSyncFrameWhenScrollerStop: Boolean = false
    var PREFS_OnlyAudio: Boolean = false
    var PREFS_OnlyVideo: Boolean = false
    var PREFS_SeekHandlerGap: Long = 0
    var PREFS_KeepPlayingWhenExit: Boolean = false


    var state_playerWithSeekBar : Boolean = false
    //进度条停止时使用关键帧

    var seekToLastPositionExecuted: Boolean = false



    var YaxisDestination = 800f


    var MediaInfo_Uri_Saved: Boolean = false
    var MediaInfo_VideoUri: Uri? = null

    var state_firstReadyReached: Boolean = false

    var allowRecord_wasPlaying: Boolean = true

    var wasPlaying: Boolean = false


    //Intent保存
    var originIntent : Intent? = null
    fun saveIntent(intent: Intent){
        if (originIntent == null) {
            originIntent = intent
        }
    }
    //文件名保存
    var MediaInfo_FileName : String = ""
    fun saveFileName(name: String){
        if (MediaInfo_FileName == "") {
            MediaInfo_FileName = name
        }
    }
    //倍速
    var PREFS_PlaySpeed: Float = 1.0f
    fun setSpeed(speed: Float){
        PREFS_PlaySpeed = speed
        player.setPlaybackSpeed(speed)
    }



    var state_PlayListProcess_Complete: Boolean = false




    override fun onCleared() {  }
}