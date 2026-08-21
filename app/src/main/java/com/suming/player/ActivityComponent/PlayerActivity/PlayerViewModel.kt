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






    var statusBarHeight = 0





    var allowRecord_wasPlaying: Boolean = true

    var wasPlaying: Boolean = false







    //标记播放器类型：传统Oro 或 新晋Neo
    val PAGE_UNDEFINED = ""
    val PAGE_TYPE_ORO = "Oro"
    val PAGE_TYPE_NEO = "Neo"
    var state_player_type = PAGE_UNDEFINED

    //下滑距离(单位需要转为px)(给个默认值200px)
    var value_scrollDownExitDistance: Int = 200



    //PRF Cache 设置项缓存
    //是否启用播放区域移动
    var PRF_Cache_EnablePlayAreaMove: Boolean = false
    var PRF_Cache_EnablePlayAreaMove_Distance: Float = 0f
    //是否寻帧时一律使用关键帧
    var PRF_Cache_UseSyncFrame_whenSeek: Boolean = true
    //是否进度条停止滚动时尾帧使用关键帧
    var PRF_Cache_UseSyncFrame_whenScrollerStop: Boolean = true
    //进度条相关
    var PREFS_AlwaysSeek: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = true




    //onPause/onStop状态
    var state_isFinishing = true



    override fun onCleared() {

    }
}