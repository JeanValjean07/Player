package com.suming.player.FuncionalPack

import android.util.Log
import com.suming.player.PlayerSingleton

object ActivityCount {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ActivityCount: $msg")
        }
    }

    /*
    //活动显示中计数器
    var activityCount_onForeground: Int = 0
    fun incrementActivityCount_onForeground(){
        activityCount_onForeground++

        consoleLog("activityCount_onForeground: $activityCount_onForeground")
    }
    fun decrementActivityCount_onForeground(){
        activityCount_onForeground--

        consoleLog("activityCount_onForeground: $activityCount_onForeground")
    }


    //活动存活中计数器
    var activityCount_onAlive: Int = 0
    fun incrementActivityCount_onAlive(){
        activityCount_onAlive++

        consoleLog("activityCount_onAlive: $activityCount_onAlive")
    }
    fun decrementActivityCount_onAlive(){
        activityCount_onAlive--

        consoleLog("activityCount_onAlive: $activityCount_onAlive")
    }


    //播放页专用计数器
    //播放页显示中计数器(仅绑定大播放页)(只有大播放页利用此值判断是否暂停播放)
    var activityCount_onForeground_BP: Int = 0
    fun incrementActivityCount_onForeground_BP(){
        activityCount_onForeground_BP++
        consoleLog("activityCount_onForeground_BP: $activityCount_onForeground_BP")

        //若播放页存活
        if (activityCount_onAlive_BP != 0) {

            PlayerSingleton.onPlayerActivityOnStop()

        }
    }
    fun decrementActivityCount_onForeground_BP(){
        activityCount_onForeground_BP--
        consoleLog("activityCount_onForeground_BP: $activityCount_onForeground_BP")
    }
    //播放页存活中计数器(仅绑定大播放页)(只有大播放页利用此值判断是否暂停播放)
    var activityCount_onAlive_BP: Int = 0
    fun incrementActivityCount_onAlive_BP(){
        activityCount_onAlive_BP++
        consoleLog("activityCount_onAlive_BP: $activityCount_onAlive_BP")
    }
    fun decrementActivityCount_onAlive_BP(){
        activityCount_onAlive_BP--
        consoleLog("activityCount_onAlive_BP: $activityCount_onAlive_BP")
    }

     */





}