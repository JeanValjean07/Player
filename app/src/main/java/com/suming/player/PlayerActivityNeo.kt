package com.suming.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.Display
import android.view.GestureDetector
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.IndepFragment.PlayerFragmentEqualizer
import com.suming.player.ActivityComponent.IndepFragment.PlayerFragmentMediaInfo
import com.suming.player.ActivityComponent.PlayerActivity.PlayerFragmentMoreButton
import com.suming.player.ActivityComponent.PlayerActivity.PlayerScrollerAdapter
import com.suming.player.ActivityComponent.PlayerActivity.PlayerViewModel
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncPack_ListManager.ListManagerFragment
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.ConnectCenter
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.FrameExtractor
import com.suming.player.FuncionalPack.FrameListener
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.MediaUriManager
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.FuncionalPack.ScrollerHelper
import com.suming.player.IndepService.FloatingWindowService
import com.suming.player.ViewWidget.CircleButton
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
@Suppress("NewApi","/unused")
class PlayerActivityNeo: AppCompatActivity(){
    //变量初始化
    //<editor-fold desc="变量初始化">
    //音量配置参数
    private var maxVolume = 0
    private var currentVolume = 0
    private var originalVolume = 0


    //功能:倍速滚动
    private var lastPlaySpeed = 0f

    //功能:VideoSeek



    //播放区域点击事件
    private var touchState_two_fingers = false
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
    private var touchCenter = false
    private var scrollDistance = 0
    //方向回调
    private var orientationChangeTime = 0L
    private var LastOrientationChangeTime = 0L



    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    //空闲定时器
    private var IDLE_Timer: CountDownTimer? = null
    private val IDLE_MS = 5_000L
    private var state_EnterAnimationCompleted = false
    //更新时间戳参数
    private var lastMillis = 0L
    //倍速播放
    private var currentSpeed = 1.0f

    private var singleTap = false






    private var onScroll_currentMillis = 0L
    private var onScroll_scrollPercent = 0f
    private var onScroll_seekToMs = 0L


    private var videoTimeSyncHandler_currentPosition = 0L


    //</editor-fold>
    //测试中变量
    //<editor-fold desc="测试中变量">




    private var clickMillis_MoreOptionPage = 0L



    private var switchLandscape_downMillis = 0L
    private var switchLandscape_upMillis = 0L



    private var touchState_need_exit = false
    private var touchState_left_noticed = false
    private var touchState_right_noticed = false
    private var touchState_need_exit_vibrated = false
    private var touchState_scroll_vibrated = false

    private var state_HeadSetInserted = false


    private var state_RootCardClosing = false

    private var touchCenterDistance = 0f

    private var state_onPlayError = false


