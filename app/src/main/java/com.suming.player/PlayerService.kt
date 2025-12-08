package com.suming.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlayerService(): MediaSessionService() {
    //媒体会话实例
    private var mediaSession: MediaSession? = null
    //媒体信息和配置信息
    private var info_MediaTitle: String? = null
    //设置
    private lateinit var PREFS: SharedPreferences
    private var PREFS_UseMediaSession: Boolean = true
    //通知标识变量
    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        //读取配置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS_UseMediaSession = PREFS.getBoolean("PREFS_UseMediaSession", false)
        //是否启用播控中心
        if (PREFS_UseMediaSession) {
            //获取播放器实例
            val player = PlayerSingleton.getPlayer(application)
            //指定通知provider
            setMediaNotificationProvider(ToolCustomNotificationSession(this))
            //创建媒体会话包装器
            val wrapper = ToolPlayerWrapper(player)

            //创建基本媒体会话
            //mediaSession = MediaSession.Builder(this, player).build()

            //创建自定义媒体会话
            mediaSession = MediaSession.Builder(this, wrapper)
                .setCallback(object : MediaSession.Callback {
                    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                            .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                            .build()

                        //使用默认按钮 .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        //使用自定按钮 .setAvailablePlayerCommands(playerCommands)
                    }
                    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                        super.onPostConnect(session, controller)

                    }
                })
                .build()
            //设置会话点击意图
            mediaSession?.setSessionActivity(createPendingIntent())
        }

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            release()
            mediaSession = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        mediaSession?.player?.pause()
        mediaSession?.release()

        stopForeground(STOP_FOREGROUND_REMOVE)

        stopSelf()

    }
    //接收Intent额外信息
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //取出数据
        intent?.let {
            info_MediaTitle = it.getStringExtra("info_to_service_MediaTitle")
        }

        //是否启用自定义通知
        if (!PREFS_UseMediaSession){
            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            startForeground(NOTIF_ID, NotificationCustomized)
        }

        //END
        return START_NOT_STICKY
    }


    //Functions
    //自定义通知:构建常规通知
    private fun BuildCustomizeNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(createPendingIntent())
            .setContentText(info_MediaTitle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .addAction(android.R.drawable.ic_media_play, "播放", broadcastPlay())
            .addAction(android.R.drawable.ic_media_pause, "暂停", broadcastPause())
            .setAutoCancel(false)
            .build()
    }
    //自定义通知:构建自定布局通知
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
        remoteView.setTextViewText(R.id.tvTitle, info_MediaTitle)
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
    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "播放控制", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            description = "后台音频播放"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    //通知卡片和媒体会话卡片:点击拉起
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