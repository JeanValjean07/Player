package com.suming.player

import android.content.Context
import android.content.SharedPreferences
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


    //项：使用的播放器类型
    private var PREFS_usePlayerType = -1
    fun setPREFS_usePlayerType(playerType: Int){
        PREFS_usePlayerType = playerType
        PREFS_PlayEngin.edit { putInt("PREFS_usePlayerType", playerType) }
    }
    fun getPREFS_usePlayerType(): Int{

        return PREFS_usePlayerType
    }
    private fun Fetch_PREFS_usePlayerType(){
        PREFS_usePlayerType = PREFS_MainPage.getInt("PREFS_usePlayerType", -1)
    }


    private var PREFS_DisableSmallPlayer = -1





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
                PREFS_PlayEngin.edit { putInt("PREFS_DisableMediaArtWork", 0) }
            }
        }

        return PREFS_DisableMediaArtWork == 1
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







}