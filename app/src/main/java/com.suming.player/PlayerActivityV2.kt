package com.suming.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
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
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
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
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.MoreExecutors
import com.suming.player.PlayerActivity.DeviceCompatUtil.isCompatibleDevice
import data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

@UnstableApi
class PlayerActivityV2: AppCompatActivity(){
    //视频信息预读
    private var videoDuration = 0         //多成员使用:视频时长
    private var absolutePath = ""        //多成员使用:视频绝对路径
    private lateinit var videoUri: Uri
    //播放状态
    private var wasPlaying = true
    private var ONSTOP_WasPlaying = false
    private var LIFE_ONSTOP_WasPlaying = false
    //音量配置参数
    private var maxVolume = 0
    private var currentVolume = 0
    private var originalVolume = 0
    //缩略图绘制参数
    private var SCROLLERINFO_MaxPicNumber = 20         //缩略图最大数量(写死)
    private var SCROLLERINFO_EachPicWidth = 0          //单张缩略图最大宽度(现场计算),高度45dp布局写死
    private var SCROLLERINFO_PicNumber = 0             //缩略图数量(现场计算)
    private var SCROLLERINFO_EachPicDuration: Int = 0  //单张缩略图对应时长(现场计算)
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
    //点击隐藏控件
    private var widgetsShowing = true
    //时间戳刷新间隔
    private var videoTimeSyncGap = 50L
    private var scrollParam1 = 0
    private var scrollParam2 = 0
    private var syncScrollRunnableGap = 16L

    //功能:倍速滚动
    private var currentTime = 0L
    private var lastPlaySpeed = 0f
    private var forceSeekGap = 5000L
    //功能:VideoSeek
    private var isSeekReady = true  //Seek结束标记
    private var backSeek = false    //滚动方向标记
    //判断体:PlayerReady(播放状态)
    private var READY_FromFirstEntry = true
    private var READY_FromSeek = false
    private var READY_FromLastSeek = false
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
    private var PREFS_S_UseHighRefreshRate = false
    private var PREFS_S_UseCompatScroller = false
    private var PREFS_S_CloseVideoTrack = false
    //RunnableRunning
    private var seekRunnableRunning = false
    private var smartScrollRunnableRunning = false
    private var syncScrollRunnableRunning = false



    //LastSeek保底Seek机制
    private var LastSeekLaunched = false


    //传递给通知或播控中心的视频信息字段
    private var STRING_VideoTitle = ""
    private var STRING_VideoArtist = ""
    private var STRING_FileName = ""

    //安全期
    private var SECUREINTERVAL_SEEKONCE = false
    private var SECUREINTERVAL_ONDOWN = false
    private var SECUREINTERVAL_FINGERUP = false


    private var DESTROY_FromSavedInstance = false


    //自动旋转状态
    private var rotationSetting = 0

    private var observerOnStoped = false
    private var observerOnStarted = false

    private var STATE_FingerTouching = false
    //PlayerReady
    private var STATE_PlayerReady = false


        //音量变化步长
        private var volumnChangeGap = 1



        //状态栏高度
    private var statusBarHeight = 0
    //方向回调
    private var orientationChangeTime = 0L
    private var LastOrientationChangeTime = 0L

    //音量恢复
    private var volumeRecExecuted = false
    //耳机链接状态
    private var headSet = false

    private var OrientationEventListener: OrientationEventListener? = null
    private var OrientationEventListener2: OrientationEventListener? = null

    private val vm: PlayerExoViewModel by viewModels { PlayerExoFactory.getInstance(application) }

