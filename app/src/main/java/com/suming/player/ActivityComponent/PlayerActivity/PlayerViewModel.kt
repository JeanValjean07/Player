package com.suming.player.ActivityComponent.PlayerActivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Suppress("/unused")
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    //空字段
    var Undefined = ""




    //屏幕旋转
    var FromManualPortrait: Boolean = true
    var OrientationValue = 0
    var Manual: Boolean = false
    var Auto: Boolean = false
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


    //音量控制
    var volumeManager_zeroVolume_noticed: Boolean = false
    //音量变化步长(Dp)
    var volumeManager_changeStep = 100

    //亮度控制
    var brightManager_state_brightness_changed: Boolean = false
    var brightManager_current_brightness: Float = 0f
    //亮度变化步长(Dp)
    var brightManager_changeStep = 100



    var onOrientationChanging: Boolean = false


    //控件隐藏/显示状态
    var state_controllerShowing = true




    //上次是否在播放(进度条控制器专用)
    var wasPlaying: Boolean = false

    //S_AreaType
    var state_s_area_type = S_Area_Helper.S_AreaType_UNDEFINED

    //下滑距离(单位需要转为px)(给个默认值200 Dp)
    var value_scrollDownExitDistance: Int = 1000



    //PRF Cache 设置项缓存
    //是否启用播放区域移动
    var PRF_Cache_EnablePlayAreaMove: Boolean = false
    var PRF_Cache_EnablePlayAreaMove_Distance: Float = 0f
    //寻帧时关键帧偏好
    var PRF_Cache_SyncFrame_Dynamic: Boolean = true
    //是否竖屏时也开启自动隐藏控件
    var PRF_Cache_EnableAutoHideController_whenPortrait: Boolean = false
    //是否禁用播放区域上下滑动手势
    var PRF_Cache_DisableViewFollowing: Boolean = true

    //进度条相关
    var PREFS_AlwaysSeek: Boolean = false
    var PREFS_TapJump: Boolean = false
    var PREFS_LinkScroll: Boolean = true




    //onPause/onStop状态
    var state_isFinishing = true



    override fun onCleared() {

    }
}