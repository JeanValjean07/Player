package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class DeviceInfoActivity: AppCompatActivity() {
    //内存值
    private var MemMaxByte = 0L
    private var MemAvailByte = 0L
    private var MemNowByte = 0L
    private lateinit var memoryMax: TextView
    private lateinit var memoryUsed: TextView
    private lateinit var memoryAvailable: TextView
    //服务状态
    private var state_MemoryFreshRunning = false
    private var state_FloatWindowRunning = false
    //设置
    private lateinit var PREFS_DeviceInfo: SharedPreferences
    private var PREFS_MemoryFreshGap = 0L


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_device_info)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //设置读取
        PREFS_DeviceInfo = getSharedPreferences("PREFS_DeviceInfo", MODE_PRIVATE)
        if (PREFS_DeviceInfo.contains("PREFS_MemoryFreshGap")) {
            PREFS_MemoryFreshGap = PREFS_DeviceInfo.getLong("PREFS_MemoryFreshGap", 1000L)
        }else{
            PREFS_DeviceInfo.edit { putLong("PREFS_MemoryFreshGap", 1000L) }
            PREFS_MemoryFreshGap = 1000L
        }


        //按钮：返回
        val ButtonBack = findViewById<ImageButton>(R.id.buttonExit)
        ButtonBack.setOnClickListener {
            finish()
        }
        //按钮：开启内存刷新
        val ButtonMemoryFresh = findViewById<MaterialButton>(R.id.ButtonMemory)
        ButtonMemoryFresh.setOnClickListener {
            if(state_MemoryFreshRunning) {
                stopMemRefresh()
            }else{
                startMemRefresh()
            }
        }


        startMemRefresh()




    }

    override fun onDestroy() {
        super.onDestroy()
        stopMemRefresh()
    }

    //内存刷新循环
    //Runnable:MemRefresh
    private val MemRefreshHandler = Handler(Looper.getMainLooper())
    private var MemRefresh = object : Runnable{
        override fun run() {
            MemAvailByte = getMemoryAvail()
            MemNowByte = MemMaxByte - MemAvailByte

            setMemValue()

            MemRefreshHandler.postDelayed(this, PREFS_MemoryFreshGap)
        }
    }
    private fun startMemRefresh() {
        state_MemoryFreshRunning = true
        initMemInfo()
        val ButtonMemoryFresh = findViewById<MaterialButton>(R.id.ButtonMemory)
        ButtonMemoryFresh.text = "停止内存刷新"
        //获取总值
        MemMaxByte = getMemoryMax()
        //启动循环
        MemRefreshHandler.post(MemRefresh)
    }
    private fun stopMemRefresh() {
        state_MemoryFreshRunning = false
        val ButtonMemoryFresh = findViewById<MaterialButton>(R.id.ButtonMemory)
        ButtonMemoryFresh.text = "开启内存刷新"
        MemRefreshHandler.removeCallbacks(MemRefresh)
    }
    //内存读取
    private fun getMemoryMax(): Long {
        val actMgr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info   = ActivityManager.MemoryInfo().also { actMgr.getMemoryInfo(it) }
        return info.totalMem
    }
    private fun getMemoryAvail(): Long {
        val actMgr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info   = ActivityManager.MemoryInfo().also { actMgr.getMemoryInfo(it) }
        return info.availMem
    }
    //内存数值上报显示
    private fun initMemInfo() {
        memoryMax = findViewById(R.id.memory_max)
        memoryUsed = findViewById(R.id.memory_used)
        memoryAvailable = findViewById(R.id.memory_available)
    }
    @SuppressLint("SetTextI18n")
    private fun setMemValue() {
        memoryMax.text = "%.2f GB".format(MemMaxByte / (1024.0 * 1024 * 1024))
        memoryUsed.text = "%.2f GB".format(MemNowByte / (1024.0 * 1024 * 1024))
        memoryAvailable.text = "%.2f GB".format(MemAvailByte / (1024.0 * 1024 * 1024))
    }


}