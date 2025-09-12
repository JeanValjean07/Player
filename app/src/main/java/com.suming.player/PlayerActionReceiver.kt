package com.suming.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi

class PlayerActionReceiver:BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            "PLAYER_PLAY"  -> {
                val intent2 = Intent("114514").apply {
                    putExtra("key", "PLAYER_PLAY")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
            "PLAYER_PAUSE" -> {
                val intent2 = Intent("114514").apply {
                    putExtra("key", "PLAYER_PAUSE")
                }
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent2)
            }
        }
    }
}
