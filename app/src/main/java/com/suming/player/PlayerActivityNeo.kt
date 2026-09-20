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
import android.util.DisplayMetrics
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
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
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
import androidx.core.view.isVisible
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.IndepFragment.PlayerFragmentEqualizer
import com.suming.player.ActivityComponent.IndepFragment.PlayerFragmentMediaInfo
import com.suming.player.ActivityComponent.PlayerActivity.PlayerFragmentMoreButton
import com.suming.player.ActivityComponent.PlayerActivity.PlayerScrollerAdapter
import com.suming.player.ActivityComponent.PlayerActivity.PlayerViewModel
import com.suming.player.ActivityComponent.PlayerActivity.S_Area_Helper
import com.suming.player.ActivityComponent.PlayerActivity.SmoothScroller
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncPack_ListManager.ListManagerFragment
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.ConnectCenter
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.FrameExtractor
import com.suming.player.FuncionalPack.FrameListener
import com.suming.player.FuncionalPack.IntentRepo
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.SystemListener
import com.suming.player.FuncionalPack.ScrollerHelper
import com.suming.player.FuncionalPack.SettingsCenter
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
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow

@UnstableApi
@Suppress("NewApi","/unused")
class PlayerActivityNeo: AppCompatActivity() {
    //变量初始化
    //<editor-fold desc="变量初始化">
    //方向回调
    private var orientationChangeTime = 0L
    private var LastOrientationChangeTime = 0L

    //自动旋转状态
    private var rotationSetting = 0

    //倍速播放
    private var currentSpeed = 1.0f

