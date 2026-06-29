package com.suming.player.ActivityComponent.IndepFragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect
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
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.PlayerInfoCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
@RequiresApi(Build.VERSION_CODES.Q)
class PlayerFragmentMediaInfo: DialogFragment() {
    companion object {
        fun newInstance() = PlayerFragmentMediaInfo().apply {
            arguments = bundleOf(    )
        }
    }


    //信息变量
    private var absolutePath = ""
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


    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { _, _ -> WindowInsetsCompat.CONSUMED }
                //三星专用:显示到挖空区域
                dialog?.window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                dialog?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOutHorizontal)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog?.window?.statusBarColor = Color(0x00000000).toArgb()
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.statusBarColor = Color(0x00000000).toArgb()
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_media_info, container, false)
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


    }

    override fun onResume() {
        super.onResume()
        //发布开启事件
        returnFragment(FragmentConnector.fragment_event_open)
    }
    override fun onDestroy() {
        super.onDestroy()
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

    }

    private fun mainBusiness(){
        lifecycleScope.launch(Dispatchers.IO){
            //读取信息
            val MediaInfoPack = PlayerInfoCenter.getMediaInfoPack()
            if (MediaInfoPack == null){
                requireContext().showCustomToast("信息读取失败")
                dismiss()
                return@launch
            }

            absolutePath = MediaInfoPack.MediaInfo_AbsolutePath
            Fps_real_float_ExoEngin = MediaInfoPack.MediaInfo_RealFps
            val retriever = MediaMetadataRetriever()
            try{
                retriever.setDataSource(absolutePath)
            }catch (e: Exception){
                requireContext().showCustomToast("信息解码失败($e)")
                consoleLog("MediaMetadataRetriever() 发生错误：$e")
                dismiss()
                return@launch
            }


            videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?:""
            videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?:""
            videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?:""
            videoMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?:""
            videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?:""
            videoFileName = (File(absolutePath)).name ?: ""
            videoTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?:""
            videoArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?:""
            videoDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?:""
            val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?:"0.0"

            try {
                Fps_capture_MediaMetadataRetriever = captureFps.toFloat()
            }catch(e: Exception){

            }

            retriever.release()

            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(absolutePath)
                //遍历所有轨道
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        //视频轨道
                        Fps_real_int_MediaExtractor = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        consoleLog("MediaExtractor() 找到视频轨道，帧率为 $Fps_real_int_MediaExtractor")
                    }else{
                        consoleLog("MediaExtractor() 未找到视频轨道")
                    }
                }
            } catch (e: Exception) {
                consoleLog("MediaExtractor() 发生错误：$e")
            } finally {
                extractor.release()
            }

            //选出要显示的实际帧率
            realFpsForShow = if(Fps_real_float_ExoEngin != 0f) {
                Fps_real_float_ExoEngin
            }else{
                Fps_real_int_MediaExtractor.toFloat()
            }


            extractor.release()


            forceUpdate.value++

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
    private val forceUpdate = mutableStateOf(0)
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
            color = androidx.compose.ui.graphics.Color.Transparent,
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
                                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.background(androidx.compose.ui.graphics.Color.Transparent),
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
                     backgroundColor: androidx.compose.ui.graphics.Color = ColorPack.primary,
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
            if (forceUpdate.value > 0){
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
                Text(text = "视频分辨率：$videoWidth x $videoHeight")
                Text(text = "\n")
                Text(text = "视频时长：${videoDuration.toLong() / 1000} 秒丨${FormatTime_withChar(videoDuration.toLong())}")
                Text(text = "\n")
                //采集帧率
                if (Fps_capture_MediaMetadataRetriever.toFloat() != 0f){
                    Text(text =  "采集帧率：$Fps_capture_MediaMetadataRetriever FPS" )
                    Text(text = "\n")
                }
                //视频实际帧率
                Text(text =  "实际帧率：$realFpsForShow FPS" )
                Text(text = "\n")
                Text(text = "视频编码：$videoMimeType")
                Text(text = "\n")
                Text(text = "视频码率：${videoBitrate.toLong() / 1000} kbps")
                Text(text = "\n")
                Text(text = "视频文件名：$videoFileName")
                Text(text = "\n")
                //视频标题
                if (videoTitle.isBlank()) {
                    Text(text = "视频标题：未写入此条元数据")
                } else {
                    Text(text = "视频标题：$videoTitle")
                }
                Text(text = "\n")
                //视频艺术家
                if (videoArtist.isBlank()) {
                    Text(text = "视频艺术家：未写入此条元数据")
                } else {
                    Text(text = "视频艺术家：$videoArtist")
                }
                Text(text = "\n")
                //视频日期
                if (videoDate == "19040101T000000.000Z") {
                    Text(text = "视频日期：未写入此条元数据")
                } else {
                    Text(text = "视频日期：$videoDate")
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
    //发布事件
    private fun returnFragment(event: String){
        val result = bundleOf(FragmentConnector.receive_key to event)
        setFragmentResult(FragmentConnector.fragment_request_key_media_info, result)
    }
    private fun returnFragment(event: String,extra: String){
        val result = bundleOf(FragmentConnector.receive_key to event,FragmentConnector.extra_key to extra)
        setFragmentResult(FragmentConnector.fragment_request_key_media_info, result)
    }
    //设置面板高度
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

            mainCard.post {
                if (targetScreenHeightDp < 50){
                    mainCard.layoutParams.width = screenWidthPx
                }else{
                    mainCard.layoutParams.width = targetScreenWidthPx
                }
                //把高度改为match parent
                mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                val statusBarHeight = getStatusBarHeightFromView(mainCard)
                mainCard.setContentPadding(0, statusBarHeight, 0, 0)

                mainCard.requestLayout()
            }

        }else{
            //计算目标高度
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()

            mainCard.post {
                if (targetScreenHeightDp < 450){
                    mainCard.layoutParams.height = screenHeightPx
                }else{
                    mainCard.layoutParams.height = targetHeightPx
                }
                mainCard.requestLayout()
            }
        }
    }
    fun getStatusBarHeightFromView(view: View): Int {
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        return rect.top
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