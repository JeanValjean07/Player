package com.suming.player.ActivityComponent.PlayerService

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suming.player.ExternalInvokeManager
import com.suming.player.PlayerActionReceiver
import com.suming.player.PlayerActivityNeo
import com.suming.player.PlayerActivityOro
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.ActivityComponent.PlayerService.CustomNotificationSession
import com.suming.player.ActivityComponent.PlayerActivity.ToolPlayerWrapper
import com.suming.player.FuncionalPack.BroadcastActions
import com.suming.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
@Suppress("unused")
class PlayerService: MediaSessionService() {
    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }

    //媒体会话
    private var mediaSession: MediaSession? = null

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerService: $msg")
        }
    }



    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        consoleLog("触发 onCreate")
        //从serviceLinker获取信息丨按理说媒体会话还无需用到这些信息,供以后添加自定义通知使用
        val (uriString, fileName, mediaArtist) = ServiceConnector.getMediaBasicInfo()
        MediaInfo_MediaUriString = uriString
        MediaInfo_FileName = fileName
        MediaInfo_Artist = mediaArtist


        //获取播放器
        val player = PlayerSingleton.getInitPlayer(application)

        //指定通知,包含设置自定义控制按钮和播控中心小图标
        setMediaNotificationProvider(CustomNotificationSession(this))

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
        mediaSession?.setSessionActivity(createPendingIntentManager())



    }
    //接收Intent额外信息
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        consoleLog("触发 onStartCommand")
        super.onStartCommand(intent, flags, startId)
        //取出intent的数据
        //getMediaInfo(intent)




        return START_REDELIVER_INTENT
    }
    //获取媒体会话
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        consoleLog("触发 onGetSession")

        return mediaSession
    }
    //手动关闭服务时调用
    override fun onDestroy() {
        super.onDestroy()
        consoleLog("触发 onDestroy")

        //关闭媒体会话
        releaseMediaSession()


    }
    //仅在后台划卡时触发,而且前提是系统不执行强行停止
    override fun onTaskRemoved(rootIntent: Intent?) {
        consoleLog("触发 onTaskRemoved")
        //关闭媒体会话
        releaseMediaSession()

        //关闭服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        //关闭播放器(改为仅暂停视频和关闭监听)(暂时停用)
        //PlayerSingleton.stopPlayBundle(false,this)
    }




    //关闭媒体会话
    private fun releaseMediaSession() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
    }


    //媒体信息
    private var MediaInfo_MediaUriString = ""
    private var MediaInfo_FileName = ""
    private var MediaInfo_Artist = ""
    private fun getMediaInfo(intent: Intent?){
        intent?.let {
            MediaInfo_MediaUriString = it.getStringExtra("info_to_service_MediaUriString") ?: ""
            MediaInfo_FileName = it.getStringExtra("info_to_service_FileName") ?: ""
            MediaInfo_Artist = it.getStringExtra("info_to_service_Artist") ?: ""
        }
    }


    //构建自定义通知(自定标题+横排文本按钮)
    private fun BuildCustomizeNotification(): Notification {

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
    //构建纯自定布局通知(完全自定布局)
    private fun BuildCustomViewNotification(): Notification {
        // 1. 创建 RemoteViews
        val remoteView = RemoteViews(packageName, R.layout.notification_player)

        // 2. 给每个按钮挂 PendingIntent（用 requestCode 区分）
        remoteView.setOnClickPendingIntent(
            R.id.ButtonPause,
            PendingIntent.getBroadcast(
                this, 100,
                Intent("ACTION_PLAY"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.ButtonNext,
            PendingIntent.getBroadcast(
                this, 101,
                Intent("ACTION_NEXT"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        remoteView.setOnClickPendingIntent(
            R.id.ButtonPrevious,
            PendingIntent.getBroadcast(
                this, 102,
                Intent("ACTION_EXIT"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
    //创建通知通道
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


    //通过观察者动态更改拉起活动意图(未启用)
    private var Job_observe: Job? = null
    private var coroutine_observe = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun startObserve_PendingIntent(){
        Job_observe?.cancel()
        Job_observe = coroutine_observe.launch {
            ServiceConnector.MediaType.collect { mediaType ->

            }
        }
    }
    //拉起活动意图(暂未使用)
    //直接拉起管理器,管理器自动判断到底拉起哪个页面
    private fun createPendingIntentManager(): PendingIntent {
        val intent = Intent(this, ExternalInvokeManager::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")

        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun createPendingIntentVideoNeo(): PendingIntent {
        val intent = Intent(this, PlayerActivityNeo::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")
            .putExtra("MediaInfo_MediaUri", MediaInfo_MediaUriString)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun createPendingIntentVideoOro(): PendingIntent {
        val intent = Intent(this, PlayerActivityOro::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")
            .putExtra("MediaInfo_MediaUri", MediaInfo_MediaUriString)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun createPendingIntentMusic(): PendingIntent {
        val intent = Intent(this, MusicPlayer::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra("IntentSource", "FromPendingIntent")
            .putExtra("MediaInfo_MediaUri", MediaInfo_MediaUriString)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }


    //基于广播的播放指令(暂未使用)
    private fun BroadcastPlay(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java)
        //加入action
        intent.apply { action = BroadcastActions.broadcast_action_play }


        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun BroadcastPause(): PendingIntent {
        val intent = Intent(this, PlayerActionReceiver::class.java)
        //加入action
        intent.apply { action = BroadcastActions.broadcast_action_pause }

        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }


//service END
}