    private var INTERUPT_WasPlaying = false
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
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player)

        //其他预设
        preCheck()

        //恢复暂存数据
        if (savedInstanceState == null) {
            PlayerExoSingleton.releasePlayer() //初次打开:关闭播放器实例
            stopBackgroundServices()           //初次打开:关闭后台播放服务
        }else{
            //取出Intent
            intent = savedInstanceState.getParcelable("CONTENT_INTENT")
        }

        //设置项读取,检查和预置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        if (!prefs.contains("PREFS_GenerateThumbSYNC")) {
            prefsEditor.putBoolean("PREFS_GenerateThumbSYNC", true)
            PREFS_RC_GenerateThumbSYNC = prefs.getBoolean("PREFS_GenerateThumbSYNC", true)
        } else {
            PREFS_RC_GenerateThumbSYNC = prefs.getBoolean("PREFS_GenerateThumbSYNC", true)
        }
        if (!prefs.contains("PREFS_TapScrolling")) {
            prefsEditor.putBoolean("PREFS_TapScrolling", false)
            PREFS_RC_TapScrollEnabled = prefs.getBoolean("PREFS_TapScrolling", false)
        } else {
            PREFS_RC_TapScrollEnabled = prefs.getBoolean("PREFS_TapScrolling", false)
        }
        if (!prefs.contains("PREFS_LinkScrolling")) {
            prefsEditor.putBoolean("PREFS_LinkScrolling", true)
            PREFS_RC_LinkScrollEnabled = prefs.getBoolean("PREFS_LinkScrolling", false)
        } else {
            PREFS_RC_LinkScrollEnabled = prefs.getBoolean("PREFS_LinkScrolling", false)
        }
        if (!prefs.contains("PREFS_AlwaysSeek")) {
            prefsEditor.putBoolean("PREFS_AlwaysSeek", true)
            PREFS_RC_AlwaysSeekEnabled = prefs.getBoolean("PREFS_AlwaysSeek", false)
        } else {
            PREFS_RC_AlwaysSeekEnabled = prefs.getBoolean("PREFS_AlwaysSeek", false)
        }
        if (!prefs.contains("PREFS_BackgroundPlay")) {
            prefsEditor.putBoolean("PREFS_BackgroundPlay", false)
            PREFS_RC_BackgroundPlay = prefs.getBoolean("PREFS_BackgroundPlay", false)
        } else {
            PREFS_RC_BackgroundPlay = prefs.getBoolean("PREFS_BackgroundPlay", false)
        }
        if (!prefs.contains("PREFS_LoopPlay")) {
            prefsEditor.putBoolean("PREFS_LoopPlay", false)
            PREFS_RC_LoopPlay = prefs.getBoolean("PREFS_LoopPlay", false)
        } else {
            PREFS_RC_LoopPlay = prefs.getBoolean("PREFS_LoopPlay", false)
        }
        if (!prefs.contains("PREFS_ExitWhenEnd")) {
            prefsEditor.putBoolean("PREFS_ExitWhenEnd", false)
            PREFS_S_ExitWhenEnd = prefs.getBoolean("PREFS_ExitWhenEnd", false)
        } else {
            PREFS_S_ExitWhenEnd = prefs.getBoolean("PREFS_ExitWhenEnd", false)
        }
        if (!prefs.contains("PREFS_UseLongScroller")) {
            prefsEditor.putBoolean("PREFS_UseLongScroller", false)
            PREFS_S_UseLongScroller = prefs.getBoolean("PREFS_UseLongScroller", false)
        } else {
            PREFS_S_UseLongScroller = prefs.getBoolean("PREFS_UseLongScroller", false)
        }
        if (!prefs.contains("PREFS_UseLongSeekGap")) {
            prefsEditor.putBoolean("PREFS_UseLongSeekGap", false)
            PREFS_S_UseLongSeekGap = prefs.getBoolean("PREFS_UseLongSeekGap", false)
            if (PREFS_S_UseLongSeekGap) {
                forceSeekGap = 20000L
            }
        } else {
            PREFS_S_UseLongSeekGap = prefs.getBoolean("PREFS_UseLongSeekGap", false)
            if (PREFS_S_UseLongSeekGap) {
                forceSeekGap = 20000L
            }
        }
        if (!prefs.contains("PREFS_UseBlackScreenInLandscape")) {
            prefsEditor.putBoolean("PREFS_UseBlackScreenInLandscape", false)
            PREFS_S_UseBlackScreenInLandscape = prefs.getBoolean("PREFS_UseBlackScreenInLandscape", false)
        } else {
            PREFS_S_UseBlackScreenInLandscape =
                prefs.getBoolean("PREFS_UseBlackScreenInLandscape", false)
        }
        if (!prefs.contains("PREFS_UseHighRefreshRate")) {
            prefsEditor.putBoolean("PREFS_UseHighRefreshRate", false)
            PREFS_S_UseHighRefreshRate = prefs.getBoolean("PREFS_UseHighRefreshRate", false)
        } else {
            PREFS_S_UseHighRefreshRate = prefs.getBoolean("PREFS_UseHighRefreshRate", false)
        }
        if (!prefs.contains("PREFS_UseCompatScroller")) {
            prefsEditor.putBoolean("PREFS_UseCompatScroller", false)
            PREFS_S_UseCompatScroller = prefs.getBoolean("PREFS_UseCompatScroller", false)
        } else {
            PREFS_S_UseCompatScroller = prefs.getBoolean("PREFS_UseCompatScroller", false)
        }
        if (!prefs.contains("PREFS_CloseVideoTrack")) {
            prefsEditor.putBoolean("PREFS_CloseVideoTrack", true)
            PREFS_S_CloseVideoTrack = prefs.getBoolean("PREFS_CloseVideoTrack", false)
        } else {
            PREFS_S_CloseVideoTrack = prefs.getBoolean("PREFS_CloseVideoTrack", false)
        }
        if (!prefs.contains("INFO_STATUSBAR_HEIGHT")) {
            statusBarHeight = 200
        } else {
            statusBarHeight = prefs.getInt("INFO_STATUSBAR_HEIGHT", 0)
        }
        prefsEditor.apply()

        //方向回调:监听屏幕旋转
        OrientationEventListener2 = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                //把方向角数值映射为状态量
                if (orientation > 260 && orientation < 280) { vm.OrientationValue = 1 }
                else if (orientation > 80 && orientation < 100) { vm.OrientationValue = 2 }
                else if (orientation > 340 && orientation < 360) { vm.OrientationValue = 0 }
                //进入锁
                orientationChangeTime = System.currentTimeMillis()
                if (orientationChangeTime - LastOrientationChangeTime < 1) { return }
                LastOrientationChangeTime = orientationChangeTime
                //读取自动旋转状态
                rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                //自动旋转开启
                if (rotationSetting == 1){
                    //当前为竖屏
                    if (vm.currentOrientation == 0){
                        //标准横屏ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 1) return
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //反向横屏ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 2) return
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                    }
                    //当前为正向横屏
                    else if (vm.currentOrientation == 1){
                        //反向横屏ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 2) {
                            val buttonLinkContainer = findViewById<FrameLayout>(R.id.buttonLinkContainer)
                            buttonLinkContainer.layoutParams = (buttonLinkContainer.layoutParams as ViewGroup.MarginLayoutParams).apply { marginEnd = 200 }
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                        //竖屏ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                    }
                    //当前为反向横屏
                    else if (vm.currentOrientation == 2){
                        //标准横屏ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            val buttonExit = findViewById<ImageButton>(R.id.buttonExit)
                            val CONTROLLER_CurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
                            buttonExit.layoutParams = (buttonExit.layoutParams as ViewGroup.MarginLayoutParams).apply { marginStart = 160 }
                            CONTROLLER_CurrentTime.layoutParams = (CONTROLLER_CurrentTime.layoutParams as ViewGroup.MarginLayoutParams).apply { marginStart = 200 }
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //竖屏ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                    }
                }
                //自动旋转关闭
                else if (rotationSetting == 0){
                    if (!vm.FromManualPortrait){
                        //转动到正向横屏ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            val buttonExit = findViewById<ImageButton>(R.id.buttonExit)
                            val CONTROLLER_CurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
                            buttonExit.layoutParams = (buttonExit.layoutParams as ViewGroup.MarginLayoutParams).apply { marginStart = 160 }
                            CONTROLLER_CurrentTime.layoutParams = (CONTROLLER_CurrentTime.layoutParams as ViewGroup.MarginLayoutParams).apply { marginStart = 200 }
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //转动到反向横屏ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            val buttonLinkContainer = findViewById<FrameLayout>(R.id.buttonLinkContainer)
                            buttonLinkContainer.layoutParams = (buttonLinkContainer.layoutParams as ViewGroup.MarginLayoutParams).apply { marginEnd = 200 }
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                    }
                }
            }
        }.apply { enable() }



        //界面初始化:默认颜色设置+控件动态变位
        AppBarSetting()


        val vm = ViewModelProvider(
            this,
            PlayerExoFactory.getInstance(application)
        )[PlayerExoViewModel::class.java]

        //刷新率强制修改
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && PREFS_S_UseHighRefreshRate) {
            val wm = windowManager
            val mode = wm.defaultDisplay.mode
            val fps = mode.refreshRate
            window.attributes = window.attributes.apply {
                preferredRefreshRate = fps
            }
        }

        //生命周期监听:判断应用是否退出
        lifecycleScope.launch {
            ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundObserver)
        }

        //音频设备监听
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(DeviceCallback, null)
        //内部广播接收
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val filter = IntentFilter("LOCAL_RECEIVER")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("key")
                if (data == "PLAYER_PLAY") {
                    requestAudioFocus()
                    if (vm.playEnd) {
                        vm.playEnd = false
                        vm.player.seekTo(0)
                        vm.player.play()
                    } else {
                        vm.player.play()
                    }
                    if (PREFS_RC_LinkScrollEnabled) startScrollerSync()
                }
                if (data == "PLAYER_PAUSE") {
                    INTERUPT_WasPlaying = vm.player.isPlaying
                    vm.player.pause()
                }
            }
        }
        localBroadcastManager.registerReceiver(receiver, filter)



        //区分打开方式并反序列化
        val videoItem: VideoItem? = when (intent?.action) {

            Intent.ACTION_SEND -> {
                PREFS_S_ExitWhenEnd = true
                val uri =
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?: return finish()
                VideoItem(0, uri, "", 0)
            }

            Intent.ACTION_VIEW -> {
                PREFS_S_ExitWhenEnd = true
                val uri = intent.data ?: return finish()
                VideoItem(0, uri, "", 0)
            }

            else -> {
                PREFS_S_ExitWhenEnd = false
                IntentCompat.getParcelableExtra(intent, "video", VideoItem::class.java)
            }
        }
        if (videoItem == null) {
            Toast.makeText(this, "读取视频失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        videoUri = videoItem.uri

        //绑定播放器输出
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = vm.player

        //初次打开时传递视频链接
        if (savedInstanceState == null) {
            vm.setVideoUri(videoUri)
        } else {
            buttonRefresh()
            val cover = findViewById<View>(R.id.cover)
            cover.animate().alpha(0f).setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { cover.visibility = View.GONE }
                .start()
        }

        //播放器事件监听
        vm.player.addListener(object : Player.Listener {
            @SuppressLint("SwitchIntDef")
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        STATE_PlayerReady = true
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

        //视频当前时间位置
        val CONTROLLER_CurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
        var CONTROLLER_CurrentTime_Color = 1
        CONTROLLER_CurrentTime.setOnClickListener {
            if (CONTROLLER_CurrentTime_Color == 1) {
                CONTROLLER_CurrentTime_Color = 0
                CONTROLLER_CurrentTime.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.BlackGrey2
                    )
                )
            } else {
                CONTROLLER_CurrentTime_Color = 1
                CONTROLLER_CurrentTime.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.WhiteGrey2
                    )
                )
            }
        }

        //控件：缩略图滚动条初始化
        val CONTROLLER_ThumbScroller = findViewById<RecyclerView>(R.id.rvThumbnails)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val sidePadding = screenWidth / 2
        //进度条端点处理
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            CONTROLLER_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var scrollerMarginType = 0
            if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor") {
                scrollerMarginType = 2
                val statusBarHeight = prefs.getInt("INFO_STATUSBAR_HEIGHT", 0)
                CONTROLLER_ThumbScroller.setPadding(
                    sidePadding + statusBarHeight / 2,
                    0,
                    sidePadding + statusBarHeight / 2 - 1,
                    0
                )
            } else if (Build.BRAND == "samsung") {
                scrollerMarginType = 1
                CONTROLLER_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            } else {
                scrollerMarginType = 1
                CONTROLLER_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }
            if (PREFS_S_UseLongScroller) {
                val statusBarHeight = prefs.getInt("INFO_STATUSBAR_HEIGHT", 0)
                if (scrollerMarginType == 2) {
                    CONTROLLER_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                } else {
                    CONTROLLER_ThumbScroller.setPadding(
                        sidePadding + statusBarHeight / 2,
                        0,
                        sidePadding + statusBarHeight / 2 - 1,
                        0
                    )
                }
            }
        }
        val videoUri = videoItem.uri
        absolutePath = getAbsoluteFilePath(this@PlayerActivityV2, videoUri).toString()
        CONTROLLER_ThumbScroller.layoutManager = LinearLayoutManager(this@PlayerActivityV2, LinearLayoutManager.HORIZONTAL, false)
        val retriever = MediaMetadataRetriever()
        //信息读取
        STRING_VideoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        STRING_VideoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val file = File(absolutePath)
        STRING_FileName = file.name
        //视频无法打开时的处理
        try {
            retriever.setDataSource(this@PlayerActivityV2, videoUri)
        } catch (_: Exception) {
            val data = Intent().apply {
                putExtra("key", "needRefresh")
            }
            setResult(RESULT_OK, data)
            finish()
            return
        }
        CONTROLLER_ThumbScroller.itemAnimator = null
        CONTROLLER_ThumbScroller.layoutParams.width = 0
        videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
        //视频时长超过120秒时进度条仅显秒
        if (videoDuration > 120_000L) {
            videoTimeSyncGap = 500L
        }
        //长进度条分图机制
        if (PREFS_S_UseLongScroller) {
            SCROLLERINFO_EachPicWidth = (47 * displayMetrics.density).toInt()

            if (videoDuration > 1_0000_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 500.0).toInt()
                SCROLLERINFO_PicNumber = 500
            } else if (videoDuration > 7500_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 400.0).toInt()
                SCROLLERINFO_PicNumber = 400
            } else if (videoDuration > 5000_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 300.0).toInt()
                SCROLLERINFO_PicNumber = 300
            } else if (videoDuration > 500_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 200.0).toInt()
                SCROLLERINFO_PicNumber = 200
            } else {
                SCROLLERINFO_EachPicDuration = 1000
                SCROLLERINFO_PicNumber = min((max((videoDuration / 1000), 1)), 500)
            }
        } else {
            syncScrollRunnableGap = ((videoDuration / 1000) * (1000.0 / 3600)).toLong()
            if (videoDuration / 1000 > SCROLLERINFO_PicNumber) {
                SCROLLERINFO_EachPicWidth = (47 * displayMetrics.density).toInt()
                SCROLLERINFO_EachPicDuration =
                    (videoDuration.div(100) * 100) / SCROLLERINFO_MaxPicNumber
                SCROLLERINFO_PicNumber = SCROLLERINFO_MaxPicNumber
            } else {
                SCROLLERINFO_EachPicWidth = (47 * displayMetrics.density).toInt()
                SCROLLERINFO_PicNumber = (videoDuration / 1000) + 1
                SCROLLERINFO_EachPicDuration =
                    (videoDuration.div(100) * 100) / SCROLLERINFO_PicNumber
            }
        }
        retriever.release()



        //gestureDetectorScroller -onSingleTap -onDown
        val gestureDetectorScroller = GestureDetector(this, object : SimpleOnGestureListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                singleTap = true
                if (!PREFS_RC_TapScrollEnabled) {
                    if (PREFS_RC_LinkScrollEnabled) {
                        notice("未开启单击跳转,如需跳转请先开启,或关闭链接滚动", 1000)
                        return false
                    }
                }
                //根据百分比计算具体跳转时间点
                val totalContentWidth = CONTROLLER_ThumbScroller.computeHorizontalScrollRange()
                val scrolled = CONTROLLER_ThumbScroller.computeHorizontalScrollOffset()
                val leftPadding = CONTROLLER_ThumbScroller.paddingLeft
                val xInContent = e.x + scrolled - leftPadding
                if (totalContentWidth <= 0) return false
                val percent = xInContent / totalContentWidth
                val seekToMs = (percent * vm.player.duration).toLong().coerceIn(0, vm.player.duration)
                if (seekToMs <= 0) {
                    return false
                }
                if (seekToMs >= vm.player.duration) {
                    return false
                }
                //发送跳转命令
                vm.player.seekTo(seekToMs)
                notice("跳转至${formatTime2(seekToMs)}", 1000)
                lifecycleScope.launch {
                    startScrollerSync()
                    delay(20)
                    if (!PREFS_RC_LinkScrollEnabled) {
                        stopScrollerSync()
                    }
                }



                if (wasPlaying) {
                    wasPlaying = false
                    vm.player.play()
                }
                return true
            }
            override fun onDown(e: MotionEvent): Boolean {
                //更改按压状态
                STATE_FingerTouching = true
                //未开启链接滚动时驳回
                if (!PREFS_RC_LinkScrollEnabled) return false
                //播放状态记录
                if (!smartScrollRunnableRunning && !seekRunnableRunning && !scrolling && !SECUREINTERVAL_SEEKONCE && !SECUREINTERVAL_ONDOWN && !SECUREINTERVAL_FINGERUP) {
                    wasPlaying = vm.player.isPlaying
                    //ONDOWN安全期开启
                    lifecycleScope.launch {
                        SecureIntervalJob()
                    }
                }
                return false
            }
        })
        //RecyclerView-事件监听器 (中间层)
        CONTROLLER_ThumbScroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                scrollerTouching = true
                if (e.action == MotionEvent.ACTION_UP) {
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
        CONTROLLER_ThumbScroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    dragging = true
                    scrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    dragging = false
                    scrolling = true
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    dragging = false
                    scrolling = false
                    return
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!scrolling && !dragging) {
                    return
                }  //此状态说明进度条是在随视频滚动,用户没有操作
                if (scrollerTouching) {
                    if (dx == 1 || dx == -1) {
                        vm.player.setSeekParameters(SeekParameters.EXACT)
                    } else {
                        vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                } else {
                    vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                }
                if (PREFS_RC_LinkScrollEnabled) {
                    val percentScroll = recyclerView.computeHorizontalScrollOffset()
                        .toFloat() / CONTROLLER_ThumbScroller.computeHorizontalScrollRange()
                    videoTimeTo = (percentScroll * vm.player.duration).toLong()
                    currentTime = videoTimeTo
                    if (videoTimeSyncGap >= 500L) {
                        CONTROLLER_CurrentTime.text = formatTime1(videoTimeTo)
                    } else {
                        CONTROLLER_CurrentTime.text = formatTime(videoTimeTo)
                    }
                } else {
                    return
                } //时间戳跟随进度条
                stopVideoTimeSync()
                stopScrollerSync()
                if (dx > 0) {
                    if (PREFS_RC_AlwaysSeekEnabled) {
                        backSeek = false
                        startVideoSeek()
                    } else {
                        stopVideoSeek()
                        startVideoSmartScroll()
                    }
                } else if (dx < 0) {
                    backSeek = true
                    startVideoSeek()
                }
            }
        })


        //固定控件初始化：退出按钮
        val buttonExit = findViewById<View>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            vm.player.playWhenReady = false
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
        if (PREFS_RC_LoopPlay) {
            vm.player.repeatMode = Player.REPEAT_MODE_ONE
            buttonLoopPlayMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        } else {
            vm.player.repeatMode == Player.REPEAT_MODE_OFF
            buttonLoopPlayMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLoopPlay.setOnClickListener {
            if (vm.player.repeatMode == Player.REPEAT_MODE_OFF) {
                vm.player.repeatMode = Player.REPEAT_MODE_ONE
                prefs.edit { putBoolean("PREFS_LoopPlay", true).apply() }
                buttonLoopPlayMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单集循环", 1000)
                if (vm.playEnd) {
                    vm.playEnd = false
                    vm.player.seekTo(0)
                    vm.player.play()
                }
            } else {
                vm.player.repeatMode = Player.REPEAT_MODE_OFF
                prefs.edit { putBoolean("PREFS_LoopPlay", false).apply() }
                buttonLoopPlayMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单集循环", 1000)
            }
        }
        //按钮：返回视频开头
        val buttonBackToStart = findViewById<FrameLayout>(R.id.buttonActualBackToStart)
        val buttonBackToStartMaterial = findViewById<MaterialButton>(R.id.buttonMaterialBackToStart)
        buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        buttonBackToStart.setOnClickListener {
            stopVideoSmartScroll()
            lifecycleScope.launch {
                buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityV2, R.color.ButtonBg))
                delay(200)
                buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityV2, R.color.ButtonBgClosed))
            }
            stopVideoSeek()
            CONTROLLER_ThumbScroller.stopScroll()
            if (PREFS_RC_LinkScrollEnabled) startScrollerSync()
            vm.player.seekTo(0)
            playVideo()
            notice("返回视频起始", 1000)
        }
        //按钮：暂停/继续播放
        val buttonPause = findViewById<FrameLayout>(R.id.buttonPause)
        buttonPause.setOnClickListener {
            if (vm.player.isPlaying) {
                pauseVideo()
                stopScrollerSync()
                notice("暂停", 1000)
                buttonRefresh()
            } else {
                onDown = false
                fling = false
                notice("继续播放", 1000)
                playerView.player = vm.player
                lifecycleScope.launch {
                    CONTROLLER_ThumbScroller.stopScroll()
                    delay(20)
                    if (vm.playEnd) {
                        vm.playEnd = false
                        vm.player.seekTo(0)
                        playVideo()
                        notice("视频已结束,开始重播", 1000)
                        buttonRefresh()
                        return@launch
                    } else {
                        playVideo()
                        return@launch
                    }
                }
            }
        }
        //按钮：后台播放
        val buttonBackgroundPlay = findViewById<FrameLayout>(R.id.buttonActualBackgroundPlay)
        val buttonBackgroundPlayMaterial = findViewById<MaterialButton>(R.id.buttonMaterialBackgroundPlay)
        if (PREFS_RC_BackgroundPlay) {
            buttonBackgroundPlayMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        } else {
            buttonBackgroundPlayMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonBackgroundPlay.setOnClickListener {
            checkNotificationPermission()
            if (!prefs.getBoolean("PREFS_BackgroundPlay", false)) {
                PREFS_RC_BackgroundPlay = true
                prefs.edit { putBoolean("PREFS_BackgroundPlay", true).apply() }
                notice("已开启后台播放", 1000)
                buttonBackgroundPlayMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
            } else {
                PREFS_RC_BackgroundPlay = false
                prefs.edit { putBoolean("PREFS_BackgroundPlay", false).apply() }
                notice("已关闭后台播放", 1000)
                buttonBackgroundPlayMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
            }
        }
        //按钮：单击跳转
        val buttonTap = findViewById<FrameLayout>(R.id.buttonActualTap)
        val buttonTapMaterial = findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (PREFS_RC_TapScrollEnabled) {
            buttonTapMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        } else {
            buttonTapMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonTap.setOnClickListener {
            if (!prefs.getBoolean("PREFS_TapScrolling", false)) {
                PREFS_RC_TapScrollEnabled = true
                prefs.edit { putBoolean("PREFS_TapScrolling", true).apply() }
                buttonTapMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启单击跳转", 1000)
            } else {
                PREFS_RC_TapScrollEnabled = false
                prefs.edit { putBoolean("PREFS_TapScrolling", false).apply() }
                buttonTapMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭单击跳转", 1000)
            }
        }
        //按钮：链接滚动条与视频进度
        val buttonLink = findViewById<FrameLayout>(R.id.buttonActualLink)
        val buttonLinkMaterial = findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (PREFS_RC_LinkScrollEnabled) {
            buttonLinkMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        } else {
            buttonLinkMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonLink.setOnClickListener {
            if (!prefs.getBoolean("PREFS_LinkScrolling", false)) {
                PREFS_RC_LinkScrollEnabled = true
                prefs.edit { putBoolean("PREFS_LinkScrolling", true).apply() }
                notice("已将进度条与视频进度同步", 1000)
                isSeekReady = true
                CONTROLLER_ThumbScroller.stopScroll()
                startScrollerSync()
                buttonLinkMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                stopVideoSeek()
            } else {
                PREFS_RC_LinkScrollEnabled = false
                prefs.edit { putBoolean("PREFS_LinkScrolling", false).apply() }
                stopScrollerSync()
                CONTROLLER_ThumbScroller.stopScroll()
                buttonLinkMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭链接滚动条与视频进度", 2500)
            }
        }
        //按钮：AlwaysSeek
        val buttonAlwaysSeek = findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val buttonAlwaysMaterial = findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (PREFS_RC_AlwaysSeekEnabled) {
            buttonAlwaysMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        } else {
            buttonAlwaysMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        buttonAlwaysSeek.setOnClickListener {
            if (!prefs.getBoolean("PREFS_AlwaysSeek", false)) {
                PREFS_RC_AlwaysSeekEnabled = true
                prefs.edit { putBoolean("PREFS_AlwaysSeek", true).apply() }
                buttonAlwaysMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
                notice("已开启AlwaysSeek", 1000)
            } else {
                PREFS_RC_AlwaysSeekEnabled = false
                prefs.edit { putBoolean("PREFS_AlwaysSeek", false).apply() }
                buttonAlwaysMaterial.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
                notice("已关闭AlwaysSeek", 3000)
            }
        }
        //按钮：切换横屏
        val buttonSwitchLandscape = findViewById<FrameLayout>(R.id.buttonActualSwitchLandscape)
        buttonSwitchLandscape.setOnClickListener {
            ButtonChangeOrientation()
        }
        //播放区域点击事件
        var longPress = false
        var touchLeft = false
        var touchRight = false
        var scrollDistance = 0
        val gestureDetectorPlayArea = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (vm.player.isPlaying) {
                    pauseVideo()
                    stopScrollerSync()
                    notice("暂停播放", 1000)
                    buttonRefresh()
                } else {
                    if (vm.playEnd) {
                        vm.playEnd = false
                        vm.player.seekTo(0)
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
                if (!vm.player.isPlaying) {
                    return
                }
                vm.player.setPlaybackSpeed(2.0f)
                notice("倍速播放中", 114514)
                longPress = true
                super.onLongPress(e)
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (touchLeft) {
                    scrollDistance += distanceY.toInt()
                    val windowInfo = window.attributes
                    //亮度修改操作
                    if (scrollDistance > 50) {
                        val newBrightness = (windowInfo.screenBrightness + 0.1f).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 +1 (${newBrightness}/1)", 1000)
                        }else{
                            notice("亮度已到上限", 1000)
                        }
                    }else if (scrollDistance < -50){
                        val newBrightness = (windowInfo.screenBrightness - 0.1f).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 -1 (${newBrightness}/1)", 1000)
                        }else{
                            notice("亮度已到下限", 1000)
                        }
                    }
                    //数值越界置位
                    if (scrollDistance > 50 || scrollDistance < -50) {
                        scrollDistance = 0
                    }
                }
                if (touchRight) {
                    scrollDistance += distanceY.toInt()
                    //快速下滑紧急静音
                    if (scrollDistance < -150){
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        notice("快速静音", 1000)
                    }
                    //音量修改操作
                    if (scrollDistance > volumnChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume = currentVolume + 1
                        if (currentVolume <= maxVolume){
                            if (headSet){
                                if (currentVolume <= (maxVolume*0.6).toInt()){
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                    notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                }else{
                                    notice("佩戴耳机时,音量不能超过${(maxVolume*0.6).toInt()},除非使用音量键调整", 1000)
                                }
                            }else{
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                            }
                        }
                    }else if (scrollDistance< -volumnChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume = currentVolume - 1
                        if (currentVolume >= 0){
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                        }
                    }
                    //数值越界置位
                    if (scrollDistance>50 || scrollDistance< -50) {
                        scrollDistance = 0
                    }
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
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
                    vm.player.setPlaybackSpeed(1.0f)
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
        //按钮：重新加载(暂时改为直接退出)
        val buttonReload = findViewById<Button>(R.id.buttonReload)
        buttonReload.setOnClickListener {
            finish()
        }




        //根据机型选择启用播控中心或自定义通知
        if (savedInstanceState == null) {
            if (Build.BRAND == "Xiaomi" || Build.BRAND == "samsung") {
                val outFile = File(cacheDir, "thumb_${absolutePath.hashCode()}_cover.jpg")
                val SessionToken = SessionToken(this, ComponentName(this, PlayerBackgroundServices::class.java))
                val MediaSessionController = MediaController.Builder(this, SessionToken).buildAsync()
                MediaSessionController.addListener({
                    val controller = MediaSessionController.get()
                    controller.setMediaItem(
                        MediaItem.Builder()
                            .setUri(videoUri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(STRING_FileName)
                                    .setArtist(STRING_VideoArtist)
                                    .setArtworkUri(Uri.fromFile(outFile))
                                    .build()
                            )
                            .build()
                    )

                    controller.prepare()
                }, MoreExecutors.directExecutor())
            }
            else if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor"){
                lifecycleScope.launch {
                    delay(1000)
                    startBackgroundServices()
                }
            }
        }
        //绑定Adapter
        lifecycleScope.launch(Dispatchers.IO) {
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()

            if (SCROLLERINFO_EachPicDuration > 1000){
                PREFS_RC_GenerateThumbSYNC = true
            }

            withContext(Dispatchers.Main) {
                CONTROLLER_ThumbScroller.adapter = PlayerScrollerAdapter(this@PlayerActivityV2,
                    absolutePath,playerScrollerViewModel.thumbItems,SCROLLERINFO_EachPicWidth,SCROLLERINFO_PicNumber,SCROLLERINFO_EachPicDuration,PREFS_RC_GenerateThumbSYNC)
            }

            if(PREFS_RC_LinkScrollEnabled){ startScrollerSync() }
            delay(100)
            startVideoTimeSync()
        }
        //播放失败检查
        if (savedInstanceState == null){
            lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)
                if (!STATE_PlayerReady){
                    withContext(Dispatchers.Main){
                        val playerError = findViewById<LinearLayout>(R.id.playerError)
                        playerError.visibility = View.VISIBLE
                    }
                }
            }
        }
        //系统手势监听：返回键重写
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ExitByOrientation()
            }
        })
    } //onCreate END



    //Runnable:根据视频时间更新进度条位置
    private val syncScrollTaskHandler = Handler(Looper.getMainLooper())
    private val syncScrollTask = object : Runnable {
        override fun run() {
            syncScrollRunnableRunning = true
            if (SCROLLERINFO_EachPicDuration == 0){
                return
            }
            scrollParam1 = (vm.player.currentPosition / SCROLLERINFO_EachPicDuration).toInt()
            scrollParam2 = ((vm.player.currentPosition - scrollParam1*SCROLLERINFO_EachPicDuration)*SCROLLERINFO_EachPicWidth/SCROLLERINFO_EachPicDuration).toInt()
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            val lm = recyclerView.layoutManager as LinearLayoutManager
            if (vm.playEnd && !vm.player.isPlaying){
                //vm.playEnd = false
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
            val CONTROLLER_CurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
            val currentPosition = vm.player.currentPosition
            if (videoTimeSyncGap >= 500L){
                CONTROLLER_CurrentTime.text = formatTime1(currentPosition)
            }else{
                CONTROLLER_CurrentTime.text = formatTime(currentPosition)
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
            val videoPosition = vm.player.currentPosition
            val scrollerPosition =  vm.player.duration * (recyclerView.computeHorizontalScrollOffset().toFloat()/recyclerView.computeHorizontalScrollRange())
            vm.player.volume = 0f
            if (scrollerPosition < videoPosition +100) {
                if (scrollerTouching){
                    vm.player.pause()
                }else{
                    if (wasPlaying){
                        smartScrollRunnableRunning = false
                        playVideo()
                        vm.player.volume = 1f
                    } else {
                        smartScrollRunnableRunning = false
                        vm.player.pause()
                    }
                }
            }else{
                val positionGap = scrollerPosition - videoPosition
                if (positionGap > forceSeekGap){
                    vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    vm.player.seekTo(scrollerPosition.toLong())
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
                    vm.player.setPlaybackSpeed(speed5)
                }else{
                    vm.player.play()
                }
                videoSmartScrollHandler.postDelayed(this,delayGap)
            }
        }
    }
    private fun startVideoSmartScroll() {
        stopScrollerSync()
        stopVideoTimeSync()
        vm.player.volume = 0f
        vm.player.play()
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
            val seekToMs   = (percent * vm.player.duration).toLong()
            seekRunnableRunning = true
            if (backSeek){
                if (seekToMs < vm.player.currentPosition){
                    vm.player.pause()
                    if (seekToMs < 200){
                        READY_FromSeek = true
                        vm.player.seekTo(0)
                    }else if (seekToMs > vm.player.duration - 300){
                        READY_FromSeek = true
                        vm.player.seekTo(vm.player.duration - 300)
                    }else{
                        if (isSeekReady){
                            isSeekReady = false
                            READY_FromSeek = true
                            vm.player.seekTo(seekToMs)
                        }else{
                            if (SECUREINTERVAL_FINGERUP){
                                vm.player.play()
                            }
                        }
                    }
                }
            }else{
                vm.player.pause()
                if (seekToMs < 200){
                    READY_FromSeek = true
                    vm.player.seekTo(0)
                }else if (seekToMs > vm.player.duration - 300){
                    READY_FromSeek = true
                    vm.player.seekTo(vm.player.duration - 300)
                }else{
                    if (isSeekReady){
                        isSeekReady = false
                        READY_FromSeek = true
                        vm.player.seekTo(seekToMs)
                    }else{
                        if (SECUREINTERVAL_FINGERUP){
                            vm.player.play()
                        }
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
        vm.playEnd = false
        if (seekRunnableRunning) return
        videoSeekHandler.post(videoSeek)
    }
    private fun stopVideoSeek() {
        seekRunnableRunning = false
        videoSeekHandler.removeCallbacks(videoSeek)
    }
    //Job:显示通知
    private var showNoticeJob: Job? = null
    private var showNoticeJobLong: Job? = null
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
    //Job:ONDOWN安全期倒计时
    private var secureIntervalJob: Job? = null
    private fun SecureIntervalJob() {
        secureIntervalJob?.cancel()
        secureIntervalJob = lifecycleScope.launch {
            SECUREINTERVAL_ONDOWN = true
            delay(200)
            SECUREINTERVAL_ONDOWN = false
        }
    }
    //Job:FINGER_UP安全期倒计时
    private var secureIntervalJobFingerUp: Job? = null
    private fun SecureIntervalJobFingerUp() {
        secureIntervalJobFingerUp?.cancel()
        secureIntervalJobFingerUp = lifecycleScope.launch {
            SECUREINTERVAL_FINGERUP = true
            delay(200)
            SECUREINTERVAL_FINGERUP = false
        }
    }


    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        //启动监听屏幕旋转程序：此处监听器关闭,工作暂时转移到onCreate的OrientationEventListener2
        OrientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                //orientationChange(orientation)
            }
        }.apply {
            disable()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //数值计算
        DESTROY_FromSavedInstance = true  //给onDestroy判断是否真的退出
        //保存原始Intent
        outState.putParcelable("CONTENT_INTENT", intent)
    }

    override fun onPause() {
        super.onPause()
        ONSTOP_WasPlaying = vm.player.isPlaying
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
        observerOnStoped = false
        observerOnStarted = false
        requestAudioFocus()
        if (PREFS_RC_LinkScrollEnabled && LIFE_ONSTOP_WasPlaying) {
            LIFE_ONSTOP_WasPlaying = false
            startScrollerSync()
            startVideoTimeSync()
            vm.player.play()
        }else if (ONSTOP_WasPlaying) {
            ONSTOP_WasPlaying = false
            startScrollerSync()
            startVideoTimeSync()
            vm.player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //旋转屏幕
        if (DESTROY_FromSavedInstance){
            stopVideoSeek()
            stopVideoSmartScroll()
            stopVideoTimeSync()
            stopScrollerSync()
        }
        //真正退出
        else{
            stopBackgroundServices()
            PlayerExoSingleton.releasePlayer()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                originalVolume = currentVolume
                false
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        if (newIntent?.action != null){
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            finish()
            startActivity(newIntent)
        }
    }


    //Functions
    private fun AppBarSetting() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets -> WindowInsetsCompat.CONSUMED }
                    window.decorView.post {
                        window.insetsController?.let { controller ->
                            controller.hide(WindowInsets.Type.statusBars())
                            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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


            //控件位置动态调整
            val display = windowManager.defaultDisplay
            val rotation = display?.rotation
            //控件位置动态调整:正向横屏
            if (rotation == Surface.ROTATION_90) {
                val buttonExit = findViewById<ImageButton>(R.id.buttonExit)
                val CONTROLLER_CurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
                (buttonExit.layoutParams as FrameLayout.LayoutParams).marginStart = (150)
                (CONTROLLER_CurrentTime.layoutParams as ConstraintLayout.LayoutParams).marginStart = (200)
            }
            //控件位置动态调整:反向横屏
            else if (rotation == Surface.ROTATION_270) {
                val mediumActions = findViewById<ConstraintLayout>(R.id.MediumActionsContainer)
                (mediumActions.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (200)
            }


            //始终使用深色横屏页
            if (PREFS_S_UseBlackScreenInLandscape) {
                setBlackScreenInLandscape()
            }
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val buttonExit = findViewById<View>(R.id.buttonExit)
            (buttonExit.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (statusBarHeight + 8)
        }
    }
    //方向回调:逻辑改进onCreate,但此处需要保留
    @SuppressLint("SourceLockedOrientationActivity")
    private fun orientationChange(orientation: Int){
        //手动更改控件位置功能已无奈取消
        //把方向角数值映射为状态量
        if (orientation > 260 && orientation < 280) { vm.OrientationValue = 1 }
        else if (orientation > 80 && orientation < 100) { vm.OrientationValue = 2 }
        else if (orientation > 340 && orientation < 360) { vm.OrientationValue = 0 }
        //进入锁
        orientationChangeTime = System.currentTimeMillis()
        if (orientationChangeTime - LastOrientationChangeTime < 1) { return }
        LastOrientationChangeTime = orientationChangeTime
        //读取自动旋转状态
        rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        //自动旋转开启
        if (rotationSetting == 1){
            //当前为竖屏
            if (vm.currentOrientation == 0){
                //标准横屏ORIENTATION_LANDSCAPE
                if (vm.OrientationValue == 1) {
                    if (vm.Manual && vm.LastLandscapeOrientation == 1) return
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
                //反向横屏ORIENTATION_REVERSE_LANDSCAPE
                else if (vm.OrientationValue == 2) {
                    if (vm.Manual && vm.LastLandscapeOrientation == 2) return
                    vm.currentOrientation = 2
                    vm.LastLandscapeOrientation = 2
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }
            }
            //当前为正向横屏
            else if (vm.currentOrientation == 1){
                //反向横屏ORIENTATION_REVERSE_LANDSCAPE
                if (vm.OrientationValue == 2) {
                    vm.currentOrientation = 2
                    vm.LastLandscapeOrientation = 2
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }
                //竖屏ORIENTATION_PORTRAIT
                else if (vm.OrientationValue == 0) {
                    if (vm.Manual) return
                    vm.currentOrientation = 0
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                }
            }
            //当前为反向横屏
            else if (vm.currentOrientation == 2){
                //标准横屏ORIENTATION_LANDSCAPE
                if (vm.OrientationValue == 1) {
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
                //竖屏ORIENTATION_PORTRAIT
                else if (vm.OrientationValue == 0) {
                    if (vm.Manual) return
                    vm.currentOrientation = 0
                    vm.setAuto()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                }
            }
        }
        //自动旋转关闭
        else if (rotationSetting == 0){
            if (!vm.FromManualPortrait){
                //转动到正向横屏ORIENTATION_REVERSE_LANDSCAPE
                if (vm.OrientationValue == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
                //转动到反向横屏ORIENTATION_REVERSE_LANDSCAPE
                else if (vm.OrientationValue == 2) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }
            }
        }
    }
    //切换横屏
    @SuppressLint("SourceLockedOrientationActivity")
    private fun ButtonChangeOrientation(){
        //自动旋转关闭
        if (rotationSetting == 0){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (vm.OrientationValue == 1){
                    vm.FromManualPortrait = false
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }else if (vm.OrientationValue == 2){
                    vm.FromManualPortrait = false
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }else{
                    vm.FromManualPortrait = false
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.FromManualPortrait = true
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            }
        }
        //自动旋转开启
        else if (rotationSetting == 1){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (vm.OrientationValue == 1){
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setManual()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }else if (vm.OrientationValue == 2){
                    vm.currentOrientation = 2
                    vm.LastLandscapeOrientation = 2
                    vm.setManual()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }else{
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setManual()
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
            }else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.currentOrientation = 0
                vm.setManual()
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            }
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun ExitByOrientation(){
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            vm.setManual()
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            //vm.player.release()
            finish()
        }
    }
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
    //设置:横屏时一律使用黑色背景
    private fun setBlackScreenInLandscape(){
        val playerView = findViewById<View>(R.id.playerView)
        val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
        val scrollerContainer = findViewById<View>(R.id.scrollerContainer)
        val buttonExit = findViewById<ImageButton>(R.id.buttonExit)
        val seekStop = findViewById<TextView>(R.id.tvCurrentTime)
        playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        scrollerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.BlackGrey))
        recyclerView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))
        buttonExit.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.White))
        seekStop.setTextColor(ContextCompat.getColor(this, R.color.White))
    }
    //开启后台播放服务
    private fun startBackgroundServices(){
        val intent = Intent(this, PlayerBackgroundServices::class.java)
        //传递媒体信息
        intent.putExtra("MEDIA_TITLE", STRING_FileName)
        //正式开启服务
        startService(intent)
    }
    //关闭后台播放服务
    private fun stopBackgroundServices(){
        stopService(Intent(this, PlayerBackgroundServices::class.java))
    }
    //后台播放只播音轨
    private fun playerSelectSoundTrack(){
        if (PREFS_S_CloseVideoTrack){
            vm.selectAudioOnly()
        }
    }
    //回到前台恢复所有轨道
    private fun playerRecoveryAllTrack(){
        if (PREFS_S_CloseVideoTrack){
            vm.recoveryAllTrack()
        }
    }
    //生命周期监听:判断应用是否退出
    private val AppForegroundObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            if (observerOnStarted){ return }
            observerOnStarted = true
            OrientationEventListener2?.enable()
            if (PREFS_RC_BackgroundPlay) {
                playerRecoveryAllTrack()
            }
            if (PREFS_RC_LinkScrollEnabled && vm.player.isPlaying) startScrollerSync()
        }
        override fun onStop(owner: LifecycleOwner) {
            if (observerOnStoped){ return }
            observerOnStoped = true
            OrientationEventListener2?.disable()
            LIFE_ONSTOP_WasPlaying = vm.player.isPlaying
            if (PREFS_RC_BackgroundPlay) {
                playerSelectSoundTrack()
            }else{
                vm.player.pause()
            }
        }
    }
    //耳机插入和拔出监听
    private val DeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val relevant = removedDevices.filter {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (relevant.isNotEmpty()) {
                vm.player.pause()
                headSet = false
            }
        }
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val relevant = addedDevices.filter {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            if (relevant.isNotEmpty()) {
                headSet = true
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume >= (maxVolume*0.6).toInt()){
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    notice("检测到耳机插入,音量已限制为${(maxVolume*0.6).toInt()}", 1000)
                }
            }
        }
    }
    //音频焦点
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private fun requestAudioFocus(){
        val AudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    INTERUPT_WasPlaying = vm.player.isPlaying
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    INTERUPT_WasPlaying = vm.player.isPlaying
                    vm.player.pause()
                    }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    INTERUPT_WasPlaying = vm.player.isPlaying
                    vm.player.pause()
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (INTERUPT_WasPlaying){
                        INTERUPT_WasPlaying = false
                        vm.player.play()
                    }
                }
            }
        }
        audioManager.requestAudioFocus(AudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }
    //音量淡入淡出
    private fun volumeRec(){
        volumeRecExecuted = true
        lifecycleScope.launch(Dispatchers.Main) {
            for (i in 1..10) {
                val volume = i/10f
                vm.player.volume = volume
                if (i == 10){
                    volumeRecExecuted = false
                }else{
                    Thread.sleep(100)
                }
            }
        }
    }
    //保底Seek机制
    private fun LastSeek(){
        if (LastSeekLaunched) return
        lifecycleScope.launch {
            SECUREINTERVAL_SEEKONCE = true
            delay(200)
            SECUREINTERVAL_SEEKONCE = false
        }

        LastSeekLaunched = true
        READY_FromSeek = false
        val thumbScroller = findViewById<RecyclerView>(R.id.rvThumbnails)
        thumbScroller.stopScroll()
        stopVideoSeek()
        val totalWidthOnce = thumbScroller.computeHorizontalScrollRange()
        val offsetOnce     = thumbScroller.computeHorizontalScrollOffset()
        val percentOnce    = offsetOnce.toFloat() / totalWidthOnce
        val seekToMsOnce   = (percentOnce * vm.player.duration).toLong()
        READY_FromLastSeek = true
        //根据滑动状态确定使用精确帧还是关键帧
        if (STATE_FingerTouching){
            vm.player.setSeekParameters(SeekParameters.EXACT)
        }else{
            vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        }
        vm.player.seekTo(seekToMsOnce)
    }
    //手指抬起事件
    private fun scrollerStoppingTapUp(){
        STATE_FingerTouching = false
        //开启安全期倒计时
        lifecycleScope.launch {
            SecureIntervalJobFingerUp()
        }
        if (!PREFS_RC_LinkScrollEnabled) return
        if (isSeekReady){
            if (wasPlaying){
                playVideo()
            }
        }
    }

    private fun playerReady(){
        if (READY_FromFirstEntry) {
            READY_FromFirstEntry = false
            requestAudioFocus()
            playVideo()
            val playerError = findViewById<LinearLayout>(R.id.playerError)
            playerError.visibility = View.GONE
            val cover = findViewById<View>(R.id.cover)
            cover.animate().alpha(0f).setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { cover.visibility = View.GONE }
                .start()
        }
        if (READY_FromSeek){
            READY_FromSeek = false
            isSeekReady = true
            if (!scrolling){
                if (!LastSeekLaunched){ LastSeek() } //直接触发
                return
            } else {
                lifecycleScope.launch {
                    delay(100)
                    if (!scrolling){
                        if (!LastSeekLaunched){ LastSeek() }  //第一次保底
                        return@launch
                    }else{
                        delay(200)
                        if (!scrolling){
                            if (!LastSeekLaunched){ LastSeek() } //第二次保底
                            return@launch
                        }else{
                            delay(200)
                            if (!scrolling){
                                if (!LastSeekLaunched){ LastSeek() } //第三次保底
                                return@launch
                            }
                        }
                    }
                }
            }
            return
        }
        if (READY_FromLastSeek){
            lifecycleScope.launch(Dispatchers.Main) {
                delay(200)
                LastSeekLaunched = false
            }
            READY_FromLastSeek = false
            isSeekReady = true
            if (wasPlaying) {
                playVideo()
            }
            return
        }
    }

    private fun playerEnd(){
        notice("视频结束",1000)
        vm.player.pause()
        vm.playEnd = true
        stopVideoTimeSync()
        stopScrollerSync()
        if (PREFS_S_ExitWhenEnd){
            finish()
        }
        if (PREFS_RC_LoopPlay){
            vm.player.seekTo(0)
        }
    }

    private fun pauseVideo(){
        vm.player.pause()
        stopVideoTimeSync()
        stopScrollerSync()
    }

    private fun playVideo(){
        requestAudioFocus()
        vm.player.setPlaybackSpeed(1f)
        vm.player.play()
        if (PREFS_RC_LinkScrollEnabled && !scrollerTouching && !scrolling){ startScrollerSync() }
        lifecycleScope.launch {
            delay(100)
            startVideoTimeSync()
        }
    }
    //检查通知权限
    private fun checkNotificationPermission(){
        val areEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!areEnabled) {
            Toast.makeText(this, "后台播放服务需要利用通知保活,请先开启通知权限", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }
    }

    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
        var absolutePath: String? = null

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    absolutePath = it.getString(columnIndex)
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            absolutePath = uri.path
        }

        if (absolutePath != null && File(absolutePath).exists()) {
            return absolutePath
        }
        return null
    }

    private fun buttonRefresh(){
        val PauseImage = findViewById<ImageView>(R.id.PauseImage)
        val ContinueImage = findViewById<ImageView>(R.id.ContinueImage)
        if (vm.player.isPlaying){
            PauseImage.visibility = View.VISIBLE
            ContinueImage.visibility = View.GONE
        }
        else{
            PauseImage.visibility = View.GONE
            ContinueImage.visibility = View.VISIBLE
        }
    }

    private fun preCheck(){
        //获取自动旋转状态
        rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        //兼容性检查
        isCompatibleDevice = isCompatibleDevice()
        //屏幕方向检查
        val buttonMaterialSwitchLandscape = findViewById<MaterialButton>(R.id.buttonMaterialSwitchLandscape)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            vm.FromManualPortrait = false
            vm.currentOrientation = 1
            buttonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBg))
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            vm.FromManualPortrait = true
            vm.currentOrientation = 0
            buttonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        }
        //亮度
        val windowInfo = window.attributes
        if (!vm.BrightnessChanged) {
            vm.BrightnessChanged = true
            var initBrightness = windowInfo.screenBrightness
            if (initBrightness < 0) {
                initBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                vm.BrightnessValue = initBrightness
                windowInfo.screenBrightness = initBrightness
                window.attributes = windowInfo
            }
        }else{
            windowInfo.screenBrightness = vm.BrightnessValue
            window.attributes = windowInfo
            Log.i("SuMing", "已设置亮度 vm.BrightnessValue: ${vm.BrightnessValue}")
        }
        //音量
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumnChangeGap = 750/maxVolume
        if (originalVolume == 0 && !vm.NoVolumeNoticed) {
            vm.NoVolumeNoticed = true
            notice("当前音量为0", 3000)
        }
        //检查是否有耳机连接
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val isHeadsetConnected = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        if (isHeadsetConnected) {
            headSet = true
            if (currentVolume > (maxVolume*0.6).toInt()){
                currentVolume = (maxVolume*0.6).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                notice("检测到耳机佩戴,音量已降低至${(maxVolume*0.6).toInt()}", 1000)
            }
        } else {
            headSet = false
        }
    }
    //格式化时间显示
    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        //val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = milliseconds % 1000
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