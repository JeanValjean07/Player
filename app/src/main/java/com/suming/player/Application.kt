package com.suming.player

import android.app.Application
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.MediaDataBaseMaster
import com.suming.player.FuncionalPack.SettingsCenter
import com.suming.player.FuncionalPack.SystemListener
import com.suming.player.PlayerImplements.EngineSettings

class Application : Application() {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()


        //1.传统播放体系-播放器单例
        PlayerSingleton.setContext(this)
        //2.列表管理器
        ListManagerHelper.setContext(this)
        //3.系统状态监听器
        SystemListener.setContext(this)
        //4.设置中心
        SettingsCenter.setContext(this)
        //
        MediaDataBaseMaster.setContext(this)

        //测试中的项
        //1.可切换引擎系统设置管理中心
        EngineSettings.setContext(this)


    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "ApplicationGlobal: $msg")
        }
    }

}