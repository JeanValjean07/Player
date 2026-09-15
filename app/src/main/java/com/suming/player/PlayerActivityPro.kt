package com.suming.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
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

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivityTest: $msg")
        }
    }


    //视频视图容器
    private lateinit var PlayerView_Container : FrameLayout

    //连接ExoPlayerImpl的回调通道


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化显示
        init_display()

        val (URI,SOURCE) = ExtractIntent(intent)
        //连接到播放器接口
        IPlayer = PlayerHolder.get_ins_refresh(this)

        //
        IPlayer?.build_player()

        //动态插入视图
        IPlayer?.attachTextureView(this, PlayerView_Container)


        //设置播放状态
        IPlayer?.setPlayWhenReady(true)


        //设置给播放器
        IPlayer?.setMediaItem(URI)




        //
        //startMainBusiness(savedInstanceState)








    }

    //初始化显示
    private fun init_display(){
        //显示配置
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_type_pro)
        //初始化全局view
        fun init_view(){
            PlayerView_Container = findViewById(R.id.playerContainer)
        }
        init_view()



    }

    //
    private fun startMainBusiness(savedInstanceState: Bundle?) {

        val (URI,SOURCE) = ExtractIntent(intent)
        consoleLog( "onCreate: 设置播放 URI $URI, SOURCE $SOURCE")

        if (savedInstanceState == null){
            consoleLog( "savedInstanceState == null, IPlayer:${IPlayer}")


            //动态插入视图
            IPlayer?.attachTextureView(this, PlayerView_Container)


            //设置播放状态
            IPlayer?.setPlayWhenReady(true)


            //设置给播放器
            IPlayer?.setMediaItem(URI)



        }else{
            consoleLog( "savedInstanceState != null")
        }

    }
    //提取 媒体URI 和 启动来源标记 SOURCE
    private fun ExtractIntent(intent: Intent): Pair<Uri, String>{

        val URI = IntentCompat.getParcelableExtra(intent, IntentRepo.URI,  Uri::class.java) ?: Uri.EMPTY
        val SOURCE = intent.getStringExtra(IntentRepo.SOURCE) ?: ""


        return Pair(URI, SOURCE)
    }




}