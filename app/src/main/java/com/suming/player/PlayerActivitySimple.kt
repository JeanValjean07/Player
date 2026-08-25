package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import com.suming.player.FuncionalPack.PlayerInfoCenter

class PlayerActivitySimple : AppCompatActivity() {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivitySimple: $msg")
        }
    }
    //ctx
    private val ctx = this@PlayerActivitySimple
    //播放器
    private var player: ExoPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //显示配置
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_type_simple)


        consoleLog("onCreate")
        mainBusiness(savedInstanceState)


    }


    //播放器回调监听
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    consoleLog("onPlaybackStateChanged: STATE_READY")
                }
                Player.STATE_ENDED -> {
                    consoleLog("onPlaybackStateChanged: STATE_ENDED")
                }
                //播放器进入空闲状态
                Player.STATE_IDLE -> {
                    consoleLog("onPlaybackStateChanged: STATE_IDLE")
                }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //修改可观察标志,触发更新
            PlayerInfoCenter.updateObservableIsPlaying(isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            consoleLog("onMediaItemTransition: $mediaItem, $reason")
        }
        override fun onTracksChanged(tracks: Tracks) {
            for (trackGroup in tracks.groups) {
                val format = trackGroup.getTrackFormat(0)
                val fps = format.frameRate
                PlayerInfoCenter.SET_Media_ActualFPS(fps)
                break
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            consoleLog("播放器错误:${error} message:${error.message} cause:${error.cause} errorCodeName:${error.errorCodeName}")


        }
    }

    //主业务线
    @OptIn(UnstableApi::class)
    private fun mainBusiness(savedInstanceState: Bundle?){

        //获取原始链接并转换为标准格式链接
        val intentUri = getOriginalIntentUri(intent)
        consoleLog("intentUri: $intentUri")


        //决策程序
        if (intentUri != Uri.EMPTY){
            if (player == null){
                player = ExoPlayer.Builder(ctx)
                    .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .setMaxSeekToPreviousPositionMs(1_000_000L)

                    .build()
                    .apply {
                        prepare()
                        playWhenReady = false
                    }

                player?.removeListener(PlayerStateListener)
                player?.addListener(PlayerStateListener)

            }

            try {
                //开始构建mediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(intentUri)
                    .build()

                //设置给播放器
                player?.setMediaItem(mediaItem)
            }catch (e: Exception){
                consoleLog("setMediaItem failed: ${e.message}")
            }


            player?.play()

            val playerView = findViewById<PlayerView>(R.id.playerView)
            playerView.player = player

        }



    }

    //提取原始链接
    private fun getOriginalIntentUri(intent: Intent): Uri {
        //获取原始链接
        val intentUri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)?: Uri.EMPTY
        consoleLog("intentUri: $intentUri")

        return if (intentUri == Uri.EMPTY){
            Uri.EMPTY
        }else{
            intentUri
        }
    }






}