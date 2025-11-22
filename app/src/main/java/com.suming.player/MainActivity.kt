package com.suming.player

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
import data.MediaModel.MediaItem_video
import data.MediaDataReader.MediaReader_video
import data.MediaItemRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.recyclerview.widget.DefaultItemAnimator

@Suppress("unused")
class MainActivity: AppCompatActivity() {

    //成员变量
    private lateinit var adapter: MainActivityAdapter
    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFS_MediaStore: SharedPreferences

    //权限检查
    private val REQUEST_STORAGE_PERMISSION = 1001
    //状态栏高度
    private var statusBarHeight = 0
    //震动时间
    private var PREFS_VibrateMillis = 0L
    private var PREFS_UseSysVibrate = false
    //基本设置项
    private var PREFS_UsePlayerWithSeekBar = false
    private var PREFS_UseTestingPlayer = false






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
                PlayerSingleton.clearMediaItem()
            }
            else if (result.data?.getStringExtra("HAS_CLOSED") == "HAS_CLOSED") {
                notice("该视频已关闭", 2000)
                PlayerSingleton.clearMediaItem()
            }
        }
    }

    //生命周期
    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_old)

        //读取设置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        if (!PREFS.contains("PREFS_VibrateMillis")){
            PREFS.edit { putLong("PREFS_VibrateMillis", 10L).apply() }
            PREFS_VibrateMillis = 10L
        }else{
            PREFS_VibrateMillis = PREFS.getLong("PREFS_VibrateMillis", 10L)
        }
        if (!PREFS.contains("PREFS_UseSysVibrate")){
            PREFS.edit { putBoolean("PREFS_UseSysVibrate", true).apply() }
            PREFS_UseSysVibrate = true
        }else{
            PREFS_UseSysVibrate = PREFS.getBoolean("PREFS_UseSysVibrate", true)
        }

        if (!PREFS.contains("PREFS_UseTestingPlayer")){
            PREFS.edit { putBoolean("PREFS_UseTestingPlayer", false).apply() }
            PREFS_UseTestingPlayer = false
        }else{
            PREFS_UseTestingPlayer = PREFS.getBoolean("PREFS_UseTestingPlayer", false)
        }

        //基于设备信息
        if (!PREFS.contains("PREFS_UsePlayerWithSeekBar")){
            if (Build.BRAND.equals("xiaomi",ignoreCase = true) || Build.BRAND.equals("redmi",ignoreCase = true)){
                PREFS.edit { putBoolean("PREFS_UsePlayerWithSeekBar", true).apply() }
                PREFS_UsePlayerWithSeekBar = true
            }else{
                PREFS.edit { putBoolean("PREFS_UsePlayerWithSeekBar", false).apply() }
                PREFS_UsePlayerWithSeekBar = false
            }
        }else{
            PREFS_UsePlayerWithSeekBar = PREFS.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        }

        //基于设备信息的预写入
        if (!PREFS.contains("PREFS_EnablePlayAreaMove")){
            if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", false).apply() }
            }else{
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", true).apply() }
            }
        }
        if (!PREFS.contains("PREFS_UseHighRefreshRate")) {
            if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                PREFS.edit { putBoolean("PREFS_UseHighRefreshRate", true).apply() }
            }else{
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", false).apply() }
            }
        }

        //内容避让状态栏并预读取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            if (!PREFS.contains("INFO_STATUSBAR_HEIGHT")){
                statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                PREFS.edit { putInt("INFO_STATUSBAR_HEIGHT", statusBarHeight).apply() }
            }
            insets
        }
        //读取媒体库设置
        PREFS_MediaStore = getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
        if (!PREFS_MediaStore.contains("show_hide_items")){
            PREFS_MediaStore.edit { putBoolean("show_hide_items", false).apply() }
        }else{
            PREFS_MediaStore.edit { putBoolean("show_hide_items", false).apply() }
        }

        //准备工作+加载视频
        preCheck()

        //加载
        load()

        /*
        val uri = "content://media/external/video/media/781".toUri()

        PlayerSingleton.addInitializationCallback {
            runOnUiThread {
                Log.d("SuMing", "播放器已初始化完成，可以进行操作")
                PlayerSingleton.setMediaUri(uri)

            }
        }

         */


        PlayerSingleton.getPlayer(application)



        //按钮：刷新列表
        val ButtonRefresh = findViewById<Button>(R.id.buttonRefresh)
        ButtonRefresh.setOnClickListener {
            vibrate()
            runOnUiThread { adapter.refresh() }
            val recyclerview1 = findViewById<RecyclerView>(R.id.recyclerview1)
            recyclerview1.smoothScrollToPosition(0)
        }
        //按钮：指南
        val button2 = findViewById<Button>(R.id.buttonGuidance)
        button2.setOnClickListener {
            vibrate()
            val intent = Intent(this, GuidanceActivity::class.java)
            startActivity(intent)
        }
        //按钮：设置
        val buttonSettings= findViewById<Button>(R.id.buttonSetting)
        buttonSettings.setOnClickListener {
            vibrate()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        //提示卡点击时关闭
        val noticeCard = findViewById<CardView>(R.id.noticeCard)
        noticeCard.setOnClickListener {
            vibrate()
            noticeCard.visibility = View.GONE
        }
        //按钮：安卓媒体库设置
        val ButtonMediaStoreSettings = findViewById<ImageButton>(R.id.ButtonMediaStoreSettings)
        ButtonMediaStoreSettings.setOnClickListener {
            vibrate()
            MainActivityFragmentMediaStoreSettings.newInstance().show(supportFragmentManager, "MainActivityFragmentMediaStoreSettings")
        }
        //显示隐藏的视频
        val gestureDetectorToolbarTitle = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                vibrate()
                //逻辑修改
                val show_hide_items = PREFS_MediaStore.getBoolean("show_hide_items", false)
                if (show_hide_items){
                    PREFS_MediaStore.edit { putBoolean("show_hide_items", false).apply() }
                    notice("不显示隐藏的视频,刷新后生效", 2000)
                }else{
                    PREFS_MediaStore.edit { putBoolean("show_hide_items", true).apply() }
                    notice("将显示隐藏的视频,刷新后生效", 2000)
                }
                super.onLongPress(e)
            }
        })
        val ToolbarTitle = findViewById<TextView>(R.id.toolbar_title)
        ToolbarTitle.setOnTouchListener { _, event ->
            gestureDetectorToolbarTitle.onTouchEvent(event)
        }



        //媒体库设置返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_MediaStore", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            if (ReceiveKey == "Refresh Now"){
                load()
            }


        }



        //监听返回手势
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }//onCreate END

    override fun onResume() {
        super.onResume()
        PREFS_UsePlayerWithSeekBar = PREFS.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        PREFS_UseTestingPlayer = PREFS.getBoolean("PREFS_UseTestingPlayer", false)
    }


    //Functions
    private fun preCheck(){
        //申请媒体权限
        lifecycleScope.launch {
            delay(300)
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
                startCheckPermission()
            }
        }

    }
    @OptIn(UnstableApi::class)
    //加载视频列表+点击事件处理
    private fun load(){
        val pager = Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { MediaReader_video(context = this@MainActivity,contentResolver) }
        )

        val recyclerview1 = findViewById<RecyclerView>(R.id.recyclerview1)
        recyclerview1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
       // recyclerview1.animation = DefaultItemAnimator()


        //注册adapter
        adapter = MainActivityAdapter(
            context = this,
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
            },
            onItemHideClick = { filename,flag_need_hide ->
                HideItem(filename,flag_need_hide)
            }
        )
        recyclerview1.adapter = adapter
        lifecycleScope.launch {
            pager.flow.collect { adapter.submitData(it) }
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
        if (PREFS_VibrateMillis <= 0L) {
            return
        }
        val vib = this@MainActivity.vibrator()
        if (PREFS_UseSysVibrate) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vib.vibrate(effect)
        }
        else{
            vib.vibrate(VibrationEffect.createOneShot(PREFS_VibrateMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    //启动播放器
    @OptIn(UnstableApi::class)
    private fun startPlayer(item: MediaItem_video){

        //使用测试播放页
        if (PREFS_UseTestingPlayer){
            val intent = Intent(this, PlayerActivityTest::class.java).apply {
                putExtra("video", item)
            }.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            detailLauncher.launch(intent)
            return
        }

        //使用传统播放页
        if (PREFS_UsePlayerWithSeekBar){
            val intent = Intent(this, PlayerActivitySeekBar::class.java).apply {
                putExtra("video", item)
            }.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            detailLauncher.launch(intent)
        }
        //使用新晋播放页
        else{
            val intent = Intent(this, PlayerActivity::class.java)
                .apply {
                    putExtra("video", item)
                }
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            detailLauncher.launch(intent)
        }
    }
    //隐藏
    private fun HideItem(filename: String,flag_need_hide: Boolean) {
        //更新数据库
        lifecycleScope.launch {
            //根据flag_need_hide来判断是否隐藏
            MediaItemRepo.get(this@MainActivity).update_PREFS_HideThisItem(filename,flag_need_hide)
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
    //Runnable:检查权限循环
    private val checkPermissionHandler = Handler(Looper.getMainLooper())
    private var checkPermission = object : Runnable{
        override fun run() {
            val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(requiredPermission), REQUEST_STORAGE_PERMISSION)
                notice("需要访问媒体权限来读取视频", 1000)
                checkPermissionHandler.postDelayed(this, 100)
            }else{
                notice("媒体权限已授权", 2000)
                load()
            }
        }
    }
    private fun startCheckPermission() {
        checkPermissionHandler.post(checkPermission)
    }
    private fun stopCheckPermission() {
        checkPermissionHandler.removeCallbacks(checkPermission)
    }



//class END
}

