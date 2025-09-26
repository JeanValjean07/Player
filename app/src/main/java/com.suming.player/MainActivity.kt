package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suming.player.MainActivity.DeviceCompatUtil.isCompatibleDevice
import data.source.LocalVideoSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity() {

    //注册adapter
    private lateinit var adapter: MainActivityAdapter
    //设置
    private var PREFS_S_UseMVVMPlayer = false
    //兼容性1检查
    private var isCompatibleDevice = false
    //权限检查
    private val REQUEST_STORAGE_PERMISSION = 1001

    //无法打开视频时的接收器
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.getStringExtra("key") == "needRefresh") {
                notice("这条视频似乎无法播放", 3000)
                load()
            }
        }
    }
    //旧机型兼容判断
    object DeviceCompatUtil {
        /*
        private val SOC_MAP = mapOf(
            "kirin710" to 700,
            "kirin970" to 970,
            "kirin980" to 980,
            "kirin990" to 990,
            "kirin9000" to 1000,

            "msm8998"  to 835,
            "sdm845"   to 845,
        )
        */
        fun isCompatibleDevice(): Boolean {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val hw = Build.HARDWARE.lowercase()
                //val soc = SOC_MAP.entries.find { hw.contains(it.key) }?.value ?: return false
                return when {
                    hw.contains("kirin") -> return true
                    hw.contains("sdm") -> return true       //暂不完善soc细分判断
                    else -> false
                }
            }else{
                return false
            }
        }
    }



    //生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //读取设置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if (!prefs.contains("PREFS_UseMVVMPlayer")){
            prefs.edit { putBoolean("PREFS_UseMVVMPlayer", true).apply() }
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        } else{
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        }
        //预读取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            if (!prefs.contains("INFO_STATUSBAR_HEIGHT")){
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
                    val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    Log.d("SuMing", "读取状态栏高度: $statusBarHeight")
                    prefs.edit { putInt("INFO_STATUSBAR_HEIGHT", statusBarHeight).apply() }
                    insets
                }
            }

            insets
        }

        //准备工作+加载视频
        preCheck()
        load()

        //按钮：刷新列表
        val button1 = findViewById<FloatingActionButton>(R.id.fab)
        button1.setOnClickListener {
            runOnUiThread { adapter.refresh() }
            val recyclerview1 = findViewById<RecyclerView>(R.id.recyclerview1)
            recyclerview1.smoothScrollToPosition(0)
        }
        //按钮：指南
        val button2 = findViewById<Button>(R.id.buttonGuidance)
        button2.setOnClickListener {
            val intent = Intent(this, GuidanceActivity::class.java)
            if (isCompatibleDevice){
                intent.putExtra("deviceInfo", "old")
            }
            startActivity(intent)
        }
        //按钮：设置
        val buttonSettings= findViewById<Button>(R.id.buttonSetting)
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.noticeCard)
        noticeCard.setOnClickListener {
            noticeCard.visibility = View.GONE
        }



        //监听返回手势
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                Handler(Looper.getMainLooper()).postDelayed({
                    val pid = Process.myPid()
                    Process.killProcess(pid)
                }, 500)
            }
        })

    }//onCreate END

    override fun onResume() {
        super.onResume()
        //重读设置
        val prefs = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if (!prefs.contains("PREFS_UseMVVCPlayer")){
            prefs.edit { putBoolean("PREFS_UseMVVCPlayer", true).apply() }
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        } else{
            PREFS_S_UseMVVMPlayer = prefs.getBoolean("PREFS_UseMVVMPlayer", false)
        }
    }





    //Functions
    private fun preCheck(){
        //申请媒体权限
        lifecycleScope.launch {
            delay(2000)
            val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(requiredPermission),
                    REQUEST_STORAGE_PERMISSION
                )
                notice("需要访问媒体权限来读取视频,授权后请手动刷新", 5000)
            }
        }

        //兼容性检查
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
            isCompatibleDevice = isCompatibleDevice()
            if (isCompatibleDevice){
                notice("\"指南\"页面有关于您设备兼容性的消息,请前往查看", 10000)
            }
        }
    }
    @OptIn(UnstableApi::class)
    //加载视频列表+点击事件处理
    private fun load(){
        val pager = Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LocalVideoSource(contentResolver, this) }
        )

        val recyclerview1 = findViewById<RecyclerView>(R.id.recyclerview1)
        recyclerview1.layoutManager = LinearLayoutManager(this)


        //注册点击事件
        adapter = MainActivityAdapter(
            onItemClick = { item ->
                if (PREFS_S_UseMVVMPlayer){
                    val intent = Intent(this, PlayerActivityMVVM::class.java).apply { putExtra("video", item) }
                    detailLauncher.launch(intent)
                }
                else{
                    val intent = Intent(this, PlayerActivity::class.java).apply { putExtra("video", item) }
                    detailLauncher.launch(intent)
                }
            },
            onDurationClick = { item ->
                notice("视频时长:${formatTime1(item.durationMs)}", 2000)
            },
            onOptionClick = { item ->
                val popup = PopupMenu(this, recyclerview1)
                popup.menuInflater.inflate(R.menu.activity_main_menu, popup.menu)
                popup.setOnMenuItemClickListener { /*handle*/; true }
                popup.show()
            }
        )


        recyclerview1.adapter = adapter

        lifecycleScope.launch {
            pager.flow.collect { adapter.submitData(it) }
        }
    }

    //显示通知
    private var showNoticeJob: Job? = null
    private fun showNoticeJob(text: String, duration: Long) {
        showNoticeJob?.cancel()
        showNoticeJob = lifecycleScope.launch {
            val notice = findViewById<TextView>(R.id.notice)
            val noticeCard = findViewById<CardView>(R.id.noticeCard)
            noticeCard.visibility = View.VISIBLE
            notice.text = text
            delay(duration)
            noticeCard.visibility = View.GONE
        }
    }
    private fun notice(text: String, duration: Long) {
        showNoticeJob(text, duration)
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


}//class END
