package com.suming.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession


class PlayerForegroundServices: MediaLibraryService() {
    private var player: ExoPlayer? = null

    private var PREFS_S_UseMVVMPlayer = false

    override fun onCreate() {
        super.onCreate()

        //读取设置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if (!prefs.contains("PREFS_UseMVVMPlayer")){
            prefs.edit { putBoolean("PREFS_UseMVVMPlayer", true) }
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        } else{
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        }






        player = ExoPlayer.Builder(this@PlayerForegroundServices).build()
        val notification = buildNotification()
        createNotificationChannel()
        startForeground(NOTIF_ID, notification)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(createPendingIntent())
            .setContentTitle("媒体播放中")
            .setContentText("媒体播放中")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification_area)
            .addAction(android.R.drawable.ic_media_play, "播放", broadcastPlay())
            .addAction(android.R.drawable.ic_media_pause, "暂停", broadcastPause())
            .setAutoCancel(false)
            .build()
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        TODO("Not yet implemented")
    }

    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "播放控制",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = "后台音频播放"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    @OptIn(UnstableApi::class)
    private fun createPendingIntent(): PendingIntent {
        if (PREFS_S_UseMVVMPlayer){
            val intent = Intent(this, PlayerActivityMVVM::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }.putExtra("SOURCE","FROM_PENDING" )
            return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            val intent = Intent(this, PlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }.putExtra("SOURCE","FROM_PENDING" )
            return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }


    @OptIn(UnstableApi::class)
    private fun broadcastPlay(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PLAY"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @OptIn(UnstableApi::class)
    private fun broadcastPause(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PAUSE"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }



}