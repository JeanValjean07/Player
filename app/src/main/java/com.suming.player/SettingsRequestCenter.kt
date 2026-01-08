package com.suming.player

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


@Suppress("unused")
object SettingsRequestCenter {

    //设置清单
    private lateinit var PREFS_PlayEngin: SharedPreferences
    private lateinit var PREFS_DataBase: SharedPreferences
    private lateinit var PREFS_PlayList: SharedPreferences
    private lateinit var PREFS_MainPage: SharedPreferences
    //设置清单标记
    private var state_PREFS_PlayEngin_initialized = false
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

        if (!state_PREFS_PlayEngin_initialized){
            PREFS_PlayEngin = context.getSharedPreferences("PREFS_PlayEngin", 0)
            state_PREFS_PlayEngin_initialized = true
        }

        if (PREFS_DisableMediaArtWork == -1){
            PREFS_DisableMediaArtWork = PREFS_PlayEngin.getInt("PREFS_DisableMediaArtWork", -1)
            if (PREFS_DisableMediaArtWork == -1){
                PREFS_DisableMediaArtWork = 0
                PREFS_PlayEngin.edit { putInt("PREFS_DisableMediaArtWork", 0) }
            }
        }

        return PREFS_DisableMediaArtWork == 1
    }







}