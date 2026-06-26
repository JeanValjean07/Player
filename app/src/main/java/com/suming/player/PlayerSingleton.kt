package com.suming.player

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.suming.player.ActivityComponent.PlayerService.PlayerService
import com.suming.player.AddonTools.ToolEventBus
import com.suming.player.DataPack.DataBaseMediaItem.MediaItemRepo
import com.suming.player.DataPack.DataBaseMediaItem.MediaItemSetting
import com.suming.player.DataPack.MediaInfo
import com.suming.player.FuncPack_ListManager.PlayerListManager
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaDataBaseMaster
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.MediaUriManager
import com.suming.player.FuncionalPack.PlayerInFoCenter
import com.suming.player.FuncionalPack.PlayerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
@Suppress("unused")
object PlayerSingleton {
    //应用引用
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {
            consoleLog("PlayerSingleton.setContext")
            this.context = context
        }else{
            consoleLog("PlayerSingleton.setContext error")
        }
    }
    fun getApplicationContext(): Context = context.applicationContext

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerSingleton: $msg")
        }
    }

    //播放器内部实例
    private var _player: ExoPlayer? = null

    //初始化播放器
    private fun buildPlayer(context: Context): ExoPlayer {
        val trackSelector = getTrackSelector(context)
        val rendererFactory = getRendererFactory(context)
        //创建播放器
        val ExoPlayer = ExoPlayer.Builder(context)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMaxSeekToPreviousPositionMs(1_000_000L)
            .setTrackSelector(trackSelector)
            .setRenderersFactory(rendererFactory)
            .build()
            .apply {
                prepare()
                playWhenReady = false
            }

        return ExoPlayer
    }
    //获取播放器,未启动时将播放器初始化
    fun getInitPlayer(context: Context): ExoPlayer = _player ?: synchronized(this) {
        _player ?: buildPlayer(context).also { _player = it }
    }.also {
        stateLock_isPlayerInitialized = true
        initializationCallbacks.forEach { callback -> callback.invoke() }
        initializationCallbacks.clear()
        //添加播放器状态监听
        addPlayerStateListener()
    }
    //获取播放器但不初始化
    fun getPlayer(): ExoPlayer? = _player

    //播放器组件
    fun getTrackSelector(context: Context): DefaultTrackSelector =
        inner_trackSelector ?: synchronized(this) {
            inner_trackSelector ?: DefaultTrackSelector(context)
                .also { inner_trackSelector = it }

        }
    fun getRendererFactory(context: Context): RenderersFactory =
        inner_rendererFactory ?: synchronized(this) {
            inner_rendererFactory ?: DefaultRenderersFactory(context)
                //.setEnableDecoderFallback(true)
                .also { inner_rendererFactory = it }
        }
    fun createCustomCodecFactory(): MediaCodecAdapter.Factory {
        @Suppress("DEPRECATION")
        return MediaCodecAdapter.Factory.DEFAULT
    }
    @SuppressLint("StaticFieldLeak")
    private var inner_trackSelector: DefaultTrackSelector? = null
    private var inner_rendererFactory: RenderersFactory? = null
    //外部获取播放器&组件
    fun getPlayerFromService(): ExoPlayer?{
        return _player
    }

    //播放器初始化监听
    private val initializationCallbacks = mutableListOf<() -> Unit>()
    private var stateLock_isPlayerInitialized = false
    private fun addInitializationCallback(callback: () -> Unit) {
        synchronized(initializationCallbacks) {
            if (stateLock_isPlayerInitialized && _player != null) {
                callback.invoke()
            } else {
                initializationCallbacks.add(callback)
            }
        }
    }

    //播放器回调监听
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY ->  playState_Ready()
                Player.STATE_ENDED ->  playState_End(context)
                Player.STATE_IDLE ->  {   }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //修改可观察标志,触发更新
            PlayerInFoCenter.updateObservableIsPlaying(isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onMediaItemChanged(mediaItem,context)
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            onFatalErrorOccur(error)
        }
    }
    private var playerState_PlayerStateListenerAdded = false
    fun addPlayerStateListener(){
        if (playerState_PlayerStateListenerAdded) return

        _player?.removeListener(PlayerStateListener)
        _player?.addListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = true
    }
    fun removePlayerStateListener(){
        _player?.removeListener(PlayerStateListener)
        playerState_PlayerStateListenerAdded = false
    }



    //播放器错误处理
    private fun onFatalErrorOccur(error: PlaybackException){
        consoleLog("播放器错误:${error} message:${error.message} cause:${error.cause} errorCodeName:${error.errorCodeName}")

        //记录原本的媒体uri,然后重启播放器


    }

    //销毁播放器并关闭媒体会话
    fun stopPlayEngine(){
        //销毁播放器
        releasePlayer()
        //重置媒体状态
        PlayerInFoCenter.clearCurrentMediaInfo()
        //关闭本侧的媒体会话
        stopMediaSession(context)

    }


    //获取当前媒体的播放进度
    fun getEnginCurrentProgress(): Long {
        return _player?.currentPosition ?: -1
    }

    //判断传入的链接是否为正在播放的项(核心)//TODO
    fun isthisUriOngoing(uriNeedCheck: Uri): Boolean {
        //从播放核心获取信息
        val MediaInfo_MediaUriStandard = "114514"

        //如果传入标准链接,就直接对比标准链接
        if (MediaUriManager.isMediaUriStandard(uriNeedCheck)){

            return uriNeedCheck.toString() == MediaInfo_MediaUriStandard
        }
        //若不是标准链接,先转成标准链接,再对比
        val standardUriNeedCheck = MediaUriManager.getStandardMediaUri(uriNeedCheck,context)

        return standardUriNeedCheck.toString() == MediaInfo_MediaUriStandard
    }



    //Long Process Functions
    //设置/变更媒体(设置新媒体项)
    private fun setMediaItemCore(uri: Uri, playWhenReady: Boolean, context: Context): Boolean {
        //先判断是否是正在播放的媒体
        if (isthisUriOngoing(uri)) return false

        //保存上个媒体的需要保存的东西
        if (MediaInfoPackLocal != null){
            saveLastMediaInfo(context,MediaInfoPackLocal!!)
        }


        //解码新媒体信息
        MediaInfoPackLocal = null
        retrieveMediaInFo(context, uri)
        if (MediaInfoPackLocal == null){
            return false
        }
        //把信息传递给PlayerInFoCenter
        PlayerInFoCenter.setMediaInfoPack(MediaInfoPackLocal!!)

        //重置单个媒体状态
        clearItemState()


        //设置播放状态
        _player?.playWhenReady = playWhenReady

        //合成并设置媒体项
        val cover_img_uri = getArtworkFrameUri(context, uri)

        //开始构建mediaItem
        val mediaItem = MediaItem.Builder()
            .setUri(MediaInfoPackLocal!!.MediaInfo_MediaUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(MediaInfoPackLocal!!.MediaInfo_FileName)
                    .setArtist(MediaInfoPackLocal!!.MediaInfo_MediaArtist)
                    .setArtworkUri(cover_img_uri)
                    .build())
            .build()

        //设置给播放器
        _player?.setMediaItem(mediaItem)

        return true
    }
    //设置新媒体项的外部接口(以后可以加些过滤)(返回是否设置成功)
    fun setMediaItem(uri: Uri, playWhenReady: Boolean, context: Context): Boolean {
        //设置新媒体项
        val success = setMediaItemCore(uri, playWhenReady, context)

        return success
    }
    //完成媒体项变更的后续操作
    private fun onMediaItemChanged(mediaItem: MediaItem?, context: Context){
        if (mediaItem == null) return

        //启动服务和媒体会话
        startSessionService(context)

        //记录到清单
        writeToRecord(context,MediaInfoPackLocal?.MediaInfo_MediaUriString ?: "")

        //读取单个媒体播放设置(由MediaDataBaseMaster读取并传回)
        val DataBaseID = MediaInfoPackLocal?.MediaInfo_DataBaseID ?: ""
        MediaDataBaseMaster.fetchMediaItemPack(itemID = DataBaseID, context = context)



        //请求音频焦点
        PlayerListener.requestAudioFocus(context, force_request = false)

        //修改可观察标志,触发更新
        PlayerInFoCenter.updateObservableUriString(MediaInfoPackLocal?.MediaInfo_MediaUriString ?: "")

    }

    //保存上个媒体的需保存内容
    private fun saveLastMediaInfo(context:Context ,oldInfoPack: MediaInfo){
        //获取当前媒体ID数据
        val DataBaseID = oldInfoPack.MediaInfo_DataBaseID
        val mediaDuration = oldInfoPack.MediaInfo_Duration
        val currentPosition = _player?.currentPosition ?: 0L

        //保存播放进度
        if(currentPosition in 0..mediaDuration){
            //使用MediaDataBaseMaster承担保存任务
            MediaDataBaseMaster.saveProgress(
                itemID = DataBaseID,
                currentPosition = currentPosition,
                duration = mediaDuration,
                context = context
            )
        }


    }

    //读取媒体信息
    private var MediaInfoPackLocal: MediaInfo? = null
    private fun retrieveMediaInFo(context: Context,uri: Uri){
        val (_,_MediaInfoPack) = MediaInfoRetriever.retrieveMediaInfo(context,uri)

        MediaInfoPackLocal = _MediaInfoPack
    }
    //记下到播放记录
    private fun writeToRecord(context: Context,uriStandard: String){
        //把记录保存到记录管理器
        MediaRecordManager(context).writeOneRecord(uriStandard)
    }
    //获取艺术图链接
    private fun getArtworkFrameUri(context: Context, uri: Uri): Uri?{
        if (uri != MediaInfoPackLocal!!.MediaInfo_MediaUri){
            consoleLog("发生了严重错误 getArtworkFrameUri")
            return null
        }

        //
        val uriNumOnly = MediaInfoPackLocal!!.MediaInfo_MediaUriNumOnly
        val mediaType = MediaInfoPackLocal!!.MediaInfo_MediaType

        var cover_img_uri = Uri.EMPTY
        if ( SettingsRequestCenter.get_PREFS_DisableMediaArtWork(context) ) {
            return null
        }else{
            //从ArtworkFrameManager获取即可
            cover_img_uri = ArtworkFrameManager.get_Artwork_Frame_Uri(context, mediaType, uriNumOnly)

        }

        return cover_img_uri
    }
    //启动服务和媒体会话
    private fun startSessionService(context: Context){
        //链接到媒体会话
        connectToMediaSession(context)
        //未来可能需要自行写入信息以支持自定义通知
    }



    //媒体会话和服务
    private var controller: MediaController? = null
    private var MediaSessionController: ListenableFuture<MediaController>? = null
    private var sessionState_MediaSession_connected = false
    //连接到媒体会话控制器
    private fun connectToMediaSession(context: Context){
        if (sessionState_MediaSession_connected) return
        sessionState_MediaSession_connected = true
        val SessionToken = SessionToken(context as Application, ComponentName(context, PlayerService::class.java))
        MediaSessionController = MediaController.Builder(context, SessionToken).buildAsync()
        MediaSessionController?.addListener({
            controller = MediaSessionController?.get()
            sessionState_MediaSession_connected = true
        }, MoreExecutors.directExecutor())
    }
    //关闭媒体会话控制器
    private fun stopMediaSessionController(){
        MediaSessionController?.get()?.run { release() }
        MediaSessionController = null
        controller = null
        sessionState_MediaSession_connected = false
    }
    private fun stopServices(context: Context){
        context.stopService(Intent(context, PlayerService::class.java))
        sessionState_MediaSession_connected = false
    }
    //外部接口-完整清除媒体会话
    fun stopMediaSession(context: Context){
        stopMediaSessionController()
        stopServices(context)
        sessionState_MediaSession_connected = false
    }
    //外部接口-完整启动媒体会话
    fun startMediaSession(context: Context){
        //用内部接口
        startSessionService(context)
    }



    //👀丨关闭各种组件
    //关闭播放器核心
    private fun DevastatePlayEnginBundle(context: Context){
        //执行播放器释放
        releasePlayer()
        //播放器监听器跟随销毁,重置状态
        playerState_PlayerStateListenerAdded = false
    }
    //完全清除媒体会话
    private fun DevastateMediaSessionBundle(context: Context){
        stopMediaSession(context)
    }




    //获取当前在播放的媒体项的链接(来自播放核心)(也可在PlayerInFoCenter获取缓存)
    fun getState_currentMediaItem_Uri(): Pair<Boolean, Uri> {
        if (_player == null) {
            return Pair(false, Uri.EMPTY)
        }

        val currentMediaItem = _player?.currentMediaItem

        if (currentMediaItem == null){
            consoleLog("getState_currentItem: currentMediaItem is null")
            return Pair(false, Uri.EMPTY)
        }
        val uri = currentMediaItem.localConfiguration?.uri
        if (uri == null){
            consoleLog("getState_currentItem: uri is null")
            return Pair(false, Uri.EMPTY)
        }else{
            consoleLog("getState_currentItem: currentMediaItem uri: $uri")
            return Pair(true, uri)
        }

    }
    //是否正在播放
    fun getState_isNowPlaying(): Boolean {
        return _player?.isPlaying ?: false
    }
    //获取当前媒体项完整数据包
    fun getState_currentMediaItem_Pack(): MediaItem? {
        val currentMediaItem = _player?.currentMediaItem

        return currentMediaItem
    }
    //获取当前播放进度
    fun getState_currentPosition(): Long {
        return _player?.currentPosition ?: 0L
    }

    //播放和暂停
    private var playState_playEnd = false
    private var playState_wasPlaying = false
    //继续/开始播放
    fun continuePlay(requestFocus: Boolean = true, context: Context) {
        //播放结束时自动回到起始并重播
        if (playState_playEnd){
            playState_playEnd = false
            _player?.seekTo(0)
        }
        playState_wasPlaying = true


        //请求音频焦点
        if (requestFocus){
            PlayerListener.requestAudioFocus(context,requestFocus)
        }

        //保险操作
        //1.重置倍速
        if (_player != null && _player?.playbackParameters?.speed != Para_OriginalPlaySpeed){
            _player?.setPlaybackSpeed(Para_OriginalPlaySpeed)
        }

        //写入可观察信息
        PlayerInFoCenter.updateObservableIsPlaying(true)

        //最终开始播放
        _player?.play()

    }
    //暂停播放
    fun pausePlay(){
        //修改播放标记,记录本次暂停之前,到底有没有真的处于播放状态
        if (_player?.isPlaying == true){
            setState_wasPlaying(true)
        }else{
            setState_wasPlaying(false)
        }

        //写入可观察信息
        PlayerInFoCenter.updateObservableIsPlaying(false)

        //最终暂停
        _player?.pause()

    }
    //特殊情况下手动设置是否继续播放的标志
    fun setState_wasPlaying(wasPlaying: Boolean){
        playState_wasPlaying = wasPlaying
    }
    fun cancelState_PlayEnd(){
        playState_playEnd = false
    }

    //播放状态
    private var singleItemState_readyOnce = false            //视频是否首次Ready
    private var singleItemState_notApply = false             //单个媒体参数是否已经应用
    //重置单个媒体播放状态
    private fun clearItemState(){
        singleItemState_readyOnce = false
        singleItemState_notApply = false
        mark_needApplyPara = false
    }
    //播放状态-已准备好
    private fun playState_Ready(){
        singleItemState_readyOnce = true
        //本次是否需要应用独立的项参数
        if (mark_needApplyPara){ ApplyParameters()}

    }
    private var mark_needApplyPara = false
    //播放状态-当前媒体结束
    private fun playState_End(context: Context){
        //若开启了本次播放完成后关闭功能
        if (timerState_autoShut_Reach){
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //让播放暂停
            pausePlay()
        }
        //告知列表管理器本次播放完成的消息,并带上当前媒体的链接


    }
    //由列表管理器进行操作
    //循环播放-寻到视频起始并播放
    fun ListCommand_repeatMedia(){
        _player?.seekTo(0)
        continuePlay(true,context)
    }
    //播完暂停-暂停视频
    fun ListCommand_justStop(){
        playState_playEnd = true
        pausePlay()
    }
    //列表模式-由列表管理器告知下一个媒体该放什么
    fun ListCommand_playNewItem(){

    }





    //释放播放器
    fun releasePlayer() {
        releasePlayer_standardExo()
        _player = null
        playerState_PlayerStateListenerAdded = false
    }


    //ExoPlayer标准方法
    //清除媒体项
    fun clearMediaItem_standardExo() { _player?.clearMediaItems() }
    //挂起
    fun stopPlayer_standardExo() { _player?.stop() }
    //释放
    fun releasePlayer_standardExo() {
        _player?.release()
    }



    //来自数据库的单个媒体参数
    //应用播放参数
    private fun ApplyParameters(){
        //视频已经Ready,立即应用参数
        if (singleItemState_readyOnce){
            //执行后关闭标记
            mark_needApplyPara = false
            //先解包
            val para_saveProgress = itemParaPack?.PREFS_SaveProgress ?: false
            val state_lastPosition = itemParaPack?.State_LastPosition ?: 0L

            if (para_saveProgress){
                if (state_lastPosition > 0L){
                    ApplyParametersCore(state_lastPosition)
                }
            }

        }else{
            mark_needApplyPara = true
        }
    }
    private fun ApplyParametersCore(lastPosition: Long){
        _player?.seekTo(lastPosition)
    }
    private var itemParaPack: MediaItemSetting? = null
    //接收MediaDataBaseMaster发回的完整参数包
    fun receiveParameters(itemPara: MediaItemSetting){
        itemParaPack = itemPara

        ApplyParameters()
    }




    //轨道启用和禁用(需升级为引用计数自动切换)
    private var state_VideoTrack_Disabled = true
    private var state_AudioTrack_Disabled = true
    fun trackAffair_DisableVideoTrack(){
        //防止重复执行
        if (state_VideoTrack_Disabled) return
        //执行禁用视频轨道
        state_VideoTrack_Disabled = true
        inner_trackSelector?.parameters = inner_trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()

    }
    fun trackAffair_EnableVideoTrack(){
        //
        if (state_VideoTrack_Disabled){
            state_VideoTrack_Disabled = false
            inner_trackSelector?.parameters = inner_trackSelector!!
                .buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .build()

        }
    }
    fun trackAffair_DisableAudioTrack(){
        //
        if (state_AudioTrack_Disabled) return
        //执行禁用音频轨道
        state_AudioTrack_Disabled = true
        inner_trackSelector?.parameters = inner_trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()

    }
    fun trackAffair_EnableAudioTrack(){
        if (state_AudioTrack_Disabled){
            state_AudioTrack_Disabled = false
            inner_trackSelector?.parameters = inner_trackSelector!!
                .buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }
    }

    //倍速管理
    private var Para_OriginalPlaySpeed = 1.0f
    fun setPlaySpeed(speed: Float){
        _player?.setPlaybackSpeed(speed)
        Para_OriginalPlaySpeed = speed
    }
    fun setPlaySpeedByLongPress(speed: Float){
        _player?.setPlaybackSpeed(speed)
    }
    fun getPlaySpeed(): Pair<Float, Float>{
        return Pair(_player?.playbackParameters?.speed ?: 1.0f, Para_OriginalPlaySpeed)
    }




    //其他播放器功能
    //前台状态汇总器
    //注意：后台播放控制功能仅在播放页才生效,如果在主页并开启了MiniView,则永远保持开启后台播放,无法关闭
    //ActivityOnResume和ActivityOnStop仅接收视频播放页传回的回调
    fun ActivityOnResume(context: Context){
        stopBackgroundPlay(context)
    }
    fun ActivityOnStop(context: Context){
        startBackgroundPlay(context)
    }
    //开始后台播放-操作合集
    private fun startBackgroundPlay(context: Context){
        //检查是否开启后台播放功能
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){

        }else{
            pausePlay()
        }
    }
    //回到前台播放-操作合集
    private fun stopBackgroundPlay(context: Context){
        //检查是否开启后台播放功能
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){

        }else{
            //关闭后台播放功能：开始继续播放
            if(playState_wasPlaying){
                continuePlay(true, context)
            }
        }
    }



    //定时关闭倒计时器
    private var timer_autoShut: CountDownTimer? = null
    private var countDownDuration_Ms = 0
    private var shutDownMoment = ""
    private var timerState_autoShut_Reach = false
    private fun timer_notification(context: Context) {
        val channelId = "toast_replace"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .setContentTitle(null)
            .setContentText("本次播放完毕后将自动关闭")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)

    }
    private fun timer_DisableAutoShut(){
        countDownDuration_Ms = 0
        shutDownMoment = ""
        timerState_autoShut_Reach = false
        timer_autoShut?.cancel()
    }
    private fun timer_startAutoShut(countDownDuration_Ms: Int){
        timer_autoShut?.cancel()
        timer_autoShut = object : CountDownTimer(countDownDuration_Ms.toLong(), 1000000L) {
            override fun onTick( millisUntilFinished: Long) {}
            override fun onFinish() { timerState_autoShut_Reach = true }
        }.start()
    }
    private fun timer_autoShut_Reach(context: Context) {
        //需等待当前媒体结束后关闭
        if (SettingsRequestCenter.get_PREFS_OnlyStopUnMediaEnd(context)) {
            countDownDuration_Ms = 0
            shutDownMoment = "shutdown_when_end"
            timerState_autoShut_Reach = true
            timer_notification(context)
        }
        //直接关闭
        else{
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //关闭监听器
            PlayerListener.stopListener(context)
            //关闭播放器
            stopPlayer_standardExo()
        }
    }
    fun set_timer_autoShut(CountDownDuration_Min: Int){
        //传入0即为关闭
        if (CountDownDuration_Min == 0){
            timer_DisableAutoShut()
            return
        }
        //记录倒计时时长,单位：毫秒
        countDownDuration_Ms = (CountDownDuration_Min * 60_000L).toInt()
        //计算关闭时间
        //val nowDateTime: String = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val nowMillis = System.currentTimeMillis()
        val shutDownMillis = nowMillis + countDownDuration_Ms.toLong()  //分钟转毫秒
        val pattern = SimpleDateFormat("HH时mm分ss秒", Locale.getDefault())
        shutDownMoment = pattern.format(Date(shutDownMillis))
        //启动倒计时
        timer_startAutoShut(countDownDuration_Ms)
    }
    fun get_timer_autoShut(): String{
        return shutDownMoment
    }


//object END
}