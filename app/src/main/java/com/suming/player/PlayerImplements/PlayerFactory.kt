package com.suming.player.PlayerImplements

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.PlayerImplements.ExoPlayer.ExoPlayerImpl
import com.suming.player.PlayerImplements.MediaPlayer.MediaPlayerImpl

object PlayerFactory {

    //注意:PlayerFactory需要持有application级别的长生命周期

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerFactory: $msg")
        }
    }


    @OptIn(UnstableApi::class)
    fun create(context: Context, engineType: String): PlayerInterface {
        consoleLog("create: 初始化播放器, engineType: $engineType")

        val appContext = context.applicationContext
        return when (engineType) {
            EngineType.Engine_ExoPlayer -> ExoPlayerImpl(appContext)
            EngineType.Engine_MediaPlayer -> MediaPlayerImpl(appContext)
            else -> ExoPlayerImpl(appContext)
        }
    }


}