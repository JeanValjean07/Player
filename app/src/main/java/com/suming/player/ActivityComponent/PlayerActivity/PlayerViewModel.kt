package com.suming.player.ActivityComponent.PlayerActivity

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi

@UnstableApi
//@Suppress("unused")
class PlayerViewModel(application: Application) : AndroidViewModel(application) {


    //屏幕旋转状态
    var FromManualPortrait: Boolean = true
    var OrientationValue = 0


    //手动旋转
    var Manual: Boolean = false

    var Auto: Boolean = false

    //视频播放状态
    var playEnd: Boolean = false


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


    var PREFS_EnablePlayAreaMove: Boolean = false



    var statusBarHeight = 0

    //设置项
    var PREFS_AlwaysSeek: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = true



    //全新设置变量体系(仅保存需高频次访问的变量)
    var PREFS_UseOnlySyncFrameWhenSeek: Boolean = true
    var PREFS_UseSyncFrameWhenScrollerStop = true



    var VALUE_Int_statusBarHeight: Int = 0




    //标记播放器类型：传统Oro 或 新晋Neo
    var state_player_type = ""



    var allowRecord_wasPlaying: Boolean = true

    var wasPlaying: Boolean = false



    override fun onCleared() {

    }
}