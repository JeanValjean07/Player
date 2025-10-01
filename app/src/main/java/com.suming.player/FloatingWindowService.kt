package com.suming.player

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.paging.LOG_TAG
import kotlinx.coroutines.delay
import kotlin.concurrent.thread


class FloatingWindowService : Service() {
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    private var mParams: WindowManager.LayoutParams? = null

     //视频尺寸
    private var videoSizeWidth = 0
    private var videoSizeHeight = 0
    //屏幕宽度
    private var screenWidth = 0



    private var ACTION_DOWN_F_X = 0
    private var ACTION_DOWN_F_Y = 0

    private var ACTION_DOWN_W_X = 0
    private var ACTION_DOWN_W_Y = 0


    private var ACTION_MOVE_X  = 0
    private var ACTION_MOVE_Y  = 0

     //原始尺寸
    private var originWidth = 0
    private var originHeight = 0

    private var isFolded = false

     //视频尺寸动态尺寸
    private var videoSizeWidthD = 0
    private var videoSizeHeightD = 0



    //视频尺寸标志位
    private var sizeSign = 0



    override fun onBind(intent: Intent?): IBinder? {
        Log.d("SuMing","FloatingWindowService onBind:$intent")
        //获取视频尺寸
        videoSizeWidth = intent?.getIntExtra("VIDEO_SIZE_WIDTH", 0) ?: 0
        videoSizeHeight = intent?.getIntExtra("VIDEO_SIZE_HEIGHT", 0) ?: 0
        //屏幕宽度
        screenWidth = intent?.getIntExtra("SCREEN_WIDTH", 0) ?: 0
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //获取视频尺寸和屏幕宽度
        videoSizeWidth = intent?.getIntExtra("VIDEO_SIZE_WIDTH", 0) ?: 0
        videoSizeHeight = intent?.getIntExtra("VIDEO_SIZE_HEIGHT", 0) ?: 0
        screenWidth = intent?.getIntExtra("SCREEN_WIDTH", 0) ?: 0
        //动态设置尺寸
        videoSizeWidthD = screenWidth / 2
        videoSizeHeightD = (videoSizeWidthD * (videoSizeHeight.toFloat() / videoSizeWidth)).toInt()
        mParams?.width = videoSizeWidthD
        mParams?.height = videoSizeHeightD + 100
        mWindowManager?.updateViewLayout(mFloatingView, mParams!!)

        return super.onStartCommand(intent, flags, startId)
    }



    @OptIn(UnstableApi::class)
    @SuppressLint("RtlHardcoded", "InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager?
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.player_floating_window, null)



