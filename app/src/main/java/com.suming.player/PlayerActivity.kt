package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.suming.player.PlayerActivity.DeviceCompatUtil.isCompatibleDevice
import data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode
import kotlin.math.min

@UnstableApi
class PlayerActivity: AppCompatActivity(){

    //时间戳信息显示位
    private lateinit var tvCurrentTime: TextView
    //视频信息预读
    private var videoDuration = 0         //多成员使用:视频时长
    private var absolutePath = ""        //多成员使用:视频绝对路径
    private lateinit var videoUri: Uri
    //播放器初始化
    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private var wasPlaying = true         //上一次暂停时是否在播放
    //音量配置参数
    private var currentVolume = 0
    //缩略图绘制参数
    private var maxPicNumber = 20         //缩略图最大数量(写死)
    private var eachPicWidth = 0          //单张缩略图最大宽度(现场计算),高度45dp布局写死
    private var picNumber = 0             //缩略图数量(现场计算)
    private var eachPicDuration: Int = 0  //单张缩略图对应时长(现场计算)
    //点击和滑动状态标识 + onScrolled回调参数
    private var onDown = false
    private var dragging = false
    private var fling =false
    private var singleTap = false
    private var scrolling = false
    private var scrollerTouching = false
    private var videoTimeTo = 0L
    //旧机型标识
    private var isCompatibleDevice = false
    //视频打开失败标识
    private var cannotOpen = false
    //点击隐藏控件
    private var widgetsShowing = true
    //时间戳刷新间隔
    private var videoTimeSyncGap = 50L
    private var scrollParam1 = 0
    private var scrollParam2 = 0
    private var syncScrollRunnableGap = 16L
    //SavedInstanceState
    private var SAVEDINSTANCE_WasPlaying = false
    private var SAVEDINSTANCE_SeekFromSave = false
    //功能:倍速滚动
    private var currentTime = 0L
    private var lastPlaySpeed = 0f
    private var forceSeekGap = 5000L
    //功能:VideoSeek
    private var isSeekReady = true  //Seek结束标记
    private var backSeek = false    //滚动方向标记
    //功能:滚动后判断是否继续播放:SeekOnce运行标记
    private var fromSeekOnce = false
    //判断体:PlayerReady(播放状态)
    private var firstEntry = true
    private var playerReady = false
    private var playerReadyFromFirst = false
    private var playEnd = false
    private var readyFromSeek = false
    //设置:PREFS_RC
    private var PREFS_RC_TapScrollEnabled = false
    private var PREFS_RC_LinkScrollEnabled = false
    private var PREFS_RC_AlwaysSeekEnabled = false
    private var PREFS_RC_GenerateThumbSYNC = true
    private var PREFS_RC_BackgroundPlay = false
    private var PREFS_RC_LoopPlay = false
    //设置:PREFS_S
    private var PREFS_S_ExitWhenEnd = false
    private var PREFS_S_UseLongScroller = false
    private var PREFS_S_UseLongSeekGap = false
    private var PREFS_S_UseBlackScreenInLandscape = false
    //RunnableRunning
    private var seekRunnableRunning = false
    private var smartScrollRunnableRunning = false
    private var syncScrollRunnableRunning = false

