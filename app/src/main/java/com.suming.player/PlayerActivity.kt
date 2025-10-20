package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.MoreExecutors
import data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@UnstableApi
//@Suppress("unused")
class PlayerActivity: AppCompatActivity(){
    //变量初始化
    //<editor-fold desc="变量初始化">
    //视频信息预读
    private var videoDuration = 0
    private var absolutePath = ""
    private lateinit var videoUri: Uri
    //播放状态
    private var wasPlaying = true
    //音量配置参数
    private var maxVolume = 0
    private var currentVolume = 0
    private var originalVolume = 0
    //缩略图绘制参数
    private var SCROLLERINFO_MaxPicNumber = 20
    private var SCROLLERINFO_EachPicWidth = 0
    private var SCROLLERINFO_PicNumber = 0
    private var SCROLLERINFO_EachPicDuration: Int = 0
    //点击和滑动状态标识 + onScrolled回调参数
    private var onDown = false
    private var dragging = false
    private var fling =false
    private var singleTap = false
    private var scrolling = false
    private var scrollerTouching = false
    private var videoTimeTo = 0L
    private var STATE_FingerTouching = false
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
    private var isSeekReady = true
    private var backSeek = false
    //判断体:PlayerReady(播放状态)
    private var READY_FromFirstEntry = true
    private var READY_FromSeek = false
    private var READY_FromLastSeek = false
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

    //自动旋转状态
    private var rotationSetting = 0
    //PlayerReady
    private var STATE_PlayerReady = false
    //音量变化步长
    private var volumeChangeGap = 1
    //滑动手势
    private var longPress = false
    private var touchLeft = false
    private var touchRight = false
    private var scrollDistance = 0
    //方向回调
    private var orientationChangeTime = 0L
    private var LastOrientationChangeTime = 0L
    //音量恢复
    private var volumeRecExecuted = false
    //耳机链接状态
    private var headSet = false
    //方向监听器初始化
    private lateinit var OEL: OrientationEventListener

    //视频播放状态监听器
    private var PlayerStateListener: Player.Listener? = null

    //ViewModel
    private val vm: PlayerViewModel by viewModels { PlayerExoFactory.getInstance(application) }
    //视频尺寸
    private var videoSizeWidth = 0
    private var videoSizeHeight = 0

    private lateinit var playerView: PlayerView


    private lateinit var equalizer: Equalizer

    //空闲定时器
    private var IDLE_Timer: CountDownTimer? = null
    private val IDLE_MS = 5_000L


    private var EnterAnimationComplete = false

    private var fps = 0f



    //</editor-fold>

    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx","DEPRECATION")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player)

        //其他预设
        preCheck()


        //恢复暂存数据
        if (savedInstanceState == null) {
            PlayerExoSingleton.releasePlayer()    //初次打开:关闭播放器实例
            stopBackgroundServices()              //初次打开:关闭后台播放服务
        }else{
            //取出Intent
            intent = savedInstanceState.getParcelable("CONTENT_INTENT")
        }


