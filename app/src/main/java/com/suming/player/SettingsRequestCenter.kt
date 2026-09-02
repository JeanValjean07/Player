package com.suming.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.suming.player.FuncionalPack.DeviceInfo
import kotlin.math.sqrt

@Suppress("unused")
object SettingsRequestCenter {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "SettingsRequestCenter: $msg")
        }
    }

    //设置清单
    private lateinit var PREFS_PlayMusicPage: SharedPreferences
    private lateinit var PREFS_DataBase: SharedPreferences
    private lateinit var PREFS_PlayList: SharedPreferences

    //设置清单标记
    private var state_PREFS_PlayMusicPage_initialized = false
    private var state_PREFS_PlayList_initialized = false





    //👝 Pandora_MainPage 首页相关配置 -------------------------------------------------------------
    private var Pandora_MainPage: SharedPreferences? = null
    const val Pandora_MainPage_Name = "Pandora_MainPage"
    private fun OpenPandora_MainPage(context: Context){
        if (Pandora_MainPage == null){
            Pandora_MainPage = context.getSharedPreferences(Pandora_MainPage_Name, 0)
        }
    }
    //启用主页MiniView
    private var PRF_EnableMiniView = -1
    const val PRF_EnableMiniView_Name = "PRF_EnableMiniView"
    fun SET_PRF_EnableMiniView(context: Context, enable: Boolean){
        OpenPandora_MainPage(context)

        PRF_EnableMiniView = if (enable) 1 else 0
        Pandora_MainPage!!.edit { putInt(PRF_EnableMiniView_Name, PRF_EnableMiniView) }
    }
    fun GET_PRF_EnableMiniView(context: Context): Boolean {
        OpenPandora_MainPage(context)

        //确保配置项已被读取过
        if (PRF_EnableMiniView == -1) {
            PRF_EnableMiniView = Pandora_MainPage!!.getInt(PRF_EnableMiniView_Name, -1)
            if (PRF_EnableMiniView == -1) {
                PRF_EnableMiniView = 1
                Pandora_MainPage!!.edit { putInt(PRF_EnableMiniView_Name, 1) }
            }
        }
        return PRF_EnableMiniView == 1
    }
    //始终在MiniView中使用图片
    private var PRF_AlwaysUseImageInMiniView = -1
    const val PRF_AlwaysUseImageInMiniView_Name = "PRF_AlwaysUseImageInMiniView"
    fun SET_PRF_AlwaysUseImageInMiniView(context: Context, enable: Boolean){
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //设置时转为int写入本地缓存
        PRF_AlwaysUseImageInMiniView = if (enable) 1 else 0
        //写入配置单
        Pandora_MainPage!!.edit { putInt(PRF_AlwaysUseImageInMiniView_Name, if (enable) 1 else 0) }
    }
    fun GET_PRF_AlwaysUseImageInMiniView(context: Context): Boolean {
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为-1时)
        if (PRF_AlwaysUseImageInMiniView == -1) {
            //从配置单读取
            PRF_AlwaysUseImageInMiniView = Pandora_MainPage!!.getInt(PRF_AlwaysUseImageInMiniView_Name, -1)
            //如果配置单内无该项,写入默认值
            if (PRF_AlwaysUseImageInMiniView == -1) {
                //默认设为开启
                PRF_AlwaysUseImageInMiniView = 0
                Pandora_MainPage!!.edit { putInt(PRF_AlwaysUseImageInMiniView_Name, 0) }

                /*
                //按机型判断
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_DisableMainPageSmallPlayer = 1
                    Pandora_MainPage!!.edit { putInt(PREFS_DisableMainPageSmallPlayer_Name, 1) }
                }else{
                    PREFS_DisableMainPageSmallPlayer = 0
                    Pandora_MainPage!!.edit { putInt(PREFS_DisableMainPageSmallPlayer_Name, 0) }
                }

                 */
            }
        }
        //返回结果
        return PRF_AlwaysUseImageInMiniView == 1
    }
    //每次启动时都重新读取媒体
    private var PREFS_QueryNewMediaOnStart = -1
    const val PREFS_QueryNewMediaOnStart_Name = "PREFS_QueryNewMediaOnStart"
    fun set_PREFS_QueryNewMediaOnStart(context: Context, enable: Boolean){
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //设置时转为int写入本地缓存
        PREFS_QueryNewMediaOnStart = if (enable) 1 else 0
        //写入配置单
        Pandora_MainPage!!.edit { putInt(PREFS_QueryNewMediaOnStart_Name, if (enable) 1 else 0) }
    }
    fun get_PREFS_QueryNewMediaOnStart(context: Context): Boolean {
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为-1时)
        if (PREFS_QueryNewMediaOnStart == -1) {
            //从配置单读取
            PREFS_QueryNewMediaOnStart = Pandora_MainPage!!.getInt(PREFS_QueryNewMediaOnStart_Name, -1)
            //如果配置单内无该项,写入默认值
            if (PREFS_QueryNewMediaOnStart == -1) {
                //默认设为关闭
                PREFS_QueryNewMediaOnStart = 0
                Pandora_MainPage!!.edit { putInt(PREFS_QueryNewMediaOnStart_Name, 0) }
            }
        }
        //返回结果
        return PREFS_QueryNewMediaOnStart == 1
    }
    //每次启动时继续上次的媒体
    private var PREFS_EnableContinuePlay = -1
    const val PREFS_EnableContinuePlay_Name = "PREFS_EnableContinuePlay"
    fun set_PREFS_EnableContinuePlay(context: Context, enable: Boolean){
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //设置时转为int写入本地缓存
        PREFS_EnableContinuePlay = if (enable) 1 else 0
        //写入配置单
        Pandora_MainPage!!.edit { putInt(PREFS_EnableContinuePlay_Name, if (enable) 1 else 0) }
    }
    fun get_PREFS_EnableContinuePlay(context: Context): Boolean {
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为-1时)
        if (PREFS_EnableContinuePlay == -1) {
            //从配置单读取
            PREFS_EnableContinuePlay = Pandora_MainPage!!.getInt(PREFS_EnableContinuePlay_Name, -1)
            //如果配置单内无该项,写入默认值
            if (PREFS_EnableContinuePlay == -1) {
                //默认设为开启
                PREFS_EnableContinuePlay = 1
                Pandora_MainPage!!.edit { putInt(PREFS_EnableContinuePlay_Name, 1) }
            }
        }
        //返回结果
        return PREFS_EnableContinuePlay == 1
    }
    //继续播放上次的媒体时直接启动播放器
    private var PRF_ContinuePlay_withEngin = -1
    const val PRF_ContinuePlay_withEngin_Name = "PRF_ContinuePlay_withEngin"
    fun SET_PRF_ContinuePlay_withEngin(context: Context, enable: Boolean){
        OpenPandora_MainPage(context)

        //设置时转为int写入本地缓存
        PRF_ContinuePlay_withEngin = if (enable) 1 else 0
        //写入配置单
        Pandora_MainPage!!.edit { putInt(PRF_ContinuePlay_withEngin_Name, if (enable) 1 else 0) }
    }
    fun GET_PRF_ContinuePlay_withEngin(context: Context): Boolean {
        OpenPandora_MainPage(context)

        //仅在未读取过时才读取(也就是值为-1时)
        if (PRF_ContinuePlay_withEngin == -1) {
            //从配置单读取
            PRF_ContinuePlay_withEngin = Pandora_MainPage!!.getInt(PRF_ContinuePlay_withEngin_Name, -1)
            //如果配置单内无该项,写入默认值
            if (PRF_ContinuePlay_withEngin == -1) {
                //默认设为开启
                PRF_ContinuePlay_withEngin = 1
                Pandora_MainPage!!.edit { putInt(PRF_ContinuePlay_withEngin_Name, 1) }
            }
        }
        //返回结果
        return PRF_ContinuePlay_withEngin == 1
    }

    //默认播放行为
    const val action_just_in_mini_view = "action_just_in_mini_view"
    const val action_use_whole_play_page = "action_use_whole_play_page"
    private var PRF_DefaultPlayBehavior = ""
    const val PRF_DefaultPlayBehavior_Name = "PRF_DefaultPlayBehavior"
    fun SET_PRF_DefaultPlayBehavior(context: Context, target: String){
        OpenPandora_MainPage(context)

        //写入本地缓存
        PRF_DefaultPlayBehavior = target
        //写入配置单
        Pandora_MainPage!!.edit { putString(PRF_DefaultPlayBehavior_Name, target) }
    }
    fun GET_PRF_DefaultPlayBehavior(context: Context): String {
        OpenPandora_MainPage(context)

        //仅在未读取过时才读取(也就是值为空时)
        if (PRF_DefaultPlayBehavior == "") {
            //从配置单读取
            PRF_DefaultPlayBehavior = Pandora_MainPage!!.getString(PRF_DefaultPlayBehavior_Name, "") ?: ""
            //如果配置单内无该项,写入默认值
            if (PRF_DefaultPlayBehavior == "") {
                //默认设为使用完整播放页
                PRF_DefaultPlayBehavior = action_use_whole_play_page
                Pandora_MainPage!!.edit { putString(PRF_DefaultPlayBehavior_Name,action_use_whole_play_page ) }
            }
        }

        //返回结果
        return PRF_DefaultPlayBehavior
    }

    //默认显示页签
    const val tab_mark_video = "acquiesce_tab_video"
    const val tab_mark_music = "acquiesce_tab_music"
    const val tab_mark_gallery = "acquiesce_tab_gallery"
    const val tab_mark_last = "acquiesce_tab_last"
    const val tab_mark_null = ""
    const val PREFS_AcquiesceTab_Name = "PREFS_AcquiesceTab"
    private var PREFS_AcquiesceTab = tab_mark_null
    fun set_PREFS_AcquiesceTab(context: Context, target: String){
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        consoleLog("set_PREFS_AcquiesceTab: $target")
        //写入本地缓存
        PREFS_AcquiesceTab = target
        //写入配置单
        Pandora_MainPage!!.edit { putString(PREFS_AcquiesceTab_Name, target) }
    }
    fun get_PREFS_AcquiesceTab(context: Context): String {
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为时)
        if (PREFS_AcquiesceTab == tab_mark_null) {
            //从配置单读取
            PREFS_AcquiesceTab = Pandora_MainPage!!.getString(PREFS_AcquiesceTab_Name, tab_mark_null) ?: tab_mark_null
            //如果配置单内无该项,写入默认值
            if (PREFS_AcquiesceTab == tab_mark_null) {
                //默认设为跟随上次停留的页签
                PREFS_AcquiesceTab = tab_mark_last
                Pandora_MainPage!!.edit { putString(PREFS_AcquiesceTab_Name,tab_mark_last ) }
            }
        }
        //返回结果
        return PREFS_AcquiesceTab
    }
    //State 上次停留的页签
    private var State_LastStayTab = tab_mark_null
    const val State_LastStayTab_Name = "State_LastStayTab"
    fun set_State_LastStayTab(context: Context, target: String){
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //设置时转为int写入本地缓存
        State_LastStayTab = target
        //写入配置单
        Pandora_MainPage!!.edit { putString(State_LastStayTab_Name, target) }
    }
    fun get_State_LastStayTab(context: Context): String {
        //确保配置单已初始化
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为""时)
        if (State_LastStayTab == tab_mark_null) {
            //从配置单读取
            State_LastStayTab = Pandora_MainPage!!.getString(State_LastStayTab_Name, tab_mark_null) ?: tab_mark_null

            //默认保持为空
            //为空时返回保底视频,但不写入
            if (State_LastStayTab == tab_mark_null){
                State_LastStayTab = tab_mark_video
            }

        }
        //返回结果
        return State_LastStayTab
    }

    //测试时长
    const val PRF_forTestDelayMillis_Name = "PRF_forTestDelayMillis"
    private var PRF_forTestDelayMillis = 0L
    fun SET_PRF_forTestDelayMillis(context: Context, target: Long){
        OpenPandora_MainPage(context)
        //写入本地缓存
        PRF_forTestDelayMillis = target
        //写入配置单
        Pandora_MainPage!!.edit { putLong(PRF_forTestDelayMillis_Name, target) }
    }
    fun GET_PRF_forTestDelayMillis(context: Context): Long {
        OpenPandora_MainPage(context)
        //仅在未读取过时才读取(也就是值为0时)
        if (PRF_forTestDelayMillis == 0L) {
            //从配置单读取
            PRF_forTestDelayMillis = Pandora_MainPage!!.getLong(PRF_forTestDelayMillis_Name, 0L)
            //如果配置单内无该项,写入默认值
            if (PRF_forTestDelayMillis == 0L) {
                //默认设为50
                PRF_forTestDelayMillis = 50L
                Pandora_MainPage!!.edit { putLong(PRF_forTestDelayMillis_Name, 50L) }
            }
        }
        //返回结果
        return PRF_forTestDelayMillis
    }




    //👝 Pandora_MediaStore 媒体库相关配置 -------------------------------------------------------------
    private var Pandora_MediaStore: SharedPreferences? = null
    const val Pandora_MediaStore_Name = "Pandora_MediaStore"
    private fun OpenPandora_MediaStore(context: Context){
        if (Pandora_MediaStore == null){
            Pandora_MediaStore = context.getSharedPreferences(Pandora_MediaStore_Name, 0)
        }
    }
    //读取时检查文件是否有效
    private var PREFS_EnableFileExistCheck = -1
    const val PREFS_EnableFileExistCheck_Name = "PREFS_EnableFileExistCheck"
    fun set_PREFS_EnableFileExistCheck(context: Context, enable: Boolean){
        OpenPandora_MediaStore(context)

        PREFS_EnableFileExistCheck = if (enable) 1 else 0

        Pandora_MediaStore!!.edit { putInt(PREFS_EnableFileExistCheck_Name, if (enable) 1 else 0) }

    }
    fun get_PREFS_EnableFileExistCheck(context: Context): Boolean{
        OpenPandora_MediaStore(context)

        if (PREFS_EnableFileExistCheck == -1){
            PREFS_EnableFileExistCheck = Pandora_MediaStore!!.getInt(PREFS_EnableFileExistCheck_Name, -1)
            if (PREFS_EnableFileExistCheck == -1){
                //默认设为关闭
                PREFS_EnableFileExistCheck = 0
                Pandora_MediaStore!!.edit { putInt(PREFS_EnableFileExistCheck_Name, 0) }
            }
        }

        return PREFS_EnableFileExistCheck == 1
    }
    //通用排序方式
    const val sort_method_filename = "sort_method_filename"
    const val sort_method_duration = "sort_method_duration"
    const val sort_method_date_added = "sort_method_date_added"
    const val sort_method_file_size = "sort_method_file_size"
    const val sort_method_mime_type = "sort_method_mime_type"
    const val sort_method_null = "sort_method_null"
    //视频专属排序方式(暂无?)
    //音频专属排序方式(暂无?)
    //视频列表排序方式
    private var PREFS_video_sortMethod = sort_method_null
    const val sort_method_video = "sort_method_video"
    fun set_PREFS_video_sortMethod(context: Context, method: String){
        OpenPandora_MediaStore(context)

        PREFS_video_sortMethod = method
        Pandora_MediaStore!!.edit { putString(sort_method_video, method) }
    }
    fun get_PREFS_video_sortMethod(context: Context): String{
        OpenPandora_MediaStore(context)

        PREFS_video_sortMethod = Pandora_MediaStore!!.getString(sort_method_video, sort_method_null) ?: sort_method_null
        if (PREFS_video_sortMethod == sort_method_null){
            //默认设为添加时间
            PREFS_video_sortMethod = sort_method_date_added
            Pandora_MediaStore!!.edit { putString(sort_method_video, sort_method_date_added) }
        }

        return PREFS_video_sortMethod
    }
    //音乐列表排序方式
    private var PREFS_audio_sortMethod = sort_method_null
    const val sort_method_audio = "sort_method_audio"
    fun set_PREFS_audio_sortMethod(context: Context, method: String){
        OpenPandora_MediaStore(context)

        PREFS_audio_sortMethod = method
        Pandora_MediaStore!!.edit { putString(sort_method_audio, method) }
    }
    fun get_PREFS_audio_sortMethod(context: Context): String{
        OpenPandora_MediaStore(context)

        PREFS_audio_sortMethod = Pandora_MediaStore!!.getString(sort_method_audio, sort_method_null) ?: sort_method_null
        if (PREFS_audio_sortMethod == sort_method_null){
            //默认设为添加时间
            PREFS_audio_sortMethod = sort_method_date_added
            Pandora_MediaStore!!.edit { putString(sort_method_audio, sort_method_date_added) }
        }

        return PREFS_audio_sortMethod
    }
    //升降序
    const val sort_orientation_DESC = "sort_orientation_DESC" //降序
    const val sort_orientation_ASC = "sort_orientation_ASC"   //升序
    //升序和降序-视频
    private var PREFS_video_sortOrientation = sort_orientation_DESC
    const val sort_orientation_video = "sort_orientation_video"
    fun set_PREFS_video_sortOrientation(context: Context, orientation: String){
        PREFS_video_sortOrientation = orientation
        Pandora_MediaStore!!.edit { putString(sort_orientation_video, orientation) }
    }
    fun get_PREFS_video_sortOrientation(context: Context): String{
        return PREFS_video_sortOrientation
    }
    //升序和降序-音频
    private var PREFS_audio_sortOrientation = sort_orientation_DESC
    const val sort_orientation_audio = "sort_orientation_audio"
    fun set_PREFS_audio_sortOrientation(context: Context, orientation: String){
        PREFS_audio_sortOrientation = orientation
        Pandora_MediaStore!!.edit { putString(sort_orientation_audio, orientation) }
    }
    fun get_PREFS_audio_sortOrientation(context: Context): String{
        return PREFS_audio_sortOrientation
    }




    //PREFS in PREFS_PlayEngin -------------------------------------------------------------
    private lateinit var PREFS_PlayEngin: SharedPreferences
    private var state_PREFS_PlayEngin_initialized = false
    const val PREFS_PlayEngin_Name = "PREFS_PlayEngin"
    private fun init_PREFS_PlayEngin(context: Context){
        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences(PREFS_PlayEngin_Name, 0)
            state_PREFS_PlayEngin_initialized = true
        }
    }
    //禁用媒体会话插入预览图
    private var PREFS_DisableMediaArtWork = -1
    const val PREFS_DisableMediaArtWork_Name = "PREFS_DisableMediaArtWork"
    fun SET_PREFS_DisableMediaArtWork(context: Context, disable: Boolean){
        init_PREFS_PlayEngin(context)

        PREFS_DisableMediaArtWork = if (disable) 1 else 0
        PREFS_PlayEngin.edit { putInt(PREFS_DisableMediaArtWork_Name, if (disable) 1 else 0) }
    }
    fun GET_PREFS_DisableMediaArtWork(context: Context): Boolean{
        init_PREFS_PlayEngin(context)

        //仅在无缓存时读取
        if (PREFS_DisableMediaArtWork == -1){
            PREFS_DisableMediaArtWork = PREFS_PlayEngin.getInt(PREFS_DisableMediaArtWork_Name, -1)
            //配置默认值(仅在已过测的设备上关闭禁用预览图,即开启预览图)
            if (PREFS_DisableMediaArtWork == -1){
                val BRAND = DeviceInfo.GET_BRAND()
                val ANDROID_VERSION = DeviceInfo.GET_AndroidVersion()
                when (BRAND){
                    "huawei","honor" ->{
                        when (ANDROID_VERSION){
                            29 -> PREFS_DisableMediaArtWork = 0
                            else -> PREFS_DisableMediaArtWork = 1
                        }
                    }
                    "samsung" -> PREFS_DisableMediaArtWork = 0
                    else -> PREFS_DisableMediaArtWork = 1

                }
                //写入配置项
                PREFS_PlayEngin.edit { putInt(PREFS_DisableMediaArtWork_Name, PREFS_DisableMediaArtWork) }
            }
        }

        return PREFS_DisableMediaArtWork == 1
    }
    //后台播放时关闭视频轨道(默认设置区分安卓版本)
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
    //后台划卡时关闭播放器
    private var PREFS_StopPlayerWhenTaskRemoved = -1
    const val PREFS_StopPlayerWhenTaskRemoved_Name = "PREFS_StopPlayerWhenTaskRemoved"
    fun set_PREFS_StopPlayerWhenTaskRemoved(stopPlayerWhenTaskRemoved: Boolean){
        PREFS_StopPlayerWhenTaskRemoved = if (stopPlayerWhenTaskRemoved) 1 else 0
        PREFS_PlayEngin.edit { putInt(PREFS_StopPlayerWhenTaskRemoved_Name, if (stopPlayerWhenTaskRemoved) 1 else 0) }
    }
    fun get_PREFS_StopPlayerWhenTaskRemoved(context: Context): Boolean{
        //确保配置清单已初始化
        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences("PREFS_PlayEngin", 0)
            state_PREFS_PlayEngin_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_StopPlayerWhenTaskRemoved == -1){
            PREFS_StopPlayerWhenTaskRemoved = PREFS_PlayEngin.getInt(PREFS_StopPlayerWhenTaskRemoved_Name, -1)
            if (PREFS_StopPlayerWhenTaskRemoved == -1){
                PREFS_StopPlayerWhenTaskRemoved = 1
                PREFS_PlayEngin.edit { putInt(PREFS_StopPlayerWhenTaskRemoved_Name, 1) }
            }
        }

        return PREFS_StopPlayerWhenTaskRemoved == 1
    }


    //PREFS in PREFS_PlayVideoPage -------------------------------------------------------------
    private lateinit var PREFS_PlayVideoPage: SharedPreferences
    private var state_PREFS_PlayVideoPage_initialized = false
    const val PREFS_PlayVideoPage_Name = "PREFS_PlayVideoPage"
    private fun initPlayVideoPageSetting(context: Context){
        if (state_PREFS_PlayVideoPage_initialized) return
        PREFS_PlayVideoPage = context.getSharedPreferences( PREFS_PlayVideoPage_Name, 0)
        state_PREFS_PlayVideoPage_initialized = true
    }
    //播放页样式
    const val PlayPageType_Oro = 0
    const val PlayPageType_Neo = 1
    const val PlayPageType_Test = 2
    private var PRF_PlayPageType = -1
    const val PRF_PlayPageType_Name = "PRF_PlayPageType_Name"
    fun SET_PRF_PlayPageType(context: Context, targetType: Int){
        initPlayVideoPageSetting(context)

        PRF_PlayPageType = targetType
        PREFS_PlayVideoPage.edit { putInt(PRF_PlayPageType_Name, targetType) }
    }
    fun GET_PRF_PlayPageType(context: Context): Int{
        initPlayVideoPageSetting(context)

        //无缓存时读取
        if (PRF_PlayPageType == -1) {
            PRF_PlayPageType = PREFS_PlayVideoPage.getInt(PRF_PlayPageType_Name, -1)
            //未写入时写入默认值
            if (PRF_PlayPageType == -1) {
                PRF_PlayPageType = PlayPageType_Neo
                PREFS_PlayVideoPage.edit { putInt(PRF_PlayPageType_Name, PlayPageType_Neo) }
            }
        }

        return PRF_PlayPageType
    }

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
                PREFS_EnableTapJump = 1
                PREFS_PlayVideoPage.edit { putInt("PREFS_EnableTapJump", 1) }
            }
        }

        return PREFS_EnableTapJump == 1
    }
    //锁定刷新率
    private var PREFS_LockRefreshRate = -1
    fun set_PREFS_LockRefreshRate(enable: Boolean){
        PREFS_LockRefreshRate = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt("PREFS_LockRefreshRate", if (enable) 1 else 0) }
    }
    fun get_PREFS_LockRefreshRate(context: Context): Boolean {
        //确保配置清单已初始化
        if (!state_PREFS_PlayVideoPage_initialized) {
            PREFS_PlayVideoPage = context.getSharedPreferences("PREFS_PlayVideoPage", 0)
            state_PREFS_PlayVideoPage_initialized = true
        }
        //确保配置项已被读取过
        if (PREFS_LockRefreshRate == -1) {
            PREFS_LockRefreshRate = PREFS_PlayVideoPage.getInt("PREFS_LockRefreshRate", -1)
            if (PREFS_LockRefreshRate == -1) {
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_LockRefreshRate = 1
                    PREFS_PlayVideoPage.edit { putInt("PREFS_LockRefreshRate", 1) }
                }else{
                    PREFS_LockRefreshRate = 0
                    PREFS_PlayVideoPage.edit { putInt("PREFS_LockRefreshRate", 0) }
                }
            }
        }

        return PREFS_LockRefreshRate == 1
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
    //退出时确保是竖屏(默认设置区分设备dpi)
    private var PRF_SwitchPortrait_whenExit = -1
    const val PRF_SwitchPortrait_whenExit_Name = "PRF_SwitchPortrait_whenExit"
    fun SET_PRF_SwitchPortrait_whenExit(enable: Boolean){
        //写入缓存和清单
        PRF_SwitchPortrait_whenExit = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt(PRF_SwitchPortrait_whenExit_Name, if (enable) 1 else 0) }
    }
    fun GET_PRF_SwitchPortrait_whenExit(context: Context): Boolean {
        initPlayVideoPageSetting(context)

        //仅在无缓存时读取
        if (PRF_SwitchPortrait_whenExit == -1) {
            PRF_SwitchPortrait_whenExit = PREFS_PlayVideoPage.getInt(PRF_SwitchPortrait_whenExit_Name, -1)
            if (PRF_SwitchPortrait_whenExit == -1) {
                //默认不开启
                PREFS_PlayVideoPage.edit { putInt(PRF_SwitchPortrait_whenExit_Name, 0) }
                PRF_SwitchPortrait_whenExit = 0

                //默认值根据是否为平板选择
                /*
                val isDeviceTablet = isDeviceTablet(context)
                if (isDeviceTablet){
                    PREFS_PlayVideoPage.edit { putInt(PRF_SwitchPortrait_whenExit_Name, 0) }
                    PRF_SwitchPortrait_whenExit = 0
                }else{
                    PREFS_PlayVideoPage.edit { putInt(PRF_SwitchPortrait_whenExit_Name, 1) }
                    PRF_SwitchPortrait_whenExit = 1
                }

                 */
            }
        }

        return PRF_SwitchPortrait_whenExit == 1
    }
    private fun isDeviceTablet(context: Context): Boolean{
        val displayMetrics = context.resources.displayMetrics
        val widthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val heightInches = displayMetrics.heightPixels / displayMetrics.ydpi

        //计算屏幕对角线尺寸inch
        val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)

        //默认把7英寸以上算做平板
        return diagonalInches >= 7.0
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
    //保持界面常亮
    private var PRF_KeepScreenOn = -1
    const val PRF_KeepScreenOn_Name = "PRF_KeepScreenOn"
    fun SET_PRF_KeepScreenOn(context: Context, enable: Boolean){
        initPlayVideoPageSetting(context)

        PRF_KeepScreenOn = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt(PRF_KeepScreenOn_Name, if (enable) 1 else 0) }

    }
    fun GET_PRF_KeepScreenOn(context: Context): Boolean{
        initPlayVideoPageSetting(context)

        if (PRF_KeepScreenOn == -1){
            PRF_KeepScreenOn = PREFS_PlayVideoPage.getInt(PRF_KeepScreenOn_Name, -1)
            if (PRF_KeepScreenOn == -1){
                PRF_KeepScreenOn = 1
                PREFS_PlayVideoPage.edit { putInt(PRF_KeepScreenOn_Name, 1) }
            }
        }

        return PRF_KeepScreenOn == 1
    }
    //竖屏时也开启自动隐藏控件
    private var PRF_EnableAutoHideController_whenPortrait = -1
    const val PRF_EnableAutoHideController_whenPortrait_Name = "PRF_EnableAutoHideController_whenPortrait"
    fun SET_PRF_EnableAutoHideController_whenPortrait(context: Context, enable: Boolean) {
        initPlayVideoPageSetting(context)

        PRF_EnableAutoHideController_whenPortrait = if (enable) 1 else 0
        PREFS_PlayVideoPage.edit { putInt(PRF_EnableAutoHideController_whenPortrait_Name, if (enable) 1 else 0)}


    }
    fun GET_PRF_EnableAutoHideController_whenPortrait(context: Context): Boolean {
        initPlayVideoPageSetting(context)

        if (PRF_EnableAutoHideController_whenPortrait == -1){
            PRF_EnableAutoHideController_whenPortrait = PREFS_PlayVideoPage.getInt(PRF_EnableAutoHideController_whenPortrait_Name, -1)
            //设置默认值为关闭
            if (PRF_EnableAutoHideController_whenPortrait == -1){
                PRF_EnableAutoHideController_whenPortrait = 0
                PREFS_PlayVideoPage.edit { putInt(PRF_EnableAutoHideController_whenPortrait_Name, 0) }
            }
        }


        return PRF_EnableAutoHideController_whenPortrait == 1
    }

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

    //连续寻帧间隔(默认值66ms/15Hz)
    private var value_seekVideo_runnableGapMs = -1L
    const val value_seekVideo_runnableGapMs_Name = "value_seekVideo_runnableGapMs"
    fun set_value_seekVideo_runnableGapMs(context: Context, gap: Long){
        initPlayVideoPageSetting(context)

        //刷新缓存并写入本地
        value_seekVideo_runnableGapMs = gap
        PREFS_PlayVideoPage.edit { putLong(value_seekVideo_runnableGapMs_Name, gap) }
    }
    fun get_value_seekVideo_runnableGapMs(context: Context): Long {
        initPlayVideoPageSetting(context)

        //仅在无缓存时读盘
        if (value_seekVideo_runnableGapMs == -1L) {
            value_seekVideo_runnableGapMs = PREFS_PlayVideoPage.getLong(value_seekVideo_runnableGapMs_Name, -1L)
            //设置默认值(设为66ms/15Hz)
            if (value_seekVideo_runnableGapMs == -1L) {
                value_seekVideo_runnableGapMs = 66L
                PREFS_PlayVideoPage.edit { putLong(value_seekVideo_runnableGapMs_Name, 66L) }
            }
        }
        return value_seekVideo_runnableGapMs
    }
    //时间戳(被动)刷新间隔(默认值66ms/15Hz)
    private var value_timeStamp_updateGapMs = -1L
    const val value_timeStamp_updateGapMs_Name = "value_timeStamp_updateGapMs"
    fun set_value_timeStamp_updateGapMs(context: Context, gap: Long){
        initPlayVideoPageSetting(context)

        //刷新缓存并写入本地
        value_timeStamp_updateGapMs = gap
        PREFS_PlayVideoPage.edit { putLong(value_timeStamp_updateGapMs_Name, gap) }
    }
    fun get_value_timeStamp_updateGapMs(context: Context): Long {
        initPlayVideoPageSetting(context)

        //仅在无缓存时读盘
        if (value_timeStamp_updateGapMs == -1L) {
            value_timeStamp_updateGapMs = PREFS_PlayVideoPage.getLong(value_timeStamp_updateGapMs_Name, -1L)
            //设置默认值(设为66ms/15Hz)
            if (value_timeStamp_updateGapMs == -1L) {
                value_timeStamp_updateGapMs = 66L
                PREFS_PlayVideoPage.edit { putLong(value_timeStamp_updateGapMs_Name, 66L) }
            }
        }

        return value_timeStamp_updateGapMs
    }
    //进度条(被动)刷新间隔(默认值66ms/15Hz)
    private var value_syncScroller_runnableGapMs = -1L
    const val value_syncScroller_runnableGapMs_Name = "value_syncScroller_runnableGapMs"
    fun get_value_syncScroller_runnableGapMs(context: Context):Long{
        initPlayVideoPageSetting(context)

        //仅在无缓存时读盘
        if (value_syncScroller_runnableGapMs == -1L) {
            value_syncScroller_runnableGapMs = PREFS_PlayVideoPage.getLong(value_syncScroller_runnableGapMs_Name, -1L)
            //设置默认值(设为66ms/15Hz)
            if (value_syncScroller_runnableGapMs == -1L) {
                value_syncScroller_runnableGapMs = 33L
                PREFS_PlayVideoPage.edit { putLong(value_syncScroller_runnableGapMs_Name, 33L) }
            }
        }

        return value_syncScroller_runnableGapMs
    }
    fun set_value_syncScroller_runnableGapMs(context: Context, targetValue: Long){
        initPlayVideoPageSetting(context)

        //检查数值合法性
        if (targetValue !in 0L..1000L) return

        //刷新缓存并写入本地
        value_syncScroller_runnableGapMs = targetValue
        PREFS_PlayVideoPage.edit { putLong(value_syncScroller_runnableGapMs_Name, targetValue) }

    }
    //SeekBar(被动)刷新间隔(默认值66ms/15Hz)
    private var value_syncSeekbar_runnableGapMs = -1L
    const val value_syncSeekbar_runnableGapMs_Name = "value_syncSeekbar_runnableGapMs"
    fun get_value_syncSeekbar_runnableGapMs(context: Context):Long{
        initPlayVideoPageSetting(context)

        //仅在无缓存时读盘
        if (value_syncSeekbar_runnableGapMs == -1L) {
            value_syncSeekbar_runnableGapMs = PREFS_PlayVideoPage.getLong(value_syncSeekbar_runnableGapMs_Name, -1L)
            //设置默认值(设为1s)
            if (value_syncSeekbar_runnableGapMs == -1L) {
                value_syncSeekbar_runnableGapMs = 1000L
                PREFS_PlayVideoPage.edit { putLong(value_syncSeekbar_runnableGapMs_Name, 1000L) }
            }
        }

        return value_syncSeekbar_runnableGapMs
    }
    fun set_value_syncSeekbar_runnableGapMs(context: Context, targetValue: Long){
        initPlayVideoPageSetting(context)

        //检查数值合法性
        if (targetValue !in 0L..3000L) return

        //刷新缓存并写入本地
        value_syncSeekbar_runnableGapMs = targetValue
        PREFS_PlayVideoPage.edit { putLong(value_syncSeekbar_runnableGapMs_Name, targetValue) }

    }



}