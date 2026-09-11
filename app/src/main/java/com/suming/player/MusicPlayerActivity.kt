package com.suming.player

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode
import kotlin.math.hypot
import kotlin.math.pow

class MusicPlayerActivity : AppCompatActivity() {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerActivitySimple: $msg")
        }
    }
    //ctx
    private val context = this@MusicPlayerActivity
    //播放器引用
    private var player: ExoPlayer ?= null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //显示初始化
        init_display()

        //获取播放器引用
        player = PlayerSingleton.get_player_ref()



        //onMediaItemTrans
        updateMediaDuration()
        startVideoTimeSync()
        updateMediaArtwork()
        updateMediaTitleArtist()


        //注册控件
        register()

        registerSeekBar()


    }

    override fun finish() {
        super.finish()
        //使用收起动画
        @Suppress("DEPRECATION")
        overridePendingTransition(
                R.anim.slide_just_appear,
                R.anim.slide_out_vertical
            )
    }

    private fun init_display(){
        enableEdgeToEdge()
        setContentView(R.layout.activity_music_player)
        //初始化视图
        fun init_view(){
            seekbar = findViewById(R.id.seekBar)

            time_stamp_current = findViewById(R.id.time_stamp_current)
            time_stamp_duration = findViewById(R.id.time_stamp_duration)

            media_artwork = findViewById(R.id.media_artwork)

            media_title = findViewById(R.id.media_title)
            media_artist = findViewById(R.id.media_artist)

        }
        init_view()


    }




    //注册基本控件
    private fun register(){
        //注册控制按钮
        lifecycleScope.launch(Dispatchers.Main) {
            //退出按钮
            val ButtonExit = findViewById<CircleButton>(R.id.TopBarArea_ButtonExit)
            ButtonExit.setOnClickListener {
                finish()
            }

            //提示卡点击时关闭
            val noticeCard = findViewById<CardView>(R.id.noticeCapsule)
            noticeCard.setOnClickListener {
                ToolVibrate().vibrate(context)
                noticeCard.visibility = View.GONE
            }

        }
    }



    //标题和艺术家
    private lateinit var media_title : TextView
    private lateinit var media_artist : TextView
    private fun updateMediaTitleArtist(){
        val title = PlayerInfoCenter.GET_Media_Title()
        val artist = PlayerInfoCenter.GET_Media_Artist()

        //
        media_title.text = title
        media_artist.text = artist

        //设置点击事件
        media_title.setOnClickListener {
            ToolVibrate().vibrate(context)

            media_title.isSelected = true

            val file_name = PlayerInfoCenter.GET_Media_FileName()

            notice("标题:${title}\n文件名:${file_name}",6000)

        }

        media_artist.setOnClickListener {
            ToolVibrate().vibrate(context)

            it.isSelected = true


        }

    }


    //艺术图 ARTWORK
    private lateinit var media_artwork : ImageView
    private fun updateMediaArtwork(){
        //获取当前媒体类型(可以把视频当音乐播放)
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        if (mediaType != MediaType.Audio) {
            //使用默认
            return
        }
        //获取媒体ID
        val media_NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        //获取封面图片
        val artwork = ArtworkFrameManager.GET_ArtworkFrame_Audio_Album(context,media_NUM_ID)
        if (artwork == null) {
            lifecycleScope.launch (Dispatchers.IO){
                //获取当前媒体 URI
                val URI_U = PlayerInfoCenter.GET_Media_URI_S_FP().toUri()

                //截取大图
                val bitmap = ArtworkCapturer.captureAlbumInMusic(context, URI_U ,needCompress = false)
                if (bitmap != null){
                    //显示
                    withContext(Dispatchers.Main){ media_artwork.setImageBitmap(bitmap) }
                }else{
                    //使用默认

                    return@launch
                }

            }


            return
        }
        //图片上屏
        media_artwork.setImageBitmap(artwork)


    }


    //时间戳 TIME_STAMP
    private lateinit var time_stamp_current: TextView
    private lateinit var time_stamp_duration: TextView
    //刷新时间总长
    private fun updateMediaDuration(){
        //获取时长
        //val duration = player?.duration ?: return
        val duration = PlayerInfoCenter.GET_Media_Duration()
        if (duration < 0) return

        time_stamp_duration.text = FormatTime_onlyNum(duration)

    }
    //单次刷新时间戳
    private fun timeStampSync_by_seekbar(progress: Int){
        //计算当前progress对应视频进度
        val duration = PlayerInfoCenter.GET_Media_Duration()
        if (duration < 0) return

        val target_time = duration * progress / 1000

        time_stamp_current.text = FormatTime_onlyNum(target_time)
    }
    //Runnable-2:根据视频时间更新时间窗口进度数值
    private val task_timeStampSync_Handler = Handler(Looper.getMainLooper())
    private fun timeStampSync_Core(){
        //获取当前进度
        timeStampSync_cache_currentPosition = player?.currentPosition ?: return

        time_stamp_current.text = FormatTime_onlyNum(timeStampSync_cache_currentPosition)
    }
    private var task_timeStampSync_Runnable = object : Runnable{
        override fun run() {
            timeStampSync_Core()

            task_timeStampSync_Handler.postDelayed(this, 1000)
        }
    }
    private var timeStampSync_cache_currentPosition = 0L
    private fun startVideoTimeSync() {
        task_timeStampSync_Handler.post(task_timeStampSync_Runnable)
    }
    private fun stopVideoTimeSync() {
        task_timeStampSync_Handler.removeCallbacks(task_timeStampSync_Runnable)
    }
    private var value_timeStamp_updateGapMs = 10L //时间戳刷新间隔


    //控件 SEEKBAR
    private lateinit var seekbar : SeekBar
    //注册 SEEK_BAR 控制函数
    private fun registerSeekBar(){
        lifecycleScope.launch(Dispatchers.Main){
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser){
                        val onProgressChanged_currentMillis = System.currentTimeMillis()
                        if (onProgressChanged_currentMillis - seekbar_updateTimerStamp_lastMillis < value_timeStamp_updateGapMs) return
                        seekbar_updateTimerStamp_lastMillis = onProgressChanged_currentMillis

                        //刷新显示
                        timeStampSync_by_seekbar(progress)

                    }
                }

                //触摸立即触发
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //关闭被动控制
                    stopSeekBarSync()
                    stopVideoTimeSync()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //读取进度条当前位置比例
                    val duration = player?.duration ?: -1L
                    val progress = seekBar?.progress ?: -1
                    if (duration <= 0L || progress < 0) return
                    val seekToPosition = (progress / 1000f * duration).toLong()
                    //consoleLog("duration = $duration, seekToPosition = $seekToPosition")

                    player?.seekTo(seekToPosition)
                    //确保在播放
                    player?.play()

                    //开启被动控制
                    startSeekBarSync()
                    startVideoTimeSync()
                }
            })

            //开启被控
            if (player?.isPlaying == true){
                startSeekBarSync()
            }else{
                stopSeekBarSync()
            }
        }
    }
    private var seekbar_updateTimerStamp_lastMillis = 0L
    //Runnable-1.1:根据视频时间更新 SEEK_BAR 位置 SEEK_BAR 被动刷新
    private val task_syncSeekBarPosition_Handler = Handler(Looper.getMainLooper())
    private val task_syncSeekBarPosition_Runnable = object : Runnable {
        override fun run(){
            //consoleLog("task_syncSeekBarPosition_Runnable")

            val duration = player?.duration ?: -1L
            val currentPosition = player?.currentPosition ?: -1L
            if (duration == -1L || currentPosition == -1L) return
            //计算视频进度比例
            val progressRatio = currentPosition / duration.toFloat()
            //consoleLog("progressRatio = $progressRatio")
            //设定进度条位置(步进1000)
            seekbar.progress = (progressRatio * 1000).toInt()

            //进入下一次循环
            task_syncSeekBarPosition_Handler.postDelayed(this,value_syncSeekBar_runnableGapMs )  //value_syncSeekBar_runnableGapMs
        }
    }
    private fun startSeekBarSync() {
        //consoleLog("startSeekBarSync $num_random")
        //进入锁
        if (task_syncSeekBarPosition_Running) return
        task_syncSeekBarPosition_Running = true

        //发起滚动任务
        task_syncSeekBarPosition_Handler.post(task_syncSeekBarPosition_Runnable)
    }
    private fun stopSeekBarSync() {
        task_syncSeekBarPosition_Running = false
        task_syncSeekBarPosition_Handler.removeCallbacks(task_syncSeekBarPosition_Runnable)
    }
    private var value_syncSeekBar_runnableGapMs = 100L //seekbar位置刷新间隔
    private var task_syncSeekBarPosition_Running = false




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


}