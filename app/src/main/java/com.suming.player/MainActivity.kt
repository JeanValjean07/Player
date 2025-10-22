package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import data.model.VideoItem
import data.source.LocalVideoSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity() {

    //注册adapter
    private lateinit var adapter: MainActivityAdapter
    //权限检查
    private val REQUEST_STORAGE_PERMISSION = 1001
    //状态栏高度
    private var statusBarHeight = 0

    //无法打开视频时的接收器
    @SuppressLint("UnsafeOptInUsageError")
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.getStringExtra("key") == "NEED_REFRESH") {
                notice("这条视频似乎无法播放", 3000)
                load()
            }
            else if (result.data?.getStringExtra("NEED_CLOSE") == "NEED_CLOSE") {
                PlayerExoSingleton.stopPlayer()
            }
            else if (result.data?.getStringExtra("HAS_CLOSED") == "HAS_CLOSED") {
                notice("该视频已关闭", 2000)
                PlayerExoSingleton.stopPlayer()
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
        val prefs = getSharedPreferences("PREFS", MODE_PRIVATE)
        //内容避让状态栏并预读取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            if (!prefs.contains("INFO_STATUSBAR_HEIGHT")){
                statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                prefs.edit { putInt("INFO_STATUSBAR_HEIGHT", statusBarHeight).apply() }
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


        /*
        //视频项关闭时的处理
        if (intent.getBooleanExtra("ITEM_CLOSED", false)) {
            notice("已关闭该视频", 2000)
        }

         */



        //监听返回手势
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }//onCreate END


    override fun onResume() {
        super.onResume()
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
                startPlayer(item)
            },
            onDurationClick = { item ->
                notice("视频时长:${formatTime1(item.durationMs)}", 2000)
            },
            onOptionClick = { item ->
                val popup = PopupMenu(this, recyclerview1)
                popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                popup.setOnMenuItemClickListener { /*handle*/; true }
                popup.show()
            }
        )


        recyclerview1.adapter = adapter

        lifecycleScope.launch {
            pager.flow.collect { adapter.submitData(it) }
        }
    }

    private fun BroadcastFinish(): Intent {
        val intent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = "PLAYER_FINISH"
        }
        return intent
    }

    @OptIn(UnstableApi::class)
    private fun startPlayer(item: VideoItem){
        val intent = Intent(this, PlayerActivity::class.java).apply { putExtra("video", item) }
        detailLauncher.launch(intent)
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