    //旧机型兼容判断
    object DeviceCompatUtil {
        /*
        private val SOC_MAP = mapOf(
            "kirin710" to 700,
            "kirin970" to 970,
            "kirin980" to 980,
            "kirin990" to 990,
            "kirin9000" to 1000,

            "msm8998"  to 835,
            "sdm845"   to 845,
        )
        */
        fun isCompatibleDevice(): Boolean {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val hw = Build.HARDWARE.lowercase()
                //val soc = SOC_MAP.entries.find { hw.contains(it.key) }?.value ?: return false
                return when {
                    hw.contains("kirin") -> return true
                    hw.contains("sdm") -> return true       //暂不完善soc细分判断
                    else -> false
                }
            }else{
                return false
            }
        }
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)


        //设置项读取,检查和预置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if (!prefs.contains("PREFS_GenerateThumbSYNC")){
            prefs.edit { putBoolean("PREFS_GenerateThumbSYNC", true) }
            PREFS_RC_GenerateThumbSYNC = prefs.getBoolean("PREFS_GenerateThumbSYNC", true)
        }else{
            PREFS_RC_GenerateThumbSYNC = prefs.getBoolean("PREFS_GenerateThumbSYNC", true)
        }
        if (!prefs.contains("PREFS_TapScrolling")){
            prefs.edit { putBoolean("PREFS_TapScrolling", false) }
            PREFS_RC_TapScrollEnabled = prefs.getBoolean("PREFS_TapScrolling", false)
        }else{
            PREFS_RC_TapScrollEnabled = prefs.getBoolean("PREFS_TapScrolling", false)
        }
        if (!prefs.contains("PREFS_LinkScrolling")){
            prefs.edit { putBoolean("PREFS_LinkScrolling", true) }
            PREFS_RC_LinkScrollEnabled = prefs.getBoolean("PREFS_LinkScrolling", false)
        } else {
            PREFS_RC_LinkScrollEnabled = prefs.getBoolean("PREFS_LinkScrolling", false)
        }
        if (!prefs.contains("PREFS_AlwaysSeek")){
            prefs.edit { putBoolean("PREFS_AlwaysSeek", false) }
            PREFS_RC_AlwaysSeekEnabled = prefs.getBoolean("PREFS_AlwaysSeek", false)
        } else{
            PREFS_RC_AlwaysSeekEnabled = prefs.getBoolean("PREFS_AlwaysSeek", false)
        }
        if (!prefs.contains("PREFS_BackgroundPlay")){
            prefs.edit { putBoolean("PREFS_BackgroundPlay", false) }
            PREFS_RC_BackgroundPlay = prefs.getBoolean("PREFS_BackgroundPlay", false)
        } else{
            PREFS_RC_BackgroundPlay = prefs.getBoolean("PREFS_BackgroundPlay", false)
        }
        if (!prefs.contains("PREFS_LoopPlay")){
            prefs.edit { putBoolean("PREFS_LoopPlay", false) }
            PREFS_RC_LoopPlay = prefs.getBoolean("PREFS_LoopPlay", false)
        } else{
            PREFS_RC_LoopPlay = prefs.getBoolean("PREFS_LoopPlay", false)
        }
        if (!prefs.contains("PREFS_ExitWhenEnd")){
            prefs.edit { putBoolean("PREFS_ExitWhenEnd", false) }
            PREFS_S_ExitWhenEnd = prefs.getBoolean("PREFS_ExitWhenEnd", false)
        } else{
            PREFS_S_ExitWhenEnd = prefs.getBoolean("PREFS_ExitWhenEnd", false)
        }
        if (!prefs.contains("PREFS_UseLongScroller")){
            prefs.edit { putBoolean("PREFS_UseLongScroller", false) }
            PREFS_S_UseLongScroller = prefs.getBoolean("PREFS_UseLongScroller", false)
        } else{
            PREFS_S_UseLongScroller = prefs.getBoolean("PREFS_UseLongScroller", false)
        }
        if (!prefs.contains("PREFS_UseLongSeekGap")){
            prefs.edit { putBoolean("PREFS_UseLongSeekGap", false) }
            PREFS_S_UseLongSeekGap = prefs.getBoolean("PREFS_UseLongSeekGap", false)
            if (PREFS_S_UseLongSeekGap){
                forceSeekGap = 20000L
            }
        } else{
            PREFS_S_UseLongSeekGap = prefs.getBoolean("PREFS_UseLongSeekGap", false)
            if (PREFS_S_UseLongSeekGap){
                forceSeekGap = 20000L
            }
        }
        if (!prefs.contains("PREFS_UseBlackScreenInLandscape")){
            prefs.edit { putBoolean("PREFS_UseBlackScreenInLandscape", false) }
            PREFS_S_UseBlackScreenInLandscape = prefs.getBoolean("PREFS_UseBlackScreenInLandscape", false)
        } else{
            PREFS_S_UseBlackScreenInLandscape = prefs.getBoolean("PREFS_UseBlackScreenInLandscape", false)
        }

        //横屏:完全隐藏状态栏,竖屏:显示到状态栏下并动态更改退出按钮位置
        fun orientationDecide(){
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                statusBarsSetting()
                if (PREFS_S_UseBlackScreenInLandscape){
                    setBlackScreenInLandscape()
                }
            }else{
                val buttonExit=findViewById<View>(R.id.buttonExit)
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
                    val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    (buttonExit.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (statusBarHeight + 8)
                    insets
                }
            }
        }
        orientationDecide()


        //音频设备回调
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(deviceCallback, null)

        //内部广播接收
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val filter = IntentFilter("114514")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("key")
                if (data == "PLAYER_PLAY"){
                    if (playEnd){
                        player.seekTo(0)
                        player.play()
                    }else{
                        player.play()
                    }
                }
                if (data == "PLAYER_PAUSE"){
                    player.pause()
                }
            }
        }
        localBroadcastManager.registerReceiver(receiver, filter)

        //其他预设
        preCheck()

        //区分打开方式并反序列化
        val videoItem: VideoItem? = when (intent?.action) {
            Intent.ACTION_SEND -> {
                PREFS_S_ExitWhenEnd = true
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                VideoItem(0, uri, "" , 0)
            }
            Intent.ACTION_VIEW -> {
                PREFS_S_ExitWhenEnd = true
                val uri = intent.data ?: return finish()
                VideoItem(0, uri, "", 0)
            }
            else -> {
                PREFS_S_ExitWhenEnd = false
                IntentCompat.getParcelableExtra(intent, "video", VideoItem::class.java)}
        }
        if (videoItem == null) {
            Toast.makeText(this, "读取视频失败", Toast.LENGTH_SHORT).show()
            cannotOpen = true
            finish()
            return
        }

        //指定videoUri并在重载中修改
        videoUri = videoItem.uri
        if (savedInstanceState != null) {
            val uri = savedInstanceState.getString("SAVEDINSTANCE_Uri")?.toUri()
            val videoUriRec = uri!!
            videoUri = videoUriRec
            SAVEDINSTANCE_WasPlaying = savedInstanceState.getBoolean("SAVEDINSTANCE_WasPlaying")
            currentTime = savedInstanceState.getLong("SAVEDINSTANCE_CurrentTime")
            //firstEntry = savedInstanceState.getBoolean("SAVEDINSTANCE_FirstEntry")
            if (!firstEntry){
                val cover = findViewById<View>(R.id.cover)
                cover.visibility = View.GONE
            }
        }

        //初始化ExoPlayer
        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(WAKE_MODE_NETWORK)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = false
            }
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = player



        if (savedInstanceState != null) {
            player.seekTo(currentTime)
            SAVEDINSTANCE_SeekFromSave = true
        }

        //动态控件初始化：时间戳 + 总时长 + 遮罩淡出
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        player.addListener(object : Player.Listener {
            @SuppressLint("SwitchIntDef")
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        playerReadyFromFirst = true
                        playerReady = true
                        playerReady()
                    }
                    Player.STATE_ENDED -> {
                        playerEnd()
                    }
                    Player.STATE_IDLE -> {
                        stopVideoTimeSync()
                        stopScrollerSync()
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                buttonRefresh()
            }
        })


        //时间戳文本
        val seekStop = findViewById<TextView>(R.id.tvCurrentTime)
        seekStop.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                notice("暂停播放",1000)
            } else {
                player.play()
                notice("继续播放",1000)

            }
            buttonRefresh()
        }

        //控件：缩略图滚动条初始化
        val thumbScroller = findViewById<RecyclerView>(R.id.rvThumbnails)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val sidePadding = screenWidth / 2
        thumbScroller.setPadding(sidePadding, 0, sidePadding-1, 0) //右边需要减一，否则滑动区域会超出
        val videoUri = videoItem.uri
        absolutePath = getAbsoluteFilePath(this@PlayerActivity, videoUri).toString()
        thumbScroller.layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this@PlayerActivity, videoUri)
        } catch (_: Exception) {
            val data = Intent().apply {
                putExtra("key", "needRefresh")
            }
            setResult(RESULT_OK, data)
            finish()
            return
        }
        thumbScroller.itemAnimator = null
        thumbScroller.layoutParams.width = 0
        videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
        if (videoDuration > 100000L){
            videoTimeSyncGap = 500L
        }
        if (PREFS_S_UseLongScroller){
            eachPicWidth = (47 * displayMetrics.density).toInt()
            eachPicDuration = (((videoDuration/1000) / 100.0).toInt())*500
            picNumber = min((videoDuration/eachPicDuration), 500)
        }else{
            syncScrollRunnableGap = ((videoDuration/1000)*(1000.0/3600)).toLong()
            if (videoDuration/1000 > maxPicNumber){
                eachPicWidth = (47 * displayMetrics.density).toInt()
                eachPicDuration = (videoDuration.div(100)*100) / maxPicNumber
                picNumber = maxPicNumber
            }else{
                eachPicWidth = (47 * displayMetrics.density).toInt()
                picNumber = (videoDuration/1000) + 1
                eachPicDuration = (videoDuration.div(100)*100)/picNumber
            }
        }
        retriever.release()


        //gestureDetectorScroller -onSingleTap -onDown
        val gestureDetectorScroller = GestureDetector(this, object : SimpleOnGestureListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                singleTap = true
                if (!PREFS_RC_TapScrollEnabled) {
                    if (PREFS_RC_LinkScrollEnabled){
                        notice("未开启单击跳转,如需跳转请先开启,或关闭链接滚动", 1000)
                        if (wasPlaying) {
                            wasPlaying = false
                            player.play()
                        }
                        return false
                    }
                }
                //根据百分比计算具体跳转时间点
                val totalContentWidth = thumbScroller.computeHorizontalScrollRange()
                val scrolled = thumbScroller.computeHorizontalScrollOffset()
                val leftPadding = thumbScroller.paddingLeft
                val xInContent = e.x + scrolled - leftPadding
                if (totalContentWidth <= 0) return false
                val percent = xInContent / totalContentWidth
                val seekToMs = (percent * player.duration).toLong().coerceIn(0, player.duration)
                if (seekToMs <= 0) {
                    return false
                }
                if (seekToMs >= player.duration){
                    return false
                }
                //发送跳转命令
                player.seekTo(seekToMs)
                notice("跳转至${formatTime2(seekToMs)}",1000)
                lifecycleScope.launch {
                    startScrollerSync()
                    delay(20)
                    if (!PREFS_RC_LinkScrollEnabled){
                        stopScrollerSync()
                    }
                }

                if (wasPlaying){
                    wasPlaying = false
                    player.play()
                }
                return true
            }
            override fun onDown(e: MotionEvent): Boolean {
                seekOnceUp = false
                if (!PREFS_RC_LinkScrollEnabled) return false
                //播放状态记录
                if (!smartScrollRunnableRunning && !seekRunnableRunning && !scrolling){
                    wasPlaying = false
                    if (player.isPlaying){
                        player.pause()
                        wasPlaying = true
                    }
                }

                if (playEnd){
                    playEnd = false
                }

                if (!PREFS_RC_TapScrollEnabled) return false
                return false
            }
        })
        //RecyclerView-事件监听器 (中间层)
        thumbScroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                scrollerTouching = true
                if (e.action == MotionEvent.ACTION_UP){
                    scrollerTouching = false
                    scrollerStoppingTapUp()
                }
                gestureDetectorScroller.onTouchEvent(e)
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                gestureDetectorScroller.onTouchEvent(e)
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        //RecyclerView-事件监听器 -onScrollStateChanged -onScrolled
        thumbScroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    dragging = true
                    scrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_SETTLING){
                    dragging = false
                    scrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    dragging = false
                    scrolling = false
                    return
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!scrolling && !dragging) {
                    return
                }  //此状态说明进度条是在随视频滚动,用户没有操作

                if (scrollerTouching){
                    if (dx == 1 || dx == -1){
                        player.setSeekParameters(SeekParameters.EXACT)
                    }else{
                        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                }else{
                    player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                }
                if (PREFS_RC_LinkScrollEnabled){
                    val percentScroll = recyclerView.computeHorizontalScrollOffset().toFloat() / thumbScroller.computeHorizontalScrollRange()
                    videoTimeTo = (percentScroll * player.duration).toLong()
                    currentTime = videoTimeTo
                    if (videoTimeSyncGap >= 500L){
                        tvCurrentTime.text = formatTime1(videoTimeTo)
                    }else{
                        tvCurrentTime.text = formatTime(videoTimeTo)
                    }
                } else {return} //时间戳跟随进度条
                stopVideoTimeSync()
                stopScrollerSync()
                if (dx > 0){
                    if (PREFS_RC_AlwaysSeekEnabled){
                        backSeek = false
                        startVideoSeek()
                    } else {
                        stopVideoSeek()
                        startVideoSmartScroll()
                    }
                } else if (dx < 0){
                    backSeek = true
                    startVideoSeek()
                }
            }
        })



        //固定控件初始化：退出按钮
        val buttonExit=findViewById<View>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            player.playWhenReady = false
            finish()
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.noticeCard)
        noticeCard.setOnClickListener {
            noticeCard.visibility = View.GONE
        }
        //按钮：循环播放
        val buttonLoopPlay = findViewById<FrameLayout>(R.id.buttonActualLoopPlay)
        val buttonLoopPlayMaterial = findViewById<MaterialButton>(R.id.buttonMaterialLoopPlay)
        if (PREFS_RC_LoopPlay){
            player.repeatMode = Player.REPEAT_MODE_ONE
            buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            player.repeatMode == Player.REPEAT_MODE_OFF
            buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLoopPlay.setOnClickListener {
            if (player.repeatMode == Player.REPEAT_MODE_OFF){
                player.repeatMode = Player.REPEAT_MODE_ONE
                prefs.edit { putBoolean("PREFS_LoopPlay", true) }
                buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单集循环",1000)
                if (playEnd){
                    playEnd = false
                    player.seekTo(0)
                    player.play()
                }
            }else{
                player.repeatMode = Player.REPEAT_MODE_OFF
                prefs.edit { putBoolean("PREFS_LoopPlay", false) }
                buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单集循环",1000)
            }
        }
        //按钮：返回视频开头
        val buttonBackToStart = findViewById<FrameLayout>(R.id.buttonActualBackToStart)
        buttonBackToStart.setOnClickListener {
            stopVideoSmartScroll()
            stopVideoSeek()
            thumbScroller.stopScroll()
            if (PREFS_RC_LinkScrollEnabled) startScrollerSync()
            player.seekTo(0)
            playVideo()
            notice("返回视频起始",1000)
        }
        //按钮：暂停视频
        val buttonPause = findViewById<FrameLayout>(R.id.buttonPause)
        buttonPause.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
                stopScrollerSync()
                notice("暂停",1000)
                buttonRefresh()
            } else {
                onDown = false
                fling = false
                notice("继续播放",1000)
                lifecycleScope.launch {
                    thumbScroller.stopScroll()
                    delay(20)
                    if (playEnd){
                        playEnd = false
                        player.seekTo(0)
                        notice("视频已结束,开始重播",1000)
                        buttonRefresh()
                        return@launch
                    }else{
                        playVideo()
                        return@launch
                    }
                }
            }
        }
        //按钮：后台播放
        val buttonBackgroundPlay = findViewById<FrameLayout>(R.id.buttonActualBackgroundPlay)
        val buttonBackgroundPlayMaterial = findViewById<MaterialButton>(R.id.buttonMaterialBackgroundPlay)
        if (PREFS_RC_BackgroundPlay){
            buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonBackgroundPlay.setOnClickListener {
            if (!prefs.getBoolean("PREFS_BackgroundPlay",false)){
                PREFS_RC_BackgroundPlay = true
                prefs.edit { putBoolean("PREFS_BackgroundPlay", true) }
                notice("已开启后台播放",1000)
                buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
            }
            else{
                PREFS_RC_BackgroundPlay = false
                prefs.edit { putBoolean("PREFS_BackgroundPlay", false) }
                notice("已关闭后台播放",1000)
                buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
            }
        }
        //按钮：单击跳转
        val buttonTap = findViewById<FrameLayout>(R.id.buttonActualTap)
        val buttonTapMaterial = findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (PREFS_RC_TapScrollEnabled){
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonTap.setOnClickListener {
            if (!prefs.getBoolean("PREFS_TapScrolling",false)){
                PREFS_RC_TapScrollEnabled = true
                prefs.edit { putBoolean("PREFS_TapScrolling", true) }
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单击跳转",1000)
            }
            else{
                PREFS_RC_TapScrollEnabled = false
                prefs.edit { putBoolean("PREFS_TapScrolling", false) }
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单击跳转",1000)
            }
        }
        //按钮：链接滚动条与视频进度
        val buttonLink = findViewById<FrameLayout>(R.id.buttonActualLink)
        val buttonLinkMaterial = findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (PREFS_RC_LinkScrollEnabled){
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLink.setOnClickListener {
            if (!prefs.getBoolean("PREFS_LinkScrolling",false)){
                PREFS_RC_LinkScrollEnabled = true
                prefs.edit { putBoolean("PREFS_LinkScrolling", true) }
                notice("已将进度条与视频进度同步",1000)
                isSeekReady = true
                thumbScroller.stopScroll()
                startScrollerSync()
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                stopVideoSeek()
            }
            else{
                PREFS_RC_LinkScrollEnabled = false
                prefs.edit { putBoolean("PREFS_LinkScrolling", false) }
                stopScrollerSync()
                thumbScroller.stopScroll()
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭链接滚动条与视频进度",2500)
            }
        }
        //按钮：AlwaysSeek
        val buttonAlwaysSeek = findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val buttonAlwaysMaterial = findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (PREFS_RC_AlwaysSeekEnabled){
            buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonAlwaysSeek.setOnClickListener {
            if (!prefs.getBoolean("PREFS_AlwaysSeek",false)){
                PREFS_RC_AlwaysSeekEnabled = true
                prefs.edit { putBoolean("PREFS_AlwaysSeek", true) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启AlwaysSeek",1000)
            }
            else{
                PREFS_RC_AlwaysSeekEnabled = false
                prefs.edit { putBoolean("PREFS_AlwaysSeek", false) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭AlwaysSeek",3000)
            }
        }
        //播放区域点击事件
        var longPress = false
        var touchLeft = false
        var touchRight = false
        var scrollDistance = 0
        val gestureDetectorPlayArea = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (player.isPlaying) {
                    pauseVideo()
                    stopScrollerSync()
                    notice("暂停播放", 1000)
                    buttonRefresh()
                } else {
                    if (playEnd) {
                        playEnd = false
                        player.seekTo(0)
                        playVideo()
                        notice("视频已结束,开始重播", 1000)
                    } else {
                        playVideo()
                        startScrollerSync()
                        notice("继续播放", 1000)
                        buttonRefresh()
                    }
                }
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                changeBackgroundColor()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                if (!player.isPlaying) {
                    return
                }
                player.setPlaybackSpeed(2.0f)
                notice("倍速播放中", 114514)
                longPress = true
                super.onLongPress(e)
            }
        })
        val playArea = findViewById<View>(R.id.playerView)
        playArea.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val y = event.y
                if (y < 250){
                    return@setOnTouchListener false
                }
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val x = event.x
                if (x < screenWidth / 2) {
                    touchLeft = true
                } else {
                    touchRight = true
                }
            }
            if (event.action == MotionEvent.ACTION_UP) {
                scrollDistance = 0
                touchLeft = false
                touchRight = false
                if (longPress) {
                    longPress = false
                    player.setPlaybackSpeed(1.0f)
                    val noticeCard = findViewById<CardView>(R.id.noticeCard)
                    noticeCard.visibility = View.GONE
                }
            }
            gestureDetectorPlayArea.onTouchEvent(event)
        }




        //绑定Adapter
        lifecycleScope.launch(Dispatchers.IO) {
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()

            if (eachPicDuration > 1000){
                PREFS_RC_GenerateThumbSYNC = true
            }

            withContext(Dispatchers.Main) {
                thumbScroller.adapter = PlayerScrollerAdapter(this@PlayerActivity,
                    absolutePath,playerScrollerViewModel.thumbItems,eachPicWidth,picNumber,eachPicDuration,PREFS_RC_GenerateThumbSYNC)
            }

            if(PREFS_RC_LinkScrollEnabled){ startScrollerSync() }
            delay(100)
            startVideoTimeSync()
        }
        //播放失败检查
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            if (!playerReadyFromFirst){
                withContext(Dispatchers.Main){
                    val playerError = findViewById<LinearLayout>(R.id.playerError)
                    playerError.visibility = View.VISIBLE
                }
            }
        }
        //系统手势监听：返回键重写
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MediaCodec.createDecoderByType("video/avc").release()
                player.release()
                finish()
            }
        })
    } //onCreate END


    //Runnable:根据视频时间更新进度条位置
    private val syncScrollTaskHandler = Handler(Looper.getMainLooper())
    private val syncScrollTask = object : Runnable {
        override fun run() {
            syncScrollRunnableRunning = true
            if (eachPicDuration == 0){
                return
            }
            scrollParam1 = (player.currentPosition / eachPicDuration).toInt()
            scrollParam2 = ((player.currentPosition - scrollParam1*eachPicDuration)*eachPicWidth/eachPicDuration).toInt()
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val lm = recyclerView.layoutManager as LinearLayoutManager
            if (playEnd && !player.isPlaying){
                playEnd = false
                scrollParam1 = scrollParam1 - 1
                scrollParam2 = 150
                lm.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
                syncScrollRunnableRunning = false
            }else{
                lm.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
                syncScrollTaskHandler.postDelayed(this, syncScrollRunnableGap)
            }
        }
    }
    private fun startScrollerSync() {
        if (syncScrollRunnableRunning){
            return
        }
        syncScrollTaskHandler.post(syncScrollTask)
    }
    private fun stopScrollerSync() {
        syncScrollRunnableRunning = false
        syncScrollTaskHandler.removeCallbacks(syncScrollTask)
    }
    //Runnable:根据视频时间更新时间戳
    private val videoTimeSyncHandler = Handler(Looper.getMainLooper())
    private var videoTimeSync = object : Runnable{
        override fun run() {
            val currentPosition = player.currentPosition
            if (videoTimeSyncGap >= 500L){
                tvCurrentTime.text = formatTime1(currentPosition)
            }else{
                tvCurrentTime.text = formatTime(currentPosition)
            }
            videoTimeSyncHandler.postDelayed(this, videoTimeSyncGap)
        }
    }
    private fun startVideoTimeSync() {
        videoTimeSyncHandler.post(videoTimeSync)
    }
    private fun stopVideoTimeSync() {
        videoTimeSyncHandler.removeCallbacks(videoTimeSync)
    }
    //Runnable:视频倍速滚动
    private val videoSmartScrollHandler = Handler(Looper.getMainLooper())
    private var videoSmartScroll = object : Runnable{
        override fun run() {
            smartScrollRunnableRunning = true
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            var delayGap = if (dragging){ 30L } else{ 30L }
            val videoPosition = player.currentPosition
            val scrollerPosition =  player.duration * (recyclerView.computeHorizontalScrollOffset().toFloat()/recyclerView.computeHorizontalScrollRange())
            player.volume = 0f
            if (scrollerPosition < videoPosition +100) {
                if (scrollerTouching){
                    player.pause()
                }else{
                    if (wasPlaying){
                        smartScrollRunnableRunning = false
                        playVideo()
                    } else {
                        smartScrollRunnableRunning = false
                        player.pause()
                    }
                }
            }else{
                val positionGap = scrollerPosition - videoPosition
                if (positionGap > forceSeekGap){
                    player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    player.seekTo(scrollerPosition.toLong())
                }
                var speed5 = (((positionGap / 100).toInt()) /10.0).toFloat()
                if (speed5 > lastPlaySpeed){
                    speed5 = speed5 + 0.1f
                }else if(speed5 < lastPlaySpeed){
                    speed5 = speed5 - 0.1f
                }

                if (wasPlaying && !scrollerTouching){
                    if (speed5 <= 1.0){
                        speed5 = 1.0f
                    }
                }

                if (speed5 > 0f){
                    player.setPlaybackSpeed(speed5)
                }else{
                    player.play()
                }
                videoSmartScrollHandler.postDelayed(this,delayGap)
            }
        }
    }
    private fun startVideoSmartScroll() {
        stopScrollerSync()
        stopVideoTimeSync()
        player.volume = 0f
        player.play()
        if (singleTap){
            singleTap = false
            return
        }
        videoSmartScrollHandler.post(videoSmartScroll)
    }
    private fun stopVideoSmartScroll() {
        smartScrollRunnableRunning = false
        videoSmartScrollHandler.removeCallbacks(videoSmartScroll)
    }
    //Runnable:视频Seek滚动
    private val videoSeekHandler = Handler(Looper.getMainLooper())
    private var videoSeek = object : Runnable{
        override fun run() {
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val totalWidth = recyclerView.computeHorizontalScrollRange()
            val offset     = recyclerView.computeHorizontalScrollOffset()
            val percent    = offset.toFloat() / totalWidth
            val seekToMs   = (percent * player.duration).toLong()
            seekRunnableRunning = true
            if (backSeek){
                if (seekToMs < player.currentPosition){
                    player.pause()
                    if (seekToMs < 200){
                        readyFromSeek = true
                        player.seekTo(0)
                    }else if (seekToMs > player.duration - 300){
                        readyFromSeek = true
                        player.seekTo(player.duration - 300)
                    }else{
                        if (isSeekReady){
                            isSeekReady = false
                            readyFromSeek = true
                            player.seekTo(seekToMs)
                        }
                    }
                }
            }else{
                player.pause()
                if (seekToMs < 200){
                    readyFromSeek = true
                    player.seekTo(0)
                }else if (seekToMs > player.duration - 300){
                    readyFromSeek = true
                    player.seekTo(player.duration - 300)
                }else{
                    if (isSeekReady){
                        isSeekReady = false
                        readyFromSeek = true
                        player.seekTo(seekToMs)
                    }
                }
            }
            if (scrollerTouching || scrolling) {
                videoSeekHandler.postDelayed(this, 100)
            }else{
                seekRunnableRunning = false
            }

        }
    }
    private fun startVideoSeek() {
        if (seekRunnableRunning) return
        videoSeekHandler.post(videoSeek)
    }
    private fun stopVideoSeek() {
        seekRunnableRunning = false
        videoSeekHandler.removeCallbacks(videoSeek)
    }
    //Job:显示通知
    private var showNoticeJob: Job? = null
    private fun showNoticeJob(text: String, duration: Long) {
        showNoticeJob?.cancel()
        showNoticeJob = lifecycleScope.launch {
            val notice = findViewById<TextView>(R.id.notice)
            val noticeCard = findViewById<CardView>(R.id.noticeCard)
            noticeCard.visibility = View.VISIBLE
            notice.text = text
            delay(duration)
            noticeCard.visibility = View.GONE
        }
    }
    private var showNoticeJobLong: Job? = null
    private fun showNoticeJobLong(text: String) {
        showNoticeJobLong?.cancel()
        showNoticeJobLong = lifecycleScope.launch {
            val notice = findViewById<TextView>(R.id.notice)
            val noticeCard = findViewById<CardView>(R.id.noticeCard)
            noticeCard.visibility = View.VISIBLE
            notice.text = text
        }
    }
    private fun notice(text: String, duration: Long) {
        if (duration > 114513){
            showNoticeJobLong(text)
        }
        showNoticeJob(text, duration)
    }


    //动画播完才开始准备播放器
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        player.prepare()
        if (wasPlaying){
            player.playWhenReady = true
            wasPlaying = false
        }else{
            wasPlaying = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("SAVEDINSTANCE_WasPlaying", SAVEDINSTANCE_WasPlaying)
        outState.putString("SAVEDINSTANCE_Uri", videoUri.toString())
        outState.putLong("SAVEDINSTANCE_CurrentTime", player.currentPosition)
        //outState.putBoolean("SAVEDINSTANCE_FirstEntry", firstEntry)
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying){
            wasPlaying = true
            SAVEDINSTANCE_WasPlaying = true
        }
        if (!PREFS_RC_BackgroundPlay) {
            player.pause()
        }else{
            playerSelectSoundTrack()
            startForegroundServices()
        }
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onStop() {
        super.onStop()
        stopVideoTimeSync()
        stopScrollerSync()
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onResume() {
        super.onResume()
        if (PREFS_RC_LinkScrollEnabled && wasPlaying) startScrollerSync()
        if (wasPlaying) startVideoTimeSync()
        if (PREFS_RC_BackgroundPlay) {
            playerRecoveryAllTrack()
            stopForegroundServices()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVideoSeek()
        stopVideoSmartScroll()
        stopVideoTimeSync()
        stopScrollerSync()
        stopForegroundServices()
        if (!cannotOpen){
            player.release()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                false
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        intent = newIntent
        if (intent?.action != null){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            finish()
            startActivity(intent)
        }
    }

    //Functions
    //点击时隐藏控件并设为黑色背景
    private fun changeBackgroundColor(){
        val bottomCard = findViewById<View>(R.id.bottomCardContainer)
        val mediumActions = findViewById<ConstraintLayout>(R.id.MediumActionsContainer)
        val playerView = findViewById<View>(R.id.playerView)
        val buttonExit = findViewById<ImageButton>(R.id.buttonExit)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (widgetsShowing){
                widgetsShowing = false
                bottomCard.visibility = View.GONE
                mediumActions.visibility = View.GONE
                buttonExit.visibility = View.GONE
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO && !PREFS_S_UseBlackScreenInLandscape){
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                }
                stopScrollerSync()
                stopVideoTimeSync()
            }else{
                if(PREFS_RC_LinkScrollEnabled) { startScrollerSync() }
                startVideoTimeSync()
                widgetsShowing = true
                bottomCard.visibility = View.VISIBLE
                mediumActions.visibility = View.VISIBLE
                buttonExit.visibility = View.VISIBLE
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO && !PREFS_S_UseBlackScreenInLandscape){
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
                }
            }
        }else{
            val mediumActions2 = findViewById<ConstraintLayout>(R.id.MediumActionsContainer2)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            val root = findViewById<ConstraintLayout>(R.id.root)
            if (widgetsShowing){
                widgetsShowing = false
                bottomCard.visibility = View.GONE
                mediumActions.visibility = View.GONE
                buttonExit.visibility = View.GONE
                mediumActions2.visibility = View.GONE
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                    toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                    root.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                }
                stopScrollerSync()
                stopVideoTimeSync()
            }else{
                if(PREFS_RC_LinkScrollEnabled) { startScrollerSync() }
                startVideoTimeSync()
                widgetsShowing = true
                bottomCard.visibility = View.VISIBLE
                mediumActions.visibility = View.VISIBLE
                buttonExit.visibility = View.VISIBLE
                mediumActions2.visibility = View.VISIBLE
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                    toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadBackground))
                    root.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
                }
            }
        }
    }

    private fun setBlackScreenInLandscape(){
        val playerView = findViewById<View>(R.id.playerView)
        val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
        val scrollerContainer = findViewById<View>(R.id.scrollerContainer)
        playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        scrollerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.BlackGrey))
        recyclerView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))
    }

    //开启后台播放服务
    private fun startForegroundServices(){
        startService(Intent(this, PlayerBackgroundServices::class.java))
    }
    //关闭后台播放服务
    private fun stopForegroundServices(){
        stopService(Intent(this, PlayerBackgroundServices::class.java))
    }
    //后台播放只播音轨
    private fun playerSelectSoundTrack(){
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setMaxVideoSize(0, 0)
            .build()
    }
    //回到前台恢复所有轨道
    private fun playerRecoveryAllTrack(){
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .build()
    }

    private var seekOnceUp = false
    private fun seekOnce(){
        seekOnceUp = true
        readyFromSeek = false
        val thumbScroller = findViewById<RecyclerView>(R.id.rvThumbnails)
        thumbScroller.stopScroll()
        stopVideoSeek()
        val totalWidthOnce = thumbScroller.computeHorizontalScrollRange()
        val offsetOnce     = thumbScroller.computeHorizontalScrollOffset()
        val percentOnce    = offsetOnce.toFloat() / totalWidthOnce
        val seekToMsOnce   = (percentOnce * player.duration).toLong()
        fromSeekOnce = true
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        player.seekTo(seekToMsOnce)
    }

    private fun scrollerStoppingTapUp(){
        if (!PREFS_RC_LinkScrollEnabled) return
        if (isSeekReady){
            if (wasPlaying){
                playVideo()
            }
        }
    }

    private fun playerReady(){
        if (firstEntry) {
            firstEntry = false
            playVideo()
            val playerError = findViewById<LinearLayout>(R.id.playerError)
            playerError.visibility = View.GONE
            val cover = findViewById<View>(R.id.cover)
            cover.animate().alpha(0f).setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { cover.visibility = View.GONE }
                .start()
        } //首次打开显示遮罩动画
        if (PREFS_RC_LoopPlay) {
            PREFS_RC_LoopPlay = false
            playVideo()
            return
        } //循环播放操作
        if (readyFromSeek){
            readyFromSeek = false
            isSeekReady = true
            if (!scrolling){
                if (!seekOnceUp){ seekOnce() } //直接触发
                return
            } else {
                lifecycleScope.launch {
                    delay(100)
                    if (!scrolling){
                        if (!seekOnceUp){ seekOnce() }  //第一次保底
                        return@launch
                    }else{
                        delay(200)
                        if (!scrolling){
                            if (!seekOnceUp){ seekOnce() } //第二次保底
                            return@launch
                        }else{
                            delay(200)
                            if (!scrolling){
                                if (!seekOnceUp){ seekOnce() } //第三次保底
                                return@launch
                            }
                        }
                    }
                }
            }
            return
        }
        if (fromSeekOnce){
            fromSeekOnce = false
            isSeekReady = true
            if (wasPlaying) {
                playVideo()
            }
            return
        }
        if (SAVEDINSTANCE_SeekFromSave){
            SAVEDINSTANCE_SeekFromSave = false
            if (SAVEDINSTANCE_WasPlaying){
                SAVEDINSTANCE_WasPlaying = false
                playVideo()
            }else{
                player.pause()
            }
            return
        }
    }

    private fun playerEnd(){
        notice("视频结束",1000)
        player.pause()
        playEnd = true
        stopVideoTimeSync()
        stopScrollerSync()
        if (PREFS_S_ExitWhenEnd){
            finish()
        }
        if (PREFS_RC_LoopPlay){
            player.seekTo(0)
        }
    }

    private fun pauseVideo(){
        player.pause()
        stopVideoTimeSync()
        stopScrollerSync()
    }

    private fun playVideo(){
        player.volume = currentVolume.toFloat()
        player.setPlaybackSpeed(1f)
        player.play()
        if (PREFS_RC_LinkScrollEnabled && !scrollerTouching && !scrolling){ startScrollerSync() }
        lifecycleScope.launch {
            delay(100)
            startVideoTimeSync()
        }
    }

    //检查通知权限
    private fun checkNotificationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean -> }
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val channelId = "BackgroundPlay"
            val channelName = "后台播放"
            val channelDescription = "用于在后台播放音频"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            player.pause()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            player.pause()
        }
    }

    //真全屏需添加以下元数据到活动清单文件
    /*
    <meta-data
    android:name="android.notch_support"
    android:value="true"/>
    */
    private fun statusBarsSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
                WindowInsetsCompat.CONSUMED
            }
            window.decorView.post {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
        var absolutePath: String? = null
        // 检查URI
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    absolutePath = it.getString(columnIndex) //绝对路径
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            absolutePath = uri.path //文件URI直接获取路径
        }
        // 验证路径是否存在
        if (absolutePath != null && File(absolutePath).exists()) {
            return absolutePath
        }
        return null
    }

    private fun buttonRefresh(){
        val PauseImage = findViewById<ImageView>(R.id.PauseImage)
        val ContinueImage = findViewById<ImageView>(R.id.ContinueImage)
        if (player.isPlaying){
            PauseImage.visibility = View.VISIBLE
            ContinueImage.visibility = View.GONE
        }
        else{
            PauseImage.visibility = View.GONE
            ContinueImage.visibility = View.VISIBLE
        }
    }

    private fun preCheck(){
        //兼容性检查
        isCompatibleDevice = isCompatibleDevice()
        //音量
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        //通知权限
        checkNotificationPermission()
    }

    //格式化时间显示
    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        //val hours = totalSeconds / 3600      //此类视频基本不可能超过一小时
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = milliseconds % 1000 // 提取毫秒部分
        return String.format("%02d:%02d.%03d",  minutes, seconds, millis)
    }
    @SuppressLint("DefaultLocale")
    private fun formatTime1(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours == 0L){
            String.format("%02d:%02d",  minutes, seconds)
        }else{
            String.format("%02d:%02d:%02d",  hours, minutes, seconds)
        }
    }
    private fun formatTime2(raw: Long): String {
        val cent  = raw % 1000
        val totalSec = raw / 1000
        val min  = totalSec / 60
        val sec  = totalSec % 60
        return "%02d:%02d.%03d".format(min, sec, cent)
    }

}