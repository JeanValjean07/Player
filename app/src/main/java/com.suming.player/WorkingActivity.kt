package com.suming.player

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.suming.player.WorkingActivity.DeviceCompatUtil.isCompatibleDevice
import com.suming.player.data.model.ThumbItem
import data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
class WorkingActivity: AppCompatActivity()  {

    //时间戳信息显示位
    private lateinit var tvCurrentTime: TextView
    //视频信息预读
    private var absolutePath = ""       //视频绝对路径,多个成员要读取
    //播放器状态标识
    private lateinit var player: ExoPlayer
    private var playerEnd = false     //上一次是否播放到末尾了
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
    private var onScrolling = false
    private var dragging = false
    private var fling =false
    private var singleTap = false
    private var scrolling = false
    //设置
    private lateinit var sharedPref: SharedPreferences
    private var tapScrollEnabled = false
    private var linkScrollEnabled = false
    private var alwaysSeekEnabled = false
    //Seek程序用到的参数
    private var isSeekReady = false  //seek进入锁
    //倍速程序用到的参数
    private var currentTime = 0L     //进度条对应时间,用于和当前播放器返回时间比较,防止反向
    private var lastPlaySpeed = 0f   //速度没变时,不发送给ExoPlayer
    //进度条随视频进度滚动程序用到的参数
    private var syncScrollTaskRunning = false
    //onScrolled回调中用来判断滚动方向
    private var thisScrollerPosition = 0
    private var lastScrollerPosition = 0
    private var scrollerPositionGap = 0
    //旧机型标识
    private var isCompatibleDevice = false
    private var cannotOpen = false
    //点击隐藏
    private var widgetsShowing = true

    private var fromRecovery = false

    private lateinit var videoUri: Uri
    private lateinit var videoUriRec: Uri