        //设置项读取,检查和预置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if(savedInstanceState == null){
            val prefsEditor = prefs.edit()
            if (!prefs.contains("PREFS_GenerateThumbSYNC")) {
                prefsEditor.putBoolean("PREFS_GenerateThumbSYNC", true)
                vm.PREFS_GenerateThumbSYNC = true
            } else {
                vm.PREFS_GenerateThumbSYNC = prefs.getBoolean("PREFS_GenerateThumbSYNC", true)
            }
            if (!prefs.contains("PREFS_TapJump")) {
                prefsEditor.putBoolean("PREFS_TapJump", false)
                vm.PREFS_TapJump = false
            } else {
                vm.PREFS_TapJump = prefs.getBoolean("PREFS_TapJump", false)
            }
            if (!prefs.contains("PREFS_LinkScroll")) {
                prefsEditor.putBoolean("PREFS_LinkScroll", true)
                vm.PREFS_LinkScroll = true
            } else {
                vm.PREFS_LinkScroll = prefs.getBoolean("PREFS_LinkScroll", false)
            }
            if (!prefs.contains("PREFS_AlwaysSeek")) {
                prefsEditor.putBoolean("PREFS_AlwaysSeek", true)
                vm.PREFS_AlwaysSeek = true
            } else {
                vm.PREFS_AlwaysSeek = prefs.getBoolean("PREFS_AlwaysSeek", false)
            }
            if (!prefs.contains("PREFS_BackgroundPlay")) {
                prefsEditor.putBoolean("PREFS_BackgroundPlay", false)
                vm.PREFS_BackgroundPlay = false
            } else {
                vm.PREFS_BackgroundPlay = prefs.getBoolean("PREFS_BackgroundPlay", false)
            }
            if (!prefs.contains("PREFS_LoopPlay")) {
                prefsEditor.putBoolean("PREFS_LoopPlay", false)
                vm.PREFS_LoopPlay = false
                vm.player.repeatMode = Player.REPEAT_MODE_OFF
            } else {
                vm.PREFS_LoopPlay = prefs.getBoolean("PREFS_LoopPlay", false)
                if (vm.PREFS_LoopPlay) {
                    vm.player.repeatMode = Player.REPEAT_MODE_ONE
                } else {
                    vm.player.repeatMode = Player.REPEAT_MODE_OFF
                }
            }
            if (!prefs.contains("PREFS_ExitWhenEnd")) {
                prefsEditor.putBoolean("PREFS_ExitWhenEnd", false)
                vm.PREFS_ExitWhenEnd = false
            } else {
                vm.PREFS_ExitWhenEnd = prefs.getBoolean("PREFS_ExitWhenEnd", false)
            }
            if (!prefs.contains("PREFS_UseLongScroller")) {
                prefsEditor.putBoolean("PREFS_UseLongScroller", false)
                vm.PREFS_UseLongScroller = false
            } else {
                vm.PREFS_UseLongScroller = prefs.getBoolean("PREFS_UseLongScroller", false)
            }
            if (!prefs.contains("PREFS_UseLongSeekGap")) {
                prefsEditor.putBoolean("PREFS_UseLongSeekGap", false)
                vm.PREFS_UseLongSeekGap = false
            } else {
                vm.PREFS_UseLongSeekGap = prefs.getBoolean("PREFS_UseLongSeekGap", false)
                if (vm.PREFS_UseLongSeekGap) {
                    forceSeekGap = 20000L
                }
            }
            if (!prefs.contains("PREFS_UseBlackBackground")) {
                prefsEditor.putBoolean("PREFS_UseBlackBackground", false)
                vm.PREFS_UseBlackBackground = false
            } else {
                vm.PREFS_UseBlackBackground =
                    prefs.getBoolean("PREFS_UseBlackBackground", false)
            }
            if (!prefs.contains("PREFS_UseHighRefreshRate")) {
                prefsEditor.putBoolean("PREFS_UseHighRefreshRate", false)
                vm.PREFS_UseHighRefreshRate = false
            } else {
                vm.PREFS_UseHighRefreshRate = prefs.getBoolean("PREFS_UseHighRefreshRate", false)
            }
            if (!prefs.contains("PREFS_UseCompatScroller")) {
                prefsEditor.putBoolean("PREFS_UseCompatScroller", false)
                vm.PREFS_UseCompatScroller = false
            } else {
                vm.PREFS_UseCompatScroller = prefs.getBoolean("PREFS_UseCompatScroller", false)
            }
            if (!prefs.contains("PREFS_CloseVideoTrack")) {
                prefsEditor.putBoolean("PREFS_CloseVideoTrack", true)
                vm.PREFS_CloseVideoTrack = true
            } else {
                vm.PREFS_CloseVideoTrack = prefs.getBoolean("PREFS_CloseVideoTrack", false)
            }
            if (!prefs.contains("PREFS_SealOEL")) {
                prefsEditor.putBoolean("PREFS_SealOEL", false)
                vm.PREFS_SealOEL = false
            } else {
                vm.PREFS_SealOEL = prefs.getBoolean("PREFS_SealOEL", false)
            }
            if (!prefs.contains("INFO_STATUSBAR_HEIGHT")) {
                vm.statusBarHeight = 200
            } else {
                vm.statusBarHeight = prefs.getInt("INFO_STATUSBAR_HEIGHT", 0)
            }
            prefsEditor.apply()
        }

        //区分打开方式并反序列化
        val videoItem: VideoItem? = when (intent?.action) {

            Intent.ACTION_SEND -> {
                vm.PREFS_ExitWhenEnd = true
                val uri =
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        ?: return finish()
                VideoItem(0, uri, "", 0)
            }

            Intent.ACTION_VIEW -> {
                vm.PREFS_ExitWhenEnd = true
                val uri = intent.data ?: return finish()
                VideoItem(0, uri, "", 0)
            }

            else ->  {
                vm.PREFS_ExitWhenEnd = false
                IntentCompat.getParcelableExtra(intent, "video", VideoItem::class.java)
            }
        }
        if (videoItem == null) {
            val intentBackToMainActivity = Intent(this, MainActivity::class.java)
            intentBackToMainActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intentBackToMainActivity)
            finish()
            return
        }
        videoUri = videoItem.uri

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this@PlayerActivity, videoUri)

        if (vm.PREFS_UseBlackBackground) {
            vm.ShouldUseBlackBackground = true
            setPageToDark()
        }



        //方向监听器
        OEL = object : OrientationEventListener(this) {
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
                        //从 竖屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 1) return
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //从 竖屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 2) return
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                    }
                    //当前为正向横屏
                    else if (vm.currentOrientation == 1){
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 2) {
                            //按钮避让:横排按钮区&更多选项按钮
                            val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)
                            (ButtonArea1.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (150)
                            val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
                            (TopBarArea_ButtonMoreOptions.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = (150)
                            //更改状态并发起旋转
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                        //从 正向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                    }
                    //当前为反向横屏
                    else if (vm.currentOrientation == 2){
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            //按钮避让时间框&退出按钮
                            val TopBarArea_ButtonExit = findViewById<View>(R.id.TopBarArea_ButtonExit)
                            val ControllerArea_VideoCurrentTimeCard = findViewById<CardView>(R.id.VideoCurrentTimeCard)
                            (TopBarArea_ButtonExit.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
                            (ControllerArea_VideoCurrentTimeCard.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
                            //更改状态并发起旋转
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //从 反向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                    }
                }
                //自动旋转关闭
                else if (rotationSetting == 0){
                    if (!vm.FromManualPortrait){
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            //按钮避让时间框&退出按钮
                            val TopBarArea_ButtonExit = findViewById<View>(R.id.TopBarArea_ButtonExit)
                            (TopBarArea_ButtonExit.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
                            val ControllerArea_VideoCurrentTimeCard = findViewById<CardView>(R.id.VideoCurrentTimeCard)
                            (ControllerArea_VideoCurrentTimeCard.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
                            //更改状态并发起旋转
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            //按钮避让:横排按钮区&更多选项按钮
                            val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)
                            (ButtonArea1.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (150)
                            val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
                            (TopBarArea_ButtonMoreOptions.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = (150)
                            //更改状态并发起旋转
                            vm.onOrientationChanging = true
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                    }
                }
            }
        }
        if (vm.PREFS_SealOEL){
            OEL.disable()
        }else{
            OEL.enable()
        }


        //界面初始化:默认颜色设置+控件动态变位
        AppBarSetting()


        val vm = ViewModelProvider(this, PlayerExoFactory.getInstance(application))[PlayerViewModel::class.java]

        //刷新率强制修改
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vm.PREFS_UseHighRefreshRate) {
            val wm = windowManager
            val mode = wm.defaultDisplay.mode
            val fps = mode.refreshRate
            window.attributes = window.attributes.apply {
                preferredRefreshRate = fps
            }
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
                    if (vm.PREFS_LinkScroll) startScrollerSync()
                }
                if (data == "PLAYER_PAUSE") {
                    wasPlaying = vm.player.isPlaying
                    vm.player.pause()
                }
            }
        }
        localBroadcastManager.registerReceiver(receiver, filter)

        




        //绑定播放器输出
        playerView = findViewById(R.id.playerView)
        playerView.player = vm.player

        //初次打开时传递视频链接
        if (savedInstanceState == null) {
            vm.setVideoUri(videoUri)
        } else {
            buttonRefresh()
            val cover = findViewById<View>(R.id.cover)
            cover.visibility = View.GONE
        }

        //播放器事件监听
        PlayerStateListener = object : Player.Listener {
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
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoSizeWidth = videoSize.width
                    videoSizeHeight = videoSize.height

                    /*
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                        if(videoSizeHeight.toFloat() / videoSizeWidth < 1.5){
                            setBlackScreen()
                        }
                    }
                    else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                        if(videoSizeHeight.toFloat() / videoSizeWidth > 1.3){
                            setBlackScreen()
                        }
                    }

                     */
                }

            }
            override fun onTracksChanged(tracks: Tracks) {
                for (trackGroup in tracks.groups) {
                    val format = trackGroup.getTrackFormat(0)
                    fps = format.frameRate
                    break
                }
            }
        }
        vm.player.addListener(PlayerStateListener!!)






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
        val Controller_ThumbScroller = findViewById<RecyclerView>(R.id.rvThumbnails)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val sidePadding = screenWidth / 2

        val videoUri = videoItem.uri
        absolutePath = getAbsoluteFilePath(this@PlayerActivity, videoUri).toString()
        Controller_ThumbScroller.layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
        //信息读取
        STRING_VideoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        STRING_VideoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        val file = File(absolutePath)
        STRING_FileName = file.name
        //视频无法打开时的处理
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
        Controller_ThumbScroller.itemAnimator = null
        Controller_ThumbScroller.layoutParams.width = 0
        videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0

        /*
        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

        if (vm.PREFS_UseBlackBackground) {
            vm.ShouldUseBlackBackground = true
            setPageToDark()
        }else{
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (videoSizeWidth.toFloat() / videoSizeHeight > 1.8){
                    setPageToDark()
                    vm.ShouldUseBlackBackground = true
                }
            }else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (videoSizeHeight.toFloat() / videoSizeWidth > 1.8){
                    setPageToDark()
                    vm.ShouldUseBlackBackground = true
                }
            }

        }

         */



        val tvWholeTime = findViewById<TextView>(R.id.tvWholeTime)
        tvWholeTime.text = formatTime1(videoDuration.toLong())

        retriever.release()


        //进度条绘制
        if (vm.PREFS_UseLongScroller) {
            SCROLLERINFO_EachPicWidth = (47 * displayMetrics.density).toInt()
            if (videoDuration > 1_0000_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 500.0).toInt()
                SCROLLERINFO_PicNumber = 500
            }
            else if (videoDuration > 7500_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 400.0).toInt()
                SCROLLERINFO_PicNumber = 400
            }
            else if (videoDuration > 5000_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 300.0).toInt()
                SCROLLERINFO_PicNumber = 300
            }
            else if (videoDuration > 500_000L) {
                SCROLLERINFO_EachPicDuration = (videoDuration / 200.0).toInt()
                SCROLLERINFO_PicNumber = 200
            }
            else {
                SCROLLERINFO_EachPicDuration = 1000
                SCROLLERINFO_PicNumber = min((max((videoDuration / 1000), 1)), 500)
            }
        } //使用超长进度条
        else {
            syncScrollRunnableGap = ((videoDuration / 1000) * (1000.0 / 3600)).toLong()
            if (videoDuration / 1000 > SCROLLERINFO_MaxPicNumber) {
                SCROLLERINFO_EachPicWidth = (40 * displayMetrics.density).toInt()
                SCROLLERINFO_EachPicDuration = (videoDuration.div(100) * 100) / SCROLLERINFO_MaxPicNumber
                SCROLLERINFO_PicNumber = SCROLLERINFO_MaxPicNumber
            } else {
                SCROLLERINFO_EachPicWidth = (40 * displayMetrics.density).toInt()
                SCROLLERINFO_PicNumber = (videoDuration / 1000) + 1
                SCROLLERINFO_EachPicDuration = (videoDuration.div(100) * 100) / SCROLLERINFO_PicNumber
            }
        }  //使用普通进度条
        //进度条端点处理
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Controller_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var scrollerMarginType: Int
            if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor") {
                scrollerMarginType = 2
                Controller_ThumbScroller.setPadding(
                    sidePadding + vm.statusBarHeight / 2,
                    0,
                    sidePadding + vm.statusBarHeight / 2 - 1,
                    0
                )
            }
            else if (Build.BRAND == "samsung") {
                scrollerMarginType = 1
                Controller_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }
            else {
                scrollerMarginType = 1
                Controller_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }
            if (vm.PREFS_UseCompatScroller) {
                if (scrollerMarginType == 2) {
                    Controller_ThumbScroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                }else {
                    Controller_ThumbScroller.setPadding(sidePadding + vm.statusBarHeight / 2, 0, sidePadding + vm.statusBarHeight / 2 - 1, 0)
                }
            }
        }




        //gestureDetectorScroller -onSingleTap -onDown
        val gestureDetectorScroller = GestureDetector(this, object : SimpleOnGestureListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                singleTap = true
                if (!vm.PREFS_TapJump) {
                    if (vm.PREFS_LinkScroll) {
                        notice("未开启单击跳转,如需跳转请先开启,或关闭链接滚动", 1000)
                        return false
                    }
                }
                //根据百分比计算具体跳转时间点
                val totalContentWidth = Controller_ThumbScroller.computeHorizontalScrollRange()
                val scrolled = Controller_ThumbScroller.computeHorizontalScrollOffset()
                val leftPadding = Controller_ThumbScroller.paddingLeft
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
                    if (!vm.PREFS_LinkScroll) {
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
                if (!vm.PREFS_LinkScroll) return false
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
        Controller_ThumbScroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                scrollerTouching = true
                if (e.action == MotionEvent.ACTION_UP) {
                    scrollerTouching = false
                    scrollerStoppingTapUp()
                    startIdleTimer()
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
        Controller_ThumbScroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                if (vm.PREFS_LinkScroll) {
                    val percentScroll = recyclerView.computeHorizontalScrollOffset().toFloat() / Controller_ThumbScroller.computeHorizontalScrollRange()
                    videoTimeTo = (percentScroll * vm.player.duration).toLong()
                    currentTime = videoTimeTo
                    CONTROLLER_CurrentTime.text = formatTime1(videoTimeTo)
                } else {
                    return
                } //时间戳跟随进度条
                stopVideoTimeSync()
                stopScrollerSync()
                if (dx > 0) {
                    if (vm.PREFS_AlwaysSeek) {
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




        //固定控件初始化：退出按钮:短按退出,长按开小窗
        val gestureDetectorExitButton = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                PlayerExoSingleton.stopPlayer()
                stopFloatingWindow()
                finish()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                startFloatingWindow()
                super.onLongPress(e)
            }
        })
        val buttonExit = findViewById<ImageButton>(R.id.TopBarArea_ButtonExit)
        buttonExit.setOnTouchListener { _, event ->
            gestureDetectorExitButton.onTouchEvent(event)
        }
        //更多选项
        val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
        TopBarArea_ButtonMoreOptions.setOnClickListener {
            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.NoticeCard)
        noticeCard.setOnClickListener {
            noticeCard.visibility = View.GONE
        }
        //按钮：返回视频开头
        val buttonBackToStart = findViewById<FrameLayout>(R.id.buttonActualBackToStart)
        val buttonBackToStartMaterial = findViewById<MaterialButton>(R.id.buttonMaterialBackToStart)
        buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonBgClosed))
        buttonBackToStart.setOnClickListener {
            stopVideoSmartScroll()
            lifecycleScope.launch {
                buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivity, R.color.ButtonBg))
                delay(200)
                buttonBackToStartMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivity, R.color.ButtonBgClosed))
            }
            stopVideoSeek()
            Controller_ThumbScroller.stopScroll()
            if (vm.PREFS_LinkScroll) startScrollerSync()
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
                lifecycleScope.launch {
                    Controller_ThumbScroller.stopScroll()
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
        //按钮：切换横屏
        val buttonSwitchLandscape = findViewById<FrameLayout>(R.id.buttonActualSwitchLandscape)
        buttonSwitchLandscape.setOnClickListener {
            ButtonChangeOrientation()
        }
        //按钮：更多选项
        val buttonMoreOptions = findViewById<FrameLayout>(R.id.buttonActualMoreButton)
        buttonMoreOptions.setOnClickListener {
            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
        }
        //播放区域点击事件

        var STATE_2Fingers = false
        var ACTION_POINTER_DOWN = false

        var originalDistance = 0f
        var distanceGap: Float

        var center0x = 0f
        var center0y = 0f

        var center1x = 0f
        var center1y = 0f

        var originalScale = 1f
        var scale: Double
        var definiteScale = 1.0f



        var center2x: Float
        var center2y: Float

        var center0pivoted = false

        var finger1x = 0f
        var finger1y = 0f
        var finger2x: Float
        var finger2y: Float
        

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
                if (ACTION_POINTER_DOWN) { return true }
                changeBackgroundColor()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                if (ACTION_POINTER_DOWN) return
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
                    vm.BrightnessChanged = true
                    var newBrightness: Float
                    if (scrollDistance > 50) {

                        newBrightness = (vm.BrightnessValue + 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()

                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 +1 (${(newBrightness*100).toInt()}/100)", 1000)
                        }else{
                            notice("亮度已到上限", 1000)
                        }
                    }else if (scrollDistance < -50){
                        newBrightness = (vm.BrightnessValue - 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 -1 (${(newBrightness*100).toInt()}/100)", 1000)
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
                    if (scrollDistance > volumeChangeGap){
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
                    }else if (scrollDistance< -volumeChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume = currentVolume - 1
                        if (currentVolume >= 0){
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                        }else{
                            notice("音量已到最低", 1000)
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
        val playerTouchPad = findViewById<View>(R.id.playerTouchPad)
        playerTouchPad.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    //变更状态标记
                    ACTION_POINTER_DOWN = false
                    STATE_2Fingers = false
                    //记录1指初始坐标
                    finger1x = event.x
                    finger1y = event.y

                    //屏蔽顶部和底部区域,防止和系统下拉上滑动作冲突
                    if (finger1y < screenHeight * 0.2 || finger1y > screenHeight * 0.95){
                        return@setOnTouchListener false
                    }
                    if (finger1x < screenWidth / 2) {
                        touchLeft = true
                    } else {
                        touchRight = true
                    }
                    gestureDetectorPlayArea.onTouchEvent(event)
                }
                MotionEvent.ACTION_UP -> {
                    STATE_2Fingers = false
                    scrollDistance = 0
                    touchLeft = false
                    touchRight = false

                    center0x = playerView.pivotX
                    center0y = playerView.pivotY

                    playerView.pivotX = center0x
                    playerView.pivotY = center0y


                    originalScale = definiteScale

                    if (longPress) {
                        longPress = false
                        vm.player.setPlaybackSpeed(1.0f)
                        val NoticeCard = findViewById<CardView>(R.id.NoticeCard)
                        NoticeCard.visibility = View.GONE
                    }
                    startIdleTimer()
                    gestureDetectorPlayArea.onTouchEvent(event)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    //取手指2的坐标
                    ACTION_POINTER_DOWN = true
                    val ptrIndex = event.actionIndex
                    finger2x = event.getX(ptrIndex)
                    finger2y = event.getY(ptrIndex)
                    if (event.pointerCount == 2){
                        //notice("双指缩放可缩放播放区域", 2000)
                        //更改标志位
                        STATE_2Fingers = true
                        //计算缩放中心点:只算一次
                        if (!center0pivoted){
                            center0x = (finger1x + finger2x) / 2
                            center0y = (finger1y + finger2y) / 2
                            playerView.pivotX = center0x
                            playerView.pivotY = center0y
                            center0pivoted = true
                        }
                        center1x = (finger1x + finger2x) / 2
                        center1y = (finger1y + finger2y) / 2
                        //计算初始双指距离
                        originalDistance = hypot(finger1x - finger2x, finger1y - finger2y)
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 2){
                        STATE_2Fingers = false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    //双指在滑动
                    if (STATE_2Fingers){
                        finger1x = event.getX(0)
                        finger1y = event.getY(0)
                        finger2x = event.getX(1)
                        finger2y = event.getY(1)

                        //平移
                        center2x = (finger1x + finger2x) / 2
                        center2y = (finger1y + finger2y) / 2
                        val centerGapX = center2x - center1x
                        val centerGapY = center2y - center1y
                        if ( playerView.scaleX <= 1){
                            playerView.pivotX = (center0x + centerGapX)
                            playerView.pivotY = (center0y + centerGapY)
                        }else{
                            playerView.pivotX = (center0x - centerGapX)
                            playerView.pivotY = (center0y - centerGapY)
                        }


                        //缩放
                        val distance = hypot(finger1x - finger2x, finger1y - finger2y)
                        distanceGap = (distance - originalDistance)
                        scale = 4.0.pow(distanceGap / 400.0)
                        definiteScale =  originalScale * scale.toFloat()
                        playerView.scaleX = definiteScale
                        playerView.scaleY = definiteScale

                    }
                    //单指在滑动
                    else{
                        if (!ACTION_POINTER_DOWN){
                            gestureDetectorPlayArea.onTouchEvent(event)
                        }

                    }
                }
            }
            onTouchEvent(event)
        }

        //均衡器页面返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_EQUALIZER", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            if (ReceiveKey == "Add"){
                for (i in 0 until equalizer.numberOfBands) {
                    val gain = +3000
                    equalizer.setBandLevel(i.toShort(), gain.toShort())
                    notice("均衡器 +1(${equalizer.getBandLevel(i.toShort())/1000})", 1000)
                }
            }
            else if (ReceiveKey == "Sub"){
                for (i in 0 until equalizer.numberOfBands) {
                    val gain = -3000
                    equalizer.setBandLevel(i.toShort(), gain.toShort())
                    notice("均衡器 -1(${equalizer.getBandLevel(i.toShort())/1000})", 1000)
                }
            }


        }

        //更多按钮页面返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            if (ReceiveKey == "BackgroundPlay"){
                if (vm.PREFS_BackgroundPlay){
                    prefs.edit { putBoolean("PREFS_BackgroundPlay", true).apply() }
                    notice("已开启后台播放", 1000)
                } else {
                    checkNotificationPermission()
                    prefs.edit { putBoolean("PREFS_BackgroundPlay", false).apply() }
                    notice("已关闭后台播放", 1000)
                }
            }
            else if (ReceiveKey == "LoopPlay"){
                if (vm.player.repeatMode == Player.REPEAT_MODE_OFF){
                    vm.player.repeatMode = Player.REPEAT_MODE_ONE
                    vm.PREFS_LoopPlay = true
                    prefs.edit { putBoolean("PREFS_LoopPlay", true).apply() }
                    notice("已开启单集循环", 1000)
                } else {
                    vm.player.repeatMode = Player.REPEAT_MODE_OFF
                    vm.PREFS_LoopPlay = false
                    prefs.edit { putBoolean("PREFS_LoopPlay", false).apply() }
                    notice("已关闭单集循环", 1000)
                }
            }
            else if (ReceiveKey == "AlwaysSeek"){
                if (vm.PREFS_AlwaysSeek){
                    vm.PREFS_AlwaysSeek = false
                    prefs.edit { putBoolean("PREFS_AlwaysSeek", false).apply() }
                    notice("已关闭AlwaysSeek", 3000)
                } else {
                    vm.PREFS_AlwaysSeek = true
                    prefs.edit { putBoolean("PREFS_AlwaysSeek", true).apply() }
                    notice("已开启AlwaysSeek", 3000)
                }
            }
            else if (ReceiveKey == "TapScroll"){
                if (vm.PREFS_TapJump){
                    vm.PREFS_TapJump = false
                    prefs.edit { putBoolean("PREFS_TapScrolling", false).apply() }
                    notice("已关闭单击跳转", 1000)
                } else {
                    vm.PREFS_TapJump = true
                    prefs.edit { putBoolean("PREFS_TapScrolling", true).apply() }
                    notice("已开启单击跳转", 1000)
                }
            }
            else if (ReceiveKey == "LinkScroll"){
                if (vm.PREFS_LinkScroll) {
                    vm.PREFS_LinkScroll = false
                    prefs.edit { putBoolean("PREFS_LinkScrolling", false).apply() }
                    stopScrollerSync()
                    Controller_ThumbScroller.stopScroll()
                    notice("已关闭链接滚动条与视频进度", 2500)
                } else {
                    vm.PREFS_LinkScroll = true
                    prefs.edit { putBoolean("PREFS_LinkScrolling", true).apply() }
                    notice("已将进度条与视频进度同步", 1000)
                    isSeekReady = true
                    Controller_ThumbScroller.stopScroll()
                    startScrollerSync()
                    stopVideoSeek()
                }
            }
            else if (ReceiveKey == "SealOEL"){
                if (vm.PREFS_SealOEL){
                    prefs.edit { putBoolean("PREFS_SealOEL", true).apply() }
                    OEL.disable()
                    notice("已关闭方向监听器", 1000)
                } else {
                    prefs.edit { putBoolean("PREFS_SealOEL", false).apply() }
                    OEL.enable()
                    notice("已开启方向监听器", 1000)
                }
            }
            else if (ReceiveKey == "OnlyAudio"){
                if (vm.PREFS_OnlyAudio){
                    //确保声音开启
                    vm.player.volume = 1f
                    //仅播放音频：关闭视频轨道
                    vm.selectAudioOnly()
                    notice("已开启仅播放音频", 1000)
                } else {
                    //仅播放音频：打开视频轨道
                    vm.recoveryAllTrack()
                    notice("已关闭仅播放音频", 1000)
                }
            }
            else if (ReceiveKey == "OnlyVideo"){
                if (vm.PREFS_OnlyVideo){
                    //确保视频开启
                    vm.recoveryAllTrack()
                    //仅播放视频：关闭声音
                    vm.player.volume = 0f
                    notice("已开启仅播放视频", 1000)
                } else {
                    vm.player.volume = 1f
                    notice("已关闭仅播放视频", 1000)

                }
            }
            else if (ReceiveKey == "VideoInfo"){
                //读取数据
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(absolutePath)
                val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val videoFps = fps
                val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                val videoMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                val videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

                val videoFileName = STRING_FileName
                val videoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val videoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val videoDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

                //将数据传递给Fragment
                val videoInfoFragment = PlayerFragmentVideoInfo.newInstance(
                    videoWidth?.toInt() ?: 0,
                    videoHeight?.toInt() ?: 0,
                    videoDuration?.toLong() ?: 0,
                    videoFps,
                    captureFps?.toFloat() ?: 0f,
                    videoMimeType ?: "",
                    videoBitrate?.toLong() ?: 0,
                    videoFileName,
                    videoTitle ?: "",
                    videoArtist ?: "",
                    videoDate ?: ""
                )
                videoInfoFragment.show(supportFragmentManager, "PlayerVideoInfoFragment")
            }
            else if (ReceiveKey == "SetSpeed"){
                val dialog = Dialog(this)
                val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
                dialog.setContentView(dialogView)
                val title: TextView = dialogView.findViewById(R.id.dialog_title)
                val Description:TextView = dialogView.findViewById(R.id.dialog_description)
                val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
                val Button: Button = dialogView.findViewById(R.id.dialog_button)

                title.text = "自定义倍速"
                Description.text = "输入您的自定义倍速,最大允许数值为5.0"
                EditText.hint = ""
                Button.text = "确定"

                val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                Button.setOnClickListener {
                    val userInput = EditText.text.toString()
                    if (userInput.isEmpty()){
                        notice("未输入内容", 1000)
                    } else {
                        val inputValue = userInput.toFloat()
                        if(inputValue > 0.0 && inputValue < 5.0){
                            vm.player.setPlaybackSpeed(inputValue)
                            notice("已将倍速设置为$inputValue", 2000)
                        } else {
                            notice("数值过大", 3000)
                        }
                    }
                    dialog.dismiss()
                }
                dialog.show()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(50)
                    EditText.requestFocus()
                    imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            else if (ReceiveKey == "SysShare"){
                shareVideo(this, videoUri)
            }
            else if (ReceiveKey == "Equalizer"){
                equalizer = Equalizer(1, vm.player.audioSessionId)
                equalizer.enabled = true
                PlayerFragmentEqualizer.newInstance().show(supportFragmentManager, "PlayerEqualizerFragment")
            }
        }

        //根据机型选择启用播控中心或自定义通知
        if (savedInstanceState == null) {
            //三星小米可直接使用播控中心
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
            //华为播控中心为白名单,启用自定义控制通知
            else if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor"){
                lifecycleScope.launch {
                    delay(1000)
                    startBackgroundServices()
                }
            }
            //其他机型,默认启用自定义通知
            else{
                lifecycleScope.launch {
                    delay(1000)
                    startBackgroundServices()
                }
            }
        }

        if (vm.controllerHided){
            setControllerInvisibleNoAnimation()
        }

        //开启空闲倒计时
        startIdleTimer()

        //绑定Adapter
        lifecycleScope.launch(Dispatchers.IO) {
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()

            if (SCROLLERINFO_EachPicDuration > 1000){
                vm.PREFS_GenerateThumbSYNC = true
            }

            withContext(Dispatchers.Main) {
                Controller_ThumbScroller.adapter = PlayerScrollerAdapter(this@PlayerActivity,
                    absolutePath,playerScrollerViewModel.thumbItems,SCROLLERINFO_EachPicWidth,SCROLLERINFO_PicNumber,SCROLLERINFO_EachPicDuration,vm.PREFS_GenerateThumbSYNC)
            }

            if(vm.PREFS_LinkScroll){ startScrollerSync() }
            delay(100)
            startVideoTimeSync()
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

            CONTROLLER_CurrentTime.text = formatTime1(currentPosition)

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
            val NoticeCardText = findViewById<TextView>(R.id.NoticeCardText)
            val NoticeCard = findViewById<CardView>(R.id.NoticeCard)
            NoticeCard.visibility = View.VISIBLE
            NoticeCardText.text = text
            delay(duration)
            NoticeCard.visibility = View.GONE
        }
    }
    private fun showNoticeJobLong(text: String) {
        showNoticeJobLong?.cancel()
        showNoticeJobLong = lifecycleScope.launch {
            val NoticeCardText = findViewById<TextView>(R.id.NoticeCardText)
            val NoticeCard = findViewById<CardView>(R.id.NoticeCard)
            NoticeCard.visibility = View.VISIBLE
            NoticeCardText.text = text
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
    //Job:关闭视频轨道倒计时
    private var closeVideoTrackJob: Job? = null
    private fun CloseVideoTrackJob() {
        closeVideoTrackJob?.cancel()
        closeVideoTrackJob = lifecycleScope.launch {
            delay(30_000)
            vm.selectAudioOnly()
            vm.closeVideoTrackJobRunning = false
        }
    }


    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        EnterAnimationComplete = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //保存原始Intent
        outState.putParcelable("CONTENT_INTENT", intent)
    }

    override fun onPause() {
        super.onPause()
        //关闭视频控制
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onStop() {
        super.onStop()
        //退出应用
        if (!vm.onOrientationChanging){
            //关闭旋转监听器
            OEL.disable()
            //记录播放状态
            wasPlaying = vm.player.isPlaying
            //是否后台播放
            if (vm.PREFS_BackgroundPlay) {
                startBackgroundPlay()
            }else{
                vm.player.pause()
            }
        }
        //通用
        stopVideoTimeSync()
        stopScrollerSync()
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onResume() {
        super.onResume()
        //如果关闭视频轨道倒计时正在运行
        if (vm.closeVideoTrackJobRunning){
            vm.closeVideoTrackJobRunning = false
            closeVideoTrackJob?.cancel()
            //重新绑定播放器以防万一
            playerView.player = null
            playerView.player = vm.player
        }
        //onResume来自旋转屏幕
        if (vm.onOrientationChanging){
            vm.onOrientationChanging = false
        }
        //onResume来自桌面进入
        else{
            //开启旋转监听器
            if (!vm.PREFS_SealOEL) { OEL.enable() }
            //后台播放操作:恢复播放状态
            if (wasPlaying) { vm.player.play() }
            //后台播放操作:恢复视频轨道
            if (vm.PREFS_BackgroundPlay && !vm.PREFS_OnlyAudio) {
                stopBackgroundPlay()
            }
            //判断是否需要开启ScrollerSync
            startVideoTimeSync()
            if (vm.PREFS_LinkScroll && vm.player.isPlaying) startScrollerSync()
        }

        //onResume来自浮窗
        if (vm.inFloatingWindow){
            vm.inFloatingWindow = false
            stopFloatingWindow()
            playerView.player = null
            playerView.player = vm.player
        }

        //通用
        requestAudioFocus()

    }

    override fun onDestroy() {
        super.onDestroy()
        //onDestroy来自旋转屏幕
        if (vm.onOrientationChanging){
            stopVideoSeek()
            stopVideoSmartScroll()
            stopVideoTimeSync()
            stopScrollerSync()
        }
        //onDestroy来自退出
        else{
            stopBackgroundServices()
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
    //用户交互监听器
    override fun onUserInteraction() {
        super.onUserInteraction()
        IDLE_Timer?.cancel()
    }


    //Functions
    //分享视频(已知Uri)
    private fun shareVideo(context: Context, videoUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "分享视频")
        context.startActivity(chooser)
    }
    //空闲倒计时
    private fun startIdleTimer() {
        IDLE_Timer?.cancel()
        IDLE_Timer = object : CountDownTimer(IDLE_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() { idleTimeout() }
        }.start()
    }
    private fun idleTimeout() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setControllerInvisible()
        }
    }
    //控件隐藏和显示
    private fun setControllerInvisibleNoAnimation() {
        //状态标记变更
        widgetsShowing = false
        vm.controllerHided = true
        //被控控件控制
        stopScrollerSync()
        stopVideoTimeSync()
        //显示控制
        //<editor-fold desc="显示控制(隐藏)">
        val TopBarArea = findViewById<LinearLayout>(R.id.TopBarArea)
        val ScrollerRootArea = findViewById<View>(R.id.ScrollerRootArea)
        val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)

        ScrollerRootArea.visibility = View.GONE
        ButtonArea1.visibility = View.GONE
        TopBarArea.visibility = View.GONE
        //</editor-fold>
        setBackgroundInvisible()
    }
    private fun setControllerInvisible() {
        //状态标记变更
        widgetsShowing = false
        vm.controllerHided = true
        //被控控件控制
        stopScrollerSync()
        stopVideoTimeSync()
        //显示控制
        //<editor-fold desc="显示控制(隐藏)">
        val TopBarArea = findViewById<LinearLayout>(R.id.TopBarArea)
        val ScrollerRootArea = findViewById<View>(R.id.ScrollerRootArea)
        val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)

        ScrollerRootArea.animate().alpha(0f).setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { ScrollerRootArea.visibility = View.GONE }
            .start()
        ButtonArea1.animate().alpha(0f).setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { ButtonArea1.visibility = View.GONE }
            .start()
        TopBarArea.animate().alpha(0f).setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { TopBarArea.visibility = View.GONE }
            .start()

        //</editor-fold>
        setBackgroundInvisible()
    }
    private fun setControllerVisible() {
        //状态标记变更
        widgetsShowing = true
        vm.controllerHided = false
        //被控控件控制
        if(vm.PREFS_LinkScroll) { startScrollerSync() }
        startVideoTimeSync()
        //显示控制
        //<editor-fold desc="显示控制(显示)">
        val TopBarArea = findViewById<LinearLayout>(R.id.TopBarArea)
        val ScrollerRootArea = findViewById<View>(R.id.ScrollerRootArea)
        val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)

        ScrollerRootArea.visibility = View.VISIBLE
        ButtonArea1.visibility = View.VISIBLE
        TopBarArea.visibility = View.VISIBLE


        ScrollerRootArea.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { null }
            .start()
        ButtonArea1.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { null }
            .start()
        TopBarArea.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { null }
            .start()

        //</editor-fold>
        setBackgroundVisible()
    }
    private fun setBackgroundVisible(){
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        if (vm.ShouldUseBlackBackground){
            playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        }else{
            playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
        }
    }
    private fun setBackgroundInvisible(){
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
    }
    private fun changeBackgroundColor(){
        if (widgetsShowing){
            setControllerInvisible()
        }else{
            setControllerVisible()
        }
    }
    private fun setPageToDark(){
        val playerView = findViewById<View>(R.id.playerView)
        playerView.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))

        val cover = findViewById<View>(R.id.cover)
        cover.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))

        val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
        val ScrollerRootArea = findViewById<View>(R.id.ScrollerRootArea)

        recyclerView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))
        ScrollerRootArea.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))


    }
    //启动和关闭小窗
    private fun startFloatingWindow() {
        fun checkOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(this)
        }
        if (!checkOverlayPermission()){
            notice("请先开启悬浮窗权限", 1000)
            return
        }else{
            notice("尝试启动小窗", 1000)
            //启动小窗服务
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val intentFloatingWindow = Intent(applicationContext, FloatingWindowService::class.java)
            intentFloatingWindow.putExtra("VIDEO_SIZE_WIDTH", videoSizeWidth)
            intentFloatingWindow.putExtra("VIDEO_SIZE_HEIGHT", videoSizeHeight)
            intentFloatingWindow.putExtra("SCREEN_WIDTH", screenWidth)
            startService(intentFloatingWindow)


            vm.inFloatingWindow = true
            //返回系统桌面
            val intentHomeLauncher = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intentHomeLauncher)

            /*
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("LOCAL_RECEIVER").apply {
                putExtra("key", "PLAYER_REBIND")
            })

             */
        }
    }
    private fun stopFloatingWindow() {
        stopService(Intent(applicationContext, FloatingWindowService::class.java))
    }
    //设置状态栏样式:横屏时隐藏状态栏
    @Suppress("DEPRECATION")
    private fun AppBarSetting() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets -> WindowInsetsCompat.CONSUMED }
                window.decorView.post { window.insetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.statusBars())
                        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } }
                //三星专用:显示到挖空区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                    window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                }

            //控件位置动态调整
            val displayManager = this.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = display?.rotation
            //控件位置动态调整:正向横屏
            if (rotation == Surface.ROTATION_90) {
                val TopBarArea_ButtonExit = findViewById<View>(R.id.TopBarArea_ButtonExit)
                (TopBarArea_ButtonExit.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
                val ControllerArea_VideoCurrentTimeCard = findViewById<CardView>(R.id.VideoCurrentTimeCard)
                (ControllerArea_VideoCurrentTimeCard.layoutParams as ViewGroup.MarginLayoutParams).marginStart = (150)
            }
            //控件位置动态调整:反向横屏
            else if (rotation == Surface.ROTATION_270) {
                val ButtonArea1 = findViewById<ConstraintLayout>(R.id.ButtonArea1)
                (ButtonArea1.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (150)
                val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
                (TopBarArea_ButtonMoreOptions.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = (150)
            }
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val TopBarArea = findViewById<View>(R.id.TopBarArea)
            (TopBarArea.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (vm.statusBarHeight + 8)
        }
    }
    //切换横屏
    @SuppressLint("SourceLockedOrientationActivity")
    private fun ButtonChangeOrientation(){
        //自动旋转关闭
        if (rotationSetting == 0){
            //当前为竖屏
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (vm.OrientationValue == 1){
                    vm.FromManualPortrait = false
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
                else if (vm.OrientationValue == 2){
                    vm.FromManualPortrait = false
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }
                else{
                    vm.FromManualPortrait = false
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
            }
            //当前为横屏
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.FromManualPortrait = true
                vm.onOrientationChanging = true
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
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
                else if (vm.OrientationValue == 2){
                    vm.currentOrientation = 2
                    vm.LastLandscapeOrientation = 2
                    vm.setManual()
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                }
                else{
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setManual()
                    vm.onOrientationChanging = true
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.currentOrientation = 0
                vm.setManual()
                vm.onOrientationChanging = true
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            }
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun ExitByOrientation(){
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            vm.setManual()
            vm.onOrientationChanging = true
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            if (vm.controllerHided){
                notice("再按一次退出",2000)
                setControllerVisible()
                vm.controllerHided = false
            }
            else{
                if (EnterAnimationComplete){
                    PlayerExoSingleton.stopPlayer()
                    stopFloatingWindow()
                    finish()
                }else{
                    EnterAnimationComplete = true
                    val data = Intent().apply { putExtra("key", "needClosePlayer") }
                    setResult(RESULT_OK, data)
                    finish()
                    return
                }
            }
        }
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
    private fun startBackgroundPlay(){
        if (vm.PREFS_CloseVideoTrack && !vm.inFloatingWindow){
            CloseVideoTrackJob()
            vm.closeVideoTrackJobRunning = true
        }
    }
    //回到前台恢复所有轨道
    private fun stopBackgroundPlay(){
        if (vm.PREFS_CloseVideoTrack){
            vm.recoveryAllTrack()
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
                    if (!vm.NOTICED_HeadSetInsert){
                        vm.NOTICED_HeadSetInsert = true
                        notice("检测到耳机插入,音量已限制为${(maxVolume*0.6).toInt()}", 1000)
                    }
                }
            }
        }
    }
    //音频焦点
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private fun requestAudioFocus(){
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        wasPlaying = vm.player.isPlaying
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        wasPlaying = vm.player.isPlaying
                        vm.player.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        wasPlaying = vm.player.isPlaying
                        vm.player.pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (wasPlaying){
                            wasPlaying = false
                            vm.player.play()
                        }
                    }
                }
            }
            .build()

        audioManager.requestAudioFocus(focusRequest)
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
        /*
        //根据滑动状态确定使用精确帧还是关键帧
        if (STATE_FingerTouching){
            vm.player.setSeekParameters(SeekParameters.EXACT)
        }else{
            vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        }

         */
        vm.player.seekTo(seekToMsOnce)
    }
    //手指抬起事件
    private fun scrollerStoppingTapUp(){
        STATE_FingerTouching = false
        //开启安全期倒计时
        lifecycleScope.launch {
            SecureIntervalJobFingerUp()
        }
        if (!vm.PREFS_LinkScroll) return
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
        //停止被控控件
        stopVideoTimeSync()
        stopScrollerSync()
        //播放结束时让控件显示
        setControllerVisible()
        Handler(Looper.getMainLooper()).postDelayed({ stopScrollerSync() }, 100)
        IDLE_Timer?.cancel()
        //自动退出和循环播放控制
        if (vm.PREFS_ExitWhenEnd){
            finish()
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
        if (vm.PREFS_LinkScroll && !scrollerTouching && !scrolling){ startScrollerSync() }
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
            var initBrightness = windowInfo.screenBrightness
            if (initBrightness < 0) {
                initBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                vm.BrightnessValue = initBrightness
            }
        }else{
            windowInfo.screenBrightness = vm.BrightnessValue
            window.attributes = windowInfo
        }
        //音量
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeChangeGap = 750/maxVolume
        if (originalVolume == 0 && !vm.NOTICED_VolumeIsZero) {
            vm.NOTICED_VolumeIsZero = true
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