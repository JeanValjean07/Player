package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs

@UnstableApi
class PlayerFragmentVideoInfo: DialogFragment() {
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //自动关闭标志位
    private var lockPage = false
    //静态工厂
    companion object {
        private const val ARG_VIDEO_WIDTH = "video_width"
        private const val ARG_VIDEO_HEIGHT = "video_height"
        private const val ARG_VIDEO_DURATION = "video_duration"
        private const val ARG_VIDEO_FPS = "video_fps"
        private const val ARG_CAPTURE_FPS = "capture_fps"
        private const val ARG_VIDEO_MIME_TYPE = "video_mime_type"
        private const val ARG_VIDEO_BITRATE = "video_bitrate"
        private const val ARG_VIDEO_FILE_NAME = "video_file_name"
        private const val ARG_VIDEO_TITLE = "video_title"
        private const val ARG_VIDEO_ARTIST = "video_artist"
        private const val ARG_VIDEO_DATE = "video_date"

        fun newInstance(
            width: Int,
            height: Int,
            duration: Long,
            fps: Float,
            capFps: Float,
            mime: String,
            bitrate: Long,
            fileName: String,
            title: String,
            artist: String,
            date: String
        ) = PlayerFragmentVideoInfo().apply {
            arguments = bundleOf(
                ARG_VIDEO_WIDTH to width,
                ARG_VIDEO_HEIGHT to height,
                ARG_VIDEO_DURATION to duration,
                ARG_VIDEO_FPS to fps,
                ARG_CAPTURE_FPS to capFps,
                ARG_VIDEO_MIME_TYPE to mime,
                ARG_VIDEO_BITRATE to bitrate,
                ARG_VIDEO_FILE_NAME to fileName,
                ARG_VIDEO_TITLE to title,
                ARG_VIDEO_ARTIST to artist,
                ARG_VIDEO_DATE to date
            )
        }
    }
    //信息变量
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var videoDuration: Long = 0
    private var videoFps: Float = 0f
    private var captureFps: Float = 0f
    private lateinit var videoMimeType: String
    private var videoBitrate: Long = 0
    private lateinit var videoFileName: String
    private lateinit var videoTitle: String
    private lateinit var videoArtist: String
    private lateinit var videoDate: String


    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }

                /*
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }

                 */

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
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
        requireArguments().let { b ->
            videoWidth = b.getInt(ARG_VIDEO_WIDTH)
            videoHeight = b.getInt(ARG_VIDEO_HEIGHT)
            videoDuration = b.getLong(ARG_VIDEO_DURATION)
            videoFps = b.getFloat(ARG_VIDEO_FPS)
            captureFps = b.getFloat(ARG_CAPTURE_FPS)
            videoMimeType = b.getString(ARG_VIDEO_MIME_TYPE, "")
            videoBitrate = b.getLong(ARG_VIDEO_BITRATE)
            videoFileName = b.getString(ARG_VIDEO_FILE_NAME, "")
            videoTitle = b.getString(ARG_VIDEO_TITLE, "")
            videoArtist = b.getString(ARG_VIDEO_ARTIST, "")
            videoDate = b.getString(ARG_VIDEO_DATE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_video_info, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //按钮：退出
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            Dismiss(true)
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            Dismiss(true)
        }
        //按钮：锁定页面
        val ButtonLock = view.findViewById<ImageButton>(R.id.buttonLock)
        ButtonLock.setOnClickListener {
            vibrate()
            lockPage = !lockPage
            if (lockPage) {
                ButtonLock.setImageResource(R.drawable.ic_more_button_lock_on)
            } else {
                ButtonLock.setImageResource(R.drawable.ic_more_button_lock_off)
            }
        }


        //信息显示
        val composeVideoInfo = view.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_video_info)
        composeVideoInfo.setContent {
            PlayerComposeVideoInfo()
        }


        //面板下滑关闭(NestedScrollView)
        if (!vm.PREFS_CloseFragmentGesture){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginY = RootCard.translationY
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                var NestedScrollViewAtTop = true
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
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
                            RootCard.translationY = RootCardOriginY + deltaY
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (deltaY >= 400f){
                                Dismiss(true)
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
                var Y_move_ensure = false
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginX = RootCard.translationX
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            down_x = event.rawX
                            down_y = event.rawY
                            Y_move_ensure = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            deltaY = event.rawY - down_y
                            deltaX = event.rawX - down_x
                            if (deltaX < 0){
                                return@setOnTouchListener false
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
                                Dismiss(true)
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
        //监听返回手势(DialogFragment)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Dismiss(false)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

    }



    //Functions
    @Composable
    private fun PlayerComposeVideoInfo(){

        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                color = colorResource(R.color.HeadText)
            )
        )
        {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                Text(text = "视频分辨率：$videoWidth x $videoHeight")
                Text(text = "\n")

                Text(text = "视频时长：${videoDuration / 1000} 秒丨${FormatTime_withChar(videoDuration)}")
                Text(text = "\n")

                //视频实际帧率
                if (captureFps != 0f && captureFps > videoFps){
                    Text(text = "视频帧率：$videoFps FPS (受限于设备性能,实际帧率未达到采集帧率)")
                }else{
                    Text(text = "视频帧率：$videoFps FPS")
                }
                Text(text = "\n")

                //采集帧率
                if (captureFps == 0f){
                    Text(text = "采集帧率：0 FPS (来自网络的视频通常无此值)")
                }else{
                    Text(text = "采集帧率：$captureFps FPS")
                }
                Text(text = "\n")

                Text(text = "视频编码：$videoMimeType")
                Text(text = "\n")

                Text(text = "视频码率：${videoBitrate / 1000} kbps")
                Text(text = "\n")

                Text(text = "视频文件名：$videoFileName")
                Text(text = "\n")

                //视频标题
                if (videoTitle.isBlank()){ Text(text = "视频标题：未写入此条元数据") }else{ Text(text = "视频标题：$videoTitle") }
                Text(text = "\n")

                //视频艺术家
                if (videoArtist.isBlank()){ Text(text = "视频艺术家：未写入此条元数据") }else{ Text(text = "视频艺术家：$videoArtist") }
                Text(text = "\n")

                //视频日期
                if (videoDate == "19040101T000000.000Z"){ Text(text = "视频日期：未写入此条元数据") }else{ Text(text = "视频日期：$videoDate") }

            }
        }

    }

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
        val vib = requireContext().vibrator()
        if (vm.PREFS_UseSysVibrate) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vib.vibrate(effect)
        }
        else{
            vib.vibrate(VibrationEffect.createOneShot(vm.PREFS_VibrateMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    //自定义退出逻辑
    private fun customDismiss(){
        if (!lockPage) {
            Dismiss(false)
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ vibrate() }
        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()

    }


}