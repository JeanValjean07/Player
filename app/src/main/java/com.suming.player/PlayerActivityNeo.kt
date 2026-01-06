package com.suming.player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.media.AudioDeviceInfo
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
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
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
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.suming.player.ListManager.FragmentPlayList
import com.suming.player.ListManager.PlayerListManager
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaItem.MediaItemSetting
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

@UnstableApi
@RequiresApi(Build.VERSION_CODES.Q)
@Suppress("unused")
class PlayerActivityNeo: AppCompatActivity(){
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

    //缩略图绘制参数
    private var ScrollerInfo_MaxPicNumber = 20
    private var ScrollerInfo_EachPicWidth = 0
    private var ScrollerInfo_PicNumber = 0
    private var ScrollerInfo_EachPicDuration: Int = 0
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
    private var touchCenter = false
    private var scrollDistance = 0
    //方向回调
    private var orientationChangeTime = 0L
    private var LastOrientationChangeTime = 0L
    //音量恢复
    private var volumeRecExecuted = false
    //耳机链接状态
    private var headSet = false


    //视频尺寸
    private var videoSizeWidth = 0
    private var videoSizeHeight = 0

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    //空闲定时器
    private var IDLE_Timer: CountDownTimer? = null
    private val IDLE_MS = 5_000L
    private var state_EnterAnimationCompleted = false
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


    //lateInitItem -复杂工具
    private lateinit var receiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var equalizer: Equalizer

    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFSEditor: SharedPreferences.Editor
    private lateinit var PREFS_List: SharedPreferences

    private lateinit var retriever: MediaMetadataRetriever

    private lateinit var scrollerLayoutManager: LinearLayoutManager


    private var onScroll_currentMillis = 0L
    private var onScroll_scrollPercent = 0f
    private var onScroll_seekToMs = 0L

    private var VideoSeekHandler_totalWidth = 0
    private var VideoSeekHandler_offset = 0
    private var VideoSeekHandler_percent = 0f
    private var VideoSeekHandler_seekToMs = 0L

    private var videoTimeSyncHandler_currentPosition = 0L

    private var DataBaseProfile: MediaItemSetting? = null

    //</editor-fold>
    //测试中变量
    //<editor-fold desc="测试中变量">


    private var onDestroy_fromErrorExit = false



    private var clickMillis_MoreOptionPage = 0L

    private var state_need_return = false

    private var switchLandscape_downMillis = 0L
    private var switchLandscape_upMillis = 0L

    private var ExitJob_downMillis = 0L
    private var ExitJob_upMillis = 0L

    private lateinit var PREFS_MediaStore: SharedPreferences

    private var state_switch_item = false


    private var touchState_need_exit = false
    private var touchState_left_noticed = false
    private var touchState_right_noticed = false
    private var touchState_need_exit_vibrated = false
    private var touchState_scroll_vibrated = false

    private var state_HeadSetInserted = false

    private var state_need_start_new_item = false

    private var state_RootCardClosing = false

    private var touchCenterDistance = 0f

    private var state_onPlayError = false


    //</editor-fold>

    //获取播放器实例
    private val player get() = PlayerSingleton.getPlayer(application)
    //ViewModel
    private val vm: PlayerViewModel by viewModels()