    //</editor-fold>

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivityNeo: $msg")
        }
    }
    //context
    private val context = this@PlayerActivityNeo
    //获取播放器引用
    private var player: ExoPlayer? = null
    //连接到viewModel
    private val viewModel: PlayerViewModel by viewModels()
    //空字段
    private val Undefined = ""
    //点击过滤
    private var clickMillis_MoreOptionPage = 0L
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()




    override fun attachBaseContext(newBase: Context?) {
        //在onCreate之前切换颜色模式,可避免活动重建
        if (SettingsCenter.GET_PREFS_AlwaysUseDarkTheme()) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        }
        super.attachBaseContext(newBase)
    }
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化
        init()

        //确保播放器已启动并确保本地监听器已附加
        connectToExoPlayer()

        startExoPlayerIdleObserver()

        //注册控件和手势
        register()

        //主业务逻辑
        mainBusiness(savedInstanceState)


        //缓存需要频繁取用的变量+数值计算
        lifecycleScope.launch(Dispatchers.IO) {
            //播放区域移动动画
            viewModel.PRF_Cache_EnablePlayAreaMove = SettingsCenter.GET_PREFS_EnablePlayAreaMoveAnim()
            //播放区域移动动画距离计算
            if (viewModel.PRF_Cache_EnablePlayAreaMove){
                if (viewModel.PRF_Cache_EnablePlayAreaMove_Distance == 0f){
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
                    viewModel.PRF_Cache_EnablePlayAreaMove_Distance = (normalCenterMarginTop - areaCenterMarginTop).toFloat()

                }
            }

            //寻帧时关键帧偏好
            val SyncFramePrefs = SettingsCenter.GET_PREFS_Video_SyncFramePrefs()
            viewModel.PRF_Cache_SyncFrame_Dynamic = SyncFramePrefs == SettingsCenter.SYNC_FRAME_PREFS_Dynamic
            if (SyncFramePrefs == SettingsCenter.SYNC_FRAME_PREFS_AlwaysSync){
                withContext(Dispatchers.Main){
                    setSeekParameter_useSync(1,true)
                }
            }else if (SyncFramePrefs == SettingsCenter.SYNC_FRAME_PREFS_AlwaysExact){
                withContext(Dispatchers.Main){
                    setSeekParameter_useSync(0,true)
                }
            }
            //竖屏时也开启自动隐藏控件
            viewModel.PRF_Cache_EnableAutoHideController_whenPortrait = SettingsCenter.GET_PRF_EnableAutoHideController_whenPortrait()
            //是否禁用播放区域上下滑动手势
            viewModel.PRF_Cache_DisableViewFollowing = SettingsCenter.GET_PREFS_Video_DisableViewFollowing()

            //读取进度条配置(已不再支持为每个视频单独配置,但暂未从数据库移除数据)
            viewModel.PREFS_AlwaysSeek = SettingsCenter.GET_PREFS_EnableAlwaysSeek()
            viewModel.PREFS_LinkScroll = SettingsCenter.GET_PREFS_EnableLinkScroll()
            viewModel.PREFS_TapJump = SettingsCenter.GET_PREFS_EnableTapJump()

            //下滑关闭距离(dp转px)
            viewModel.value_scrollDownExitDistance = dp2px(50f)

            //视频寻帧间隔
            value_seekVideo_runnableGapMs = SettingsCenter.get_value_seekVideo_runnableGapMs()
            //进度条更新间隔
            value_syncScroller_runnableGapMs = SettingsCenter.get_value_syncScroller_runnableGapMs()
            //时间戳更新间隔
            value_timeStamp_updateGapMs = SettingsCenter.get_value_timeStamp_updateGapMs()
            //SeekBar更新间隔
            value_syncSeekBar_runnableGapMs = SettingsCenter.get_value_syncSeekbar_runnableGapMs()

        }

        //注册监听器
        lifecycleScope.launch(Dispatchers.Main) {
            //开启旋转监听器
            startOrientationListener()
        }

    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        consoleLog("onNewIntent")
        if (newIntent?.action != null){
            when (newIntent.action) {
                //系统面板：分享
                Intent.ACTION_SEND -> {
                    //获取原始链接
                    val (URI, file_path) = detectOriginalInfo_fromIntent(newIntent)
                    val currentUri = PlayerSingleton.get_engine_ongoing_URI().second
                    //判断是否是同一个视频
                    if (URI == currentUri){
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        lifecycleScope.launch(Dispatchers.IO){
                            setNewMediaItem(URI,file_path)
                        }
                    }
                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {
                    val (URI, file_path) = detectOriginalInfo_fromIntent(newIntent)
                    val currentUri = PlayerSingleton.get_engine_ongoing_URI()
                    consoleLog("currentUri: $currentUri, URI: $URI")
                    //判断是否是同一个视频
                    if (URI == currentUri){
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        //设置新的媒体项
                        lifecycleScope.launch(Dispatchers.IO){
                            setNewMediaItem(URI,file_path)
                        }
                    }
                }
                //常规重复调用(来自EntranceActivity)
                IntentRepo.ACTION_NEW_INTENT -> {
                    consoleLog("onNewIntent ACTION_NEW_INTENT")
                    val (URI, file_path) = detectOriginalInfo_fromIntent(newIntent)
                    val ongoing_URI = PlayerSingleton.get_engine_ongoing_URI().second
                    consoleLog("onNewIntent -新的数据: URI:$URI, ongoing_URI:$ongoing_URI")
                    //判断是否是同一个视频
                    if (URI == ongoing_URI){
                        //继续播放
                        continuePlay()
                    }else{
                        //设置新的媒体项
                        lifecycleScope.launch(Dispatchers.IO){
                            setNewMediaItem(URI,file_path)
                        }
                    }
                }
            }
        }
    }
    //初始化
    private fun init() {
        //显示配置
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_player_type_neo)

        //视图初始化
        fun init_view(){
            //s_area
            s_area = findViewById(R.id.controller_s_area)
            s_area_scroller = findViewById(R.id.controller_s_area_scroller)
            s_area_seekbar = findViewById(R.id.controller_s_area_seekbar)
            //控制器view
            scroller = findViewById(R.id.controller_scroller_recyclerView)
            seekbar = findViewById(R.id.controller_seekbar)
            //其他
            controller_bottom_bar = findViewById(R.id.controller_bottom_bar)
            root = findViewById(R.id.root)
            root_secondary = findViewById(R.id.root_secondary)
            controllerLayer = findViewById(R.id.controllerLayer)
            controller_top_bar = findViewById(R.id.controller_top_bar)
            controller_timer_current = findViewById(R.id.controller_timer_current)
            controller_timer_total = findViewById(R.id.controller_timer_total)
            noticeCapsule = findViewById(R.id.noticeCapsule)
            playerView = findViewById(R.id.playerView)
            layer_error = findViewById(R.id.player_core_layer_error)
            layer_error_text = findViewById(R.id.layer_error_text)
            cover = findViewById(R.id.cover)

        }
        init_view()


        //是否开启强制高刷
        if (SettingsCenter.GET_PREFS_LockRefreshRate()) requestHighRefreshRate()

        //读取并缓存当前颜色模式
        isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        //读取当前屏幕方向
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


        //初始化界面参数
        updateScreenParameters()

        //获取自动旋转状态
        rotationSetting = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)


        //初始化亮度控制
        brightnessDetectCompute()

        //初始化音量控制
        volumeDetectCompute()

    }





    //注册
    @SuppressLint("ClickableViewAccessibility")
    private fun register() {
        //注册控制按钮
        lifecycleScope.launch(Dispatchers.Main) {
            //退出按钮
            val ButtonExit = findViewById<CircleButton>(R.id.TopBarArea_ButtonExit)
            ButtonExit.setOnClickListener {
                //scroller.stopScroll()
                exitActivity_ensure()
            }
            //更多选项
            val TopBarArea_ButtonMoreOptions = findViewById<CircleButton>(R.id.TopBarArea_ButtonMoreOptions)
            TopBarArea_ButtonMoreOptions.setOnClickListener {
                scroller.stopScroll()
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
                ToolVibrate.vibrate()
                noticeCard.visibility = View.GONE
            }
            //暂停/继续播放
            val PauseButton = findViewById<CircleButton>(R.id.ButtonPause)
            PauseButton.setOnClickListener {
                scroller.stopScroll()
                //控制播放
                if (player?.isPlaying == true) {
                    scroller.stopScroll()
                    pausePlay()
                    stop_S_Area_PassiveControl()
                    notice("暂停", 1000)
                    updateButtonState()
                }else{
                    //暂停滚动
                    scroller.stopScroll()
                    //判断是否需要重播
                    if (PlayerSingleton.get_state_playEnd()) {
                        //PlayerSingleton.remove_state_playEnd() 不消耗状态
                        //寻回起始位置
                        player?.seekTo(0L)
                        //发起重播
                        continuePlay()
                        //平滑滚动到进度条起始位置
                        syncScrollTask_Core_smoothSlowly_Compute(0, true)
                        notice("开始重播", 2000)

                    }else{
                        //普通继续播放
                        continuePlay()
                        notice("继续播放", 2000)
                    }
                }
                //确保播放区域在普通位置
                ensure_moveArea_place()
            }
            //遮罩层点击时检查并关闭
            cover.setOnClickListener {
                ToolVibrate.vibrate()
                if (shouldCloseCover()){
                    cover.visibility = View.GONE
                }else{
                    showCustomToast("???", 3)
                }
            }
            //切换横屏
            val ButtonLandscapeButton = findViewById<CircleButton>(R.id.ButtonLandscape)
            ButtonLandscapeButton.setOnTouchListener { _, event ->
                when (event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        ToolVibrate.vibrate()

                        scroller.stopScroll()

                        //切换横屏
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

                scroller.stopScroll()

                //防止快速点击
                if (System.currentTimeMillis() - clickMillis_MoreOptionPage < 800) {
                    return@setOnClickListener
                }
                clickMillis_MoreOptionPage = System.currentTimeMillis()
                //启动弹窗
                startMoreButtonFragment()
            }

        }


        //注册Fragment监听器(Fragment -> Activity 反向传递需要用带 reverse 的 request key)
        registerFragmentResultListener()
        //监听系统手势
        registerOnBackPressListener()
        //注册播放区域手势操作
        registerGestureLayer()

    }
    //主业务线
    private fun mainBusiness(savedInstanceState: Bundle?) {

        //获取原始链接
        val (URI_U_O, file_path) = detectOriginalInfo_fromIntent(intent)
        //将URI缓存为字符串
        val URI_S_O = URI_U_O.toString()


        //获取正在播放信息
        val ongoing_URI = PlayerSingleton.get_engine_ongoing_URI().second
        val ongoing_MediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()

        //日志-获取到的信息
        //consoleLog("intentUriStandard: $intentUriStandard, ongoing_URI: $ongoing_URI")

        //设置媒体项决策程序 savedInstanceState == null 仅在首次启动时决定是否播放
        if (savedInstanceState == null){
            //
            if (URI_S_O == Undefined && ongoing_URI == Uri.EMPTY ){
                //分支描述:未传入播放链接,也没有正在播放的项,弹窗主动输入(彩蛋分支)

                //弹窗主动输入播放链接
                queryManualInputUri()

            }else{
                if (URI_S_O == Undefined){
                    //分支描述:未传入原始链接,检查正在播放的项

                    //检查有没有正在播放的项
                    if (ongoing_URI == Uri.EMPTY){
                        //没有正在播放的项
                        queryManualInputUri()
                    }else{
                        //有正在播放的项
                        if (ongoing_MediaType == MediaType.Video){
                            //正在播放的是视频,直接绑定
                            connectCurrentMedia()
                        }else{
                            custom_finish()
                        }
                    }
                }else{
                    //分支-传入了原始链接

                    //检查新项是否与当前项相同
                    if (URI_S_O != ongoing_URI.toString()){
                        //传入链接,但与当前播放项不同,播放新项
                        //consoleLog("传入链接,但与当前播放项不同,播放新项")

                        //发起播放新项
                        startPlayNewMedia(URI_U_O,file_path)

                    }else{
                        //传入链接,但与当前播放项相同,直接绑定,但是要先判断是不是视频
                        //consoleLog("传入链接,但与当前播放项相同,直接绑定,但是要先判断是不是视频")
                        if (ongoing_MediaType == MediaType.Video){
                            //正在播放的是视频,直接绑定
                            connectCurrentMedia()
                        }else{
                            custom_finish()
                        }
                    }
                }
            }
        }else{
            //
            if (ongoing_URI == Uri.EMPTY){
                showErrorCover("当前没有正在播放的项")
            }else{
                connectCurrentMedia()
            }
        }


    }




    //提取原始URI信息
    private fun detectOriginalInfo_fromIntent(intent: Intent): Pair<Uri, String> {
        //获取原始链接中的信息
        val Intent_URI = IntentCompat.getParcelableExtra(intent, IntentRepo.URI,  Uri::class.java) ?: Uri.EMPTY
        val Intent_FILE_PATH = intent.getStringExtra(IntentRepo.FILE_PATH) ?: Undefined
        //consoleLog("detectOriginalInfo_fromIntent: Intent_URI = $Intent_URI, Intent_FILE_PATH = $Intent_FILE_PATH")

        return Pair(Intent_URI, Intent_FILE_PATH)
    }
    //手动输入链接并发起播放
    @SuppressLint("InflateParams")
    private fun queryManualInputUri() {
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
                    startPlayNewMedia(userInput.toUri(),file_path = "")
                }else{
                    showCustomToast("链接无效", 3)
                }
            }
        }
        ButtonCancel.setOnClickListener {
            dialog.dismiss()
            custom_finish()
        }
        dialog.show()
        //接管返回操作
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            if (!isUriValid){ custom_finish() }
        }
        //自动弹出键盘
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            EditText.setSelection(Editable.length)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    //开启播放新媒体项
    private fun startPlayNewMedia(URI_UP: Uri, file_path: String) {
        //设置新媒体项
        lifecycleScope.launch(Dispatchers.IO){
            setNewMediaItem(URI_UP,file_path)
        }

    }
    //连接正在播放的媒体
    private fun connectCurrentMedia() {
        //检查正在播放的媒体的类型
        val ongoing_MediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        if (ongoing_MediaType != MediaType.Video){
            showCustomToast("当前正在播放的项不是视频,自动退出播放页", 3)
            custom_finish()
            return
        }

        //绑定播放器视图
        bindPlayerView()

        //添加播放器事件监听
        startExoPlayerListener()

        //关闭遮罩
        closeCover()

        //更新屏幕常亮状态
        updateKeepScreenOn()

        //刷新进度条
        update_S_Area_Adapter()

        //更新时间戳
        updateTimerWindow()
        //刷新控制按钮
        updateButtonState()

        //检查自动播放执行情况
        if (!PlayerSingleton.singleItemState_autoPlayExecuted){
            PlayerSingleton.singleItemState_autoPlayExecuted = true
            //自动播放
            continuePlay(true)
        }

    }

    //Fragment返回值监听
    private fun registerFragmentResultListener() {
        lifecycleScope.launch(Dispatchers.Main) {
            //均衡器面板  Fragment -> Activity  fragment_request_key_equalizer_reverse
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_equalizer_reverse, context) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //开启/退出事件
                    FragmentConnector.fragment_event_close -> {
                        //开启被控组件
                        start_S_Area_PassiveControl()
                        startVideoTimeSync()
                        //播放区域移移动(暂未启用)
                    }
                    FragmentConnector.fragment_event_open -> {
                        //关闭被控组件
                        stop_S_Area_PassiveControl()
                        stopVideoTimeSync()
                        //播放区域移移动(暂未启用)

                    }

                }
            }
            //更多操作面板  Fragment -> Activity  fragment_request_key_more_button_reverse
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_more_button_reverse, context) { _, bundle ->
                val receive_key = bundle.getString(FragmentConnector.receive_key)
                when(receive_key){
                    //截屏
                    FragmentConnector.fragment_more_button_capture_frame -> {
                        captureScreenShot()
                    }
                    //回到视频起始
                    FragmentConnector.fragment_more_button_back_to_start -> {
                        player?.seekTo(0)
                        player?.play()

                        //平滑滚动到进度条起始位置
                        syncScrollTask_Core_smoothSlowly_Compute(0, true)

                        start_S_Area_PassiveControl()
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

                    //进度条SeekMode刷新
                    FragmentConnector.fragment_more_button_updated_seek_mode -> {
                        onSettingChange_SeekMode()
                    }
                    FragmentConnector.fragment_more_button_updated_link_scroll -> {
                        onSettingChange_LinkScroll()
                    }
                    FragmentConnector.fragment_more_button_updated_tap_jump -> {
                        onSettingChange_TapJump()
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
                        val uriString = PlayerInfoCenter.GET_Media_URI_S_FP()
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
                    FragmentConnector.fragment_more_button_bind_play_view -> reBindPlayerView()
                    //删除自定义封面图
                    FragmentConnector.fragment_more_button_delete_custom_cover -> deleteCustomCover()
                    //立即退出
                    FragmentConnector.fragment_more_button_exit_right_now -> custom_finish()
                    //刷新屏幕常亮状态
                    FragmentConnector.fragment_more_button_update_keep_screen_on -> updateKeepScreenOn()
                }
            }
            //播放列表  Fragment -> Activity  fragment_request_key_play_list_reverse
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_play_list_reverse, context) { _, bundle ->
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
            //媒体信息  Fragment -> Activity  fragment_request_key_media_info_reverse
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_media_info_reverse, context) { _, bundle ->
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
    }




    //播放器监听器
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    playState_playerReady()
                }
                Player.STATE_ENDED -> {
                    playState_playEnd()
                }
                Player.STATE_IDLE -> {
                    //IDLE处理已转移到PlayerInfoCenter的Observable中
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
            showCustomToast("出错了:${error.message}", 3)
        }
    }
    private var state_PlayerListenerAdded: Boolean = false

    //播放器ID观察器
    private fun startExoPlayerIdleObserver() {
        lifecycleScope.launch(Dispatchers.Main){
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableIsIdle.collect { state ->
                    //consoleLog("observableIsIdle: $state")
                    when(state){
                        -1L -> {
                            //播放器从未启动过,不操作

                        }
                        0L -> {
                            //播放器空闲事件
                            onPlayEngineIdle()
                        }
                        else -> {
                            if (state == cache_player_ID){
                                //播放器实例未变更,无操作

                            }else{
                                //播放器重启事件
                                onPlayEnginRestart(state)

                            }
                        }

                    }
                }
            }
        }
    }
    //播放器进入空闲状态
    private fun onPlayEngineIdle() {
        //consoleLog("onPlayEngineIdle")
        //进行收尾工作(移除监听器,移除引用)
        removeExoPlayerListener()
        player = null
        //清除播放项
        onMediaItemCleared()
        showErrorCover("当前未在播放")

    }
    //播放器重新上线
    private fun onPlayEnginRestart(new_ID:Long) {
        //consoleLog("onPlayEnginRestart: $new_ID")

        //移除一次旧的监听器(保底)
        removeExoPlayerListener()

        //重新拿取引用
        player = PlayerSingleton.get_player_ref()
        if (player == null){
            //consoleLog("onPlayEnginRestart: 未拿到播放器引用")
            return
        }else{
            //consoleLog("onPlayEnginRestart: 成功拿到播放器引用")

            //主动检查正在播放的项
            val (ongoing,ongoing_URI) = PlayerSingleton.get_engine_ongoing_URI()
            //consoleLog("onPlayEnginRestart: 正在播放项:ongoing:${ongoing}, ongoing_URI:$ongoing_URI")
            if (ongoing){
                connectCurrentMedia()
                closeErrorCover()
            }else{
                //
                showErrorCover("当前没有正在播放的项")
            }
        }
        //记入缓存
        cache_player_ID = new_ID

        //重新添加监听器(将state_PlayerListenerAdded置于false以强制添加)
        state_PlayerListenerAdded = false
        startExoPlayerListener()
    }


    //连接ExoPlayer(可发起启动)
    private var cache_player_ID = 0L
    private fun connectToExoPlayer() {
        //确保播放器已启动并获得引用
        player = PlayerSingleton.init_player_get_ref()
        //记入缓存
        cache_player_ID = PlayerSingleton.cache_player_instance_id
        //添加播放器事件监听
        startExoPlayerListener()
    }
    private fun startExoPlayerListener() {
        if (state_PlayerListenerAdded) return
        state_PlayerListenerAdded = true
        //consoleLog("startExoPlayerListener")
        player?.addListener(PlayerStateListener)

    }
    private fun removeExoPlayerListener() {
        //consoleLog("removeExoPlayerListener")
        player?.removeListener(PlayerStateListener)
    }
    //设置新媒体项
    private suspend fun setNewMediaItem(URI_U_FP: Uri,file_path:String): Boolean {
        if (state_setting_media) return false
        state_setting_media = true

        //缓存URI为字符串
        val URI_S_FP = URI_U_FP.toString()

        //确保已启动播放器
        withContext(Dispatchers.Main){ connectToExoPlayer() }

        //写入本次Ready来源
        Mark_playerReadyFrom = Mark_playerReadyFrom_setNewItem

        //确认设置新媒体项
        val result = PlayerSingleton.setMediaItem(URI_U_FP, file_path,true)
        when(result) {
            ActivityResultConnector.OBRTV_Engine_SetItemSuccess -> {
                //设置成功
                //consoleLog("setNewMediaItem: 设置成功")

                withContext(Dispatchers.Main){ bindPlayerView() }

                state_setting_media = false

                //返回成功信息
                return true
            }

            ActivityResultConnector.OBRTV_Engine_AlreadyPlayingTargetItem -> {
                //设置失败-目标项已在播放

                //链接当前播放项
                withContext(Dispatchers.Main){ connectCurrentMedia() }

                state_setting_media = false
                return false

            }

            ActivityResultConnector.OBRTV_Engine_RetrieveFailed -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.get_engine_ongoing_URI()
                    if (ongoing) {
                        showCustomToast("媒体解码失败")
                        //链接当前播放项
                        connectCurrentMedia()
                    } else {
                        //设置失败-无法获取目标项信息
                        showErrorCover("媒体解码失败")

                    }
                }

                state_setting_media = false
                //媒体解码失败,结束设置流程
                return false
            }

            ActivityResultConnector.OBRTV_Engine_TypeNotSupport -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.get_engine_ongoing_URI()
                    if (ongoing) {
                        showCustomToast("不支持的媒体类型")
                        //链接当前播放项
                        connectCurrentMedia()
                    } else {
                        //设置失败-媒体类型不支持
                        showErrorCover("不支持的媒体类型")

                    }
                }

                state_setting_media = false
                //不支持的媒体类型,结束设置流程
                return false
            }

            ActivityResultConnector.OBRTV_Engine_SoFrequent -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.get_engine_ongoing_URI()
                    if (ongoing) {
                        showCustomToast("设置过于频繁")
                        //链接当前播放项
                        connectCurrentMedia()
                    } else {
                        //设置失败-过于频繁
                        showErrorCover("设置过于频繁")

                    }
                }

                state_setting_media = false
                //设置过于频繁,结束设置流程
                return false
            }

            ActivityResultConnector.OBRTV_Engine_Locked -> {
                withContext(Dispatchers.Main){
                    showCustomToast("播放器处于锁定窗口期", 3)
                    finish()

                }

                state_setting_media = false
                return false
            }

            else -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.get_engine_ongoing_URI()
                    if (ongoing) {
                        showCustomToast("未知错误")
                        //链接当前播放项
                        connectCurrentMedia()
                    } else {
                        //未知错误
                        showErrorCover("未知错误")

                    }
                }

                state_setting_media = false
                //未知错误,结束设置流程
                return false
            }
        }

    }
    private var state_setting_media = false
    //媒体项变更回调(需升级为观察者观察统一状态)
    private fun onMediaItemChanged(mediaItem: MediaItem?) {
        //consoleLog("onMediaItemChanged: $mediaItem")
        if (mediaItem == null){
            consoleLog("onMediaItemChanged: 失败-媒体项为空")
            return
        }

        //隐藏错误面板
        closeErrorCover()

        //非视频时主动退出页面
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        if (mediaType != MediaType.Video){
            //
            if (mediaType == MediaType.Audio) showCustomToast("已切换到音乐",3)
            //
            custom_finish()
            return
        }


        //重新绑定播放器视图
        bindPlayerView()

        //刷新屏幕常亮状态
        updateKeepScreenOn()
        //刷新视频总长度
        updateTimerWindow()
        //刷新进度条
        update_S_Area_Adapter()
        //刷新按钮
        updateButtonState()


    }

    //媒体项被清除回调
    private fun onMediaItemCleared() {

        //解绑播放区域
        unbindPlayerView()
        //关闭进度条组件
        show_s_area_type(S_Area_Helper.S_AreaType_UNDEFINED)
        //刷新屏幕常亮状态
        updateKeepScreenOn()


        if (PlayerSingleton.state_real_clear){
            showErrorCover("未在播放")
        }else{
            showErrorCover("加载中",hide_buttons = true)
        }

    }

    //播放状态变更回调触发
    private fun isPlayingChanged() {
        if (scrollerDesire_Active) return

        //更新按钮状态
        updateButtonState()
        //更新屏幕常亮状态
        updateKeepScreenOn()
        //更新循环函数状态
        updateLoopFunctionState()

    }

    //更新循环函数状态
    private fun updateLoopFunctionState() {
        if (scrollerDesire_Active) return

        val isPlaying = PlayerSingleton.get_engine_is_playing()
        if (isPlaying){
            start_S_Area_PassiveControl()
            startVideoTimeSync()
        }else{
            stop_S_Area_PassiveControl()
            stopVideoTimeSync()
        }

    }


    //检查文件是否还存在
    private fun detectFileExistState() {
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
                //showCustomToast("文件已不存在")
                //清除当前项
                PlayerSingleton.clearMediaItem()

                showErrorCover("目标文件已不存在")

            }
        }

    }

    //检查是否有媒体正在在播放并获取链接
    private fun isAnyMediaOngoing(): Pair<Boolean, String> {
        //从播放器获取当前媒体状态
        val (ongoing,currentMediaItem) = PlayerSingleton.get_engine_ongoing_URI()

        return if (ongoing){
            val currentMediaUriString = currentMediaItem.toString()
            Pair(true,currentMediaUriString)
        }else{
            Pair(false,"")
        }
    }


    //重新绑定播放器视图+重置位置
    private fun reBindPlayerView() {
        //重新绑定视图
        bindPlayerView()
        //重置位置
        playerView.scaleX = 1f
        playerView.scaleY = 1f
        playerView.pivotX = 0f
        playerView.pivotY = 0f

        //重新注册手势
        registerGestureLayer()

    }




    //确认关闭操作(已不再承担事务清理工作,仅存一个是否保持播放的判断,若无需考虑是否保持播放可直接使用finish())
    @SuppressLint("SourceLockedOrientationActivity")
    private fun exitActivity() {
        exitActivity_showController()
    }
    private fun exitActivity_recOrientation() {
        //退出前是否先转为竖屏
        val switchPortrait = SettingsCenter.GET_PRF_SwitchPortrait_whenExit()
        if (switchPortrait){
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape){
                viewModel.setManual()
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
    private fun exitActivity_showController() {
        if (!viewModel.state_controllerShowing){
            notice("再按一次退出",2000)
            //显示控件
            setControllerVisible()
            viewModel.state_controllerShowing = true
            //延迟振动避免与系统振动冲突
            lifecycleScope.launch{
                delay(75)
                ToolVibrate.vibrate()
            }

        }else{
            //确认退出
            //exitActivity_ensure()

            exitActivity_recOrientation()

        }
    }
    private fun exitActivity_ensure() {
        scroller.stopScroll()

        val needCloseEngin = !SettingsCenter.GET_PRF_EnableMiniView()
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
        custom_finish()

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
                    viewModel.OrientationValue = 1
                } else if (orientation in 81..<100) {
                    viewModel.OrientationValue = 2
                } else if (orientation in 341..<360) {
                    viewModel.OrientationValue = 0
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
                    if (viewModel.currentOrientation == 0) {
                        //从 竖屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (viewModel.OrientationValue == 1) {
                            if (viewModel.Manual && viewModel.LastLandscapeOrientation == 1) return
                            viewModel.currentOrientation = 1
                            viewModel.LastLandscapeOrientation = 1
                            viewModel.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 竖屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (viewModel.OrientationValue == 2) {
                            if (viewModel.Manual && viewModel.LastLandscapeOrientation == 2) return
                            viewModel.currentOrientation = 2
                            viewModel.LastLandscapeOrientation = 2
                            viewModel.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                    }
                    //当前为正向横屏
                    else if (viewModel.currentOrientation == 1) {
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (viewModel.OrientationValue == 2) {
                            //更改状态并发起旋转
                            viewModel.currentOrientation = 2
                            viewModel.LastLandscapeOrientation = 2
                            viewModel.setAuto()
                            setOrientation_REVERSE_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (viewModel.OrientationValue == 0) {
                            if (viewModel.Manual) return
                            viewModel.currentOrientation = 0
                            viewModel.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                    //当前为反向横屏
                    else if (viewModel.currentOrientation == 2) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_LANDSCAPE
                        if (viewModel.OrientationValue == 1) {
                            //更改状态并发起旋转
                            viewModel.currentOrientation = 1
                            viewModel.LastLandscapeOrientation = 1
                            viewModel.setAuto()
                            setOrientation_LANDSCAPE()
                        }
                        //从 反向横屏 转动到 竖屏 ORIENTATION_PORTRAIT
                        else if (viewModel.OrientationValue == 0) {
                            if (viewModel.Manual) return
                            viewModel.currentOrientation = 0
                            viewModel.setAuto()
                            setOrientation_PORTRAIT()
                        }
                    }
                }
                //自动旋转关闭
                else if (rotationSetting == 0) {
                    if (!viewModel.FromManualPortrait) {
                        //从 反向横屏 转动到 正向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        if (viewModel.OrientationValue == 1) {
                            //更改状态并发起旋转
                            setOrientation_LANDSCAPE()
                        }
                        //从 正向横屏 转动到 反向横屏 ORIENTATION_REVERSE_LANDSCAPE
                        else if (viewModel.OrientationValue == 2) {
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
    private fun startOrientationListener() {
        //未开启旋转监听器
        if (!SettingsCenter.GET_PREFS_EnableOrientationListener()) return
        //已有一例监听器
        if (state_orientationListenerEnabled) return
        //确保监听器已注册
        if (!state_orientationListenerInitialized) setupOrientationListener()

        orientationListener.enable()
        state_orientationListenerEnabled = true
    }
    private fun stopOrientationListener() {
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

                viewModel.state_isFinishing = true
            }else{

                viewModel.state_isFinishing = false

                //是否后台播放
                PlayerSingleton.startBackgroundPlay()
            }
        }

        //关闭视频控制
        stopVideoSeek()
        stopVideoSmartScroll()
        stopVideoTimeSync()
        stop_S_Area_PassiveControl()
        //关闭旋转监听器
        stopOrientationListener()

        //关闭隐藏控件倒计时
        closeIdleTimer()


    }

    override fun onResume() {
        super.onResume()

        //判断是否继续播放
        if (viewModel.state_isFinishing){
            //consoleLog("onResume 来自活动销毁")
        }else{
            //consoleLog("onResume 来自活动暂退桌面")
            //是否后台播放
            PlayerSingleton.stopBackgroundPlay()
        }

        updateControllers()

        //更新屏幕常亮状态
        updateKeepScreenOn()

        //开启隐藏控件倒计时
        startIdleTimer()

        //关闭小窗
        stopFloatingWindow()

        //检查文件是否存在
        detectFileExistState()


        //状态机(经典代码,别删除)
        /*
        //区分onResume原因：
        if (viewModel.state_onStopDecider_Running){
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
            if (viewModel.state_onStop_ByReBuild){
                //开启视频控件
                startScrollerSync()
                startVideoTimeSync()
            }
            //首次启动 暂无动作 viewModel.state_onStop_ByRealExit
            //活动暂退桌面：小窗模式在这里包含
            if (viewModel.state_onStop_ByLossFocus){
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
            viewModel.set_onStop_all_reset()
        }

         */
    }

    override fun onDestroy() {
        super.onDestroy()

        //停止UI端操作
        scroller.stopScroll()
        stopVideoSmartScroll()
        stopVideoSeek()
        stop_S_Area_PassiveControl()
        stopVideoTimeSync()

        //关闭小窗
        stopFloatingWindow(true)

        //解绑播放器视图
        playerView.player = null

        //关闭播放器状态监听+丢弃引用
        removeExoPlayerListener()
        player = null
        cache_player_ID = 0L

        //关闭本地监听器
        stopOrientationListener()

        //关闭隐藏控件倒计时
        closeIdleTimer()

    }

    private var state_EnterAnimationCompleted = false
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        state_EnterAnimationCompleted = true

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                //跟随写入缓存
                volumeManager_currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)


                false
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //consoleLog("onSaveInstanceState")

    }
    //用户交互监听器
    override fun onUserInteraction() {
        super.onUserInteraction()
        //consoleLog("onUserInteraction")

        //开启隐藏控件倒计时
        startIdleTimer()
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
                //修改横屏状态缓存
                isLandscape = true
                //更新屏幕参数
                updateScreenParameters()
                //同步滚动位置
                syncScrollTask_Core_smoothSlowly_Compute(-1L,true)

            }
            //切换至竖屏
            Configuration.ORIENTATION_PORTRAIT -> {
                //修改横屏状态缓存
                isLandscape = false
                //更新屏幕参数
                updateScreenParameters()
                //同步滚动位置
                syncScrollTask_Core_smoothSlowly_Compute(-1L,true)

            }
        }
    }
    //重写finish()以应用动画(活动内不应主动调用finish()而是使用exitActivity()入口)
    private var useSlideOutAnim = true
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        //判断是否使用活动层级自定义收起动画

        //判断是否使用收起动画 if (useSlideOutAnim)

        //已改为一律使用收起动画

        overridePendingTransition(R.anim.slide_just_appear, R.anim.slide_out_vertical)



    }
    //自定义退出方式
    private fun custom_finish() {
        //可下拉模式
        /*
        root.animate()
            .translationY(2500f)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { finish() }
            .duration = 200

         */
        //不可下拉模式
        finish()
    }



    //更新屏幕常亮状态
    private fun updateKeepScreenOn() {
        val keepOn = SettingsCenter.GET_PRF_KeepScreenOn()
        if (keepOn){
            root.keepScreenOn = PlayerSingleton.get_engine_is_playing()
        }else{
            root.keepScreenOn = false
        }
    }

    //启动更多操作面板
    private fun startMoreButtonFragment() {
        onFragmentStartPrepare()

        PlayerFragmentMoreButton.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_more_button)
    }
    //启动均衡器面板
    private fun startEqualizerFragment() {
        onFragmentStartPrepare()

        PlayerFragmentEqualizer.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_equalizer)
    }
    //启动媒体信息面板
    private fun startMediaIndoFragment() {
        onFragmentStartPrepare()

        PlayerFragmentMediaInfo.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_media_info)
    }
    //启动播放列表面板
    private fun startPlayListFragment() {
        onFragmentStartPrepare()

        ListManagerFragment.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_play_list)
    }
    //关闭所有DialogFragment
    private fun closeAllDialogFragments() {
        val manager = supportFragmentManager
        val fragments = manager.fragments
        fragments.forEach { fragment ->
            if (fragment is DialogFragment && fragment.isVisible) {
                fragment.dismiss()
            }
        }
    }
    //检查是否有Dialog Fragment处于开启状态
    private fun checkDialogFragmentOpen(): Boolean {
        val manager = supportFragmentManager
        val fragments = manager.fragments
        for (fragment in fragments) {
            if (fragment is DialogFragment && fragment.isVisible) {
                return true
            }
        }
        return false
    }
    //面板弹出通用操作
    private fun onFragmentStartPrepare() {

    }

    //绑定播放器视图
    private fun bindPlayerView() {
        playerView.player = null
        playerView.player = player
    }
    //解绑播放器视图
    private fun unbindPlayerView() {
        playerView.player = null
    }

    //修改方向监听器状态
    private fun updateOrientationListener() {
        //读取设置
        val enable = SettingsCenter.GET_PREFS_EnableOrientationListener()
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
    private fun onFragmentOpen() {
        //增加计数
        fragment_count += 1
        //consoleLog("计数增加。当前Fragment计数：$fragment_count")
        //仅在数量为0时才执行
        if (fragment_count > 0){
            //关闭被控组件
            stop_S_Area_PassiveControl()
            stopVideoTimeSync()
            //播放区域移移动
            moveArea_playView_Up()
        }
    }
    private fun onFragmentClose() {
        //减少计数
        if (fragment_count <= 0){
            fragment_count = 0
            return
        }
        fragment_count -= 1
        //consoleLog("计数减少。当前Fragment计数：$fragment_count")
        if (fragment_count <= 0){
            //开启被控组件
            start_S_Area_PassiveControl()
            startVideoTimeSync()
            //播放区域移移动
            moveArea_playView_Down()
        }
    }

    //界面更新
    private fun updateControllers() {
        //通用
        updateButtonState()


        val isPlaying = player?.isPlaying ?: false
        if (isPlaying){
            start_S_Area_PassiveControl()
            startVideoTimeSync()
        }else{
            stop_S_Area_PassiveControl()
            stopVideoTimeSync()
        }
    }


    //音量管理与提示 volumeManager
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private fun volumeDetectCompute() {
        //缓存当前音量信息
        volumeManager_maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeManager_currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        //计算音量切换步长
        viewModel.volumeManager_changeStep = 750 / volumeManager_maxVolume

        //音量未开启时显示提示
        if (volumeManager_currentVolume == 0 && !viewModel.volumeManager_zeroVolume_noticed) {
            viewModel.volumeManager_zeroVolume_noticed = true
            notice("当前未开启声音", 1000)
        }

    }
    //设备音量环境
    private var volumeManager_maxVolume = 0
    private var volumeManager_currentVolume = 0

    //亮度控制 brightnessManager
    private fun brightnessDetectCompute() {
        val windowInfo = window.attributes
        if (!viewModel.brightManager_state_brightness_changed) {
            if (windowInfo.screenBrightness == -1f) {
                viewModel.brightManager_current_brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f

            }
        }else{
            //重建时应用之前的亮度

            windowInfo.screenBrightness = viewModel.brightManager_current_brightness
            window.attributes = windowInfo
        }
    }

    //设置项修改封装函数
    private fun onSettingChange_SeekMode() {
        //读取viewModel中的最新值
        val enable = viewModel.PREFS_AlwaysSeek
        //执行对应操作
        if (enable){
            notice("寻帧模式已设置为常规寻帧", 3000)
            //停止滚动已尽快结束AlwaysSeek进程
            scroller.stopScroll()
        }else{
            notice("寻帧模式已设置为倍速模拟", 3000)
        }
    }
    private fun onSettingChange_LinkScroll() {
        //读取viewModel中的最新值
        val enable = viewModel.PREFS_LinkScroll
        //执行对应操作
        if (enable){
            viewModel.PREFS_LinkScroll = true
            notice("已将进度条与视频进度同步", 3000)
        }else{
            scroller.stopScroll()
            start_S_Area_PassiveControl()
            stopVideoSeek()
            stop_S_Area_PassiveControl()
            notice("已关闭链接滚动条与视频进度", 3000)
        }
    }
    private fun onSettingChange_TapJump() {
        //读取viewModel中的最新值
        val enable = viewModel.PREFS_TapJump
        //执行对应操作
        if (enable){
            notice("已开启单击跳转", 3000)
        }else{
            notice("已关闭单击跳转", 3000)
        }
    }
    //清除进度条截图
    private fun clearScrollerFrames() {
        //获取当前视频ID
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        //删除进度条截图
        val success = ScrollerHelper.deleteScrollerFrame(this, NUM_ID)
        //显示反馈
        if (success){
            showCustomToast("删除成功", 3)
            //删除成功时清除scroller adapter内的缓存
            scrollerAdapter?.clearBitmapCache()
        }else{
            showCustomToast("删除失败", 3)
        }
    }
    //视频区域抬高动画
    private var isPlayView_Up = false
    private fun moveArea_playView_Down() {
        //恢复动作不需要限定在竖屏下
        //if (isLandscape) return

        //是否开启视频区域抬高动画
        val isEnable = viewModel.PRF_Cache_EnablePlayAreaMove
        if (isEnable){
            isPlayView_Up = false

            playerView.animate()
                .translationY(0f)
                .setInterpolator(DecelerateInterpolator(3f))
                .setDuration(500)
                .start()

        }

    }
    private fun moveArea_playView_Up() {
        //仅在竖屏下有效
        if (isLandscape) return

        //是否开启视频区域抬高动画
        val isEnable = viewModel.PRF_Cache_EnablePlayAreaMove
        if (isEnable){
            isPlayView_Up = true
            //动画插值器
            val interpolator = PathInterpolatorCompat.create(
                0.4f, 0.0f,
                0.2f, 1.0f
            )
            //计算高度
            val moveDistance = viewModel.PRF_Cache_EnablePlayAreaMove_Distance

            playerView.animate()
                .translationY(-(moveDistance))
                .setInterpolator(interpolator)
                .setDuration(450)
                .start()

        }

    }
    private fun ensure_moveArea_place() {
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
    private fun captureScreenShot() {
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

            if (viewModel.wasPlaying){ player?.play() }

        }
        notice("请稍等", 3000)
        viewModel.wasPlaying = player?.isPlaying ?: false
        player?.pause()
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
    private fun updateCoverFrame_captureCurrentFrame(media_api_id: Long) {
        fun handleSuccess(bitmap: Bitmap) {
            //保存图片
            ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap_Custom(
                this@PlayerActivityNeo,
                MediaType.Video,
                media_api_id,
                bitmap
            )

            //恢复播放状态
            if (viewModel.wasPlaying){ player?.play() }

            //获取当前文件路径
            val file_path = PlayerInfoCenter.GET_Media_FilePath()

            //发布完成消息
            updateCoverFrame_publishMessage(file_path, media_api_id)

            showCustomToast("截取封面完成", 3)

        }
        //记录原本的播放状态
        viewModel.wasPlaying = player?.isPlaying ?: false
        player?.pause()
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
    private fun updateCoverFrame_useDefaultCover(media_api_id: Long) {
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
    private fun updateCoverFrame_publishMessage(file_path:String, media_api_id: Long) {
        ConnectCenter.setCoverFrameUpdateEvent_targetFileInfo(file_path, media_api_id)
        ConnectCenter.setState_connector(ConnectCenter.connector_event_cover_frame_update)
    }
    private fun deleteCustomCover() {
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
    //空闲定时器
    private var IDLE_Timer: CountDownTimer? = null
    private val IDLE_MS = 5_000L
    private fun startIdleTimer() {
        IDLE_Timer?.cancel()
        IDLE_Timer = object : CountDownTimer(IDLE_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() { idleTimeout() }
        }.start()
    }
    private fun closeIdleTimer() {
        IDLE_Timer?.cancel()
    }
    private fun idleTimeout() {
        if (isLandscape){
            if (!scrollerDesire_Active) setControllerInvisible()
        }else{
            if (!isDarkTheme){
                if (viewModel.PRF_Cache_EnableAutoHideController_whenPortrait){
                    if (!scrollerDesire_Active) setControllerInvisible()
                }
            }
        }
    }
    //启动和关闭小窗
    private var state_FloatingWindow_Active = false
    private fun startFloatingWindow() {
        //检查悬浮窗权限是否开启
        fun checkOverlayPermission(): Boolean {
            return Settings.canDrawOverlays(this)
        }
        if (!checkOverlayPermission()){
            showCustomToast("请先开启悬浮窗权限", 3)
            return
        }
        //通过检测，确认启动小窗
        else{
            if (state_FloatingWindow_Active) return
            state_FloatingWindow_Active = true

            //启动小窗服务
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val intentFloatingWindow = Intent(applicationContext, FloatingWindowService::class.java)
            intentFloatingWindow.putExtra("VIDEO_SIZE_WIDTH", videoSizeWidth)
            intentFloatingWindow.putExtra("VIDEO_SIZE_HEIGHT", videoSizeHeight)
            intentFloatingWindow.putExtra("SCREEN_WIDTH", screenWidth)
            intentFloatingWindow.putExtra("state_PlayerType", 1)   //该传入值需要区分页面类型 flag_page_type
            startService(intentFloatingWindow)

            //主动返回系统桌面
            val intentHomeLauncher = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intentHomeLauncher)
        }
    }
    private fun stopFloatingWindow(from_destroy: Boolean = false) {
        if (!state_FloatingWindow_Active) return
        state_FloatingWindow_Active = false

        stopService(Intent(applicationContext, FloatingWindowService::class.java))

        if (!from_destroy){
            //不是来自活动销毁时，重新绑定播放器视图
            bindPlayerView()
        }
    }
    //切换横屏
    private fun ButtonChangeOrientation(flag_short_or_long: String) {
        //自动旋转关闭
        if (rotationSetting == 0){
            //当前为竖屏
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    viewModel.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (viewModel.OrientationValue == 1){
                    viewModel.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
                else if (viewModel.OrientationValue == 2){
                    viewModel.FromManualPortrait = false
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    viewModel.FromManualPortrait = false
                    setOrientation_LANDSCAPE()
                }
            }
            //当前为横屏
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                viewModel.FromManualPortrait = true
                setOrientation_PORTRAIT()
            }
        }
        //自动旋转开启
        else if (rotationSetting == 1){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                if (flag_short_or_long == "long"){
                    viewModel.FromManualPortrait = true
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else if (viewModel.OrientationValue == 1){
                    viewModel.currentOrientation = 1
                    viewModel.LastLandscapeOrientation = 1
                    viewModel.setManual()
                    setOrientation_LANDSCAPE()
                }
                else if (viewModel.OrientationValue == 2){
                    viewModel.currentOrientation = 2
                    viewModel.LastLandscapeOrientation = 2
                    viewModel.setManual()
                    setOrientation_REVERSE_LANDSCAPE()
                }
                else{
                    viewModel.currentOrientation = 1
                    viewModel.LastLandscapeOrientation = 1
                    viewModel.setManual()
                    setOrientation_LANDSCAPE()
                }
            }
            else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                viewModel.currentOrientation = 0
                viewModel.setManual()
                setOrientation_PORTRAIT()
            }
        }
    }
    private var switchLandscape_downMillis = 0L //按下时间
    private var switchLandscape_upMillis = 0L   //抬起时间
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setOrientation_PORTRAIT() {
        scroller.stopScroll()
        viewModel.onOrientationChanging = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    private fun setOrientation_LANDSCAPE() {
        scroller.stopScroll()
        viewModel.onOrientationChanging = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    private fun setOrientation_REVERSE_LANDSCAPE() {
        scroller.stopScroll()
        viewModel.onOrientationChanging = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    }
    //返回手势监听 (可升级)
    private fun registerOnBackPressListener() {
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
    //注册播放区域手势
    private var state_playView_singleTap = false
    private var state_playView_longPress = false
    @SuppressLint("ClickableViewAccessibility")
    private fun registerGestureLayer() {
        lifecycleScope.launch(Dispatchers.Main) {

            delay(500)

            //播放区域点击事件
            //<editor-fold desc="点击事件变量">
            //手指计数
            var touchFingerCount = 0

            var originalDistance = 0f
            var distanceGap = 0f
            var center0x = 0f
            var center0y = 0f
            var center1x = 0f
            var center1y = 0f
            var originalScale = 1f
            var scale = 1.0
            var definiteScale = 1.0f
            var center2x = 0f
            var center2y = 0f
            var center0pivoted = false
            var finger1x = 0f
            var finger1y = 0f
            var finger2x = 0f
            var finger2y = 0f

            //滑动结果标记
            var scroll_result = 0   // 1:退出活动 2:恢复顶部 3:恢复播放区域


            //点击区域
            var touchArea = 0   //1:左区域 2:右区域 3:中心区域
            //纵向滑动阶梯起始坐标
            var e_y_stage = 0f
            //执行锁(只能执行一次的触发点使用)
            var execute_lock = false
            var execute_lock_addon = false
            var execute_lock_addon_filter = false
            var execute_lock_addon_scroll_end_trigger = false
            //</editor-fold desc="点击事件">
            //播放区域
            val gestureLayer = findViewById<View>(R.id.playerTouchPad)
            //播放区域点击事件
            val gestureDetectorPlayArea = GestureDetector(context,object:GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        //控制播放
                        if (player?.isPlaying == true) {
                            pausePlay()
                            stop_S_Area_PassiveControl()
                            notice("暂停播放", 1000)
                            updateButtonState()
                        } else {
                            if (PlayerSingleton.get_state_playEnd()) {
                                //PlayerSingleton.remove_state_playEnd() 不消耗状态
                                //寻回起始位置
                                player?.seekTo(0L)
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
                        if (touchFingerCount == 2) {
                            return true
                        }
                        //触发控件显示变更
                        changeBackgroundColor()
                        //确保播放区域在普通位置
                        ensure_moveArea_place()

                        return true
                    }
                    override fun onLongPress(e: MotionEvent) {
                        if (touchFingerCount == 2) return
                        if (player?.isPlaying == false) {
                            return
                        }
                        currentSpeed = player?.playbackParameters?.speed ?: 1.0f
                        player?.setPlaybackSpeed(currentSpeed * 2.0f)
                        notice("倍速播放中(${currentSpeed * 2.0f}x)", 114514)
                        setControllerInvisibleNoAnimation()
                        state_playView_longPress = true
                        ToolVibrate.vibrate()
                        super.onLongPress(e)
                    }
                    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float):Boolean {

                        when (touchArea){
                            //左侧区域(控制亮度)(消费距离间隔,不使用执行锁)
                            1 -> {
                                //计算阶梯坐标差值
                                val gap = (e2.rawY - e_y_stage)
                                consoleLog("gap:$gap")
                                //执行亮度修改
                                when {
                                    gap < -10 -> {
                                        //刷新参照值
                                        e_y_stage = e2.rawY
                                        //计算目标亮度(拿缓存计算)
                                        val target = (viewModel.brightManager_current_brightness + 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                                        if (target in 0.0..1.0) {
                                            //打包为 windowInfo 并应用给系统
                                            val windowInfo = window.attributes
                                            windowInfo.screenBrightness = target
                                            window.attributes = windowInfo
                                            //同步写回缓存
                                            viewModel.brightManager_current_brightness = target

                                            ToolVibrate.vibrate()
                                            notice("亮度 +1 (${(target * 100).toInt()}/100)", 1000)
                                        }else{
                                            ToolVibrate.vibrate()
                                            notice("亮度已到上限", 1000)
                                        }
                                    }
                                    gap > 10 -> {
                                        //刷新参照值
                                        e_y_stage = e2.rawY
                                        //计算目标亮度(拿缓存计算)
                                        val target = (viewModel.brightManager_current_brightness - 0.01f).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                                        if (target in 0.0..1.0) {
                                            //打包为 windowInfo 并应用给系统
                                            val windowInfo = window.attributes
                                            windowInfo.screenBrightness = target
                                            window.attributes = windowInfo
                                            //同步写回缓存
                                            viewModel.brightManager_current_brightness = target

                                            ToolVibrate.vibrate()
                                            notice("亮度 -1 (${(target * 100).toInt()}/100)", 1000)
                                        }else{

                                            ToolVibrate.vibrate()
                                            notice("亮度已到下限", 1000)
                                        }
                                    }
                                }
                            }
                            //右侧区域(控制音量)(消费距离间隔,不使用执行锁)
                            2 -> {
                                //计算阶梯坐标差值
                                val gap = (e2.rawY - e_y_stage)
                                //执行音量修改
                                when {
                                    gap > viewModel.volumeManager_changeStep -> {
                                        //刷新参照值
                                        e_y_stage = e2.rawY
                                        //计算目标音量
                                        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1
                                        //决定是否应用
                                        if (volume >= 0) {
                                            //执行音量应用
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)

                                            ToolVibrate.vibrate()
                                            notice("音量 -1 ($volume/$volumeManager_maxVolume)", 1000)
                                        }else{

                                            ToolVibrate.vibrate()
                                            notice("音量已到最低", 1000)
                                        }
                                    }
                                    gap < -viewModel.volumeManager_changeStep -> {
                                        //刷新参照值
                                        e_y_stage = e2.rawY
                                        //计算目标音量
                                        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1
                                        //执行音量设置
                                        if (volume <= volumeManager_maxVolume){
                                            if (SystemListener.get_state_headset_on()){
                                                if (volume <= (volumeManager_maxVolume * 0.6).toInt()) {
                                                    //执行音量应用
                                                    audioManager.setStreamVolume(
                                                        AudioManager.STREAM_MUSIC,
                                                        volume,
                                                        0
                                                    )

                                                    ToolVibrate.vibrate()
                                                    notice("音量 +1 ($volume/$volumeManager_maxVolume)", 1000)
                                                }else{

                                                    ToolVibrate.vibrate()
                                                    notice("佩戴耳机时,音量最高为${(volumeManager_maxVolume * 0.6).toInt()},使用音量键继续增大", 1000)
                                                }
                                            }else{
                                                //执行音量应用
                                                audioManager.setStreamVolume(
                                                    AudioManager.STREAM_MUSIC,
                                                    volume,
                                                    0
                                                )

                                                ToolVibrate.vibrate()
                                                notice("音量 +1 ($volume/$volumeManager_maxVolume)", 1000)
                                            }
                                        }else{

                                            ToolVibrate.vibrate()
                                            notice("音量已到最高", 1000)
                                        }
                                    }
                                }
                            }
                            //中间区域(扩展退出和扩展面板弹出)(消费总距离,需要使用执行锁)
                            3 -> {
                                //计算阶梯坐标差值
                                val gap = (e2.rawY - e_y_stage)

                                //判断操作
                                when {
                                    //下滑退出
                                    gap > 0 -> {
                                        if (execute_lock) return false

                                        //关闭多余任务
                                        onScrollExitAnimStart()

                                        when {
                                            gap < viewModel.value_scrollDownExitDistance -> {
                                                //标记为需要恢复顶部
                                                scroll_result = 2
                                            }
                                            gap >= viewModel.value_scrollDownExitDistance -> {
                                                if (!execute_lock_addon){
                                                    execute_lock_addon = true
                                                    ToolVibrate.vibrate()
                                                }

                                                //标记为需要退出
                                                scroll_result = 1

                                                execute_lock_addon_scroll_end_trigger = true

                                                /*
                                                root_secondary.animate()
                                                    .translationY(2500f)
                                                    .alpha(0f)
                                                    .setInterpolator(LinearInterpolator())
                                                    .duration = 100

                                                 */


                                                //直接退出
                                                custom_finish()

                                            }
                                        }

                                    }
                                    //上滑打开扩展面板
                                    gap < -0 -> {
                                        //判断是否需要执行
                                        if (execute_lock) return false

                                        when {
                                            gap > -viewModel.value_scrollDownExitDistance -> {
                                                //标记为需要恢复播放区域位置
                                                scroll_result = 3
                                            }
                                            gap <= -viewModel.value_scrollDownExitDistance -> {
                                                //打开执行锁
                                                execute_lock = true
                                                execute_lock_addon_scroll_end_trigger = true


                                                //标记为不需要恢复播放区域位置
                                                scroll_result = if (isLandscape) {
                                                    playerView.animate()
                                                        .translationY(0f)
                                                        .setInterpolator(DecelerateInterpolator())
                                                        .duration = 300


                                                    3
                                                } else {

                                                    0
                                                }


                                                //执行打开扩展面板
                                                ToolVibrate.vibrate()
                                                startMoreButtonFragment()

                                            }
                                        }

                                    }
                                }

                            }
                        }

                        return super.onScroll(e1, e2, distanceX, distanceY)
                    }
                } )
            gestureLayer.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        //重置所有状态
                        fun reset(){
                            //记录手指计数
                            touchFingerCount = 1
                            //重置执行锁
                            execute_lock = false
                            execute_lock_addon = false
                            execute_lock_addon_filter = false
                            execute_lock_addon_scroll_end_trigger = false

                        }
                        reset()


                        //记录1指初始坐标
                        finger1x = event.x
                        finger1y = event.y

                        //屏蔽纵向误触区域
                        if (finger1y < display_screen_height_pixels * 0.05 || finger1y > display_screen_height_pixels * 0.9){
                            return@setOnTouchListener false
                        }


                        //记录纵向阶梯起始坐标
                        e_y_stage = event.y


                        //判断点击区域
                        touchArea = when {
                            (finger1x < display_screen_width_pixels * 0.2) -> {
                                1
                            }

                            (finger1x > display_screen_width_pixels * 0.8) -> {
                                2
                            }

                            else -> {
                                3
                            }
                        }


                        //传递
                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_UP -> {
                        //重置所有状态
                        fun reset(){
                            //重置手指计数
                            touchFingerCount = 0
                            //重置点击区域标记
                            touchArea = 0
                            //重置执行锁
                            execute_lock = false
                            execute_lock_addon = false
                            execute_lock_addon_filter = false
                            execute_lock_addon_scroll_end_trigger = false

                            center0x = playerView.pivotX
                            center0y = playerView.pivotY
                            playerView.pivotX = center0x
                            playerView.pivotY = center0y
                            originalScale = definiteScale
                        }
                        reset()

                        //长按
                        if (state_playView_longPress) {
                            state_playView_longPress = false
                            player?.setPlaybackSpeed(currentSpeed)


                            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
                            NoticeCard.visibility = View.GONE
                        }

                        fun root_secondary_scroll_back(){
                            root_secondary.animate()
                                .translationY(0f)
                                .setInterpolator( DecelerateInterpolator(2f) )
                                .withEndAction { onScrollExitAnimTraceEnd() }
                                .duration = 300
                        }
                        fun playerView_scroll_back(){
                            playerView.animate()
                                .translationY(0f)
                                .setInterpolator( DecelerateInterpolator(2f) )
                                .duration = 300
                        }
                        //滚动结果
                        when (scroll_result){
                            0 -> {
                                //未触发滚动时做保底检查
                                //check_view_position()
                            }
                            1 -> {
                                custom_finish()

                            }
                            2 -> {
                                root_secondary_scroll_back()
                            }
                            3 -> {
                                playerView_scroll_back()
                            }
                        }


                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        //记录多指计数
                        touchFingerCount = 2

                        val ptrIndex = event.actionIndex

                        finger2x = event.getX(ptrIndex)
                        finger2y = event.getY(ptrIndex)
                        if (event.pointerCount == 2){
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
                            touchFingerCount = 1
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        when (touchFingerCount) {
                            1 -> {
                                if (!state_playView_longPress){
                                    //处理上下拖动
                                    if (!execute_lock_addon_scroll_end_trigger && touchArea == 3){
                                        //计算移动量
                                        val moveY = event.rawY - finger1y

                                        if (moveY.absoluteValue >= 30){
                                            when {
                                                //下滑
                                                moveY > 0 -> {
                                                    //移动视图区域
                                                    if (!viewModel.PRF_Cache_DisableViewFollowing) {
                                                        root_secondary.translationY = (moveY - 30) * 3
                                                    }else{
                                                        //notice("继续下拉关闭播放页",300)
                                                    }

                                                }
                                                //上滑
                                                moveY < 0 -> {
                                                    //移动视图区域
                                                    if (!viewModel.PRF_Cache_DisableViewFollowing) {
                                                        playerView.translationY = moveY + 30
                                                    }else{
                                                       // notice("继续上滑打开选项面板",300)
                                                    }


                                                }
                                            }
                                        }

                                    }

                                }

                                //gestureDetector精度似乎不够
                                gestureDetectorPlayArea.onTouchEvent(event)

                            }
                            2 -> {
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
                        }

                    }
                }
                onTouchEvent(event)
            }
        }

    }
    //下拉关闭跟随
    private fun onScrollExitAnimStart() {
        //关闭所有任务
        stopVideoTimeSync()
        stopVideoSmartScroll()
        stopVideoSeek()
        stop_S_Area_PassiveControl()
        scroller.stopScroll()
    }
    private fun onScrollExitAnimTraceEnd() {
        //重开所有任务
        startVideoTimeSync()
        start_S_Area_PassiveControl()
        //平滑滚动进度条
        syncScrollTask_Core_smoothSlowly_Compute(-1L,true)

    }

    //刷新时间显示窗口
    private fun updateTimeStampWindow() {
        //触发密度控制
        onScroll_currentMillis = System.currentTimeMillis()
        if (onScroll_currentMillis - scroller_updateTimerStamp_lastMillis < value_timeStamp_updateGapMs) return
        scroller_updateTimerStamp_lastMillis = onScroll_currentMillis

        //仅在开启链接滚动时刷新时间
        if (viewModel.PREFS_LinkScroll) {

            //计算对应时间
            onScroll_scrollPercent = scroller.computeHorizontalScrollOffset().toFloat() / scroller.computeHorizontalScrollRange()
            onScroll_seekToMs = (onScroll_scrollPercent * PlayerInfoCenter.GET_Media_Duration()).toLong()

            //刷新时间显示
            controller_timer_current.text = FormatTime_onlyNum(onScroll_seekToMs)

        }
    }
    private var onScroll_currentMillis = 0L
    private var onScroll_scrollPercent = 0f
    private var onScroll_seekToMs = 0L
    //单次点击位置计算
    private fun postSingleTapSeek(e_x: Float) {
        //根据百分比计算具体跳转视频时间点
        val duration = player?.duration ?: -1L
        if (duration <= 0) return

        val totalContentWidth = scroller.computeHorizontalScrollRange()
        val scrolled = scroller.computeHorizontalScrollOffset()
        val leftPadding = scroller.paddingLeft
        val xInContent = e_x + scrolled - leftPadding
        if (totalContentWidth <= 0) return
        val percent = xInContent / totalContentWidth
        val seekToMs = (percent * duration).toLong().coerceIn(0, duration)
        //验证目标位置是否有效
        if (seekToMs !in 1..<duration) return


        //设置为寻找精确帧
        setSeekParameter_useSync(0)
        //记录原播放状态
        playState_singleTap_wasPlaying = player?.isPlaying ?: false

        //发送跳转命令
        seekTo_Core(seekToMs, Mark_playerReadyFrom_SingleTapSeek)
        //平滑滚动到目标位置
        syncScrollTask_Core_smoothSlowly_Compute(seekToMs, true)

        //发布通知
        notice("跳转至${FormatTime_withChar(seekToMs)}", 1000)
    }
    //跑完一个完整滚动事件
    private fun onScrollOnceComplete() {
        //清理状态
        clearScrollerState()

        //判断是否继续播放或保持暂停
        if (playState_scroller_wasPlaying) continuePlay()

        //恢复音量
        if (PlayerSingleton.get_engine_volume_actual() == 0f) PlayerSingleton.rec_engine_volume()

        //重置寻帧偏好
        setSeekParameter_useSync(1)
        //清除playEnd状态
        PlayerSingleton.remove_state_playEnd()

    }

    //seekParameters管理
    private var seekParameter_useSync = -1 // 0 = 使用精确帧 , 1 = 使用同步帧
    private fun setSeekParameter_useSync(target: Int,force:Boolean=false) {
        if (force){
            when (target){
                //使用精确帧
                0 -> {
                    if (seekParameter_useSync == 0) return

                    //consoleLog("设置为寻找精确帧")
                    //设置为寻找精确帧
                    player?.setSeekParameters(SeekParameters.EXACT)
                    seekParameter_useSync = 0

                }
                //使用同步帧
                1 -> {
                    if (seekParameter_useSync == 1) return

                    //consoleLog("设置为寻找关键帧")
                    //设置为寻找关键帧
                    player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    seekParameter_useSync = 1
                }
            }
        }else{
            if (!viewModel.PRF_Cache_SyncFrame_Dynamic) return

            if (seekParameter_useSync == -1){
                //从player读取当前seekParameters
                val useSync = player?.seekParameters == SeekParameters.CLOSEST_SYNC
                seekParameter_useSync = if (useSync) 1 else 0
            }
            when (target){
                //使用精确帧
                0 -> {
                    if (seekParameter_useSync == 0) return

                    //consoleLog("设置为寻找精确帧")
                    //设置为寻找精确帧
                    player?.setSeekParameters(SeekParameters.EXACT)
                    seekParameter_useSync = 0

                }
                //使用同步帧
                1 -> {
                    if (seekParameter_useSync == 1) return

                    //consoleLog("设置为寻找关键帧")
                    //设置为寻找关键帧
                    player?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    seekParameter_useSync = 1
                }
            }

        }

    }

    //进度条状态
    private var scrollerState_DRAGGING = false
    private var scrollerState_SETTLING = false
    private var scrollerState_IDLE = true
    private var scrollerTouchState_ACTION_DOWN = false
    private var scrollerMotionState_Forward = true
    private var scrollerDesire_Active = false //主动被动状态:触摸立即进入true,直到下一次自然停止
    private fun clearScrollerState() {
        //consoleLog("clearScrollerState")

        scrollerState_DRAGGING = false
        scrollerState_SETTLING = false
        scrollerTouchState_ACTION_DOWN = false
        scrollerDesire_Active = false

        Mark_playerReadyFrom = Undefined

        //posted_seek_count = 0
        //processed_seek_count = 0

        scrollerWasPlayingState_recorded = false

    } //重置为未触摸过的状态
    //状态playerReady
    private fun playState_playerReady() {
        //检验来源
        when(Mark_playerReadyFrom){
            Mark_playerReadyFrom_MidSectionSeek -> {

                //修改状态
                isSeekReady = true
            }
            Mark_playerReadyFrom_TailSeek -> {
                //一个滚动事件完整跑完
                onScrollOnceComplete()

                //修改状态
                isSeekReady = true
            }
            Mark_playerReadyFrom_SingleTapSeek -> {
                //
                syncScrollTask_Core_Compute()

                //恢复播放状态
                if (playState_singleTap_wasPlaying) continuePlay()

                clearScrollerState()

                //修改状态
                isSeekReady = true
            }
            //来自新的媒体设置成功
            Mark_playerReadyFrom_setNewItem -> {
                //开启控件显示
                start_S_Area_PassiveControl()

                //启动播放
                continuePlay()

                //隐藏遮罩
                closeCover(delay = true)

                //修改状态
                isSeekReady = true
            }
            //外部控制寻帧
            else -> {

                syncScrollTask_Core_Compute()

                //修改状态
                isSeekReady = true
            }
        }

    }
    private var Mark_playerReadyFrom = Undefined
    private val Mark_playerReadyFrom_TailSeek = "Mark_playerReadyFrom_TailSeek"
    private val Mark_playerReadyFrom_MidSectionSeek = "Mark_playerReadyFrom_MidSectionSeek"
    private val Mark_playerReadyFrom_SingleTapSeek = "Mark_playerReadyFrom_SingleTapSeek"
    private val Mark_playerReadyFrom_setNewItem = "Mark_playerReadyFrom_setNewItem"
    //状态playEnd
    private fun playState_playEnd() {
        //已移除媒体项时不触发
        if (!PlayerSingleton.get_engine_ongoing_URI().first) return
        //根据循环模式执行操作
        val loopMode = ListManagerHelper.getLoopMode()
        when (loopMode) {
            ListManagerHelper.LOOP_MODE_ONE -> {
                notice("单集循环", 3000)
                //平滑滚动进度条到起始位置
                syncScrollTask_Core_smoothSlowly_Compute(0,true)
            }
            ListManagerHelper.LOOP_MODE_ALL -> {

            }
            ListManagerHelper.LOOP_MODE_OFF -> {
                //显示提示
                notice("视频结束", 1000)
                //停止被控控件
                stopVideoTimeSync()
                stop_S_Area_PassiveControl()
                //播放结束时让控件显示
                setControllerVisible()
                //结束自动隐藏控件计时器
                IDLE_Timer?.cancel()

            }
        }
    }
    //播放与暂停
    @Suppress("SameParameterValue")
    private fun pausePlay() {
        //调用暂停播放(确保活动内唯一调用)
        PlayerSingleton.pausePlay()
        //设为手动暂停状态
        PlayerSingleton.manualPause = true


        //关闭本地界面更新
        stopVideoTimeSync()
        stop_S_Area_PassiveControl()
    }
    @Suppress("SameParameterValue")
    private fun continuePlay(need_requestFocus: Boolean = true) {

        //调用继续播放(确保活动内唯一调用)
        PlayerSingleton.continuePlay(need_requestFocus)

        //开启本地界面更新
        start_S_Area_PassiveControl()
        startVideoTimeSync()
    }

    //界面控件
    private lateinit var controller_bottom_bar : LinearLayout //底部按钮区域
    private lateinit var root : CardView //根布局
    private lateinit var root_secondary : ConstraintLayout
    private lateinit var controllerLayer : ConstraintLayout //控件层
    private lateinit var controller_top_bar : LinearLayout //顶部按钮区域
    private lateinit var controller_timer_current : TextView //当前时间
    private lateinit var controller_timer_total : TextView //总时间
    private lateinit var noticeCapsule : CardView //通知胶囊卡片
    private lateinit var playerView: PlayerView //播放区域
    //刷新视频总长度
    private fun updateTimerWindow() {
        //设置时间戳-总时长显示位
        val mediaDuration = PlayerInfoCenter.GET_Media_Duration()
        controller_timer_total.text = FormatTime_onlyNum(mediaDuration)
        //开始时间戳更新
        if (player?.isPlaying == true) startVideoTimeSync()
    }
    //进度控制区域 state_current_s_area : 0 = seekbar , 1 = scroller
    private lateinit var s_area : ConstraintLayout
    private lateinit var s_area_scroller : ConstraintLayout
    private lateinit var s_area_seekbar : LinearLayout
    private var state_current_s_area = S_Area_Helper.S_AreaType_UNDEFINED
    private fun show_s_area_type(target:Int) {
        //consoleLog("show_s_area_type : $target")
        when(target){
            S_Area_Helper.S_AreaType_SEEKBAR -> {
                s_area_scroller.visibility = View.GONE
                s_area_seekbar.visibility = View.VISIBLE

                //写入标识
                state_current_s_area = S_Area_Helper.S_AreaType_SEEKBAR
                //写入viewModel
                viewModel.state_s_area_type = S_Area_Helper.S_AreaType_SEEKBAR

                //开启seekBar控制函数
                setupSeekBarFunction()

                //关闭scroller组件
                close_scroller_components()
            }
            S_Area_Helper.S_AreaType_SCROLLER -> {
                s_area_seekbar.visibility = View.GONE
                s_area_scroller.visibility = View.VISIBLE
                s_area_scroller.alpha = 0f
                s_area_scroller.animate().alpha(1f).setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                //写入标识
                state_current_s_area = S_Area_Helper.S_AreaType_SCROLLER
                //写入viewModel
                viewModel.state_s_area_type = S_Area_Helper.S_AreaType_SCROLLER

                //注册进度条控制逻辑
                setupScrollerFunction()

            }
            S_Area_Helper.S_AreaType_UNDEFINED -> {
                s_area_scroller.visibility = View.GONE
                s_area_seekbar.visibility = View.GONE
                //写入标识
                state_current_s_area = S_Area_Helper.S_AreaType_UNDEFINED
                //写入viewModel
                viewModel.state_s_area_type = S_Area_Helper.S_AreaType_UNDEFINED

                //关闭所有组件
                close_scroller_components()
            }
        }
    }
    //启动S_Area被动控制函数(scroller或seekbar:自动绑定视频位置)(被动控制函数必须在此调用)
    private fun start_S_Area_PassiveControl() {
        //consoleLog("start_S_Area_PassiveControl num_random-$num_random")
        //检测当前用的S_Area类型
        when(state_current_s_area){
            S_Area_Helper.S_AreaType_SCROLLER -> {
                //启动scroller被动控制
                startScrollerSync()
            }
            S_Area_Helper.S_AreaType_SEEKBAR -> {
                //启动seekbar被动控制
                startSeekBarSync()
            }
            S_Area_Helper.S_AreaType_UNDEFINED -> {

            }
        }
    }
    //停止S_Area被动控制函数(scroller或seekbar:自动绑定视频位置)(被动控制函数必须在此调用)
    private fun stop_S_Area_PassiveControl() {

        //停止scroller被动控制
        stopScrollerSync()
        //停止seekbar被动控制
        stopSeekBarSync()
    }

    //进度条区域
    private lateinit var scroller : RecyclerView
    private var scrollerLayoutManager : LinearLayoutManager ?= null
    private var scrollerAdapter : PlayerScrollerAdapter ?= null
    private fun close_scroller_components() {
        //关闭scroller
        scroller.adapter = null
        scrollerAdapter = null
        scrollerLayoutManager = null
    }
    private fun update_S_Area_Adapter() {
        lifecycleScope.launch(Dispatchers.IO) {
            val useSeekBar = SettingsCenter.GET_PRF_Video_Screening_Type() == S_Area_Helper.S_AreaType_SEEKBAR
            if (useSeekBar){
                //consoleLog("updateScrollerAdapter 使用 SEEKBAR")
                withContext(Dispatchers.Main){ show_s_area_type(S_Area_Helper.S_AreaType_SEEKBAR) }
                return@launch
            }
            //consoleLog("updateScrollerAdapter 使用 SCROLLER")
            //获取屏幕信息
            val density = display_screen_density
            if (density == 0f){
                consoleLog("updateScrollerAdapter 进度条：获取屏幕density失败，无法显示进度条")
                withContext(Dispatchers.Main){ show_s_area_type(S_Area_Helper.S_AreaType_SEEKBAR) }
                return@launch
            }
            //计算单图宽度px (40dp)
            ScrollerHelper.singleFrame_WidthPx = (40 * density).toInt()
            //计算进度条参数
            //先从信息中心拿到各种必要信息
            val uriNumOnly = PlayerInfoCenter.GET_Media_NUM_ID()
            val mediaDuration = PlayerInfoCenter.GET_Media_Duration()
            if (uriNumOnly <= 0L || mediaDuration == 0L){
                consoleLog("updateScrollerAdapter 进度条：获取信息无效，无法显示进度条")
                withContext(Dispatchers.Main){ withContext(Dispatchers.Main){ show_s_area_type(S_Area_Helper.S_AreaType_SEEKBAR) } }
                return@launch
            }
            //委托给scrollerHelper处理
            val success = ScrollerHelper.prepareForNewMedia(uriNumOnly, mediaDuration)
            if (!success) {
                //consoleLog("updateScrollerAdapter 进度条：信息有效，但解码失败，无法显示进度条")
                withContext(Dispatchers.Main){ withContext(Dispatchers.Main){ show_s_area_type(S_Area_Helper.S_AreaType_SEEKBAR) } }
                return@launch
            }
            //计算单侧图片数量(屏幕宽度参与计算)
            ScrollerHelper.halfScreenEndIndex = min(((display_screen_width_pixels / 2 / ScrollerHelper.singleFrame_WidthPx) + 1),ScrollerHelper.allFrame_totalFrameNumber - 1)
            //consoleLog("updateScrollerAdapter 进度条：半屏结束位索引为${ScrollerHelper.halfScreenEndIndex}")
            //已确认进度条可显示
            //consoleLog("updateScrollerAdapter 进度条：已确认参数上支持显示，开始初始化Adapter")
            //检查能否解码
            val URI = PlayerInfoCenter.GET_Media_URI_S_FP().toUri()
            val success_r = ScrollerHelper.setup_retriever(context, URI)
            if (!success_r){
                consoleLog("updateScrollerAdapter 进度条：解码失败，无法显示进度条")
                withContext(Dispatchers.Main){ show_s_area_type(S_Area_Helper.S_AreaType_SEEKBAR) }
                return@launch
            }

            //应用(已确定参数上支持显示,但还没确定Adapter能不能解码)
            withContext(Dispatchers.Main) {

                //初始化scroller组件
                scrollerLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                scroller.layoutManager = scrollerLayoutManager
                scroller.itemAnimator = null
                scroller.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                scroller.layoutParams.width = 0

                scrollerAdapter = PlayerScrollerAdapter(context, mediaDuration,scroller)

                //显示进度条区域
                show_s_area_type(S_Area_Helper.S_AreaType_SCROLLER)

                //设置进度条内边距
                setScrollerPadding()

                //绑定adapter
                scroller.adapter = scrollerAdapter

                //开启被控
                if (player?.isPlaying == true){
                    start_S_Area_PassiveControl()
                }else{
                    syncScrollTask_Core_Compute()
                }

            }
        }
    }
    //scroller控制器
    private var state_scroller_no_move = false
    private fun setupScrollerFunction() {
        //记录上一次的横轴像素坐标
        var last_e_x = 0f
        //执行注册任务
        //lifecycleScope.launch(Dispatchers.Main) {
            //Scroller事件 gestureDetector层 -onSingleTap -onDown
            val gestureDetectorScroller = GestureDetector(
                this@PlayerActivityNeo,
                object : GestureDetector.SimpleOnGestureListener() {
                    @SuppressLint("SetTextI18n")
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        state_playView_singleTap = true
                        //进入条件
                        if (!viewModel.PREFS_TapJump) {
                            if (viewModel.PREFS_LinkScroll) {
                                //notice("未开启单击跳转", 1000)
                                return false
                            }
                        }

                        //发起单次点击跳转
                        postSingleTapSeek(e.x)


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

                        }
                        MotionEvent.ACTION_UP -> {
                            //consoleLog("ACTION_UP")

                            scrollerTouchState_ACTION_DOWN = false

                        }
                        MotionEvent.ACTION_MOVE -> {
                            //consoleLog("ACTION_MOVE：e.x：${e.x}，last_e_x：$last_e_x")
                            //判断是否在移动
                            val gap_pixels = (last_e_x - e.x).absoluteValue
                            //consoleLog("ACTION_MOVE：gap_pixels：$gap_pixels")
                            state_scroller_no_move = if (gap_pixels < 1){
                                //consoleLog("判断为未在移动 NO_MOVING")
                                true
                            }else{
                                //consoleLog("判断为在移动 MOVING")
                                //更新上一次的横轴像素坐标
                                last_e_x = e.x

                                false
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

                            scrollerState_DRAGGING = true
                            scrollerState_SETTLING = false
                            scrollerState_IDLE = false

                            scrollerDesire_Active = true

                            //发生用户操作-停止界面组件同步
                            stopVideoTimeSync()
                            stop_S_Area_PassiveControl()

                            //记录当前播放状态
                            recordScrollerWasPlayingState()

                            return
                        }
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            //consoleLog("RecyclerView.SCROLL_STATE_SETTLING")

                            scrollerState_DRAGGING = false
                            scrollerState_SETTLING = true
                            scrollerState_IDLE = false

                            //scrollerDesire_Active = true  //SCROLL_STATE_SETTLING 不一定由拖动发起

                            return
                        }
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            //consoleLog("RecyclerView.SCROLL_STATE_IDLE")

                            scrollerState_DRAGGING = false
                            scrollerState_SETTLING = false
                            scrollerState_IDLE = true



                            //触发事件
                            if (scrollerDesire_Active && viewModel.PREFS_LinkScroll) {
                                //检查次数  //备用条件 processed_seek_count == posted_seek_count
                                if (isSeekReady){
                                    //一个滚动事件完整跑完
                                    onScrollOnceComplete()

                                }else{
                                    //未完整跑完,重定向到Ready函数
                                    Mark_playerReadyFrom = Mark_playerReadyFrom_TailSeek

                                }

                            }

                            scrollerDesire_Active = false



                            return
                        }
                    }
                }
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!scrollerDesire_Active) {
                        //未主动操作时也偶尔触发,故基于scrollerDesire_Active过滤
                        return
                    }

                    //修改seek参数(慢速滚动时切到精确帧,快速滚动时切到关键帧)
                    if (scrollerTouchState_ACTION_DOWN){
                        //修改视频seek参数
                        if (viewModel.PRF_Cache_SyncFrame_Dynamic){
                            if (dx in -2..2){
                                //进入低速滑动阶段
                                setSeekParameter_useSync(0)

                            }else{
                                //高速滑动阶段持续使用关键帧
                                setSeekParameter_useSync(1)
                            }
                        }
                    }else{
                        //consoleLog("onScrolled 无操作 重置")

                        setSeekParameter_useSync(1)
                    }

                    //时间窗数值跟随进度条位置随动
                    updateTimeStampWindow()

                    //记录运动方向
                    if (dx > 0){
                        //进度条视频正向走
                        scrollerMotionState_Forward = true

                    }else if(dx < 0){
                        //进度条视频反向走
                        scrollerMotionState_Forward = false

                    }

                    //执行随动操作
                    if (viewModel.PREFS_LinkScroll) {
                        //已开启视频跟随进度条滚动

                        if (scrollerMotionState_Forward){
                            //正向滚动

                            if (viewModel.PREFS_AlwaysSeek) {
                                //跳转方式:寻帧
                                startVideoSeek()
                                stopVideoSmartScroll()
                            }else{
                                //跳转方式:倍速滚动
                                stopVideoSeek()
                                startVideoSmartScroll()
                            }
                        }else{
                            //反向滚动
                            startVideoSeek()

                            stopVideoSmartScroll()

                        }
                    }

                }
            })

        //}
    }
    private var scroller_updateTimerStamp_lastMillis = 0L    //进度条被动刷新时的间隔
    private var scrollerWasPlayingState_recorded = false //本轮滚动是否记录过播放状态变化
    private var playState_scroller_wasPlaying = false //本轮滚动是否播放
    private fun recordScrollerWasPlayingState() {
        if (scrollerWasPlayingState_recorded) return
        scrollerWasPlayingState_recorded = true
        //记录当前播放状态变化
        playState_scroller_wasPlaying = player?.isPlaying ?: false
        //consoleLog("playState_scroller_wasPlaying : $playState_scroller_wasPlaying")

    }
    private var playState_singleTap_wasPlaying = false //singleTap专用wasPlaying
    //传统seekBar区域控制
    private lateinit var seekbar : SeekBar
    //seekbar控制器
    private fun setupSeekBarFunction() {
        lifecycleScope.launch(Dispatchers.Main){
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser){

                        //读取进度条当前位置比例
                        val duration = player?.duration ?: -1L
                        if (duration <= 0) return
                        //计算对应位置
                        val seekToPosition = (progress / 1000f * duration).toLong()

                        //发起seek
                        seekTo_Core(seekToPosition,Mark_playerReadyFrom_MidSectionSeek)
                    }
                }
                //触摸立即触发
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //暂停被动控制
                    stop_S_Area_PassiveControl()
                    //记录暂停状态
                    recordScrollerWasPlayingState()
                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //检查松手时是否有seek指令还在运行
                    if (isSeekReady){
                        //恢复播放状态
                        if (playState_scroller_wasPlaying) continuePlay()
                        //
                        clearScrollerState()

                    }else{
                        Mark_playerReadyFrom = Mark_playerReadyFrom_TailSeek
                    }


                }
            })

            //开启被控
            if (player?.isPlaying == true){
                start_S_Area_PassiveControl()
            }else{
                syncSeekBarPosition_Core()
            }
        }
    }
    //缓存显示配置
    private var isDarkTheme = false  //深色模式
    private var isLandscape = false  //横屏
    //控件隐藏和显示
    private fun setControllerInvisibleNoAnimation() {
        //状态标记变更
        viewModel.state_controllerShowing = false

        //停止被控控件控制
        stopVideoTimeSync()
        //仅在新晋播放页使用
        stop_S_Area_PassiveControl()
        scroller.stopScroll()
        //仅在传统播放页使用
        //stopSeekBarSync()

        //隐藏控件并设置背景为黑色
        controllerLayer.visibility = View.GONE
        setBackgroundInvisible()
    }
    private fun setControllerInvisible() {
        //状态标记变更
        viewModel.state_controllerShowing = false

        //停止被控控件控制
        stopVideoTimeSync()
        //仅在新晋播放页使用
        stop_S_Area_PassiveControl()
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
        viewModel.state_controllerShowing = true

        //启动被控控件控制
        startVideoTimeSync()
        //仅在新晋播放页使用
        start_S_Area_PassiveControl()
        //仅在传统播放页使用
        //startSeekBarSync()

        //隐藏控件并设置背景为有色
        setBackgroundVisible()
        controllerLayer.visibility = View.VISIBLE

    }
    private fun setControllerVisible() {
        //状态标记变更
        viewModel.state_controllerShowing = true

        //启动被控控件控制
        startVideoTimeSync()
        //仅在新晋播放页使用
        start_S_Area_PassiveControl()
        //仅在传统播放页使用
        //startSeekBarSync()

        //显示控件并设置背景为有色
        setBackgroundVisible()
        controllerLayer.visibility = View.VISIBLE
        controllerLayer.animate().alpha(1f).setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

    }
    private fun setBackgroundVisible() {
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Background))
    }
    private fun setBackgroundInvisible() {
        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.Black))
    }
    private fun changeBackgroundColor() {
        if (viewModel.state_controllerShowing){
            setControllerInvisible()
        }else{
            setControllerVisible()
        }
    }
    //进度条内边距设置
    @Suppress("DEPRECATION")
    private fun setScrollerPadding() {
        //根据横竖屏做不同设置
        if (isLandscape){

            //读取屏幕宽度(需要包含状态栏)
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            val screenWidth = displayMetrics.widthPixels
            val sidePadding = screenWidth / 2


            if (SettingsCenter.GET_PREFS_UseCompatScroller()) {
                scroller.setPadding(sidePadding + DeviceInfo.statusBarHeight / 2, 0, sidePadding + DeviceInfo.statusBarHeight / 2 - 1, 0)
            }else{
                scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
            }

        }else{
            //计算边距
            sidePadding = display_screen_width_pixels / 2
            //竖屏
            scroller.setPadding(sidePadding, 0, sidePadding - 1, 0)
        }
    }
    private var sidePadding = 0
    //状态栏配置
    @Suppress("DEPRECATION")
    private fun setStatusBarParams() {
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
    private fun updateLandscapeButton() {
        val ButtonLandscape = findViewById<CircleButton>(R.id.ButtonLandscape)
        //动态切换颜色和Icon的TintColor
        if (isLandscape) {
            ButtonLandscape.setMainColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.VideoPlayerColorPack_CardBackground_State_ON))
            if(isDarkTheme){
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.Black))
            }else{
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.Black))
            }
        }else{
            ButtonLandscape.setMainColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.VideoPlayerColorPack_CardBackground_State_OFF))
            if(isDarkTheme){
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.White))
            }else{
                ButtonLandscape.setIconTintColor(ContextCompat.getColor(this@PlayerActivityNeo, R.color.black))
            }
        }

    } //横屏按钮
    private fun updateButtonState() {
        //禁入条件
        if (!isSeekReady) return
        if (scrollerDesire_Active) return


        //
        val pauseButton = findViewById<CircleButton>(R.id.ButtonPause)
        if (player?.isPlaying == true){
            pauseButton.setIconResource(R.drawable.ic_controller_neo_pause)
        }else{
            pauseButton.setIconResource(R.drawable.ic_controller_neo_play)
        }
    }   //暂停按钮
    //通知卡片位置设置
    private fun setNoticeCardPosition() {
        if (isLandscape){
            //横屏
            (noticeCapsule.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(5f))
        }else{
            //竖屏
            (noticeCapsule.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (dp2px(100f))
        }
    }
    //调整控件位置
    private fun setControllerPosition() {
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
    private fun updateScreenParameters() {
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight == 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
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
        if (!viewModel.state_controllerShowing){ setControllerInvisibleNoAnimation() }

        //刷新横屏按钮
        updateLandscapeButton()
        //设定进度条边界
        setScrollerPadding()


    }
    //关闭遮罩
    private lateinit var cover: LinearLayout
    private fun closeCover(anim: Boolean = false,delay: Boolean = false) {
        //直接隐藏
        fun just_hide() {
            cover.visibility = View.GONE
        }
        //带动画隐藏
        fun hide_with_anim() {
            cover.animate().alpha(0f).setDuration(25).withEndAction { cover.visibility = View.GONE }
        }
        if (delay){
            Handler(mainLooper).postDelayed({
                hide_with_anim()
            }, 0) //疑似低性能机型上会有延时导致黑屏闪过,有的话拉大延时

        }else{
            if(anim){
                hide_with_anim()
            }else{
                just_hide()
            }
        }
    }
    //无播放项遮罩
    private lateinit var layer_error: LinearLayout
    private lateinit var layer_error_text: TextView
    private fun closeErrorCover() {
        //隐藏错误面板
        layer_error_text.text = ""
        layer_error.visibility = View.GONE
    }
    private fun showErrorCover(text: String,hide_buttons: Boolean=false) {
        if (cover.isVisible) return


        //修改提示文本
        layer_error_text.text = text
        layer_error.visibility = View.VISIBLE

        //让控件显示
        setControllerVisible()

        //控制按钮显示状态
        val exitButton = findViewById<CardView>(R.id.layer_error_exit)
        val openListButton = findViewById<CardView>(R.id.layer_error_open_list)
        val closeButton = findViewById<CardView>(R.id.layer_error_close)
        //隐藏或设置点击
        if (hide_buttons){
            exitButton.visibility = View.GONE
            openListButton.visibility = View.GONE
            closeButton.visibility = View.GONE
        }else{
            exitButton.visibility = View.VISIBLE
            openListButton.visibility = View.VISIBLE
            closeButton.visibility = View.VISIBLE
            //设置点击事件
            exitButton.setOnClickListener {
                ToolVibrate.vibrate()

                custom_finish()
            }
            openListButton.setOnClickListener {
                ToolVibrate.vibrate()

                startPlayListFragment()
            }
            closeButton.setOnClickListener {
                ToolVibrate.vibrate()

                if (shouldCloseCover()){
                    closeErrorCover()
                }else{
                    showCustomToast("再次检查发现,目前确实没有媒体在播放", 3)
                }
            }
        }

    }
    //检查并返回是否应当关闭遮罩
    private fun shouldCloseCover(): Boolean {
        //检查播放器是否已经正常恢复
        val (current_media_ongoing, current_media_uri) = PlayerSingleton.get_engine_ongoing_URI()
        if (!current_media_ongoing || current_media_uri == Uri.EMPTY){


            return false
        }else{
            closeErrorCover()

            return true
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
    private fun unlockBrightnessControl() {
        val windowInfo = window.attributes

        windowInfo.screenBrightness = -1f
        window.attributes = windowInfo

        viewModel.brightManager_state_brightness_changed = false

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

        if (targetHeight <= 0) return
        if (view.layoutParams.height == targetHeight) return

        view.layoutParams.height = 0
        view.visibility = View.VISIBLE


        val animator = ValueAnimator.ofInt(0, targetHeight)


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

        if (targetHeight <= 0) return
        if (view.layoutParams.height == targetHeight) return

        view.layoutParams.height = 0
        view.visibility = View.VISIBLE


        val animator = ValueAnimator.ofInt(0, targetHeight)

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            view.layoutParams.height = animatedValue
            view.requestLayout()
        }
        animator.duration = 200

        animator.start()
    }
    //请求使用高刷新率
    private fun requestHighRefreshRate() {
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
    //seekTo 统一入口
    private var isSeekReady = true
    private fun seekTo_Core(pos: Long, mark: String) {
        if (isSeekReady){
            isSeekReady = false

            //暂停播放
            pausePlay()

            //发起Seek
            Mark_playerReadyFrom = mark
            player?.seekTo(pos)
        }
    }


    //Runnable-1.0:根据视频时间更新scroller位置
    private val task_syncScrollerPosition_Handler = Handler(Looper.getMainLooper())
    private val task_syncScrollerPosition_Runnable = object : Runnable {
        @SuppressLint("ServiceCast")
        override fun run() {
            //consoleLog("task_syncScrollerPosition_Runnable")

            //视频是否还处于播放状态
            if (player?.isPlaying != true || scrollerDesire_Active){

                //不再进入下一轮循环
                task_syncScrollerPosition_Running = false
            }else{
                //视频处于播放状态,更新进度条位置
                syncScrollTask_Core_Compute()

                //进入下一次循环
                task_syncScrollerPosition_Handler.postDelayed(this,value_syncScroller_runnableGapMs )  //value_syncScroller_runnableGapMs
            }
        }
    }
    private fun syncScrollTask_Core_Compute() {
        if (ScrollerHelper.singleFrame_durationMs <= 0L) return
        if (state_scrollSmoothSlowly_Running) return
        if (scrollerDesire_Active) return

        val currentPosition = player?.currentPosition ?: -1L
        if (currentPosition == -1L) return

        scrollerParamMain = ( currentPosition / ScrollerHelper.singleFrame_durationMs ).toInt()
        scrollerParamOffset = (( currentPosition - scrollerParamMain * ScrollerHelper.singleFrame_durationMs ) * ScrollerHelper.singleFrame_WidthPx / ScrollerHelper.singleFrame_durationMs ).toInt()

        //设定进度条位置(新版)
        syncScrollTask_Core_Execute(scrollerParamMain, scrollerParamOffset)


    } //进度条位置刷新核心函数
    private fun syncScrollTask_Core_Execute(p1: Int, p2: Int){
        val smoothScroller = SmoothScroller(this,p2,value_syncScroller_runnableGapMs)
        smoothScroller.targetPosition = p1

        scrollerLayoutManager?.startSmoothScroll(smoothScroller)
    }
    private var state_scrollSmoothSlowly_Running = false
    private val scrollSmoothSlowly_Millis = 500L
    private fun syncScrollTask_Core_smoothSlowly_Compute(targetVideoPos_o: Long, force: Boolean = false) {
        if (ScrollerHelper.singleFrame_durationMs <= 0L) return
        if (state_scrollSmoothSlowly_Running && !force) return
        if (!viewModel.state_controllerShowing) return

        val targetVideoPos = if (targetVideoPos_o == -1L) {
            player?.currentPosition ?: -1L
        }else{
            targetVideoPos_o
        }
        if (targetVideoPos < 0L) return

        scrollerParamMain = ( targetVideoPos / ScrollerHelper.singleFrame_durationMs ).toInt()
        scrollerParamOffset = (( targetVideoPos - scrollerParamMain * ScrollerHelper.singleFrame_durationMs ) * ScrollerHelper.singleFrame_WidthPx / ScrollerHelper.singleFrame_durationMs ).toInt()

        //发起滚动任务
        syncScrollTask_Core_smoothSlowly_Execute(scrollerParamMain, scrollerParamOffset, force)

    }
    private fun syncScrollTask_Core_smoothSlowly_Execute(p1: Int, p2: Int,force: Boolean = false) {
        if (state_scrollSmoothSlowly_Running && !force) return
        state_scrollSmoothSlowly_Running = true
        val smoothScroller = SmoothScroller(this,p2,scrollSmoothSlowly_Millis)
        smoothScroller.targetPosition = p1

        //倒计时需要可取消
        smoothSlowly_Execute_Delay()

        scroller.stopScroll()
        scrollerLayoutManager?.startSmoothScroll(smoothScroller)
    }
    private var smoothSlowly_Execute_Delay_Job: Job? = null
    private fun smoothSlowly_Execute_Delay() {
        smoothSlowly_Execute_Delay_Job?.cancel()
        smoothSlowly_Execute_Delay_Job = lifecycleScope.launch {
            delay(scrollSmoothSlowly_Millis)

            state_scrollSmoothSlowly_Running = false

            smoothSlowly_Execute_Delay_Job?.cancel()

        }
    }
    private var scrollerParamMain = 0      //进度条大分段位置参数
    private var scrollerParamOffset = 0    //进度条微调偏移量
    private fun startScrollerSync() {
        //未开启该项设置
        if (!viewModel.PREFS_LinkScroll) return
        //未显示控件层
        if (!viewModel.state_controllerShowing) return
        //操作中
        if (scrollerDesire_Active) return
        //未在播放状态
        if (player?.isPlaying != true) return
        //
        if (ScrollerHelper.singleFrame_durationMs <= 0L) return
        //重复锁
        if (task_syncScrollerPosition_Running) return
        task_syncScrollerPosition_Running = true


        //scrollerLayoutManager = scroller.layoutManager as LinearLayoutManager
        //发起滚动任务
        task_syncScrollerPosition_Handler.post(task_syncScrollerPosition_Runnable)
    }
    private fun stopScrollerSync() {
        if (!task_syncScrollerPosition_Running) return
        task_syncScrollerPosition_Running = false

        task_syncScrollerPosition_Handler.removeCallbacks(task_syncScrollerPosition_Runnable)
    }
    private var value_syncScroller_runnableGapMs = 0L //进度条位置刷新间隔
    private var task_syncScrollerPosition_Running = false
    //Runnable-1.1:根据视频时间更新seekbar位置
    private val task_syncSeekBarPosition_Handler = Handler(Looper.getMainLooper())
    private fun syncSeekBarPosition_Core() {
        val duration = player?.duration ?: -1L
        val currentPosition = player?.currentPosition ?: -1L
        if (duration == -1L || currentPosition == -1L) return
        //计算视频进度比例
        val progressRatio = currentPosition / duration.toFloat()
        //consoleLog("progressRatio = $progressRatio")
        //设定进度条位置(步进1000)
        seekbar.progress = (progressRatio * 1000).toInt()
    }
    private val task_syncSeekBarPosition_Runnable = object : Runnable {
        override fun run(){

            syncSeekBarPosition_Core()

            //进入下一次循环
            task_syncSeekBarPosition_Handler.postDelayed(this,value_syncSeekBar_runnableGapMs )  //value_syncSeekBar_runnableGapMs
        }
    }
    private fun startSeekBarSync() {
        //未显示控件层
        if (!viewModel.state_controllerShowing) return
        //操作中
        if (scrollerDesire_Active) return
        //重复锁
        if (task_syncSeekBarPosition_Running) return
        task_syncSeekBarPosition_Running = true



        //发起滚动任务
        task_syncSeekBarPosition_Handler.post(task_syncSeekBarPosition_Runnable)
    }
    private fun stopSeekBarSync() {
        if (!task_syncSeekBarPosition_Running) return
        task_syncSeekBarPosition_Running = false

        task_syncSeekBarPosition_Handler.removeCallbacks(task_syncSeekBarPosition_Runnable)
    }
    private var value_syncSeekBar_runnableGapMs = 0L //seekbar位置刷新间隔
    private var task_syncSeekBarPosition_Running = false
    //Runnable-2:根据视频时间更新时间窗口进度数值
    private val task_timeStampSync_Handler = Handler(Looper.getMainLooper())
    private fun timeStampSync_Core() {
        //获取时间
        timeStampSync_cache_currentPosition = player?.currentPosition ?: return
        //显示
        controller_timer_current.text = FormatTime_onlyNum(timeStampSync_cache_currentPosition)
    }
    private var task_timeStampSync_Runnable = object : Runnable {
        override fun run() {

            timeStampSync_Core()

            task_timeStampSync_Handler.postDelayed(this, 500)
        }
    }
    private var timeStampSync_cache_currentPosition = 0L
    private fun startVideoTimeSync() {
        //未显示控件层
        if (!viewModel.state_controllerShowing) return
        //未在播放状态
        if (player?.isPlaying != true) return
        //重复锁
        if (task_timeStamp_update_Running) return
        task_timeStamp_update_Running = true


        task_timeStampSync_Handler.post(task_timeStampSync_Runnable)
    }
    private fun stopVideoTimeSync() {
        if (!task_timeStamp_update_Running) return
        task_timeStamp_update_Running = false

        task_timeStampSync_Handler.removeCallbacks(task_timeStampSync_Runnable)
    }
    private var value_timeStamp_updateGapMs = 0L //时间戳刷新间隔
    private var task_timeStamp_update_Running = false
    //Runnable-3:视频滚动寻帧-倍速假寻帧方案
    private val task_smartScrollLoop_Handler = Handler(Looper.getMainLooper())
    private var task_smartScrollLoop_Runnable = object : Runnable {
        override fun run() {
            consoleLog("task_smartScrollLoop_Runnable")
            //
            var delay_millis = 50L
            //获取视频位置
            val currentPosition = player?.currentPosition ?: -1L
            val duration =  player?.duration ?: -1L
            if (currentPosition < 0 || duration < 0) return
            //获取进度条位置对应的视频位置
            val scrollerCorresPosition = duration * (scroller.computeHorizontalScrollOffset().toFloat() / scroller.computeHorizontalScrollRange())

            //计算进度差值
            val progress_gap = scrollerCorresPosition - currentPosition

            if (scrollerDesire_Active) {
                //consoleLog("滚动寻帧 -scrollerDesire_Active -进度差值：${progress_gap}")

                //关闭声音
                suppressVolume()


                when {
                    //距离过近,暂停播放
                    progress_gap <= 100 -> {
                        //暂停
                        player?.pause()

                    }
                    //使用倍速寻帧
                    (progress_gap in 100f..7000f) -> {

                        var target_speed = (((progress_gap / 100).toInt()) /10.0).toFloat()

                        if (target_speed > smartScrollLoop_last_speed){
                            target_speed += 0.2f
                        }else if(target_speed < smartScrollLoop_last_speed){
                            target_speed -= 0.2f
                        }

                        //??? 似乎有的设备限制了最高只能8倍
                        val MAX_EFFICIENT_SPEED = 20.0f
                        target_speed = target_speed.coerceAtMost(MAX_EFFICIENT_SPEED)

                        //consoleLog("滚动寻帧 -确认使用倍速：计算目标倍速：${speed5}")
                        if (target_speed > 0f){

                            player?.setPlaybackSpeed(target_speed)
                            //确保播放
                            player?.play()
                        }
                    }
                    //距离过远,直接寻帧
                    progress_gap > 7000f -> {
                        //发起seek
                        seekTo_Core(scrollerCorresPosition.toLong(),Mark_playerReadyFrom_MidSectionSeek)

                    }

                }



                task_smartScrollLoop_Handler.postDelayed(this,delay_millis)
            }else{
                //consoleLog("滚动寻帧 -中断")
                //执行中断处理

                //检查最终差值
                when {
                    //距离过近,暂停播放
                    progress_gap <= 3000 -> {
                        //意味着一个滚动任务完整结束
                        onScrollOnceComplete()

                    }
                    progress_gap > 3000f -> {
                        //发起seek
                        seekTo_Core(scrollerCorresPosition.toLong(), Mark_playerReadyFrom_TailSeek)

                    }

                }


                //恢复声音
                recoverVolume()


                //标记smartScroll关闭
                smartScrollRunnableRunning = false


            }


        }
    }
    private var smartScrollLoop_last_speed = 0f
    private fun startVideoSmartScroll() {
        if (!scrollerDesire_Active) return
        //重复锁
        if (smartScrollRunnableRunning) return
        smartScrollRunnableRunning = true

        //关闭进度条同步任务
        stop_S_Area_PassiveControl()
        stopVideoTimeSync()
        //关闭音量
        suppressVolume()
        //开始播放
        player?.play()


        task_smartScrollLoop_Handler.post(task_smartScrollLoop_Runnable)
    }
    private fun stopVideoSmartScroll() {
        if (!smartScrollRunnableRunning) return
        smartScrollRunnableRunning = false

        task_smartScrollLoop_Handler.removeCallbacks(task_smartScrollLoop_Runnable)
    }
    private var smartScrollRunnableRunning = false
    private var Job_SmartScroll_recoverVolume: Job? = null  //恢复音量任务
    private fun recoverVolume() {
        Job_SmartScroll_recoverVolume?.cancel()
        SwitchLandscapeJob = lifecycleScope.launch {
            delay(1000)
            //恢复声音
            PlayerSingleton.rec_engine_volume()
        }
    }
    private fun suppressVolume() {
        Job_SmartScroll_recoverVolume?.cancel()
        //压制声音
        PlayerSingleton.disable_engine_volume()
    }
    //Runnable-4:视频滚动寻帧-标准真寻帧方案
    private val task_standardSeekLoop_Handler = Handler(Looper.getMainLooper())
    private var task_standardSeekLoop_Runnable = object : Runnable{
        override fun run() {

            if (scrollerDesire_Active){

                //发起寻帧
                standardSeekLoop_Core()

                //进度条还在Active状态-继续循环
                task_standardSeekLoop_Handler.postDelayed(this, value_seekVideo_runnableGapMs)

            }else{
                //脱离循环
                task_standardSeekLoop_Running = false
            }

        }
    }
    private fun standardSeekLoop_Core(){
        //仅在进度条Active状态下执行寻帧
        if (scrollerDesire_Active){
            if (scrollerTouchState_ACTION_DOWN && state_scroller_no_move) return

            //根据 进度条比例位置 计算 目标视频位置
            val currentPosition = player?.currentPosition ?: -1L
            val duration = player?.duration ?: -1L
            if (currentPosition == -1L || duration == -1L) return

            val totalScrollerLength = scroller.computeHorizontalScrollRange()
            if (totalScrollerLength == 0) return

            val scrollerPos_Offset = scroller.computeHorizontalScrollOffset()
            val scrollerPos_Percent = scrollerPos_Offset.toFloat() / totalScrollerLength
            val targetSeekToMs = (scrollerPos_Percent * duration).toLong()


            //仅在空闲时发起下一次寻帧
            if (isSeekReady){
                //不同滚动方向操作不同
                when(scrollerMotionState_Forward){
                    //正向滚动
                    true -> {
                        if (targetSeekToMs < currentPosition){
                            //目标位置接近起始,直接置0快速回起始
                            if (targetSeekToMs < 50){
                                if (scrollerDesire_Active) seekTo_Core(0, Mark_playerReadyFrom_MidSectionSeek)
                            }else{
                                if (scrollerDesire_Active) seekTo_Core( targetSeekToMs,Mark_playerReadyFrom_MidSectionSeek)
                            }
                        }else{
                            if (scrollerDesire_Active) seekTo_Core(targetSeekToMs,Mark_playerReadyFrom_MidSectionSeek)
                        }
                    }
                    //反向
                    false -> {
                        if (targetSeekToMs < currentPosition){
                            //目标位置接近起始,直接置0快速回起始
                            if (targetSeekToMs < 50){
                                if (scrollerDesire_Active) seekTo_Core( 0,Mark_playerReadyFrom_MidSectionSeek)
                            }else{
                                if (scrollerDesire_Active) seekTo_Core( targetSeekToMs,Mark_playerReadyFrom_MidSectionSeek)
                            }
                        }else{
                            if (scrollerDesire_Active) seekTo_Core(targetSeekToMs,Mark_playerReadyFrom_MidSectionSeek)
                        }
                    }
                }
            }
        }

    }
    private var value_seekVideo_runnableGapMs = 0L
    private var task_standardSeekLoop_Running = false
    private fun startVideoSeek() {
        if (!scrollerDesire_Active) return
        //重复锁
        if (task_standardSeekLoop_Running) return
        task_standardSeekLoop_Running = true

        //开启循环
        task_standardSeekLoop_Handler.post(task_standardSeekLoop_Runnable)
    }
    private fun stopVideoSeek() {
        if (!task_standardSeekLoop_Running) return
        task_standardSeekLoop_Running = false

        task_standardSeekLoop_Handler.removeCallbacks(task_standardSeekLoop_Runnable)
    }
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
            ToolVibrate.vibrate()
            ButtonChangeOrientation("long")
        }
    }


    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

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
                    viewModel.set_onStop_ByLossFocus()
                    if(!state_FromFloatingWindow){
                        PlayerSingleton.ActivityOnStop(this@PlayerActivityNeo)
                    }
                }
                //活动被销毁
                else{
                    //活动销毁但保存了数据：活动因深色模式切换或尺寸切换发生重建
                    if (state_onSaveInstanceState_reach){
                        viewModel.set_onStop_ByReBuild()
                    }
                    //活动销毁且未保存数据：确实退出了活动
                    else{
                        viewModel.set_onStop_ByRealExit()
                    }
                }
                //决策函数运行结束
                viewModel.state_onStopDecider_Running = false
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
        viewModel.state_onStopDecider_Running = true
        onStopDecideHandler.post(onStopDecideTask)
    }
    private fun stopOnStopDecider() {
        onStopDecideHandler.removeCallbacks(onStopDecideTask)
        viewModel.state_onStopDecider_Running = false
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

