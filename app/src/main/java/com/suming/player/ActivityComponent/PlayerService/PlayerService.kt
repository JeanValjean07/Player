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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.suming.player.ActivityComponent.PlayerActivity.ToolPlayerWrapper
import com.suming.player.EntranceActivity
import com.suming.player.FuncionalPack.IntentRepo
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.FuncionalPack.SOURCE_CODE
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.SettingsRequestCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
@Suppress("/unused")
class PlayerService: MediaSessionService() {
    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL_ID = "playback"
    }

    //媒体会话
    private var mediaSession: MediaSession? = null

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
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
        val player = PlayerSingleton.getInitPlayer()

        //指定通知,包含设置自定义控制按钮和播控中心小图标
        setMediaNotificationProvider(CustomNotificationSession(this))

        //创建媒体会话包装器
        val wrapper = ToolPlayerWrapper(player)

        //创建自定义媒体会话
        mediaSession = MediaSession.Builder(this, wrapper)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    consoleLog("触发 onConnect")

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                        .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                        .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        .build()

                    //使用默认按钮 .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                    //使用自定按钮 .setAvailablePlayerCommands(playerCommands)
                }
                override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                    super.onPostConnect(session, controller)
                    consoleLog("触发 onPostConnect")
                }
                override fun onPlayerInteractionFinished( session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommands: Player.Commands ) {
                    super.onPlayerInteractionFinished(session, controllerInfo, playerCommands)
                    consoleLog("触发 onPlayerInteractionFinished")
                    //播放/暂停
                    if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
                        consoleLog("播放/暂停")
                        //播放或暂停
                        pauseOrContinue()

                    }
                    //下一曲
                    if (playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT)) {
                        consoleLog("下一曲")
                    }
                    //上一曲
                    if (playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS)) {
                        consoleLog("上一曲")
                    }
                    //停止播放(划掉音频播控卡片)
                    if (playerCommands.contains(Player.COMMAND_STOP)) {
                        consoleLog("停止")
                        //关掉播放引擎和监听器
                        stopPlayBundle()

                    }
                    //拖动进度
                    if (playerCommands.contains(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)) {
                        consoleLog("定位默认")
                    }
                    //以下未触发过,不知道是什么
                    if (playerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                        consoleLog("当前媒体定位")
                    }
                    if (playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
                        consoleLog("下一媒体")
                    }
                    if (playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                        consoleLog("上一媒体")
                    }
                    if (playerCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE)) {
                        consoleLog("随机模式")
                    }
                    if (playerCommands.contains(Player.COMMAND_SET_REPEAT_MODE)) {
                        consoleLog("循环模式")
                    }
                }
            })
            .build()


        //设置会话点击意图
        mediaSession?.setSessionActivity(createPendingIntentManager())

    }
    //接收Intent额外信息
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //consoleLog("触发 onStartCommand, intent: $intent, intent.action: ${intent?.action}, flags: $flags, startId: $startId")
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

    override fun onDestroy() {
        super.onDestroy()
        consoleLog("触发 onDestroy")

        stopLocalAll()

    }
    //仅在后台划卡时触发,而且前提是系统不执行强行停止
    override fun onTaskRemoved(rootIntent: Intent?) {
        consoleLog("触发 onTaskRemoved")

        val needStopEngine = SettingsRequestCenter.get_PREFS_StopPlayerWhenTaskRemoved(this@PlayerService) ||
                                       !SettingsRequestCenter.GET_PRF_EnableMiniView(this@PlayerService)


        if (needStopEngine) stopPlayBundle()



    }




    //External Operation Functions
    //销毁播放器和媒体会话
    private fun stopPlayBundle() {
        PlayerSingleton.stopPlayEngine()
        //关闭监听器
        stopPlayerListener()
        //关闭本地的媒体会话和服务
        stopLocalAll()
    }
    //关掉监听器
    private fun stopPlayerListener() {
        PlayerListener.stopListener()
    }
    //播放或暂停
    private fun pauseOrContinue() {
        //先检查目前是不是在播放(读取到的是父类修改后的状态,原本的播放状态应取反)
        val isPlaying = !PlayerSingleton.GET_STE_isNowPlaying()
        consoleLog("pauseOrContinue() 操作之前是否在播放 isPlaying: $isPlaying")

        //切换state_perception_on
        if (isPlaying){
            //执行了暂停操作


            if (!PlayerListener.isFocus){
                //在无焦点的状态下暂停,意味着再次失去焦点时期望自动暂停
                PlayerListener.state_perception_on = true
            }else{
                PlayerListener.state_perception_on = false
            }

        }else{
            //执行了继续播放操作
            if (!PlayerListener.isFocus){

                //在无焦点的状态下继续播放,意味着再次失去焦点时不期望自动暂停
                PlayerListener.state_perception_on = false

            }else{
                PlayerListener.state_perception_on = true
            }


        }
    }



    //Internal Operation Functions
    //关闭媒体会话实例
    private fun releaseMediaSession() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
    }
    //关闭服务
    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    //全套关闭本地服务
    private fun stopLocalAll() {
        releaseMediaSession()
        stopService()
    }





    //Functions
    //媒体信息 从intent获取媒体信息工具函数
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
            .addAction(android.R.drawable.ic_media_play, "播放", null)
            .addAction(android.R.drawable.ic_media_pause, "暂停", null)
            .setAutoCancel(false)
            .build()


    }
    //构建纯自定布局通知(完全自定布局)
    private fun BuildCustomViewNotification(): Notification {
        // 1. 创建 RemoteViews
        val remoteView = RemoteViews(packageName, R.layout.notification_custom_controller)

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
        val intent = Intent(this, EntranceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
            .putExtra(IntentRepo.SOURCE, SOURCE_CODE.SOURCE_Pending)

        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    //基于广播的播放指令(已废弃)
    /*
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

     */



}