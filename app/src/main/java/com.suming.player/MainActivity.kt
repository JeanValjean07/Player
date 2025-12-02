package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import android.widget.Toast
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
import androidx.paging.PagingSource
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import data.MediaModel.MediaItemForVideo
import data.MediaDataReader.MediaStoreReaderForVideo
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaStore.MediaStoreRepo
import data.DataBaseMediaStore.MediaStoreSetting
import data.MediaDataReader.MediaDataBaseReaderForVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("unused")
class MainActivity: AppCompatActivity() {

    //成员变量
    private lateinit var adapter: MainActivityAdapter
    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFS_MediaStore: SharedPreferences
    private lateinit var PREFS_Main: SharedPreferences

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

    //是否已读取媒体库
    private var state_MediaStoreReaded = false

    //界面内容
    private lateinit var loadingCard: CardView
    private lateinit var loadingText: TextView


    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_old)
        //界面实例获取
        preCheckAndInit()

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
        //内容避让状态栏并预读取状态栏高度写入设置
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
        PREFS_Main = getSharedPreferences("PREFS_Main", MODE_PRIVATE)


        if (!PREFS_Main.contains("state_MediaStoreReaded")){
            PREFS_Main.edit { putBoolean("state_MediaStoreReaded", false).apply() }
            state_MediaStoreReaded = false
        }else{
            state_MediaStoreReaded = PREFS_Main.getBoolean("state_MediaStoreReaded", false)
        }

        if (!state_MediaStoreReaded){
            loadFromMediaStore()
        }else{
            loadFromDataBase()
        }






        //按钮：刷新列表
        val ButtonRefresh = findViewById<Button>(R.id.buttonRefresh)
        ButtonRefresh.setOnClickListener {
            vibrate()
            notice("仅从本地数据库刷新,如需重新读取本机媒体,请点击\"安卓媒体库\"页签的设置齿轮", 5000)
            loadFromDataBase()
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

            }
            if (ReceiveKey == "ReLoadFromMediaStore"){
                loadFromMediaStore()
            }
        }
        //监听返回手势
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    //onCreate END
    }

    //错误接收器:detailLauncher
    @SuppressLint("UnsafeOptInUsageError")
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            when(result.data?.getStringExtra("key")){
                "NEED_REFRESH" -> {
                    //loadFromDataBase()
                }
                "NEED_CLOSE" -> {
                    //PlayerSingleton.clearMediaItem()
                }
            }
        }
    }
    //onResume时更新一些设置变量
    override fun onResume() {
        super.onResume()
        PREFS_UsePlayerWithSeekBar = PREFS.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        PREFS_UseTestingPlayer = PREFS.getBoolean("PREFS_UseTestingPlayer", false)
    }



    //Functions
    private fun preCheckAndInit(){
        //界面实例获取
        loadingCard = findViewById(R.id.loadingCard)
        loadingText = findViewById(R.id.loading)
        //事件总线注册
        setupEventBus()

    }
    @OptIn(UnstableApi::class)
    //显示/隐藏加载卡片
    private fun showLoadingCard(){
        loadingText.text = "正在加载媒体"
        loadingCard.visibility = View.VISIBLE
    }
    private fun showNeedPermission(){
        loadingText.text = "需要授予媒体访问权限后才能读取"
        loadingCard.visibility = View.VISIBLE
    }
    private fun closeLoadingCard(){
        loadingText.text = "加载完成"
        Handler().postDelayed({
            loadingCard.visibility = View.GONE
        }, 500)
    }
    //从媒体库接口读取
    private fun startLoadFromMediaStore(){
        showLoadingCard()
        //发起后台线程加载
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaReader = MediaStoreReaderForVideo(this@MainActivity, contentResolver)
            mediaReader.readAndSaveAllVideos()
        }
    }
    private fun loadFromMediaStore(){
        lifecycleScope.launch(Dispatchers.IO) {
            //读取之前不能清库,否则丢失隐藏属性
            //MediaStoreRepo.get(this@MainActivity).clearAll()
            withContext(Dispatchers.Main){
                //权限检查
                val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                //权限未授予
                if (ContextCompat.checkSelfPermission(this@MainActivity, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(requiredPermission),
                        REQUEST_STORAGE_PERMISSION
                    )
                    //权限提示
                    showNeedPermission()

                    startCheckPermission()
                }
                //权限已授予
                else{
                    startLoadFromMediaStore()
                }
            }
        }
    }
    //从本地数据库加载
    private fun loadFromDataBase() {

        val recyclerview1 = findViewById<RecyclerView>(R.id.recyclerview1)
        recyclerview1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)


        //注册adapter
        adapter = MainActivityAdapter(
            context = this,
            onItemClick = { item ->
                startPlayer(item)
            },
            onDurationClick = { item ->
                notice("视频时长:${FormatTime_withChar(item.durationMs)}", 2000)
            },
            onOptionClick = { item ->
                val popup = PopupMenu(this, recyclerview1)
                popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                popup.setOnMenuItemClickListener { /*handle*/; true }
                popup.show()
            },
            onItemHideClick = { filename,flag_need_hide ->
                HideItem(filename)
            }
        )
        recyclerview1.adapter = adapter


        val pager = Pager(PagingConfig(pageSize = 20)) {
            MediaDataBaseReaderForVideo(context = this@MainActivity)
        }

        lifecycleScope.launch {
            pager.flow.collect { pagingData ->
                adapter.submitData(pagingData)
            }
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
    private fun startPlayer(item: MediaItemForVideo){
        vibrate()
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
    private fun HideItem(filename: String) {
        lifecycleScope.launch {
            val isHidden = MediaStoreRepo.get(this@MainActivity).getHideStatus(filename) ?: return@launch

            if (isHidden){
                showCustomToast("已取消隐藏", Toast.LENGTH_SHORT, 3)
            }
            else{
                showCustomToast("已隐藏", Toast.LENGTH_SHORT, 3)
            }
            MediaStoreRepo.get(this@MainActivity).updateHiddenStatus(filename,!isHidden)

            withContext(Dispatchers.Main){
                loadFromDataBase()
            }
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
                checkPermissionHandler.postDelayed(this, 100)
            }else{
                //进行最终读取操作
                startLoadFromMediaStore()
            }
        }
    }
    private fun startCheckPermission() {
        checkPermissionHandler.post(checkPermission)
    }
    private fun stopCheckPermission() {
        checkPermissionHandler.removeCallbacks(checkPermission)
    }
    //RxJava事件总线
    private var disposable: io.reactivex.rxjava3.disposables.Disposable? = null
    private fun setupEventBus() {
        disposable = ToolEventBus.events
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({
                handlePlayerEvent(it)
            }, {
                showCustomToast("事件总线注册失败:${it.message}", Toast.LENGTH_SHORT,3)
            })
    }
    private fun handlePlayerEvent(event: String) {
        when (event) {
            "MediaStore_Video_Query_Complete" -> {
                //关提示卡
                closeLoadingCard()
                //修改是否完成过加载记录
                saveLoadState()
                //数据已经保存到数据库,开始从数据库解析
                loadFromDataBase()
            }
        }
    }
    //保存加载状态
    private fun saveLoadState() {
        PREFS_Main.edit { putBoolean("state_MediaStoreReaded", true).apply() }
    }

//class END
}