    //</editor-fold>

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivityNeo: $msg")
        }
    }
    //context
    val context = this@PlayerActivityNeo
    //获取播放器引用
    private val player get() = PlayerSingleton.getInitPlayer()
    //连接到viewModel
    private val playerViewModel: PlayerViewModel by viewModels()
    //字段
    private val Undefined = ""



    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx","DEPRECATION", "CommitPrefEdits")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化
        init()

        //注册控件和手势
        register()

        //主业务逻辑
        mainBusiness()


        startPlayerStateObserver()


        //缓存需要频繁取用的变量+数值计算
        lifecycleScope.launch(Dispatchers.IO) {
            //播放区域移动动画
            playerViewModel.PRF_Cache_EnablePlayAreaMove = SettingsRequestCenter.get_PREFS_EnablePlayAreaMoveAnim(context)
            //播放区域移动动画距离计算(或许可以更持久化储存)
            if (playerViewModel.PRF_Cache_EnablePlayAreaMove){
                if (playerViewModel.PRF_Cache_EnablePlayAreaMove_Distance == 0f){
                    val displayMetrics = context.resources.displayMetrics
                    //屏幕宽高px
                    //val widthPx = displayMetrics.widthPixels
                    val heightPx = displayMetrics.heightPixels
                    //卡片上剩余高度px
                    val areaHeightPx = heightPx * 0.3
                    //中心点
                    val normalCenterMarginTop = heightPx / 2f
                    val areaCenterMarginTop = (areaHeightPx / 2f)

                    //中心点移动距离
                    playerViewModel.PRF_Cache_EnablePlayAreaMove_Distance = (normalCenterMarginTop - areaCenterMarginTop).toFloat()

                }
            }

            //寻帧时一律使用关键帧
            playerViewModel.PRF_Cache_UseSyncFrame_whenSeek = SettingsRequestCenter.get_PREFS_UseOnlySyncFrameWhenSeek(context)
            //进度条停止滚动时尾帧使用关键帧
            playerViewModel.PRF_Cache_UseSyncFrame_whenScrollerStop = SettingsRequestCenter.get_PREFS_UseSyncFrameWhenScrollerStop(context)

            //读取进度条配置(已不再支持为每个视频单独配置,但暂未从数据库移除数据)
            playerViewModel.PREFS_AlwaysSeek = SettingsRequestCenter.get_PREFS_EnableAlwaysSeek(context)
            playerViewModel.PREFS_LinkScroll = SettingsRequestCenter.get_PREFS_EnableLinkScroll(context)
            playerViewModel.PREFS_TapJump = SettingsRequestCenter.get_PREFS_EnableTapJump(context)

            //下滑距离(50dp转px)
            playerViewModel.value_scrollDownExitDistance = dp2px(50f)

            //视频寻帧间隔
            value_seekVideo_runnableGapMs = SettingsRequestCenter.get_value_seekVideo_runnableGapMs(context)
            //进度条更新间隔
            value_syncScroller_runnableGapMs = SettingsRequestCenter.get_value_syncScroller_runnableGapMs(context)
            //时间戳更新间隔
            value_timeStamp_updateGapMs = SettingsRequestCenter.get_value_timeStamp_updateGapMs(context)

        }

        //注册监听器
        lifecycleScope.launch(Dispatchers.Main) {
            //开启旋转监听器
            startOrientationListener()
        }

    }

    private fun init(){
        //显示配置
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_type_neo)

        //视图初始化
        scroller = findViewById(R.id.controller_scroller_recyclerView)
        controller_bottom_bar = findViewById(R.id.controller_bottom_bar)
        rootConstraint = findViewById(R.id.rootConstraint)
        controllerLayer = findViewById(R.id.controllerLayer)
        controller_top_bar = findViewById(R.id.controller_top_bar)
        controller_timer_current = findViewById(R.id.controller_timer_current)
        controller_timer_total = findViewById(R.id.controller_timer_total)
        noticeCapsule = findViewById(R.id.noticeCapsule)
        playerView = findViewById(R.id.playerView)

        //主线程设置项
        //是否开启了强制深色主题
        if (SettingsRequestCenter.get_PREFS_AlwaysUseDarkTheme(context)) delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        //是否开启强制高刷
        if (SettingsRequestCenter.get_PREFS_LockRefreshRate(context)) requestHighRefreshRate()

        //读取并缓存当前颜色模式
        isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        //读取当前屏幕方向
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //设置页面标识
        playerViewModel.state_player_type = playerViewModel.PAGE_TYPE_NEO


        //初始化界面参数
        updateScreenParameters()

        //获取自动旋转状态
        rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        //亮度
        val windowInfo = window.attributes
        if (!playerViewModel.BrightnessChanged) {
            var initBrightness = windowInfo.screenBrightness
            if (initBrightness < 0) {
                initBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                playerViewModel.BrightnessValue = initBrightness
            }
        }else{
            windowInfo.screenBrightness = playerViewModel.BrightnessValue
            window.attributes = windowInfo
        }
        //音量管理与提示
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeChangeGap = 750/maxVolume
        if (originalVolume == 0 && !playerViewModel.NOTICED_VolumeIsZero) {
            playerViewModel.NOTICED_VolumeIsZero = true
            notice("当前音量为0", 3000)
        }

    }

    //注册
    @SuppressLint("ClickableViewAccessibility")
    private fun register(){
        //注册进度条控制逻辑
        setupScrollerFunction()
        //注册控制按钮
        lifecycleScope.launch(Dispatchers.Main) {
            //退出按钮
            val ButtonExit = findViewById<CircleButton>(R.id.TopBarArea_ButtonExit)
            ButtonExit.setOnClickListener {
                exitActivity_ensure()
            }
            /*
            ButtonExit.setOnTouchListener { _, event ->
                when (event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                        ExitJob_upMillis = 0L
                        ExitJob_downMillis = System.currentTimeMillis()
                        ExitJob()
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP -> {
                        ExitJob?.cancel()
                        ExitJob_upMillis = System.currentTimeMillis()
                        if (ExitJob_upMillis - ExitJob_downMillis < 300){
                            exitActivity()
                        }
                        return@setOnTouchListener true
                    }
                }
                onTouchEvent(event)
            }

             */
            //更多选项
            val TopBarArea_ButtonMoreOptions = findViewById<CircleButton>(R.id.TopBarArea_ButtonMoreOptions)
            TopBarArea_ButtonMoreOptions.setOnClickListener {
                //防止快速点击
                if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                    return@setOnClickListener
                }
                clickMillis_MoreOptionPage = System.currentTimeMillis()
                //启动弹窗
                startMoreButtonFragment()
            }
            //提示卡点击时关闭
            val noticeCard = findViewById<CardView>(R.id.noticeCapsule)
            noticeCard.setOnClickListener {
                ToolVibrate().vibrate(this@PlayerActivityNeo)
                noticeCard.visibility = View.GONE
            }
            //暂停/继续播放
            val PauseButton = findViewById<CircleButton>(R.id.ButtonPause)
            PauseButton.setOnClickListener {
                //控制播放
                if (player.isPlaying) {
                    scroller.stopScroll()
                    pausePlay()
                    stopScrollerSync()
                    notice("暂停", 1000)
                    updateButtonState()
                }else{
                    scroller.stopScroll()
                    if (playerViewModel.playEnd) {
                        playerViewModel.playEnd = false
                        continuePlay()
                        notice("开始重播", 2000)
                        updateButtonState()
                    } else {
                        continuePlay()
                        notice("继续播放", 2000)
                    }
                }
                //确保播放区域在普通位置
                ensure_moveArea_place()
            }
            //切换横屏
            val ButtonLandscapeButton = findViewById<CircleButton>(R.id.ButtonLandscape)
            ButtonLandscapeButton.setOnTouchListener { _, event ->
                when (event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        ToolVibrate().vibrate(this@PlayerActivityNeo)
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
            //更多选项
            val ButtonMoreOption = findViewById<CircleButton>(R.id.ButtonMoreOption)
            ButtonMoreOption.setOnClickListener {
                //防止快速点击
                if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                    return@setOnClickListener
                }
                clickMillis_MoreOptionPage = System.currentTimeMillis()
                //启动弹窗
                startMoreButtonFragment()
            }
            //播放区域点击事件
            val gestureDetectorPlayArea = GestureDetector(
                this@PlayerActivityNeo,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        //控制播放
                        if (player.isPlaying) {
                            pausePlay()
                            stopScrollerSync()
                            notice("暂停播放", 1000)
                            updateButtonState()
                        } else {
                            if (playerViewModel.playEnd) {
                                playerViewModel.playEnd = false
                                continuePlay()
                                notice("开始重播", 1000)
                            } else {
                                continuePlay()
                                notice("继续播放", 1000)
                                updateButtonState()
                            }
                        }
                        //确保播放区域在普通位置
                        ensure_moveArea_place()

                        return true
                    }
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        if (ACTION_POINTER_DOWN) {
                            return true
                        }
                        //触发控件显示变更
                        changeBackgroundColor()
                        //确保播放区域在普通位置
                        ensure_moveArea_place()

                        return true
                    }
                    override fun onLongPress(e: MotionEvent) {
                        if (ACTION_POINTER_DOWN) return
                        if (!player.isPlaying) {
                            return
                        }
                        currentSpeed = player.playbackParameters.speed
                        player.setPlaybackSpeed(currentSpeed * 2.0f)
                        notice("倍速播放中(${currentSpeed * 2.0f}x)", 114514)
                        setControllerInvisibleNoAnimation()
                        longPress = true
                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                        super.onLongPress(e)
                    }
                    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float):Boolean {
                        if (touchLeft) {
                            //点击区域功能提示:仅一次
                            if (!touchState_left_noticed) {
                                //notice("继续上下滑动可调整亮度", 1000)
                                touchState_left_noticed = true
                            }
                            //累积滑动距离
                            scrollDistance += distanceY.toInt()
                            val windowInfo = window.attributes
                            //开始亮度修改
                            playerViewModel.BrightnessChanged = true
                            var newBrightness: Float
                            //上滑
                            if (scrollDistance > 50) {
                                newBrightness = (playerViewModel.BrightnessValue + 0.01f).toBigDecimal()
                                    .setScale(2, RoundingMode.HALF_UP).toFloat()
                                if (newBrightness in 0.0..1.0) {
                                    windowInfo.screenBrightness = newBrightness
                                    window.attributes = windowInfo
                                    playerViewModel.BrightnessValue = newBrightness
                                    notice("亮度 +1 (${(newBrightness * 100).toInt()}/100)", 1000)
                                } else {
                                    notice("亮度已到上限", 1000)
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                                    }
                                }
                            }
                            //下滑
                            else if (scrollDistance < -50) {
                                newBrightness = (playerViewModel.BrightnessValue - 0.01f).toBigDecimal()
                                    .setScale(2, RoundingMode.HALF_UP).toFloat()
                                if (newBrightness in 0.0..1.0) {
                                    windowInfo.screenBrightness = newBrightness
                                    window.attributes = windowInfo
                                    playerViewModel.BrightnessValue = newBrightness
                                    notice("亮度 -1 (${(newBrightness * 100).toInt()}/100)", 1000)
                                } else {
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                                    }
                                    notice("亮度已到下限", 1000)
                                }
                            }
                            //数值越界重置
                            if (scrollDistance > 50 || scrollDistance < -50) {
                                scrollDistance = 0
                            }
                        }
                        if (touchRight) {
                            //点击区域功能提示:仅一次
                            if (!touchState_right_noticed) {
                                //notice("继续上下滑动可调整音量", 1000)
                                touchState_right_noticed = true
                            }
                            //累积滑动距离
                            scrollDistance += distanceY.toInt()
                            //快速下滑紧急静音
                            if (scrollDistance < -150) {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                notice("快速静音", 2000)
                            }
                            //普通音量修改
                            if (scrollDistance > volumeChangeGap) {
                                var currentVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                currentVolume += 1
                                if (currentVolume <= maxVolume) {
                                    if (state_HeadSetInserted) {
                                        if (currentVolume <= (maxVolume * 0.6).toInt()) {
                                            audioManager.setStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                currentVolume,
                                                0
                                            )
                                            notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                        } else {
                                            if (!touchState_scroll_vibrated) {
                                                touchState_scroll_vibrated = true
                                                ToolVibrate().vibrate(this@PlayerActivityNeo)
                                            }
                                            notice(
                                                "佩戴耳机时,音量不能超过${(maxVolume * 0.6).toInt()},除非使用音量键调整",
                                                1000
                                            )
                                        }
                                    } else {
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            currentVolume,
                                            0
                                        )
                                        notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                    }
                                } else {
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                                    }
                                    notice("音量已到最高", 1000)
                                }
                            } else if (scrollDistance < -volumeChangeGap) {
                                var currentVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                currentVolume -= 1
                                if (currentVolume >= 0) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0
                                    )
                                    notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                                } else {
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                                    }
                                    notice("音量已到最低", 1000)
                                }
                            }
                            //数值越界置位
                            if (scrollDistance > 50 || scrollDistance < -50) {
                                scrollDistance = 0
                            }
                        }
                        if (touchCenter) {
                            state_RootCardClosing = true
                            touchCenterDistance += distanceY
                            if (touchCenterDistance < -playerViewModel.value_scrollDownExitDistance) {
                                touchState_need_exit = true
                                //振动:仅一次
                                if (!touchState_need_exit_vibrated) {
                                    touchState_need_exit_vibrated = true
                                    ToolVibrate().vibrate(this@PlayerActivityNeo)
                                }
                            } else {
                                touchState_need_exit = false
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
                        touchState_two_fingers = false
                        //重置部分状态
                        touchState_need_exit_vibrated = false
                        touchState_need_exit = false
                        touchState_left_noticed = false
                        touchState_right_noticed = false
                        touchState_scroll_vibrated = false
                        //记录1指初始坐标
                        finger1x = event.x
                        finger1y = event.y
                        //屏蔽纵向误触区域
                        if (finger1y < display_screen_height_pixels * 0.2
                            || finger1y > display_screen_height_pixels * 0.95){
                            return@setOnTouchListener false
                        }
                        //分割横向功能区:初步信息获取
                        if (finger1x < display_screen_width_pixels * 0.2) {
                            touchLeft = true
                        }
                        else if(finger1x > display_screen_width_pixels * 0.8){
                            state_HeadSetInserted = PlayerListener.getState_isHeadsetPlugged(this@PlayerActivityNeo)
                            touchRight = true
                        }
                        else{
                            touchCenter = true
                            touchCenterDistance = 0f
                        }

                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_UP -> {
                        //重置部分状态
                        touchState_two_fingers = false
                        touchLeft = false
                        touchRight = false
                        touchCenter = false
                        //重置部分数值
                        scrollDistance = 0
                        center0x = playerView.pivotX
                        center0y = playerView.pivotY
                        playerView.pivotX = center0x
                        playerView.pivotY = center0y
                        originalScale = definiteScale
                        //事件处理:长按
                        if (longPress) {
                            longPress = false
                            player.setPlaybackSpeed(currentSpeed)


                            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
                            NoticeCard.visibility = View.GONE
                        }
                        //事件处理:下滑退出
                        if (touchState_need_exit){
                            exitActivity_ensure()
                        }

                        //固定事件:点击后开启隐藏控件倒计时
                        startIdleTimer()

                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        //记录手指2的坐标
                        ACTION_POINTER_DOWN = true
                        val ptrIndex = event.actionIndex
                        finger2x = event.getX(ptrIndex)
                        finger2y = event.getY(ptrIndex)
                        if (event.pointerCount == 2){
                            //notice("双指缩放可缩放播放区域", 2000)
                            //更改标志位
                            touchState_two_fingers = true
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
                            touchState_two_fingers = false
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //双指在滑动
                        if (touchState_two_fingers){
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
        }
        //注册Fragment监听器
        lifecycleScope.launch(Dispatchers.Main) {
            //均衡器面板
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_equalizer, context) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //开启/退出事件
                    FragmentConnector.fragment_event_close -> {
                        //开启被控组件
                        startScrollerSync(2)
                        startVideoTimeSync()
                        //播放区域移移动(暂未启用)
                    }
                    FragmentConnector.fragment_event_open -> {
                        //关闭被控组件
                        stopScrollerSync()
                        stopVideoTimeSync()
                        //播放区域移移动(暂未启用)

                    }

                }
            }
            //更多操作面板
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_more_button, context) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //截屏
                    FragmentConnector.fragment_more_button_capture_frame -> {
                        captureScreenShot()
                    }
                    //回到视频起始
                    FragmentConnector.fragment_more_button_back_to_start -> {
                        player.seekTo(0)
                        player.play()
                        startScrollerSync(3)
                        notice("回到视频起始", 3000)
                    }
                    //打开播放列表
                    FragmentConnector.fragment_more_button_start_play_list -> {
                        startPlayListFragment()
                    }
                    //截取全部帧
                    FragmentConnector.fragment_more_button_extract_frame -> {
                        val file_path = PlayerInfoCenter.GET_Media_FilePath()
                        val fileName = PlayerInfoCenter.GET_Media_FileName()
                        if (file_path == "" && fileName == ""){
                            showCustomToast("失败", 3)
                            return@setFragmentResultListener
                        }
                        ExtractFrame(file_path, fileName)
                    }
                    //开启/关闭方向监听器
                    FragmentConnector.fragment_more_button_switch_ori_listener -> {
                        updateOrientationListener()
                    }

                    //进度条
                    "AlwaysSeek" -> {
                        changeStateAlwaysSeek(bundle.getBoolean("target"))
                    }
                    "LinkScroll" -> {
                        changeStateLinkScroll(bundle.getBoolean("target"))
                    }
                    "TapJump" -> {
                        changeStateTapJump(bundle.getBoolean("target"))
                    }

                    //开启小窗模式
                    FragmentConnector.fragment_more_button_start_pip_window -> {
                        startFloatingWindow()
                    }
                    //打开媒体信息面板
                    FragmentConnector.fragment_more_button_open_video_info -> {
                        startMediaIndoFragment()
                    }
                    //使用系统分享面板
                    FragmentConnector.fragment_more_button_sys_share_video -> {
                        val uriString = PlayerInfoCenter.GET_Media_UriStandard()
                        shareVideo(this@PlayerActivityNeo, uriString.toUri())
                    }
                    //更新视频封面
                    FragmentConnector.fragment_more_button_update_cover_frame -> {
                        val Method = bundle.getString(FragmentConnector.extra_key)
                        when(Method){
                            FragmentConnector.update_cover_frame_use_current_frame -> {
                                updateCoverFrame_captureCurrentFrame(PlayerInfoCenter.GET_Media_NUM_ID())
                            }
                            FragmentConnector.update_cover_frame_use_default_frame -> {
                                updateCoverFrame_useDefaultCover(PlayerInfoCenter.GET_Media_NUM_ID())
                            }
                            FragmentConnector.update_cover_frame_pick_local_frame -> {
                                showCustomToast("暂不支持此功能", 3)
                            }
                        }
                    }
                    //打开均衡器面板
                    FragmentConnector.fragment_more_button_open_equalizer -> startEqualizerFragment()
                    //清除当前进度条缩略图
                    FragmentConnector.fragment_more_button_clear_miniature -> clearScrollerFrames()
                    //解除亮度控制
                    FragmentConnector.fragment_more_button_unlock_brightness_control -> unlockBrightnessControl()
                    //开启/退出事件
                    FragmentConnector.fragment_event_close -> {
                        onFragmentClose()
                    }
                    FragmentConnector.fragment_event_open -> {
                        onFragmentOpen()
                    }
                    //重新绑定播放器视图
                    FragmentConnector.fragment_more_button_bind_play_view -> bindPlayerView()
                    //删除自定义封面图
                    FragmentConnector.fragment_more_button_delete_custom_cover -> deleteCustomCover()
                    //立即退出(来源于设置0秒后自动退出)
                    FragmentConnector.fragment_more_button_exit_right_now -> finish()
                    //刷新屏幕常亮状态
                    FragmentConnector.fragment_more_button_update_keep_screen_on -> updateKeepScreenOn()
                }
            }
            //播放列表
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_play_list, context) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //开启/退出事件
                    FragmentConnector.fragment_event_close -> {
                        onFragmentClose()
                    }
                    FragmentConnector.fragment_event_open -> {
                        onFragmentOpen()
                    }
                }
            }
            //媒体信息
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_media_info, this@PlayerActivityNeo) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //开启/退出事件
                    FragmentConnector.fragment_event_close -> {
                        onFragmentClose()
                    }
                    FragmentConnector.fragment_event_open -> {
                        onFragmentOpen()
                    }
                }
            }
        }
        //监听系统手势
        lifecycleScope.launch(Dispatchers.Main) {
            //监听系统手势返回
            onBackPressedDispatcher.addCallback(this@PlayerActivityNeo, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //退出入口
                    exitActivity()
                }
            })
        }

    }
    //主业务线
    private fun mainBusiness(){

        //获取原始链接并转换为标准格式链接
        val intentUri = getOriginalIntentUri(intent)
        //consoleLog("intentUri: $intentUri")
        val intentUriString = intentUri.toString()
        //consoleLog("intentUriString: $intentUriString")
        val intentUriStandard = MediaUriManager.getStandardMediaUri(intentUriString, this@PlayerActivityNeo)
        //consoleLog("intentUriStandard: $intentUriStandard")

        //获取正在播放信息
        val ongoingUriStandard = PlayerSingleton.GET_STE_currentMediaItem_Uri().second
        val ongoingMediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()

        //日志-获取到的信息
        //consoleLog("intentUriStandard: $intentUriStandard, ongoingUriStandard: $ongoingUriStandard")

        //决策程序
        if (intentUri == Uri.EMPTY && ongoingUriStandard == Uri.EMPTY ){
            queryManualInputUri()
        }else{
            //未传入原始链接
            if (intentUri == Uri.EMPTY){
                //检查有没有正在播放的
                if (ongoingUriStandard == Uri.EMPTY){
                    //没有正在播放的项
                    queryManualInputUri()
                }else{
                    //有正在播放的项
                    if (ongoingMediaType == MediaType.Video){
                        //正在播放的是视频,直接绑定
                        connectCurrentMedia()
                    }else{
                        finish()
                    }
                }
            }else{
                //传入原始链接
                if (intentUriStandard == Undefined){
                    //但链接转码失败,无法播放
                    showCustomToast("暂未适配路径式链接处理程序,无法播放,请使用“在其他应用打开”发起播放")
                    //停止播放引擎
                    PlayerSingleton.stopPlayEngine()
                    //退出活动
                    finish()

                    return
                }else{
                    //传入链接且可播放
                    if (intentUriStandard != ongoingUriStandard.toString()){
                        //传入链接,但与当前播放项不同,播放新项
                        //consoleLog("传入链接,但与当前播放项不同,播放新项")
                        startPlayNewMedia(intentUriString.toUri())

                    }else{
                        //传入链接,但与当前播放项相同,直接绑定
                        //consoleLog("传入链接,但与当前播放项相同,直接绑定")
                        if (ongoingMediaType == MediaType.Video){
                            //正在播放的是视频,直接绑定
                            connectCurrentMedia()
                        }else{
                            finish()
                        }
                    }
                }
            }
        }

    }



    //提取原始链接
    private fun getOriginalIntentUri(intent: Intent): Uri {
        //获取原始链接
        val intentUri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)?: Uri.EMPTY

        return if (intentUri == Uri.EMPTY){
            Uri.EMPTY
        }else{
            intentUri
        }
    }
    //手动输入链接并发起播放
    @SuppressLint("InflateParams")
    private fun queryManualInputUri(){
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_uri, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val ButtonEnsure: Button = dialogView.findViewById(R.id.dialog_button_ensure)
        val ButtonCancel: Button = dialogView.findViewById(R.id.dialog_button_cancel)
        var isUriValid = false
        //修改提示文本
        title.text = "未能获取到媒体链接"
        Description.text = "您可以手动输入链接"
        val Editable = Editable.Factory.getInstance().newEditable("content://media/external/video/media/")
        EditText.text = Editable
        ButtonEnsure.text = "确定"
        ButtonCancel.text = "取消并退出"
        //设置点击事件
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        ButtonEnsure.setOnClickListener {
            val userInput = EditText.text.toString()
            if (userInput.isEmpty()){
                showCustomToast("未输入内容", 3)
            }else{
                if (MediaInfoRetriever.isUriStringValid(this, userInput)){
                    isUriValid = true
                    dialog.dismiss()
                    startPlayNewMedia(userInput.toUri())
                }else{
                    showCustomToast("链接无效", 3)
                }
            }
        }
        ButtonCancel.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
        //接管返回操作
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            if (!isUriValid){ finish() }
        }
        //自动弹出键盘
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.setSelection(Editable.length)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    //开启播放新媒体项
    private fun startPlayNewMedia(uri: Uri){
        //设置新媒体项
        setNewMediaItem(uri)

        //开启屏幕常量
        setKeepScreenOn(true)
    }
    //连接正在播放的媒体
    private fun connectCurrentMedia(){
        //绑定播放器视图
        bindPlayerView()

        //添加播放器事件监听
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        state_PlayerListenerAdded = true

        //关闭遮罩
        closeCover()

        //更新屏幕常亮状态
        updateKeepScreenOn()

        //刷新进度条
        updateScrollerAdapter()
        //更新时间戳
        updateTimerWindow()
        //刷新控制按钮
        updateButtonState()

    }





    //播放器监听器
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    STATE_PlayerReady = true
                    playState_playerReady()
                }
                Player.STATE_ENDED -> {
                    playState_playEnd()
                }
                //播放器进入空闲状态(也是死亡状态,因为不可主动恢复,必须重建,等同于播放器已被销毁)
                Player.STATE_IDLE -> {
                    //consoleLog("onPlaybackStateChanged: STATE_IDLE")
                }
            }
        }
        //播放状态变更
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //播放状态变更
            isPlayingChanged()
        }
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize.width > 0 && videoSize.height > 0) {
                videoSizeWidth = videoSize.width
                videoSizeHeight = videoSize.height
            }
        }

        //媒体项变更(clearItem时会收到null)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            //consoleLog("onMediaItemTransition: $mediaItem, $reason")
            //媒体项变更
            if (mediaItem == null){
                //媒体项被清除
                onMediaItemCleared()
            }else{
                onMediaItemChanged(mediaItem)
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            state_onPlayError = true
            showCustomToast("播放错误: ${error.message}", 3)
        }
    }
    private var state_PlayerListenerAdded: Boolean = false
    //播放状态观察者
    private fun startPlayerStateObserver(){
        //目前只观察IDLE状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableIsPlayerIDLE.collect() { idle ->
                    //false时不操作
                    if (!idle) return@collect
                    consoleLog("观察到播放器IDLE状态变更: new idle: $idle")

                    onPlayEngineIDLE()


                }
            }
        }

    }
    //启动ExoPlayer
    private fun startExoPlayer(){
        //确保播放器已启动(已在类空间内启动过)
        //PlayerSingleton.getInitPlayer(this)
        //添加播放器事件监听
        startExoPlayerListener()

    }
    private fun startExoPlayerListener(){
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        state_PlayerListenerAdded = true
    }
    //设置新媒体项
    private fun setNewMediaItem(uri: Uri){
        //确保已启动播放器
        startExoPlayer()

        //显示遮罩
        showCover()
        //写入本次Ready来源
        Mark_playerReadyFrom = Mark_playerReadyFrom_setNewItem
        //确认设置新媒体项
        val success = PlayerSingleton.setMediaItem(uri, true)

        //成功时绑定一次播放器视图,作为保险
        if (success){
            bindPlayerView()
        }else{
            consoleLog("setNewMediaItem: 失败-设置新媒体项")
            //进入检查流程
            //1.检查文件是否还在
            val file = File(uri.path ?: "")
            if (!file.exists()){
                consoleLog("setNewMediaItem: 失败-文件已不存在")
                showCustomToast("此文件已不存在,请刷新列表", 3)
                finish()
                return
            }

            //如果未检查到问题,提示未知错误
            showCustomToast("播放失败:未知错误", 3)

        }

    }
    //媒体项变更回调(需升级为观察者观察统一状态)
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null){ return }
        //是音乐时主动退出页面

        if (PlayerInfoCenter.GET_Media_SPECIFIC_TYPE() != MediaType.Video){
            finish()
            return
        }

        //重新绑定播放器视图
        bindPlayerView()


        //刷新视频总长度
        updateTimerWindow()
        //刷新进度条
        updateScrollerAdapter()
        //刷新按钮
        updateButtonState()

    }


    //播放状态变更回调触发
    private fun isPlayingChanged(){
        //更新按钮状态
        updateButtonState()
        //更新屏幕常亮状态
        updateKeepScreenOn()
        //更新循环函数状态
        updateLoopFunctionState()

    }


    //更新循环函数状态
    private fun updateLoopFunctionState(){
        val isPlaying = PlayerSingleton.GET_STE_isNowPlaying()
        if (isPlaying){
            startScrollerSync(4)
            startVideoTimeSync()
        }else{
            stopScrollerSync()
            stopVideoTimeSync()
        }

    }

    //播放器进入空闲状态(也是死亡状态,因为不可主动恢复,必须重建,等同于播放器已被销毁)
    private fun onPlayEngineIDLE(){
        showCustomToast("播放器已离线", 3)
        //把播放器引擎关闭
        PlayerSingleton.stopPlayEngine()
        //离开活动
        finish()

    }

    //检查文件是否还存在
    private fun isFileExist(){
        //获取file_path
        val file_path = PlayerInfoCenter.GET_Media_FilePath()
        //consoleLog("isFileExist: file_path:$file_path")
        if (file_path.isEmpty()) return
        //检查是否有媒体正在在播放
        val isAnyMediaOngoing = isAnyMediaOngoing().first
        //consoleLog("isFileExist: isAnyMediaOngoing:$isAnyMediaOngoing")
        if (isAnyMediaOngoing) {
            val exist = MediaInfoRetriever.isFileExist(file_path)
            //consoleLog("isFileExist: exist:$exist")
            if (!exist){
                //文件不存在
                showCustomToast("媒体已失效")
                //清除当前项
                PlayerSingleton.clearMediaItem()
                //离开活动
                finish()

            }
        }

    }
    //检查是否有媒体正在在播放并获取链接
    private fun isAnyMediaOngoing(): Pair<Boolean, String>{
        //从播放器获取当前媒体状态
        val (ongoing,currentMediaItem) = PlayerSingleton.GET_STE_currentMediaItem_Uri()

        return if (ongoing){
            val currentMediaUriString = currentMediaItem.toString()
            Pair(true,currentMediaUriString)
        }else{
            Pair(false,"")
        }
    }

    //媒体项被清除回调
    private fun onMediaItemCleared(){
        showCustomToast("当前播放项已被清除", 3)

        //离开活动
        finish()
    }






    //确认关闭操作(已不再承担事务清理工作,仅存一个是否保持播放的判断,若无需考虑是否保持播放可直接使用finish())
    @SuppressLint("SourceLockedOrientationActivity")
    private fun exitActivity(){
        exitActivity_showController()
    }
    private fun exitActivity_recOrientation(){
        //退出前是否先转为竖屏
        val switchPortrait = SettingsRequestCenter.GET_PRF_SwitchPortrait_whenExit(this@PlayerActivityNeo)
        if (switchPortrait){
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape){
                playerViewModel.setManual()
                //触发旋转未竖屏
                setOrientation_PORTRAIT()
            }else{
                //确保控件显示时才能退出
                //exitActivity_showController()
                exitActivity_ensure()
            }
        }else{
            //退出前无需转为竖屏

            //确保控件显示时才能退出
            //exitActivity_showController()

            exitActivity_ensure()

        }
    }
    private fun exitActivity_showController(){
        if (!playerViewModel.state_controllerShowing){
            notice("再按一次退出",2000)
            //显示控件
            setControllerVisible()
            playerViewModel.state_controllerShowing = true
            //延迟振动避免与系统振动冲突
            lifecycleScope.launch{
                delay(75)
                ToolVibrate().vibrate(this@PlayerActivityNeo)
            }

        }else{
            //确认退出
            //exitActivity_ensure()

            exitActivity_recOrientation()

        }
    }
    private fun exitActivity_ensure(){
        val needCloseEngin = !SettingsRequestCenter.GET_PRF_EnableMiniView(this@PlayerActivityNeo)
        if (needCloseEngin){
            //consoleLog("关闭MiniView useSlideOutAnim = false")
            useSlideOutAnim = false
            //关闭播放器
            PlayerSingleton.clearMediaItem()
        }else{
            //consoleLog("开启MiniView useSlideOutAnim = true")
            useSlideOutAnim = true

        }

        //关闭活动
        finish()

        //发回主界面ActivityResultApi
        /*
        val data = Intent().apply { putExtra("key", "EnsureExitButKeepPlaying") }
        setResult(RESULT_OK, data)

         */
    }


    //方向监听器
    private lateinit var orientationListener : OrientationEventListener
    private fun setupOrientationListener() {
        if (state_orientationListenerInitialized) return
        state_orientationListenerInitialized = true

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                //把方向角数值映射为状态量
                if (orientation in 261..<280) {
                    playerViewModel.OrientationValue = 1
                } else if (orientation in 81..<100) {
                    playerViewModel.OrientationValue = 2
                } else if (orientation in 341..<360) {
                    playerViewModel.OrientationValue = 0
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
                    if (playerViewModel.currentOrientation == 0) {
                        //从 竖屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (playerViewModel.OrientationValue == 1) {
                            if (playerViewModel.Manual && playerViewModel.LastLandscapeOrientation == 1) return
                            playerViewModel.currentOrientation = 1
                            playerViewModel.LastLandscapeOrientation = 1
                            playerViewModel.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 竖屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (playerViewModel.OrientationValue == 2) {
                            if (playerViewModel.Manual && playerViewModel.LastLandscapeOrientation == 2) return
                            playerViewModel.currentOrientation = 2
                            playerViewModel.LastLandscapeOrientation = 2
                            playerViewModel.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                    }
                    //当前为正向横屏
                    else if (playerViewModel.currentOrientation == 1) {
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (playerViewModel.OrientationValue == 2) {
                            //更改状态并发起旋转
                            playerViewModel.currentOrientation = 2
                            playerViewModel.LastLandscapeOrientation = 2
                            playerViewModel.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (playerViewModel.OrientationValue == 0) {
                            if (playerViewModel.Manual) return
                            playerViewModel.currentOrientation = 0
                            playerViewModel.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                    //当前为反向横屏
                    else if (playerViewModel.currentOrientation == 2) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (playerViewModel.OrientationValue == 1) {
                            //更改状态并发起旋转
                            playerViewModel.currentOrientation = 1
                            playerViewModel.LastLandscapeOrientation = 1
                            playerViewModel.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 反向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (playerViewModel.OrientationValue == 0) {
                            if (playerViewModel.Manual) return
                            playerViewModel.currentOrientation = 0
                            playerViewModel.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                }
                //自动旋转关闭
                else if (rotationSetting == 0) {
                    if (!playerViewModel.FromManualPortrait) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (playerViewModel.OrientationValue == 1) {
                            //更改状态并发起旋转
                            setOrientation_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (playerViewModel.OrientationValue == 2) {
                            //更改状态并发起旋转
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                    }
                }
            }
        }
    }
    private var state_orientationListenerInitialized = false
    private var state_orientationListenerEnabled = false
    private fun startOrientationListener(){
        //未开启旋转监听器
        if (!SettingsRequestCenter.get_PREFS_EnableOrientationListener(this)) return
        //已有一例监听器
        if (state_orientationListenerEnabled) return
        //确保监听器已注册
        if (!state_orientationListenerInitialized) setupOrientationListener()

        orientationListener.enable()
        state_orientationListenerEnabled = true
    }
    private fun stopOrientationListener(){
        if (!state_orientationListenerEnabled) return
        orientationListener.disable()
        state_orientationListenerEnabled = false
    }



    //some callBacks
    override fun onPause() {
        super.onPause()

        //来自活动主动退出
        val isManualFinish = isFinishing
        val isActivityRebuild = isChangingConfigurations
        if (!isActivityRebuild){
            if (isManualFinish){

                playerViewModel.state_isFinishing = true
            }else{

                playerViewModel.state_isFinishing = false

                //是否后台播放
                PlayerSingleton.startBackgroundPlay()
            }
        }

        //关闭视频控制
        stopVideoSeek()
        stopVideoSmartScroll()
        stopVideoTimeSync()
        stopScrollerSync()
        //关闭旋转监听器
        stopOrientationListener()


    }

    override fun onResume() {
        super.onResume()

        //更新屏幕常亮状态
        updateKeepScreenOn()
        //判断是否继续播放
        if (playerViewModel.state_isFinishing){
            //consoleLog("onResume 来自活动销毁")
        }else{
            //consoleLog("onResume 来自活动暂退桌面")
            //是否后台播放
            PlayerSingleton.stopBackgroundPlay()
        }

        //检查文件是否存在
        isFileExist()


        //状态机(经典代码,别删除)
        /*
        //区分onResume原因：
        if (playerViewModel.state_onStopDecider_Running){
            //决策函数运行中：无法有效判断，但这种情况大概率是重建，除非回桌面后又立即点开
            //可能来自浮窗
            if (state_FromFloatingWindow){
                //关闭小窗服务
                stopFloatingWindow()
                //重新绑定播放器
                playerView.player = null
                playerView.player = player
            }
            //开启视频控件
            startScrollerSync()
            startVideoTimeSync()
        }else{
            //活动重建
            if (playerViewModel.state_onStop_ByReBuild){
                //开启视频控件
                startScrollerSync()
                startVideoTimeSync()
            }
            //首次启动 暂无动作 playerViewModel.state_onStop_ByRealExit
            //活动暂退桌面：小窗模式在这里包含
            if (playerViewModel.state_onStop_ByLossFocus){
                //可能来自浮窗
                if (state_FromFloatingWindow){
                    //关闭小窗服务
                    stopFloatingWindow()
                    //重新绑定播放器
                    playerView.player = null
                    playerView.player = player
                }
                //开始继续播放
                PlayerSingleton.ActivityOnResume(this)
                //开启视频控件
                startScrollerSync()
                startVideoTimeSync()
            }
            //通用
            //重置状态
            playerViewModel.set_onStop_all_reset()
        }

         */
    }

    override fun onDestroy() {
        super.onDestroy()

        //停止UI端操作
        scroller.stopScroll()
        stopVideoSmartScroll()
        stopVideoSeek()
        stopScrollerSync()
        stopVideoTimeSync()

        //关闭播放器状态监听
        player.removeListener(PlayerStateListener)

        //关闭本地监听器
        stopOrientationListener()
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        state_EnterAnimationCompleted = true

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
        //consoleLog("onNewIntent")
        if (newIntent?.action != null){
            when (newIntent.action) {
                //系统面板：分享
                Intent.ACTION_SEND -> {
                    val uri = IntentCompat.getParcelableExtra(newIntent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
                    val currentUri = PlayerSingleton.GET_STE_currentMediaItem_Uri().second
                    //判断是否是同一个视频
                    if (uri == currentUri){
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        setNewMediaItem(uri)
                    }
                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {
                    val uri = newIntent.data ?: return
                    val currentUri = PlayerSingleton.GET_STE_currentMediaItem_Uri()
                    consoleLog("currentUri: $currentUri, uri: $uri")
                    //判断是否是同一个视频
                    if (uri == currentUri){
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        setNewMediaItem(uri)
                    }
                }
                //常规重复调用(来自PortalActivity)
                "ACTION_NEW_INTENT" -> {
                    //consoleLog("onNewIntent ACTION_NEW_INTENT")
                    val uri = IntentCompat.getParcelableExtra(newIntent, "uri", Uri::class.java) ?: return
                    val currentUri = PlayerSingleton.GET_STE_currentMediaItem_Uri().second
                    //consoleLog("currentUri: $currentUri, uri: $uri")
                    //判断是否是同一个视频
                    if (uri == currentUri){
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        setNewMediaItem(uri)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //consoleLog("onSaveInstanceState")

    }
    //用户交互监听器
    override fun onUserInteraction() {
        super.onUserInteraction()
        IDLE_Timer?.cancel()
    }
    //android:configChanges="orientation|screenSize|screenLayout"
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //consoleLog("onConfigurationChanged:newConfig:${newConfig.orientation}")
        //通用行为
        //关闭所有Fragment
        closeAllDialogFragments()
        //界面配置
        when(newConfig.orientation){
            //切换至横屏
            Configuration.ORIENTATION_LANDSCAPE -> {
                isLandscape = true
                updateScreenParameters()
                //启动隐藏控件倒计时
                startIdleTimer()

            }
            //切换至竖屏
            Configuration.ORIENTATION_PORTRAIT -> {
                isLandscape = false
                updateScreenParameters()

            }
        }
    }
    //重写finish()以应用动画(活动内不应主动调用finish()而是使用exitActivity()入口)
    private var useSlideOutAnim = true
    override fun finish() {
        super.finish()
        //判断是否使用收起动画
        if (useSlideOutAnim){
            //使用收起动画
            @Suppress("DEPRECATION")
            overridePendingTransition(
                R.anim.slide_just_appear,
                R.anim.slide_out_vertical
            )
        }

    }


    //控制屏幕常量
    private fun setKeepScreenOn(on: Boolean){
        if (on){
            val continueOn = SettingsRequestCenter.GET_PRF_KeepScreenOn(context)
            if (continueOn){
                rootConstraint.keepScreenOn = true
            }else{
                rootConstraint.keepScreenOn = false
            }
        }else{
            //关闭屏幕常亮
            rootConstraint.keepScreenOn = false
        }

    }
    //更新屏幕常亮状态
    private fun updateKeepScreenOn(){
        val keepOn = SettingsRequestCenter.GET_PRF_KeepScreenOn(context)
        if (keepOn){
            rootConstraint.keepScreenOn = PlayerSingleton.GET_STE_isNowPlaying()
        }else{
            rootConstraint.keepScreenOn = false
        }

    }

    //启动更多操作面板
    private fun startMoreButtonFragment(){
        onFragmentStart()

        PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_more_button)
    }
    //启动均衡器面板
    private fun startEqualizerFragment(){
        onFragmentStart()

        PlayerFragmentEqualizer.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_equalizer)
    }
    //启动媒体信息面板
    private fun startMediaIndoFragment(){
        onFragmentStart()

        PlayerFragmentMediaInfo.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_media_info)
    }
    //启动播放列表面板
    private fun startPlayListFragment(){
        onFragmentStart()

        ListManagerFragment.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_play_list)
    }
    //关闭所有DialogFragment
    private fun closeAllDialogFragments(){
        val manager = supportFragmentManager
        val fragments = manager.fragments
        fragments.forEach { fragment ->
            if (fragment is DialogFragment && fragment.isVisible) {
                fragment.dismiss()
            }
        }
    }
    //面板弹出通用操作
    private fun onFragmentStart(){
        //TODO
    }

    //绑定播放器视图
    private fun bindPlayerView(){
        //consoleLog("bindPlayerView")
        playerView.player = null
        playerView.player = player
    }
    //修改方向监听器状态
    private fun updateOrientationListener(){
        //读取设置
        val enable = SettingsRequestCenter.get_PREFS_EnableOrientationListener(this)
        //开启或关闭
        if (enable){
            startOrientationListener()
            notice("已开启方向监听器", 1000)
        }else{
            stopOrientationListener()
            notice("已关闭方向监听器", 1000)
        }
    }
    //Fragment计数器
    private var fragment_count = 0
    private fun onFragmentOpen(){
        //增加计数
        fragment_count += 1
        //consoleLog("计数增加。当前Fragment计数：$fragment_count")
        //仅在数量为0时才执行
        if (fragment_count > 0){
            //关闭被控组件
            stopScrollerSync()
            stopVideoTimeSync()
            //播放区域移移动
            moveArea_playView_Up()
        }
    }
    private fun onFragmentClose(){
        //减少计数
        if (fragment_count <= 0){
            fragment_count = 0
            return
        }
        fragment_count -= 1
        //consoleLog("计数减少。当前Fragment计数：$fragment_count")
        if (fragment_count <= 0){
            //开启被控组件
            startScrollerSync(5)
            startVideoTimeSync()
            //播放区域移移动
            moveArea_playView_Down()
        }
    }


    //设置项修改封装函数
    private fun changeStateAlwaysSeek(target: Boolean){
        if (target){
            notice("已关闭AlwaysSeek", 3000)
        }else{
            notice("已开启AlwaysSeek", 3000)
        }
    }
    private fun changeStateLinkScroll(target: Boolean){
        if (target){
            playerViewModel.PREFS_LinkScroll = true
            notice("已将进度条与视频进度同步", 3000)
        }else{
            scroller.stopScroll()
            startScrollerSync(6)
            stopVideoSeek()
            stopScrollerSync()
            notice("已关闭链接滚动条与视频进度", 3000)
        }
    }
    private fun changeStateTapJump(target: Boolean){
        if (target){
            notice("已开启TapJump", 3000)
        }else{
            notice("已关闭TapJump", 3000)
        }
    }
    //清除进度条截图
    private fun clearScrollerFrames(){
        //获取当前视频ID
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        //删除进度条截图
        ScrollerHelper.deleteScrollerFrame(this, NUM_ID)
    }
    //视频区域抬高动画
    private var isPlayView_Up = false
    private fun moveArea_playView_Down(){
        //恢复动作不需要限定在竖屏下
        //if (isLandscape) return

        //是否开启视频区域抬高动画
        val isEnable = playerViewModel.PRF_Cache_EnablePlayAreaMove
        if (isEnable){
            isPlayView_Up = false

            playerView.animate()
                .translationY(0f)
                .setInterpolator(DecelerateInterpolator(3f))
                .setDuration(700)
                .start()

        }

    }
    private fun moveArea_playView_Up() {
        //仅在竖屏下有效
        if (isLandscape) return

        //是否开启视频区域抬高动画
        val isEnable = playerViewModel.PRF_Cache_EnablePlayAreaMove
        if (isEnable){
            isPlayView_Up = true
            //动画插值器
            val interpolator = PathInterpolatorCompat.create(
                0.4f, 0.0f,
                0.2f, 1.0f
            )
            //计算高度
            val moveDistance = playerViewModel.PRF_Cache_EnablePlayAreaMove_Distance

            playerView.animate()
                .translationY(-(moveDistance))
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        }

    }
    private fun ensure_moveArea_place(){
        if (isPlayView_Up){
            fragment_count = 0
            moveArea_playView_Down()
        }
    }
    //提取帧函数
    private fun ExtractFrame(videoPath: String, filename: String) {
        val frameExtractor = FrameExtractor(object : FrameListener {
            override fun onFrameExtracted(bitmap: Bitmap, presentationTimeUs: Long) {
                val save_path =
                    File(cacheDir, "Media/${filename.hashCode()}/frame/${presentationTimeUs}.jpg")
                save_path.parentFile?.mkdirs()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, save_path.outputStream())

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
    //截屏(要用到视频尺寸数值)
    private var videoSizeWidth = 0
    private var videoSizeHeight = 0
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            }

            notice("已截屏并保存到系统截屏文件夹", 3000)

            if (playerViewModel.wasPlaying){ player.play() }

        }
        notice("请稍等", 3000)
        playerViewModel.wasPlaying = player.isPlaying
        player.pause()
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
    private fun updateCoverFrame_captureCurrentFrame(media_api_id: Long){
        fun handleSuccess(bitmap: Bitmap) {
            //保存图片
            ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap_Custom(
                this@PlayerActivityNeo,
                MediaType.Video,
                media_api_id,
                bitmap
            )

            //恢复播放状态
            if (playerViewModel.wasPlaying){ player.play() }

            //获取当前文件路径
            val file_path = PlayerInfoCenter.GET_Media_FilePath()

            //发布完成消息
            updateCoverFrame_publishMessage(file_path, media_api_id)

            showCustomToast("截取封面完成", 3)

        }
        //记录原本的播放状态
        playerViewModel.wasPlaying = player.isPlaying
        player.pause()
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
    private fun updateCoverFrame_useDefaultCover(media_api_id: Long){
        //获取默认封面
        val bitmap = ArtworkCapturer.getDefaultVideoCoverFrame(this@PlayerActivityNeo)
        if (bitmap == null) {
            showCustomToast("默认封面素材提取失败", 3)
            return
        }

        //保存图片
        ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap_Custom(
            this@PlayerActivityNeo,
            MediaType.Video,
            media_api_id,
            bitmap
        )

        //获取当前文件路径
        val file_path = PlayerInfoCenter.GET_Media_FilePath()

        //发布完成消息
        updateCoverFrame_publishMessage(file_path, media_api_id)


        showCustomToast("已完成", 3)
    }
    private fun updateCoverFrame_publishMessage(file_path:String, media_api_id: Long){
        ConnectCenter.setCoverFrameUpdateEvent_targetFileInfo(file_path, media_api_id)
        ConnectCenter.setState_connector(ConnectCenter.connector_event_cover_frame_update)
    }
    private fun deleteCustomCover(){
        //获取当前视频ID
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        val file_path = PlayerInfoCenter.GET_Media_FilePath()
        if (NUM_ID == 0L || file_path.isEmpty()){
            showCustomToast("删除失败(媒体信息获取出错)", 3)
            return
        }
        //删除自定义封面图
        lifecycleScope.launch(Dispatchers.IO) {
            val success = ArtworkFrameManager.delete_artwork_custom_single_video(
                this@PlayerActivityNeo,
                NUM_ID
            )
            withContext(Dispatchers.Main){
                if (success){
                    //刷新封面
                    updateCoverFrame_publishMessage(file_path, NUM_ID)

                    showCustomToast("删除成功", 3)
                }else{
                    showCustomToast("删除失败", 3)
                }
            }
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
    //启动和关闭小窗
    private var state_FromFloatingWindow = false
    private fun startFloatingWindow() {
        //检查悬浮窗权限是否开启
        fun checkOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(this)
        }
        if (!checkOverlayPermission()){
            notice("请先开启悬浮窗权限", 1000)
            return
        }
        //通过检测，确认启动小窗
        else{
            //启动小窗服务
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val intentFloatingWindow = Intent(applicationContext, FloatingWindowService::class.java)
            intentFloatingWindow.putExtra("VIDEO_SIZE_WIDTH", videoSizeWidth)
            intentFloatingWindow.putExtra("VIDEO_SIZE_HEIGHT", videoSizeHeight)
            intentFloatingWindow.putExtra("SCREEN_WIDTH", screenWidth)
            intentFloatingWindow.putExtra("state_PlayerType", 1)   //该传入值需要区分页面类型 flag_page_type
            startService(intentFloatingWindow)
            //修改状态
            state_FromFloatingWindow = true
            //主动返回系统桌面
            val intentHomeLauncher = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intentHomeLauncher)
        }
    }
    private fun stopFloatingWindow() {
        state_FromFloatingWindow = false
        stopService(Intent(applicationContext, FloatingWindowService::class.java))
    }
    //切换横屏
    private fun ButtonChangeOrientation(flag_short_or_long: String){
        //自动旋转关闭
        if (rotationSetting == 0){
            //当前为竖屏
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    playerViewModel.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (playerViewModel.OrientationValue == 1){
                    playerViewModel.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
                else if (playerViewModel.OrientationValue == 2){
                    playerViewModel.FromManualPortrait = false
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    playerViewModel.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
            }
            //当前为横屏
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                playerViewModel.FromManualPortrait = true
                setOrientation_PORTRAIT()
            }
        }
        //自动旋转开启
        else if (rotationSetting == 1){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    playerViewModel.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (playerViewModel.OrientationValue == 1){
                    playerViewModel.currentOrientation = 1
                    playerViewModel.LastLandscapeOrientation = 1
                    playerViewModel.setManual()
                    setOrientation_LANDSCAPE()
                }
                else if (playerViewModel.OrientationValue == 2){
                    playerViewModel.currentOrientation = 2
                    playerViewModel.LastLandscapeOrientation = 2
                    playerViewModel.setManual()
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    playerViewModel.currentOrientation = 1
                    playerViewModel.LastLandscapeOrientation = 1
                    playerViewModel.setManual()
                    setOrientation_LANDSCAPE()
                }
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                playerViewModel.currentOrientation = 0
                playerViewModel.setManual()
                setOrientation_PORTRAIT()
            }
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setOrientation_PORTRAIT(){
        scroller.stopScroll()
        playerViewModel.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
    private fun setOrientation_LANDSCAPE(){
        scroller.stopScroll()
        playerViewModel.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }
    private fun setOrientation_REVERSE_LANDSCAPE(){
        scroller.stopScroll()
        playerViewModel.onOrientationChanging = true
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
    }

    //scroller控制器
    private fun setupScrollerFunction(){
        lifecycleScope.launch(Dispatchers.Main) {
            //Scroller事件 gestureDetector层 -onSingleTap -onDown
            val gestureDetectorScroller = GestureDetector(
                this@PlayerActivityNeo,
                object : GestureDetector.SimpleOnGestureListener() {
                    @SuppressLint("SetTextI18n")
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        singleTap = true
                        if (!playerViewModel.PREFS_TapJump) {
                            if (playerViewModel.PREFS_LinkScroll) {
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
                        val seekToMs = (percent * player.duration).toLong().coerceIn(0, player.duration)

                        if (seekToMs <= 0) {
                            return false
                        }
                        if (seekToMs >= player.duration) {
                            return false
                        }

                        //设置为寻找关键帧
                        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        //记录原播放状态
                        playState_singleTap_wasPlaying = player.isPlaying

                        //发送跳转命令
                        seekTo_Core(8, seekToMs, Mark_playerReadyFrom_SingleTap)

                        notice("跳转至${FormatTime_withChar(seekToMs)}", 1000)


                        return true
                    }
                })
            //Scroller事件 -原生层
            scroller.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                //承担 action_down 和 action_up
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when(e.action){
                        MotionEvent.ACTION_DOWN -> {
                            //consoleLog("ACTION_DOWN")

                            scrollerTouchState_ACTION_DOWN = true
                            scrollerDesire_Active = true

                            scrollerTouchState_DRAGGING = false
                            scrollerTouchState_SETTLING = false


                        }
                        MotionEvent.ACTION_UP -> {
                            //consoleLog("ACTION_UP")

                            scrollerTouchState_ACTION_DOWN = false

                            if (!scrollerTouchState_DRAGGING || !scrollerTouchState_SETTLING){
                                scrollerDesire_Active = false
                            }

                        }
                    }
                    //detector承担长按
                    gestureDetectorScroller.onTouchEvent(e)

                    return false
                }

                //以下未使用
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                    gestureDetectorScroller.onTouchEvent(e)
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
            //Scroller事件 -滚动层 -onScrollStateChanged -onScrolled
            scroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                //onScrollStateChanged 状态标记变更 (在action_down和action_up之后触发)
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    when(newState){
                        RecyclerView.SCROLL_STATE_DRAGGING -> {
                            //consoleLog("RecyclerView.SCROLL_STATE_DRAGGING")

                            scrollerTouchState_DRAGGING = true
                            scrollerTouchState_SETTLING = false

                            scrollerDesire_Active = true

                            //发生用户操作-停止界面组件同步
                            stopVideoTimeSync()
                            stopScrollerSync()

                            //记录当前播放状态
                            recordScrollerWasPlayingState()

                            return
                        }
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            //consoleLog("RecyclerView.SCROLL_STATE_SETTLING")

                            scrollerTouchState_DRAGGING = false
                            scrollerTouchState_SETTLING = true

                            scrollerDesire_Active = true

                            return
                        }
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            //consoleLog("RecyclerView.SCROLL_STATE_IDLE")

                            scrollerTouchState_DRAGGING = false
                            scrollerTouchState_SETTLING = false

                            scrollerDesire_Active = false

                            //清理状态
                            clearScrollerState()

                            //触发事件
                            if (playerViewModel.PREFS_LinkScroll) {
                                //检查次数  //备用条件 processed_seek_count == posted_seek_count
                                if (isSeekReady){
                                    //一个滚动事件完整跑完
                                    onScrollOnceComplete()

                                }else{
                                    //未完整跑完,重定向到Ready函数
                                    Mark_playerReadyFrom = Mark_playerReadyFrom_ChaseSeek

                                }

                            }


                            return
                        }
                    }
                }
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    //consoleLog("onScrolled")
                    if (!scrollerDesire_Active) {
                        //未主动操作时也偶尔触发,故基于scrollerDesire_Active过滤
                        return
                    }

                    //修改seek参数(慢速滚动时切到精确帧,快速滚动时切到关键帧)
                    if (scrollerTouchState_DRAGGING || scrollerTouchState_SETTLING){
                        //修改视频seek参数
                        if (dx == 1 || dx == -1){
                            if (playerViewModel.PRF_Cache_UseSyncFrame_whenSeek) {
                                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                            }else{
                                player.setSeekParameters(SeekParameters.EXACT)
                            }
                        } else {
                            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                    }else{
                        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }

                    //时间窗数值跟随进度条位置随动
                    onScroll_currentMillis = System.currentTimeMillis()
                    if (onScroll_currentMillis - lastMillis > value_timeStamp_updateGapMs) {
                        lastMillis = onScroll_currentMillis
                        if (playerViewModel.PREFS_LinkScroll) {

                            //计算对应时间戳
                            onScroll_scrollPercent = recyclerView.computeHorizontalScrollOffset().toFloat() / scroller.computeHorizontalScrollRange()
                            onScroll_seekToMs = (onScroll_scrollPercent * PlayerInfoCenter.GET_Media_Duration()).toLong()

                            //刷新时间显示
                            controller_timer_current.text = FormatTime_onlyNum(onScroll_seekToMs)

                        } else {
                            return
                        }
                    }

                    //记录运动方向
                    if (dx > 0){
                        //进度条视频正向走
                        scrollerMotionState_Forward = true

                    }else if(dx < 0){
                        //进度条视频反向走
                        scrollerMotionState_Forward = false

                    }

                    //执行随动操作
                    if (playerViewModel.PREFS_LinkScroll) {
                        //已开启视频跟随进度条滚动

                        if (scrollerMotionState_Forward){
                            //正向滚动

                            if (playerViewModel.PREFS_AlwaysSeek) {
                                //跳转方式:寻帧
                                startVideoSeek(1)
                            }else{
                                //跳转方式:倍速滚动
                                stopVideoSeek()
                                startVideoSmartScroll()
                            }
                        }else{
                            //反向滚动
                            startVideoSeek(2)

                            stopVideoSmartScroll()

                        }
                    }

                }
            })
        }
    }
    private var scrollerWasPlayingState_recorded = false //本轮滚动是否记录过播放状态变化
    private var scrollerLastAccurateSeekState_recorded = false //本轮滚动是否寻过精确尾帧
    private var playState_scroller_wasPlaying = false //本轮滚动是否播放
    private fun recordScrollerWasPlayingState(){
        if (scrollerWasPlayingState_recorded) return
        scrollerWasPlayingState_recorded = true
        //记录当前播放状态变化
        playState_scroller_wasPlaying = player.isPlaying
        //consoleLog("playState_scroller_wasPlaying : $playState_scroller_wasPlaying")

    }
    private var playState_singleTap_wasPlaying = false //singleTap专用wasPlaying
    //跑完一个完整滚动事件
    private fun onScrollOnceComplete(){
        //consoleLog("一个滚动事件完整跑完 ${System.currentTimeMillis()}", 1000)
        //清除playEnd状态
        playerViewModel.playEnd = false
        PlayerSingleton.cancelState_PlayEnd()

        //是否需要截取一次精确伪帧
        if (playerViewModel.PRF_Cache_UseSyncFrame_whenScrollerStop){
            if (scrollerLastAccurateSeekState_recorded){
                //恢复播放状态
                if (playState_scroller_wasPlaying){
                    continuePlay()
                }
                //重置为寻找关键帧
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }else{
                scrollerLastAccurateSeekState_recorded = true
                //寻一次尾帧
                standardSeekLoop_Core(true)
            }
        }else{
            //恢复播放状态
            if (playState_scroller_wasPlaying){
                continuePlay()
            }
            //重置为寻找关键帧
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        }
    }

    //进度条状态
    private var scrollerTouchState_DRAGGING = false
    private var scrollerTouchState_SETTLING = false
    private var scrollerTouchState_ACTION_DOWN = false
    private var scrollerMotionState_Forward = true
    private var scrollerDesire_Active = false //主动被动状态:触摸立即进入true,直到下一次自然停止
    private fun clearScrollerState(){

        scrollerTouchState_DRAGGING = false
        scrollerTouchState_SETTLING = false
        scrollerTouchState_ACTION_DOWN = false
        scrollerDesire_Active = false

        posted_seek_count = 0
        processed_seek_count = 0

        scrollerWasPlayingState_recorded = false
        scrollerLastAccurateSeekState_recorded = false

    } //重置为未触摸过的状态
    //状态playerReady
    private fun playState_playerReady(){
        //修改状态
        isSeekReady = true
        //检验来源
        when(Mark_playerReadyFrom){
            Mark_playerReadyFrom_NormalSeek -> {
                //consoleLog("Mark_playerReadyFrom_NormalSeek")

                //记录处理次数
                processed_seek_count++
                //consoleLog("processed_seek_count : $processed_seek_count")


            }
            Mark_playerReadyFrom_ChaseSeek -> {
                //一个滚动事件完整跑完
                onScrollOnceComplete()
            }
            Mark_playerReadyFrom_SingleTap -> {
                //consoleLog("Mark_playerReadyFrom_SingleTap")

                syncScrollTask_Core()
                //恢复播放状态
                if (playState_singleTap_wasPlaying){
                    continuePlay()
                }
            }
            //来自新的媒体设置成功
            Mark_playerReadyFrom_setNewItem -> {
                //开启控件显示
                stopScrollerSync()
                startScrollerSync(7)

                //启动播放
                continuePlay()

                //隐藏遮罩
                closeCover()
            }
            //其他非预期的来源
            else -> {
                //consoleLog("Mark_playerReadyFrom : $Mark_playerReadyFrom")
            }
        }

    }
    private var Mark_playerReadyFrom = Undefined
    private val Mark_playerReadyFrom_ChaseSeek = "Mark_playerReadyFrom_ChaseSeek"
    private val Mark_playerReadyFrom_SingleTap = "Mark_playerReadyFrom_SingleTap"
    private val Mark_playerReadyFrom_NormalSeek = "Mark_playerReadyFrom_NormalSeek"
    private val Mark_playerReadyFrom_setNewItem = "Mark_playerReadyFrom_setNewItem"
    //状态playEnd
    private fun playState_playEnd(){
        val loopMode = ListManagerHelper.getLoopMode(this)
        when (loopMode) {
            ListManagerHelper.LOOP_MODE_ONE -> {
                notice("单集循环", 3000)
            }
            ListManagerHelper.LOOP_MODE_ALL -> {

            }
            ListManagerHelper.LOOP_MODE_OFF -> {
                playerViewModel.playEnd = true
                notice("视频结束", 1000)
                //停止被控控件
                stopVideoTimeSync()
                stopScrollerSync()
                //播放结束时让控件显示
                setControllerVisible()
                Handler(Looper.getMainLooper()).postDelayed({ stopScrollerSync() }, 100)
                IDLE_Timer?.cancel()

            }
        }
    }
    //播放与暂停
    @Suppress("SameParameterValue")
    private fun pausePlay(){
        //调用暂停播放(确保活动内唯一调用)
        PlayerSingleton.pausePlay()

        //关闭屏幕常量
        setKeepScreenOn(false)

        //关闭本地界面更新
        stopVideoTimeSync()
        stopScrollerSync()
    }
    @Suppress("SameParameterValue")
    private fun continuePlay(need_requestFocus: Boolean = true){
        //调用继续播放(确保活动内唯一调用)
        PlayerSingleton.continuePlay(need_requestFocus)

        //开启屏幕常量
        setKeepScreenOn(true)

        //开启本地界面更新
        startScrollerSync(8)
        startVideoTimeSync()
    }

    //界面控件
    private lateinit var scroller : RecyclerView
    private lateinit var controller_bottom_bar : LinearLayout //底部按钮区域
    private lateinit var rootConstraint : ConstraintLayout //根约束布局
    private lateinit var controllerLayer : ConstraintLayout //控件层
    private lateinit var controller_top_bar : LinearLayout //顶部按钮区域
    private lateinit var controller_timer_current : TextView //当前时间
    private lateinit var controller_timer_total : TextView //总时间
    private lateinit var noticeCapsule : CardView //通知胶囊卡片
    private lateinit var playerView: PlayerView //播放区域
    //刷新视频总长度
    private fun updateTimerWindow(){
        //设置时间戳-总时长显示位
        val mediaDuration = PlayerInfoCenter.GET_Media_Duration()
        controller_timer_total.text = FormatTime_onlyNum(mediaDuration)
        //开始时间戳更新
        if (player.isPlaying) startVideoTimeSync()
    }
    //更新进度条
    private lateinit var scrollerLayoutManager: LinearLayoutManager
    private fun foldScrollerArea(){
        val player_scroller_center_line = findViewById<View>(R.id.player_scroller_center_line)
        val controller_scroller_container = findViewById<ConstraintLayout>(R.id.controller_scroller_container)
        val controller_bottom_padding = findViewById<View>(R.id.controller_bottom_padding)

        player_scroller_center_line.visibility = View.GONE

        controller_scroller_container.visibility = View.GONE

        controller_bottom_padding.background = null


    }
    private fun updateScrollerAdapter(){
        lifecycleScope.launch(Dispatchers.IO) {
            //初始化进度条布局
            scrollerLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            scroller.layoutManager = scrollerLayoutManager
            scroller.itemAnimator = null
            scroller.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            scroller.layoutParams.width = 0

            //获取屏幕density
            val density = display_screen_density
            if (density == 0f) return@launch
            ScrollerHelper.singleFrame_WidthPx = (40 * density).toInt()

            //计算进度条参数(委托给scrollerHelper)
            //先从信息中心拿到各种必要信息
            val uriNumOnly = PlayerInfoCenter.GET_Media_NUM_ID()
            val mediaDuration = PlayerInfoCenter.GET_Media_Duration()
            val absolutePath = PlayerInfoCenter.GET_Media_FilePath()
            if (uriNumOnly == 0L || mediaDuration == 0L || absolutePath == "" ){
                consoleLog("updateScrollerAdapter 进度条：获取信息无效，无法显示进度条")
                withContext(Dispatchers.Main) {
                    foldScrollerArea()
                    showCustomToast("发生错误,无法显示进度条")
                }
                return@launch
            }
            //委托给scrollerHelper处理
            val success = ScrollerHelper.prepareForNewMedia(uriNumOnly, mediaDuration, absolutePath)
            if (!success) {
                consoleLog("updateScrollerAdapter 进度条：信息有效，但解码失败，无法显示进度条")
                withContext(Dispatchers.Main) {
                    foldScrollerArea()
                    showCustomToast("发生错误,无法显示进度条")
                }
                return@launch
            }
            //初始化scrollerAdapter
            val scrollerAdapter = PlayerScrollerAdapter(context, mediaDuration,absolutePath)

            //应用
            withContext(Dispatchers.Main) {
                //设置进度条内边距
                setScrollerPadding()

                //绑定adapter
                scroller.adapter = scrollerAdapter

                //开启被控
                if (player.isPlaying){
                    startScrollerSync(9)
                }else{
                    syncScrollTask_Core()
                }


            }
        }
    }
    //缓存显示配置
    private var isDarkTheme = false  //深色模式
    private var isLandscape = false  //横屏
    //控件隐藏和显示
    private fun setControllerInvisibleNoAnimation() {
        //状态标记变更
        playerViewModel.state_controllerShowing = false

        //停止被控控件控制
        stopVideoTimeSync()
        //仅在新晋播放页使用
        stopScrollerSync()
        scroller.stopScroll()
        //仅在传统播放页使用
        //stopSeekBarSync()

        //隐藏控件并设置背景为黑色
        controllerLayer.visibility = View.GONE
        setBackgroundInvisible()
    }
    private fun setControllerInvisible() {
        //状态标记变更
        playerViewModel.state_controllerShowing = false

        //停止被控控件控制
        stopVideoTimeSync()
        //仅在新晋播放页使用
        stopScrollerSync()
        scroller.stopScroll()
        //仅在传统播放页使用
        //stopSeekBarSync()

        //隐藏控件并设置背景为黑色
        setBackgroundInvisible()
        controllerLayer.animate().alpha(0f).setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { controllerLayer.visibility = View.GONE }
            .start()


    }
    private fun setControllerVisibleNoAnimation() {
        //状态标记变更
        playerViewModel.state_controllerShowing = true

        //启动被控控件控制
        startVideoTimeSync()
        //仅在新晋播放页使用
        startScrollerSync(11)
        //仅在传统播放页使用
        //startSeekBarSync()

        //隐藏控件并设置背景为有色
        setBackgroundVisible()
        controllerLayer.visibility = View.VISIBLE

    }
    private fun setControllerVisible() {
        //状态标记变更
        playerViewModel.state_controllerShowing = true

        //启动被控控件控制
        startVideoTimeSync()
        //仅在新晋播放页使用
        startScrollerSync(10)
        //仅在传统播放页使用
        //startSeekBarSync()

        //隐藏控件并设置背景为有色
        setBackgroundVisible()
        controllerLayer.visibility = View.VISIBLE
        controllerLayer.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

    }
    private fun setBackgroundVisible(){
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
    }
    private fun setBackgroundInvisible(){
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
    }
    private fun changeBackgroundColor(){
        if (playerViewModel.state_controllerShowing){
            setControllerInvisible()
        }else{
            setControllerVisible()
        }
    }
    //进度条内边距设置
    private fun setScrollerPadding(){
        //计算边距
        sidePadding = display_screen_width_pixels / 2
        //根据横竖屏做不同设置
        if (isLandscape){
            //横屏
            var scrollerMarginType: Int
            //华为
            when (Build.BRAND) {
                "huawei", "HUAWEI", "HONOR", "honor" -> {
                    scrollerMarginType = 2
                    scroller.setPadding(
                        sidePadding + DeviceInfo.statusBarHeight / 2,
                        0,
                        sidePadding + DeviceInfo.statusBarHeight / 2 - 1,
                        0
                    )
                }
                //三星
                "samsung" -> {
                    scrollerMarginType = 1
                    scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                }
                //其他机型
                else -> {
                    scrollerMarginType = 1
                    scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                }
            }
            //使用兼容模式时,仅对原计算结果取反
            if (SettingsRequestCenter.get_PREFS_UseCompatScroller(this@PlayerActivityNeo)) {
                if (scrollerMarginType == 2) {
                    scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
                }
                else {
                    scroller.setPadding(sidePadding + DeviceInfo.statusBarHeight / 2, 0, sidePadding + DeviceInfo.statusBarHeight / 2 - 1, 0)
                }
            }
        }else{
            //竖屏
            scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        }
    }
    private var sidePadding = 0
    //状态栏配置
    @Suppress("DEPRECATION")
    private fun setStatusBarParams(){
        if (isLandscape){
            //横屏

            //设置控件层铺满屏幕顶部
            ViewCompat.setFitsSystemWindows(controllerLayer, true)
            controllerLayer.requestLayout()

            //其他设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                //动态调整布局内边距
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                    controllerLayer.updatePadding(top = systemBars.top)

                    WindowInsetsCompat.CONSUMED
                }
                //监听状态栏变化
                window.decorView.post { window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }else{
                //设置全屏显示相关行为
                if (isDarkTheme){
                    //恢复默认行为
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                                    //隐藏状态栏
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    //设置状态栏可短暂划出
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    //将内容显示到状态栏下方
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                            )

                }else{
                    //恢复默认行为
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                                    //隐藏状态栏
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    //设置状态栏可短暂划出
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    //将内容显示到状态栏下方
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

                            )
                }
            }
        }else{
            //竖屏

            //设置控件层不铺满屏幕顶部
            ViewCompat.setFitsSystemWindows(controllerLayer, true)
            controllerLayer.requestLayout()

            //其他设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                    controllerLayer.updatePadding(top = systemBars.top)

                    WindowInsetsCompat.CONSUMED
                }
                window.decorView.post { window.insetsController?.let { controller ->
                    controller.show(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
                }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }else{
                //设置全屏显示相关行为
                if (isDarkTheme){
                    //恢复默认行为
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                                    //将内容显示到状态栏下方(不隐藏状态栏)
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                                    //设置状态栏字体颜色
                                    //View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

                            )

                }else{
                    //恢复默认行为
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                                    //将内容显示到状态栏下方(不隐藏状态栏)
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

                            )
                }
            }


        }
    }
    //刷新按钮状态
    private fun updateLandscapeButton(){
        val ButtonLandscape = findViewById<CircleButton>(R.id.ButtonLandscape)
        //动态切换颜色和Icon的TintColor
        if (isLandscape) {
            ButtonLandscape.setMainColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.MainColorPack_CardButtonBackground_state_ON))
            if(isDarkTheme){
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.Black))
            }else{
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.Black))
            }
        }else{
            ButtonLandscape.setMainColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.MainColorPack_CardButtonBackground_state_OFF))
            if(isDarkTheme){
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.White))
            }else{
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.black))
            }
        }

    } //横屏按钮
    private fun updateButtonState(){
        val pauseButton = findViewById<CircleButton>(R.id.ButtonPause)
        if (player.isPlaying){
            pauseButton.setIconResource(R.drawable.ic_controller_neo_pause)
        }else{
            pauseButton.setIconResource(R.drawable.ic_controller_neo_play)
        }
    }   //暂停按钮
    //通知卡片位置设置
    private fun setNoticeCardPosition(){
        if (isLandscape){
            //横屏
            (noticeCapsule.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(5f))
        }else{
            //竖屏
            (noticeCapsule.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(100f))
        }
    }
    //调整控件位置
    private fun setControllerPosition(){
        //横屏
        if (isLandscape){
            //控件位置动态调整
            val displayManager = this.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = display?.rotation
            //正向横屏
            if (rotation == Surface.ROTATION_90) {
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (DeviceInfo.statusBarHeight)
                (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (DeviceInfo.statusBarHeight)
            }
            //反向横屏
            else if (rotation == Surface.ROTATION_270) {
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (DeviceInfo.statusBarHeight)
                (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (DeviceInfo.statusBarHeight)
            }
        }else{
            //竖屏时重置所有
            (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
            (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
            (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
            (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
        }
    }
    //更新屏幕参数
    private var display_screen_height_pixels: Int = 0
    private var display_screen_width_pixels: Int = 0
    private var display_screen_density: Float = 0f
    private fun updateScreenParameters(){
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight == 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootConstraint)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                DeviceInfo.statusBarHeight = systemBars.top

                insets
            }
        }else{
            //状态栏设置
            setStatusBarParams()
        }
        //获取屏幕宽高和密度
        val DisplayMetrics = resources.displayMetrics
        display_screen_width_pixels = DisplayMetrics.widthPixels
        display_screen_height_pixels = DisplayMetrics.heightPixels
        display_screen_density = DisplayMetrics.density

        //调整控件位置
        setControllerPosition()
        //通知卡片位置
        setNoticeCardPosition()
        //恢复隐藏控件状态
        if (!playerViewModel.state_controllerShowing){ setControllerInvisibleNoAnimation() }

        //刷新横屏按钮
        updateLandscapeButton()
        //设定进度条边界
        setScrollerPadding()


    }
    //关闭遮罩
    private fun closeCover(anim: Boolean = false, animDuration: Long = 250){
        val cover = findViewById<LinearLayout>(R.id.cover)
        if(anim){
            cover.animate().alpha(0f).setDuration(animDuration).withEndAction { cover.visibility = View.GONE }
        }else{
            cover.visibility = View.GONE
        }
    }
    private fun showCover(anim: Boolean = false, animDuration: Long = 250){
        val cover = findViewById<LinearLayout>(R.id.cover)
        if(anim){
            cover.animate().alpha(1f).setDuration(animDuration).withEndAction { cover.visibility = View.VISIBLE }
        }else{
            cover.visibility = View.VISIBLE
        }
    }

    //格式化时间戳显示
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
    //解除亮度控制
    private fun unlockBrightnessControl(){
        val windowInfo = window.attributes

        windowInfo.screenBrightness = -1f
        window.attributes = windowInfo

        playerViewModel.BrightnessChanged = false

        showCustomToast("已解除亮度控制,现在您可以使用系统亮度控制了", 3)
    }
    //dp转px
    private fun dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    @Suppress("unused")
    private fun px2dp(pxValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
    //展开动画
    @Suppress("unused")
    private fun viewFold(view: LinearLayout) {
        //设置初始高度为0
        view.measure(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = view.measuredHeight

        // 如果目标高度为0，则无需动画
        if (targetHeight <= 0) return
        // 如果当前高度已经是目标高度，则无需动画
        if (view.layoutParams.height == targetHeight) return

        // 初始高度设为0 (为了动画能从0开始)
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE


        val animator = ValueAnimator.ofInt(0, targetHeight)

        // 3. 设置动画更新监听器
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            view.layoutParams.height = animatedValue
            view.requestLayout()
        }
        animator.duration = 200

        animator.start()
    }
    @Suppress("unused")
    private fun viewExpand(view: LinearLayout) {
        //设置初始高度为0
        view.measure(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = view.measuredHeight

        // 如果目标高度为0，则无需动画
        if (targetHeight <= 0) return
        // 如果当前高度已经是目标高度，则无需动画
        if (view.layoutParams.height == targetHeight) return

        // 初始高度设为0 (为了动画能从0开始)
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE


        val animator = ValueAnimator.ofInt(0, targetHeight)

        // 3. 设置动画更新监听器
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            view.layoutParams.height = animatedValue
            view.requestLayout()
        }
        animator.duration = 200

        animator.start()
    }
    //请求使用高刷新率
    private fun requestHighRefreshRate(){
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val supportedModes = display.supportedModes
        //读取当前可以最高刷新率
        var maxRefreshRate = 0f
        for (mode in supportedModes) {
            val refreshRate = mode.refreshRate
            if (refreshRate > maxRefreshRate) {
                maxRefreshRate = refreshRate
            }
        }
        //consoleLog("requestHighRefreshRate 决策目标刷新率为：$maxRefreshRate")

        window.attributes.preferredRefreshRate = maxRefreshRate
    }
    //seekTo统一入口
    private fun seekTo_Core(num:Int ,pos: Long, mark: String){
        //consoleLog("seekTo_Core num-$num |||| $pos $mark")
        if (isSeekReady){
            isSeekReady = false

            //记录发起次数
            posted_seek_count++
            //consoleLog("posted_seek_count : $posted_seek_count")

            //暂停播放
            player.pause()

            //发起Seek
            Mark_playerReadyFrom = mark
            player.seekTo(pos)
        }
    } //num_max = 7
    private var isSeekReady = true
    private var posted_seek_count = 0
    private var processed_seek_count = 0
    //轻量暂停和轻量继续(循环专业)
    private fun pausePlay_Light(){

    }
    private fun continuePlay_Light(){

    }

    //Runnable-1:根据视频时间更新进度条位置
    private val task_syncScrollerPosition_Handler = Handler(Looper.getMainLooper())
    private val task_syncScrollerPosition_Runnable = object : Runnable {
        @SuppressLint("ServiceCast")
        override fun run() {
            //consoleLog("task_syncScrollerPosition_Runnable")

            //视频处于播放结束状态
            if (playerViewModel.playEnd && !player.isPlaying){
                //让进度条滚动到末尾
                scrollerParamMain -= 1
                scrollerParamOffset = 150
                //发起滚动
                scrollerLayoutManager.scrollToPositionWithOffset(scrollerParamMain, -scrollerParamOffset)

                //不再进入下一轮循环
                task_syncScrollerPosition_Running = false
            }else{
                syncScrollTask_Core()

                //进入下一次循环
                task_syncScrollerPosition_Handler.postDelayed(this, value_syncScroller_runnableGapMs)
            }
        }
    }
    private fun syncScrollTask_Core(){
        //计算位置参数
        scrollerParamMain = ( player.currentPosition / ScrollerHelper.singleFrame_durationMs ).toInt()
        scrollerParamOffset = (( player.currentPosition - scrollerParamMain * ScrollerHelper.singleFrame_durationMs ) * ScrollerHelper.singleFrame_WidthPx / ScrollerHelper.singleFrame_durationMs ).toInt()

        //设定进度条位置
        scrollerLayoutManager.scrollToPositionWithOffset(scrollerParamMain, -scrollerParamOffset)

    } //进度条位置刷新核心函数
    private fun startScrollerSync(num: Int) {
        //consoleLog("startScrollerSync $num")
        //未开启该项设置
        if (!playerViewModel.PREFS_LinkScroll) return
        //未在播放状态
        if (!player.isPlaying) return
        //进入锁
        if (task_syncScrollerPosition_Running) return
        task_syncScrollerPosition_Running = true
        //检查参数有效性
        if (ScrollerHelper.singleFrame_durationMs == 0L) return

        //scrollerLayoutManager = scroller.layoutManager as LinearLayoutManager
        //发起滚动任务
        task_syncScrollerPosition_Handler.post(task_syncScrollerPosition_Runnable)
    }   //num_max = 11
    private fun stopScrollerSync() {
        task_syncScrollerPosition_Running = false
        task_syncScrollerPosition_Handler.removeCallbacks(task_syncScrollerPosition_Runnable)
    }
    private var value_syncScroller_runnableGapMs = 0L //进度条位置刷新间隔
    private var task_syncScrollerPosition_Running = false
    private var scrollerParamMain = 0      //进度条大分段位置参数
    private var scrollerParamOffset = 0    //进度条微调偏移量
    //Runnable-2:根据视频时间更新时间窗口进度数值
    private val task_timeStampSync_Handler = Handler(Looper.getMainLooper())
    private var task_timeStampSync_Runnable = object : Runnable{
        override fun run() {
            videoTimeSyncHandler_currentPosition = player.currentPosition

            controller_timer_current.text = FormatTime_onlyNum(videoTimeSyncHandler_currentPosition)

            task_timeStampSync_Handler.postDelayed(this, 1000)
        }
    }
    private fun startVideoTimeSync() {
        task_timeStampSync_Handler.post(task_timeStampSync_Runnable)
    }
    private fun stopVideoTimeSync() {
        task_timeStampSync_Handler.removeCallbacks(task_timeStampSync_Runnable)
    }
    private var value_timeStamp_updateGapMs = 0L //时间戳刷新间隔
    //Runnable-3:视频滚动寻帧-倍速假寻帧方案
    private val videoSmartScrollHandler = Handler(Looper.getMainLooper())
    private var videoSmartScroll = object : Runnable{
        override fun run() {
            /*
            playerViewModel.allowRecord_wasPlaying = false
            var delayGap = if (scrollerDesire_Active){ 30L } else{ 30L }
            val videoPosition = player.currentPosition
            val scrollerPosition = player.duration * (scroller.computeHorizontalScrollOffset().toFloat()/scroller.computeHorizontalScrollRange())
            player.volume = 0f
            if (scrollerState_Moving) {
                if (player.currentPosition > scrollerPosition - 100) {
                    player.pause()
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


                    if (speed5 > 0f){ player.setPlaybackSpeed(speed5) }
                }
                videoSmartScrollHandler.postDelayed(this,delayGap)
            }
            else{
                player.volume = 1f
                if (lastSeekExecuted) return
                lastSeekExecuted = true

                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                smartScrollRunnableRunning = false
                playerReadyFrom_SmartScrollLastSeek = true
                startSmartScrollLastSeek()
            }

             */
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
        if (smartScrollRunnableRunning) return
        smartScrollRunnableRunning = true
        videoSmartScrollHandler.post(videoSmartScroll)
    }
    private fun stopVideoSmartScroll() {
        smartScrollRunnableRunning = false
        videoSmartScrollHandler.removeCallbacks(videoSmartScroll)
    }
    private var smartScrollRunnableRunning = false
    //Runnable-4:视频滚动寻帧-标准真寻帧方案
    private val task_standardSeekLoop_Handler = Handler(Looper.getMainLooper())
    private var task_standardSeekLoop_Runnable = object : Runnable{
        override fun run() {
            //consoleLog("task_standardSeekLoop_Runnable")

            standardSeekLoop_Core()

            //循环脱离决策
            if (scrollerDesire_Active){

                //进度条还在Active状态-继续循环
                task_standardSeekLoop_Handler.postDelayed(this, value_seekVideo_runnableGapMs)

            }else{
                //脱离循环
                task_standardSeekLoop_Running = false
            }

        }
    }
    private fun standardSeekLoop_Core(forceOnce: Boolean = false){
        if (forceOnce){
            //根据 进度条比例位置 计算 目标视频位置
            val totalScrollerLength = scroller.computeHorizontalScrollRange()
            val scrollerPos_Offset = scroller.computeHorizontalScrollOffset()
            val scrollerPos_Percent = scrollerPos_Offset.toFloat() / totalScrollerLength
            val targetSeekToMs = (scrollerPos_Percent * player.duration).toLong()

            if (isSeekReady){
                //设置精确帧
                player.setSeekParameters(SeekParameters.EXACT)

                //发起寻帧
                seekTo_Core(7,targetSeekToMs, Mark_playerReadyFrom_ChaseSeek)
            }

        }else{
            //仅在进度条Active状态下执行寻帧
            if (scrollerDesire_Active){
                //根据 进度条比例位置 计算 目标视频位置
                val totalScrollerLength = scroller.computeHorizontalScrollRange()
                val scrollerPos_Offset = scroller.computeHorizontalScrollOffset()
                val scrollerPos_Percent = scrollerPos_Offset.toFloat() / totalScrollerLength
                val targetSeekToMs = (scrollerPos_Percent * player.duration).toLong()

                //仅在空闲时发起下一次寻帧
                if (isSeekReady){
                    //不同滚动方向操作不同
                    when(scrollerMotionState_Forward){
                        //正向滚动
                        true -> {
                            if (targetSeekToMs < player.currentPosition){
                                //目标位置接近起始,直接置0快速回起始
                                if (targetSeekToMs < 50){
                                    if (scrollerDesire_Active) seekTo_Core(1,0, Mark_playerReadyFrom_NormalSeek)
                                }else{
                                    if (scrollerDesire_Active) seekTo_Core(2, targetSeekToMs,Mark_playerReadyFrom_NormalSeek)
                                }
                            }else{
                                if (scrollerDesire_Active) seekTo_Core(3,targetSeekToMs,Mark_playerReadyFrom_NormalSeek)
                            }
                        }
                        //反向
                        false -> {
                            if (targetSeekToMs < player.currentPosition){
                                //目标位置接近起始,直接置0快速回起始
                                if (targetSeekToMs < 50){
                                    if (scrollerDesire_Active) seekTo_Core(4, 0,Mark_playerReadyFrom_NormalSeek)
                                }else{
                                    if (scrollerDesire_Active) seekTo_Core(5, targetSeekToMs,Mark_playerReadyFrom_NormalSeek)
                                }
                            }else{
                                if (scrollerDesire_Active) seekTo_Core(6,targetSeekToMs,Mark_playerReadyFrom_NormalSeek)
                            }
                        }
                    }
                }
            }
        }

    }
    private fun startVideoSeek(num: Int) {
        //consoleLog("startVideoSeek:$num")

        if (task_standardSeekLoop_Running) return
        task_standardSeekLoop_Running = true

        //开启循环
        task_standardSeekLoop_Handler.post(task_standardSeekLoop_Runnable)
    }  //num_max = 2
    private fun stopVideoSeek() {
        task_standardSeekLoop_Running = false
        task_standardSeekLoop_Handler.removeCallbacks(task_standardSeekLoop_Runnable)
    }
    private var value_seekVideo_runnableGapMs = 0L
    private var task_standardSeekLoop_Running = false
    //显示通知
    private var showNoticeJob: Job? = null
    private var showNoticeJobLong: Job? = null
    private fun showNoticeJob(text: String, duration: Long) {
        showNoticeJob?.cancel()
        showNoticeJob = lifecycleScope.launch {
            val NoticeCardText = findViewById<TextView>(R.id.NoticeCardText)
            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
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
            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
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
    //长按横屏按钮
    private var SwitchLandscapeJob: Job? = null
    private fun SwitchLandscapeJob() {
        SwitchLandscapeJob?.cancel()
        SwitchLandscapeJob = lifecycleScope.launch {
            delay(500)
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            ButtonChangeOrientation("long")
        }
    }

    //已废弃代码
    //退出动作决策程序(已废弃)
    /*
    private var state_onDestroy_reach = false
    private var state_onSaveInstanceState_reach = false
    private var onStopDecideCount = 0L
    private val onStopDecideHandler = Handler(Looper.getMainLooper())
    private val onStopDecideTask = object : Runnable {
        override fun run() {
            //修改计数位以在必要时退出循环
            onStopDecideCount++
            //等待100毫秒后检查状态变量
            if (onStopDecideCount > 100){
                //未触发onDestroy,活动暂退桌面
                if (!state_onDestroy_reach){
                    playerViewModel.set_onStop_ByLossFocus()
                    if(!state_FromFloatingWindow){
                        PlayerSingleton.ActivityOnStop(this@PlayerActivityNeo)
                    }
                }
                //活动被销毁
                else{
                    //活动销毁但保存了数据：活动因深色模式切换或尺寸切换发生重建
                    if (state_onSaveInstanceState_reach){
                        playerViewModel.set_onStop_ByReBuild()
                    }
                    //活动销毁且未保存数据：确实退出了活动
                    else{
                        playerViewModel.set_onStop_ByRealExit()
                    }
                }
                //决策函数运行结束
                playerViewModel.state_onStopDecider_Running = false
            }
            //循环100毫秒后检测
            else{
                onStopDecideHandler.postDelayed(this, 1)
            }
        }
    }
    private fun startOnStopDecider() {
        //因保持播放状态退出时：不报告状态
        if (state_FromExitKeepPlaying) return
        //重置计数位并启动检测程序
        onStopDecideCount = 0L
        playerViewModel.state_onStopDecider_Running = true
        onStopDecideHandler.post(onStopDecideTask)
    }
    private fun stopOnStopDecider() {
        onStopDecideHandler.removeCallbacks(onStopDecideTask)
        playerViewModel.state_onStopDecider_Running = false
    }

     */
    //退出延时器(已废弃)
    /*
    private var ExitJob: Job? = null
    private fun ExitJob() {
        ExitJob?.cancel()
        ExitJob = lifecycleScope.launch {
            delay(500)
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            exitActivity()
        }
    }

     */
    //RxJava事件总线(已废弃)
    /*
    private var state_EventBus_Registered = false
    private fun registerEventBus(){
        if (state_EventBus_Registered) return
        setupEventBus()
        state_EventBus_Registered = true
    }
    private fun unregisterEventBus(){
        disposable?.dispose()
        state_EventBus_Registered = false
    }
    private var disposable: Disposable? = null
    private fun setupEventBus() {
        disposable = ToolEventBus.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                HandlePlayerEvent(it)
            }, {
                showCustomToast("事件总线注册失败:${it.message}", 3)
            })
    }
    private fun HandlePlayerEvent(event: String) {
        when (event) {
            //播控中心按钮操作
            "SessionController_Next" -> {
                //来自 ToolEventBus.sendEvent("SessionController_Next")
            }
            "SessionController_Previous" -> {
                //来自 ToolEventBus.sendEvent("SessionController_Previous")
            }
            "SessionController_Play" -> {
                //来自 ToolEventBus.sendEvent("SessionController_Play")

            }
            "SessionController_Pause" -> {
                //来自 ToolEventBus.sendEvent("SessionController_Pause")

            }

        }
    }

     */

}

