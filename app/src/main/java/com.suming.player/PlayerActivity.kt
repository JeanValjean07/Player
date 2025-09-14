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
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
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


@UnstableApi
class PlayerActivity: AppCompatActivity(){

    //时间戳信息显示位
    private lateinit var tvCurrentTime: TextView
    //视频信息预读
    private var absolutePath = ""       //视频绝对路径,多个成员要读取
    //播放器状态标识
    private lateinit var player: ExoPlayer
    private lateinit var videoUri: Uri
    private var wasPlaying = true     //上一次暂停时是否在播放
    //音量配置参数
    private var currentVolume = 0
    //缩略图绘制参数
    private var maxPicNumber = 20         //缩略图最大数量(写死)
    private var eachPicWidth = 0          //单张缩略图最大宽度(现场计算),高度45dp布局写死
    private var picNumber = 0             //缩略图数量(现场计算)
    private var eachPicDuration: Int = 0  //单张缩略图对应时长(现场计算)
    //点击和滑动状态标识
    private var onDown = false
    private var dragging = false
    private var fling =false
    private var singleTap = false
    private var scrolling = false
    //设置
    private var tapScrollEnabled = false
    private var linkScrollEnabled = false
    private var alwaysSeekEnabled = false
    private var BackgroundPlay = false
    //Seek程序用到的参数
    private var isSeekReady = false

    //倍速程序用到的参数
    private var currentTime = 0L
    private var lastPlaySpeed = 0f
    //进度条随视频进度滚动程序用到的参数
    private var syncScrollTaskRunning = false
    //旧机型标识
    private var isCompatibleDevice = false
    private var cannotOpen = false
    //点击隐藏
    private var widgetsShowing = true
    private var firstEntry = true

    private var lastSeekExecuted = false

    private var loopPlay = false
    private var SETTING_loopPlay = false
    private var exitWhenEnd = 0
    private var SETTING_exitWhenEnd = false

    private var videoTimeSyncGap = 50L

    private var videoDuration = 0

    private lateinit var trackSelector: DefaultTrackSelector

    //消耗变量
    private var playerReady = false
    private var playerReadyFromFirst = false
    private var playEnd = false
    private var readyFromSeek = false

    private var scrollerTouching = false

    private var backSeek = false

    private var videoTimeTo = 0L

    private var seekRunnableRunning = false


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

