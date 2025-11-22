package com.suming.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures


@UnstableApi
class PlayerServiceTest(): MediaSessionService() {
    //MediaSession
    private var mediaSession: MediaSession? = null
    private var EnsureMediaSession = false

    //媒体信息和配置信息
    private var info_MediaTitle: String? = null

    private lateinit var PREFS: SharedPreferences
    private var PREFS_UseMediaSession: Boolean = false



    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        //读取配置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS_UseMediaSession = PREFS.getBoolean("PREFS_UseMediaSession", false)

        // 创建播放器实例
        val player = PlayerSingleton.getPlayer(application)

        // 创建媒体会话回调
        val sessionCallback = object : MediaSession.Callback {
            // 使用正确的方法签名处理自定义命令
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == "UPDATE_METADATA_FIELDS") {
                    // 从Bundle中获取各个元数据字段
                    val title = args.getString("METADATA_TITLE")
                    val artist = args.getString("METADATA_ARTIST")

                    // 在服务端重建MediaMetadata对象
                    val metadataBuilder = MediaMetadata.Builder()
                    title?.let { metadataBuilder.setTitle(it) }
                    artist?.let { metadataBuilder.setArtist(it) }

                    // 创建新的MediaItem，保留原有URI但更新元数据
                    /*
                    val newMediaItem = MediaItem.Builder()
                        .setUri(currentMediaItem.localConfiguration?.uri)
                        .setMediaMetadata(metadataBuilder.build())
                        .build()

                    // 替换当前媒体项，不改变播放状态
                    player.replaceMediaItem(player.currentMediaItemIndex, newMediaItem)

                    // 恢复播放位置和状态
                    player.seekTo(currentPosition)
                    if (isPlaying) {
                        player.playWhenReady = true
                    }

                     */




                    // 返回成功结果
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {

                val sessionCommands = SessionCommands.Builder()
                    .build()

                val playerCommands = Player.Commands.Builder()
                    .addAll(Player.COMMAND_PLAY_PAUSE) // 添加播放暂停命令
                    .addAll(Player.COMMAND_SEEK_TO_NEXT) // 添加下一首命令
                    .addAll(Player.COMMAND_SEEK_TO_PREVIOUS) // 添加上一首命令
                    .build()

                // 处理连接逻辑
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    playerCommands
                )
            }

        }

        // 创建媒体会话时设置回调
        mediaSession = MediaSession.Builder(this, player)
            .setId("TestSession")
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    Log.d("SuMing", "服务端: 接受连接请求")

                    val sessionCommands = SessionCommands.Builder()
                        .build()

                    val playerCommands = Player.Commands.Builder()
                        .addAll(Player.COMMAND_PLAY_PAUSE) // 添加播放暂停命令
                        .addAll(Player.COMMAND_SEEK_TO_NEXT) // 添加下一首命令
                        .addAll(Player.COMMAND_SEEK_TO_PREVIOUS) // 添加上一首命令
                        .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        // 还可以设置其他参数，如自定义布局等
                        .build()
                }
            })
            .build()



        Log.d("SuMing", "服务端: MediaSession 创建完成")

        mediaSession?.setSessionActivity(createPendingIntent())

        //不论需不需要播控中心都要发通知,这是安卓要求,只是有播控中心时自动隐藏通知
        val NotificationCustomized = BuildCustomizeNotification()
        createNotificationChannel()
        startForeground(NOTIF_ID, NotificationCustomized)

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SuMing", "服务端: onDestroy() 服务销毁")

        mediaSession?.run {
            release()
            mediaSession = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        Log.d("SuMing", "服务端: onTaskRemoved 服务销毁")

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
            info_MediaTitle = it.getStringExtra("info_to_service_MediaTitle")
        }


        //启用通知
        if (!PREFS_UseMediaSession){

            val NotificationCustomized = BuildCustomizeNotification()
            createNotificationChannel()
            //startForeground(NOTIF_ID, NotificationCustomized)

        }


        //END
        return START_NOT_STICKY
    }




    //自定义通知:构建通知
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
        val intent = Intent(this, PlayerActivityTest::class.java).apply {
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