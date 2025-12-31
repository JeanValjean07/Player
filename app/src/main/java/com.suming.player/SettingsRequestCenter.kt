package com.suming.player

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
    private var state_PREFS_PlayEngin_load = false
    private var state_PREFS_DataBase_load = false
    private var state_PREFS_PlayList_load = false
    private var state_PREFS_MainPage_load = false


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





}