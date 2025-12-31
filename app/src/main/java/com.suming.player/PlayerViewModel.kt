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

    //媒体信息
    var MediaInfo_MediaType = ""
    var MediaInfo_MediaTitle = ""
    var MediaInfo_MediaArtist = ""
    var MediaInfo_MediaDuration = 0L
    var MediaInfo_FileName = ""
    var MediaInfo_AbsolutePath = ""
    var MediaInfo_MediaUri = Uri.EMPTY!!
    var MediaInfo_MediaUriString = ""
    fun saveInfoToViewModel(type: String,title: String,artist: String,duration: Long,filename: String, path: String,uri: Uri, uriString: String){
        MediaInfo_MediaType = type
        MediaInfo_MediaTitle = title
        MediaInfo_MediaArtist = artist
        MediaInfo_MediaDuration = duration
        MediaInfo_FileName = filename
        MediaInfo_AbsolutePath = path
        MediaInfo_MediaUri = uri
        MediaInfo_MediaUriString = uriString
    }


    //播放器监听器绑定状态




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


    //控件隐藏/显示状态
    var state_controllerShowing = true

    //退出状态判定
    var state_onStopDecider_Running = false
    var state_onStop_ByReBuild = false
    var state_onStop_ByLossFocus = false
    var state_onStop_ByRealExit = true
    fun set_onStop_ByReBuild(){
        state_onStop_ByReBuild = true
        state_onStop_ByLossFocus = false
        state_onStop_ByRealExit = false
    }
    fun set_onStop_ByLossFocus(){
        state_onStop_ByLossFocus = true
        state_onStop_ByReBuild = false
        state_onStop_ByRealExit = false
    }
    fun set_onStop_ByRealExit(){
        state_onStop_ByRealExit = true
        state_onStop_ByReBuild = false
        state_onStop_ByLossFocus = false
    }
    fun set_onStop_all_reset(){
        state_onStop_ByReBuild = false
        state_onStop_ByLossFocus = false
        state_onStop_ByRealExit = false
    }



    var statusBarHeight = 0

    var state_FromSysStart: Boolean = false

    var PREFS_ExitWhenEnd: Boolean = false



    //设置项
    var PREFS_AlwaysSeek: Boolean = false
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
    var PREFS_SeekHandlerGap: Long = 0
    var PREFS_KeepPlayingWhenExit: Boolean = false


    //标记播放器类型：传统Oro 或 新晋Neo
    var state_player_type = ""


    var YaxisDestination = 800f



    var allowRecord_wasPlaying: Boolean = true

    var wasPlaying: Boolean = false



    override fun onCleared() {

    }
}