    //以下几个可能跟上面的功能有重复
    private var lastScrollerPositionSeek = 0
    private var dontScrollThisTime = false
    private var dropThisOnDown = false

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
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideStatusBar()
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_working)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //音量
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        //设置项读取,检查和预置
        sharedPref = getSharedPreferences("SpeedSpyPrefs", MODE_PRIVATE)
        val prefs = getSharedPreferences("SpeedSpyPrefs", MODE_PRIVATE)
        if (!prefs.contains("tapScrolling")){
            prefs.edit { putBoolean("tapScrolling", false) }
            tapScrollEnabled = prefs.getBoolean("tapScrolling", false)
        }else{
            tapScrollEnabled = prefs.getBoolean("tapScrolling", false)
        }
        if (!prefs.contains("linkScrolling")){
            prefs.edit { putBoolean("linkScrolling", true) }
            linkScrollEnabled = prefs.getBoolean("linkScrolling", false)
        } else{
            linkScrollEnabled = prefs.getBoolean("linkScrolling", false) }
        if (!prefs.contains("alwaysSeek")){
            prefs.edit { putBoolean("alwaysSeek", false) }
            alwaysSeekEnabled = prefs.getBoolean("alwaysSeek", false)
        } else{
            alwaysSeekEnabled = prefs.getBoolean("alwaysSeek", false) }
        preCheck()


        //反序列化item + 支持用分享打开和用其他应用打开，暂不支持批量打开
        val videoItem: VideoItem? = when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                VideoItem(0, uri, "" , 0)
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return finish()
                VideoItem(0, uri, "", 0)
            }
            else -> { IntentCompat.getParcelableExtra(intent, "video", VideoItem::class.java)}
        }
        if (videoItem == null) {
            Toast.makeText(this, "无法打开这条视频", Toast.LENGTH_SHORT).show()
            cannotOpen = true
            finish()
            return
        } //防空

        videoUri = videoItem.uri
        if (savedInstanceState != null) {
            val uri = savedInstanceState.getString("uri")?.toUri()
            val videoUriRec = uri!!
            videoUri = videoUriRec
            wasPlaying = savedInstanceState.getBoolean("wasPlaying")
            currentTime = savedInstanceState.getLong("currentTime")
        }



        val playerView = findViewById<PlayerView>(R.id.playerView)
        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.EXACT)
            .setWakeMode(WAKE_MODE_NETWORK)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = false
            }
        playerView.player = player

        if (savedInstanceState != null) {
            player.seekTo(currentTime)
        }
        Log.e("SuMing","$wasPlaying")
        if (wasPlaying) {
            playVideo()
        }




        //动态控件初始化：时间戳 + 总时长 + 遮罩淡出
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isSeekReady = true
                    buttonRefresh()
                    //开始播放遮罩淡出
                    val cover = findViewById<View>(R.id.cover)
                    cover.animate()
                        .alpha(0f)
                        .setDuration(100)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { cover.visibility = View.GONE }
                        .start()
                }
                when (state) {
                    Player.STATE_READY -> {
                        if (!alwaysSeekEnabled){
                           return
                        }
                        if (wasPlaying) {
                            playVideo()
                        }
                    }
                    Player.STATE_ENDED -> {
                        stopVideoTimeSync()
                        stopScrollerSync()
                        playerEnd = true
                        pauseVideo()
                        notice("视频结束",1000)
                    }
                    Player.STATE_IDLE -> {
                        stopVideoTimeSync()
                        stopScrollerSync()
                    }
                    Player.STATE_BUFFERING -> {  }
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
                if (player.currentPosition >= player.duration){
                    player.seekTo(0)
                    player.play()
                    if (linkScrollEnabled){ startScrollerSync() }
                    notice("视频已结束,开始重播",1000)
                }
                else{
                    player.play()
                    notice("继续播放",1000)
                }
            }
            buttonRefresh()
        }

        //控件：缩略图滚动条初始化
        val rvThumbnails = findViewById<RecyclerView>(R.id.rvThumbnails)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val sidePadding = screenWidth / 2
        rvThumbnails.setPadding(sidePadding, 0, sidePadding-1, 0) //右边需要减一，否则滑动区域会超出
        var videoUri = videoItem.uri
        absolutePath = getAbsoluteFilePath(this@WorkingActivity, videoUri).toString()
        rvThumbnails.layoutManager = LinearLayoutManager(this@WorkingActivity, LinearLayoutManager.HORIZONTAL, false)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this@WorkingActivity, videoUri)
        } catch (_: Exception) {
            val data = Intent().apply {
                putExtra("key", "needRefresh")
            }
            setResult(RESULT_OK, data)
            finish()
            return
        }
        rvThumbnails.itemAnimator = null
        rvThumbnails.layoutParams.width = 0
        val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()
        if (videoDuration != null) {
            if (videoDuration/1000 > maxPicNumber){  //用47dp计算,做成长方形一点的
                eachPicWidth = (47 * displayMetrics.density).toInt()
                eachPicDuration = (videoDuration.div(100)*100) / maxPicNumber
                picNumber = maxPicNumber
            }else{
                eachPicWidth = (47 * displayMetrics.density).toInt()
                picNumber = videoDuration/1000+1
                eachPicDuration = (videoDuration.div(100)*100)/picNumber
            }
        } else{
            notice("视频长度获取失败,无法绘制控制界面",5000)
            finish()
        }
        retriever.release()


        //RecyclerView-事件监听器 -onSingleTap -onDown
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
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
                val totalContentWidth = rvThumbnails.computeHorizontalScrollRange()
                val scrolled = rvThumbnails.computeHorizontalScrollOffset()
                val leftPadding = rvThumbnails.paddingLeft
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
                wasPlaying = false
                if (player.isPlaying){
                    player.pause()
                    wasPlaying = true
                }
                stopVideoSeek()
                if (!linkScrollEnabled) return false
                if (!tapScrollEnabled) return false
                stopScrollerSync()
                //状态参量置位
                dropThisOnDown = true


                buttonRefresh()
                return false
            }
        })
        //RecyclerView-事件监听器 (中间层)
        rvThumbnails.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean =
                gestureDetector.onTouchEvent(e)
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                gestureDetector.onTouchEvent(e)
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        //RecyclerView-事件监听器 -onScrollStateChanged -onScrolled
        rvThumbnails.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    dragging = true
                    scrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_SETTLING){
                    dragging = false
                    onScrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    dragging = false
                    scrolling = false
                    return
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (linkScrollEnabled){
                    val percent = recyclerView.computeHorizontalScrollOffset().toFloat() / rvThumbnails.computeHorizontalScrollRange()
                    val seekToMs = (percent * player.duration).toLong()
                    currentTime = seekToMs
                    tvCurrentTime.text = formatTime(seekToMs)
                }
                if (!scrolling && !dragging) { //此状态说明进度条是在随视频滚动,用户没有操作
                    return
                }
                if (!linkScrollEnabled) { //此状态说明进度条是在随视频滚动,用户没有操作
                    return
                }
                //检查滑动方向：由于必须触发滑动才会计算滑动Gap,故滑动Gap不会为0
                thisScrollerPosition = recyclerView.computeHorizontalScrollOffset()
                scrollerPositionGap = thisScrollerPosition - lastScrollerPosition
                lastScrollerPosition = thisScrollerPosition
                if (dropThisOnDown){
                    dropThisOnDown = false
                    return
                }
                stopScrollerSync()  //进度条变成上级控制层,关闭所有将进度条作为下级被控层的函数
                if (scrollerPositionGap > 0 && scrollerPositionGap < 100){
                    if(alwaysSeekEnabled){
                        pauseVideo()  //seek时必须暂停视频
                        stopVideoSmartScroll()
                        startVideoSeek()
                    }else{
                        stopVideoSeek()
                        startVideoSmartScroll()
                    }
                }
                else if(scrollerPositionGap < 0 && scrollerPositionGap > -100){
                    stopVideoTimeSync()
                    pauseVideo()
                    val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
                    val totalWidth2 = recyclerView.computeHorizontalScrollRange()
                    val offset2 = recyclerView.computeHorizontalScrollOffset()
                    val percent2 = offset2.toFloat() / totalWidth2
                    val seekToMs2 = (percent2 * player.duration).toLong()
                    if (seekToMs2 > player.currentPosition){
                        return
                    }
                    stopVideoSmartScroll()
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
        //按钮：暂停视频
        val buttonPause = findViewById<FrameLayout>(R.id.buttonPause)
        buttonPause.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
                stopScrollerSync()
                notice("暂停",1000)
                buttonRefresh()
            } else {
                if (playerEnd){
                    notice("视频已结束,为您重播",1000)
                    player.seekTo(0)
                    playerEnd = false
                }
                else{
                    onDown = false
                    fling = false
                    notice("继续播放",1000)
                }
                lifecycleScope.launch {
                    player.volume = currentVolume.toFloat()
                    player.setPlaybackSpeed(1.0f)
                    delay(20)
                    playVideo()
                }
                if (linkScrollEnabled){ startScrollerSync() }
                buttonRefresh()
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
                sharedPref.edit { putBoolean("tapScrolling", true) }
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单击跳转",1000)
            }
            else{
                tapScrollEnabled = false
                sharedPref.edit { putBoolean("tapScrolling", false) }
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
            if (!prefs.getBoolean("linkScrolling",false)){ //启用链接滚动条与视频进度
                linkScrollEnabled = true
                sharedPref.edit { putBoolean("linkScrolling", true) }
                notice("已将进度条与视频进度同步",1000)
                startScrollerSync()
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                stopVideoSeek()
            }
            else{  //关闭链接滚动条与视频进度
                linkScrollEnabled = false
                sharedPref.edit { putBoolean("linkScrolling", false) }
                stopScrollerSync()
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
                sharedPref.edit { putBoolean("alwaysSeek", true) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启AlwaysSeek",1000)
            }
            else{
                alwaysSeekEnabled = false
                sharedPref.edit { putBoolean("alwaysSeek", false) }
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭AlwaysSeek,正向拖动进度条时将启用倍速算法",3000)
            }
        }
        //点击隐藏控件
        val bottomCard = findViewById<View>(R.id.bottomCardContainer)
        val playArea = findViewById<View>(R.id.playerView)
        val mediumActions = findViewById<ConstraintLayout>(R.id.MediumActionsContainer)
        playArea.setOnClickListener {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                if (widgetsShowing){
                    widgetsShowing = false
                    bottomCard.visibility = View.GONE
                    mediumActions.visibility = View.GONE
                    buttonExit.visibility = View.GONE
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.HeadText))
                }else{
                    widgetsShowing = true
                    bottomCard.visibility = View.VISIBLE
                    mediumActions.visibility = View.VISIBLE
                    buttonExit.visibility = View.VISIBLE
                    playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
                }
            }
        }



        //发起adapter联动
        lifecycleScope.launch(Dispatchers.IO) {
            videoUri = videoItem.uri
            val thumbs = MutableList(picNumber) { sec ->
                val file = File(
                    filesDir,
                    "thumbs/${File(videoUri.path!!).nameWithoutExtension}/$sec.jpg"
                )
                ThumbItem(videoUri.toString(), file, picNumber.toLong())
            }
            if (savedInstanceState == null) {
                delay(800)
            }
            withContext(Dispatchers.Main) {
                rvThumbnails.adapter = WorkingActivityAdapter(this@WorkingActivity,
                    absolutePath, thumbs,eachPicWidth,picNumber,eachPicDuration)
            }

            if(linkScrollEnabled){ startScrollerSync() }
            delay(200)
            startVideoTimeSync()

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


    //runnable-1 - 根据视频时间更新进度条位置: 主控：视频时间  被控：进度条位置
    private val syncScrollTaskHandler = Handler(Looper.getMainLooper())
    private val syncScrollTask = object : Runnable {
        override fun run() {
            //基于实时读取视频当前时间的图片滚动，模拟连续刷新
            if (playerEnd){
                playerEnd = false
                notice("视频已播放至结尾,返回当前光标位置",1000)
                val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
                val totalScrollerLength = recyclerView.computeHorizontalScrollRange()
                val currentScrollerPositionSeek = recyclerView.computeHorizontalScrollOffset()
                val percent = currentScrollerPositionSeek.toFloat() / totalScrollerLength
                val seekToMs = (percent * player.duration).toLong()
                player.seekTo(seekToMs)
            }
            val gap = 16L

            val scrollParam1 = (player.currentPosition / eachPicDuration).toInt()
            val scrollParam2 = ((player.currentPosition - scrollParam1*eachPicDuration)*eachPicWidth/eachPicDuration).toInt()

            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val lm = recyclerView.layoutManager as LinearLayoutManager
            lm.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
            syncScrollTaskHandler.postDelayed(this, gap)
        }
    }
    private fun startScrollerSync() {
        if (dontScrollThisTime){
            dontScrollThisTime = false
            return
        }
        syncScrollTaskRunning = true
        syncScrollTaskHandler.post(syncScrollTask)
    }
    private fun stopScrollerSync() {
        syncScrollTaskRunning = false
        syncScrollTaskHandler.removeCallbacks(syncScrollTask)
    }
    //runnable-2 - 根据视频时间更新时间戳: 主控：视频时间  被控：时间戳
    private val videoTimeSyncHandler = Handler(Looper.getMainLooper())
    private var videoTimeSync = object : Runnable{
        override fun run() {
            val currentPosition = player.currentPosition
            tvCurrentTime.text = formatTime(currentPosition)
            videoTimeSyncHandler.post(this)
        }
    }
    private fun startVideoTimeSync() {
        videoTimeSyncHandler.post(videoTimeSync)
    }
    private fun stopVideoTimeSync() {
        videoTimeSyncHandler.removeCallbacks(videoTimeSync)
    }
    //runnable-3 - 视频智能倍速滚动
    private var videoSmartScrollRunning = false
    private val videoSmartScrollHandler = Handler(Looper.getMainLooper())
    private var videoSmartScroll = object : Runnable{
        override fun run() {
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            var delayGap = if (dragging){ 30L } else{ 30L }
            val videoPosition = (player.currentPosition)
            val scrollerPosition =  player.duration * (recyclerView.computeHorizontalScrollOffset().toFloat()/recyclerView.computeHorizontalScrollRange())
            if (scrollerPosition < videoPosition +100) {
                player.pause()
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
            val currentScrollerPositionSeek = recyclerView.computeHorizontalScrollOffset()
            if (currentScrollerPositionSeek != lastScrollerPositionSeek){
                lastScrollerPositionSeek = currentScrollerPositionSeek
                if (currentTime != 0L && currentTime < (player.duration/1000*1000) - 200) {
                    if (isSeekReady) {
                        isSeekReady = false
                        seekJob()
                    }
                } else if (currentTime < 100){
                    player.seekTo(0)
                } else if (currentTime > (player.duration/1000*1000) - 200) {
                    lifecycleScope.launch {
                        player.pause()
                        player.seekTo(player.duration - 500)
                        delay(200)
                        player.seekTo(player.duration)
                    }
                }
                videoSeekHandler.post(this)
            }else{
                return
            }
        }
    }
    private fun startVideoSeek() {
        if (playerEnd) playerEnd = false
        videoSeekHandler.post(videoSeek)
    }
    private fun stopVideoSeek() {
        videoSeekHandler.removeCallbacks(videoSeek)
    }


    //job-1 -seek
    private var seekJob: Job? = null
    private fun seekJob() {
        seekJob?.cancel()
        seekJob = lifecycleScope.launch {
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val totalWidth = recyclerView.computeHorizontalScrollRange()
            val offset     = recyclerView.computeHorizontalScrollOffset()
            val percent    = offset.toFloat() / totalWidth
            val seekToMs   = (percent * player.duration).toLong()
            player.seekTo(seekToMs)
            isSeekReady = false
            //isSeekReady需在播放器状态监听器中更改
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
    private fun notice(text: String, duration: Long) {
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
        Log.e("SuMing","保存状态：$wasPlaying")
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying){ wasPlaying = true }
        player.stop()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVideoSeek()
        stopVideoSmartScroll()
        stopVideoTimeSync()
        stopScrollerSync()
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


    //Functions
    private fun pauseVideo(){
        stopVideoTimeSync()
        wasPlaying = false
        player.pause()
    }
    private fun playVideo(){
        if (!scrolling){
            player.play()
            if (linkScrollEnabled){ startScrollerSync() }
            lifecycleScope.launch {
                delay(1000)
                startVideoTimeSync()
            }
        }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsetsCompat.Type.statusBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
        isCompatibleDevice = isCompatibleDevice()
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
    private fun formatTime2(raw: Long): String {
        val cent  = raw % 1000
        val totalSec = raw / 1000
        val min  = totalSec / 60
        val sec  = totalSec % 60
        return "%02d:%02d.%03d".format(min, sec, cent)
    }


}//class END