        //横屏:完全隐藏状态栏,竖屏:显示到状态栏下并动态更改退出按钮位置
        fun orientationDecide(){
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                hideStatusBar()
            }else{
                val buttonExit=findViewById<View>(R.id.buttonExit)
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
                    val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    (buttonExit.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (statusBarHeight + 8)
                    insets
                }
                buttonExit.alpha = 0f
                buttonExit.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(LinearInterpolator())
                    .start()
            }
        }
        orientationDecide()

        //设置项读取,检查和预置
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        var generateThumbSYNC: Int
        var seekSYNC: Int
        if(!prefs.contains("generateThumbSYNC")){
            prefs.edit { putInt("generateThumbSYNC", 1) }
            generateThumbSYNC = prefs.getInt("generateThumbSYNC", 1)
        }else{
            generateThumbSYNC = prefs.getInt("generateThumbSYNC", 1)
        }
        if(!prefs.contains("seekSYNC")){
            prefs.edit { putInt("seekSYNC", 1) }
            seekSYNC = prefs.getInt("seekSYNC", 1)
        }else{
            seekSYNC = prefs.getInt("seekSYNC", 1)
        }
        if (!prefs.contains("tapScrolling")){
            prefs.edit { putBoolean("tapScrolling", false) }
            tapScrollEnabled = prefs.getBoolean("tapScrolling", false)
        }else{
            tapScrollEnabled = prefs.getBoolean("tapScrolling", false)
        }
        if (!prefs.contains("linkScrolling")){
            prefs.edit { putBoolean("linkScrolling", true) }
            linkScrollEnabled = prefs.getBoolean("linkScrolling", false)
        } else {
            linkScrollEnabled = prefs.getBoolean("linkScrolling", false) }
        if (!prefs.contains("alwaysSeek")){
            prefs.edit { putBoolean("alwaysSeek", false) }
            alwaysSeekEnabled = prefs.getBoolean("alwaysSeek", false)
        } else{
            alwaysSeekEnabled = prefs.getBoolean("alwaysSeek", false) }
        if (!prefs.contains("backgroundPlay")){
            prefs.edit { putBoolean("backgroundPlay", false) }
            BackgroundPlay = prefs.getBoolean("backgroundPlay", false)
        } else{
            BackgroundPlay = prefs.getBoolean("backgroundPlay", false) }
        if (!prefs.contains("loopPlay")){
            prefs.edit { putBoolean("loopPlay", false) }
            loopPlay = prefs.getBoolean("loopPlay", false)
        } else{
            loopPlay = prefs.getBoolean("loopPlay", false) }
        if (!prefs.contains("exitWhenEnd")){
            prefs.edit { putInt("exitWhenEnd", 1) }
            exitWhenEnd = prefs.getInt("exitWhenEnd", 1)
            SETTING_exitWhenEnd = when (exitWhenEnd) {
                1 -> true
                else -> false
            }
        } else{
            exitWhenEnd = prefs.getInt("exitWhenEnd", 1)
            SETTING_exitWhenEnd = when (exitWhenEnd) {
                1 -> true
                else -> false
            }
        }

        //内部广播接收
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val filter = IntentFilter("114514")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("key")
                if (data == "PLAYER_PLAY"){
                    player.play()
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
                SETTING_exitWhenEnd = true
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                VideoItem(0, uri, "" , 0)
            }
            Intent.ACTION_VIEW -> {
                SETTING_exitWhenEnd = true
                val uri = intent.data ?: return finish()
                VideoItem(0, uri, "", 0)
            }
            else -> {
                SETTING_exitWhenEnd = false
                IntentCompat.getParcelableExtra(intent, "video", VideoItem::class.java)}
        }
        if (videoItem == null) {
            Toast.makeText(this, "无法打开这条视频", Toast.LENGTH_SHORT).show()
            cannotOpen = true
            finish()
            return
        }

        //指定videoUri并在重载中修改
        videoUri = videoItem.uri
        if (savedInstanceState != null) {
            val uri = savedInstanceState.getString("uri")?.toUri()
            val videoUriRec = uri!!
            videoUri = videoUriRec
            wasPlaying = savedInstanceState.getBoolean("wasPlaying")
            currentTime = savedInstanceState.getLong("currentTime")
            firstEntry = savedInstanceState.getBoolean("firstEntry")
            if (!firstEntry){
                val cover = findViewById<View>(R.id.cover)
                cover.visibility = View.GONE
            }
        }


        //初始化ExoPlayer
        trackSelector = DefaultTrackSelector(this)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        if (seekSYNC == 1){
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
        }else{
            player = ExoPlayer.Builder(this)
                .setSeekParameters(SeekParameters.EXACT)
                .setWakeMode(WAKE_MODE_NETWORK)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(videoUri))
                    prepare()
                    playWhenReady = false
                }
        }
        playerView.player = player


        if (savedInstanceState != null) {
            player.seekTo(currentTime)
        }
        if (wasPlaying) {
            playVideo()
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
        if (videoDuration/1000 > maxPicNumber){  //用47dp计算,做成长方形一点的
            eachPicWidth = (47 * displayMetrics.density).toInt()
            eachPicDuration = (videoDuration.div(100)*100) / maxPicNumber
            picNumber = maxPicNumber
        }else{
            eachPicWidth = (47 * displayMetrics.density).toInt()
            picNumber = videoDuration/1000+1
            eachPicDuration = (videoDuration.div(100)*100)/picNumber
        }
        retriever.release()


        //gestureDetectorScroller -onSingleTap -onDown
        val gestureDetectorScroller = GestureDetector(this, object : SimpleOnGestureListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                singleTap = true
                if (!tapScrollEnabled) {
                    if (linkScrollEnabled){
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
                    if (!linkScrollEnabled){
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
                if (!linkScrollEnabled) return false
                //播放状态记录
                if (!videoSmartScrollRunning){
                    wasPlaying = false
                    if (player.isPlaying){
                        player.pause()
                        wasPlaying = true
                    }
                }
                stopVideoSeek()
                if (!linkScrollEnabled) return false
                if (!tapScrollEnabled) return false
                stopScrollerSync()

                pauseVideo()


                buttonRefresh()
                return false
            }
        })
        //RecyclerView-事件监听器 (中间层)
        thumbScroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                scrollerTouching = true
                if (e.action == MotionEvent.ACTION_UP){
                    scrollerTouching = false
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
                if (linkScrollEnabled){
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
                    if (alwaysSeekEnabled){
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
        if (player.repeatMode == Player.REPEAT_MODE_ONE){
            buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else if(player.repeatMode == Player.REPEAT_MODE_OFF){
            buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLoopPlay.setOnClickListener {
            if (player.repeatMode == Player.REPEAT_MODE_OFF){
                player.repeatMode = Player.REPEAT_MODE_ONE
                buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单集循环",1000)
                if (playEnd){
                    playEnd = false
                    player.seekTo(0)
                    player.play()
                }
            }else{
                player.repeatMode = Player.REPEAT_MODE_OFF
                buttonLoopPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单集循环",1000)
            }
        }
        //按钮：返回视频开头
        val buttonBackToStart = findViewById<FrameLayout>(R.id.buttonActualBackToStart)
        buttonBackToStart.setOnClickListener {
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
        if (BackgroundPlay){
            buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonBackgroundPlay.setOnClickListener {
            if (!prefs.getBoolean("backgroundPlay",false)){
                BackgroundPlay = true
                prefs.edit { putBoolean("backgroundPlay", true) }
                notice("已开启后台播放",1000)
                buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
            }
            else{
                BackgroundPlay = false
                prefs.edit { putBoolean("backgroundPlay", false) }
                notice("已关闭后台播放",1000)
                buttonBackgroundPlayMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
            }
        }
        //按钮：单击跳转
        val buttonTap = findViewById<FrameLayout>(R.id.buttonActualTap)
        val buttonTapMaterial = findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (tapScrollEnabled){
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonTap.setOnClickListener {
            if (!prefs.getBoolean("tapScrolling",false)){
                tapScrollEnabled = true
                prefs.edit { putBoolean("tapScrolling", true) }
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单击跳转",1000)
            }
            else{
                tapScrollEnabled = false
                prefs.edit { putBoolean("tapScrolling", false) }
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单击跳转",1000)
            }
        }
        //按钮：链接滚动条与视频进度
        val buttonLink = findViewById<FrameLayout>(R.id.buttonActualLink)
        val buttonLinkMaterial = findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (linkScrollEnabled){
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLink.setOnClickListener {
            if (!prefs.getBoolean("linkScrolling",false)){
                linkScrollEnabled = true
                prefs.edit { putBoolean("linkScrolling", true) }
                notice("已将进度条与视频进度同步",1000)
                thumbScroller.stopScroll()
                startCheckStatus()
                startScrollerSync()
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                stopVideoSeek()
            }
            else{  //关闭链接滚动条与视频进度
                linkScrollEnabled = false
                prefs.edit { putBoolean("linkScrolling", false) }
                stopScrollerSync()
                thumbScroller.stopScroll()
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭链接滚动条与视频进度",2500)
            }
        }
        //按钮：AlwaysSeek
        val buttonAlwaysSeek = findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val buttonAlwaysMaterial = findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (alwaysSeekEnabled){
            buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }else{
            buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonAlwaysSeek.setOnClickListener {
            if (!prefs.getBoolean("alwaysSeek",false)){
                alwaysSeekEnabled = true
                prefs.edit { putBoolean("alwaysSeek", true) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启AlwaysSeek",1000)
            }
            else{
                alwaysSeekEnabled = false
                prefs.edit { putBoolean("alwaysSeek", false) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭AlwaysSeek,正向拖动进度条时将使用倍速播放",3000)
            }
        }
        //播放区域点击事件
        var longPress = false
        var touchLeft = false
        var touchRight = false
        var scrollDistance = 0
        val gestureDetectorPlayArea = GestureDetector(this, object : SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (player.isPlaying){
                        pauseVideo()
                        stopScrollerSync()
                        notice("暂停播放",1000)
                        buttonRefresh()
                    } else {
                        playVideo()
                        startScrollerSync()
                        notice("继续播放",1000)
                        buttonRefresh()
                    }
                    return true
                }
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    changeBackgroundColor()
                    return true
                }
                override fun onLongPress(e: MotionEvent) {
                    if (!player.isPlaying){
                        return
                    }
                    player.setPlaybackSpeed(2.0f)
                    notice("倍速播放中",114514)
                    longPress = true
                    super.onLongPress(e)
                }
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                    if (touchLeft){
                        scrollDistance += distanceY.toInt()
                        val windowInfo = window.attributes
                        var initBrightness = windowInfo.screenBrightness
                        if (initBrightness < 0) {
                            initBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                            windowInfo.screenBrightness = initBrightness
                            window.attributes = windowInfo
                        }
                        val newBrightness = windowInfo.screenBrightness + scrollDistance.toFloat()/10000
                        if (newBrightness <= 1.0 && newBrightness >= 0.0){
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                        }
                    }
                    if (touchRight){
                        scrollDistance += distanceY.toInt()
                        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        player.volume = player.volume + scrollDistance.toFloat()/10000
                    }
                    return super.onScroll(e1, e2, distanceX, distanceY)
                }
            })
        val playArea = findViewById<View>(R.id.playerView)
        playArea.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
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
                if(longPress){
                    longPress = false
                    player.setPlaybackSpeed(1.0f)
                    val noticeCard = findViewById<CardView>(R.id.noticeCard)
                    noticeCard.visibility = View.GONE
                }
            }

            gestureDetectorPlayArea.onTouchEvent(event)
        }
        //按钮：关闭错误提示
        val buttonCloseErrorNotice = findViewById<TextView>(R.id.buttonCloseErrorNotice)
        buttonCloseErrorNotice.setOnClickListener {
            val playerError = findViewById<LinearLayout>(R.id.playerError)
            playerError.visibility = View.GONE
        }
        //按钮：重新加载
        val buttonReload = findViewById<Button>(R.id.buttonReload)
        buttonReload.setOnClickListener {
            val playerError = findViewById<LinearLayout>(R.id.playerError)
            playerError.visibility = View.GONE
            player.release()
            lifecycleScope.launch(Dispatchers.Main ){
                delay(500)
                if (seekSYNC == 1){
                    player = ExoPlayer.Builder(applicationContext)
                        .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        .setWakeMode(WAKE_MODE_NETWORK)
                        .setTrackSelector(trackSelector)
                        .build()
                        .apply {
                            setMediaItem(MediaItem.fromUri(videoUri))
                            prepare()
                            playWhenReady = false
                        }
                    player.play()
                }else{
                    player = ExoPlayer.Builder(applicationContext)
                        .setSeekParameters(SeekParameters.EXACT)
                        .setWakeMode(WAKE_MODE_NETWORK)
                        .setTrackSelector(trackSelector)
                        .build()
                        .apply {
                            setMediaItem(MediaItem.fromUri(videoUri))
                            prepare()
                            playWhenReady = false
                        }
                    player.play()
                }
                playerView.player = player
            }
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()

            if (eachPicDuration > 1000){
                generateThumbSYNC = 1
            }

            withContext(Dispatchers.Main) {
                thumbScroller.adapter = PlayerScrollerAdapter(this@PlayerActivity,
                    absolutePath,playerScrollerViewModel.thumbItems,eachPicWidth,picNumber,eachPicDuration,generateThumbSYNC)
            }

            if(linkScrollEnabled){ startScrollerSync() }
            delay(100)
            startVideoTimeSync()
        }


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
    }//onCreate END


    //runnable-1 - 根据视频时间更新进度条位置
    var scrollParam1 = 0
    var scrollParam2 = 0
    private val syncScrollTaskHandler = Handler(Looper.getMainLooper())
    private val syncScrollTask = object : Runnable {
        override fun run() {
            val gap = 16L
            scrollParam1 = (player.currentPosition / eachPicDuration).toInt()
            scrollParam2 = ((player.currentPosition - scrollParam1*eachPicDuration)*eachPicWidth/eachPicDuration).toInt()
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val lm = recyclerView.layoutManager as LinearLayoutManager
            if (playEnd && !player.isPlaying){
                playEnd = false
                scrollParam1 = scrollParam1 - 1
                scrollParam2 = 150
                lm.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
            }else{
                lm.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
                syncScrollTaskHandler.postDelayed(this, gap)
            }
        }
    }
    private fun startScrollerSync() {
        syncScrollTaskRunning = true
        syncScrollTaskHandler.post(syncScrollTask)
    }
    private fun stopScrollerSync() {
        syncScrollTaskRunning = false
        syncScrollTaskHandler.removeCallbacks(syncScrollTask)
    }
    //runnable-2 - 根据视频时间更新时间戳
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
    //runnable-3 - 视频倍速滚动
    private var videoSmartScrollRunning = false
    private val videoSmartScrollHandler = Handler(Looper.getMainLooper())
    private var videoSmartScroll = object : Runnable{
        override fun run() {
            videoSmartScrollRunning = true
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            var delayGap = if (dragging){ 30L } else{ 30L }
            val videoPosition = player.currentPosition
            val scrollerPosition =  player.duration * (recyclerView.computeHorizontalScrollOffset().toFloat()/recyclerView.computeHorizontalScrollRange())
            if (scrollerPosition < videoPosition +100) {
                if (scrollerTouching){
                    player.pause()
                }else{
                    if (wasPlaying){
                        videoSmartScrollRunning = false
                        playVideo()

                    } else {
                        videoSmartScrollRunning = false
                        player.pause()
                    }
                }
            }else{
                val positionGap = scrollerPosition - videoPosition
                if (positionGap > 5000){
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
        videoSmartScrollRunning = false
        videoSmartScrollHandler.removeCallbacks(videoSmartScroll)
    }
    //runnable-4 - 视频Seek滚动
    private val videoSeekHandler = Handler(Looper.getMainLooper())
    private var videoSeek = object : Runnable{
        override fun run() {
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val totalWidth = recyclerView.computeHorizontalScrollRange()
            val offset     = recyclerView.computeHorizontalScrollOffset()
            val percent    = offset.toFloat() / totalWidth
            val seekToMs   = (percent * player.duration).toLong()


            if (backSeek){
                backSeek = false
                if (seekToMs < player.currentPosition){
                    if (seekToMs < 200){
                        player.seekTo(0)
                        readyFromSeek = true
                    }else if (seekToMs > player.duration - 300){
                        player.seekTo(player.duration - 300)
                        readyFromSeek = true
                    }else{
                        if (isSeekReady){
                            isSeekReady = false
                            seekJob(seekToMs)
                        }
                    }
                }
            }else{
                if (seekToMs < 200){
                    player.seekTo(0)
                    readyFromSeek = true
                }else if (seekToMs > player.duration - 300){
                    player.seekTo(player.duration - 300)
                    readyFromSeek = true
                }else{
                    if (isSeekReady){
                        isSeekReady = false
                        seekJob(seekToMs)
                    }
                }
            }


            if (scrollerTouching || scrolling) {
                videoSeekHandler.postDelayed(this, 100)
            }

        }
    }
    private fun startVideoSeek() {
        if (seekRunnableRunning) return
        seekRunnableRunning = true
        videoSeekHandler.post(videoSeek)
    }
    private fun stopVideoSeek() {
        seekRunnableRunning = false
        videoSeekHandler.removeCallbacks(videoSeek)
    }
    //runnable-5 - 状态检查
    private val checkStatusHandler = Handler(Looper.getMainLooper())
    private var checkStatus = object : Runnable {
        override fun run(){
            val lastScrollerParam1 = scrollParam1
            val lastScrollerParam2 = scrollParam2
            val delayGap = (videoDuration / 1000).toLong()
            lifecycleScope.launch {
                delay(500)
                if (lastScrollerParam1 == scrollParam1 && lastScrollerParam2 == scrollParam2){
                    stopScrollerSync()
                }
            }
            checkStatusHandler.postDelayed(this,delayGap)
        }
    }
    private fun startCheckStatus() {
        checkStatusHandler.post(checkStatus)
    }
    private fun stopCheckStatus() {
        checkStatusHandler.removeCallbacks(checkStatus)
    }


    //job-1 -seek
    private var seekJob: Job? = null
    private fun seekJob(seekToMs: Long) {
        seekJob = lifecycleScope.launch {
            player.seekTo(seekToMs)
            readyFromSeek = true
        }
    }
    //job-2 -showNotice
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
        outState.putBoolean("wasPlaying", wasPlaying)
        outState.putString("uri", videoUri.toString())
        outState.putLong("currentTime", player.currentPosition)
        outState.putBoolean("firstEntry", firstEntry)
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying){ wasPlaying = true }
        if (!BackgroundPlay) {
            player.stop()
        }else{
            playerSelectSoundTrack()
            startForegroundServices()
        }
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onStop() {
        super.onStop()
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onResume() {
        super.onResume()
        if (linkScrollEnabled) startScrollerSync()
        if (BackgroundPlay) {
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
        stopCheckStatus()
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
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                }
                stopScrollerSync()
                stopVideoTimeSync()
            }else{
                if(linkScrollEnabled) { startScrollerSync() }
                startVideoTimeSync()
                widgetsShowing = true
                bottomCard.visibility = View.VISIBLE
                mediumActions.visibility = View.VISIBLE
                buttonExit.visibility = View.VISIBLE
                if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
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
                if(linkScrollEnabled) { startScrollerSync() }
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
    //开启后台播放服务
    private fun startForegroundServices(){
        startService(Intent(this, PlayerForegroundServices::class.java))
    }
    //关闭后台播放服务
    private fun stopForegroundServices(){
        stopService(Intent(this, PlayerForegroundServices::class.java))
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
        if (loopPlay) {
            loopPlay = false
            playVideo()
            return
        } //循环播放操作
        if (readyFromSeek){
            readyFromSeek = false
            isSeekReady = true
            if (scrolling) return
            if (wasPlaying) {
                playVideo()
                return
            }
        }
        isSeekReady = true

        if (!lastSeekExecuted) {
            lastSeekExecuted = true
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val totalWidthForLastSeek = recyclerView.computeHorizontalScrollRange()
            val offsetForLastSeek     = recyclerView.computeHorizontalScrollOffset()
            val percentForLastSeek    = offsetForLastSeek.toFloat() / totalWidthForLastSeek
            val seekToMsForLastSeek   = (percentForLastSeek * player.duration).toLong()
            seekJob?.cancel()
            player.seekTo(seekToMsForLastSeek)
        }
        if (wasPlaying) {
            playVideo()
        }
    }

    private fun playerEnd(){
        notice("视频结束",1000)
        player.pause()
        playEnd = true
        stopVideoTimeSync()
        stopScrollerSync()
        loopPlay = SETTING_loopPlay
        if (SETTING_exitWhenEnd){
            finish()
        }
        if (loopPlay){
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
        if (linkScrollEnabled && !scrollerTouching && !scrolling){ startScrollerSync() }
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

    //真全屏需添加以下元数据到活动清单文件
    /*
    <meta-data
    android:name="android.notch_support"
    android:value="true"/>
    */
    private fun hideStatusBar() {
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
        //val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d",  minutes, seconds)
    }
    private fun formatTime2(raw: Long): String {
        val cent  = raw % 1000
        val totalSec = raw / 1000
        val min  = totalSec / 60
        val sec  = totalSec % 60
        return "%02d:%02d.%03d".format(min, sec, cent)
    }

}