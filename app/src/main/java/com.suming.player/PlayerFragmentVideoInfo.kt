package com.suming.player

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerFragmentVideoInfo: DialogFragment() {

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



    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }
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

    @SuppressLint("UseGetLayoutInflater", "InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        //按钮：退出
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            dismiss()
        }


        Log.d("SuMing", "VideoInfo: Width $videoWidth Height $videoHeight,Duration $videoDuration ms, $videoFps fps  $captureFps fps, $videoMimeType, $videoBitrate bps")

        //信息显示
        val composeVideoInfo = view.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_video_info)
        composeVideoInfo.setContent {
            PlayerComposeVideoInfo()
        }








    } //onViewCreated END



    //Functions
    @Composable
    private fun PlayerComposeVideoInfo(){
        Column(
            Modifier
                .padding(16.dp)
        ) {

            Text(text = "视频分辨率：$videoWidth x $videoHeight")
            Text(text = "\n")

            Text(text = "视频时长：${videoDuration / 1000} 秒丨${formatTime1(videoDuration)}")
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

    @SuppressLint("DefaultLocale")
    private fun formatTime1(milliseconds: Long): String {
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


}