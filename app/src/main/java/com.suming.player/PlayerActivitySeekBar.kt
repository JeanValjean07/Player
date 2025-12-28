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
import android.widget.SeekBar
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
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.suming.player.ListManager.FragmentPlayList
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaItem.MediaItemSetting
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
import kotlin.math.pow

@UnstableApi
@Suppress("unused")
class PlayerActivitySeekBar: AppCompatActivity(){
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
    //媒体信息
    private lateinit var MediaInfo_VideoUri: Uri
    private var MediaInfo_VideoDuration = 0
    private var MediaInfo_AbsolutePath = ""
    private var MediaInfo_VideoTitle = ""
    private var MediaInfo_VideoArtist = ""
    private var MediaInfo_FileName = ""
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
    //倍速播放
    private var currentSpeed = 1.0f
    //播放结束需要定时关闭
    private var playEnd_NeedShutDown = false
    //VideoSeekHandler
    private var videoSeekHandlerGap = 0L
    //进度条参数
    private var sidePadding = 0
    private var screenWidth = 0
    private var screenHeight = 0

    //lateInitItem -控件
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


    private var videoTimeSyncHandler_currentPosition = 0L

    private var DataBaseProfile: MediaItemSetting? = null

    private lateinit var controller: MediaController

    //</editor-fold>
    //测试中变量
    //<editor-fold desc="测试中变量">

    private lateinit var seekBar: SeekBar


    private var playEnd_fromClearMediaItem = false
    private var onDestroy_fromErrorExit = false

    private var onDestroy_fromExitButKeepPlaying = false
    private var onDestroy_fromEnsureExit = false

    private var state_onBackground = false

    private var ExitJob_downMillis = 0L
    private var ExitJob_upMillis = 0L

    private var clickMillis_MoreOptionPage = 0L

    private lateinit var focusRequest: AudioFocusRequest

    private var state_need_return = false

    private var switchLandscape_downMillis = 0L
    private var switchLandscape_upMillis = 0L

    private lateinit var PREFS_MediaStore: SharedPreferences

    private var state_switch_item = false

    private lateinit var MediaSessionController: ListenableFuture<MediaController>



    //</editor-fold>

    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx","DEPRECATION", "CommitPrefEdits")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_seek_bar)
        //初始化部分控件
        fun lateInitItem(){
            seekBar = findViewById(R.id.seek_bar)
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

        /*
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
                    if (vm.PREFS_LinkScroll) startSeekBarSync()
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
                        stopSeekBarSync()
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

            ButtonRefresh()
            //移除遮罩
            val cover = findViewById<LinearLayout>(R.id.cover)
            cover.visibility = View.GONE
        }




        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = vm.player.duration
                    val seekToPosition = (progress.toFloat() / 1000 * duration.toFloat()).toLong()
                    vm.player.seekTo(seekToPosition)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopSeekBarSync()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                startSeekBarSync()
            }
        })

        //退出按钮
        ButtonExit.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    ToolVibrate().vibrate(this@PlayerActivitySeekBar)
                    ExitJob_upMillis = 0L
                    ExitJob_downMillis = System.currentTimeMillis()
                    ExitJob()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    ExitJob?.cancel()
                    ExitJob_upMillis = System.currentTimeMillis()
                    if (ExitJob_upMillis - ExitJob_downMillis < 300){
                        EnsureExit_close_all_stuff()
                    }
                    return@setOnTouchListener true
                }
            }
            onTouchEvent(event)
        }
        //更多选项
        val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
        TopBarArea_ButtonMoreOptions.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
            ToolVibrate().vibrate(this@PlayerActivitySeekBar)
            noticeCard.visibility = View.GONE
        }
        //按钮：暂停/继续播放
        val buttonPause = findViewById<FrameLayout>(R.id.buttonPause)
        buttonPause.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivitySeekBar)
            if (vm.player.isPlaying) {
                pauseVideo()
                stopSeekBarSync()
                notice("暂停", 1000)
                ButtonRefresh()
            } else {
                //播放或暂停
                lifecycleScope.launch {
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
                    ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
            ToolVibrate().vibrate(this@PlayerActivitySeekBar)

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
                    stopSeekBarSync()
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
                        startSeekBarSync()
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
                ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                                ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                                ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                                        ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                                ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                                ToolVibrate().vibrate(this@PlayerActivitySeekBar)
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
                    if (vm.PREFS_LinkScroll) startSeekBarSync()
                    notice("回到视频起始", 3000)
                }
                "PlayList" -> {
                    FragmentPlayList.newInstance().show(supportFragmentManager, "PlayerListFragment")
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
                                    MediaItemRepo.get(this@PlayerActivitySeekBar).saveSetting(newSetting)
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
                        MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_SavePositionWhenExit(MediaInfo_FileName,vm.PREFS_SavePositionWhenExit)
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
                        shareVideo(this@PlayerActivitySeekBar, MediaInfo_VideoUri)
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
                "UnBindBrightness" -> {
                    unBindBrightness()
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

                }
                "NextMedia" -> {

                }
                //直接切换
                "switchItem" -> {

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







        //表明页面状态 需要区分页面类型 flag_page_type
        vm.state_playerWithSeekBar = true
        //检查播放器状态
        checkPlayerState(3000)


        //系统手势监听：返回键重写
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //ExitByOrientation()
            }
        })

        */




    //onCreate END
    }


