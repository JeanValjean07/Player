package com.suming.player.ActivityComponent.IndepFragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.cardview.widget.CardView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
@Suppress("NewApi","unused")
class PlayerFragmentMediaInfo: DialogFragment() {
    companion object {
        fun newInstance() = PlayerFragmentMediaInfo().apply {
            arguments = bundleOf(    )
        }
    }


    //信息变量
    private var file_path = ""
    private var videoWidth = ""
    private var videoHeight = ""
    private var videoDuration = ""
    private var Fps_real_int_MediaExtractor = 0  //来自MediaExtractor的真实帧率,但取整了
    private var Fps_real_float_ExoEngin = 0f    //来自ExoPlayer的真实浮点帧率,需要由播放器播放后传入
    private var Fps_capture_MediaMetadataRetriever = 0f  //来自MediaMetadataRetriever的采集帧率,即录制时设置的目标帧率,实际无法达到
    private var videoMimeType = ""
    private var videoBitrate = ""
    private var videoFileName = ""
    private var videoTitle = ""
    private var videoArtist = ""
    private var videoDate = ""
    private var realFpsForShow = 0f



    override fun onStart() {
        super.onStart()
        //初始化显示
        initDisplay()
    }

    @Suppress("DEPRECATION")
    private fun initDisplay(){
        //获取window
        val window = dialog?.window ?: return
        //检查横竖屏状态
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //检查深色模式
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        //执行通用设置
        //设置状态栏背景为透明(否则有色块跟随动画飞出)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        //设置背景压暗幅度
        window.setDimAmount(0f)

        //执行绑定屏幕方向的设置
        if (isLandscape){
            //横屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOutHorizontal)


            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, _ -> WindowInsetsCompat.CONSUMED }

                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }else{
            //竖屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOut)



            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                //ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):View{
        //获得view
        val view = inflater.inflate(R.layout.fragment_media_info, container, false)

        //初始化界面
        init(view)

        return view
    }

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化
        init(view)

        register(view)

        mainBusiness()


        //设置composeRoot
        ComposeRoot.setContent {
            ComposeRoot()
        }

