package com.suming.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit

@Suppress("unused")
object SettingsRequestCenter {

    //设置清单
    private lateinit var PREFS_PlayEngin: SharedPreferences
    private lateinit var PREFS_PlayVideoPage: SharedPreferences
    private lateinit var PREFS_PlayMusicPage: SharedPreferences
    private lateinit var PREFS_DataBase: SharedPreferences
    private lateinit var PREFS_PlayList: SharedPreferences
    private lateinit var PREFS_MainPage: SharedPreferences
    //设置清单标记
    private var state_PREFS_PlayEngin_initialized = false
    private var state_PREFS_PlayVideoPage_initialized = false
    private var state_PREFS_PlayMusicPage_initialized = false
    private var state_PREFS_DataBase_initialized = false
    private var state_PREFS_PlayList_initialized = false
    private var state_PREFS_MainPage_initialized = false




    //PREFS in PREFS_MainPage
    //禁用主页面小播放器
    private var PREFS_DisableMainPageSmallPlayer = -1
    fun set_PREFS_DisableMainPageSmallPlayer(enable: Boolean){
        PREFS_DisableMainPageSmallPlayer = if (enable) 1 else 0
        PREFS_MainPage.edit { putInt("PREFS_DisableMainPageSmallPlayer", if (enable) 1 else 0) }
    }
    fun get_PREFS_DisableMainPageSmallPlayer(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_MainPage_initialized) {
            PREFS_MainPage = context.getSharedPreferences("PREFS_MainPage", 0)
            state_PREFS_MainPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_DisableMainPageSmallPlayer == -1) {
            PREFS_DisableMainPageSmallPlayer = PREFS_MainPage.getInt("PREFS_DisableMainPageSmallPlayer", -1)
            if (PREFS_DisableMainPageSmallPlayer == -1) {
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_DisableMainPageSmallPlayer = 1
                    PREFS_MainPage.edit { putInt("PREFS_DisableMainPageSmallPlayer", 1) }
                }else{
                    PREFS_DisableMainPageSmallPlayer = 0
                    PREFS_MainPage.edit { putInt("PREFS_DisableMainPageSmallPlayer", 0) }
                }
            }
        }
        return PREFS_DisableMainPageSmallPlayer == 1
    }


    //PREFS in PREFS_PlayEngin
    //禁用媒体会话插入预览图
    private var PREFS_DisableMediaArtWork = -1
    fun set_PREFS_DisableMediaArtWork(disable: Boolean){
        PREFS_DisableMediaArtWork = if (disable) 1 else 0
        PREFS_PlayEngin.edit { putInt("PREFS_DisableMediaArtWork", if (disable) 1 else 0) }
    }
    fun get_PREFS_DisableMediaArtWork(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences("PREFS_PlayEngin", 0)
            state_PREFS_PlayEngin_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_DisableMediaArtWork == -1){
            PREFS_DisableMediaArtWork = PREFS_PlayEngin.getInt("PREFS_DisableMediaArtWork", -1)
            if (PREFS_DisableMediaArtWork == -1){
                PREFS_DisableMediaArtWork = 0
                if (Build.BRAND.equals("huawei",ignoreCase = true)){
                    PREFS_PlayEngin.edit { putInt("PREFS_DisableMediaArtWork", 0) }
                }else{
                    PREFS_PlayEngin.edit { putInt("PREFS_DisableMediaArtWork", 0) }
                }
            }
        }

        return PREFS_DisableMediaArtWork == 1
    }
    //后台播放时关闭视频轨道
    private var PREFS_DisableVideoTrackOnBack = -1
    fun set_PREFS_DisableVideoTrackOnBack(disable: Boolean){
        PREFS_DisableVideoTrackOnBack = if (disable) 1 else 0
        PREFS_PlayEngin.edit { putInt("PREFS_DisableVideoTrackOnBack", if (disable) 1 else 0) }
    }
    fun get_PREFS_DisableVideoTrackOnBack(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences("PREFS_PlayEngin", 0)
            state_PREFS_PlayEngin_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_DisableVideoTrackOnBack == -1){
            PREFS_DisableVideoTrackOnBack = PREFS_PlayEngin.getInt("PREFS_DisableVideoTrackOnBack", -1)
            if (PREFS_DisableVideoTrackOnBack == -1){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    PREFS_DisableVideoTrackOnBack = 1
                    PREFS_PlayEngin.edit { putInt("PREFS_DisableVideoTrackOnBack", 1) }
                }else{
                    PREFS_DisableVideoTrackOnBack = 0
                    PREFS_PlayEngin.edit { putInt("PREFS_DisableVideoTrackOnBack", 0) }
                }
            }
        }

        return PREFS_DisableVideoTrackOnBack == 1
    }
    //仅在播放完成后退出
    private var PREFS_OnlyStopUnMediaEnd = -1
    fun set_PREFS_OnlyStopUnMediaEnd(onlyStopUnMediaEnd: Boolean){
        PREFS_OnlyStopUnMediaEnd = if (onlyStopUnMediaEnd) 1 else 0
        PREFS_PlayEngin.edit { putInt("PREFS_OnlyStopUnMediaEnd", if (onlyStopUnMediaEnd) 1 else 0) }
    }
    fun get_PREFS_OnlyStopUnMediaEnd(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences("PREFS_PlayEngin", 0)
            state_PREFS_PlayEngin_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_OnlyStopUnMediaEnd == -1){
            PREFS_OnlyStopUnMediaEnd = PREFS_PlayEngin.getInt("PREFS_OnlyStopUnMediaEnd", -1)
            if (PREFS_OnlyStopUnMediaEnd == -1){
                PREFS_OnlyStopUnMediaEnd = 0
                PREFS_PlayEngin.edit { putInt("PREFS_OnlyStopUnMediaEnd", 0) }
            }
        }

        return PREFS_OnlyStopUnMediaEnd == 1
    }


    //PREFS in PREFS_PlayVideoPage
    //后台播放
    private var PREFS_BackgroundPlay = -1
    fun set_PREFS_BackgroundPlay(backgroundPlay: Boolean){
        PREFS_BackgroundPlay = if (backgroundPlay) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_BackgroundPlay", if (backgroundPlay) 1 else 0) }
    }
    fun get_PREFS_BackgroundPlay(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized){
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_BackgroundPlay == -1){
            PREFS_BackgroundPlay = PREFS_PlayVideoPage.getInt("PREFS_BackgroundPlay", -1)
            if (PREFS_BackgroundPlay == -1){
                PREFS_BackgroundPlay = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_BackgroundPlay", 1) }
            }
        }

        return PREFS_BackgroundPlay == 1
    }
    //始终使用深色播放界面
    private var PREFS_AlwaysUseDarkTheme = -1
    fun set_PREFS_AlwaysUseDarkTheme(alwaysUseDarkTheme: Boolean){
        PREFS_AlwaysUseDarkTheme = if (alwaysUseDarkTheme) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_AlwaysUseDarkTheme", if (alwaysUseDarkTheme) 1 else 0) }
    }
    fun get_PREFS_AlwaysUseDarkTheme(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized){
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_AlwaysUseDarkTheme == -1){
            PREFS_AlwaysUseDarkTheme = PREFS_PlayVideoPage.getInt("PREFS_AlwaysUseDarkTheme", -1)
            if (PREFS_AlwaysUseDarkTheme == -1){
                PREFS_AlwaysUseDarkTheme = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_AlwaysUseDarkTheme", 0) }
            }
        }

        return PREFS_AlwaysUseDarkTheme == 1
    }
    //AlwaysSeek
    private var PREFS_EnableAlwaysSeek = -1
    fun set_PREFS_EnableAlwaysSeek(enable: Boolean){
        PREFS_EnableAlwaysSeek = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnableAlwaysSeek", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableAlwaysSeek(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnableAlwaysSeek == -1) {
            PREFS_EnableAlwaysSeek = PREFS_PlayVideoPage.getInt("PREFS_EnableAlwaysSeek", -1)
            if (PREFS_EnableAlwaysSeek == -1) {
                PREFS_EnableAlwaysSeek = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnableAlwaysSeek", 1) }
            }
        }

        return PREFS_EnableAlwaysSeek == 1
    }
    //LinkScroll
    private var PREFS_EnableLinkScroll = -1
    fun set_PREFS_EnableLinkScroll(enable: Boolean){
        PREFS_EnableLinkScroll = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnableLinkScroll", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableLinkScroll(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnableLinkScroll == -1) {
            PREFS_EnableLinkScroll = PREFS_PlayVideoPage.getInt("PREFS_EnableLinkScroll", -1)
            if (PREFS_EnableLinkScroll == -1) {
                PREFS_EnableLinkScroll = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnableLinkScroll", 1) }
            }
        }

        return PREFS_EnableLinkScroll == 1
    }
    //TapJump
    private var PREFS_EnableTapJump = -1
    fun set_PREFS_EnableTapJump(enable: Boolean){
        PREFS_EnableTapJump = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnableTapJump", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableTapJump(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnableTapJump == -1) {
            PREFS_EnableTapJump = PREFS_PlayVideoPage.getInt("PREFS_EnableTapJump", -1)
            if (PREFS_EnableTapJump == -1) {
                PREFS_EnableTapJump = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnableTapJump", 0) }
            }
        }

        return PREFS_EnableTapJump == 1
    }
    //使用高刷新率
    private var PREFS_EnableHighRefreshRate = -1
    fun set_PREFS_EnableHighRefreshRate(enable: Boolean){
        PREFS_EnableHighRefreshRate = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnableHighRefreshRate", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableHighRefreshRate(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnableHighRefreshRate == -1) {
            PREFS_EnableHighRefreshRate = PREFS_PlayVideoPage.getInt("PREFS_EnableHighRefreshRate", -1)
            if (PREFS_EnableHighRefreshRate == -1) {
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_EnableHighRefreshRate = 1
                    PREFS_PlayVideoPage.edit { putInt("PREFS_EnableHighRefreshRate", 1) }
                }else{
                    PREFS_EnableHighRefreshRate = 0
                    PREFS_PlayVideoPage.edit { putInt("PREFS_EnableHighRefreshRate", 0) }
                }
            }
        }

        return PREFS_EnableHighRefreshRate == 1
    }
    //从其他应用启动时,播放结束自动退出
    private var PREFS_AutoExitWhenEnd = -1
    fun set_PREFS_AutoExitWhenEnd(enable: Boolean){
        PREFS_AutoExitWhenEnd = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_AutoExitWhenEnd", if (enable) 1 else 0) }
    }
    fun get_PREFS_AutoExitWhenEnd(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_AutoExitWhenEnd == -1) {
            PREFS_AutoExitWhenEnd = PREFS_PlayVideoPage.getInt("PREFS_AutoExitWhenEnd", -1)
            if (PREFS_AutoExitWhenEnd == -1) {
                PREFS_AutoExitWhenEnd = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_AutoExitWhenEnd", 0) }
            }
        }
        return PREFS_AutoExitWhenEnd == 1
    }
    //退出时保持继续播放
    private var PREFS_RetainPlayingWhenFinish = -1
    fun set_PREFS_RetainPlayingWhenFinish(enable: Boolean){
        PREFS_RetainPlayingWhenFinish = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_RetainPlayingWhenFinish", if (enable) 1 else 0) }
    }
    fun get_PREFS_RetainPlayingWhenFinish(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_RetainPlayingWhenFinish == -1) {
            PREFS_RetainPlayingWhenFinish = PREFS_PlayVideoPage.getInt("PREFS_RetainPlayingWhenFinish", -1)
            if (PREFS_RetainPlayingWhenFinish == -1) {
                PREFS_RetainPlayingWhenFinish = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_RetainPlayingWhenFinish", 1) }
            }
        }
        return PREFS_RetainPlayingWhenFinish == 1
    }
    //开启方向监听器
    private var PREFS_EnableOrientationListener = -1
    fun set_PREFS_EnableOrientationListener(enable: Boolean){
        PREFS_EnableOrientationListener = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnableOrientationListener", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableOrientationListener(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnableOrientationListener == -1) {
            PREFS_EnableOrientationListener = PREFS_PlayVideoPage.getInt("PREFS_EnableOrientationListener", -1)
            if (PREFS_EnableOrientationListener == -1) {
                PREFS_EnableOrientationListener = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnableOrientationListener", 0) }
            }
        }

        return PREFS_EnableOrientationListener == 1
    }
    //关闭更多操作面板下滑手势
    private var PREFS_DisableFragmentGesture = -1
    fun set_PREFS_DisableFragmentGesture(enable: Boolean){
        PREFS_DisableFragmentGesture = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_DisableFragmentGesture", if (enable) 1 else 0) }
    }
    fun get_PREFS_DisableFragmentGesture(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_DisableFragmentGesture == -1) {
            PREFS_DisableFragmentGesture = PREFS_PlayVideoPage.getInt("PREFS_DisableFragmentGesture", -1)
            if (PREFS_DisableFragmentGesture == -1) {
                PREFS_DisableFragmentGesture = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_DisableFragmentGesture", 0) }
            }
        }
        return PREFS_DisableFragmentGesture == 1
    }
    //退出时确保是竖屏
    private var PREFS_EnsurePortraitWhenExit = -1
    fun set_PREFS_EnsurePortraitWhenExit(enable: Boolean){
        PREFS_EnsurePortraitWhenExit = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnsurePortraitWhenExit", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnsurePortraitWhenExit(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnsurePortraitWhenExit == -1) {
            PREFS_EnsurePortraitWhenExit = PREFS_PlayVideoPage.getInt("PREFS_EnsurePortraitWhenExit", -1)
            if (PREFS_EnsurePortraitWhenExit == -1) {
                PREFS_EnsurePortraitWhenExit = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnsurePortraitWhenExit", 0) }
            }
        }
        return PREFS_EnsurePortraitWhenExit == 1
    }
    //播放区域移动动画
    private var PREFS_EnablePlayAreaMoveAnim = -1
    fun set_PREFS_EnablePlayAreaMoveAnim(enable: Boolean){
        PREFS_EnablePlayAreaMoveAnim = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_EnablePlayAreaMoveAnim", if (enable) 1 else 0) }
    }
    fun get_PREFS_EnablePlayAreaMoveAnim(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_EnablePlayAreaMoveAnim == -1) {
            PREFS_EnablePlayAreaMoveAnim = PREFS_PlayVideoPage.getInt("PREFS_EnablePlayAreaMoveAnim", -1)
            if (PREFS_EnablePlayAreaMoveAnim == -1) {
                PREFS_EnablePlayAreaMoveAnim = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnablePlayAreaMoveAnim", 1) }
            }
        }
        return PREFS_EnablePlayAreaMoveAnim == 1
    }
    //播放页样式丨0 = 经典, 1 = 新晋
    private var PREFS_PlayPageType = -1
    fun set_PREFS_PlayPageType(playPageType: Int){
        PREFS_PlayPageType = playPageType
        PREFS_PlayVideoPage.edit { putInt("PREFS_PlayPageType", playPageType) }
    }
    fun get_PREFS_PlayPageType(context: Context): Int{
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_PlayPageType == -1) {
            PREFS_PlayPageType = PREFS_PlayVideoPage.getInt("PREFS_PlayPageType", -1)
            if (PREFS_PlayPageType == -1) {
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_PlayPageType = 1
                    PREFS_PlayVideoPage.edit { putInt("PREFS_PlayPageType", 1) }
                }else{
                    PREFS_PlayPageType = 0
                    PREFS_PlayVideoPage.edit { putInt("PREFS_PlayPageType", 0) }
                }
            }
        }
        return PREFS_PlayPageType
    }


    //进度条相关设置
    //进度条截取时使用关键帧
    private var PREFS_UseSyncFrameInScroller = -1
    fun set_PREFS_UseSyncFrameInScroller(enable: Boolean){
        PREFS_UseSyncFrameInScroller = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_UseSyncFrameInScroller", if (enable) 1 else 0) }
    }
    fun get_PREFS_UseSyncFrameInScroller(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_UseSyncFrameInScroller == -1) {
            PREFS_UseSyncFrameInScroller = PREFS_PlayVideoPage.getInt("PREFS_UseSyncFrameInScroller", -1)
            if (PREFS_UseSyncFrameInScroller == -1) {
                PREFS_UseSyncFrameInScroller = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_UseSyncFrameInScroller", 1) }
            }
        }
        return PREFS_UseSyncFrameInScroller == 1
    }
    //进度条停止滚动时使用关键尾帧
    private var PREFS_UseSyncFrameWhenScrollerStop = -1
    fun set_PREFS_UseSyncFrameWhenScrollerStop(enable: Boolean){
        PREFS_UseSyncFrameWhenScrollerStop = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_UseSyncFrameWhenScrollerStop", if (enable) 1 else 0) }
    }
    fun get_PREFS_UseSyncFrameWhenScrollerStop(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_UseSyncFrameWhenScrollerStop == -1) {
            PREFS_UseSyncFrameWhenScrollerStop = PREFS_PlayVideoPage.getInt("PREFS_UseSyncFrameWhenScrollerStop", -1)
            if (PREFS_UseSyncFrameWhenScrollerStop == -1) {
                PREFS_UseSyncFrameWhenScrollerStop = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_UseSyncFrameWhenScrollerStop", 0) }
            }
        }

        return PREFS_UseSyncFrameWhenScrollerStop == 1
    }
    //使用超长进度条
    private var PREFS_UseSuperLongScroller = -1
    fun set_PREFS_UseSuperLongScroller(enable: Boolean){
        PREFS_UseSuperLongScroller = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_UseSuperLongScroller", if (enable) 1 else 0) }
    }
    fun get_PREFS_UseSuperLongScroller(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_UseSuperLongScroller == -1) {
            PREFS_UseSuperLongScroller = PREFS_PlayVideoPage.getInt("PREFS_UseSuperLongScroller", -1)
            if (PREFS_UseSuperLongScroller == -1) {
                PREFS_UseSuperLongScroller = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_UseSuperLongScroller", 0) }
            }
        }
        return PREFS_UseSuperLongScroller == 1
    }
    //进度条端点绘制采用兼容模式
    private var PREFS_UseCompatScroller = -1
    fun set_PREFS_UseCompatScroller(enable: Boolean){
        PREFS_UseCompatScroller = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_UseCompatScroller", if (enable) 1 else 0) }
    }
    fun get_PREFS_UseCompatScroller(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_UseCompatScroller == -1) {
            PREFS_UseCompatScroller = PREFS_PlayVideoPage.getInt("PREFS_UseCompatScroller", -1)
            if (PREFS_UseCompatScroller == -1) {
                PREFS_UseCompatScroller = 0
                PREFS_PlayVideoPage.edit { putInt("PREFS_UseCompatScroller", 0) }
            }
        }
        return PREFS_UseCompatScroller == 1
    }
    //寻帧时一律使用关键帧
    private var PREFS_UseOnlySyncFrameWhenSeek = -1
    fun set_PREFS_UseOnlySyncFrameWhenSeek(enable: Boolean){
        PREFS_UseOnlySyncFrameWhenSeek = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_UseOnlySyncFrameWhenSeek", if (enable) 1 else 0) }
    }
    fun get_PREFS_UseOnlySyncFrameWhenSeek(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_UseSyncFrameInScroller == -1) {
            PREFS_UseOnlySyncFrameWhenSeek = PREFS_PlayVideoPage.getInt("PREFS_UseOnlySyncFrameWhenSeek", -1)
            if (PREFS_UseOnlySyncFrameWhenSeek == -1) {
                PREFS_UseOnlySyncFrameWhenSeek = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_UseOnlySyncFrameWhenSeek", 1) }
            }
        }

        return PREFS_UseOnlySyncFrameWhenSeek == 1
    }

    //数值设置
    //时间戳刷新间隔
    private var VALUE_Gap_TimerUpdate = -1L
    fun set_VALUE_Gap_TimerUpdate(gap: Long){
        VALUE_Gap_TimerUpdate = gap
        PREFS_PlayVideoPage.edit { putLong("VALUE_Gap_TimerUpdate", gap) }
    }
    fun get_VALUE_Gap_TimerUpdate(context: Context): Long {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (VALUE_Gap_TimerUpdate == -1L) {
            VALUE_Gap_TimerUpdate = PREFS_PlayVideoPage.getLong("VALUE_Gap_TimerUpdate", -1L)
            if (VALUE_Gap_TimerUpdate == -1L) {
                VALUE_Gap_TimerUpdate = 33L
                PREFS_PlayVideoPage.edit { putLong("VALUE_Gap_TimerUpdate", 33L) }
            }
        }
        return VALUE_Gap_TimerUpdate
    }
    //连续寻帧间隔
    private var VALUE_Gap_SeekHandlerGap = -1L
    fun set_VALUE_Gap_SeekHandlerGap(gap: Long){
        VALUE_Gap_SeekHandlerGap = gap
        PREFS_PlayVideoPage.edit { putLong("VALUE_Gap_SeekHandlerGap", gap) }
    }
    fun get_VALUE_Gap_SeekHandlerGap(context: Context): Long {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (VALUE_Gap_SeekHandlerGap == -1L) {
            VALUE_Gap_SeekHandlerGap = PREFS_PlayVideoPage.getLong("VALUE_Gap_SeekHandlerGap", -1L)
            if (VALUE_Gap_SeekHandlerGap == -1L) {
                VALUE_Gap_SeekHandlerGap = 0L
                PREFS_PlayVideoPage.edit { putLong("VALUE_Gap_SeekHandlerGap", 0L) }
            }
        }

        return VALUE_Gap_SeekHandlerGap
    }
    //状态栏高度
    private var VALUE_Int_statusBarHeight = -1
    fun set_VALUE_Int_statusBarHeight(height: Int){
        VALUE_Int_statusBarHeight = height
        PREFS_PlayVideoPage.edit { putInt("VALUE_Int_statusBarHeight", height) }
    }
    fun get_VALUE_Int_statusBarHeight(context: Context): Int {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (VALUE_Int_statusBarHeight == -1) {
            VALUE_Int_statusBarHeight = PREFS_PlayVideoPage.getInt("VALUE_Int_statusBarHeight", -1)
            if (VALUE_Int_statusBarHeight == -1) {
                VALUE_Int_statusBarHeight = 200
                //如果读取失败,就返回默认值200,因为无法在单例环境下决策出正确的值
            }
        }

        return VALUE_Int_statusBarHeight
    }
    fun isStatusBarHeightExist(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (VALUE_Int_statusBarHeight == -1) {
            VALUE_Int_statusBarHeight = PREFS_PlayVideoPage.getInt("VALUE_Int_statusBarHeight", -1)

        }

        return VALUE_Int_statusBarHeight != -1
    }




}