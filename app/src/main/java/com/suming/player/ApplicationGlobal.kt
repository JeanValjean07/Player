package com.suming.player

import android.app.Application
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class ApplicationGlobal : Application() {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()



        consoleLog("ApplicationGlobal.onCreate")
        PlayerSingleton.setContext(this)

    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerSingleton: $msg")
        }
    }

}