        //关闭按钮
        val closeBtn = mFloatingView!!.findViewById<ImageButton?>(R.id.close_btn)
        closeBtn?.setOnClickListener {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("LOCAL_RECEIVER").apply {
                putExtra("key", "PLAYER_PAUSE")
            })
            stopSelf()
        }
        //打开按钮
        val openBtn = mFloatingView!!.findViewById<ImageButton?>(R.id.open_btn)
        openBtn?.setOnClickListener {
            if (isFolded) {
                mParams?.width = videoSizeWidthD
                mParams?.height = videoSizeHeightD + 100
                mWindowManager?.updateViewLayout(mFloatingView, mParams!!)
                isFolded = false
            }else {
                val intent = Intent(this, PlayerActivityMVVM::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }.putExtra("SOURCE","FROM_PENDING" )
                startActivity(intent)
            }


        }
        //移动按钮
        val moveBtn = mFloatingView!!.findViewById<ImageButton?>(R.id.move_btn)
        moveBtn!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ACTION_DOWN_F_X = event.rawX.toInt()
                    ACTION_DOWN_F_Y = event.rawY.toInt()
                    ACTION_DOWN_W_X = mParams?.x ?: 0
                    ACTION_DOWN_W_Y = mParams?.y ?: 0

                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    //手指实时位置
                    ACTION_MOVE_X = event.rawX.toInt()
                    ACTION_MOVE_Y = event.rawY.toInt()
                    //计算移动距离
                    val moveX = ACTION_MOVE_X - ACTION_DOWN_F_X
                    val moveY = ACTION_MOVE_Y - ACTION_DOWN_F_Y
                    //更新窗口位置
                    mParams?.x = ACTION_DOWN_W_X - moveX
                    mParams?.y = ACTION_DOWN_W_Y - moveY
                    mWindowManager?.updateViewLayout(mFloatingView, mParams!!)

                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
        //悬浮窗拖动
        val topBar = mFloatingView!!.findViewById<LinearLayout?>(R.id.top_bar)
        topBar!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ACTION_DOWN_F_X = event.rawX.toInt()
                    ACTION_DOWN_F_Y = event.rawY.toInt()
                    ACTION_DOWN_W_X = mParams?.x ?: 0
                    ACTION_DOWN_W_Y = mParams?.y ?: 0

                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    //手指实时位置
                    ACTION_MOVE_X = event.rawX.toInt()
                    ACTION_MOVE_Y = event.rawY.toInt()
                    //计算移动距离
                    val moveX = ACTION_MOVE_X - ACTION_DOWN_F_X
                    val moveY = ACTION_MOVE_Y - ACTION_DOWN_F_Y
                    //更新窗口位置
                    mParams?.x = ACTION_DOWN_W_X - moveX
                    mParams?.y = ACTION_DOWN_W_Y - moveY
                    mWindowManager?.updateViewLayout(mFloatingView, mParams!!)

                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
        //切换浮窗大小按钮
        val resizeBtn = mFloatingView!!.findViewById<ImageButton?>(R.id.resize_btn)
        resizeBtn?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ACTION_DOWN_F_X = event.rawX.toInt()
                    ACTION_DOWN_F_Y = event.rawY.toInt()
                    ACTION_DOWN_W_X = mParams?.x ?: 0
                    ACTION_DOWN_W_Y = mParams?.y ?: 0
                    originWidth = mParams?.width ?: 0
                    originHeight = mParams?.height ?: 0

                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    ACTION_MOVE_X = event.rawX.toInt()
                    ACTION_MOVE_Y = event.rawY.toInt()

                    val moveX = ACTION_MOVE_X - ACTION_DOWN_F_X
                    val moveY = ACTION_MOVE_Y - ACTION_DOWN_F_Y

                    mParams?.width = originWidth - moveX
                    mParams?.height = originHeight - moveY

                    if (mParams?.width!! <= 350) { mParams?.width = 350 }
                    if (mParams?.height!! <= 90){ mParams?.height = 90 }

                    if (mParams?.height!! == 90) { isFolded = true }else { isFolded = false }

                    mWindowManager?.updateViewLayout(mFloatingView, mParams!!)
                }
            }
            return@setOnTouchListener false
        }



        //简单切换尺寸（onClickListener）
        /*
        if (sizeSign == 0) {
            sizeSign = 1
            mParams?.width = 300
            mParams?.height = 300
        }else if (sizeSign == 1) {
            sizeSign = 2
            mParams?.width = 400
            mParams?.height = 400
        }else if (sizeSign == 2) {
            sizeSign = 0
            mParams?.width = 500
            mParams?.height = 500
        }
        mWindowManager?.updateViewLayout(mFloatingView, mParams!!)
        */


        //LayoutParams
        val LAYOUT_FLAG: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        mParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT
        )

        //初始位置:以屏幕右下角为锚点
        mParams!!.gravity = Gravity.BOTTOM or Gravity.RIGHT
        mParams!!.x = 50
        mParams!!.y = 50


        mWindowManager!!.addView(mFloatingView, mParams)


        val player = PlayerExoSingleton._player ?: throw IllegalStateException("Player not initialized")

        val playerView = mFloatingView!!.findViewById<View?>(R.id.player_view) as PlayerView
        playerView.player = player


    }

    override fun onDestroy() {
        super.onDestroy()
        if (mFloatingView != null && mWindowManager != null) {
            mWindowManager!!.removeView(mFloatingView)
        }
    }
}