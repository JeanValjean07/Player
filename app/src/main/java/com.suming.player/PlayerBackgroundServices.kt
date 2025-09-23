package com.suming.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.delay
import kotlinx.serialization.descriptors.listSerialDescriptor


@UnstableApi
class PlayerBackgroundServices(): MediaSessionService() {
    //MediaSession
    private var mediaSession: MediaSession? = null
    //设置参数
    private var PREFS_S_UseMVVMPlayer = false
    //媒体信息
    private var INFO_TITLE: String? = null


    @OptIn(UnstableApi::class)
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

        //由于执行顺序问题,播控中心逻辑应在onCreate中,自定义通知逻辑应在onStartCommand中
        if (Build.BRAND == "Xiaomi" || Build.BRAND == "samsung"){
            //已确认三星,小米的播控中心不设限制,启用播控中心
            mediaSession = MediaSession.Builder(application, PlayerExoSingleton.getPlayer(application)).build()

            //启用自定义控制通知的代码
            /*
            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
            */
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            release()
            mediaSession = null
        }
    }
    //定制ROM一般执行不到这儿
    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.pause()
        stopSelf()
    }
    //接收Intent额外信息+主要操作
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //取出数据
        intent?.let {
            INFO_TITLE = it.getStringExtra("MEDIA_TITLE")
        }

        //由于执行顺序问题,播控中心逻辑应在onCreate中,自定义通知逻辑应在onStartCommand中
        //区分使用自定义播控通知还是系统播控中心
        if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor"){
            //华为播控中心为白名单,启用自定义控制通知
            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
        }

        //END
        return START_NOT_STICKY
    }

    //自定义通知:构建通知
    private fun BuildCustomizeNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(createPendingIntent())
            .setContentTitle("媒体播放中")
            .setContentText(INFO_TITLE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification_area)
            .addAction(android.R.drawable.ic_media_play, "播放", broadcastPlay())
            .addAction(android.R.drawable.ic_media_pause, "暂停", broadcastPause())
            .setAutoCancel(false)
            .build()
    }

    //自定义通知:创建通知通道
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

    //自定义通知:点击拉起
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
    //自定义通知:播放指令
    @OptIn(UnstableApi::class)
    private fun broadcastPlay(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PLAY"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    //自定义通知:暂停指令
    @OptIn(UnstableApi::class)
    private fun broadcastPause(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PAUSE"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }



}