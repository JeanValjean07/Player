package com.suming.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
@Suppress("unused")
class PlayerService(): MediaSessionService() {
    //通知标识变量
    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }
    //媒体会话实例
    private var mediaSession: MediaSession? = null
    //服务专项设置和媒体信息
    private lateinit var serviceLink: SharedPreferences
    private var Info_PlayPageType: Int = 0  // 0:经典 1:新晋
    private var MediaInfo_MediaType: String? = null
    private var MediaInfo_MediaUriString: String? = null
    private var MediaInfo_FileName: String? = null


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        //读取配置文件
        serviceLink = getSharedPreferences("serviceLink", MODE_PRIVATE)
        Info_PlayPageType = serviceLink.getInt("Info_PlayPageType", -1)
        MediaInfo_MediaType = serviceLink.getString("MediaInfo_MediaType", "error")
        MediaInfo_MediaUriString = serviceLink.getString("MediaInfo_MediaUriString", "error")
        MediaInfo_FileName = serviceLink.getString("MediaInfo_FileName", "error")
        Log.d("SuMing","setServiceLink Info_PlayPageType = $Info_PlayPageType")
        //获取播放器实例
        val player = PlayerSingleton.getPlayer(application)

        //指定通知,包含设置自定义控制按钮和播控中心小图标
        setMediaNotificationProvider(ToolCustomNotificationSession(this))
        //创建媒体会话包装器
        val wrapper = ToolPlayerWrapper(player)

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
        when(MediaInfo_MediaType){
            "music" -> {

            }
            "video" -> {
                when(Info_PlayPageType){
                    0 -> { mediaSession?.setSessionActivity(createPendingIntentOro()) }
                    1 -> { mediaSession?.setSessionActivity(createPendingIntentNeo()) }
                }
            }
        }

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    //手动关闭服务时调用
    override fun onDestroy() {
        super.onDestroy()

        mediaSession?.run {
            release()
            mediaSession = null
        }
    }
    //仅在后台划卡时触发,而且前提是系统不执行强行停止
    override fun onTaskRemoved(rootIntent: Intent?) {
        //销毁媒体会话
        mediaSession?.release()
        //关闭服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        //关闭播放器
        PlayerSingleton.stopPlayBundle(false,this)
    }
    //接收Intent额外信息
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //取出数据
        intent?.let {
             //MediaInfo_MediaTitle = it.getStringExtra("info_to_service_MediaTitle")
        }

        //END
        return START_REDELIVER_INTENT
    }


    //Functions
    //自定义通知:构建常规通知
    private fun BuildCustomizeNotification(): Notification {
        when(MediaInfo_MediaType){
            "music" -> {
                return NotificationCompat.Builder(this, CHANNEL_ID)
                    //.setContentIntent(createPendingIntentOro())  需更换音乐播放器
                    .setContentText(MediaInfo_FileName)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.ic_player_service_notification)
                    .addAction(android.R.drawable.ic_media_play, "播放", BroadcastPlay())
                    .addAction(android.R.drawable.ic_media_pause, "暂停", BroadcastPause())
                    .setAutoCancel(false)
                    .build()
            }
            "video" -> {
                when(Info_PlayPageType){
                    0 -> {
                        return NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentIntent(createPendingIntentOro())
                        .setContentText(MediaInfo_FileName)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setSmallIcon(R.drawable.ic_player_service_notification)
                        .addAction(android.R.drawable.ic_media_play, "播放", BroadcastPlay())
                        .addAction(android.R.drawable.ic_media_pause, "暂停", BroadcastPause())
                        .setAutoCancel(false)
                        .build()
                    }
                    1 -> {
                        return NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentIntent(createPendingIntentNeo())
                            .setContentText(MediaInfo_FileName)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setSmallIcon(R.drawable.ic_player_service_notification)
                            .addAction(android.R.drawable.ic_media_play, "播放", BroadcastPlay())
                            .addAction(android.R.drawable.ic_media_pause, "暂停", BroadcastPause())
                            .setAutoCancel(false)
                            .build()
                    }
                    else -> {
                        return NotificationCompat.Builder(this, CHANNEL_ID)
                            //.setContentIntent(createPendingIntentOro())
                            .setContentText(MediaInfo_FileName)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setSmallIcon(R.drawable.ic_player_service_notification)
                            .addAction(android.R.drawable.ic_media_play, "播放", BroadcastPlay())
                            .addAction(android.R.drawable.ic_media_pause, "暂停", BroadcastPause())
                            .setAutoCancel(false)
                            .build()
                    }
                }
            }
            else -> {
                return NotificationCompat.Builder(this, CHANNEL_ID)
                    //.setContentIntent(createPendingIntentOro())
                    .setContentText(MediaInfo_FileName)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSmallIcon(R.drawable.ic_player_service_notification)
                    .addAction(android.R.drawable.ic_media_play, "播放", BroadcastPlay())
                    .addAction(android.R.drawable.ic_media_pause, "暂停", BroadcastPause())
                    .setAutoCancel(false)
                    .build()

            }
        }
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
        remoteView.setTextViewText(R.id.tvTitle, MediaInfo_FileName)
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
    private fun createPendingIntentNeo(): PendingIntent {
        val intent = Intent(this, PlayerActivityNeo::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")
            .putExtra("MediaInfo_MediaUri", MediaInfo_MediaUriString)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun createPendingIntentOro(): PendingIntent {
        val intent = Intent(this, PlayerActivityOro::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")
            .putExtra("MediaInfo_MediaUri", MediaInfo_MediaUriString)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    //自定义通知:播放指令
    @OptIn(UnstableApi::class)
    private fun BroadcastPlay(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PLAY"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    //自定义通知:暂停指令
    @OptIn(UnstableApi::class)
    private fun BroadcastPause(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_PAUSE"
        }
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }


}