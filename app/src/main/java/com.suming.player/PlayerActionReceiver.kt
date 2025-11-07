package com.suming.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi

class PlayerActionReceiver:BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            "PLAYER_PLAY"  -> {
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_PLAY")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_PAUSE" -> {
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_PAUSE")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_EXIT" -> {
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_EXIT")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_NextMedia" -> {
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_NextMedia")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_PreviousMedia" -> {
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_PreviousMedia")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_PlayOrPause" -> {
                Log.d("SuMing", " receiver PLAYER_PlayOrPause")
                val intent2 = Intent("LOCAL_RECEIVER").apply {
                    putExtra("key", "PLAYER_PlayOrPause")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
        }
    }
}
