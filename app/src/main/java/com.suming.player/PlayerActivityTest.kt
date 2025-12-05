package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
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
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
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
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaItem.MediaItemSetting
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.system.exitProcess

@UnstableApi
//@Suppress("unused")
class PlayerActivityTest: AppCompatActivity(){
    //变量初始化
    //<editor-fold desc="变量初始化">
    //音量配置参数
    private var maxVolume = 0
    private var currentVolume = 0
    private var originalVolume = 0
    //点击隐藏控件
    private var widgetsShowing = true
    //时间戳刷新间隔
    private var videoTimeSyncGap = 50L
    private var scrollParam1 = 0
    private var scrollParam2 = 0
    private var syncScrollRunnableGap = 16L
    //功能:倍速滚动
    private var lastPlaySpeed = 0f
    private var forceSeekGap = 5000L
    //功能:VideoSeek
    private var isSeekReady = true
    //判断体:PlayerReady(播放状态)
    private var playerReadyFrom_FirstEntry = true
    private var playerReadyFrom_NormalSeek = false
    private var playerReadyFrom_LastSeek = false
    private var playerReadyFrom_SmartScrollLastSeek = false
    //RunnableRunning
    private var videoSeekHandlerRunning = false
    private var smartScrollRunnableRunning = false
    private var syncScrollRunnableRunning = false
    //媒体信息
    private lateinit var MediaInfo_VideoUri: Uri
    private var MediaInfo_VideoDuration = 0
    private var MediaInfo_AbsolutePath = ""
    private var MediaInfo_VideoTitle = ""
    private var MediaInfo_VideoArtist = ""
    private var MediaInfo_FileName = ""
    //缩略图绘制参数
    private var ScrollerInfo_MaxPicNumber = 20
    private var ScrollerInfo_EachPicWidth = 0
    private var ScrollerInfo_PicNumber = 0
    private var ScrollerInfo_EachPicDuration: Int = 0
    //播放区域点击事件
    private var STATE_2Fingers = false
    private var ACTION_POINTER_DOWN = false
    private var originalDistance = 0f
    private var distanceGap = 0f
    private var center0x = 0f
    private var center0y = 0f
    private var center1x = 0f
    private var center1y = 0f
    private var originalScale = 1f
    private var scale = 1.0
    private var definiteScale = 1.0f
    private var center2x = 0f
    private var center2y = 0f
    private var center0pivoted = false
    private var finger1x = 0f
    private var finger1y = 0f
    private var finger2x = 0f
    private var finger2y = 0f
    private var vibrated = false
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

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    //空闲定时器
    private var IDLE_Timer: CountDownTimer? = null
    private val IDLE_MS = 5_000L
    //定时关闭定时器
    private var COUNT_Timer: CountDownTimer? = null

    private var EnterAnimationComplete = false
    private var fps = 0f
    //更新时间戳参数
    private var lastMillis = 0L
    //倍速播放
    private var currentSpeed = 1.0f
    //进度条状态
    private var scrollerState_DraggingMoving = false
    private var scrollerState_DraggingStay = false
    private var scrollerState_InertiaMoving = false
    private var scrollerState_Stay = true
    private var scrollerState_Moving = true
    private var scrollerState_Pressed = true
    private var scrollerState_BackwardScroll = false
    private var singleTap = false

    //播放结束需要定时关闭
    private var playEnd_NeedShutDown = false
    //全局SeekToMs
    private var global_SeekToMs = 0L
    //VideoSeekHandler
    private var videoSeekHandlerGap = 0L
    //进度条参数
    private var sidePadding = 0
    private var screenWidth = 0
    private var screenHeight = 0

    //lateInitItem -控件
    private lateinit var scroller: RecyclerView
    private lateinit var TopBarArea : LinearLayout
    private lateinit var ButtonArea : ConstraintLayout
    private lateinit var NoticeCard : CardView
    private lateinit var ButtonExit : ImageButton
    private lateinit var TimeCard : CardView
    private lateinit var playerView: PlayerView
    private lateinit var timer_current : TextView
    private lateinit var timer_duration : TextView
    //lateInitItem -工具
    private lateinit var DisplayMetrics: DisplayMetrics
    //lateInitItem -复杂工具
    private lateinit var receiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var equalizer: Equalizer

    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFSEditor: SharedPreferences.Editor
    private lateinit var PREFS_List: SharedPreferences

    private lateinit var retriever: MediaMetadataRetriever

    private lateinit var scrollerLayoutManager: LinearLayoutManager

    private var NeedRecoverySettings = false
    private var NeedRecoverySetting_VideoOnly = false
    private var NeedRecoverySetting_SoundOnly = false
    private var NeedRecoverySetting_LastPosition = 0L

    private var onScroll_currentMillis = 0L
    private var onScroll_scrollPercent = 0f
    private var onScroll_seekToMs = 0L

    private var VideoSeekHandler_totalWidth = 0
    private var VideoSeekHandler_offset = 0
    private var VideoSeekHandler_percent = 0f
    private var VideoSeekHandler_seekToMs = 0L

    private var videoTimeSyncHandler_currentPosition = 0L

    private var DataBaseProfile: MediaItemSetting? = null

    private var state_ActivityState_onBackground = false

    private var play_list_default = ""
    private var index = 0

    private lateinit var controller: MediaController

    //</editor-fold>
    //测试中变量
    //<editor-fold desc="测试中变量">

    private var playEnd_fromClearMediaItem = false
    private var onDestroy_fromErrorExit = false

    private var onDestroy_fromEnsureExit = false

    private var state_onBackground = false

    private var clickMillis_MoreOptionPage = 0L

    private lateinit var focusRequest: AudioFocusRequest

    private var state_need_return = false

    private var switchLandscape_downMillis = 0L
    private var switchLandscape_upMillis = 0L

    //</editor-fold>

    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx","DEPRECATION", "CommitPrefEdits")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player)
        //初始化部分控件
        fun lateInitItem(){
            scroller = findViewById(R.id.rvThumbnails)
            DisplayMetrics = resources.displayMetrics
            NoticeCard = findViewById(R.id.NoticeCard)
            TopBarArea = findViewById(R.id.TopBarArea)
            ButtonArea = findViewById(R.id.ButtonArea)
            playerView = findViewById(R.id.playerView)
            TimeCard = findViewById(R.id.VideoCurrentTimeCard)
            timer_current = findViewById(R.id.timer_current)
            timer_duration = findViewById(R.id.timer_duration)
            ButtonExit = findViewById(R.id.TopBarArea_ButtonExit)
        }
        lateInitItem()
        //连接ViewModel
        val vm = ViewModelProvider(this, PlayerExoFactory.getInstance(application))[PlayerViewModel::class.java]

        //其他预设
        preCheck()

