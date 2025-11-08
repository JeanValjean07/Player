package com.suming.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri

@UnstableApi
class PlayerService(): MediaSessionService() {
    //MediaSession
    private var mediaSession: MediaSession? = null
    //媒体信息
    private var INFO_TITLE: String? = null

    private var EnsureMediaSession = false



    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()


        //由于执行顺序问题,播控中心逻辑应在onCreate中,自定义通知逻辑应在onStartCommand中
        if (Build.BRAND == "Xiaomi" || Build.BRAND == "samsung"){
            //确认已启用播控中心
            EnsureMediaSession = true
            //已确认三星,小米的播控中心不设限制,启用播控中心
            mediaSession = MediaSession.Builder(application, PlayerSingleton.getPlayer(application)).build()
            mediaSession?.setSessionActivity(createPendingIntent())


            //不论需不需要播控中心都要发通知,这是安卓要求,只是有播控中心时自动隐藏通知
            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.pause()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopForeground(true)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID)

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


        //华为播控中心为白名单,启用自定义控制通知
        if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor"){

            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
        }
        //其他机型,默认启用自定义通知
        else if (!EnsureMediaSession){
            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
        }


        //END
        return START_NOT_STICKY
    }


    //自定义通知:构建通知
    private fun BuildCustomizeNotification(): Notification {
        if (Build.BRAND == "samsung") {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(createPendingIntent())
                .setContentText(INFO_TITLE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_player_service_notification)
                .addAction(android.R.drawable.ic_media_play, "播放/暂停", BroadcastPlayOrPause())
                .addAction(android.R.drawable.ic_media_pause, "上一曲", BroadcastPrevious())
                .addAction(android.R.drawable.ic_media_pause, "下一曲", BroadcastNext())
                .setAutoCancel(false)
                .build()
        }
        else{
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(createPendingIntent())
                .setContentText(INFO_TITLE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_player_service_notification)
                .addAction(android.R.drawable.ic_media_play, "播放", broadcastPlay())
                .addAction(android.R.drawable.ic_media_pause, "暂停", broadcastPause())
                .setAutoCancel(false)
                .build()
        }
    }

    private fun BuildCustomViewNotification(): Notification {
        // 1. 创建 RemoteViews
        val remoteView = RemoteViews(packageName, R.layout.notification_player)

        // 2. 给每个按钮挂 PendingIntent（用 requestCode 区分）
        remoteView.setOnClickPendingIntent(
            R.id.ButtonPause,
            PendingIntent.getBroadcast(
                this, 100, Intent("ACTION_PLAY"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.ButtonNext,
            PendingIntent.getBroadcast(
                this, 101, Intent("ACTION_NEXT"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.ButtonPrevious,
            PendingIntent.getBroadcast(
                this, 102, Intent("ACTION_EXIT"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // 3. 动态文字/图片
        remoteView.setTextViewText(R.id.tvTitle, INFO_TITLE)
       // remoteView.setImageViewResource(R.id.ivCover, R.drawable.ic_player_service_notification)

        // 4. 构建 Notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_player_service_notification)  // 状态栏小图标必须保留
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCustomContentView(remoteView)          // 折叠时视图
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // 让系统给加圆角/背景
            .build()
    }

    //自定义通知:创建通知通道
    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "播放控制", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            description = "后台音频播放"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    //自定义通知:点击拉起
    @OptIn(UnstableApi::class)
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.putExtra("SOURCE","FROM_PENDING" )
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
    //自定义通知:退出指令
    @OptIn(UnstableApi::class)
    private fun broadcastExit(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_EXIT"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun BroadcastPlayOrPause(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PlayOrPause"
            data = "intent:playhouse/${System.currentTimeMillis()}".toUri()
        }
        return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun BroadcastNext(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_NextMedia"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun BroadcastPrevious(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PreviousMedia"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }





}