package com.suming.player.FuncionalPack

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit

@Suppress("/unused")
object SettingsCenter {

    //日志控制
    @Suppress("unused")
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "SettingsRequestCenter: $msg")
        }
    }


    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {

            this.context = context
        }
    }


    //# Pandora_MainPage 首页 设置项
    //<editor-fold desc="////首页 设置项">
    private var Pandora_MainPage: SharedPreferences? = null
    const val Pandora_MainPage_Name = "Pandora_MainPage"
    private fun OpenPandora_MainPage(){
        if (Pandora_MainPage == null){
            Pandora_MainPage = context.getSharedPreferences(Pandora_MainPage_Name, 0)
        }
    }
    //启用主页MiniView
    private var PRF_EnableMiniView = -1
    const val PRF_EnableMiniView_Name = "PRF_EnableMiniView"
    fun SET_PRF_EnableMiniView(enable: Boolean){
        OpenPandora_MainPage()

        PRF_EnableMiniView = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PRF_EnableMiniView_Name, PRF_EnableMiniView) }
    }
    fun GET_PRF_EnableMiniView(): Boolean {
        OpenPandora_MainPage()

        if (PRF_EnableMiniView == -1) {
            PRF_EnableMiniView = Pandora_MainPage?.getInt(PRF_EnableMiniView_Name, -1) ?: -1
            if (PRF_EnableMiniView == -1) {
                PRF_EnableMiniView = 1
                Pandora_MainPage?.edit { putInt(PRF_EnableMiniView_Name, 1) }
            }
        }

        return PRF_EnableMiniView == 1
    }
    //始终在MiniView中使用图片
    private var PRF_AlwaysUseImageInMiniView = -1
    const val PRF_AlwaysUseImageInMiniView_Name = "PRF_AlwaysUseImageInMiniView"
    fun SET_PRF_AlwaysUseImageInMiniView(enable: Boolean){
        OpenPandora_MainPage()

        PRF_AlwaysUseImageInMiniView = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PRF_AlwaysUseImageInMiniView_Name, PRF_AlwaysUseImageInMiniView) }
    }
    fun GET_PRF_AlwaysUseImageInMiniView(): Boolean {
        OpenPandora_MainPage()

        if (PRF_AlwaysUseImageInMiniView == -1) {
            //从配置单读取
            PRF_AlwaysUseImageInMiniView = Pandora_MainPage?.getInt(PRF_AlwaysUseImageInMiniView_Name, -1) ?: -1
            if (PRF_AlwaysUseImageInMiniView == -1) {
                //写入默认值为关闭
                PRF_AlwaysUseImageInMiniView = 0
                Pandora_MainPage?.edit { putInt(PRF_AlwaysUseImageInMiniView_Name, 0) }
            }
        }

        return PRF_AlwaysUseImageInMiniView == 1
    }
    //每次启动时都重新读取媒体
    private var PREFS_QueryNewMediaOnStart = -1
    const val PREFS_QueryNewMediaOnStart_Name = "PREFS_QueryNewMediaOnStart"
    fun set_PREFS_QueryNewMediaOnStart(enable: Boolean){
        OpenPandora_MainPage()

        PREFS_QueryNewMediaOnStart = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PREFS_QueryNewMediaOnStart_Name, PREFS_QueryNewMediaOnStart) }
    }
    fun get_PREFS_QueryNewMediaOnStart(): Boolean {
        OpenPandora_MainPage()

        if (PREFS_QueryNewMediaOnStart == -1) {
            PREFS_QueryNewMediaOnStart = Pandora_MainPage?.getInt(PREFS_QueryNewMediaOnStart_Name, -1) ?: -1
            if (PREFS_QueryNewMediaOnStart == -1) {
                //写入默认值为关闭
                PREFS_QueryNewMediaOnStart = 0
                Pandora_MainPage?.edit { putInt(PREFS_QueryNewMediaOnStart_Name, 0) }
            }
        }

        return PREFS_QueryNewMediaOnStart == 1
    }
    //每次启动时继续上次的媒体
    private var PREFS_EnableContinuePlay = -1
    const val PREFS_EnableContinuePlay_Name = "PREFS_EnableContinuePlay"
    fun set_PREFS_EnableContinuePlay(enable: Boolean){
        OpenPandora_MainPage()

        PREFS_EnableContinuePlay = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PREFS_EnableContinuePlay_Name, PREFS_EnableContinuePlay) }
    }
    fun get_PREFS_EnableContinuePlay(): Boolean {
        OpenPandora_MainPage()

        if (PREFS_EnableContinuePlay == -1) {
            //从配置单读取
            PREFS_EnableContinuePlay = Pandora_MainPage?.getInt(PREFS_EnableContinuePlay_Name, -1) ?: -1
            if (PREFS_EnableContinuePlay == -1) {
                //默认设为开启
                PREFS_EnableContinuePlay = 1
                Pandora_MainPage?.edit { putInt(PREFS_EnableContinuePlay_Name, 1) }
            }
        }

        return PREFS_EnableContinuePlay == 1
    }
    //继续播放上次的媒体时直接启动播放器
    private var PRF_ContinuePlay_withEngin = -1
    const val PRF_ContinuePlay_withEngin_Name = "PRF_ContinuePlay_withEngin"
    fun SET_PRF_ContinuePlay_withEngin(enable: Boolean){
        OpenPandora_MainPage()

        PRF_ContinuePlay_withEngin = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PRF_ContinuePlay_withEngin_Name, PRF_ContinuePlay_withEngin) }
    }
    fun GET_PRF_ContinuePlay_withEngin(): Boolean {
        OpenPandora_MainPage()

        if (PRF_ContinuePlay_withEngin == -1) {
            //从配置单读取
            PRF_ContinuePlay_withEngin = Pandora_MainPage?.getInt(PRF_ContinuePlay_withEngin_Name, -1) ?: -1
            if (PRF_ContinuePlay_withEngin == -1) {
                //默认设为开启
                PRF_ContinuePlay_withEngin = 1
                Pandora_MainPage?.edit { putInt(PRF_ContinuePlay_withEngin_Name, 1) }
            }
        }

        return PRF_ContinuePlay_withEngin == 1
    }

    //视频的默认播放行为
    const val action_just_in_mini_view = "action_just_in_mini_view"
    const val action_use_whole_play_page = "action_use_whole_play_page"
    private var PRF_DefaultPlayBehavior = ""
    const val PRF_DefaultPlayBehavior_Name = "PRF_DefaultPlayBehavior"
    fun SET_PRF_DefaultPlayBehavior(target: String){
        OpenPandora_MainPage()

        PRF_DefaultPlayBehavior = target
        Pandora_MainPage?.edit { putString(PRF_DefaultPlayBehavior_Name, target) }
    }
    fun GET_PRF_DefaultPlayBehavior(): String {
        OpenPandora_MainPage()

        if (PRF_DefaultPlayBehavior == "") {
            //从配置单读取
            PRF_DefaultPlayBehavior = Pandora_MainPage?.getString(PRF_DefaultPlayBehavior_Name, "") ?: ""
            if (PRF_DefaultPlayBehavior == "") {
                //默认设为使用完整播放页
                PRF_DefaultPlayBehavior = action_use_whole_play_page
                Pandora_MainPage?.edit { putString(PRF_DefaultPlayBehavior_Name,action_use_whole_play_page ) }
            }
        }

        return PRF_DefaultPlayBehavior
    }
    //音频的默认播放行为
    private var PRF_StartFullPage = -1
    const val PRF_StartFullPage_Name = "PRF_StartFullPage_Name"
    fun SET_PRF_StartFullPage(enable: Boolean){
        OpenPandora_MainPage()

        PRF_StartFullPage = if (enable) 1 else 0
        Pandora_MainPage?.edit { putInt(PRF_StartFullPage_Name, PRF_StartFullPage) }
    }
    fun GET_PRF_StartFullPage(): Boolean {
        OpenPandora_MainPage()

        if (PRF_StartFullPage == -1) {
            //从配置单读取
            PRF_StartFullPage = Pandora_MainPage?.getInt(PRF_StartFullPage_Name, -1) ?: -1
            //写入默认值
            if (PRF_StartFullPage == -1) {
                PRF_StartFullPage = 0
                Pandora_MainPage?.edit { putInt(PRF_StartFullPage_Name,0 ) }
            }
        }

        return PRF_StartFullPage == 1
    }


    //默认显示页签
    const val tab_mark_video = "acquiesce_tab_video"
    const val tab_mark_music = "acquiesce_tab_music"
    const val tab_mark_gallery = "acquiesce_tab_gallery"
    const val tab_mark_last = "acquiesce_tab_last"
    const val tab_mark_null = ""
    const val PREFS_AcquiesceTab_Name = "PREFS_AcquiesceTab"
    private var PREFS_AcquiesceTab = tab_mark_null
    fun set_PREFS_AcquiesceTab(target: String){
        OpenPandora_MainPage()

        PREFS_AcquiesceTab = target
        Pandora_MainPage?.edit { putString(PREFS_AcquiesceTab_Name, target) }
    }
    fun get_PREFS_AcquiesceTab(): String {
        OpenPandora_MainPage()

        if (PREFS_AcquiesceTab == tab_mark_null) {
            //从配置单读取
            PREFS_AcquiesceTab = Pandora_MainPage?.getString(PREFS_AcquiesceTab_Name, tab_mark_null) ?: tab_mark_null
            //如果配置单内无该项,写入默认值
            if (PREFS_AcquiesceTab == tab_mark_null) {
                //默认设为跟随上次停留的页签
                PREFS_AcquiesceTab = tab_mark_last
                Pandora_MainPage?.edit { putString(PREFS_AcquiesceTab_Name,tab_mark_last ) }
            }
        }

        return PREFS_AcquiesceTab
    }
    //State 上次停留的页签
    private var State_LastStayTab = tab_mark_null
    const val State_LastStayTab_Name = "State_LastStayTab"
    fun set_State_LastStayTab(target: String){
        OpenPandora_MainPage()

        State_LastStayTab = target
        Pandora_MainPage?.edit { putString(State_LastStayTab_Name, target) }
    }
    fun get_State_LastStayTab(): String {
        OpenPandora_MainPage()

        if (State_LastStayTab == tab_mark_null) {
            //从配置单读取
            State_LastStayTab = Pandora_MainPage?.getString(State_LastStayTab_Name, tab_mark_null) ?: tab_mark_null
            if (State_LastStayTab == tab_mark_null){
                State_LastStayTab = tab_mark_video
            }

        }

        return State_LastStayTab
    }

    //MiniView底部抬高高度
    private var value_MiniView_BottomPadding_Dp = -1
    const val value_MiniView_BottomPadding_Dp_Name = "value_MiniView_BottomPadding_Dp"
    fun set_value_MiniView_BottomPadding_Dp(targetValue:Int){
        OpenPandora_MainPage()

        value_MiniView_BottomPadding_Dp = targetValue
        Pandora_MainPage?.edit { putInt(value_MiniView_BottomPadding_Dp_Name, targetValue) }
    }
    fun get_value_MiniView_BottomPadding_Dp(): Int{
        OpenPandora_MainPage()

        if (value_MiniView_BottomPadding_Dp == -1){
            value_MiniView_BottomPadding_Dp = Pandora_MainPage?.getInt(value_MiniView_BottomPadding_Dp_Name, -1) ?: -1
            if (value_MiniView_BottomPadding_Dp == -1){
                value_MiniView_BottomPadding_Dp = 0
                Pandora_MainPage?.edit { putInt(value_MiniView_BottomPadding_Dp_Name, 0) }
            }
        }


        return value_MiniView_BottomPadding_Dp
    }

    //</editor-fold>


    //# Pandora_MediaStore 媒体库 设置项
    //<editor-fold desc="////媒体库 设置项">
    private var Pandora_MediaStore: SharedPreferences? = null
    const val Pandora_MediaStore_Name = "Pandora_MediaStore"
    private fun OpenPandora_MediaStore(){
        if (Pandora_MediaStore == null){
            Pandora_MediaStore = context.getSharedPreferences(Pandora_MediaStore_Name, 0)
        }
    }
    //读取时检查文件是否有效
    private var PREFS_EnableFileExistCheck = -1
    const val PREFS_EnableFileExistCheck_Name = "PREFS_EnableFileExistCheck"
    fun set_PREFS_EnableFileExistCheck(enable: Boolean){
        OpenPandora_MediaStore()

        PREFS_EnableFileExistCheck = if (enable) 1 else 0

        Pandora_MediaStore!!.edit { putInt(PREFS_EnableFileExistCheck_Name, if (enable) 1 else 0) }

    }
    fun get_PREFS_EnableFileExistCheck(): Boolean{
        OpenPandora_MediaStore()

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
    fun set_PREFS_video_sortMethod(method: String){
        OpenPandora_MediaStore()

        PREFS_video_sortMethod = method
        Pandora_MediaStore!!.edit { putString(sort_method_video, method) }
    }
    fun get_PREFS_video_sortMethod(): String{
        OpenPandora_MediaStore()

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
    fun set_PREFS_audio_sortMethod(method: String){
        OpenPandora_MediaStore()

        PREFS_audio_sortMethod = method
        Pandora_MediaStore!!.edit { putString(sort_method_audio, method) }
    }
    fun get_PREFS_audio_sortMethod(): String{
        OpenPandora_MediaStore()

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
    fun set_PREFS_video_sortOrientation(orientation: String){
        OpenPandora_MediaStore()

        PREFS_video_sortOrientation = orientation
        Pandora_MediaStore!!.edit { putString(sort_orientation_video, orientation) }
    }
    fun get_PREFS_video_sortOrientation(): String{
        OpenPandora_MediaStore()

        PREFS_video_sortOrientation = Pandora_MediaStore!!.getString(sort_orientation_video, sort_orientation_DESC) ?: sort_orientation_DESC

        return PREFS_video_sortOrientation
    }
    //升序和降序-音频
    private var PREFS_audio_sortOrientation = sort_orientation_DESC
    const val sort_orientation_audio = "sort_orientation_audio"
    fun set_PREFS_audio_sortOrientation(orientation: String){
        OpenPandora_MediaStore()

        PREFS_audio_sortOrientation = orientation
        Pandora_MediaStore!!.edit { putString(sort_orientation_audio, orientation) }
    }
    fun get_PREFS_audio_sortOrientation(): String{
        OpenPandora_MediaStore()

        PREFS_audio_sortOrientation = Pandora_MediaStore!!.getString(sort_orientation_audio, sort_orientation_DESC) ?: sort_orientation_DESC

        return PREFS_audio_sortOrientation
    }
    //</editor-fold>


    //# Pandora_Engine 播放引擎 设置项
    //<editor-fold desc="////播放引擎 设置项">
    private var Pandora_Engine: SharedPreferences?= null
    const val Pandora_Engine_Name = "Pandora_Engine"
    private fun open_Pandora_Engine(){
        if (Pandora_Engine == null){
            Pandora_Engine = context.getSharedPreferences(Pandora_Engine_Name, 0)
        }
    }
    //启用媒体会话艺术图
    private var PRF_EnableMediaSessionArtWork = -1
    const val PRF_EnableMediaSessionArtWork_Name = "PRF_EnableMediaSessionArtWork"
    fun SET_PRF_EnableMediaSessionArtWork(enable: Boolean) {
        open_Pandora_Engine()

        //写入缓存并落盘
        PRF_EnableMediaSessionArtWork = if (enable) 1 else 0
        Pandora_Engine?.edit { putInt(PRF_EnableMediaSessionArtWork_Name, PRF_EnableMediaSessionArtWork) }
    }
    fun GET_PRF_EnableMediaSessionArtWork(): Boolean {
        open_Pandora_Engine()

        //触发读取
        if (PRF_EnableMediaSessionArtWork == -1){
            PRF_EnableMediaSessionArtWork = Pandora_Engine?.getInt(PRF_EnableMediaSessionArtWork_Name, -1) ?: -1
            //触发写入默认值
            if (PRF_EnableMediaSessionArtWork == -1){
                PRF_EnableMediaSessionArtWork = 0
                Pandora_Engine?.edit { putInt(PRF_EnableMediaSessionArtWork_Name, PRF_EnableMediaSessionArtWork ) }
            }
        }

        return PRF_EnableMediaSessionArtWork == 1
    }
    //后台播放时关闭视频轨道
    private var PRF_DisableVideoTrack_whenBackground = -1
    const val PRF_DisableVideoTrack_whenBackground_Name = "PRF_DisableVideoTrack_whenBackground"
    fun SET_PREFS_DisableVideoTrack_whenBackground(disable: Boolean) {
        open_Pandora_Engine()

        //写入缓存并落盘
        PRF_DisableVideoTrack_whenBackground = if (disable) 1 else 0
        Pandora_Engine?.edit { putInt(PRF_DisableVideoTrack_whenBackground_Name, PRF_DisableVideoTrack_whenBackground ) }
    }
    fun GET_PREFS_DisableVideoTrack_whenBackground(): Boolean {
        open_Pandora_Engine()

        //触发读取
        if (PRF_DisableVideoTrack_whenBackground == -1){
            PRF_DisableVideoTrack_whenBackground = Pandora_Engine?.getInt(PRF_DisableVideoTrack_whenBackground_Name, -1) ?: -1
            //触发写入默认值
            if (PRF_DisableVideoTrack_whenBackground == -1){
                //默认设为关闭
                PRF_DisableVideoTrack_whenBackground = 0
                Pandora_Engine?.edit { putInt(PRF_DisableVideoTrack_whenBackground_Name, 0) }
            }
        }

        return PRF_DisableVideoTrack_whenBackground == 1
    }
    //设置自动关闭时,仅在剩余部分播放完成后再关闭
    private var PRF_OnlyAutoStop_whenMediaEnd = -1
    const val PRF_OnlyAutoStop_whenMediaEnd_Name = "PRF_OnlyAutoStop_whenMediaEnd"
    fun SET_PRF_OnlyAutoStop_whenMediaEnd(enable: Boolean) {
        open_Pandora_Engine()

        //写入缓存并落盘
        PRF_OnlyAutoStop_whenMediaEnd = if (enable) 1 else 0
        Pandora_Engine?.edit { putInt(PRF_OnlyAutoStop_whenMediaEnd_Name, PRF_OnlyAutoStop_whenMediaEnd) }
    }
    fun GET_PRF_OnlyAutoStop_whenMediaEnd(): Boolean {
        open_Pandora_Engine()

        //触发读取
        if (PRF_OnlyAutoStop_whenMediaEnd == -1){
            PRF_OnlyAutoStop_whenMediaEnd = Pandora_Engine?.getInt(PRF_OnlyAutoStop_whenMediaEnd_Name, -1) ?: -1
            //触发写入默认值
            if (PRF_OnlyAutoStop_whenMediaEnd == -1){
                PRF_OnlyAutoStop_whenMediaEnd = 0
                Pandora_Engine?.edit { putInt(PRF_OnlyAutoStop_whenMediaEnd_Name, 0) }
            }
        }

        return PRF_OnlyAutoStop_whenMediaEnd == 1
    }
    //后台划卡时关闭播放器
    private var PRF_stopEngine_whenTaskRemoved = -1
    const val PRF_stopEngine_whenTaskRemoved_Name = "PRF_stopEngine_whenTaskRemoved"
    fun SET_PRF_stopEngine_whenTaskRemoved(enable: Boolean) {
        open_Pandora_Engine()

        //写入缓存并落盘
        PRF_stopEngine_whenTaskRemoved = if (enable) 1 else 0
        Pandora_Engine?.edit { putInt(PRF_stopEngine_whenTaskRemoved_Name, PRF_stopEngine_whenTaskRemoved) }
    }
    fun GET_PRF_stopEngine_whenTaskRemoved(): Boolean {
        open_Pandora_Engine()

        //触发读取
        if (PRF_stopEngine_whenTaskRemoved == -1){
            PRF_stopEngine_whenTaskRemoved = Pandora_Engine?.getInt(PRF_stopEngine_whenTaskRemoved_Name, -1) ?: -1
            //触发写入默认值
            if (PRF_stopEngine_whenTaskRemoved == -1){
                //默认设为开启
                PRF_stopEngine_whenTaskRemoved = 1
                Pandora_Engine?.edit { putInt(PRF_stopEngine_whenTaskRemoved_Name, 1) }
            }
        }

        return PRF_stopEngine_whenTaskRemoved == 1
    }
    //媒体变更冷却时长
    private var value_onMediaChange_delayMillis = -1L
    const val value_onMediaChange_delayMillis_Name = "value_onMediaChange_delayMillis"
    fun set_value_onMediaChange_delayMillis(target: Long) {
        open_Pandora_Engine()

        //写入缓存并落盘
        value_onMediaChange_delayMillis = target
        Pandora_Engine?.edit { putLong(value_onMediaChange_delayMillis_Name, value_onMediaChange_delayMillis) }
    }
    fun get_value_onMediaChange_delayMillis(): Long {
        open_Pandora_Engine()

        //触发读取
        if (value_onMediaChange_delayMillis == -1L) {
            value_onMediaChange_delayMillis = Pandora_Engine?.getLong(value_onMediaChange_delayMillis_Name, -1L) ?: -1L
            //触发写入默认值
            if (value_onMediaChange_delayMillis == -1L) {
                //默认设为50Ms
                value_onMediaChange_delayMillis = 50L
                Pandora_Engine?.edit { putLong(value_onMediaChange_delayMillis_Name, 50L) }
            }
        }

        return value_onMediaChange_delayMillis
    }
    //</editor-fold>


    //# PREFS in Pandora_PlayVideoPage
    //<editor-fold desc="////视频播放页 设置项">
    private var Pandora_PlayVideoPage: SharedPreferences? = null
    const val Pandora_PlayVideoPage_Name = "Pandora_PlayVideoPage"
    private fun OpenPandora_PlayVideoPage(){
        if (Pandora_PlayVideoPage == null) {
            Pandora_PlayVideoPage = context.getSharedPreferences( Pandora_PlayVideoPage_Name, 0)
        }
    }
    //播放页样式 screening_type_ORO
    const val screening_type_ORO = 0
    const val screening_type_NEO = 1
    const val screening_type_TEST = 2
    private var PRF_Video_Screening_Type = -1
    const val PRF_Video_Screening_Type_Name = "PRF_Video_Screening_Type"
    fun SET_PRF_Video_Screening_Type(targetType: Int){
        OpenPandora_PlayVideoPage()

        PRF_Video_Screening_Type = targetType
        Pandora_PlayVideoPage?.edit { putInt(PRF_Video_Screening_Type_Name, targetType) }
    }
    fun GET_PRF_Video_Screening_Type(): Int{
        OpenPandora_PlayVideoPage()

        //无缓存时读取
        if (PRF_Video_Screening_Type == -1) {
            PRF_Video_Screening_Type = Pandora_PlayVideoPage?.getInt(PRF_Video_Screening_Type_Name, -1) ?: -1
            //未写入时写入默认值
            if (PRF_Video_Screening_Type == -1) {
                PRF_Video_Screening_Type = screening_type_NEO
                Pandora_PlayVideoPage?.edit { putInt(PRF_Video_Screening_Type_Name, screening_type_NEO) }
            }
        }

        return PRF_Video_Screening_Type
    }
    //后台播放
    private var PREFS_BackgroundPlay = -1
    const val PREFS_BackgroundPlay_Name = "PREFS_BackgroundPlay"
    fun SET_PREFS_BackgroundPlay(backgroundPlay: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_BackgroundPlay = if (backgroundPlay) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_BackgroundPlay_Name, if (backgroundPlay) 1 else 0) }
    }
    fun GET_PREFS_BackgroundPlay(): Boolean{
        OpenPandora_PlayVideoPage()

        //无缓存时触发读取
        if (PREFS_BackgroundPlay == -1){
            PREFS_BackgroundPlay = Pandora_PlayVideoPage?.getInt(PREFS_BackgroundPlay_Name, -1) ?: -1
            if (PREFS_BackgroundPlay == -1){
                PREFS_BackgroundPlay = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_BackgroundPlay_Name, 1) }
            }
        }

        return PREFS_BackgroundPlay == 1
    }
    //始终使用深色播放界面
    private var PREFS_AlwaysUseDarkTheme = -1
    const val PREFS_AlwaysUseDarkTheme_Name = "PREFS_AlwaysUseDarkTheme"
    fun SET_PREFS_AlwaysUseDarkTheme(alwaysUseDarkTheme: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_AlwaysUseDarkTheme = if (alwaysUseDarkTheme) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_AlwaysUseDarkTheme_Name , if (alwaysUseDarkTheme) 1 else 0) }
    }
    fun GET_PREFS_AlwaysUseDarkTheme(): Boolean{
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_AlwaysUseDarkTheme == -1){
            PREFS_AlwaysUseDarkTheme = Pandora_PlayVideoPage?.getInt(PREFS_AlwaysUseDarkTheme_Name , -1) ?: -1
            if (PREFS_AlwaysUseDarkTheme == -1){
                PREFS_AlwaysUseDarkTheme = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_AlwaysUseDarkTheme_Name , 0) }
            }
        }

        return PREFS_AlwaysUseDarkTheme == 1
    }
    //AlwaysSeek
    private var PREFS_EnableAlwaysSeek = -1
    const val PREFS_EnableAlwaysSeek_Name = "PREFS_EnableAlwaysSeek"
    fun SET_PREFS_EnableAlwaysSeek(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_EnableAlwaysSeek = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableAlwaysSeek_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_EnableAlwaysSeek(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_EnableAlwaysSeek == -1) {
            PREFS_EnableAlwaysSeek = Pandora_PlayVideoPage?.getInt(PREFS_EnableAlwaysSeek_Name, -1) ?: -1
            if (PREFS_EnableAlwaysSeek == -1) {
                PREFS_EnableAlwaysSeek = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableAlwaysSeek_Name, 1) }
            }
        }

        return PREFS_EnableAlwaysSeek == 1
    }
    //LinkScroll
    private var PREFS_EnableLinkScroll = -1
    const val PREFS_EnableLinkScroll_Name = "PREFS_EnableLinkScroll"
    fun SET_PREFS_EnableLinkScroll(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_EnableLinkScroll = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableLinkScroll_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_EnableLinkScroll(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_EnableLinkScroll == -1) {
            PREFS_EnableLinkScroll = Pandora_PlayVideoPage?.getInt(PREFS_EnableLinkScroll_Name, -1) ?: -1
            if (PREFS_EnableLinkScroll == -1) {
                PREFS_EnableLinkScroll = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableLinkScroll_Name, 1) }
            }
        }

        return PREFS_EnableLinkScroll == 1
    }
    //TapJump
    private var PREFS_EnableTapJump = -1
    const val PREFS_EnableTapJump_Name = "PREFS_EnableTapJump"
    fun SET_PREFS_EnableTapJump(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_EnableTapJump = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableTapJump_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_EnableTapJump(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_EnableTapJump == -1) {
            PREFS_EnableTapJump = Pandora_PlayVideoPage?.getInt(PREFS_EnableTapJump_Name, -1) ?: -1
            if (PREFS_EnableTapJump == -1) {
                PREFS_EnableTapJump = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_EnableTapJump_Name, 1) }
            }
        }

        return PREFS_EnableTapJump == 1
    }
    //锁定刷新率
    private var PREFS_Video_LockRefreshRate = -1
    const val PREFS_Video_LockRefreshRate_Name = "PREFS_Video_LockRefreshRate"
    fun SET_PREFS_LockRefreshRate(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_LockRefreshRate = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_LockRefreshRate_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_LockRefreshRate(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_LockRefreshRate == -1) {
            PREFS_Video_LockRefreshRate = Pandora_PlayVideoPage?.getInt(PREFS_Video_LockRefreshRate_Name, -1) ?: -1
            if (PREFS_Video_LockRefreshRate == -1) {
                if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                    PREFS_Video_LockRefreshRate = 1
                    Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_LockRefreshRate_Name, 1) }
                }else{
                    PREFS_Video_LockRefreshRate = 0
                    Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_LockRefreshRate_Name, 0) }
                }
            }
        }

        return PREFS_Video_LockRefreshRate == 1
    }
    //从其他应用启动时,播放结束自动退出
    private var PREFS_Video_AutoExit_whenFromEntrance = -1
    const val PREFS_Video_AutoExit_whenFromEntrance_Name = "PREFS_Video_AutoExit_whenFromEntrance"
    fun SET_PREFS_AutoExit_whenFromEntrance(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_AutoExit_whenFromEntrance = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_AutoExit_whenFromEntrance_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_AutoExit_whenFromEntrance(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_AutoExit_whenFromEntrance == -1) {
            PREFS_Video_AutoExit_whenFromEntrance = Pandora_PlayVideoPage?.getInt(PREFS_Video_AutoExit_whenFromEntrance_Name, -1) ?: -1
            if (PREFS_Video_AutoExit_whenFromEntrance == -1) {
                PREFS_Video_AutoExit_whenFromEntrance = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_AutoExit_whenFromEntrance_Name, 0) }
            }
        }

        return PREFS_Video_AutoExit_whenFromEntrance == 1
    }
    //开启方向监听器
    private var PREFS_Video_EnableOrientationListener = -1
    const val PREFS_Video_EnableOrientationListener_Name = "PREFS_Video_EnableOrientationListener"
    fun SET_PREFS_EnableOrientationListener(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_EnableOrientationListener = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_EnableOrientationListener_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_EnableOrientationListener(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_EnableOrientationListener == -1) {
            PREFS_Video_EnableOrientationListener = Pandora_PlayVideoPage?.getInt(PREFS_Video_EnableOrientationListener_Name, -1) ?: -1
            if (PREFS_Video_EnableOrientationListener == -1) {
                PREFS_Video_EnableOrientationListener = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_EnableOrientationListener_Name, 0) }
            }
        }

        return PREFS_Video_EnableOrientationListener == 1
    }
    //关闭更多操作面板下滑手势
    private var PREFS_Video_DisableFragmentGesture = -1
    const val PREFS_Video_DisableFragmentGesture_Name = "PREFS_Video_DisableFragmentGesture"
    fun SET_PREFS_DisableFragmentGesture(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_DisableFragmentGesture = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_DisableFragmentGesture_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_DisableFragmentGesture(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_DisableFragmentGesture == -1) {
            PREFS_Video_DisableFragmentGesture = Pandora_PlayVideoPage?.getInt(PREFS_Video_DisableFragmentGesture_Name, -1) ?: -1
            if (PREFS_Video_DisableFragmentGesture == -1) {
                PREFS_Video_DisableFragmentGesture = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_DisableFragmentGesture_Name, 0) }
            }
        }

        return PREFS_Video_DisableFragmentGesture == 1
    }
    //退出时确保是竖屏(默认设置区分设备dpi)
    private var PRF_Video_SwitchPortrait_whenExit = -1
    const val PRF_Video_SwitchPortrait_whenExit_Name = "PRF_Video_SwitchPortrait_whenExit"
    fun SET_PRF_SwitchPortrait_whenExit(enable: Boolean){
        OpenPandora_PlayVideoPage()
        //写入缓存和清单
        PRF_Video_SwitchPortrait_whenExit = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PRF_Video_SwitchPortrait_whenExit_Name, if (enable) 1 else 0) }
    }
    fun GET_PRF_SwitchPortrait_whenExit(): Boolean {
        OpenPandora_PlayVideoPage()

        //仅在无缓存时读取
        if (PRF_Video_SwitchPortrait_whenExit == -1) {
            PRF_Video_SwitchPortrait_whenExit = Pandora_PlayVideoPage?.getInt(PRF_Video_SwitchPortrait_whenExit_Name, -1) ?: -1
            if (PRF_Video_SwitchPortrait_whenExit == -1) {
                //默认不开启
                Pandora_PlayVideoPage?.edit { putInt(PRF_Video_SwitchPortrait_whenExit_Name, 0) }
                PRF_Video_SwitchPortrait_whenExit = 0

            }
        }

        return PRF_Video_SwitchPortrait_whenExit == 1
    }
    //播放区域移动动画
    private var PREFS_Video_EnablePlayAreaMoveAnim = -1
    const val PREFS_Video_EnablePlayAreaMoveAnim_Name = "PREFS_Video_EnablePlayAreaMoveAnim"
    fun SET_PREFS_EnablePlayAreaMoveAnim(enable: Boolean){
        OpenPandora_PlayVideoPage()
        //写入缓存和清单
        PREFS_Video_EnablePlayAreaMoveAnim = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_EnablePlayAreaMoveAnim_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_EnablePlayAreaMoveAnim(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_EnablePlayAreaMoveAnim == -1) {
            PREFS_Video_EnablePlayAreaMoveAnim = Pandora_PlayVideoPage?.getInt(PREFS_Video_EnablePlayAreaMoveAnim_Name, -1) ?: -1
            if (PREFS_Video_EnablePlayAreaMoveAnim == -1) {
                PREFS_Video_EnablePlayAreaMoveAnim = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_EnablePlayAreaMoveAnim_Name, 1) }
            }
        }

        return PREFS_Video_EnablePlayAreaMoveAnim == 1
    }
    //保持界面常亮
    private var PRF_Video_KeepScreenOn = -1
    const val PRF_Video_KeepScreenOn_Name = "PRF_Video_KeepScreenOn"
    fun SET_PRF_KeepScreenOn(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PRF_Video_KeepScreenOn = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PRF_Video_KeepScreenOn_Name, if (enable) 1 else 0) }

    }
    fun GET_PRF_KeepScreenOn(): Boolean{
        OpenPandora_PlayVideoPage()

        if (PRF_Video_KeepScreenOn == -1){
            PRF_Video_KeepScreenOn = Pandora_PlayVideoPage?.getInt(PRF_Video_KeepScreenOn_Name, -1) ?: -1
            if (PRF_Video_KeepScreenOn == -1){
                PRF_Video_KeepScreenOn = 1
                Pandora_PlayVideoPage?.edit { putInt(PRF_Video_KeepScreenOn_Name, 1) }
            }
        }

        return PRF_Video_KeepScreenOn == 1
    }
    //竖屏时也开启自动隐藏控件
    private var PRF_Video_EnableAutoHideController_whenPortrait = -1
    const val PRF_Video_EnableAutoHideController_whenPortrait_Name = "PRF_Video_EnableAutoHideController_whenPortrait"
    fun SET_PRF_EnableAutoHideController_whenPortrait(enable: Boolean) {
        OpenPandora_PlayVideoPage()

        PRF_Video_EnableAutoHideController_whenPortrait = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PRF_Video_EnableAutoHideController_whenPortrait_Name, if (enable) 1 else 0)}


    }
    fun GET_PRF_EnableAutoHideController_whenPortrait(): Boolean {
        OpenPandora_PlayVideoPage()

        if (PRF_Video_EnableAutoHideController_whenPortrait == -1){
            PRF_Video_EnableAutoHideController_whenPortrait = Pandora_PlayVideoPage?.getInt(PRF_Video_EnableAutoHideController_whenPortrait_Name, -1) ?: -1
            //设置默认值为关闭
            if (PRF_Video_EnableAutoHideController_whenPortrait == -1){
                PRF_Video_EnableAutoHideController_whenPortrait = 0
                Pandora_PlayVideoPage?.edit { putInt(PRF_Video_EnableAutoHideController_whenPortrait_Name, 0) }
            }
        }


        return PRF_Video_EnableAutoHideController_whenPortrait == 1
    }

    //进度条截取时使用关键帧
    private var PREFS_Video_UseSyncFrame_inScroller = -1
    const val PREFS_Video_UseSyncFrame_inScroller_Name = "PREFS_Video_UseSyncFrame_inScroller"
    fun SET_PREFS_UseSyncFrame_inScroller(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_UseSyncFrame_inScroller = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseSyncFrame_inScroller_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_UseSyncFrame_inScroller(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_UseSyncFrame_inScroller == -1) {
            PREFS_Video_UseSyncFrame_inScroller = Pandora_PlayVideoPage?.getInt(PREFS_Video_UseSyncFrame_inScroller_Name, -1) ?: -1
            if (PREFS_Video_UseSyncFrame_inScroller == -1) {
                PREFS_Video_UseSyncFrame_inScroller = 1
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseSyncFrame_inScroller_Name, 1) }
            }
        }

        return PREFS_Video_UseSyncFrame_inScroller == 1
    }
    //使用超长进度条
    private var PREFS_Video_UseSuperLongScroller = -1
    const val PREFS_Video_UseSuperLongScroller_Name = "PREFS_Video_UseSuperLongScroller"
    fun SET_PREFS_UseSuperLongScroller(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_UseSuperLongScroller = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseSuperLongScroller_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_UseSuperLongScroller(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_UseSuperLongScroller == -1) {
            PREFS_Video_UseSuperLongScroller = Pandora_PlayVideoPage?.getInt(PREFS_Video_UseSuperLongScroller_Name, -1) ?: -1
            if (PREFS_Video_UseSuperLongScroller == -1) {
                PREFS_Video_UseSuperLongScroller = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseSuperLongScroller_Name, 0) }
            }
        }

        return PREFS_Video_UseSuperLongScroller == 1
    }
    //进度条端点绘制采用兼容模式
    private var PREFS_Video_UseCompatScroller = -1
    const val PREFS_Video_UseCompatScroller_Name = "PREFS_Video_UseCompatScroller"
    fun SET_PREFS_UseCompatScroller(enable: Boolean){
        OpenPandora_PlayVideoPage()

        PREFS_Video_UseCompatScroller = if (enable) 1 else 0
        Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseCompatScroller_Name, if (enable) 1 else 0) }
    }
    fun GET_PREFS_UseCompatScroller(): Boolean {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_UseCompatScroller == -1) {
            PREFS_Video_UseCompatScroller = Pandora_PlayVideoPage?.getInt(PREFS_Video_UseCompatScroller_Name, -1) ?: -1
            if (PREFS_Video_UseCompatScroller == -1) {
                PREFS_Video_UseCompatScroller = 0
                Pandora_PlayVideoPage?.edit { putInt(PREFS_Video_UseCompatScroller_Name, 0) }
            }
        }

        return PREFS_Video_UseCompatScroller == 1
    }
    //寻帧时关键帧偏好
    private var PREFS_Video_SyncFramePrefs = ""
    const val PREFS_Video_SyncFramePrefs_Name = "PREFS_Video_SyncFramePrefs"
    const val SYNC_FRAME_PREFS_AlwaysSync = "SYNC_FRAME_PREFS_AlwaysSync"
    const val SYNC_FRAME_PREFS_AlwaysExact = "SYNC_FRAME_PREFS_AlwaysExact"
    const val SYNC_FRAME_PREFS_Dynamic = "SYNC_FRAME_PREFS_Dynamic"
    fun SET_PREFS_Video_SyncFramePrefs(target: String){
        OpenPandora_PlayVideoPage()

        PREFS_Video_SyncFramePrefs = target
        Pandora_PlayVideoPage?.edit { putString(PREFS_Video_SyncFramePrefs_Name, target) }
    }
    fun GET_PREFS_Video_SyncFramePrefs(): String {
        OpenPandora_PlayVideoPage()

        //确保配置项已被读取过
        if (PREFS_Video_SyncFramePrefs == "") {
            PREFS_Video_SyncFramePrefs = Pandora_PlayVideoPage?.getString(PREFS_Video_SyncFramePrefs_Name, "") ?: ""
            if (PREFS_Video_SyncFramePrefs == "") {
                PREFS_Video_SyncFramePrefs = SYNC_FRAME_PREFS_Dynamic
                Pandora_PlayVideoPage?.edit { putString(PREFS_Video_SyncFramePrefs_Name, SYNC_FRAME_PREFS_Dynamic) }
            }
        }

        return PREFS_Video_SyncFramePrefs
    }

    //连续寻帧间隔(默认值66ms/15Hz)
    //(🔰严重警告:绝不能设为0,否则播放器内部同帧时会纳秒级给出目标帧,虽然寻帧速度确实很快,表面上看不出异常,但可以跑出一秒几千次循环!并导致soc高温,比正常寻到其他帧时要高得多,可能是其内部逻辑特性)
    //(🔰严重警告:最小值需要限制在9ms (对应120 Hz) )
    private var value_video_seekVideo_runnableGapMs = -1L
    const val value_video_seekVideo_runnableGapMs_Name = "value_video_seekVideo_runnableGapMs"
    fun set_value_seekVideo_runnableGapMs(value: Long){
        OpenPandora_PlayVideoPage()

        //刷新缓存并写入本地
        value_video_seekVideo_runnableGapMs = value
        Pandora_PlayVideoPage?.edit { putLong(value_video_seekVideo_runnableGapMs_Name, value) }
    }
    fun get_value_seekVideo_runnableGapMs(): Long {
        OpenPandora_PlayVideoPage()

        //仅在无缓存时读盘
        if (value_video_seekVideo_runnableGapMs == -1L) {
            value_video_seekVideo_runnableGapMs = Pandora_PlayVideoPage?.getLong(value_video_seekVideo_runnableGapMs_Name, -1L) ?: -1L
            //设置默认值(设为66ms/15Hz)
            if (value_video_seekVideo_runnableGapMs == -1L) {
                value_video_seekVideo_runnableGapMs = 66L
                Pandora_PlayVideoPage?.edit { putLong(value_video_seekVideo_runnableGapMs_Name, 66L) }
            }
        }

        return value_video_seekVideo_runnableGapMs
    }
    //时间戳(被动)刷新间隔(默认值66ms/15Hz)
    private var value_video_timeStamp_updateGapMs = -1L
    const val value_video_timeStamp_updateGapMs_Name = "value_video_timeStamp_updateGapMs"
    fun set_value_timeStamp_updateGapMs(gap: Long){
        OpenPandora_PlayVideoPage()

        //刷新缓存并写入本地
        value_video_timeStamp_updateGapMs = gap
        Pandora_PlayVideoPage?.edit { putLong(value_video_timeStamp_updateGapMs_Name, gap) }
    }
    fun get_value_timeStamp_updateGapMs(): Long {
        OpenPandora_PlayVideoPage()

        //仅在无缓存时读盘
        if (value_video_timeStamp_updateGapMs == -1L) {
            value_video_timeStamp_updateGapMs = Pandora_PlayVideoPage?.getLong(value_video_timeStamp_updateGapMs_Name, -1L) ?: -1L
            //设置默认值(设为66ms/15Hz)
            if (value_video_timeStamp_updateGapMs == -1L) {
                value_video_timeStamp_updateGapMs = 66L
                Pandora_PlayVideoPage?.edit { putLong(value_video_timeStamp_updateGapMs_Name, 66L) }
            }
        }

        return value_video_timeStamp_updateGapMs
    }
    //进度条(被动)刷新间隔(默认值66ms/15Hz)
    private var value_video_syncScroller_runnableGapMs = -1L
    const val value_video_syncScroller_runnableGapMs_Name = "value_video_syncScroller_runnableGapMs"
    fun get_value_syncScroller_runnableGapMs():Long{
        OpenPandora_PlayVideoPage()

        //仅在无缓存时读盘
        if (value_video_syncScroller_runnableGapMs == -1L) {
            value_video_syncScroller_runnableGapMs = Pandora_PlayVideoPage?.getLong(value_video_syncScroller_runnableGapMs_Name, -1L) ?: -1L
            //设置默认值(设为66ms/15Hz)
            if (value_video_syncScroller_runnableGapMs == -1L) {
                value_video_syncScroller_runnableGapMs = 33L
                Pandora_PlayVideoPage?.edit { putLong(value_video_syncScroller_runnableGapMs_Name, 33L) }
            }
        }

        return value_video_syncScroller_runnableGapMs
    }
    fun set_value_syncScroller_runnableGapMs(targetValue: Long){
        OpenPandora_PlayVideoPage()

        //检查数值合法性
        if (targetValue !in 0L..3000L) return

        //刷新缓存并写入本地
        value_video_syncScroller_runnableGapMs = targetValue
        Pandora_PlayVideoPage?.edit { putLong(value_video_syncScroller_runnableGapMs_Name, targetValue) }

    }
    //SeekBar(被动)刷新间隔(默认值66ms/15Hz)
    private var value_video_syncSeekbar_runnableGapMs = -1L
    var value_syncSeekbar_runnableGapMs_Adapt = 3001L
    const val value_video_syncSeekbar_runnableGapMs_Name = "value_video_syncSeekbar_runnableGapMs"
    fun get_value_syncSeekbar_runnableGapMs():Long{
        OpenPandora_PlayVideoPage()

        //仅在无缓存时读盘
        if (value_video_syncSeekbar_runnableGapMs == -1L) {
            value_video_syncSeekbar_runnableGapMs = Pandora_PlayVideoPage?.getLong(value_video_syncSeekbar_runnableGapMs_Name, -1L) ?: -1L
            //设置默认值(设为1s)
            if (value_video_syncSeekbar_runnableGapMs == -1L) {
                value_video_syncSeekbar_runnableGapMs = 1000L
                Pandora_PlayVideoPage?.edit { putLong(value_video_syncSeekbar_runnableGapMs_Name, 1000L) }
            }
        }

        return value_video_syncSeekbar_runnableGapMs
    }
    fun set_value_syncSeekbar_runnableGapMs(targetValue: Long){
        OpenPandora_PlayVideoPage()

        //检查数值合法性
        if (targetValue !in 0L..3100L) return

        //刷新缓存并写入本地
        value_video_syncSeekbar_runnableGapMs = targetValue
        Pandora_PlayVideoPage?.edit { putLong(value_video_syncSeekbar_runnableGapMs_Name, targetValue) }

    }
    //</editor-fold>


    //# Pandora_PlayAudioPage 音乐页 设置项
    //<editor-fold desc="////音乐页 设置项">
    private var Pandora_PlayAudioPage: SharedPreferences? = null
    const val Pandora_PlayAudioPage_Name = "Pandora_PlayAudioPage"
    private fun OpenPandora_PlayAudioPage(){
        if (Pandora_PlayAudioPage == null) {
            Pandora_PlayAudioPage = context.getSharedPreferences( Pandora_PlayAudioPage_Name, 0)
        }
    }
    //SeekBar(被动)刷新间隔(默认值66ms/15Hz)
    private var value_audio_syncSeekbar_runnableGapMs = -1L
    var value_audio_syncSeekbar_runnableGapMs_Adapt = 3001L
    const val value_audio_syncSeekbar_runnableGapMs_Name = "value_audio_syncSeekbar_runnableGapMs"
    fun get_value_audio_syncSeekbar_runnableGapMs():Long{
        OpenPandora_PlayAudioPage()

        //仅在无缓存时读盘
        if (value_audio_syncSeekbar_runnableGapMs == -1L) {
            value_audio_syncSeekbar_runnableGapMs = Pandora_PlayAudioPage?.getLong(value_audio_syncSeekbar_runnableGapMs_Name, -1L) ?: -1L
            //设置默认值(设为1s)
            if (value_audio_syncSeekbar_runnableGapMs == -1L) {
                value_audio_syncSeekbar_runnableGapMs = 1000L
                Pandora_PlayAudioPage?.edit { putLong(value_audio_syncSeekbar_runnableGapMs_Name, 1000L) }
            }
        }

        return value_audio_syncSeekbar_runnableGapMs
    }
    fun set_value_audio_syncSeekbar_runnableGapMs(targetValue: Long){
        OpenPandora_PlayAudioPage()

        //检查数值合法性
        if (targetValue !in 0L..3100L) return

        //刷新缓存并写入本地
        value_audio_syncSeekbar_runnableGapMs = targetValue
        Pandora_PlayAudioPage?.edit { putLong(value_audio_syncSeekbar_runnableGapMs_Name, targetValue) }

    }
    //不使用专辑封面
    private var PRF_Audio_DontShowAlbumFrame = -1
    const val PRF_Audio_DontShowAlbumFrame_Name = "PRF_Audio_DontShowAlbumFrame"
    fun GET_PRF_Audio_DontShowAlbumFrame(): Boolean{
        OpenPandora_PlayAudioPage()

        if (PRF_Audio_DontShowAlbumFrame == -1){
            PRF_Audio_DontShowAlbumFrame = Pandora_PlayAudioPage?.getInt(PRF_Audio_DontShowAlbumFrame_Name, -1) ?: -1
            if (PRF_Audio_DontShowAlbumFrame == -1){
                Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_DontShowAlbumFrame_Name, 0) }
            }

        }

        return PRF_Audio_DontShowAlbumFrame == 1
    }
    fun SET_PRF_Audio_DontShowAlbumFrame(enable: Boolean){
        OpenPandora_PlayAudioPage()

        PRF_Audio_DontShowAlbumFrame = if (enable) 1 else 0
        Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_DontShowAlbumFrame_Name, PRF_Audio_DontShowAlbumFrame) }

    }
    //使用文件名作为标题
    private var PRF_Audio_UseFileNameAsTitle = -1
    const val PRF_Audio_UseFileNameAsTitle_Name = "PRF_Audio_UseFileNameAsTitle"
    fun GET_PRF_Audio_UseFileNameAsTitle(): Boolean{
        OpenPandora_PlayAudioPage()

        if (PRF_Audio_UseFileNameAsTitle == -1){
            PRF_Audio_UseFileNameAsTitle = Pandora_PlayAudioPage?.getInt(PRF_Audio_UseFileNameAsTitle_Name, -1) ?: -1
            if (PRF_Audio_UseFileNameAsTitle == -1){
                Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_UseFileNameAsTitle_Name, 0) }
            }

        }

        return PRF_Audio_UseFileNameAsTitle == 1
    }
    fun SET_PRF_Audio_UseFileNameAsTitle(enable: Boolean){
        OpenPandora_PlayAudioPage()

        PRF_Audio_UseFileNameAsTitle = if (enable) 1 else 0
        Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_UseFileNameAsTitle_Name, PRF_Audio_UseFileNameAsTitle) }

    }
    //使用专辑图手势
    private var PRF_Audio_UseArtworkGesture = -1
    const val PRF_Audio_UseArtworkGesture_Name = "PRF_Audio_UseArtworkGesture"
    fun GET_PRF_Audio_UseArtworkGesture(): Boolean{
        OpenPandora_PlayAudioPage()

        if (PRF_Audio_UseArtworkGesture == -1){
            PRF_Audio_UseArtworkGesture = Pandora_PlayAudioPage?.getInt(PRF_Audio_UseArtworkGesture_Name, -1) ?: -1
            if (PRF_Audio_UseArtworkGesture == -1){
                Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_UseArtworkGesture_Name, 0) }
            }

        }

        return PRF_Audio_UseArtworkGesture == 1
    }
    fun SET_PRF_Audio_UseArtworkGesture(enable: Boolean){
        OpenPandora_PlayAudioPage()

        PRF_Audio_UseArtworkGesture = if (enable) 1 else 0
        Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_UseArtworkGesture_Name, PRF_Audio_UseArtworkGesture) }

    }
    //自动缩放专辑封面
    private var PRF_Audio_AutoZoomArtwork = -1
    const val PRF_Audio_AutoZoomArtwork_Name = "PRF_Audio_AutoZoomArtwork"
    fun GET_PRF_Audio_AutoZoomArtwork(): Boolean{
        OpenPandora_PlayAudioPage()

        if (PRF_Audio_AutoZoomArtwork == -1){
            PRF_Audio_AutoZoomArtwork = Pandora_PlayAudioPage?.getInt(PRF_Audio_AutoZoomArtwork_Name, -1) ?: -1
            if (PRF_Audio_AutoZoomArtwork == -1){
                Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_AutoZoomArtwork_Name, 0) }
            }

        }

        return PRF_Audio_AutoZoomArtwork == 1
    }
    fun SET_PRF_Audio_AutoZoomArtwork(enable: Boolean){
        OpenPandora_PlayAudioPage()

        PRF_Audio_AutoZoomArtwork = if (enable) 1 else 0
        Pandora_PlayAudioPage?.edit { putInt(PRF_Audio_AutoZoomArtwork_Name, PRF_Audio_AutoZoomArtwork) }

    }
    //</editor-fold>



    //# Pandora_Other 其他设置
    //<editor-fold desc="////其他设置相关配置">
    private var Pandora_Other: SharedPreferences? = null
    const val Pandora_Other_Name = "Pandora_Other"
    private fun OpenPandora_Other(){
        if (Pandora_Other == null){
            Pandora_Other = context.getSharedPreferences(Pandora_Other_Name, 0)
        }
    }
    //使用全屏面板
    private var PRF_Other_UseFullScreenFragment = -1
    const val PRF_UseFullScreenFragment_Name = "PRF_Other_UseFullScreenFragment"
    fun GET_PRF_UseFullScreenFragment(): Boolean{
        OpenPandora_Other()

        if (PRF_Other_UseFullScreenFragment == -1){
            PRF_Other_UseFullScreenFragment = Pandora_Other?.getInt(PRF_UseFullScreenFragment_Name, -1) ?: -1

            if (PRF_Other_UseFullScreenFragment == -1){
                PRF_Other_UseFullScreenFragment = 0
            }
            Pandora_Other?.edit { putInt(PRF_UseFullScreenFragment_Name, PRF_Other_UseFullScreenFragment) }
        }


        return PRF_Other_UseFullScreenFragment == 1
    }
    fun SET_PRF_UseFullScreenFragment(enable: Boolean) {
        OpenPandora_Other()

        PRF_Other_UseFullScreenFragment = if (enable) 1 else 0
        Pandora_Other?.edit { putInt(PRF_UseFullScreenFragment_Name, PRF_Other_UseFullScreenFragment) }
    }
    //</editor-fold>



}