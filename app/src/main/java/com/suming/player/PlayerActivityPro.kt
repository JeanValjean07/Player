package com.suming.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.suming.player.FuncionalPack.IntentRepo
import com.suming.player.PlayerImplements.PlayerHolder
import com.suming.player.PlayerImplements.PlayerInterface

@UnstableApi
class PlayerActivityPro : AppCompatActivity() {

    //获取播放器引用
    private var IPlayer: PlayerInterface? = null

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivityTest: $msg")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //显示配置
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_multi_support)

        //
        consoleLog("onCreate")
        //初始化播放器
        consoleLog("onCreate: 初始化播放器")
        IPlayer = PlayerHolder.get_ins_refresh(this)

        IPlayer?.prepare()

        consoleLog("onCreate: 初始化播放器完成IPlayer :$IPlayer")

        val URI = IntentCompat.getParcelableExtra(intent, IntentRepo.URI,  Uri::class.java) ?: Uri.EMPTY



        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)





        consoleLog("onCreate: 设置播放 URI $URI")



        //设置播放状态
        IPlayer?.setPlayWhenReady(true)


        //设置给播放器
        IPlayer?.setMediaItem(URI)
        consoleLog("onCreate: 设置给播放器")

        //设置播放状态
        IPlayer?.play()










    }





}