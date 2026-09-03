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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
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
import com.suming.player.DataPack.DataBaseMediaSingleSetting.MediaItemSetting
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.DataClassForPlay.MediaItemForPlay
import com.suming.player.DataPack.MediaRecordPack
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.FuncionalPack.SupportFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UnstableApi
@Suppress("/unused")
object PlayerSingleton {
    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {

            this.context = context
        }
    }
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerSingleton: $msg")
        }
    }
    //空字段
    const val Undefined = ""
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()





    //播放器实例
    private var _player: ExoPlayer? = null
    //初始化播放器
    private fun buildPlayer(): ExoPlayer {
        //consoleLog("buildPlayer")

        val trackSelector = get_trackSelector(context)
        val rendererFactory = get_RendererFactory(context)
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
    //初始化播放器并获得引用
    fun init_player_get_ref(): ExoPlayer {
        //双重检查锁定初始化
        var player = _player
        if (player == null) {
            synchronized(this) {
                player = _player
                if (player == null) {
                    player = buildPlayer()
                    _player = player

                    //新的播放器实例上线
                    on_newInstance_built()

                }
            }
        }

        //添加播放器状态监听
        addPlayerStateListener()

        return player!!
    }
    //获取播放器引用但不初始化
    fun get_player_ref(): ExoPlayer? = _player
    //播放器 ID 缓存 (需公开)
    var cache_player_instance_id = 0L

    //播放器组件
    @SuppressLint("StaticFieldLeak")
    private var _trackSelector: DefaultTrackSelector? = null
    private var _rendererFactory: RenderersFactory? = null
    private fun get_trackSelector(context: Context): DefaultTrackSelector =
        _trackSelector ?: synchronized(this) {
            _trackSelector ?: DefaultTrackSelector(context)
                .also { _trackSelector = it }

        }
    private fun get_RendererFactory(context: Context): RenderersFactory =
        _rendererFactory ?: synchronized(this) {
            _rendererFactory ?: DefaultRenderersFactory(context)
                //.setEnableDecoderFallback(true)
                .also { _rendererFactory = it }
        }
    private fun release_trackSelector(){
        //_trackSelector?.release()  //不知道为什么这一行执行会崩溃说线程错误
        _trackSelector = null
    }
    private fun release_RendererFactory(){
        _rendererFactory = null
    }
    private fun create_customCodecFactory(): MediaCodecAdapter.Factory {
        @Suppress("DEPRECATION")
        return MediaCodecAdapter.Factory.DEFAULT
    }

    //播放器回调监听器
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY ->  playState_Ready()
                Player.STATE_ENDED ->  playState_End()
                //播放器进入空闲状态
                Player.STATE_IDLE -> {
                    on_EngineIdle()
                }
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //修改可观察标志,触发更新
            PlayerInfoCenter.updateObservableIsPlaying(isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            //consoleLog("onMediaItemTransition mediaItem:${mediaItem} reason:${reason}")
            onMediaItemChanged(mediaItem)
        }
        override fun onTracksChanged(tracks: Tracks) {
            for (trackGroup in tracks.groups) {
                val format = trackGroup.getTrackFormat(0)
                val fps = format.frameRate
                PlayerInfoCenter.SET_Media_ActualFPS(fps)
                break
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            on_EngineErrorOccur(error)
        }
    }
    //播放器 监听器 ID 缓存
    private var cache_player_listener_id = 0L
    //操作方法
    private fun addPlayerStateListener(){
        if (cache_player_instance_id == 0L) return
        if (cache_player_listener_id != 0L && cache_player_listener_id != cache_player_instance_id) return

        //记下监听器ID缓存
        cache_player_listener_id = cache_player_instance_id

        //执行监听器添加
        _player?.addListener(PlayerStateListener)

    }
    private fun removePlayerStateListener(){
        //清除监听器ID缓存
        cache_player_listener_id = 0L

        //执行移除
        _player?.removeListener(PlayerStateListener)
    }

    //新的播放器实例上线
    private fun on_newInstance_built(){
        //计下实例缓存
        cache_player_instance_id = System.currentTimeMillis()
        //添加监听器
        addPlayerStateListener()
        //通报实例更新
        PlayerInfoCenter.updateObservableIsIdle(cache_player_instance_id)
    }

    //播放器单一方法
    private fun core_exoplayer_clearMediaItem() {
        _player?.clearMediaItems()
    }
    private fun core_exoplayer_stop() {
        _player?.stop()
    }
    fun core_exoplayer_prepare() {
        _player?.prepare()
    }

    //释放播放器
    fun releasePlayer() {
        //移除播放器监听器
        removePlayerStateListener()
        //释放播放器并清除引用
        _player?.release()
        _player = null
        //清除ID缓存
        cache_player_instance_id = 0L


        //销毁trackSelector和rendererFactory防止偶尔复用导致exoplayer拒绝使用复用实例而崩溃
        release_trackSelector()
        release_RendererFactory()


    }
    //清除当前媒体项
    fun clearMediaItem(clear_info_center: Boolean = true){
        core_exoplayer_clearMediaItem()
        //清除媒体项信息缓存
        if (clear_info_center){
            PlayerInfoCenter.CLEAR_CurrentMediaInfo()
        }
    }

    //播放器错误处理(发生错误后应该是会自动进入idle状态)
    private fun on_EngineErrorOccur(error: PlaybackException){
        //ErrorOccur之后虽然同样进入idle状态,但只有在error导致的idle之后才尝试恢复播放

        //收集可用于恢复播放的信息
        val current_media_progress = _player?.currentPosition ?: 0L

        //仅在信息有效时开启onError标志
        if (current_media_progress > 0L) onError = true


        //解锁一次作为保底
        isLocked = false

        consoleLog(
            "EngineErrorOccur:ERROR:${error},MESSAGE:${error.message},CAUSE:${error.cause},ECN:${error.errorCodeName}\n" +
            "收集需要恢复的信息:"
        )

        //如果来自 Source Error,必须先清除媒体,再调prepare()，否之一直循环报错
        if (error.message == "Source error") clearMediaItem()


    }
    private var onError = false
    //播放器进入空闲状态
    fun on_EngineIdle(){
        consoleLog("EngineIdle:是否来自报错onError:${onError}")

        //进行操作


        //使用prepare()使播放器重新上线
        core_exoplayer_prepare()

    }


    //通知服务和媒体会话被系统侧销毁(系统侧销毁等于stop()了player让其进入idle,并带有一次主动暂停,可调用prepare()重新上线)
    fun notify_session_service_release(){
        //关闭播放器端的媒体会话(已包含关闭服务)
        stopMediaSession(context)

        //关闭监听器
        PlayerListener.stopListener()

        //调用prepare()让播放器重新上线
        core_exoplayer_prepare()
    }


    //销毁播放器并关闭媒体会话
    fun stopPlayEngineBundle(clear_info_center: Boolean = true){
        //consoleLog("stopPlayEngine")

        //清理播放项
        clearMediaItem(clear_info_center)
        //销毁播放器
        releasePlayer()
        //关闭监听器
        PlayerListener.stopListener()
        //关闭本侧的媒体会话
        stopMediaSession(context)
        //关闭服务
        stopServices(context)

    }

























    //阶段事件回环
    private var engine_phase = 0

    const val engine_phase_offline = 1   //未启动
    const val engine_phase_build_start = 2   //启动(创建)中
    const val engine_phase_build_success = 2
    const val engine_phase_build_fail = 2
    const val engine_phase_online = 3   //上线(启动完成)
    const val engine_phase_set_item_start = 4   //开始进入媒体设置流程
    const val engine_phase_set_item_success = 5   //设置完成
    const val engine_phase_item_ready = 6  //
    const val engine_phase_clear_item_start = 6  //开始清除项
    const val engine_phase_clear_item_success = 7   //清除完成
    const val engine_phase_error_occur = 21  //错误
    const val engine_phase_idle = 22   //空闲
    const val engine_phase_release_start = 8   //销毁
    const val engine_phase_release_complete = 8   //销毁





    //Long Process Functions
    //设置新媒体项的外部接口(以后可以加些过滤)(返回ActivityResultConnector内的结果码)
    private var clickMillis_setMediaItem = 0L
    suspend fun setMediaItem(URI_UP:Uri,file_path:String=Undefined,playWhenReady:Boolean,ignoreLock:Boolean=false): String {
        //检查是否已被锁定+进入流程后加锁
        if (isLocked && !ignoreLock){
            return ActivityResultConnector.OBRTV_Engine_Locked
        }
        isLocked = true
        //设置媒体项频率限制
        if (System.currentTimeMillis() - clickMillis_setMediaItem < 1500 && !ignoreLock) {

            //流程结束时解锁
            isLocked = false
            //流程中断,返回结码
            return ActivityResultConnector.OBRTV_Engine_SoFrequent
        }
        clickMillis_setMediaItem = System.currentTimeMillis()
        //检查是否启动了播放器
        if (_player == null){

            //流程结束时解锁
            isLocked = false
            //流程中断,返回结码
            return ActivityResultConnector.OBRTV_Engine_OffLine
        }

        //设置新媒体项
        val result = setMediaItemCore(URI_UP,file_path,playWhenReady)
        //流程结束时解锁
        isLocked = false

        return result
    }
    var isLocked = false
    //设置/变更媒体(设置新媒体项)(返回值为结果码)
    private suspend fun setMediaItemCore(URI_UP: Uri, file_path:String=Undefined, playWhenReady:Boolean): String {
        //consoleLog("setMediaItemCore -设置新媒体项:$uri")
        //说明:file_path的作用仅为获取文件名(也可作为文件存在检查)

        //将播放链接缓存成字符串
        val URI_S_FP = URI_UP.toString()

        //先判断是否是正在播放的媒体(约定交给外层判断)
        //Moved

        //保存上个媒体的需要保存的东西
        //TODO

        //移除上个媒体(感觉不应该在此流程里移除,副作用太多)
        //Cancelled

        //解码新媒体信息(包含检查是否需要解码:对比当前数据包的URI键是否和新URI一致,无需解码时直接拿到数据包)
        var MediaItemForPlay = MediaItemForPlay()
        val current_item_URI_SP = PlayerInfoCenter.GET_Media_URI_S_FP()
        val current_item_isCache = PlayerInfoCenter.GET_Media_isCache()
        if (current_item_URI_SP == URI_S_FP && !current_item_isCache){
            //无需再次解码
            MediaItemForPlay = PlayerInfoCenter.GET_Media_FullMediaInfoPack() ?: MediaItemForPlay()
        }else{
            //需要解码
            //获取媒体信息
            val (result,MediaItemForPlay_Cache,_) = MediaInfoRetriever.retrieveMediaInfo(
                context,
                URI_UP.toString(),
                file_path,
                Undefined,
                URI_S_FP,
            )
            //检查解码结果
            when(result){
                ActivityResultConnector.retriever_error -> {

                    return ActivityResultConnector.OBRTV_Engine_RetrieveFailed
                }
                ActivityResultConnector.retriever_type_not_support -> {
                    return ActivityResultConnector.OBRTV_Engine_TypeNotSupport
                }
                ActivityResultConnector.retriever_complete -> {
                    MediaItemForPlay = MediaItemForPlay_Cache
                }
            }
        }

        //检查媒体格式是否支持
        val format = MediaItemForPlay.media_format
        if (!SupportFormat.isFormatSupported(format)){

            return ActivityResultConnector.OBRTV_Engine_TypeNotSupport
        }

        //暂停播放
        withContext(Dispatchers.Main) { pausePlay() }

        //将MediaItemForPlay缓存到PlayerInfoCenter
        PlayerInfoCenter.SET_MediaItemForPlay_Pack(MediaItemForPlay)

        //重置单个媒体状态
        clearItemState()

        //合成并设置媒体项
        val cover_img_uri = getArtworkFrameUri(context,URI_UP)

        val delayMillis = SettingsRequestCenter.GET_PRF_forTestDelayMillis(context)
        delay(delayMillis)

        withContext(Dispatchers.Main) {
            //设置播放状态
            _player?.playWhenReady = playWhenReady

            //开始构建mediaItem
            val mediaItem = MediaItem.Builder()
                .setUri(URI_UP)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(MediaItemForPlay.file_name)
                        .setArtist(MediaItemForPlay.media_artist)
                        .setArtworkUri(cover_img_uri)
                        .build())
                .build()

            //设置给播放器
            _player?.setMediaItem(mediaItem)

        }

        return ActivityResultConnector.OBRTV_Engine_SetItemSuccess

    }
    //完成媒体项变更的后续操作
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null) return

        //解锁一次
        isLocked = false

        //启动服务和媒体会话
        startSessionService(context)

        //记录到清单
        writeToRecord(context)

        //读取单个媒体播放设置(由MediaDataBaseMaster读取并传回)


        //启动监听器(仅在播放时申请焦点)
        val focus = _player?.isPlaying ?: false
        PlayerListener.startListener(focus = focus)

        //请求音频焦点
        PlayerListener.requestAudioFocus(context, force_request = false)


    }

    //保存上个媒体的需保存内容
    private fun saveLastMediaInfo(oldInfoPack: MediaItemForPlay){
        /*
        //获取当前媒体ID数据
        val DataBaseID = oldInfoPack.MediaInfo_DataBaseID ?: ""
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

         */


    }




    //记下到播放记录
    private var coroutine_record = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun writeToRecord(context: Context){
        coroutine_record.launch {
            //获取当前信息
            val SPECIFIC_ID = PlayerInfoCenter.GET_Media_SPECIFIC_ID()

            //只有已缓存在列表的媒体才能写入记录
            val (mediaType,mediaNUMID) = MediaInfoRetriever.split_SPECIFIC_ID(SPECIFIC_ID)
            if (mediaType == MediaType.Video){
                val videoRepo = VideoRepo.get(context)
                //检查是否存在NUM_ID为目标的项
                if (!videoRepo.existsByNUM_ID(mediaNUMID)){
                    //拒绝保存
                    return@launch
                }

            }else if (mediaType == MediaType.Audio){
                val audioRepo = AudioRepo.get(context)
                //检查是否存在NUM_ID为目标的项
                if (!audioRepo.existsByNUM_ID(mediaNUMID)){
                    //拒绝保存
                    return@launch
                }

            }else{
                //拒绝保存
                return@launch
            }


            //把记录保存到记录管理器
            val mediaRecordManager = MediaRecordManager()

            val uriStandard = PlayerInfoCenter.GET_Media_URI_S_FP()
            val fileName = PlayerInfoCenter.GET_Media_FileName()
            val mediaArtist = PlayerInfoCenter.GET_Media_Artist()
            //合成信息包
            val mediaRecordPack = MediaRecordPack(
                SPECIFIC_ID,
                uriStandard,
                fileName,
                mediaArtist
            )
            //写入记录
            mediaRecordManager.writeRecord(context,mediaRecordPack)
        }
    }
    //获取艺术图链接
    private fun getArtworkFrameUri(context: Context, uri: Uri): Uri{
        if (uri.toString() != PlayerInfoCenter.GET_Media_URI_S_FP()){
            //consoleLog("发生了严重错误 getArtworkFrameUri")
            return Uri.EMPTY
        }

        //
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()

        var cover_img_uri = Uri.EMPTY
        if (SettingsRequestCenter.GET_PREFS_DisableMediaArtWork(context)){
            return Uri.EMPTY
        }else{
            //从ArtworkFrameManager获取即可
            cover_img_uri = ArtworkFrameManager.GET_ArtworkFrame_Uri(context, mediaType, NUM_ID)

        }

        return if(cover_img_uri != Uri.EMPTY){
            cover_img_uri
        }else{
            Uri.EMPTY
        }
    }


    //媒体会话和服务
    private var controller: MediaController? = null
    private var MediaSessionController: ListenableFuture<MediaController>? = null
    private var sessionState_MediaSession_connected = false
    //启动服务和媒体会话
    private fun startSessionService(context: Context){
        //链接到媒体会话
        connectToMediaSession(context)
        //未来可能需要自行写入信息以支持自定义通知
    }
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
    //完整清除媒体会话
    fun stopMediaSession(context: Context){
        stopMediaSessionController()
        stopServices(context)
        sessionState_MediaSession_connected = false
    }




    //获取当前在播放的媒体项的链接(来自播放核心)(也可在PlayerInFoCenter获取缓存)
    fun GET_STE_currentMediaItem_Uri(): Pair<Boolean, Uri> {
        if (_player == null) {
            return Pair(false, Uri.EMPTY)
        }
        //检查当前媒体
        val currentMediaItem = _player?.currentMediaItem
        if (currentMediaItem == null){

            return Pair(false, Uri.EMPTY)
        }else{
            val uri = currentMediaItem.localConfiguration?.uri

            return if (uri == null){

                Pair(false, Uri.EMPTY)
            }else{

                Pair(true, uri)
            }
        }
    }
    //是否正在播放
    fun GET_STE_isNowPlaying(): Boolean {
        if (_player == null) return false

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
    //获取是否播放结束
    fun GET_STE_playEnd(): Boolean {

        return playState_playEnd
    }

    //播放和暂停
    private var playState_playEnd = false
    private var playState_wasPlaying = false
    //继续/开始播放
    fun continuePlay(requestFocus: Boolean = true) {
        //播放结束时自动回到起始并重播
        if (playState_playEnd){
            playState_playEnd = false
            _player?.seekTo(0)
        }
        playState_wasPlaying = true

        //
        forcePause = false
        manualPause = false

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
        PlayerInfoCenter.updateObservableIsPlaying(true)

        //最终开始播放
        _player?.play()

    }
    //暂停播放
    fun pausePlay(){
        //consoleLog("pausePlay")
        //修改播放标记,记录本次暂停之前,到底有没有真的处于播放状态
        if (_player?.isPlaying == true){
            setState_wasPlaying(true)
        }else{
            setState_wasPlaying(false)
        }

        //写入可观察信息
        PlayerInfoCenter.updateObservableIsPlaying(false)

        //最终暂停
        _player?.pause()

    }
    //特殊情况下手动设置是否继续播放的标志
    fun setState_wasPlaying(wasPlaying: Boolean){
        playState_wasPlaying = wasPlaying
    }
    fun getState_wasPlaying(): Boolean = playState_wasPlaying
    //强制暂停(这种情况下,千万不能再自动继续播放)(强制暂停判断不应存在于continuePlay中,必须外部判断)
    private var forcePause = false
    fun setState_forcePause(){
        this.forcePause = true
        //自带一次暂停
        pausePlay()
    }
    fun getState_forcePause(): Boolean = forcePause
    //手动暂停
    var manualPause = false

    //重置播放结束状态
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
    private fun playState_End(){
        //若开启了本次播放完成后关闭功能
        if (timerState_autoShut_Reach){
            //关闭倒计时(含清除状态)
            timer_DisableAutoShut()
            //让播放暂停
            pausePlay()
        }

        //检查循环模式
        val loopMode = ListManagerHelper.getLoopMode()
        when(loopMode){
            ListManagerHelper.LOOP_MODE_OFF -> justStop()
            ListManagerHelper.LOOP_MODE_ONE -> repeatMedia()
            ListManagerHelper.LOOP_MODE_ALL -> requireNextMedia()
            else -> justStop()
        }
    }
    //由列表管理器进行操作
    //循环播放-寻到视频起始并播放
    fun repeatMedia(){
        _player?.seekTo(0)
        continuePlay(true)
    }
    fun checkPlayEndAndRePlay(){
        if (playState_playEnd){
            continuePlay(true)
        }
    } //列表管理器专用:切换模式时,自动开始播放
    //播完暂停-暂停视频
    fun justStop(){
        playState_playEnd = true
        pausePlay()
    }
    //列表模式-由列表管理器告知下一个媒体该放什么
    fun requireNextMedia(){
        ListManagerHelper.onPlayEndCall_switchNextMedia()

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
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()

    }
    fun trackAffair_EnableVideoTrack(){
        //
        if (state_VideoTrack_Disabled){
            state_VideoTrack_Disabled = false
            _trackSelector?.parameters = _trackSelector!!
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
        _trackSelector?.parameters = _trackSelector!!
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()

    }
    fun trackAffair_EnableAudioTrack(){
        if (state_AudioTrack_Disabled){
            state_AudioTrack_Disabled = false
            _trackSelector?.parameters = _trackSelector!!
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




    //开始后台播放-操作合集
    fun startBackgroundPlay(){
        //检查是否开启后台播放功能
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){

        }else{
            pausePlay()
        }
    }
    //回到前台播放-操作合集
    fun stopBackgroundPlay(){
        //检查是否开启后台播放功能
        if (SettingsRequestCenter.get_PREFS_BackgroundPlay(context)){

        }else{
            //关闭后台播放功能：开始继续播放
            if(playState_wasPlaying){
                if (!manualPause) {
                    consoleLog("stopBackgroundPlay 继续播放")
                    continuePlay(true)
                }
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
            PlayerListener.stopListener()
            //关闭播放器
            core_exoplayer_stop()
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