package com.suming.player

import android.app.Application
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.FuncPack_ListManager.PlayerListManager
import com.suming.player.FuncionalPack.PlayerListener

class Application : Application() {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        consoleLog("Application.onCreate")

        //启动必要项
        PlayerSingleton.setContext(this)
        PlayerListManager.setContext(this)
        PlayerListener.setContext(this)


    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "ApplicationGlobal: $msg")
        }
    }

}