        //提取uri并保存
        if (savedInstanceState == null) {
            PlayerSingleton.releasePlayer()
            if (vm.MediaInfo_Uri_Saved){
                MediaInfo_VideoUri = vm.MediaInfo_VideoUri!!
                intent = vm.originIntent
            }
            else{
                //区分打开方式
                when (intent?.action) {
                    //系统面板：分享
                    Intent.ACTION_SEND -> {
                        vm.state_FromSysStart = true
                        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                        MediaInfo_VideoUri = uri
                    }
                    //系统面板：选择其他应用打开
                    Intent.ACTION_VIEW -> {
                        vm.state_FromSysStart = true
                        val uri = intent.data ?: return finish()
                        MediaInfo_VideoUri = uri
                    }
                    //正常打开
                    else ->  {
                        vm.PREFS_ExitWhenEnd = false
                        val uri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)
                        if (uri == null){
                            val uri2 = vm.MediaInfo_VideoUri
                            if (uri2 == null){
                                showCustomToast("视频链接无效", Toast.LENGTH_SHORT, 3)
                                showCustomToast("播放失败", Toast.LENGTH_SHORT, 3)
                                onDestroy_fromErrorExit = true
                                state_need_return = true
                                finish()
                                return
                            }
                        }else{
                            MediaInfo_VideoUri = uri
                            vm.MediaInfo_VideoUri = MediaInfo_VideoUri
                        }
                    }
                }
                //保存intent至ViewModel
                vm.saveIntent(intent)
                //保存uri至ViewModel
                vm.MediaInfo_VideoUri = MediaInfo_VideoUri
                vm.MediaInfo_Uri_Saved = true
            }
        }else{
            MediaInfo_VideoUri = vm.MediaInfo_VideoUri!!
            intent = vm.originIntent
        }
        //根据uri提取基础视频信息
        getMediaInfo(MediaInfo_VideoUri)
        if (state_need_return){
            finish()
            return
        }


        //读取设置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS_List = getSharedPreferences("PREFS_List", MODE_PRIVATE)
        PREFSEditor = PREFS.edit()
        if (savedInstanceState == null){
            //固定项
            if (!PREFS.contains("PREFS_GenerateThumbSYNC")) {
                PREFSEditor.putBoolean("PREFS_GenerateThumbSYNC", true)
                vm.PREFS_GenerateThumbSYNC = true
            } else {
                vm.PREFS_GenerateThumbSYNC = PREFS.getBoolean("PREFS_GenerateThumbSYNC", true)
            }
            if (!PREFS.contains("PREFS_ExitWhenEnd")) {
                PREFSEditor.putBoolean("PREFS_ExitWhenEnd", false)
                vm.PREFS_ExitWhenEnd = false
            } else {
                vm.PREFS_ExitWhenEnd = PREFS.getBoolean("PREFS_ExitWhenEnd", false)
            }
            if (!PREFS.contains("PREFS_UseMediaSession")){
                PREFS.edit { putBoolean("PREFS_UseMediaSession", true).apply() }
                vm.PREFS_UseMediaSession = true
            }else{
                vm.PREFS_UseMediaSession = PREFS.getBoolean("PREFS_UseMediaSession", true)
            }
            if (!PREFS.contains("PREFS_InsertPreviewInMediaSession")){
                PREFSEditor.putBoolean("PREFS_InsertPreviewInMediaSession", true)
                vm.PREFS_InsertPreviewInMediaSession = true
            } else {
                vm.PREFS_InsertPreviewInMediaSession = PREFS.getBoolean("PREFS_InsertPreviewInMediaSession", false)
            }
            if (!PREFS.contains("PREFS_UseLongScroller")) {
                PREFSEditor.putBoolean("PREFS_UseLongScroller", false)
                vm.PREFS_UseLongScroller = false
            } else {
                vm.PREFS_UseLongScroller = PREFS.getBoolean("PREFS_UseLongScroller", false)
            }
            if (!PREFS.contains("PREFS_UseLongSeekGap")) {
                PREFSEditor.putBoolean("PREFS_UseLongSeekGap", false)
                vm.PREFS_UseLongSeekGap = false
            } else {
                vm.PREFS_UseLongSeekGap = PREFS.getBoolean("PREFS_UseLongSeekGap", false)
                if (vm.PREFS_UseLongSeekGap) {
                    forceSeekGap = 20000L
                }
            }
            if (!PREFS.contains("PREFS_UseBlackBackground")) {
                PREFSEditor.putBoolean("PREFS_UseBlackBackground", false)
                vm.PREFS_UseBlackBackground = false
            } else {
                vm.PREFS_UseBlackBackground = PREFS.getBoolean("PREFS_UseBlackBackground", false)
            }
            if (!PREFS.contains("PREFS_UseHighRefreshRate")) {
                PREFSEditor.putBoolean("PREFS_UseHighRefreshRate", false)
                vm.PREFS_UseHighRefreshRate = false
            } else {
                vm.PREFS_UseHighRefreshRate = PREFS.getBoolean("PREFS_UseHighRefreshRate", false)
            }
            if (!PREFS.contains("PREFS_UseCompatScroller")) {
                PREFSEditor.putBoolean("PREFS_UseCompatScroller", false)
                vm.PREFS_UseCompatScroller = false
            } else {
                vm.PREFS_UseCompatScroller = PREFS.getBoolean("PREFS_UseCompatScroller", false)
            }
            if (!PREFS.contains("PREFS_UseOnlySyncFrame")) {
                PREFSEditor.putBoolean("PREFS_UseOnlySyncFrame", true)
                vm.PREFS_UseOnlySyncFrame = true
            } else {
                vm.PREFS_UseOnlySyncFrame = PREFS.getBoolean("PREFS_UseOnlySyncFrame", true)
            }
            if (!PREFS.contains("PREFS_UseSysVibrate")){
                PREFSEditor.putBoolean("PREFS_UseSysVibrate", true)
                vm.PREFS_UseSysVibrate = true
            }else{
                vm.PREFS_UseSysVibrate = PREFS.getBoolean("PREFS_UseSysVibrate", true)
            }
            if (!PREFS.contains("PREFS_UseDataBaseForScrollerSetting")) {
                PREFSEditor.putBoolean("PREFS_EnableRoomDatabase", false)
                vm.PREFS_UseDataBaseForScrollerSetting = true
            } else {
                vm.PREFS_UseDataBaseForScrollerSetting = PREFS.getBoolean("PREFS_UseDataBaseForScrollerSetting", false)
            }
            if (!PREFS.contains("PREFS_SwitchPortraitWhenExit")) {
                PREFSEditor.putBoolean("PREFS_SwitchPortraitWhenExit", true)
                vm.PREFS_SwitchPortraitWhenExit = true
            } else {
                vm.PREFS_SwitchPortraitWhenExit = PREFS.getBoolean("PREFS_SwitchPortraitWhenExit", true)
            }
            if (!PREFS.contains("PREFS_CloseVideoTrack")) {
                PREFSEditor.putBoolean("PREFS_CloseVideoTrack", true)
                vm.PREFS_CloseVideoTrack = true
            } else {
                vm.PREFS_CloseVideoTrack = PREFS.getBoolean("PREFS_CloseVideoTrack", false)
            }
            if (!PREFS.contains("PREFS_CloseFragmentGesture")) {
                PREFSEditor.putBoolean("PREFS_CloseFragmentGesture", false)
                vm.PREFS_CloseFragmentGesture = false
            } else {
                vm.PREFS_CloseFragmentGesture = PREFS.getBoolean("PREFS_CloseFragmentGesture", false)
            }
            if (!PREFS.contains("PREFS_EnablePlayAreaMove")){
                PREFSEditor.putBoolean("PREFS_EnablePlayAreaMove", true)
                vm.PREFS_EnablePlayAreaMove = true
            }else{
                vm.PREFS_EnablePlayAreaMove = PREFS.getBoolean("PREFS_EnablePlayAreaMove", true)
            }

            //固定数值项
            if (!PREFS.contains("PREFS_TimeUpdateGap")) {
                PREFSEditor.putLong("PREFS_TimeUpdateGap", 66L)
                vm.PREFS_TimeUpdateGap = 66L
            } else {
                vm.PREFS_TimeUpdateGap = PREFS.getLong("PREFS_TimeUpdateGap", 66L)
            }
            if (!PREFS.contains("PREFS_VibrateMillis")){
                PREFS.edit { putLong("PREFS_VibrateMillis", 10L).apply() }
                vm.PREFS_VibrateMillis = 10L
            }else{
                vm.PREFS_VibrateMillis = PREFS.getLong("PREFS_VibrateMillis", 10L)
            }
            if (!PREFS.contains("INFO_STATUSBAR_HEIGHT")) {
                vm.statusBarHeight = 200
            } else {
                vm.statusBarHeight = PREFS.getInt("INFO_STATUSBAR_HEIGHT", 0)
            }
            if (!PREFS.contains("PREFS_SeekHandlerGap")) {
                PREFSEditor.putLong("PREFS_SeekHandlerGap", 0L)
                vm.PREFS_SeekHandlerGap = 0L
                videoSeekHandlerGap = 0L
            } else {
                vm.PREFS_SeekHandlerGap = PREFS.getLong("PREFS_SeekHandlerGap", 0L)
                videoSeekHandlerGap = PREFS.getLong("PREFS_SeekHandlerGap", 0L)
            }

            //可动态更改项
            if (!PREFS.contains("PREFS_BackgroundPlay")) {
                PREFSEditor.putBoolean("PREFS_BackgroundPlay", false)
                vm.PREFS_BackgroundPlay = false
            } else {
                vm.PREFS_BackgroundPlay = PREFS.getBoolean("PREFS_BackgroundPlay", false)
            }
            if (!PREFS.contains("PREFS_SealOEL")) {
                PREFSEditor.putBoolean("PREFS_SealOEL", false)
                vm.PREFS_SealOEL = false
            } else {
                vm.PREFS_SealOEL = PREFS.getBoolean("PREFS_SealOEL", false)
            }

            PREFSEditor.apply()

        }else{

            videoSeekHandlerGap = vm.PREFS_SeekHandlerGap
            //状态预置位
            vm.allowRecord_wasPlaying = true
            playerReadyFrom_FirstEntry = false
            EnterAnimationComplete = true
        }
        //读取数据库
        if (savedInstanceState == null){
            //读取数据库
            ReadRoomDataBase()
        }
        //基于设置的后续操作
        if (vm.PREFS_UseBlackBackground) {
            setPageToDark()
        }                     //使用深色播放页
        if (!vm.PREFS_UseDataBaseForScrollerSetting){
            if (!PREFS.contains("PREFS_AlwaysSeek")) {
                PREFS.edit { putBoolean("PREFS_AlwaysSeek", true).apply() }
                vm.PREFS_AlwaysSeek = true
            } else {
                vm.PREFS_AlwaysSeek = PREFS.getBoolean("PREFS_AlwaysSeek", true)
            }
            if (!PREFS.contains("PREFS_LinkScroll")) {
                PREFS.edit { putBoolean("PREFS_LinkScroll", true).apply() }
                vm.PREFS_LinkScroll = true
            } else {

                vm.PREFS_LinkScroll = PREFS.getBoolean("PREFS_LinkScroll", true)
            }
            if (!PREFS.contains("PREFS_TapJump")) {
                PREFS.edit { putBoolean("PREFS_TapJump", false).apply() }
                vm.PREFS_TapJump = false
            } else {
                vm.PREFS_TapJump = PREFS.getBoolean("PREFS_TapJump", false)
            }
        }          //选读数据库
        if (vm.PREFS_UseHighRefreshRate) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = windowManager
                val mode = wm.defaultDisplay.mode
                val fps = mode.refreshRate
                window.attributes = window.attributes.apply {
                    preferredRefreshRate = fps
                }
            }
        }                     //刷新率强制修改
        if (vm.PREFS_EnablePlayAreaMove){
            MoveYaxisCalculate()
        }                      //计算移动高度

        //监听注册
        //方向监听器
        OEL = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                //把方向角数值映射为状态量
                if (orientation > 260 && orientation < 280) {
                    vm.OrientationValue = 1
                } else if (orientation > 80 && orientation < 100) {
                    vm.OrientationValue = 2
                } else if (orientation > 340 && orientation < 360) {
                    vm.OrientationValue = 0
                }
                //进入锁
                orientationChangeTime = System.currentTimeMillis()
                if (orientationChangeTime - LastOrientationChangeTime < 1) {
                    return
                }
                LastOrientationChangeTime = orientationChangeTime
                //读取自动旋转状态
                rotationSetting = Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                )
                //自动旋转开启
                if (rotationSetting == 1) {
                    //当前为竖屏
                    if (vm.currentOrientation == 0) {
                        //从 竖屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 1) return
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 竖屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            if (vm.Manual && vm.LastLandscapeOrientation == 2) return
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                    }
                    //当前为正向横屏
                    else if (vm.currentOrientation == 1) {
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 2) {
                            //按钮避让:横排按钮区&更多选项按钮
                            setControllerLayerPadding("right")
                            //更改状态并发起旋转
                            vm.currentOrientation = 2
                            vm.LastLandscapeOrientation = 2
                            vm.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                    //当前为反向横屏
                    else if (vm.currentOrientation == 2) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            //按钮避让时间框&退出按钮
                            setControllerLayerPadding("left")
                            //更改状态并发起旋转
                            vm.currentOrientation = 1
                            vm.LastLandscapeOrientation = 1
                            vm.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 反向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (vm.OrientationValue == 0) {
                            if (vm.Manual) return
                            vm.currentOrientation = 0
                            vm.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                }
                //自动旋转关闭
                else if (rotationSetting == 0) {
                    if (!vm.FromManualPortrait) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (vm.OrientationValue == 1) {
                            //按钮避让时间框&退出按钮
                            setControllerLayerPadding("left")
                            //更改状态并发起旋转
                            setOrientation_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
                            //按钮避让:横排按钮区&更多选项按钮
                            setControllerLayerPadding("right")
                            //更改状态并发起旋转
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                    }
                }
            }
        }
        if (vm.PREFS_SealOEL) {
            OEL.disable()
        } else {
            OEL.enable()
        }
        //音频设备监听
        audioManager.registerAudioDeviceCallback(DeviceCallback, null)
        //内部广播接收
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val filter = IntentFilter("LOCAL_RECEIVER")
        receiver = object : BroadcastReceiver() {
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
                    vm.wasPlaying = vm.player.isPlaying
                    vm.player.pause()
                }
                if (data == "PLAYER_EXIT") {
                    val pid = Process.myPid()
                    Process.killProcess(pid)
                }
                if (data == "PLAYER_NextMedia") {
                    vm.player.seekToNextMediaItem()
                }
                if (data == "PLAYER_PreviousMedia") {
                    vm.player.seekToPreviousMediaItem()
                }
                if (data == "PLAYER_PlayOrPause") {
                    if (vm.player.isPlaying) {
                        vm.player.pause()
                    } else {
                        vm.player.play()
                    }
                }
            }
        }
        localBroadcastManager.registerReceiver(receiver, filter)
        //RxJava事件总线
        setupEventBus()



        //绑定播放器输出
        playerView.player = vm.player
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
                ButtonRefresh()
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoSizeWidth = videoSize.width
                    videoSizeHeight = videoSize.height
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                for (trackGroup in tracks.groups) {
                    val format = trackGroup.getTrackFormat(0)
                    fps = format.frameRate
                    break
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                        onMediaItemChanged(mediaItem)
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                        onMediaItemChanged(mediaItem)
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        onMediaItemChanged(mediaItem)
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                        onMediaItemChanged(mediaItem)
                    }
                }
            }


        }
        vm.player.addListener(PlayerStateListener!!)


        //传入视频链接
        if(savedInstanceState == null){ startPlayNewItem() }
        //刷新按钮+移除遮罩
        else {
            val MediaInfo_VideoUri = vm.MediaInfo_VideoUri!!

            getMediaInfo(MediaInfo_VideoUri)

            refreshTimeLine()

            ReadRoomDataBase()

            scrollerAdapterUpdate()

            ButtonRefresh()
            //移除遮罩
            val cover = findViewById<LinearLayout>(R.id.cover)
            cover.visibility = View.GONE
        }



        //Scroller事件 gestureDetector层 -onSingleTap -onDown
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
                val totalContentWidth = scroller.computeHorizontalScrollRange()
                val scrolled = scroller.computeHorizontalScrollOffset()
                val leftPadding = scroller.paddingLeft
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
                notice("跳转至${FormatTime_withChar(seekToMs)}", 1000)
                lifecycleScope.launch {
                    startScrollerSync()
                    delay(20)
                    if (!vm.PREFS_LinkScroll) {
                        stopScrollerSync()
                    }
                }



                if (vm.wasPlaying) {
                    vm.wasPlaying = false
                    vm.player.play()
                }
                return true
            }
        })
        //Scroller事件 -原生层
        scroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    scrollerState_Pressed = true
                    scrollerState_DraggingStay = true
                    lastSeekExecuted = false
                    if (vm.player.isPlaying && vm.allowRecord_wasPlaying) {
                        vm.wasPlaying = true
                    }
                }
                if (e.action == MotionEvent.ACTION_UP) {
                    scrollerState_Pressed = false

                    //开启控件隐藏倒计时
                    startIdleTimer()
                }
                //detector承担长按
                gestureDetectorScroller.onTouchEvent(e)
                return false
            }
            //以下未使用
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                gestureDetectorScroller.onTouchEvent(e)
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {  }
        })
        //Scroller事件 -滚动层 -onScrollStateChanged -onScrolled
        scroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            //onScrollStateChanged承担状态标记变更
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrollerState_Stay = false
                    scrollerState_Moving = true
                    scrollerState_DraggingMoving = true

                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    scrollerState_Stay = false
                    scrollerState_Moving = true
                    scrollerState_DraggingStay = false
                    scrollerState_DraggingMoving = false
                    scrollerState_InertiaMoving = true

                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrollerState_Stay = true
                    scrollerState_Moving = false
                    scrollerState_DraggingMoving = false
                    scrollerState_DraggingStay = false
                    scrollerState_InertiaMoving = false

                    return
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!vm.state_firstReadyReached) return
                //进度条随视频滚动,用户没有操作
                if (scrollerState_Stay) {
                    return
                }
                //用户操作 -停止同步
                stopVideoTimeSync()
                stopScrollerSync()
                //用户操作 -修改seek参数和间隔
                if (scrollerState_DraggingMoving) {
                    //修改视频寻帧间隔
                    if (dx > 0){
                        videoSeekHandlerGap = vm.PREFS_SeekHandlerGap
                    }else if (dx < 0){
                        videoSeekHandlerGap = vm.PREFS_SeekHandlerGap
                    }
                    //修改视频seek参数
                    if (dx == 1 || dx == -1) {
                        if (vm.PREFS_UseOnlySyncFrame){
                            vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        else {
                            vm.player.setSeekParameters(SeekParameters.EXACT)
                        }
                    } else {
                        vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                } else {
                    vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                }
                //用户操作 -时间戳跟随进度条变动
                onScroll_currentMillis = System.currentTimeMillis()
                if (onScroll_currentMillis - lastMillis > vm.PREFS_TimeUpdateGap) {
                    lastMillis = onScroll_currentMillis
                    if (vm.PREFS_LinkScroll && vm.state_firstReadyReached) {

                        //计算对应时间戳
                        onScroll_scrollPercent = recyclerView.computeHorizontalScrollOffset().toFloat() / scroller.computeHorizontalScrollRange()
                        onScroll_seekToMs = (onScroll_scrollPercent * MediaInfo_VideoDuration.toLong()).toLong()

                        //刷新时间显示
                        timer_current.text = FormatTime_onlyNum(onScroll_seekToMs)

                    } else {
                        return
                    }
                }
                //用户操作 -视频跳转
                if (!vm.PREFS_LinkScroll) {
                    return
                }
                //进度条往左走/视频正向
                if (dx > 0) {
                    //跳转方式:寻帧
                    if (vm.PREFS_AlwaysSeek) {
                        scrollerState_BackwardScroll = false
                        startVideoSeek()
                    }
                    //跳转方式:倍速滚动
                    else {
                        stopVideoSeek()
                        startVideoSmartScroll()
                    }
                }
                //进度条往右走/视频反向
                else if (dx < 0) {
                    scrollerState_BackwardScroll = true
                    vm.player.pause()
                    stopVideoSmartScroll()
                    startVideoSeek()
                }
            }
        })


        //退出按钮
        ButtonExit.setOnClickListener {
            vibrate()
            EnsureExit()
        }
        //更多选项
        val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
        TopBarArea_ButtonMoreOptions.setOnClickListener {
            vibrate()

            if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                return@setOnClickListener
            }
            clickMillis_MoreOptionPage = System.currentTimeMillis()

            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                MovePlayAreaJob()
            }
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.NoticeCard)
        noticeCard.setOnClickListener {
            vibrate()
            noticeCard.visibility = View.GONE
        }
        //按钮：暂停/继续播放
        val buttonPause = findViewById<FrameLayout>(R.id.buttonPause)
        buttonPause.setOnClickListener {
            vibrate()
            if (vm.player.isPlaying) {
                pauseVideo()
                stopScrollerSync()
                notice("暂停", 1000)
                ButtonRefresh()
            } else {
                //状态变更
                scroller.stopScroll()
                //播放或暂停
                lifecycleScope.launch {
                    scroller.stopScroll()
                    delay(20)
                    if (vm.playEnd) {
                        vm.playEnd = false
                        vm.player.seekTo(0)
                        playVideo()
                        notice("视频已结束,开始重播", 1000)
                        ButtonRefresh()
                        return@launch
                    } else {
                        playVideo()
                        return@launch
                    }
                }
                notice("继续播放", 1000)
            }
        }
        //按钮：切换横屏
        val buttonSwitchLandscape = findViewById<FrameLayout>(R.id.buttonActualSwitchLandscape)
        buttonSwitchLandscape.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    vibrate()
                    switchLandscape_upMillis = 0L
                    switchLandscape_downMillis = System.currentTimeMillis()
                    SwitchLandscapeJob()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    SwitchLandscapeJob?.cancel()
                    switchLandscape_upMillis = System.currentTimeMillis()
                    if (switchLandscape_upMillis - switchLandscape_downMillis < 300){
                        ButtonChangeOrientation("short")
                    }
                    return@setOnTouchListener true
                }
            }
            onTouchEvent(event)
        }
        //按钮：更多选项
        val buttonMoreOptions = findViewById<FrameLayout>(R.id.buttonActualMoreButton)
        buttonMoreOptions.setOnClickListener {
            vibrate()

            if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                return@setOnClickListener
            }
            clickMillis_MoreOptionPage = System.currentTimeMillis()

            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                MovePlayAreaJob()
            }
        }
        //播放区域点击事件
        val gestureDetectorPlayArea = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (vm.player.isPlaying) {
                    pauseVideo()
                    stopScrollerSync()
                    notice("暂停播放", 1000)
                    ButtonRefresh()
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
                        ButtonRefresh()
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
                currentSpeed = vm.player.playbackParameters.speed
                vm.player.setPlaybackSpeed(currentSpeed * 2.0f)
                notice("倍速播放中(${currentSpeed * 2.0f}x)", 114514)
                longPress = true
                vibrate()
                super.onLongPress(e)
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (touchLeft) {
                    scrollDistance += distanceY.toInt()
                    val windowInfo = window.attributes
                    //亮度修改操作
                    vm.BrightnessChanged = true
                    var newBrightness: Float
                    //上滑
                    if (scrollDistance > 50) {
                        newBrightness = (vm.BrightnessValue + 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 +1 (${(newBrightness*100).toInt()}/100)", 1000)
                        }
                        else{
                            notice("亮度已到上限", 1000)
                            if (!vibrated) {
                                vibrated = true
                                vibrate()
                            }
                        }
                    }
                    //下滑
                    else if (scrollDistance < -50){
                        newBrightness = (vm.BrightnessValue - 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness <= 1.0 && newBrightness >= 0.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 -1 (${(newBrightness*100).toInt()}/100)", 1000)
                        }
                        else{
                            if (!vibrated) {
                                vibrated = true
                                vibrate()
                            }
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
                                }
                                else{
                                    if (!vibrated) {
                                        vibrated = true
                                        vibrate()
                                    }
                                    notice("佩戴耳机时,音量不能超过${(maxVolume*0.6).toInt()},除非使用音量键调整", 1000)
                                }
                            }
                            else{
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                            }
                        }
                        else{
                            if (!vibrated) {
                                vibrated = true
                                vibrate()
                            }
                            notice("音量已到最高", 1000)
                        }
                    }
                    else if (scrollDistance< -volumeChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume = currentVolume - 1
                        if (currentVolume >= 0){
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                        }
                        else{
                            if (!vibrated) {
                                vibrated = true
                                vibrate()
                            }
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
                    //重置振动标志位
                    vibrated = false
                    //记录1指初始坐标
                    finger1x = event.x
                    finger1y = event.y
                    //点击区域屏蔽
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
                        vm.player.setPlaybackSpeed(currentSpeed)
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
        //更多按钮页面返回值 FROM_FRAGMENT_MORE_BUTTON
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_MORE_BUTTON", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                //顶部横排
                "Capture" -> {
                    captureScreenShot()

                }
                "BackToStart" -> {
                    vm.player.seekTo(0)
                    vm.player.play()
                    if (vm.PREFS_LinkScroll) startScrollerSync()
                    notice("回到视频起始", 3000)
                }
                "PlayList" -> {
                    PlayerFragmentPlayList.newInstance().show(supportFragmentManager, "PlayerListFragment")
                }
                "ExtractFrame" -> {
                    val videoPath = getAbsoluteFilePath(this, MediaInfo_VideoUri)
                    if (videoPath == null){
                        showCustomToast("视频绝对路径获取失败", Toast.LENGTH_SHORT, 3)
                        return@setFragmentResultListener
                    }
                    ExtractFrame(videoPath, MediaInfo_FileName)
                }
                //播放相关
                "BackgroundPlay" -> {
                    if (vm.PREFS_BackgroundPlay){
                        PREFS.edit { putBoolean("PREFS_BackgroundPlay", true).apply() }
                        notice("已开启后台播放", 1000)
                    } else {
                        checkNotificationPermission()
                        PREFS.edit { putBoolean("PREFS_BackgroundPlay", false).apply() }
                        notice("已关闭后台播放", 1000)
                    }
                }
                "RepeatMode" -> {
                    changeRepeatMode()
                }
                //倍速
                "SetSpeed" -> {
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
                        }
                        else {
                            val inputValue = userInput.toFloat()
                            if(inputValue > 0.0 && inputValue <= 5.0){
                                vm.player.setPlaybackSpeed(inputValue)
                                notice("已将倍速设置为$inputValue", 2000)
                                lifecycleScope.launch {
                                    val newSetting = MediaItemSetting(MARK_FileName = vm.MediaInfo_FileName, PREFS_PlaySpeed = inputValue)
                                    MediaItemRepo.get(this@PlayerActivityTest).saveSetting(newSetting)
                                }
                            } else {
                                notice("不允许该值", 3000)
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
                "SealOEL" -> {
                    if (vm.PREFS_SealOEL){
                        PREFS.edit { putBoolean("PREFS_SealOEL", true).apply() }
                        OEL.disable()
                        notice("已关闭方向监听器", 1000)
                    } else {
                        PREFS.edit { putBoolean("PREFS_SealOEL", false).apply() }
                        OEL.enable()
                        notice("已开启方向监听器", 1000)
                    }
                }
                "SoundOnly" -> {
                    changeStateSoundOnly()
                }
                "VideoOnly" -> {
                    changeStateVideoOnly()
                }
                "SavePositionWhenExit" -> {
                    if (vm.PREFS_SavePositionWhenExit){
                        notice("退出时将会保存进度", 2000)
                    } else {
                        notice("已关闭退出时保存进度", 2000)
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_SavePositionWhenExit(MediaInfo_FileName,vm.PREFS_SavePositionWhenExit)
                    }
                }
                //进度条
                "AlwaysSeek" -> {
                    changeStateAlwaysSeek()
                }
                "LinkScroll" -> {
                    changeStateLinkScroll()
                }
                "TapJump" -> {
                    changeStateTapJump()
                }
                //定时关闭 + 开启小窗
                "setShutDownTime" -> {
                    val dialog = Dialog(this)
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_time, null)
                    dialog.setContentView(dialogView)
                    val title: TextView = dialogView.findViewById(R.id.dialog_title)
                    val Description:TextView = dialogView.findViewById(R.id.dialog_description)
                    val EditTextHour: EditText = dialogView.findViewById(R.id.dialog_input_hour)
                    val EditTextMinute: EditText = dialogView.findViewById(R.id.dialog_input_minute)
                    val Button: Button = dialogView.findViewById(R.id.dialog_button)

                    title.text = "自定义：定时关闭时间"
                    Description.text = "请输入您的自定定时关闭时间"
                    EditTextHour.hint = "              "
                    EditTextMinute.hint = "              "
                    Button.text = "确定"

                    val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    Button.setOnClickListener {
                        val hourInput = EditTextHour.text.toString().toIntOrNull()
                        val minuteInput = EditTextMinute.text.toString().toIntOrNull()

                        var hour: Int
                        var minute: Int

                        //获取时
                        if (hourInput == null || hourInput == 0 ){
                            hour = 0
                        } else {
                            hour = hourInput
                        }
                        //获取分
                        if (minuteInput == null || minuteInput == 0 ){
                            minute = 0
                        } else {
                            minute = minuteInput
                        }

                        if (hourInput == null && minuteInput == null){
                            notice("未输入内容", 1000)
                            dialog.dismiss()
                            return@setOnClickListener
                        }
                        if (hour == 0 && minute == 0){
                            notice("立即关闭", 1000)
                            lifecycleScope.launch {
                                delay(2000)
                                val pid = Process.myPid()
                                Process.killProcess(pid)
                            }
                        }

                        //计算总分钟数
                        val totalMinutes = hour * 60 + minute
                        startTimerShutDown(totalMinutes, true)

                        dialog.dismiss()
                    }
                    dialog.show()
                    //自动弹出键盘程序
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(50)
                        EditTextHour.requestFocus()
                        imm.showSoftInput(EditTextHour, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                "chooseShutDownTime" -> {
                    val time = bundle.getInt("TIME")
                    startTimerShutDown(time, true)
                }
                "StartPiP" -> {
                    startFloatingWindow()
                }
                //底部按钮
                "VideoInfo" -> {
                    //读取数据
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(MediaInfo_AbsolutePath)
                    val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val videoFps = fps
                    val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    val videoMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    val videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

                    val videoFileName = MediaInfo_FileName
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
                "SysShare" -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(500)
                        shareVideo(this@PlayerActivityTest, MediaInfo_VideoUri)
                    }
                }
                "UpdateCover" -> {
                    updateCover(MediaInfo_FileName)
                }
                "Equalizer" -> {
                    PlayerFragmentEqualizer.newInstance().show(supportFragmentManager, "PlayerEqualizerFragment")
                }
                "clearMiniature" -> {
                    clearMiniature()
                }
                //退出事件
                "Dismiss" -> {
                    MovePlayArea_down()
                }
            }
        }
        //播放列表返回值 FROM_FRAGMENT_PLAY_LIST
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_PLAY_LIST", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                //顶部按钮
                "PreviousMedia" -> {
                    vm.player.seekToPreviousMediaItem()
                }
                "NextMedia" -> {
                    vm.player.seekToNextMediaItem()
                }

                //退出逻辑
                "Dismiss" -> {
                    MovePlayArea_down()
                }
            }
        }
        //媒体信息列表返回值 FROM_FRAGMENT_VIDEO_INFO
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_VIDEO_INFO", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                "Dismiss" -> {
                    MovePlayArea_down()
                }
            }
        }

        //读取视频列表
        val playListString = PREFS_List.getString("CurrentPlayList", "错误")
        if (playListString != null) {
            vm.List_PlayList = playListString
        }else{
            showCustomToast("读取播放列表失败", Toast.LENGTH_SHORT,3)
        }




        //系统手势监听：返回键重写
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ExitByOrientation()
            }
        })
    //onCreate END
    }


    //Testing Functions
    private fun showNotification_NotPrepared(text: String) {
        val channelId = "toast_replace"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "提示", NotificationManager.IMPORTANCE_HIGH)
            .apply {
                setSound(null, null)
                enableVibration(false)
            }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_player_service_notification)
            .setContentTitle(null)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(0)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)

    }
    //RxJava事件总线
    private var disposable: io.reactivex.rxjava3.disposables.Disposable? = null
    private fun setupEventBus() {
        disposable = ToolEventBus.events
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({
                handlePlayerEvent(it)
            }, {
                showCustomToast("事件总线注册失败:${it.message}", Toast.LENGTH_SHORT,3)
            })
    }
    private fun handlePlayerEvent(event: String) {
        when (event) {
            "SessionController_Next" -> {
                Log.d("SuMing", "SessionController_Next")
                if (vm.state_PlayListProcess_Complete){
                    vm.player.seekToNextMediaItem()
                }else{
                    showNotification_NotPrepared("播放列表还未处理完成,请稍后再试")
                }
            }
            "SessionController_Previous" -> {
                Log.d("SuMing", "SessionController_Previous")
                if (vm.state_PlayListProcess_Complete){
                    vm.player.seekToPreviousMediaItem()
                }else{
                    showNotification_NotPrepared("播放列表还未处理完成,请稍后重试")
                }
            }
            "SessionController_Play" -> {
                vm.wasPlaying = true
                if (vm.PREFS_LinkScroll && !state_onBackground ) startScrollerSync()
                startVideoTimeSync()
            }
            "SessionController_Pause" -> {
                vm.wasPlaying = false
                stopScrollerSync()
                stopVideoTimeSync()
            }
        }
    }
    //构建完整uri
    private fun BuildNewUri(id: Long): Uri{
        return "content://media/external/video/media/$id".toUri()
    }
    //数据库读取+基于数据库的操作
    private fun ReadRoomDataBase(){
        lifecycleScope.launch(Dispatchers.IO) {
            DataBaseProfile = MediaItemRepo.get(applicationContext).getSetting(vm.MediaInfo_FileName)
            if (DataBaseProfile == null){
                DataBasePreWrite()
                return@launch }
            val DataBaseSetting = DataBaseProfile!!
            //视频强单项适用设置读取
            if (DataBaseSetting.PREFS_SoundOnly){ vm.PREFS_OnlyAudio = true }else{
                vm.PREFS_OnlyAudio = false
            }
            if (DataBaseSetting.PREFS_VideoOnly){ vm.PREFS_OnlyVideo = true }else{
                vm.PREFS_OnlyVideo = false
            }
            if (DataBaseSetting.PREFS_SavePositionWhenExit){ vm.PREFS_SavePositionWhenExit = true }else{
                vm.PREFS_SavePositionWhenExit = false
            }

            //数据库后续操作
            if (vm.PREFS_OnlyAudio){
                delay(100)
                if (vm.state_firstReadyReached){
                    withContext(Dispatchers.Main) {
                        changeStateSoundOnly()
                    }
                }else{
                    NeedRecoverySettings = true
                    NeedRecoverySetting_SoundOnly = true
                }
            }
            if (vm.PREFS_OnlyVideo){
                delay(100)
                if (vm.state_firstReadyReached){
                    withContext(Dispatchers.Main) {
                        changeStateVideoOnly()
                    }
                }else{
                    NeedRecoverySettings = true
                    NeedRecoverySetting_VideoOnly = true
                }
            }
            if (vm.PREFS_SavePositionWhenExit) {
                if (vm.seekToLastPositionExecuted){ return@launch }
                vm.seekToLastPositionExecuted = true
                val LastPosition = DataBaseSetting.SaveState_ExitPosition
                if (LastPosition != 0L) {
                    delay(100)
                    if (vm.state_firstReadyReached) {
                        withContext(Dispatchers.Main) { vm.player.seekTo(LastPosition) }
                        notice("已定位到上次播放的位置", 3000)
                    } else {
                        NeedRecoverySettings = true
                        NeedRecoverySetting_LastPosition = LastPosition
                    }
                }
            }
            if (vm.PREFS_UseDataBaseForScrollerSetting){
                lifecycleScope.launch(Dispatchers.IO) {
                    if (DataBaseProfile == null){
                        return@launch
                    }
                    val DataBaseSetting = DataBaseProfile!!
                    if (DataBaseSetting.PREFS_AlwaysSeek){
                        vm.PREFS_AlwaysSeek = true
                    }else{
                        vm.PREFS_AlwaysSeek = false
                    }
                    if (DataBaseSetting.PREFS_LinkScroll){
                        vm.PREFS_LinkScroll = true
                    }else{
                        vm.PREFS_LinkScroll = false
                    }
                    if (DataBaseSetting.PREFS_TapJump){
                        vm.PREFS_TapJump = true
                    }else{
                        vm.PREFS_TapJump = false
                    }
                }
            }

        }
    }
    //媒体项变更的后续操作
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null){ return }

        //更新视频uri存档
        vm.MediaInfo_VideoUri = MediaInfo_VideoUri
        //更新全局媒体信息变量
        getMediaInfo(MediaInfo_VideoUri)
        //重新读取数据库+覆盖关键值
        ReadRoomDataBase()
        //刷新：视频总长度
        refreshTimeLine()
        //刷新：进度条更新
        scrollerAdapterUpdate()

        //开启服务的方式
        startServiceOrSession()


    }
    //连接到媒体会话(不在会话中设置媒体项)
    private fun connectToMediaSession() {
        val SessionToken = SessionToken(this, ComponentName(this, PlayerServiceTest::class.java))
        val MediaSessionController = MediaController.Builder(this, SessionToken).buildAsync()
        MediaSessionController.addListener({
            controller = MediaSessionController.get()

        }, MoreExecutors.directExecutor())

    }
    //开启服务:使用自定义通知或媒体会话
    private fun startServiceOrSession(){
        if (vm.PREFS_UseMediaSession){
            connectToMediaSession()
        }else{
            startBackgroundServices()
        }
    }
    //绑定进度条adapter,可重复刷新
    private fun scrollerAdapterUpdate(){
        lifecycleScope.launch(Dispatchers.IO) {
            //获取ViewModel
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()
            //预先规划文件夹结构并创建 + 基于文件名哈希区分
            val SubDir_ThisMedia = File(cacheDir, "Media/${MediaInfo_FileName.hashCode()}/scroller")
            if (!SubDir_ThisMedia.exists()){
                SubDir_ThisMedia.mkdirs()
            }
            //传入参数预处理
            if (ScrollerInfo_EachPicDuration > 1000){
                vm.PREFS_GenerateThumbSYNC = true
            }
            //进度条绘制参数计算
            //使用超长进度条
            if (vm.PREFS_UseLongScroller) {
                ScrollerInfo_EachPicWidth = (47 * DisplayMetrics.density).toInt()
                if (MediaInfo_VideoDuration > 1_0000_000L) {
                    ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration / 500.0).toInt()
                    ScrollerInfo_PicNumber = 500
                }
                else if (MediaInfo_VideoDuration > 7500_000L) {
                    ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration / 400.0).toInt()
                    ScrollerInfo_PicNumber = 400
                }
                else if (MediaInfo_VideoDuration > 5000_000L) {
                    ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration / 300.0).toInt()
                    ScrollerInfo_PicNumber = 300
                }
                else if (MediaInfo_VideoDuration > 500_000L) {
                    ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration / 200.0).toInt()
                    ScrollerInfo_PicNumber = 200
                }
                else {
                    ScrollerInfo_EachPicDuration = 1000
                    ScrollerInfo_PicNumber = min((max((MediaInfo_VideoDuration / 1000), 1)), 500)
                }
            }
            //使用普通进度条
            else if (MediaInfo_VideoDuration / 1000 >= ScrollerInfo_MaxPicNumber) {
                ScrollerInfo_EachPicWidth = (40 * DisplayMetrics.density).toInt()
                ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration.div(100) * 100) / ScrollerInfo_MaxPicNumber
                ScrollerInfo_PicNumber = ScrollerInfo_MaxPicNumber
            } else {
                ScrollerInfo_EachPicWidth = (40 * DisplayMetrics.density).toInt()
                ScrollerInfo_PicNumber = (MediaInfo_VideoDuration / 1000) + 1
                ScrollerInfo_EachPicDuration = (MediaInfo_VideoDuration.div(100) * 100) / ScrollerInfo_PicNumber
            }

            //移除查询参数
            val MediaInfo_AbsolutePath_clean = MediaInfo_AbsolutePath.substringBefore("?")


            //进度条边界设置
            withContext(Dispatchers.Main){
                setScrollerPadding()
            }


            //绑定Adapter
            //使用超长进度条
            if (vm.PREFS_UseLongScroller){
                withContext(Dispatchers.Main) {
                    scroller.adapter = PlayerScrollerLongAdapter(this@PlayerActivityTest,
                        MediaInfo_AbsolutePath_clean,
                        MediaInfo_FileName,
                        playerScrollerViewModel.thumbItems,
                        ScrollerInfo_EachPicWidth,
                        ScrollerInfo_PicNumber,
                        ScrollerInfo_EachPicDuration,
                        vm.PREFS_GenerateThumbSYNC,
                        scroller,
                        playerScrollerViewModel
                    )
                }
            }
            //使用标准进度条
            else{
                withContext(Dispatchers.Main) {
                    scroller.adapter = PlayerScrollerAdapter(this@PlayerActivityTest,
                        MediaInfo_AbsolutePath_clean,
                        MediaInfo_FileName,
                        playerScrollerViewModel.thumbItems,
                        ScrollerInfo_EachPicWidth,
                        ScrollerInfo_PicNumber,
                        ScrollerInfo_EachPicDuration,
                        vm.PREFS_GenerateThumbSYNC,
                        scroller,
                        playerScrollerViewModel
                    )
                }
            }


            //开启被控
            fun startSyncScrollerGapControl(){
                syncScrollRunnableGap = 0L
                lifecycleScope.launch {
                    delay(3000)
                    syncScrollRunnableGap = ((MediaInfo_VideoDuration / 1000) * (1000.0 / 3600)).toLong()
                    if (vm.PREFS_UseLongScroller){
                        syncScrollRunnableGap = 10L
                    }
                }
            }
            startSyncScrollerGapControl()
            if(vm.PREFS_LinkScroll){ startScrollerSync() }
            delay(200)
            startVideoTimeSync()

        }
    }
    //重读媒体信息并覆盖对应全局变量
    private fun getMediaInfo(uri: Uri){
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(this@PlayerActivityTest   , uri) }
        catch (_: Exception) {
            onDestroy_fromErrorExit = true
            showCustomToast("无法解码视频信息", Toast.LENGTH_SHORT, 3)
            showCustomToast("播放失败", Toast.LENGTH_SHORT, 3)
            val data = Intent().apply {
                putExtra("key", "NEED_REFRESH")
            }
            setResult(RESULT_OK, data)
            state_need_return = true
            finish()
            return
        }
        MediaInfo_AbsolutePath = getAbsoluteFilePath(this@PlayerActivityTest, MediaInfo_VideoUri).toString()
        MediaInfo_VideoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        MediaInfo_VideoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        MediaInfo_VideoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
        MediaInfo_FileName = (File(MediaInfo_AbsolutePath)).name
        vm.MediaInfo_FileName = MediaInfo_FileName
        retriever.release()
    }
    //开启播放新媒体项
    private fun startPlayNewItem(){
        //仅传入媒体uri (不推荐并弃用)
        //vm.setMediaUri(MediaInfo_VideoUri)

        //构建并传入完整媒体项
        val covers_path = File(filesDir, "miniature/cover")
        val cover_img_path = File(covers_path, "${MediaInfo_FileName.hashCode()}.webp")
        val cover_img_uri = if (vm.PREFS_InsertPreviewInMediaSession && cover_img_path.exists()) {
            try {
                FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", cover_img_path)
            }
            catch (e: Exception) {
                if (cover_img_path.canRead()) {
                    cover_img_path.toUri()
                } else {
                    null
                }
            }
        } else {
            null
        }
        val mediaItem = MediaItem.Builder()
            .setUri(MediaInfo_VideoUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(MediaInfo_FileName)
                    .setArtist(MediaInfo_VideoArtist)
                    .setArtworkUri( cover_img_uri )
                    .build()
            )
            .build()

        vm.setMediaItem(mediaItem)

    }

    private fun changeRepeatMode(){
        if (controller.repeatMode == REPEAT_MODE_OFF){
            controller.repeatMode = Player.REPEAT_MODE_ONE
            vm.player.repeatMode = Player.REPEAT_MODE_ONE
            notice("已切换为 单集循环",2000)
        }
        else if (controller.repeatMode == Player.REPEAT_MODE_ONE){
            controller.repeatMode = Player.REPEAT_MODE_ALL
            vm.player.repeatMode = Player.REPEAT_MODE_ALL
            notice("已切换为 列表循环",2000)
        }

    }
    //解析播放列表
    private fun parseMediaItemsRobust(input: String): List<MediaItem>{
        return input.split("MediaItem_video(")
            .drop(1)
            .mapNotNull { section ->

                val endIndex = section.indexOf(')')
                if (endIndex == -1) return@mapNotNull null

                val itemContent = section.substring(0, endIndex)
                parseMediaItemWithFallback(itemContent)
            }
    }
    private fun parseMediaItemWithFallback(content: String): MediaItem? {
        return try {
            //尝试键值对解析
            parseWithKeyValuePairs(content) ?: parseWithSimpleExtraction(content) //备用解析
        }
        catch (e: Exception) {
            null
        }
    }
    private fun parseWithKeyValuePairs(content: String): MediaItem? {

        val fieldMap = mutableMapOf<String, String>()

        val pattern = """(\w+)=([^,]+?(?=,\s*\w+=|$))""".toRegex()
        val matches = pattern.findAll(content)

        for (match in matches) {
            val key = match.groupValues[1].trim()
            var value = match.groupValues[2].trim()

            value = value.removeSuffix(",").trim()
            fieldMap[key] = value
        }

        val uri = fieldMap["uri"] ?: return null
        val mediaId = fieldMap["id"] ?: "unknown"
        val name = fieldMap["name"] ?: "无标题"
        val cover_img_path = File(cacheDir, "Media/${name.hashCode()}/cover/cover.jpg")



        return if(vm.PREFS_InsertPreviewInMediaSession){
            MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .setDisplayTitle(name)
                        .setArtworkUri(Uri.fromFile(cover_img_path))
                        .build()
                )
                .setTag(fieldMap)
                .build()
        }else{
            MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .setDisplayTitle(name)
                        .build()
                )
                .setTag(fieldMap)
                .build()
        }

    }
    private fun parseWithSimpleExtraction(content: String): MediaItem? {
        // 提取 uri（查找 content:// 模式）
        val uriPattern = """(content://[^,]+)""".toRegex()
        val uriMatch = uriPattern.find(content)
        val uri = uriMatch?.value ?: return null

        // 提取 id
        val idPattern = """id=(\d+)""".toRegex()
        val idMatch = idPattern.find(content)
        val mediaId = idMatch?.groupValues?.get(1) ?: "unknown"

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .build()
    }
    //确认关闭操作
    private fun EnsureExit(){
        onDestroy_fromEnsureExit = true
        //保存播放进度
        if (vm.PREFS_SavePositionWhenExit){
            val currentPosition = vm.player.currentPosition
            lifecycleScope.launch(Dispatchers.IO) {
                MediaItemRepo.get(this@PlayerActivityTest).update_State_PositionWhenExit(MediaInfo_FileName,currentPosition)
            }
        }
        //停止监听操作
        disposable?.dispose()
        OEL.disable()
        audioManager.unregisterAudioDeviceCallback(DeviceCallback)
        localBroadcastManager.unregisterReceiver(receiver)
        //停止UI端操作
        scroller.stopScroll()
        stopVideoSmartScroll()
        stopVideoSeek()
        stopScrollerSync()
        stopVideoTimeSync()
        //停止服务端操作
        stopBackgroundServices()
        PlayerSingleton.releasePlayer()
        stopFloatingWindow()
        finish()
    }
    //数据库预写
    private fun DataBasePreWrite(){
        lifecycleScope.launch(Dispatchers.IO) {
            MediaItemRepo.get(this@PlayerActivityTest).preset_all_row_default(MediaInfo_FileName)
        }
    }


    //Some CallBacks
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        EnterAnimationComplete = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //saveLongKeepItems(outState)
    }

    override fun onPause() {
        super.onPause()
        //关闭视频控制
        stopVideoSeek()
        stopVideoSmartScroll()
    }

    override fun onStop() {
        super.onStop()
        state_onBackground = true
        //退出应用
        if (!vm.onOrientationChanging){
            //关闭旋转监听器
            OEL.disable()
            //记录播放状态
            vm.wasPlaying = vm.player.isPlaying
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
        state_onBackground = false
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
            if (vm.wasPlaying) { vm.player.play() }
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
        //错误自动关闭:此状态下,监听器等内容并没有加载
        if (!onDestroy_fromErrorExit){
            disposable?.dispose()
            OEL.disable()
            audioManager.unregisterAudioDeviceCallback(DeviceCallback)
            audioManager.abandonAudioFocusRequest(focusRequest)
            localBroadcastManager.unregisterReceiver(receiver)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
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
            when (newIntent.action) {
                //系统面板：分享
                Intent.ACTION_SEND -> {
                    vm.state_FromSysStart = true
                    val uri = IntentCompat.getParcelableExtra(newIntent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                    //判断是否是同一个视频
                    if (uri == vm.MediaInfo_VideoUri) { return }
                    else{
                        //清除现有媒体
                        playEnd_fromClearMediaItem = true
                        vm.clearMediaItem()
                        //变更保存的信息
                        MediaInfo_VideoUri = uri
                        vm.MediaInfo_VideoUri = MediaInfo_VideoUri

                        //设置新的媒体项
                        vm.player.setMediaItem(MediaItem.Builder()
                            .setUri(MediaInfo_VideoUri)
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setTitle(MediaInfo_FileName)
                                .setArtist(MediaInfo_VideoArtist)
                                .build())
                            .build())
                    }
                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {
                    vm.state_FromSysStart = true
                    val uri = newIntent.data ?: return finish()
                    //判断是否是同一个视频
                    if (uri == vm.MediaInfo_VideoUri) { return }
                    else{
                        //清除现有媒体
                        playEnd_fromClearMediaItem = true
                        vm.clearMediaItem()
                        //变更保存的信息
                        MediaInfo_VideoUri = uri
                        vm.MediaInfo_VideoUri = MediaInfo_VideoUri

                        //设置新的媒体项
                        vm.player.setMediaItem(MediaItem.Builder()
                            .setUri(MediaInfo_VideoUri)
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setTitle(MediaInfo_FileName)
                                .setArtist(MediaInfo_VideoArtist)
                                .build())
                            .build())

                    }
                }
            }
        }
    }
    //用户交互监听器
    override fun onUserInteraction() {
        super.onUserInteraction()
        IDLE_Timer?.cancel()
    }
    //android:configChanges="orientation|screenSize|screenLayout"
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //横屏
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏时隐藏状态栏
            setStatusBarParams()
            //控件
            val displayManager = this.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = display?.rotation
            if (rotation == Surface.ROTATION_90) {
                setControllerLayerPadding("left")
            } //正向横屏
            else if (rotation == Surface.ROTATION_270) {
                setControllerLayerPadding("right")
            } //反向横屏
            else{
                setControllerLayerPadding("left")
            } //其他
            //进度条端点
            setScrollerPadding()
            //通知卡片
            setNoticeCardPosition("landscape")
            //启动隐藏控件倒计时
            startIdleTimer()

        }
        //竖屏
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //取消隐藏状态栏
            setStatusBarParams()
            //进度条端点
            setScrollerPadding()
            //控件
            setControllerLayerPadding("recover")
            //通知卡片
            setNoticeCardPosition("portrait")


        }
    }


    //Stable Functions
    //通知卡片位置设置:接收px值,需把dp转px
    private fun setNoticeCardPosition(type_portrait_or_landscape: String){
        if (type_portrait_or_landscape == "landscape"){
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(5f))
        }else if (type_portrait_or_landscape == "portrait") {
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(100f))
        }
    }
    //px转dp
    private fun dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    //设置项修改封装函数
    private fun changeStateSoundOnly(){
        if (vm.PREFS_OnlyAudio){
            //确保声音开启
            vm.recovery_AudioTrack()
            //仅播放音频：关闭视频轨道
            vm.close_VideoTrack()
            notice("已开启仅播放音频", 1000)
        } else {
            //仅播放音频：打开视频轨道
            vm.recovery_VideoTrack()
            notice("已关闭仅播放音频", 1000)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_SoundOnly(MediaInfo_FileName,vm.PREFS_OnlyAudio)
        }
    }
    private fun changeStateVideoOnly(){
        if (vm.PREFS_OnlyVideo){
            //确保视频开启
            vm.recovery_VideoTrack()
            //仅播放视频：关闭声音
            vm.close_AudioTrack()
            notice("已开启仅播放视频", 1000)
        } else {
            vm.recovery_AudioTrack()
            notice("已关闭仅播放视频", 1000)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_VideoOnly(MediaInfo_FileName,vm.PREFS_OnlyVideo)
        }
    }
    private fun changeStateAlwaysSeek(){
        if (vm.PREFS_UseDataBaseForScrollerSetting){
            if (vm.PREFS_AlwaysSeek){
                vm.PREFS_AlwaysSeek = false
                notice("已关闭AlwaysSeek", 3000)
            } else {
                vm.PREFS_AlwaysSeek = true
                notice("已开启AlwaysSeek", 3000)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d("SuMing", "存入数据库: ${vm.PREFS_AlwaysSeek}")
                MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_AlwaysSeek(MediaInfo_FileName,vm.PREFS_AlwaysSeek)
            }
        }
        else{
            if (vm.PREFS_AlwaysSeek){
                vm.PREFS_AlwaysSeek = false
                PREFS.edit { putBoolean("PREFS_AlwaysSeek", false).apply() }
                notice("已关闭AlwaysSeek", 3000)
            } else {
                vm.PREFS_AlwaysSeek = true
                PREFS.edit { putBoolean("PREFS_AlwaysSeek", true).apply() }
                notice("已开启AlwaysSeek", 3000)
            }
        }
    }
    private fun changeStateLinkScroll(){
        if (vm.PREFS_UseDataBaseForScrollerSetting){
            if (vm.PREFS_LinkScroll) {
                vm.PREFS_LinkScroll = false
                stopScrollerSync()
                scroller.stopScroll()
                notice("已关闭链接滚动条与视频进度", 2500)
            } else {
                vm.PREFS_LinkScroll = true
                notice("已将进度条与视频进度同步", 1000)
                isSeekReady = true
                scroller.stopScroll()
                startScrollerSync()
                stopVideoSeek()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_LinkScroll(MediaInfo_FileName,vm.PREFS_LinkScroll)
            }
        }
        else{
            if (vm.PREFS_LinkScroll) {
                vm.PREFS_LinkScroll = false
                stopScrollerSync()
                scroller.stopScroll()
                PREFS.edit { putBoolean("PREFS_LinkScroll", false).apply() }
                notice("已关闭链接滚动条与视频进度", 2500)
            } else {
                vm.PREFS_LinkScroll = true
                notice("已将进度条与视频进度同步", 1000)
                isSeekReady = true
                scroller.stopScroll()
                startScrollerSync()
                stopVideoSeek()
                PREFS.edit { putBoolean("PREFS_LinkScroll", true).apply() }
            }
        }
    }
    private fun changeStateTapJump(){
        if (vm.PREFS_UseDataBaseForScrollerSetting){
            if (vm.PREFS_TapJump){
                vm.PREFS_TapJump = false
                notice("已关闭TapJump", 3000)
            } else {
                vm.PREFS_TapJump = true
                notice("已开启TapJump", 3000)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                MediaItemRepo.get(this@PlayerActivityTest).update_PREFS_TapJump(MediaInfo_FileName,vm.PREFS_TapJump)
            }
        }
        else{
            if (vm.PREFS_TapJump){
                vm.PREFS_TapJump = false
                PREFS.edit { putBoolean("PREFS_TapJump", false).apply() }
                notice("已关闭TapJump", 3000)
            } else {
                vm.PREFS_TapJump = true
                PREFS.edit { putBoolean("PREFS_TapJump", true).apply() }
                notice("已开启TapJump", 3000)
            }
        }
    }
    //刷新视频总长度
    private fun refreshTimeLine(){
        timer_duration.text = FormatTime_onlyNum(MediaInfo_VideoDuration.toLong())
    }
    //状态栏配置
    @Suppress("DEPRECATION")
    private fun setStatusBarParams(){
        //竖屏
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            //控件层参数
            val controllerLayer = findViewById<ConstraintLayout>(R.id.ControllerLayer)
            ViewCompat.setFitsSystemWindows(controllerLayer, true)
            controllerLayer.requestLayout()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                    controllerLayer.updatePadding(top = systemBars.top)

                    WindowInsetsCompat.CONSUMED }
                window.decorView.post { window.insetsController?.let { controller ->
                    controller.show(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
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
        }
        //横屏
        else{
            //控件层参数
            val controllerLayer = findViewById<ConstraintLayout>(R.id.ControllerLayer)
            ViewCompat.setFitsSystemWindows(controllerLayer, false)
            controllerLayer.requestLayout()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                    controllerLayer.updatePadding(top = systemBars.top)

                    WindowInsetsCompat.CONSUMED }
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
        }

        //刷新旋转屏幕按钮
        val ButtonMaterialSwitchLandscape = findViewById<MaterialButton>(R.id.buttonMaterialSwitchLandscape)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityTest, R.color.ButtonBgClosed))
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityTest, R.color.ButtonBg))
        }

    }
    //屏幕尺寸配置
    private fun displayMetricsLoad(){
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }
    //进度条端点配置
    private fun setScrollerPadding(){
        displayMetricsLoad()
        scroller.layoutManager = LinearLayoutManager(this@PlayerActivityTest, LinearLayoutManager.HORIZONTAL, false)
        scroller.itemAnimator = null
        scroller.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        scroller.layoutParams.width = 0
        sidePadding = screenWidth / 2
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var scrollerMarginType: Int
            //华为
            if (Build.BRAND == "huawei" || Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR" || Build.BRAND == "honor") {
                scrollerMarginType = 2
                scroller.setPadding(
                    sidePadding + vm.statusBarHeight / 2,
                    0,
                    sidePadding + vm.statusBarHeight / 2 - 1,
                    0
                )
            }
            //三星
            else if (Build.BRAND == "samsung") {
                scrollerMarginType = 1
                scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }
            //其他机型
            else {
                scrollerMarginType = 1
                scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }
            //超长进度条
            if (vm.PREFS_UseCompatScroller) {
                if (scrollerMarginType == 2) {
                    scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                }
                else {
                    scroller.setPadding(sidePadding + vm.statusBarHeight / 2, 0, sidePadding + vm.statusBarHeight / 2 - 1, 0)
                }
            }
        }

        scrollerLayoutManager = scroller.layoutManager as LinearLayoutManager
        stopScrollerSync()
        if (vm.PREFS_LinkScroll) startScrollerSync()
    }
    //控件层移动
    private fun setControllerLayerPadding(flag_dodge_which_side: String){
        if (flag_dodge_which_side == "left"){
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight)
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (0)
            (TopBarArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight)
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (0)
        }
        else if (flag_dodge_which_side == "right"){
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight)
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (0)
            (TopBarArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight)
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (0)
        }else if (flag_dodge_which_side == "recover"){
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (0)
            (ButtonArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (0)
            (TopBarArea.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (0)
            (TopBarArea.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (0)
        }
    }
    //清除进度条截图
    private fun clearMiniature(){
        File(filesDir, "miniature/${MediaInfo_FileName.hashCode()}/scroller").deleteRecursively()
    }
    //视频区域抬高
    private fun MoveYaxisCalculate(){
        val displayMetrics = DisplayMetrics()

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val density = displayMetrics.density

        val heightPx = displayMetrics.heightPixels
        val heightHalfPx = (heightPx / 2.0).toFloat()
        val cardTopMarginPx = 300 * density

        vm.YaxisDestination = heightHalfPx - ((cardTopMarginPx + vm.statusBarHeight) / 2)

    }
    private fun MovePlayArea_down(){
        if (!vm.PREFS_EnablePlayAreaMove) return

        //取消Job:播放区域上移
        MovePlayAreaJob?.cancel()

        playerView.animate()
            .translationY(0f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(300)
            .start()
    }
    private fun MovePlayArea_up() {
        if (!vm.PREFS_EnablePlayAreaMove) return
        playerView.animate()
            .translationY(-(vm.YaxisDestination))
            .setInterpolator(DecelerateInterpolator())
            .setDuration(300)
            .start()
    }
    //震动控制
    @Suppress("DEPRECATION")
    private fun Context.vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    private fun vibrate() {
        if (vm.PREFS_VibrateMillis <= 0L) {
            return
        }
        val vib = this@PlayerActivityTest.vibrator()
        if (vm.PREFS_UseSysVibrate) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vib.vibrate(effect)
        }
        else{
            vib.vibrate(VibrationEffect.createOneShot(vm.PREFS_VibrateMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    //提取帧函数
    private fun ExtractFrame(videoPath: String, filename: String) {
        val frameExtractor = FrameExtractor(object : FrameListener {
            override fun onFrameExtracted(bitmap: Bitmap, presentationTimeUs: Long ) {
                val save_path = File(cacheDir, "Media/${filename.hashCode()}/frame/${presentationTimeUs}.jpg")
                save_path.parentFile?.mkdirs()
                bitmap.compress(JPEG, 80, save_path.outputStream())

            }
            override fun onExtractionFinished() {
                notice("提取完成", 3000)

            }
            override fun onExtractionError(message: String) {
                notice("提取失败: $message", 3000)

            }
        })
        frameExtractor.startExtraction(videoPath)

    }
    //截屏
    @SuppressLint("UseKtx")
    private fun captureScreenShot(){
        fun generateFileName(): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val current = LocalDateTime.now()
            return "IMG_${current.format(formatter)}"
        }
        fun handleSuccess(bitmap: Bitmap) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, generateFileName())
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Screenshots")
            }
            val imageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            imageUri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(JPEG, 100, stream)
                }
            }

            notice("已截屏并保存到系统截屏文件夹", 3000)

            if (vm.wasPlaying){ vm.player.play() }

        }
        notice("请稍等", 3000)
        vm.wasPlaying = vm.player.isPlaying
        vm.player.pause()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            val Bitmap = createBitmap(videoSizeWidth, videoSizeHeight)
            val surfaceView = playerView.videoSurfaceView as? SurfaceView
            val surface = surfaceView?.holder?.surface
            PixelCopy.request(surface!!, Bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        handleSuccess(Bitmap)
                    } else {
                        notice("截图失败", 3000)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }
    //更新封面
    @SuppressLint("UseKtx")
    private fun updateCover(filename: String) {
        fun handleSuccess(bitmap: Bitmap) {
            //创建文件占位并保存
            val cover_file = File(filesDir, "miniature/cover/${filename.hashCode()}.webp")
            cover_file.parentFile?.mkdirs()
            cover_file.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
            }
            //发布完成消息
            ToolEventBus.sendEvent_withExtraString(Event("PlayerActivity_CoverChanged", filename))
            showCustomToast("截取封面完成", Toast.LENGTH_SHORT,3)
            //恢复播放状态
            if (vm.wasPlaying){ vm.player.play() }
        }
        //记录原本的播放状态
        vm.wasPlaying = vm.player.isPlaying
        vm.player.pause()
        //发起截图
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            val Bitmap = createBitmap(videoSizeWidth, videoSizeHeight)
            val surfaceView = playerView.videoSurfaceView as? SurfaceView
            val surface = surfaceView?.holder?.surface
            PixelCopy.request(surface!!, Bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        handleSuccess(Bitmap)
                    } else {
                        notice("截图失败", 3000)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }
    //分享视频by uri
    private fun shareVideo(context: Context, videoUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "分享视频")
        context.startActivity(chooser)
    }
    //定时关闭倒计时:接收参数time是分钟
    private fun startTimerShutDown(time: Int ,flag_need_notice: Boolean){
        val ShotDownTime = time * 60_000L
        if (flag_need_notice) { notice("${time}分钟后自动关闭", 3000) }
        COUNT_Timer?.cancel()
        COUNT_Timer = object : CountDownTimer(ShotDownTime, 1000000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() { timerShutDown() }
        }.start()
    }
    private fun timerShutDown() {
        if (playEnd_NeedShutDown) {
            playEnd_NeedShutDown = false
            finishAndRemoveTask()
            val pid = Process.myPid()
            Process.killProcess(pid)
            exitProcess(0)
        }
        if (vm.PREFS_ShutDownWhenMediaEnd) {
            notice("本次播放结束后将关闭", 3000)
            playEnd_NeedShutDown = true
            val currentPosition = vm.player.currentPosition
            val duration = vm.player.duration
            val countSecond = (duration - currentPosition) / 1000L
            startTimerShutDown(countSecond.toInt(), false)
        }
        else{
            finishAndRemoveTask()
            val pid = Process.myPid()
            Process.killProcess(pid)
            exitProcess(0)
        }
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
        scroller.stopScroll()
        //显示控制
        //<editor-fold desc="显示控制(隐藏)">
        val ControllerLayer = findViewById<ConstraintLayout>(R.id.ControllerLayer)
        ControllerLayer.visibility = View.GONE
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
        scroller.stopScroll()
        //显示控制
        //<editor-fold desc="显示控制(隐藏)">
        val ControllerLayer = findViewById<ConstraintLayout>(R.id.ControllerLayer)
        ControllerLayer.animate().alpha(0f).setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { ControllerLayer.visibility = View.GONE }
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
        val ControllerLayer = findViewById<ConstraintLayout>(R.id.ControllerLayer)
        ControllerLayer.visibility = View.VISIBLE
        ControllerLayer.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { null }
            .start()
        //</editor-fold>
        setBackgroundVisible()
    }
    private fun setBackgroundVisible(){
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        if (vm.PREFS_UseBlackBackground){
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
        val playerViewContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerViewContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))

        val cover = findViewById<LinearLayout>(R.id.cover)
        cover.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))

        val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
        val scroller_area = findViewById<View>(R.id.scroller_area)

        recyclerView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))
        scroller_area.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))

        val top_line = findViewById<View>(R.id.player_scroller_top_line)
        val middle_line = findViewById<View>(R.id.player_scroller_center_line)
        top_line.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.player_scroller_top_line_black))
        middle_line.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))


    }
    //启动和关闭小窗
    private fun startFloatingWindow() {
        fun checkOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(this)
        }
        if (!checkOverlayPermission()){
            notice("请先开启悬浮窗权限", 1000)
            return
        }
        else{
            notice("尝试启动小窗", 1000)
            //启动小窗服务
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val intentFloatingWindow = Intent(applicationContext, FloatingWindowService::class.java)
            intentFloatingWindow.putExtra("VIDEO_SIZE_WIDTH", videoSizeWidth)
            intentFloatingWindow.putExtra("VIDEO_SIZE_HEIGHT", videoSizeHeight)
            intentFloatingWindow.putExtra("SCREEN_WIDTH", screenWidth)
            intentFloatingWindow.putExtra("SOURCE", "PlayerActivityTest")
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
    //设置状态栏样式:横屏时隐藏状态栏,包含通知卡片设置
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

            EnterAnimationComplete = true

            //控件位置动态调整
            val displayManager = this.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = display?.rotation
            //控件位置动态调整:正向横屏
            if (rotation == Surface.ROTATION_90) {
                setControllerLayerPadding("left")
            }
            //控件位置动态调整:反向横屏
            else if (rotation == Surface.ROTATION_270) {
                setControllerLayerPadding("right")
            }

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setNoticeCardPosition("portrait")
        }
    }
    //切换横屏
    private fun ButtonChangeOrientation(flag_short_or_long: String){
        //自动旋转关闭
        if (rotationSetting == 0){
            //当前为竖屏
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    vm.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (vm.OrientationValue == 1){
                    vm.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
                else if (vm.OrientationValue == 2){
                    vm.FromManualPortrait = false
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    vm.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
            }
            //当前为横屏
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.FromManualPortrait = true
                setOrientation_PORTRAIT()
            }
        }
        //自动旋转开启
        else if (rotationSetting == 1){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    vm.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (vm.OrientationValue == 1){
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setManual()
                    setOrientation_LANDSCAPE()
                }
                else if (vm.OrientationValue == 2){
                    vm.currentOrientation = 2
                    vm.LastLandscapeOrientation = 2
                    vm.setManual()
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    vm.currentOrientation = 1
                    vm.LastLandscapeOrientation = 1
                    vm.setManual()
                    setOrientation_LANDSCAPE()
                }
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.currentOrientation = 0
                vm.setManual()
                setOrientation_PORTRAIT()
            }
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setOrientation_PORTRAIT(){
        scroller.stopScroll()
        vm.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
    private fun setOrientation_LANDSCAPE(){
        scroller.stopScroll()
        vm.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }
    private fun setOrientation_REVERSE_LANDSCAPE(){
        scroller.stopScroll()
        vm.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun ExitByOrientation(){
        //退出前先转为竖屏
        if (vm.PREFS_SwitchPortraitWhenExit){
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.setManual()
                setOrientation_PORTRAIT()
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (vm.controllerHided){
                    notice("再按一次退出",2000)
                    setControllerVisible()
                    vm.controllerHided = false
                    lifecycleScope.launch{
                        delay(75)
                        vibrate()
                    }
                }
                //确认退出
                else{
                    if (EnterAnimationComplete){
                        EnsureExit()
                    } else{
                        EnterAnimationComplete = true
                        val data = Intent().apply { putExtra("NEED_CLOSE", "NEED_CLOSE") }
                        setResult(RESULT_OK, data)
                        finish()
                        return
                    }
                }
            }
        }
        //横屏可直接退出
        else{
            if (vm.controllerHided){
                notice("再按一次退出",2000)
                setControllerVisible()
                vm.controllerHided = false
                lifecycleScope.launch{
                    delay(75)
                    vibrate()
                }
            }
            //确认退出
            else{
                if (EnterAnimationComplete){ EnsureExit() }else{
                    EnterAnimationComplete = true
                    val data = Intent().apply { putExtra("NEED_CLOSE", "NEED_CLOSE") }
                    setResult(RESULT_OK, data)
                    finish()
                    return
                }
            }
        }

    }
    //开启/关闭后台播放服务
    private fun startBackgroundServices(){
        val intent = Intent(this, PlayerServiceTest::class.java)
        //传递媒体信息和配置信息
        intent.putExtra("info_to_service_MediaTitle", MediaInfo_FileName)
        intent.putExtra("info_to_service_PREFS_UseMediaSession", vm.PREFS_UseMediaSession)
        //正式开启服务
        startService(intent)
    }
    private fun stopBackgroundServices(){
        stopService(Intent(this, PlayerServiceTest::class.java))
    }
    //后台播放只播音轨+回到前台恢复音轨
    private fun startBackgroundPlay(){
        if (vm.PREFS_CloseVideoTrack && !vm.inFloatingWindow){
            CloseVideoTrackJob()
            vm.closeVideoTrackJobRunning = true
        }
    }
    private fun stopBackgroundPlay(){
        if (vm.PREFS_CloseVideoTrack){
            vm.recovery_VideoTrack()
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
    private fun requestAudioFocus(){
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        vm.wasPlaying = vm.player.isPlaying
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        vm.wasPlaying = vm.player.isPlaying
                        vm.player.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        vm.wasPlaying = vm.player.isPlaying
                        vm.player.pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (vm.wasPlaying){
                            vm.wasPlaying = false
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

    private fun playerReady(){
        isSeekReady = true
        if (playerReadyFrom_FirstEntry) {
            playerReadyFrom_FirstEntry = false
            vm.state_firstReadyReached = true
            isSeekReady = true

            stopScrollerSync()
            if (vm.PREFS_LinkScroll) startScrollerSync()

            //恢复保存的设置
            if (NeedRecoverySettings){
                if (NeedRecoverySetting_VideoOnly){
                    changeStateVideoOnly()
                }
                if (NeedRecoverySetting_SoundOnly){
                    changeStateSoundOnly()
                }
                if (NeedRecoverySetting_LastPosition != 0L){
                    vm.player.seekTo(NeedRecoverySetting_LastPosition)
                    notice("已定位到上次播放的位置", 3000)
                }else{
                    NeedRecoverySettings = false
                }
            }

            vm.global_videoDuration = vm.player.duration

            requestAudioFocus()

            playVideo()

            //隐藏遮罩
            val cover = findViewById<View>(R.id.cover)
            cover.visibility = View.GONE
            /*
            cover.animate().alpha(0f).setDuration(150)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { cover.visibility = View.GONE }
                .start()

             */

            return
        }
        if (playerReadyFrom_SmartScrollLastSeek){
            playerReadyFrom_SmartScrollLastSeek = false
            isSeekReady = true
            //恢复播放状态
            if (vm.wasPlaying){
                playVideo()
            }else{
                vm.player.pause()
                vm.player.setPlaybackSpeed(1f)
            }
            vm.allowRecord_wasPlaying = true

            return
        }
        if (playerReadyFrom_LastSeek){
            playerReadyFrom_LastSeek = false
            isSeekReady = true
            vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            //恢复播放状态
            if (vm.wasPlaying){ playVideo() }
            vm.allowRecord_wasPlaying = true

            return
        }
        if (playerReadyFrom_NormalSeek){
            playerReadyFrom_NormalSeek = false
            isSeekReady = true
            return
        }

    }

    private fun playerEnd(){
        if (playEnd_fromClearMediaItem){
            playEnd_fromClearMediaItem = false
        }
        else{
            vm.playEnd = true
            notice("视频结束",1000)
        }
        vm.player.pause()
        //停止被控控件
        stopVideoTimeSync()
        stopScrollerSync()
        //播放结束时让控件显示
        setControllerVisible()
        Handler(Looper.getMainLooper()).postDelayed({ stopScrollerSync() }, 100)
        IDLE_Timer?.cancel()
        //自动退出和循环播放控制
        if (vm.state_FromSysStart && vm.PREFS_ExitWhenEnd){
            finish()
        }
        //播放结束时关闭
        if (playEnd_NeedShutDown){
            finishAndRemoveTask()
            val pid = Process.myPid()
            Process.killProcess(pid)
            exitProcess(0)
        }
    }

    private fun pauseVideo(){
        vm.player.pause()

        vm.wasPlaying = false

        stopVideoTimeSync()
        stopScrollerSync()
    }

    private fun playVideo(){
        requestAudioFocus()
        vm.player.setPlaybackSpeed(1f)
        vm.player.play()
        if (vm.PREFS_LinkScroll){ startScrollerSync() }
        if (!vm.PREFS_OnlyVideo) {
            vm.player.volume = 1f
        }
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
    //合成绝对路径
    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
        val cleanUri = if (uri.scheme == null || uri.scheme == "file") {
            Uri.fromFile(File(uri.path?.substringBefore("?") ?: return null))
        } else {
            uri
        }
        val absolutePath: String? = when (cleanUri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                context.contentResolver.query(cleanUri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) else null
                }
            }
            ContentResolver.SCHEME_FILE    -> cleanUri.path
            else                           -> cleanUri.path
        }

        return absolutePath?.takeIf { File(it).exists() }
    }

    private fun ButtonRefresh(){
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
        //状态栏设置
        AppBarSetting()
        //恢复控件显示状态
        if (vm.controllerHided){
            setControllerInvisibleNoAnimation()
        }
        //显示参数读取
        displayMetricsLoad()
    }
    //格式化时间显示
    @SuppressLint("DefaultLocale")
    private fun FormatTime_onlyNum(milliseconds: Long): String {
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
    private fun FormatTime_withChar(raw: Long): String {
        val cent  = raw % 1000
        val totalSec = raw / 1000
        val min  = totalSec / 60
        val sec  = totalSec % 60
        return "%02d:%02d.%03d".format(min, sec, cent)
    }

    //Runnable:根据视频时间更新进度条位置
    private val syncScrollTaskHandler = Handler(Looper.getMainLooper())
    private val syncScrollTask = object : Runnable {
        @SuppressLint("ServiceCast")
        override fun run() {

            syncScrollRunnableRunning = true
            if (ScrollerInfo_EachPicDuration == 0){ return }

            scrollParam1 = ( vm.player.currentPosition / ScrollerInfo_EachPicDuration ).toInt()
            scrollParam2 = (( vm.player.currentPosition - scrollParam1 * ScrollerInfo_EachPicDuration ) * ScrollerInfo_EachPicWidth / ScrollerInfo_EachPicDuration ).toInt()

            if (vm.playEnd && !vm.player.isPlaying){
                scrollParam1 = scrollParam1 - 1
                scrollParam2 = 150
                scrollerLayoutManager.scrollToPositionWithOffset(scrollParam1, -scrollParam2)
                syncScrollRunnableRunning = false
            }
            else{

                scrollerLayoutManager.scrollToPositionWithOffset(scrollParam1, -scrollParam2)

                syncScrollTaskHandler.postDelayed(this, 1)
            }
        }
    }
    private fun startScrollerSync() {
        if (syncScrollRunnableRunning){
            return
        }
        scrollerLayoutManager = scroller.layoutManager as LinearLayoutManager
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
            videoTimeSyncHandler_currentPosition = vm.player.currentPosition

            timer_current.text = FormatTime_onlyNum(videoTimeSyncHandler_currentPosition)

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
    var lastSeekExecuted = false
    private val videoSmartScrollHandler = Handler(Looper.getMainLooper())
    private var videoSmartScroll = object : Runnable{
        override fun run() {
            vm.allowRecord_wasPlaying = false
            val recyclerView = findViewById<RecyclerView>(R.id.rvThumbnails)
            var delayGap = if (scrollerState_Pressed){ 30L } else{ 30L }
            val videoPosition = vm.player.currentPosition
            val scrollerPosition =  vm.player.duration * (recyclerView.computeHorizontalScrollOffset().toFloat()/recyclerView.computeHorizontalScrollRange())
            vm.player.volume = 0f
            if (scrollerState_Moving) {
                if (vm.player.currentPosition > scrollerPosition - 100) {
                    vm.player.pause()
                }else{
                    val positionGap = scrollerPosition - videoPosition
                    var speed5 = (((positionGap / 100).toInt()) /10.0).toFloat()

                    if (speed5 > lastPlaySpeed){
                        speed5 = speed5 + 0.2f
                    }else if(speed5 < lastPlaySpeed){
                        speed5 = speed5 - 0.2f
                    }


                    val MAX_EFFICIENT_SPEED = 20.0f
                    speed5 = speed5.coerceAtMost(MAX_EFFICIENT_SPEED)


                    if (speed5 > 0f){ vm.player.setPlaybackSpeed(speed5) }
                }
                videoSmartScrollHandler.postDelayed(this,delayGap)
            }
            else{
                if (lastSeekExecuted) return
                lastSeekExecuted = true

                global_SeekToMs = scrollerPosition.toLong()
                vm.player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                smartScrollRunnableRunning = false
                playerReadyFrom_SmartScrollLastSeek = true
                startSmartScrollLastSeek()
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
        if (smartScrollRunnableRunning) return
        smartScrollRunnableRunning = true
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
            //标记位更改
            videoSeekHandlerRunning = true

            //计算目标位置
            VideoSeekHandler_totalWidth = scroller.computeHorizontalScrollRange()
            VideoSeekHandler_offset = scroller.computeHorizontalScrollOffset()
            VideoSeekHandler_percent = VideoSeekHandler_offset.toFloat() / VideoSeekHandler_totalWidth
            VideoSeekHandler_seekToMs = (VideoSeekHandler_percent * vm.player.duration).toLong()
            //反向滚动时防止seek到前面
            if (scrollerState_BackwardScroll){
                if (vm.PREFS_AlwaysSeek) {
                    if (VideoSeekHandler_seekToMs < vm.player.currentPosition){
                        vm.player.pause()
                        if (VideoSeekHandler_seekToMs < 50){
                            playerReadyFrom_LastSeek = true
                            vm.player.seekTo(0)
                        }
                        else{
                            if (isSeekReady){
                                isSeekReady = false
                                playerReadyFrom_NormalSeek = true
                                vm.player.seekTo(VideoSeekHandler_seekToMs)
                            }
                        }
                    }
                }
                else{
                    vm.player.pause()
                    if (VideoSeekHandler_seekToMs < 50){
                        playerReadyFrom_LastSeek = true
                        vm.player.seekTo(0)
                    }
                    else{
                        if (isSeekReady){
                            isSeekReady = false
                            playerReadyFrom_NormalSeek = true
                            vm.player.seekTo(VideoSeekHandler_seekToMs)
                        }
                    }
                }
            }
            //正向seek
            else{
                vm.player.pause()
                if (isSeekReady){
                    isSeekReady = false
                    playerReadyFrom_NormalSeek = true
                    vm.player.seekTo(VideoSeekHandler_seekToMs)
                }
            }


            //决定继续运行或是结束
            if (scrollerState_Pressed || scrollerState_Moving) {
                videoSeekHandler.postDelayed(this, videoSeekHandlerGap)
            }else{
                global_SeekToMs = VideoSeekHandler_seekToMs
                startLastSeek()
                videoSeekHandlerRunning = false
            }

        }
    }
    private fun startVideoSeek() {
        vm.playEnd = false
        if (videoSeekHandlerRunning) return
        //开启后不再允许记录播放状态
        vm.allowRecord_wasPlaying = false
        videoSeekHandler.post(videoSeek)
    }
    private fun stopVideoSeek() {
        videoSeekHandlerRunning = false
        videoSeekHandler.removeCallbacks(videoSeek)
    }
    //Runnable:lastSeek
    private val lastSeekHandler = Handler(Looper.getMainLooper())
    private var lastSeek = object : Runnable{
        override fun run() {
            if (isSeekReady){
                isSeekReady = false
                vm.player.setSeekParameters(SeekParameters.EXACT)
                playerReadyFrom_LastSeek = true
                vm.player.seekTo(global_SeekToMs)
            }
            else{ videoSeekHandler.post(this) }
        }
    }
    private fun startLastSeek() {
        lastSeekHandler.post(lastSeek)
    }
    //Runnable:SmartScrollLastSeek
    private val SmartScrollLastSeekHandler = Handler(Looper.getMainLooper())
    private var SmartScrollLastSeek = object : Runnable{
        override fun run() {
            if (isSeekReady){
                isSeekReady = false
                playerReadyFrom_SmartScrollLastSeek = true
                vm.player.seekTo(global_SeekToMs)
            }else{
                videoSeekHandler.post(this)
            }
        }
    }
    private fun startSmartScrollLastSeek() {
        SmartScrollLastSeekHandler.post(SmartScrollLastSeek)
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
        }else{
            showNoticeJob(text, duration)
        }
    }
    //Job:关闭视频轨道倒计时
    private var closeVideoTrackJob: Job? = null
    private fun CloseVideoTrackJob() {
        closeVideoTrackJob?.cancel()
        closeVideoTrackJob = lifecycleScope.launch {
            delay(30_000)
            vm.close_VideoTrack()
            vm.closeVideoTrackJobRunning = false
        }
    }
    //Job:播放区域上移
    private var MovePlayAreaJob: Job? = null
    private fun MovePlayAreaJob() {
        MovePlayAreaJob?.cancel()
        MovePlayAreaJob = lifecycleScope.launch {
            delay(500)
            MovePlayArea_up()
        }
    }
    //Job:长按横屏按钮
    private var SwitchLandscapeJob: Job? = null
    private fun SwitchLandscapeJob() {
        SwitchLandscapeJob?.cancel()
        SwitchLandscapeJob = lifecycleScope.launch {
            delay(500)
            ButtonChangeOrientation("long")
        }
    }

}

//封存的函数
/*
//拉到前台
private fun pullActivity() {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    //<uses-permission android:name="android.permission.REORDER_TASKS"/>
    val tasks = am.getRunningTasks(Int.MAX_VALUE)
    for (task in tasks) {
        if (task.topActivity?.packageName == packageName) {
            am.moveTaskToFront(task.id, 0)
            break
        }
    }

    val intent = Intent(this, this::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    startActivity(intent)
}

 */