    @OptIn(UnstableApi::class)
    @SuppressLint("CutPasteId", "SetTextI18n", "InflateParams", "ClickableViewAccessibility", "RestrictedApi", "SourceLockedOrientationActivity", "UseKtx","DEPRECATION", "CommitPrefEdits")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_type_neo)
        //启动播放器单例
        PlayerSingleton.startPlayerSingleton(application)
        //其他预设
        preCheck()


        //提取uri并保存
        if (savedInstanceState == null){
            //区分打开方式
            when (intent?.action) {
                //系统面板：分享
                Intent.ACTION_SEND -> {
                    vm.state_FromSysStart = true
                    val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return finish()
                    MediaInfo_MediaUri = uri
                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {
                    vm.state_FromSysStart = true
                    val uri = intent.data
                    if (uri == null){
                        showCustomToast("链接无效，无法播放。可在附加Data中填入媒体uri", Toast.LENGTH_SHORT, 3)
                        finishAndRemoveTask()
                    }else{
                        MediaInfo_MediaUri = uri
                    }
                }
                //正常打开
                else -> {
                    //写入来源配置
                    vm.PREFS_ExitWhenEnd = false
                    //初始化媒体链接原始值
                    var originalUriString : String
                    var originalUri : Uri
                    //尝试获取原始intent的uri
                    originalUri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)?: Uri.EMPTY
                    if (originalUri == Uri.EMPTY){
                        //判断是否来自pendingIntent
                        val FromPendingIntent = intent.getStringExtra("IntentSource") ?: "error"
                        //来自pendingIntent:不设置新媒体项,从播放器单例获取uri
                        if (FromPendingIntent == "FromPendingIntent"){
                            MediaInfo_MediaUri = PlayerSingleton.getMediaInfoUri()
                        }
                        //来自其他:算作普通启动,需要读取来自pendingIntent的uri
                        else{
                            //尝试获取pendingIntent的uri
                            originalUriString = intent?.getStringExtra("MediaInfo_MediaUri") ?: "error"
                            //pendingIntent的uri不存在:尝试读取落盘保存的uri
                            if (originalUriString == "error"){
                                val INFO_PlayerSingleton = getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
                                val saveInfoUri: Uri
                                if (INFO_PlayerSingleton.contains("MediaInfo_MediaUri")){
                                    val saveInfoUriString = INFO_PlayerSingleton.getString("MediaInfo_MediaUri", "error") ?: "error"
                                    saveInfoUri = Uri.parse(saveInfoUriString)
                                    MediaInfo_MediaUri = saveInfoUri
                                }
                                else{
                                    showCustomToast("视频链接无效", Toast.LENGTH_SHORT, 3)
                                    showCustomToast("播放失败", Toast.LENGTH_SHORT, 3)
                                    onDestroy_fromErrorExit = true
                                    state_need_return = true
                                    finish()
                                    return
                                }
                            }
                            //pendingIntent的uri存在:使用这个uri
                            else{
                                originalUri = Uri.parse(originalUriString)
                                MediaInfo_MediaUri = originalUri
                            }
                        }
                    }
                    //原始Intent的uri存在:使用这个uri
                    else{
                        MediaInfo_MediaUri = originalUri
                    }
                }
            }
        }else{
            MediaInfo_MediaUri = vm.MediaInfo_MediaUri
        }
        //根据uri提取基础视频信息
        getMediaInfo(MediaInfo_MediaUri)
        //检查是否需要返回
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
            if (PREFS.contains("PREFS_KeepPlayingWhenExit")) {
                vm.PREFS_KeepPlayingWhenExit = PREFS.getBoolean("PREFS_KeepPlayingWhenExit", true)
            } else {
                PREFSEditor.putBoolean("PREFS_KeepPlayingWhenExit", true)
                vm.PREFS_KeepPlayingWhenExit = true
            }
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
                PREFSEditor.putBoolean("PREFS_SwitchPortraitWhenExit", false)
                vm.PREFS_SwitchPortraitWhenExit = false
            } else {
                vm.PREFS_SwitchPortraitWhenExit = PREFS.getBoolean("PREFS_SwitchPortraitWhenExit", false)
            }
            if (!PREFS.contains("PREFS_CloseFragmentGesture")) {
                PREFSEditor.putBoolean("PREFS_CloseFragmentGesture", false)
                vm.PREFS_CloseFragmentGesture = false
            } else {
                vm.PREFS_CloseFragmentGesture = PREFS.getBoolean("PREFS_CloseFragmentGesture", false)
            }
            if (!PREFS.contains("PREFS_EnablePlayAreaMoveAnim")){
                PREFSEditor.putBoolean("PREFS_EnablePlayAreaMoveAnim", true)
                vm.PREFS_EnablePlayAreaMoveAnim = true
            }else{
                vm.PREFS_EnablePlayAreaMoveAnim = PREFS.getBoolean("PREFS_EnablePlayAreaMoveAnim", true)
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
            state_EnterAnimationCompleted = true
        }
        //读取数据库
        if (savedInstanceState == null){ ReadRoomDataBase() }
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
        if (vm.PREFS_EnablePlayAreaMoveAnim){
            MoveYaxisCalculate()
        }                      //计算移动高度

        //RxJava事件总线
        registerEventBus()


        //传入视频链接
        if(savedInstanceState == null){
            startPlayNewItem(MediaInfo_MediaUri)
        }else{
            //重建时连接已有媒体
            onBindExistingItem()
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
                val seekToMs = (percent * player.duration).toLong().coerceIn(0, player.duration)
                if (seekToMs <= 0) {
                    return false
                }
                if (seekToMs >= player.duration) {
                    return false
                }
                //发送跳转命令
                player.seekTo(seekToMs)
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
                    player.play()
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
                    if (player.isPlaying && vm.allowRecord_wasPlaying) {
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
                //进度条随视频滚动,用户没有操作
                if (scrollerState_Stay) {
                    return
                }
                //用户操作 -停止同步
                stopVideoTimeSync()
                stopScrollerSync()
                if (playerReadyFrom_FirstEntry) return
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
                            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        else {
                            player.setSeekParameters(SeekParameters.EXACT)
                        }
                    } else {
                        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                } else {
                    player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                }
                //用户操作 -时间戳跟随进度条变动
                onScroll_currentMillis = System.currentTimeMillis()
                if (onScroll_currentMillis - lastMillis > vm.PREFS_TimeUpdateGap) {
                    lastMillis = onScroll_currentMillis
                    if (vm.PREFS_LinkScroll) {

                        //计算对应时间戳
                        onScroll_scrollPercent = recyclerView.computeHorizontalScrollOffset().toFloat() / scroller.computeHorizontalScrollRange()
                        onScroll_seekToMs = (onScroll_scrollPercent * vm.MediaInfo_MediaDuration).toLong()

                        //刷新时间显示
                        controller_timer_current.text = FormatTime_onlyNum(onScroll_seekToMs)

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
                    player.pause()
                    stopVideoSmartScroll()
                    startVideoSeek()
                }
            }
        })

        //退出按钮
        val ButtonExit = findViewById<ImageButton>(R.id.TopBarArea_ButtonExit)
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
                        EnsureExit(true)
                    }
                    return@setOnTouchListener true
                }
            }
            onTouchEvent(event)
        }
        //更多选项
        val TopBarArea_ButtonMoreOptions = findViewById<ImageButton>(R.id.TopBarArea_ButtonMoreOptions)
        TopBarArea_ButtonMoreOptions.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            //防止快速点击
            if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                return@setOnClickListener
            }
            clickMillis_MoreOptionPage = System.currentTimeMillis()
            //关闭时间和进度条同步 + 移动播放区域
            stopVideoTimeSync()
            stopScrollerSync()
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { MovePlayAreaJob() }
            //启动弹窗
            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.NoticeCard)
        noticeCard.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            noticeCard.visibility = View.GONE
        }
        //按钮：暂停/继续播放
        val ButtonPause = findViewById<ImageButton>(R.id.controller_button_playorpause)
        ButtonPause.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            if (player.isPlaying) {
                scroller.stopScroll()
                recessPlay(need_fadeOut = false)
                stopScrollerSync()
                notice("暂停", 1000)
                updateButtonState()
            }else{
                scroller.stopScroll()
                if (vm.playEnd) {
                    vm.playEnd = false
                    continuePlay(true, force_request = true, need_fadeIn = false)
                    notice("开始重播", 2000)
                    updateButtonState()
                } else {
                    continuePlay(true, force_request = true, need_fadeIn = false)
                    notice("继续播放", 2000)
                }
            }
        }
        //按钮：切换横屏
        val ButtonLandscape = findViewById<ImageButton>(R.id.controller_button_landscape)
        ButtonLandscape.setOnTouchListener { _, event ->
            when (event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    ToolVibrate().vibrate(this)
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
        val ButtonMore = findViewById<ImageButton>(R.id.controller_button_more)
        ButtonMore.setOnClickListener {
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            //防止快速点击
            if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                return@setOnClickListener
            }
            clickMillis_MoreOptionPage = System.currentTimeMillis()
            //关闭时间和进度条同步 + 移动播放区域
            stopVideoTimeSync()
            stopScrollerSync()
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { MovePlayAreaJob() }
            //启动弹窗
            PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, "PlayerMoreButtonFragment")
        }
        //播放区域点击事件
        val gestureDetectorPlayArea = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (player.isPlaying) {
                    recessPlay(need_fadeOut = false)
                    stopScrollerSync()
                    notice("暂停播放", 1000)
                    updateButtonState()
                } else {
                    if (vm.playEnd) {
                        vm.playEnd = false
                        continuePlay(true, force_request = true, need_fadeIn = false)
                        notice("开始重播", 1000)
                    } else {
                        continuePlay(true, force_request = true, need_fadeIn = false)
                        notice("继续播放", 1000)
                        updateButtonState()
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
                if (!player.isPlaying) {
                    return
                }
                currentSpeed = player.playbackParameters.speed
                player.setPlaybackSpeed(currentSpeed * 2.0f)
                notice("倍速播放中(${currentSpeed * 2.0f}x)", 114514)
                longPress = true
                ToolVibrate().vibrate(this@PlayerActivityNeo)
                super.onLongPress(e)
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (touchLeft) {
                    //点击区域功能提示:仅一次
                    if (!touchState_left_noticed){
                        //notice("继续上下滑动可调整亮度", 1000)
                        touchState_left_noticed = true
                    }
                    //累积滑动距离
                    scrollDistance += distanceY.toInt()
                    val windowInfo = window.attributes
                    //开始亮度修改
                    vm.BrightnessChanged = true
                    var newBrightness: Float
                    //上滑
                    if (scrollDistance > 50) {
                        newBrightness = (vm.BrightnessValue + 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness in 0.0..1.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 +1 (${(newBrightness*100).toInt()}/100)", 1000)
                        }
                        else{
                            notice("亮度已到上限", 1000)
                            if (!touchState_scroll_vibrated) {
                                touchState_scroll_vibrated = true
                                ToolVibrate().vibrate(this@PlayerActivityNeo)
                            }
                        }
                    }
                    //下滑
                    else if (scrollDistance < -50){
                        newBrightness = (vm.BrightnessValue - 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        if (newBrightness in 0.0..1.0) {
                            windowInfo.screenBrightness = newBrightness
                            window.attributes = windowInfo
                            vm.BrightnessValue = newBrightness
                            notice("亮度 -1 (${(newBrightness*100).toInt()}/100)", 1000)
                        }
                        else{
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
                    if (!touchState_right_noticed){
                        //notice("继续上下滑动可调整音量", 1000)
                        touchState_right_noticed = true
                    }
                    //累积滑动距离
                    scrollDistance += distanceY.toInt()
                    //快速下滑紧急静音
                    if (scrollDistance < -150){
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        notice("快速静音", 2000)
                    }
                    //普通音量修改
                    if (scrollDistance > volumeChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume += 1
                        if (currentVolume <= maxVolume){
                            if (state_HeadSetInserted){
                                if (currentVolume <= (maxVolume*0.6).toInt()){
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                    notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                }
                                else{
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@PlayerActivityNeo)
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
                            if (!touchState_scroll_vibrated) {
                                touchState_scroll_vibrated = true
                                ToolVibrate().vibrate(this@PlayerActivityNeo)
                            }
                            notice("音量已到最高", 1000)
                        }
                    }
                    else if (scrollDistance< -volumeChangeGap){
                        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        currentVolume -= 1
                        if (currentVolume >= 0){
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                        }
                        else{
                            if (!touchState_scroll_vibrated) {
                                touchState_scroll_vibrated = true
                                ToolVibrate().vibrate(this@PlayerActivityNeo)
                            }
                            notice("音量已到最低", 1000)
                        }
                    }
                    //数值越界置位
                    if (scrollDistance>50 || scrollDistance< -50) {
                        scrollDistance = 0
                    }
                }
                if (touchCenter){
                    state_RootCardClosing = true
                    touchCenterDistance += distanceY
                    if (touchCenterDistance < -300){
                        touchState_need_exit = true
                        //振动:仅一次
                        if (!touchState_need_exit_vibrated){
                            touchState_need_exit_vibrated = true
                            ToolVibrate().vibrate(this@PlayerActivityNeo)
                        }
                    }else{
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
                    if (finger1y < DisplayMetric_ScreenHeight * 0.2 || finger1y > DisplayMetric_ScreenHeight * 0.95){
                        return@setOnTouchListener false
                    }
                    //分割横向功能区:初步信息获取
                    if (finger1x < DisplayMetric_ScreenWidth * 0.2) {
                        touchLeft = true
                    }
                    else if(finger1x > DisplayMetric_ScreenWidth * 0.8){
                        state_HeadSetInserted = PlayerSingleton.getState_isHeadsetPlugged(this@PlayerActivityNeo)
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
                        val NoticeCard = findViewById<CardView>(R.id.NoticeCard)
                        NoticeCard.visibility = View.GONE
                    }
                    //事件处理:下滑退出但保持播放
                    if (touchState_need_exit){
                        EnsureExit_but_keep_playing()
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
                    player.seekTo(0)
                    player.play()
                    startScrollerSync()
                    notice("回到视频起始", 3000)
                }
                "PlayList" -> {
                    FragmentPlayList.newInstance().show(supportFragmentManager, "PlayerListFragment")
                }
                "ExtractFrame" -> {
                    val videoPath = getFilePath(this, MediaInfo_MediaUri)
                    if (videoPath == null){
                        showCustomToast("视频绝对路径获取失败", Toast.LENGTH_SHORT, 3)
                        return@setFragmentResultListener
                    }
                    ExtractFrame(videoPath, vm.MediaInfo_FileName)
                }
                //旋转监听
                "SealOEL" -> {
                    if (vm.PREFS_SealOEL){
                        PREFS.edit { putBoolean("PREFS_SealOEL", true).apply() }
                        stopOrientationListener()
                        notice("已关闭方向监听器", 1000)
                    } else {
                        PREFS.edit { putBoolean("PREFS_SealOEL", false).apply() }
                        startOrientationListener()
                        notice("已开启方向监听器", 1000)
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
                //开启小窗
                "StartPiP" -> {
                    startFloatingWindow()
                }
                //底部按钮
                "VideoInfo" -> {
                    //读取数据
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(vm.MediaInfo_AbsolutePath)
                    val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val videoFps = fps
                    val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    val videoMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    val videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

                    val videoFileName = vm.MediaInfo_FileName
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
                        shareVideo(this@PlayerActivityNeo, MediaInfo_MediaUri)
                    }
                }
                "updateCoverFrame" -> {
                    val Method = bundle.getString("Method")
                    when(Method){
                        "useCurrentFrame" -> {
                            CaptureCurrentFrameAsCover(vm.MediaInfo_FileName)
                        }
                        "useDefaultCover" -> {
                            useDefaultCover(vm.MediaInfo_FileName)
                        }
                        "pickFromLocal" -> {
                            showCustomToast("暂不支持此功能", Toast.LENGTH_SHORT, 3)
                        }
                    }
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
                //错误处理
                "BindPlayView" -> {
                    playerView.player = null
                    playerView.player = player
                }
                //退出事件
                "Dismiss" -> {
                    //开启被控组件
                    startScrollerSync()
                    startVideoTimeSync()
                    //把播放区域移回去
                    MovePlayArea_down()
                }
            }
        }
        //播放列表返回值 FROM_FRAGMENT_PLAY_LIST
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_PLAY_LIST", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                //切换逻辑由播放器单例接管
                //停止播放并退出
                "stopPlaying" -> {
                    finish()
                }
                //退出逻辑
                "Dismiss" -> {
                    //开启被控组件
                    startScrollerSync()
                    startVideoTimeSync()
                    //把播放区域移回去
                    MovePlayArea_down()
                }
            }
        }
        //媒体信息列表返回值 FROM_FRAGMENT_VIDEO_INFO
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_VIDEO_INFO", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                "Dismiss" -> {
                    //开启被控组件
                    startScrollerSync()
                    startVideoTimeSync()
                    //把播放区域移回去
                    MovePlayArea_down()
                }
            }
        }

        //表明页面状态 需要区分页面类型 flag_page_type
        vm.state_player_type = "Neo"
        //监听系统手势返回
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ExitByCheckOrientation()
            }
        })
    //onCreate END
    }



    //Testing Functions
    //播放器监听器
    private val PlayerStateListener = object : Player.Listener {
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
            updateButtonState()
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
            onMediaItemChanged(mediaItem)
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            state_onPlayError = true
            showCustomToast("播放错误: ${error.message}", Toast.LENGTH_SHORT, 3)
        }
    }
    private var state_PlayerListenerAdded: Boolean = false
    //启动ExoPlayer
    private fun startExoPlayer(){
        //确保单例端播放器已启动
        PlayerSingleton.startSingletonExoPlayer(application)
        //添加播放器事件监听
        startExoPlayerListener()

    }
    private fun startExoPlayerListener(){
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        state_PlayerListenerAdded = true
    } //添加播放器事件监听
    //设置新媒体项
    private fun setNewMediaItem(uri: Uri){
        //解码信息到本地
        getMediaInfo(uri)
        //启动播放器
        startExoPlayer()
        //绑定播放器视图
        playerView.player = null
        playerView.player = player
        //确认设置新媒体项
        PlayerSingleton.setMediaItem(MediaInfo_MediaUri, true)
    }
    //开启播放新媒体项
    private fun startPlayNewItem(uri: Uri){
        //检查是否需要设置新的媒体项
        val currentMediaItem = PlayerSingleton.getCurrentMediaItem()
        if (currentMediaItem == null){
            //设置新媒体项
            setNewMediaItem(uri)
        }else{
            //检查已有媒体信息
            val currentUri = PlayerSingleton.getMediaInfoUri()
            //已有媒体不是目标媒体,需要设置新媒体
            if (currentUri != MediaInfo_MediaUri){
                //设置新媒体项
                setNewMediaItem(uri)
            }
            //已有媒体正是目标媒体,直接绑定
            else{
                //播放已存在媒体
                onPlayExistingItem()
            }
        }
    }
    //播放已有媒体
    private fun onPlayExistingItem(){
        //绑定播放器视图
        playerView.player = null
        playerView.player = player
        //添加播放器事件监听
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        state_PlayerListenerAdded = true
        //关闭遮罩
        closeCover(0,0)
        //重置状态
        playerReadyFrom_FirstEntry = false
        //更新全局媒体信息变量
        getMediaInfo(MediaInfo_MediaUri)
        //读取数据库
        ReadRoomDataBase()
        //刷新视频进度线
        updateTimeCard()
        //刷新进度条
        updateScrollerAdapter()
        //刷新控制按钮
        updateButtonState()

    }
    //重建时连接已有媒体
    private fun onBindExistingItem(){
        //状态置位
        playerReadyFrom_FirstEntry = true
        state_EnterAnimationCompleted = true
        //绑定播放器视图
        playerView.player = null
        playerView.player = player
        //添加播放器事件监听
        player.removeListener(PlayerStateListener)
        player.addListener(PlayerStateListener)
        state_PlayerListenerAdded = true
        //关闭遮罩
        closeCover(0,0)
        //重置状态
        playerReadyFrom_FirstEntry = false
        //更新全局媒体信息变量
        getMediaInfo(MediaInfo_MediaUri)
        //读取数据库
        ReadRoomDataBase()
        //刷新视频进度线
        updateTimeCard()
        //刷新进度条
        updateScrollerAdapter()
        //刷新控制按钮
        updateButtonState()
        //隐藏顶部分割线
        HideTopLine()
        //重新获取媒体信息
        MediaInfo_MediaUri = vm.MediaInfo_MediaUri
        getMediaInfo(MediaInfo_MediaUri)

    }
    //媒体项变更回调
    private fun onMediaItemChanged(mediaItem: MediaItem?){
        if (mediaItem == null){ return }
        //检查媒体类型
        if (PlayerSingleton.getMediaInfoType() == "music"){
            EnsureExit_but_keep_playing()
            return
        }

        //重新绑定播放器视图
        playerView.player = null
        playerView.player = player
        //更新本地MediaInfo_MediaUri
        MediaInfo_MediaUri = mediaItem.mediaId.toUri()
        //更新全局媒体信息变量
        getMediaInfo(MediaInfo_MediaUri)
        //重新读取数据库+覆盖关键值
        ReadRoomDataBase()
        //刷新：视频总长度
        updateTimeCard()
        //刷新：进度条更新
        updateScrollerAdapter()
        //刷新按钮
        updateButtonState()

    }
    //RxJava事件总线:界面端减少参与播放器单例的控制
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
    private var disposable: io.reactivex.rxjava3.disposables.Disposable? = null
    private fun setupEventBus() {
        disposable = ToolEventBus.events
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({
                HandlePlayerEvent(it)
            }, {
                showCustomToast("事件总线注册失败:${it.message}", Toast.LENGTH_SHORT,3)
            })
    }
    private fun HandlePlayerEvent(event: String) {
        when (event) {
            //播控中心按钮操作
            "SessionController_Next" -> {  }
            "SessionController_Previous" -> {  }
            "SessionController_Play" -> {
                startScrollerSync()
                startVideoTimeSync()
            }
            "SessionController_Pause" -> {
                stopScrollerSync()
                stopVideoTimeSync()
            }
            //媒体类型变更
            "PlayerSingleton_MediaTypeChanged_toMusic" -> {
                //EnsureExit_but_keep_playing()
            }

        }
    }
    //数据库读取+基于数据库的操作
    private fun ReadRoomDataBase(){
        lifecycleScope.launch(Dispatchers.IO) {
            DataBaseProfile = MediaItemRepo.get(applicationContext).getSetting(vm.MediaInfo_FileName)
            if (DataBaseProfile == null){ DataBasePreWrite() ; return@launch }
            //数据库后续操作
            if (vm.PREFS_UseDataBaseForScrollerSetting){
                lifecycleScope.launch(Dispatchers.IO) {
                    if (DataBaseProfile == null){ return@launch }
                    val DataBaseSetting = DataBaseProfile!!
                    vm.PREFS_AlwaysSeek = DataBaseSetting.PREFS_AlwaysSeek
                    vm.PREFS_LinkScroll = DataBaseSetting.PREFS_LinkScroll
                    vm.PREFS_TapJump = DataBaseSetting.PREFS_TapJump
                }
            }
        }
    }
    //读取媒体信息：uri作为唯一key
    private var MediaInfo_MediaUri = Uri.EMPTY!!
    private fun getMediaInfo(uri: Uri){
        retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(this@PlayerActivityNeo, uri) }
        catch (_: Exception) {
            onDestroy_fromErrorExit = true
            showCustomToast("无法解码视频信息", Toast.LENGTH_SHORT, 3)
            showCustomToast("播放失败", Toast.LENGTH_SHORT, 3)
            state_need_return = true
            finish()
            return
        }
        PlayerSingleton.MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "error"
        val MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "error"
        var MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "error"
        val MediaInfo_MediaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
        val MediaInfo_MediaUriString = uri.toString()
        retriever.release()
        val MediaInfo_AbsolutePath = getFilePath(this@PlayerActivityNeo, MediaInfo_MediaUri).toString()
        var MediaInfo_FileName = (File(MediaInfo_AbsolutePath)).name ?: "error"
        //处理值
        if (PlayerSingleton.MediaInfo_MediaType.contains("video")){
            PlayerSingleton.MediaInfo_MediaType = "video"
        }
        else if (PlayerSingleton.MediaInfo_MediaType.contains("audio")){
            PlayerSingleton.MediaInfo_MediaType = "music"
        }
        if (MediaInfo_FileName == "error"){
            MediaInfo_FileName = "未知媒体标题"
        }
        if (MediaInfo_MediaArtist == "error" || MediaInfo_MediaArtist == "<unknown>"){
            MediaInfo_MediaArtist = "未知艺术家"
        }
        //统一保存到viewModel
        vm.saveInfoToViewModel(
            PlayerSingleton.MediaInfo_MediaType,
            MediaInfo_MediaTitle,
            MediaInfo_MediaArtist,
            MediaInfo_MediaDuration.toLong(),
            MediaInfo_FileName,
            MediaInfo_AbsolutePath,
            MediaInfo_MediaUri,
            MediaInfo_MediaUriString
        )

    }
    //确认关闭操作
    private var state_FromExitKeepPlaying = false
    private var state_FromExitCloseAllStuff = false
    private fun EnsureExit(flag_close_all: Boolean){
        if (vm.PREFS_KeepPlayingWhenExit){
            if (flag_close_all){
                EnsureExit_but_keep_playing()
                if (state_onPlayError){ PlayerSingleton.releasePlayer() }
            }else{
                EnsureExit_close_all_stuff()
            }
        }
        else{
            if (flag_close_all){
                EnsureExit_close_all_stuff()
            }else{
                EnsureExit_but_keep_playing()
                if (state_onPlayError){ PlayerSingleton.releasePlayer() }
            }
        }
    }
    private fun EnsureExit_close_all_stuff(){
        state_FromExitKeepPlaying = false
        state_FromExitCloseAllStuff = true
        //发信息回主列表
        val DetailData = Intent().apply {
            putExtra("key", "EnsureExitCloseAllStuff")
        }
        setResult(RESULT_OK, DetailData)
        //停止UI端操作
        scroller.stopScroll()
        stopVideoSmartScroll()
        stopVideoSeek()
        stopScrollerSync()
        stopVideoTimeSync()
        //停止服务端操作
        PlayerSingleton.clearMediaInfo()
        PlayerSingleton.DevastatePlayBundle(application)
        finish()
    }
    private fun EnsureExit_but_keep_playing(){
        state_FromExitKeepPlaying = true
        state_FromExitCloseAllStuff = false
        //发信息回主列表
        val DetailData = Intent().apply {
            putExtra("key", "EnsureExitButKeepPlaying")
        }
        setResult(RESULT_OK, DetailData)
        //停止UI端操作
        scroller.stopScroll()
        stopVideoSmartScroll()
        stopVideoSeek()
        stopScrollerSync()
        stopVideoTimeSync()
        //发回主界面
        val data = Intent().apply { putExtra("key", "EnsureExitButKeepPlaying") }
        setResult(RESULT_OK, data)
        //不停止服务端操作
        if (playerReadyFrom_FirstEntry){
            PlayerSingleton.DevastatePlayBundle(application)
            PlayerSingleton.clearMediaInfo()
        }
        finish()
    }
    //数据库预写
    private fun DataBasePreWrite(){
        lifecycleScope.launch(Dispatchers.IO) {
            MediaItemRepo.get(this@PlayerActivityNeo).preset_all_row_default(vm.MediaInfo_FileName)
        }
    }
    //关闭遮罩:输入1带淡出,输入0直接消失
    private fun closeCover(flag_anim: Int, num_duration: Long){
        val cover = findViewById<LinearLayout>(R.id.cover)

        if (flag_anim == 0){
            cover.visibility = View.GONE
        }
        else if (flag_anim == 1){
            cover.animate().alpha(0f).setDuration(num_duration).withEndAction {
                cover.visibility = View.GONE
            }
        }
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
                    vm.OrientationValue = 1
                } else if (orientation in 81..<100) {
                    vm.OrientationValue = 2
                } else if (orientation in 341..<360) {
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
                            //更改状态并发起旋转
                            setOrientation_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (vm.OrientationValue == 2) {
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
        if (vm.PREFS_SealOEL) return
        if (state_orientationListenerEnabled) return

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
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        state_EnterAnimationCompleted = true
        //隐藏顶部分割线
        HideTopLine()
    }

    override fun onPause() {
        super.onPause()
        //关闭视频控制
        stopVideoSeek()
        stopVideoSmartScroll()
        stopVideoTimeSync()
        stopScrollerSync()
        //关闭旋转监听器
        stopOrientationListener()
    }

    override fun onStop() {
        super.onStop()
        startOnStopDecider()
    }

    override fun onResume() {
        super.onResume()
        //区分onResume原因：
        if (vm.state_onStopDecider_Running){
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
            //Log.d("SuMing","onResume: 决策函数运行中")
        }else{
            //活动重建
            if (vm.state_onStop_ByReBuild){
                //Log.d("SuMing","onResume: 活动重建")
                //开启视频控件
                startScrollerSync()
                startVideoTimeSync()
            }
            //首次启动
            /*
            if (vm.state_onStop_ByRealExit){
                //Log.d("SuMing","onResume: 首次启动")
            }

             */
            //活动暂退桌面：小窗模式在这里包含
            if (vm.state_onStop_ByLossFocus){
                //可能来自浮窗
                if (state_FromFloatingWindow){
                    //关闭小窗服务
                    stopFloatingWindow()
                    //重新绑定播放器
                    playerView.player = null
                    playerView.player = player
                }
                //开始继续播放
                PlayerSingleton.ActivityOnResume()
                //开启视频控件
                startScrollerSync()
                startVideoTimeSync()
                //Log.d("SuMing","onResume: 活动暂退桌面")
            }
            //通用步骤：
            //重置状态
            vm.set_onStop_all_reset()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //重建类型判定变量
        state_onDestroy_reach = true
        //关闭本地监听器
        unregisterEventBus()
        stopOrientationListener()
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
                    if (uri == PlayerSingleton.getMediaInfoUri()) { return }
                    else{
                        //变更保存的信息
                        MediaInfo_MediaUri = uri
                        //设置新的媒体项
                        setNewMediaItem(MediaInfo_MediaUri)
                    }
                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {
                    vm.state_FromSysStart = true
                    val uri = newIntent.data ?: return finish()
                    //判断是否是同一个视频
                    if (uri == PlayerSingleton.getMediaInfoUri()) { return }
                    else{
                        //变更保存的信息
                        MediaInfo_MediaUri = uri
                        //设置新的媒体项
                        setNewMediaItem(MediaInfo_MediaUri)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //重建类型判定变量
        state_onSaveInstanceState_reach = true
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
            //更新界面显示
            updateScreenParameters()
            //启动隐藏控件倒计时
            startIdleTimer()

        }
        //竖屏
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //更新界面显示
            updateScreenParameters()

        }
    }
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        //判断退出方式
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            if (state_FromExitKeepPlaying){
                //显示顶部分割线
                ShowTopLine()
                //使用收起动画
                overridePendingTransition(
                    R.anim.slide_just_appear,
                    R.anim.slide_out
                )
            }
        }

    }



    //退出动作决策程序
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
                    //Log.d("SuMing","onStop: 活动暂退桌面")
                    vm.set_onStop_ByLossFocus()
                    if(!state_FromFloatingWindow){
                        PlayerSingleton.ActivityOnStop()
                    }
                }
                //活动被销毁
                else{
                    //活动销毁但保存了数据：活动因深色模式切换或尺寸切换发生重建
                    if (state_onSaveInstanceState_reach){
                        //Log.d("SuMing","onStop: 活动重建")
                        vm.set_onStop_ByReBuild()
                    }
                    //活动销毁且未保存数据：确实退出了活动
                    else{
                        //Log.d("SuMing","onStop: 活动确实退出")
                        vm.set_onStop_ByRealExit()
                    }
                }
                //决策函数运行结束
                vm.state_onStopDecider_Running = false
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
        vm.state_onStopDecider_Running = true
        onStopDecideHandler.post(onStopDecideTask)
    }
    private fun stopOnStopDecider() {
        onStopDecideHandler.removeCallbacks(onStopDecideTask)
        vm.state_onStopDecider_Running = false
    }
    //隐藏顶部分割线
    private fun HideTopLine(){
        val TopLine = findViewById<View>(R.id.TopLine)
        TopLine.visibility = View.GONE
    }
    private fun ShowTopLine(){
        val TopLine = findViewById<View>(R.id.TopLine)
        TopLine.visibility = View.VISIBLE
    }
    //解除亮度控制
    private fun unBindBrightness(){
        val windowInfo = window.attributes

        windowInfo.screenBrightness = -1f
        window.attributes = windowInfo

        vm.BrightnessChanged = false

        showCustomToast("已解除亮度控制,现在您可以使用系统亮度控制了", Toast.LENGTH_SHORT, 3)
    }
    //dp转px
    private fun dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    //px转dp
    private fun px2dp(pxValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
    //设置项修改封装函数
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
                MediaItemRepo.get(this@PlayerActivityNeo).update_PREFS_AlwaysSeek(vm.MediaInfo_FileName,vm.PREFS_AlwaysSeek)
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
                MediaItemRepo.get(this@PlayerActivityNeo).update_PREFS_LinkScroll(vm.MediaInfo_FileName,vm.PREFS_LinkScroll)
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
                MediaItemRepo.get(this@PlayerActivityNeo).update_PREFS_TapJump(vm.MediaInfo_FileName,vm.PREFS_TapJump)
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
    //清除进度条截图
    private fun clearMiniature(){
        File(filesDir, "miniature/${vm.MediaInfo_FileName.hashCode()}/scroller").deleteRecursively()
    }
    //视频区域抬高(含Job)
    @Suppress("DEPRECATION")
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
        if (vm.PREFS_EnablePlayAreaMoveAnim) {
            //取消Job:播放区域上移
            MovePlayAreaJob?.cancel()

            playerView.animate()
                .translationY(0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300)
                .start()
        }else{
            playerView.translationY = 0f
        }

    }
    private fun MovePlayArea_up() {
        if (vm.PREFS_EnablePlayAreaMoveAnim){
            playerView.animate()
                .translationY(-(vm.YaxisDestination))
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300)
                .start()
        }else{
            playerView.translationY = -(vm.YaxisDestination)
        }
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

            if (vm.wasPlaying){ player.play() }

        }
        notice("请稍等", 3000)
        vm.wasPlaying = player.isPlaying
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
    @SuppressLint("UseKtx")
    private fun CaptureCurrentFrameAsCover(filename: String) {
        fun handleSuccess(bitmap: Bitmap) {
            //创建文件占位并保存
            val cover_file = File(filesDir, "miniature/video_cover/${filename.hashCode()}.webp")
            cover_file.parentFile?.mkdirs()
            cover_file.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
            }
            //发布完成消息
            val uriNumOnly = ContentUris.parseId(MediaInfo_MediaUri)
            ToolEventBus.sendEvent_withExtraString(Event("PlayerActivity_CoverChanged", uriNumOnly.toString()))
            showCustomToast("截取封面完成", Toast.LENGTH_SHORT,3)
            //恢复播放状态
            if (vm.wasPlaying){ player.play() }
        }
        //记录原本的播放状态
        vm.wasPlaying = player.isPlaying
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
    private fun useDefaultCover(filename: String){
        val defaultCoverBitmap = vectorToBitmap(this, R.drawable.ic_album_video_album)
        if (defaultCoverBitmap == null) {
            showCustomToast("从本地文件提取默认封面素材失败", Toast.LENGTH_SHORT,3)
            return
        }
        val processedBitmap = processCenterCrop(defaultCoverBitmap)
        //创建目录
        val covers_path = File(filesDir, "miniature/video_cover")
        if (!covers_path.exists()) { covers_path.mkdirs() }
        //保存图片
        val cover_item_file = File(covers_path, "${filename.hashCode()}.webp")
        cover_item_file.outputStream().use {
            processedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)
        }
        //发布完成消息
        ToolEventBus.sendEvent_withExtraString(Event("PlayerActivity_CoverChanged", filename))
        showCustomToast("已完成", Toast.LENGTH_SHORT,3)
    }
    private fun processCenterCrop(src: Bitmap): Bitmap {
        //以后可以添加为传入量
        //processCenterCrop(src: Bitmap, targetWidth: Int = 300, targetHeight: Int): Bitmap {
        val targetWidth = 400
        val targetHeight = (targetWidth * 10 / 9)

        val srcWidth = src.width
        val srcHeight = src.height

        val scale = (targetWidth.toFloat() / srcWidth).coerceAtLeast(targetHeight.toFloat() / srcHeight)

        val scaledWidth = scale * srcWidth
        val scaledHeight = scale * srcHeight

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val targetBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(targetBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(src, null, destRect, paint)

        return targetBitmap
    }
    private fun vectorToBitmap(context: Context, @DrawableRes resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null

        val bitmap = createBitmap(400, 600)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    } //矢量图转Bitmap
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
    private fun ExitByCheckOrientation(){
        //退出前先转为竖屏
        if (vm.PREFS_SwitchPortraitWhenExit){
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                vm.setManual()
                setOrientation_PORTRAIT()
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (!vm.state_controllerShowing){
                    notice("再按一次退出",2000)
                    setControllerVisible()
                    vm.state_controllerShowing = true
                    lifecycleScope.launch{
                        delay(75)
                        ToolVibrate().vibrate(this@PlayerActivityNeo)
                    }
                }
                //确认退出
                else{
                    if (state_EnterAnimationCompleted){ EnsureExit(true) }
                    else{
                        state_EnterAnimationCompleted = true
                        notice("再按一次退出",2000)
                        //PlayerSingleton.setExitBeforeRead(true)
                        //val DetailData = Intent().apply { putExtra("ClosedBeforePlayerReady", "YesThatsWhatYouThink") }
                        //setResult(RESULT_OK, DetailData)
                        //finish()
                        return
                    }
                }
            }
        }
        //横屏可直接退出
        else{
            if (!vm.state_controllerShowing){
                notice("再按一次退出",2000)
                setControllerVisible()
                vm.state_controllerShowing = true
                lifecycleScope.launch{
                    delay(75)
                    ToolVibrate().vibrate(this@PlayerActivityNeo)
                }
            }
            //确认退出
            else{
                if (state_EnterAnimationCompleted){
                    EnsureExit(true)
                }
                else{
                    state_EnterAnimationCompleted = true
                    notice("再按一次退出",2000)
                    //PlayerSingleton.setExitBeforeRead(true)
                    //val DetailData = Intent().apply { putExtra("ClosedBeforePlayerReady", "YesThatsWhatYouThink") }
                    //setResult(RESULT_OK, DetailData)
                    //finish()
                    return
                }
            }
        }

    }

    private fun playerReady(){
        isSeekReady = true
        if (playerReadyFrom_FirstEntry) {
            playerReadyFrom_FirstEntry = false

            stopScrollerSync()
            startScrollerSync()

            //启动播放
            continuePlay(need_requestFocus = true, force_request = true, need_fadeIn = false)
            //隐藏遮罩
            closeCover(1,20)

            return
        }
        if (playerReadyFrom_SmartScrollLastSeek){
            playerReadyFrom_SmartScrollLastSeek = false
            isSeekReady = true
            //恢复播放状态
            if (vm.wasPlaying){
                continuePlay(need_requestFocus = false, force_request = false, need_fadeIn = false)
            }else{
                player.pause()
                player.setPlaybackSpeed(1f)
            }
            vm.allowRecord_wasPlaying = true

            return
        }
        if (playerReadyFrom_LastSeek){
            playerReadyFrom_LastSeek = false
            isSeekReady = true
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            //恢复播放状态
            if (vm.wasPlaying){
                continuePlay(need_requestFocus = false, force_request = false, need_fadeIn = false)
            }
            vm.allowRecord_wasPlaying = true

            return
        }
        if (playerReadyFrom_NormalSeek){
            playerReadyFrom_NormalSeek = false
            isSeekReady = true
            return
        }

    }
    //playEnd不应该控制播放,而是只专注于用户端界面的控制,播放控制转移到单例中
    private fun playerEnd(){
        val loopMode = PlayerListManager.getLoopMode(this)
        when (loopMode) {
            "ONE" -> {
                notice("单集循环", 3000)
            }
            "ALL" -> {

            }
            "OFF" -> {
                vm.playEnd = true
                notice("视频结束", 1000)
                //停止被控控件
                stopVideoTimeSync()
                stopScrollerSync()
                //播放结束时让控件显示
                setControllerVisible()
                Handler(Looper.getMainLooper()).postDelayed({ stopScrollerSync() }, 100)
                IDLE_Timer?.cancel()
                //自动退出和循环播放控制:需要重做到单例中
                //vm.state_FromSysStart
                //vm.PREFS_ExitWhenEnd

            }
        }
    }
    @Suppress("SameParameterValue")
    private fun recessPlay(need_fadeOut: Boolean){
        PlayerSingleton.recessPlay(need_fadeOut)

        stopVideoTimeSync()
        stopScrollerSync()
    }
    @Suppress("SameParameterValue")
    private fun continuePlay(need_requestFocus: Boolean, force_request: Boolean, need_fadeIn: Boolean){
        PlayerSingleton.continuePlay(need_requestFocus, force_request, need_fadeIn)


        //界面控件操作
        startScrollerSync()
        startVideoTimeSync()
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
    private fun getFilePath(context: Context, uri: Uri): String? {
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
    //显示相关
    //界面控件
    //<editor-fold desc="界面控件">
    //经典播放页专属
    //private lateinit var seekBar: SeekBar
    //private lateinit var controller_bottom_card : CardView
    //新晋播放页专属
    private lateinit var scroller : RecyclerView
    private lateinit var controller_bottom_bar : LinearLayout //底部按钮区域
    //通用
    private lateinit var rootConstraint : ConstraintLayout //根约束布局
    private lateinit var controllerLayer : ConstraintLayout //控件层
    private lateinit var controller_top_bar : LinearLayout //顶部按钮区域
    private lateinit var controller_timer_current : TextView //当前时间
    private lateinit var controller_timer_total : TextView //总时间
    private lateinit var NoticeCard : CardView //通知胶囊卡片
    private lateinit var playerView: PlayerView //播放区域
    //</editor-fold>
    //显示相关函数
    //<editor-fold desc="显示相关函数">
    //刷新视频总长度
    private fun updateTimeCard(){
        controller_timer_total.text = FormatTime_onlyNum(vm.MediaInfo_MediaDuration.toLong())
    }
    //刷新按钮状态
    private fun updateButtonState(){
        val Button = findViewById<ImageView>(R.id.controller_button_playorpause)
        if (player.isPlaying){
            Button.setImageResource(R.drawable.ic_controller_neo_pause)
        }
        else{
            Button.setImageResource(R.drawable.ic_controller_neo_play)
        }
    }
    //刷新进度条
    private fun updateScrollerAdapter(){
        lifecycleScope.launch(Dispatchers.IO) {
            //获取ViewModel
            val playerScrollerViewModel by viewModels<PlayerScrollerViewModel>()
            //预先规划文件夹结构并创建 + 基于文件名哈希区分
            val SubDir_ThisMedia = File(cacheDir, "Media/${vm.MediaInfo_FileName.hashCode()}/scroller")
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
                if (vm.MediaInfo_MediaDuration > 1_0000_000L) {
                    ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration / 500.0).toInt()
                    ScrollerInfo_PicNumber = 500
                }
                else if (vm.MediaInfo_MediaDuration > 7500_000L) {
                    ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration / 400.0).toInt()
                    ScrollerInfo_PicNumber = 400
                }
                else if (vm.MediaInfo_MediaDuration > 5000_000L) {
                    ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration / 300.0).toInt()
                    ScrollerInfo_PicNumber = 300
                }
                else if (vm.MediaInfo_MediaDuration > 500_000L) {
                    ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration / 200.0).toInt()
                    ScrollerInfo_PicNumber = 200
                }
                else {
                    ScrollerInfo_EachPicDuration = 1000
                    ScrollerInfo_PicNumber = min((max((vm.MediaInfo_MediaDuration / 1000).toInt(), 1)), 500)
                }
            }
            //使用普通进度条
            else if (vm.MediaInfo_MediaDuration / 1000 >= ScrollerInfo_MaxPicNumber) {
                ScrollerInfo_EachPicWidth = (40 * DisplayMetrics.density).toInt()
                ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration.div(100) * 100).toInt() / ScrollerInfo_MaxPicNumber
                ScrollerInfo_PicNumber = ScrollerInfo_MaxPicNumber
            } else {
                ScrollerInfo_EachPicWidth = (40 * DisplayMetrics.density).toInt()
                ScrollerInfo_PicNumber = (vm.MediaInfo_MediaDuration / 1000).toInt() + 1
                ScrollerInfo_EachPicDuration = (vm.MediaInfo_MediaDuration.div(100) * 100).toInt() / ScrollerInfo_PicNumber
            }

            //移除查询参数
            val MediaInfo_AbsolutePath_clean = vm.MediaInfo_AbsolutePath.substringBefore("?")


            //进度条边界设置
            withContext(Dispatchers.Main){
                setScrollerPadding()
            }


            //绑定Adapter
            //使用超长进度条
            if (vm.PREFS_UseLongScroller){
                withContext(Dispatchers.Main) {
                    scroller.adapter = PlayerScrollerLongAdapter(this@PlayerActivityNeo,
                        MediaInfo_AbsolutePath_clean,
                        vm.MediaInfo_FileName,
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
                    scroller.adapter = PlayerScrollerAdapter(this@PlayerActivityNeo,
                        MediaInfo_AbsolutePath_clean,
                        vm.MediaInfo_FileName,
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
                    syncScrollRunnableGap = ((vm.MediaInfo_MediaDuration / 1000) * (1000.0 / 3600)).toLong()
                    if (vm.PREFS_UseLongScroller){
                        syncScrollRunnableGap = 10L
                    }
                }
            }
            startSyncScrollerGapControl()
            startScrollerSync()
            delay(200)
            startVideoTimeSync()

        }
    }
    //控件隐藏和显示
    private fun setControllerInvisibleNoAnimation() {
        //状态标记变更
        vm.state_controllerShowing = false

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
        vm.state_controllerShowing = false

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
    private fun setControllerVisible() {
        //状态标记变更
        vm.state_controllerShowing = true

        //启动被控控件控制
        startVideoTimeSync()
        //仅在新晋播放页使用
        startScrollerSync()
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
        if (vm.state_controllerShowing){
            setControllerInvisible()
        }else{
            setControllerVisible()
        }
    }
    private fun setPageToDark(){
        val playerViewContainer = findViewById<FrameLayout>(R.id.playerContainer)
        val cover = findViewById<LinearLayout>(R.id.cover)
        val top_line = findViewById<View>(R.id.controller_scroller_top_line)
        val middle_line = findViewById<View>(R.id.player_scroller_center_line)

        playerViewContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        cover.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
        scroller.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.BlackGrey))
        top_line.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.player_scroller_top_line_black))
        middle_line.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
    }
    //进度条端点配置:仅在新晋播放页使用
    private fun setScrollerPadding(){

        scroller.layoutManager = LinearLayoutManager(this@PlayerActivityNeo, LinearLayoutManager.HORIZONTAL, false)
        scroller.itemAnimator = null
        scroller.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        scroller.layoutParams.width = 0
        sidePadding = DisplayMetric_ScreenWidth / 2

        //竖屏
        if (state_screen_orientation == 0) {
            scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        }
        //横屏
        else if (state_screen_orientation == 1) {
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
        startScrollerSync()

    }
    //状态栏配置
    @Suppress("DEPRECATION")
    private fun setStatusBarParams(){
        //横屏
        if (state_screen_orientation == 1){
            //控件层参数
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
        //竖屏
        else if (state_screen_orientation == 0) {
            //控件层参数
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
    }
    //刷新横屏按钮
    private fun updateLandscapeButton(){
        if (state_playerType == 0) return

        val ButtonMaterialSwitchLandscape = findViewById<MaterialButton>(R.id.buttonMaterialSwitchLandscape)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityNeo, R.color.ButtonBgClosed))
        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ButtonMaterialSwitchLandscape.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PlayerActivityNeo, R.color.ButtonBg))
        }

    }
    //通知卡片位置设置
    private fun setNoticeCardPosition(){
        //横屏
        if (state_screen_orientation == 1) {
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(5f))
        }
        //竖屏
        else if (state_screen_orientation == 0) {
            (NoticeCard.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(100f))
        }
    }
    //调整控件位置
    private fun setControllerPosition(){
        //横屏
        if (state_screen_orientation == 1) {
            //控件位置动态调整
            val displayManager = this.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = display?.rotation
            //正向横屏
            if (rotation == Surface.ROTATION_90) {
                when (state_playerType) {
                    //经典播放页
                    0 -> {
                        (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight)
                        //(controller_bottom_card.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight + 50)
                    }
                    //新晋播放页
                    1 -> {
                        (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight)
                        (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (vm.statusBarHeight)
                    }
                    //错误直接退出
                    else -> {
                        showCustomToast("播放器样式参数错误：既不是oro也不是neo", Toast.LENGTH_SHORT, 3)
                        finish()
                    }
                }
            }
            //反向横屏
            else if (rotation == Surface.ROTATION_270) {
                //经典播放页
                if (state_playerType == 0){
                    (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight)
                    //(controller_bottom_card.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight + 50)
                }
                //新晋播放页
                else if (state_playerType == 1){
                    (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight)
                    (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (vm.statusBarHeight)
                }
                //错误直接退出
                else{
                    showCustomToast("播放器样式参数错误：既不是oro也不是neo", Toast.LENGTH_SHORT, 3)
                    finish()
                }
            }
        }
        //竖屏重置
        else if (state_screen_orientation == 0) {
            //经典播放页
            if (state_playerType == 0){
                //(controller_bottom_card.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = (dp2px(10f))
                //(controller_bottom_card.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = (dp2px(10f))
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
            }
            //新晋播放页
            else if (state_playerType == 1){
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
                (controller_top_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
                (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
                (controller_bottom_bar.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
            }
        }
    }
    //屏幕尺寸和方向记录
    private lateinit var DisplayMetrics: DisplayMetrics
    private var DisplayMetric_ScreenWidth = 0
    private var DisplayMetric_ScreenHeight = 0
    private var state_screen_orientation = 0  //0:竖屏 1:横屏
    private fun displayMetricsLoad(){
        //记录屏幕宽高
        DisplayMetrics = resources.displayMetrics
        DisplayMetric_ScreenWidth = DisplayMetrics.widthPixels
        DisplayMetric_ScreenHeight = DisplayMetrics.heightPixels
        //屏幕方向记录
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { state_screen_orientation = 1 }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { state_screen_orientation = 0 }
    }
    //初始化界面控件 + 界面参数读取 + 控件位移
    private var state_playerType = 1  //0:传统oro 1:新晋neo
    private fun updateScreenParameters(){
        //屏幕尺寸和方向记录
        displayMetricsLoad()
        //状态栏设置
        setStatusBarParams()
        //调整控件位置
        setControllerPosition()
        //通知卡片位置
        setNoticeCardPosition()
        //恢复隐藏控件状态
        if (!vm.state_controllerShowing){ setControllerInvisibleNoAnimation() }
        //仅在新晋播放页使用
        if (state_playerType == 1){
            //刷新横屏按钮(经典页面自动跳过)
            updateLandscapeButton()
            //设定进度条边界
            setScrollerPadding()
        }

    }
    //</editor-fold>
    //初始化
    private fun preCheck(){
        //初始化部分界面控件
        fun initElement(){
            //经典播放页专属
            //seekBar = findViewById(R.id.controller_seek_bar)
            //controller_bottom_card = findViewById(R.id.controller_bottom_card)
            //新晋播放页专属
            scroller = findViewById(R.id.controller_scroller_recyclerView) //滚动条
            controller_bottom_bar = findViewById(R.id.controller_bottom_bar) //底部按钮区域
            //通用
            rootConstraint = findViewById(R.id.rootConstraint) //根约束布局
            controllerLayer = findViewById(R.id.controllerLayer) //控件层
            controller_top_bar = findViewById(R.id.controller_top_bar) //顶部按钮区域
            controller_timer_current = findViewById(R.id.controller_timer_current) //当前时间
            controller_timer_total = findViewById(R.id.controller_timer_total) //总时间
            NoticeCard = findViewById(R.id.NoticeCard) //通知胶囊卡片
            playerView = findViewById(R.id.playerView) //播放区域
        }
        initElement()
        //初始化界面参数
        updateScreenParameters()

        //获取自动旋转状态
        rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
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

            scrollParam1 = ( player.currentPosition / ScrollerInfo_EachPicDuration ).toInt()
            scrollParam2 = (( player.currentPosition - scrollParam1 * ScrollerInfo_EachPicDuration ) * ScrollerInfo_EachPicWidth / ScrollerInfo_EachPicDuration ).toInt()

            if (vm.playEnd && !player.isPlaying){
                scrollParam1 -= 1
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
        if (!vm.PREFS_LinkScroll){ return }
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
            videoTimeSyncHandler_currentPosition = player.currentPosition

            controller_timer_current.text = FormatTime_onlyNum(videoTimeSyncHandler_currentPosition)

            videoTimeSyncHandler.postDelayed(this, 1000)
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
            var delayGap = if (scrollerState_Pressed){ 30L } else{ 30L }
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
                if (lastSeekExecuted) return
                lastSeekExecuted = true

                global_SeekToMs = scrollerPosition.toLong()
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                smartScrollRunnableRunning = false
                playerReadyFrom_SmartScrollLastSeek = true
                startSmartScrollLastSeek()
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
            VideoSeekHandler_seekToMs = (VideoSeekHandler_percent * player.duration).toLong()
            //反向滚动时防止seek到前面
            if (scrollerState_BackwardScroll){
                if (vm.PREFS_AlwaysSeek) {
                    if (VideoSeekHandler_seekToMs < player.currentPosition){
                        player.pause()
                        if (VideoSeekHandler_seekToMs < 50){
                            playerReadyFrom_LastSeek = true
                            player.seekTo(0)
                        }
                        else{
                            if (isSeekReady){
                                isSeekReady = false
                                playerReadyFrom_NormalSeek = true
                                player.seekTo(VideoSeekHandler_seekToMs)
                            }
                        }
                    }
                }
                else{
                    player.pause()
                    if (VideoSeekHandler_seekToMs < 50){
                        playerReadyFrom_LastSeek = true
                        player.seekTo(0)
                    }
                    else{
                        if (isSeekReady){
                            isSeekReady = false
                            playerReadyFrom_NormalSeek = true
                            player.seekTo(VideoSeekHandler_seekToMs)
                        }
                    }
                }
            }
            //正向seek
            else{
                player.pause()
                if (isSeekReady){
                    isSeekReady = false
                    playerReadyFrom_NormalSeek = true
                    player.seekTo(VideoSeekHandler_seekToMs)
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
                player.setSeekParameters(SeekParameters.EXACT)
                playerReadyFrom_LastSeek = true
                player.seekTo(global_SeekToMs)
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
                player.seekTo(global_SeekToMs)
            }else{
                videoSeekHandler.post(this)
            }
        }
    }
    private fun startSmartScrollLastSeek() {
        SmartScrollLastSeekHandler.post(SmartScrollLastSeek)
    }
    //显示通知
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
    //长按退出按钮
    private var ExitJob: Job? = null
    private fun ExitJob() {
        ExitJob?.cancel()
        ExitJob = lifecycleScope.launch {
            delay(500)
            ToolVibrate().vibrate(this@PlayerActivityNeo)
            EnsureExit(false)
        }
    }


}