    /*
    //Testing Functions
    //显示播放错误
    private fun showPlayError(){
        val playErrorInfoText = findViewById<TextView>(R.id.playErrorInfo)
        playErrorInfoText.text = "疑似无法启动播放，点击此处重试"
        playErrorInfoText.setOnClickListener {
            playErrorInfoText.text = "正在重试启动播放器"
            restartPlayer()
        }
    }
    //播放器检测
    private fun checkPlayerState(Millis: Long){
        lifecycleScope.launch(Dispatchers.IO) {
            delay(Millis)
            if (!vm.state_firstReadyReached){
                withContext(Dispatchers.Main){
                    showPlayError()
                }
            }
        }
    }
    //重启播放器
    private fun restartPlayer(){
        PlayerSingleton.releasePlayer()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)

            withContext(Dispatchers.Main){
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
                                stopSeekBarSync()
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
               // PlayerSingleton.setMediaUri(MediaInfo_VideoUri)
            }

        }
    }
    //显示未准备通知
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

            }
            "SessionController_Previous" -> {

            }
            "SessionController_Play" -> {
                vm.wasPlaying = true
                if (vm.PREFS_LinkScroll && !state_onBackground ) startSeekBarSync()
                startVideoTimeSync()
            }
            "SessionController_Pause" -> {
                vm.wasPlaying = false
                stopSeekBarSync()
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

        //开启服务的方式
        startServiceOrSession()


    }
    //连接到媒体会话(不在会话中设置媒体项)
    private fun connectToMediaSession() {
        val SessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        MediaSessionController = MediaController.Builder(this, SessionToken).buildAsync()
        MediaSessionController.addListener({
            controller = MediaSessionController.get()
        }, MoreExecutors.directExecutor())
    }
    //开启服务:使用自定义通知或媒体会话
    private fun startServiceOrSession(){
        connectToMediaSession()
    }
    //重读媒体信息并覆盖对应全局变量
    private fun getMediaInfo(uri: Uri){
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(this@PlayerActivitySeekBar, uri) }
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
        MediaInfo_AbsolutePath = getAbsoluteFilePath(this@PlayerActivitySeekBar, MediaInfo_VideoUri).toString()
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
        //vm.setMediaUri(MediaInfo_MediaUri)

        //构建并传入完整媒体项
        val covers_path = File(filesDir, "miniature/cover")
        val cover_img_path = File(covers_path, "${MediaInfo_FileName.hashCode()}.webp")
        val cover_img_uri = if (cover_img_path.exists()) {
            try {
                FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", cover_img_path)
            }
            catch (_: Exception) {
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
    //确认关闭操作
    private fun EnsureExit_close_all_stuff(){
        onDestroy_fromEnsureExit = true
        //保存播放进度
        if (vm.PREFS_SavePositionWhenExit){
            val currentPosition = vm.player.currentPosition
            lifecycleScope.launch(Dispatchers.IO) {
                MediaItemRepo.get(this@PlayerActivitySeekBar).update_State_PositionWhenExit(MediaInfo_FileName,currentPosition)
            }
        }
        //停止监听操作
        disposable?.dispose()
        OEL.disable()
        audioManager.unregisterAudioDeviceCallback(DeviceCallback)
        localBroadcastManager.unregisterReceiver(receiver)
        //停止UI端操作
        stopSeekBarSync()
        stopVideoTimeSync()
        //停止服务端操作
        stopBackgroundServices()
        PlayerSingleton.releasePlayer()
        stopFloatingWindow()
        finish()
    }
    private fun EnsureExit_but_keep_playing(){
        onDestroy_fromExitButKeepPlaying = true
        //停止监听操作
        disposable?.dispose()
        OEL.disable()
        audioManager.unregisterAudioDeviceCallback(DeviceCallback)
        localBroadcastManager.unregisterReceiver(receiver)
        //停止UI端操作
        stopSeekBarSync()
        stopVideoTimeSync()
        //不停止服务端操作
        //stopBackgroundServices()
        //PlayerSingleton.releasePlayer()
        //stopFloatingWindow()
        finish()
    }
    //数据库预写
    private fun DataBasePreWrite(){
        lifecycleScope.launch(Dispatchers.IO) {
            MediaItemRepo.get(this@PlayerActivitySeekBar).preset_all_row_default(MediaInfo_FileName)
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
        stopSeekBarSync()
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
            if (vm.PREFS_LinkScroll && vm.player.isPlaying) startSeekBarSync()
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
            stopVideoTimeSync()
            stopSeekBarSync()
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
            //加载屏幕信息
            DisplayMetrics()
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
            //通知卡片
            setNoticeCardPosition("landscape")
            //启动隐藏控件倒计时
            startIdleTimer()

        }
        //竖屏
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //取消隐藏状态栏
            setStatusBarParams()
            //加载屏幕信息
            DisplayMetrics()
            //控件
            setControllerLayerPadding("recover")
            //通知卡片
            setNoticeCardPosition("portrait")

        }
    }


    //Stable Functions
    //解除亮度控制
    private fun unBindBrightness(){
        val windowInfo = window.attributes

        windowInfo.screenBrightness = -1f
        window.attributes = windowInfo

        vm.BrightnessChanged = false

        showCustomToast("已解除亮度控制,现在您可以使用系统亮度控制了", Toast.LENGTH_SHORT, 3)
    }
    //通知卡片位置设置:接收px值,需把dp转px
    private fun setNoticeCardPosition(type_portrait_or_landscape: String){
        if (type_portrait_or_landscape == "landscape"){
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(5f))
        }else if (type_portrait_or_landscape == "portrait") {
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(100f))
        }
    }
    //dp转px
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
            MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_SoundOnly(MediaInfo_FileName,vm.PREFS_OnlyAudio)
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
            MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_VideoOnly(MediaInfo_FileName,vm.PREFS_OnlyVideo)
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
                MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_AlwaysSeek(MediaInfo_FileName,vm.PREFS_AlwaysSeek)
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
                stopSeekBarSync()
                notice("已关闭链接滚动条与视频进度", 2500)
            } else {
                vm.PREFS_LinkScroll = true
                notice("已将进度条与视频进度同步", 1000)
                isSeekReady = true
                startSeekBarSync()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_LinkScroll(MediaInfo_FileName,vm.PREFS_LinkScroll)
            }
        }
        else{
            if (vm.PREFS_LinkScroll) {
                vm.PREFS_LinkScroll = false
                stopSeekBarSync()
                PREFS.edit { putBoolean("PREFS_LinkScroll", false).apply() }
                notice("已关闭链接滚动条与视频进度", 2500)
            } else {
                vm.PREFS_LinkScroll = true
                notice("已将进度条与视频进度同步", 1000)
                isSeekReady = true
                startSeekBarSync()
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
                MediaItemRepo.get(this@PlayerActivitySeekBar).update_PREFS_TapJump(MediaInfo_FileName,vm.PREFS_TapJump)
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
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivitySeekBar, R.color.ButtonBgClosed))
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivitySeekBar, R.color.ButtonBg))
        }

    }
    //屏幕尺寸配置
    private fun displayMetricsLoad(){
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
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
    //视频区域抬高(含Job)
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
    private var MovePlayAreaJob: Job? = null
    private fun MovePlayAreaJob() {
        MovePlayAreaJob?.cancel()
        MovePlayAreaJob = lifecycleScope.launch {
            delay(500)
            MovePlayArea_up()
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
        stopSeekBarSync()
        stopVideoTimeSync()
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
        stopSeekBarSync()
        stopVideoTimeSync()
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
        if(vm.PREFS_LinkScroll) { startSeekBarSync() }
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
        val cover = findViewById<LinearLayout>(R.id.cover)
        playerViewContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        cover.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
    }  //该函数需要区分页面类型 flag_page_type
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
            intentFloatingWindow.putExtra("SOURCE", "PlayerActivitySeekBar")   //该传入值需要区分页面类型 flag_page_type
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
        vm.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
    private fun setOrientation_LANDSCAPE(){
        vm.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }
    private fun setOrientation_REVERSE_LANDSCAPE(){
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
                        ToolVibrate().vibrate(this@PlayerActivitySeekBar)
                    }
                }
                //确认退出
                else{
                    if (EnterAnimationComplete){
                        EnsureExit_close_all_stuff()
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
                    ToolVibrate().vibrate(this@PlayerActivitySeekBar)
                }
            }
            //确认退出
            else{
                if (EnterAnimationComplete){ EnsureExit_close_all_stuff() }else{
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
        val intent = Intent(this, PlayerService::class.java)
        //传递媒体信息和配置信息
        intent.putExtra("info_to_service_MediaTitle", MediaInfo_FileName)
        //正式开启服务
        startService(intent)
    }
    private fun stopBackgroundServices(){
        stopService(Intent(this, PlayerService::class.java))
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

            stopSeekBarSync()
            if (vm.PREFS_LinkScroll) startSeekBarSync()

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
        /*
        if (playEnd_fromClearMediaItem){
            playEnd_fromClearMediaItem = false
        }

         */

    }

    private fun pauseVideo(){
        vm.player.pause()

        vm.wasPlaying = false

        stopVideoTimeSync()
        stopSeekBarSync()
    }

    private fun playVideo(){
        requestAudioFocus()
        vm.player.setPlaybackSpeed(1f)
        vm.player.play()
        if (vm.PREFS_LinkScroll){ startSeekBarSync() }
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
    private val syncSeekBarTaskHandler = Handler(Looper.getMainLooper())
    private val syncSeekBarTask = object : Runnable {
        override fun run() {
            //更新进度条位置
            val currentPosition = vm.player.currentPosition
            val duration = vm.player.duration
            if (duration > 0) {
                val progress = (currentPosition.toFloat() / duration.toFloat() * 1000).toInt()
                seekBar.progress = progress
            }


            syncSeekBarTaskHandler.postDelayed(this, 1)
        }
    }
    private fun startSeekBarSync() {




        syncSeekBarTaskHandler.post(syncSeekBarTask)
    }
    private fun stopSeekBarSync() {

        syncSeekBarTaskHandler.removeCallbacks(syncSeekBarTask)
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
    //显示通知(含Job)
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
    //关闭视频轨道倒计时(含Job)
    private var closeVideoTrackJob: Job? = null
    private fun CloseVideoTrackJob() {
        closeVideoTrackJob?.cancel()
        closeVideoTrackJob = lifecycleScope.launch {
            delay(30_000)
            vm.close_VideoTrack()
            vm.closeVideoTrackJobRunning = false
        }
    }
    //长按横屏按钮(含Job)
    private var SwitchLandscapeJob: Job? = null
    private fun SwitchLandscapeJob() {
        SwitchLandscapeJob?.cancel()
        SwitchLandscapeJob = lifecycleScope.launch {
            delay(500)
            ButtonChangeOrientation("long")
        }
    }
    //长按退出按钮(含Job)
    private var ExitJob: Job? = null
    private fun ExitJob() {
        ExitJob?.cancel()
        ExitJob = lifecycleScope.launch {
            delay(500)
            EnsureExit_but_keep_playing()
        }
    }

     */
      */


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