        //发布开启事件
        //returnFragment(FragmentConnector.fragment_event_open)


    }

    override fun onResume() {
        super.onResume()
        //发布开启事件
        returnFragment(FragmentConnector.fragment_event_open)
    }
    override fun onPause() {
        super.onPause()
        //发布关闭事件
        returnFragment(FragmentConnector.fragment_event_close)
    }

    private fun init(view: View){
        //设置卡片
        display(view)
        //初始化composeRoot
        ComposeRoot = view.findViewById(R.id.fragment_compose_root)

    }
    @SuppressLint("ClickableViewAccessibility")
    private fun register(view: View){
        //面板下滑关闭
        /*
        if (!SettingsRequestCenter.get_PREFS_DisableFragmentGesture(requireContext())){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                var deltaY_ReachPadding = false
                val RootCard = view.findViewById<CardView>(R.id.main_card)
                val RootCardOriginY = RootCard.translationY
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                var NestedScrollViewAtTop = true
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            deltaY_ReachPadding = false
                            if (NestedScrollView.scrollY != 0){
                                NestedScrollViewAtTop = false
                                return@setOnTouchListener false
                            }else{
                                NestedScrollViewAtTop = true
                                down_y = event.rawY
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!NestedScrollViewAtTop){
                                return@setOnTouchListener false
                            }
                            deltaY = event.rawY - down_y
                            if (deltaY < 0){
                                return@setOnTouchListener false
                            }
                            if (deltaY >= 400f){
                                if (!deltaY_ReachPadding){
                                    deltaY_ReachPadding = true
                                    ToolVibrate().vibrate(requireContext())
                                }
                            }
                            RootCard.translationY = RootCardOriginY + deltaY
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (deltaY >= 400f){
                                dismiss()
                            }else{
                                RootCard.animate()
                                    .translationY(0f)
                                    .setInterpolator(DecelerateInterpolator(1f))
                                    .duration = 300
                            }

                        }
                    }
                    return@setOnTouchListener false
                }
            }
            else if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                var down_y = 0f
                var deltaY = 0f
                var down_x = 0f
                var deltaX = 0f
                var deltaX_ReachPadding = false
                var Y_move_ensure = false
                val RootCard = view.findViewById<CardView>(R.id.main_card)
                val RootCardOriginX = RootCard.translationX
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            down_x = event.rawX
                            down_y = event.rawY
                            Y_move_ensure = false
                            deltaX_ReachPadding = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            deltaY = event.rawY - down_y
                            deltaX = event.rawX - down_x
                            if (deltaX < 0){
                                return@setOnTouchListener false
                            }
                            if (deltaX >= 200f){
                                if (!deltaX_ReachPadding){
                                    deltaX_ReachPadding = true
                                    ToolVibrate().vibrate(requireContext())
                                }
                            }
                            if (Y_move_ensure){
                                return@setOnTouchListener false
                            }
                            if (abs(deltaY) > abs(deltaX)){
                                Y_move_ensure = true
                                return@setOnTouchListener false
                            }
                            RootCard.translationX = RootCardOriginX + deltaX
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (Y_move_ensure){
                                return@setOnTouchListener false
                            }
                            if (deltaX >= 200f){
                                dismiss()
                            }else{
                                RootCard.animate()
                                    .translationX(0f)
                                    .setInterpolator(DecelerateInterpolator(1f))
                                    .duration = 300
                            }
                        }
                    }
                    return@setOnTouchListener false
                }
            }
        }

         */

        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.out_area)
        topArea.setOnClickListener {
            dismiss()
        }

    }

    private fun mainBusiness(){
        lifecycleScope.launch(Dispatchers.IO){
            delay(700)
            //读取信息
            val MediaInfoPack = PlayerInfoCenter.GET_Media_FullMediaInfoPack()
            if (MediaInfoPack == null){
                withContext(Dispatchers.Main){
                    requireContext().showCustomToast("信息读取失败")
                }

                dismiss()
                return@launch
            }

            file_path = MediaInfoPack.file_path
            Fps_real_float_ExoEngin = MediaInfoPack.video_actualFPS
            val retriever = MediaMetadataRetriever()
            try{
                retriever.setDataSource(file_path)
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    requireContext().showCustomToast("信息解码失败($e)")
                }

                consoleLog("MediaMetadataRetriever() 发生错误：$e")
                dismiss()
                return@launch
            }


            videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?:""
            videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?:""
            videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?:""
            videoMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?:""
            videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?:""
            videoFileName = (File(file_path)).name ?: ""
            videoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?:""
            videoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?:""
            videoDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?:""
            val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?:"0.0"

            try {
                Fps_capture_MediaMetadataRetriever = captureFps.toFloat()
            }catch(e: Exception){
                consoleLog("MediaMetadataRetriever() 发生错误：$e")
            }

            retriever.release()

            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file_path)
                //遍历所有轨道
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        //视频轨道
                        Fps_real_int_MediaExtractor = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        //consoleLog("MediaExtractor() 找到属于视频的轨道，帧率为 $Fps_real_int_MediaExtractor")
                    }
                }
            }catch(e: Exception){
                consoleLog("MediaExtractor() 发生错误：$e")
            }finally{
                extractor.release()
            }

            //选出要显示的实际帧率
            realFpsForShow = if(Fps_real_float_ExoEngin != 0f) {
                Fps_real_float_ExoEngin
            }else{
                Fps_real_int_MediaExtractor.toFloat()
            }


            extractor.release()


            forceUpdate.intValue++

        }
    }





    @Composable
    fun ComposeRoot() {
        //在root中取颜色模式
        isDarkMode = isSystemInDarkTheme()
        ColorPack = if (isDarkMode) DarkColorScheme else LightColorScheme
        //使用Box作为根布局
        Box(modifier = Modifier
            .fillMaxSize()
            .background(ColorPack.surface)
        ) {
            //顶部栏高度值
            var topBarHeight by remember { mutableIntStateOf(300) }
            val topPaddingDp = with(LocalDensity.current) {
                topBarHeight.toDp()
            }


            //最底层


            //内容层
            ContentRoot(topPaddingDp)

            //最顶层
            BrushArea()
            AdvancedTopBar(onHeightMeasured = { height ->
                //更新内边距
                topBarHeight = height
            })
        }
    }
    private lateinit var ComposeRoot: ComposeView
    private val forceUpdate = mutableIntStateOf(0)
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun AdvancedTopBar(onHeightMeasured: (height: Int) -> Unit) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .onGloballyPositioned { coordinates ->
                    onHeightMeasured(coordinates.size.height)
                },
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(59.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //左侧
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //关闭按钮
                        CircleButton(
                            onClick = {
                                ToolVibrate().vibrate(requireContext())
                                dismiss() },
                            backgroundColor = ColorPack.background.copy(alpha = 0.99f),
                            size = 40.dp,
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = Color.Gray.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.background(Color.Transparent),
                                tint = ColorPack.secondary
                            )
                        }
                        //标题文本
                        Text(
                            text = "媒体信息",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPack.primary,
                            modifier = Modifier.padding(start = 0.dp)
                        )
                    }
                    //右侧
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //锁
                        CircleButton(
                            onClick = {
                                ToolVibrate().vibrate(requireContext())

                                 },
                            backgroundColor = ColorPack.background.copy(alpha = 0.99f),
                            size = 40.dp,
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = Color.Gray.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "锁定页面",
                                modifier = Modifier.background(Color.Transparent),
                                tint = ColorPack.secondary
                            )
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun CircleButton(onClick: () -> Unit,
                     modifier: Modifier = Modifier,
                     size: Dp = 30.dp,
                     backgroundColor: Color = ColorPack.primary,
                     gradient: Brush? = null,
                     border: BorderStroke? = null,
                     elevation: Dp = 3.dp,
                     enabled: Boolean = true,
                     content: @Composable () -> Unit ) {
        val backgroundModifier = when {
            gradient != null -> Modifier.background(gradient)
            else -> Modifier.background(backgroundColor)
        }
        Box(
            modifier = modifier
                .size(size)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    clip = false,
                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                    ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)
                )
                .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
                .clip(CircleShape)
                .then(backgroundModifier)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                ) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
    @Composable
    fun BrushArea(modifier: Modifier = Modifier, height: Dp = 90.dp) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ColorPack.surface.copy(alpha = 0.90f),
                            ColorPack.surface.copy(alpha = 0.0f)
                        ),
                    )
                )
        )
    }
    @Composable
    fun ContentRoot(topBarHeight: Dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topBarHeight),
        ) {
            if (forceUpdate.intValue > 0){
                Info()
            }
        }
    }
    @Composable
    fun Info(){
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .uniformShadow()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
            border = BorderStroke(
                width = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.1f)
            ),
            colors = CardDefaults.cardColors(containerColor = ColorPack.background)
        ){
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp).fillMaxWidth()
            ) {

                    Text(text = "视频分辨率：$videoWidth x $videoHeight", color = ColorPack.secondary)
                    Text(text = "\n")
                    Text(text = "视频时长：${videoDuration.toLong() / 1000} 秒丨${FormatTime_withChar(videoDuration.toLong())}", color = ColorPack.secondary)
                    Text(text = "\n")
                    //采集帧率
                    if (Fps_capture_MediaMetadataRetriever != 0f){
                        Text(text =  "采集帧率：$Fps_capture_MediaMetadataRetriever FPS", color = ColorPack.secondary )
                        Text(text = "\n")
                    }
                    //视频实际帧率
                    Text(text =  "实际帧率(轨道计算)：$Fps_real_int_MediaExtractor FPS" , color = ColorPack.secondary)
                    Text(text = "\n")
                    Text(text =  "实际帧率(播放器回报)：$Fps_real_float_ExoEngin FPS" , color = ColorPack.secondary)
                    Text(text = "\n")
                    Text(text = "视频编码：$videoMimeType", color = ColorPack.secondary)
                    Text(text = "\n")
                    Text(text = "视频码率：${videoBitrate.toLong() / 1000} kbps", color = ColorPack.secondary)
                    Text(text = "\n")
                    Text(text = "视频文件名：$videoFileName", color = ColorPack.secondary)
                    Text(text = "\n")
                    //视频标题
                    if (videoTitle.isBlank()) {
                        Text(text = "视频标题：未写入此条元数据", color = ColorPack.secondary)
                    } else {
                        Text(text = "视频标题：$videoTitle", color = ColorPack.secondary)
                    }
                    Text(text = "\n")
                    //视频艺术家
                    if (videoArtist.isBlank()) {
                        Text(text = "视频艺术家：未写入此条元数据", color = ColorPack.secondary)
                    } else {
                        Text(text = "视频艺术家：$videoArtist", color = ColorPack.secondary)
                    }
                    Text(text = "\n")
                    //视频日期
                    if (videoDate == "19040101T000000.000Z") {
                        Text(text = "视频日期：未写入此条元数据", color = ColorPack.secondary)
                    } else {
                        Text(text = "视频日期：$videoDate",color = ColorPack.secondary)
                    }

            }
        }

    }
    //自定义阴影
    @Suppress("DEPRECATION")
    fun Modifier.uniformShadow(
        blurRadius: Float = 15f,
        shadowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f)
    ) = this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = shadowColor
                asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                    blurRadius,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }

            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = 12.dp.toPx(),
                radiusY = 12.dp.toPx(),
                paint = paint
            )
        }
    }
    //composable颜色配置
    private var isDarkMode: Boolean = false
    private lateinit var ColorPack: ColorScheme
    private val LightColorScheme = lightColorScheme(
        //全局底色
        surface = Color(0xFFFFFFFF),
        //一级和二级文字
        primary = Color(0xFF000000),
        secondary = Color(0xFF313131),
        //卡片底色
        background = Color(0xFFFFFFFF),

        )
    private val DarkColorScheme = darkColorScheme(
        //全局底色
        surface = Color(0xFF181818),
        //一级和二级文字
        primary = Color(0xFFFFFFFF),
        secondary = Color(0xFFF6F6F6),
        //卡片底色
        background = Color(0xFF121212),
    )



    //时间格式化
    @SuppressLint("DefaultLocale")
    private fun FormatTime_withChar(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours == 0L){
            String.format("%02d分%02d秒",  minutes, seconds)
        }else{
            String.format("%02d时%02d分%02d秒",  hours, minutes, seconds)
        }
    }
    //发布事件回Activity  Fragment -> Activity  fragment_request_key_media_info_reverse
    private fun returnFragment(event: String){
        val result = bundleOf(FragmentConnector.receive_key to event)
        setFragmentResult(FragmentConnector.fragment_request_key_media_info_reverse, result)
    }
    private fun returnFragment(event: String,extra: String){
        val result = bundleOf(FragmentConnector.receive_key to event,FragmentConnector.extra_key to extra)
        setFragmentResult(FragmentConnector.fragment_request_key_media_info_reverse, result)
    }
    //Tool Functions
    //设置面板几何参数
    private fun display(view: View){
        //获取当前屏幕方向
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //操作主卡片视图
        val mainCard = view.findViewById<CardView>(R.id.main_card)
        //读取屏幕信息
        val screenHeightPx = resources.displayMetrics.heightPixels
        val screenWidthPx = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density

        if (isLandscape){
            //计算目标宽度
            val targetScreenWidthPx = (screenWidthPx * 0.4).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()
            //进行宽度保底
            if (targetScreenHeightDp < 50){
                mainCard.layoutParams.width = screenWidthPx
            }else{
                mainCard.layoutParams.width = targetScreenWidthPx
            }
            //设置卡片显示参数
            mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            mainCard.setContentPadding(0, DeviceInfo.statusBarHeight, 0, 0)
            //请求布局更新
            mainCard.requestLayout()
        }else{
            //计算目标高度
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()
            //进行高度保底
            if (targetScreenHeightDp < 450){
                mainCard.layoutParams.height = screenHeightPx
            }else{
                mainCard.layoutParams.height = targetHeightPx
            }
            //请求布局更新
            mainCard.requestLayout()

        }
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            dismiss()
        }
    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerFragmentMediaInfo: $msg")
        }
    }


}