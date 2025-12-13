package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
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
import androidx.media3.ui.PlayerView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaDataReader.MediaDataBaseReaderForMusic
import data.MediaDataReader.MediaDataBaseReaderForVideo
import data.MediaDataReader.MediaStoreReaderForMusic
import data.MediaDataReader.MediaStoreReaderForVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

@Suppress("unused")
@OptIn(UnstableApi::class)
class MainActivity: AppCompatActivity() {
    //界面控件元素
    private lateinit var main_video_list_adapter: MainVideoAdapter
    private lateinit var main_music_list_adapter: MainMusicAdapter
    private lateinit var main_media_list_adapter_RecyclerView: RecyclerView
    private lateinit var loadingCard: CardView
    private lateinit var loadingText: TextView
    private lateinit var title_text: TextView
    private lateinit var ButtonCardMusic: CardView
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardGallery: CardView
    //设置和设置项
    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_ReadNewOnEachStart = false
    private var PREFS_UsePlayerWithSeekBar = false
    private var PREFS_UseTestingPlayer = false
    private var PREFS_DefaultTab = "video"
    //权限检查
    private val REQUEST_STORAGE_PERMISSION = 1001
    //状态栏高度
    private var statusBarHeight = 0
    //状态信息
    private var state_VideoMediaStoreReaded = false
    private var state_MusicMediaStoreReaded = false
    private var state_FromFirstMediaStoreRead = false
    private var state_currentPage = ""
    private var state_lastPage = ""
    private var state_PlayingCard_inited = false
    private var state_onFirstStart = false
    private var state_PlayingCard_showing = false
    private var state_PlayingCard_gone = true
    //播放中卡片
    private lateinit var PlayingCard: CardView
    private lateinit var PlayingCard_MediaName: TextView
    private lateinit var PlayingCard_MediaArtist: TextView
    private lateinit var PlayingCard_Image: ImageView
    private lateinit var PlayingCard_Video: PlayerView
    private lateinit var PlayingCard_Button: ImageButton


    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_old)
        //界面实例获取
        preCheckAndInit()
        //表明首次启动信息
        if (savedInstanceState == null){
            state_onFirstStart = true
        }else{
            state_onFirstStart = false
        }
        //读取播放设置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        if (!PREFS.contains("PREFS_EnablePlayAreaMove")){
            if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", false).apply() }
            }else{
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", true).apply() }
            }
        } //基于设备信息
        if (!PREFS.contains("PREFS_UseHighRefreshRate")) {
            if (Build.BRAND.equals("huawei",ignoreCase = true) || Build.BRAND.equals("honor",ignoreCase = true)){
                PREFS.edit { putBoolean("PREFS_UseHighRefreshRate", true).apply() }
            }else{
                PREFS.edit { putBoolean("PREFS_EnablePlayAreaMove", false).apply() }
            }
        } //基于设备信息
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
        PREFS_MediaStore.edit { putBoolean("PREFS_showHideItems", false).apply() }
        if (PREFS_MediaStore.contains("PREFS_UseTestingPlayer")){
            PREFS_UseTestingPlayer = PREFS_MediaStore.getBoolean("PREFS_UseTestingPlayer", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("PREFS_UseTestingPlayer", false).apply() }
            PREFS_UseTestingPlayer = false
        }
        if (PREFS_MediaStore.contains("PREFS_UsePlayerWithSeekBar")){
            PREFS_UsePlayerWithSeekBar = PREFS_MediaStore.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        }else{
            if (Build.BRAND.equals("xiaomi",ignoreCase = true) || Build.BRAND.equals("redmi",ignoreCase = true)){
                PREFS_MediaStore.edit { putBoolean("PREFS_UsePlayerWithSeekBar", true).apply() }
                PREFS_UsePlayerWithSeekBar = true
            }else{
                PREFS_MediaStore.edit { putBoolean("PREFS_UsePlayerWithSeekBar", false).apply() }
                PREFS_UsePlayerWithSeekBar = false
            }
        } //基于设备信息
        if (PREFS_MediaStore.contains("PREFS_ReadNewOnEachStart")){
            PREFS_ReadNewOnEachStart = PREFS_MediaStore.getBoolean("PREFS_ReadNewOnEachStart", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("PREFS_ReadNewOnEachStart", false).apply() }
            PREFS_ReadNewOnEachStart = false
        }
        if (PREFS_MediaStore.contains("state_VideoMediaStoreReaded")){
            state_VideoMediaStoreReaded = PREFS_MediaStore.getBoolean("state_VideoMediaStoreReaded", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("state_VideoMediaStoreReaded", false).apply() }
            state_VideoMediaStoreReaded = false
        }
        if (PREFS_MediaStore.contains("state_MusicMediaStoreReaded")){
            state_MusicMediaStoreReaded = PREFS_MediaStore.getBoolean("state_MusicMediaStoreReaded", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("state_MusicMediaStoreReaded", false).apply() }
            state_MusicMediaStoreReaded = false
        }
        if (PREFS_MediaStore.contains("PREFS_DefaultTab")){
            PREFS_DefaultTab = PREFS_MediaStore.getString("PREFS_DefaultTab", "video")?: "error"
            if (PREFS_DefaultTab == "error"){
                PREFS_DefaultTab = "video"
                PREFS_MediaStore.edit { putString("PREFS_DefaultTab", "video").apply() }
            }
            if (PREFS_DefaultTab != "video" && PREFS_DefaultTab != "music" && PREFS_DefaultTab != "gallery" && PREFS_DefaultTab != "last"){
                PREFS_DefaultTab = "video"
                PREFS_MediaStore.edit { putString("PREFS_DefaultTab", "video").apply() }
                showCustomToast("修复了默认页签设置", Toast.LENGTH_SHORT, 3)
            }
        }else{
            PREFS_MediaStore.edit { putString("PREFS_DefaultTab", "video").apply() }
            PREFS_DefaultTab = "video"
        }
        if (PREFS_MediaStore.contains("state_lastPage")){
            state_lastPage = PREFS_MediaStore.getString("state_lastPage", "video")?: "error"
            if (state_lastPage == "error"){
                state_lastPage = "video"
                PREFS_MediaStore.edit { putString("state_lastPage", "video").apply() }
            }
            if (state_lastPage != "video" && state_lastPage != "music" && state_lastPage != "gallery"){
                state_lastPage = "video"
                PREFS_MediaStore.edit { putString("state_lastPage", "video").apply() }
                showCustomToast("修复了默认页签设置", Toast.LENGTH_SHORT, 3)
            }
        }else{
            PREFS_MediaStore.edit { putString("state_lastPage", "video").apply() }
            state_lastPage = "video"
        }

        //按钮：指南
        val ButtonGuidance = findViewById<Button>(R.id.buttonGuidance)
        ButtonGuidance.setOnClickListener {

            //val mediaItem = PlayerSingleton.getCurrentMediaItem()
            //Log.d("SuMing", "ButtonGuidance: $mediaItem")


            ToolVibrate().vibrate(this@MainActivity)
            val intent = Intent(this, GuidanceActivity::class.java)
            startActivity(intent)
        }
        //按钮：设置
        val ButtonSettings= findViewById<Button>(R.id.buttonSetting)
        ButtonSettings.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        //提示卡点击时关闭
        val NoticeCard = findViewById<CardView>(R.id.noticeCard)
        NoticeCard.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            NoticeCard.visibility = View.GONE
        }
        //按钮：安卓媒体库设置
        val ButtonMediaStoreSetting = findViewById<ImageButton>(R.id.ButtonMediaStoreSetting)
        ButtonMediaStoreSetting.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)

            MainActivityFragmentMediaStoreSettings.newInstance().show(supportFragmentManager, "MainActivityFragmentMediaStoreSettings")
        }
        //显示隐藏的视频
        val gestureDetectorToolbarTitle = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                ToolVibrate().vibrate(this@MainActivity)
                //逻辑修改
                val show_hide_items = PREFS_MediaStore.getBoolean("PREFS_showHideItems", false)
                if (show_hide_items){
                    PREFS_MediaStore.edit { putBoolean("PREFS_showHideItems", false).apply() }
                    showCustomToast("不显示已被隐藏的视频", Toast.LENGTH_SHORT, 3)
                    //刷新列表
                    main_video_list_adapter.refresh()
                }else{
                    PREFS_MediaStore.edit { putBoolean("PREFS_showHideItems", true).apply() }
                    showCustomToast("显示已被隐藏的视频", Toast.LENGTH_SHORT, 3)
                    //刷新列表
                    main_video_list_adapter.refresh()
                    main_media_list_adapter_RecyclerView.smoothScrollToPosition(0)
                }
                super.onLongPress(e)
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return super.onSingleTapConfirmed(e)
            }
        })
        val ToolbarTitle = findViewById<TextView>(R.id.toolbar_title)
        ToolbarTitle.setOnTouchListener { _, event ->
            gestureDetectorToolbarTitle.onTouchEvent(event)
            true
        }
        //页签
        ButtonCardMusic.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            if (state_currentPage == "music"){
                main_media_list_adapter_RecyclerView.smoothScrollToPosition(0)
                return@setOnClickListener
            }
            main_media_list_adapter_RecyclerView.adapter = null
            setMusicElement()
            resetElement("music")
            state_lastPage = "music"
            PREFS_MediaStore.edit { putString("state_lastPage", "music").apply() }

            //加载业务
            lifecycleScope.launch {
                delay(50)
                withContext(Dispatchers.Main){
                    loadMusic()
                }
            }
        }
        ButtonCardVideo.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            if (state_currentPage == "video"){
                main_media_list_adapter_RecyclerView.smoothScrollToPosition(0)
                return@setOnClickListener
            }
            main_media_list_adapter_RecyclerView.adapter = null
            setVideoElement()
            resetElement("video")
            state_lastPage = "video"
            PREFS_MediaStore.edit { putString("state_lastPage", "video").apply() }

            //加载业务
            lifecycleScope.launch {
                delay(50)
                withContext(Dispatchers.Main){
                    loadVideo()
                }
            }



        }
        ButtonCardGallery.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            if (state_currentPage == "gallery"){
                main_media_list_adapter_RecyclerView.smoothScrollToPosition(0)
                return@setOnClickListener
            }
            main_media_list_adapter_RecyclerView.adapter = null
            setGalleryElement()
            resetElement("gallery")
            state_lastPage = "gallery"
            PREFS_MediaStore.edit { putString("state_lastPage", "gallery").apply() }


            //loadGallery()
        }
        //播放卡片
        PlayingCard = findViewById(R.id.PlayingCard)
        PlayingCard.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            val uri = PlayerSingleton.getMediaInfoUri()
            startPlayer(Uri.parse(uri))
        }
        PlayingCard_Button = findViewById(R.id.PlayingCard_Button)
        PlayingCard_Button.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            if (PlayerSingleton.getIsPlaying()){
                PlayerSingleton.pausePlayer()
            }else{
                PlayerSingleton.playPlayer()
            }
            setPlayingCardButton()
        }


        //判断使用何种页签
        if (savedInstanceState != null){
            state_currentPage = savedInstanceState.getString("state_currentPage", "video")?: "error"
            if (state_currentPage == "video"){
                setVideoElement()
                loadVideo()
            }
            else if (state_currentPage == "music"){
                setMusicElement()
                loadMusic()
            }
            else if (state_currentPage == "gallery"){
                setGalleryElement()
                //loadGallery()
            }
            else{
                setVideoElement()
                loadVideo()
            }
        }
        else{
            if (PREFS_DefaultTab == "last"){
                when (state_lastPage){
                    "video" -> {
                        setVideoElement()
                        loadVideo()
                    }
                    "music" -> {
                        setMusicElement()
                        loadMusic()
                    }
                    "gallery" -> {
                        setGalleryElement()
                        //loadGallery()
                    }
                }
            }
            else if (PREFS_DefaultTab == "video"){
                setVideoElement()
                loadVideo()
            }
            else if (PREFS_DefaultTab == "music"){
                setMusicElement()
                loadMusic()
            }
            else if (PREFS_DefaultTab == "gallery"){
                setGalleryElement()
                //loadGallery()
            }
            else{
                setVideoElement()
                loadVideo()
            }
        }
        //检查上次播放的媒体
        if (savedInstanceState == null){
            getLastMediaItemInfoAndPlay()
        }


        //媒体库设置返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_MediaStore", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                "RefreshByChangeMSSetting" -> {
                    if (state_currentPage == "music"){
                        main_music_list_adapter.refresh()
                    }
                    else if (state_currentPage == "video"){
                        main_video_list_adapter.refresh()
                    }
                }
                "ReLoadFromMediaStore" -> {
                    startReLoad()
                }
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
                "EnsureExitCloseAllStuff" -> {

                }
                "EnsureExitButKeepPlaying" -> {

                }
                "ClosedBeforePlayerReady" -> {
                    Log.d("SuMing", "ClosedBeforePlayerReady : ${result.data?.getStringExtra("ClosedBeforePlayerReady") ?: "null"}")
                }
            }
        }
    }
    //onResume时更新一些设置变量
    override fun onResume() {
        super.onResume()
        PREFS_UsePlayerWithSeekBar = PREFS.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        PREFS_UseTestingPlayer = PREFS.getBoolean("PREFS_UseTestingPlayer", false)

        if (state_onFirstStart){
            state_onFirstStart = false
            //读取上次播放状态
            //getLastMediaItemInfo()

            //startMediaSession()
        }
        //检查播放状态
        checkPlayingMediaThenDecide()
        //刷新按钮
        setPlayingCardButton()

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("state_currentPage", state_currentPage)
    }


    //Functions
    //获取和保存上次播放信息到键值对表
    private fun getLastMediaItemInfoAndPlay(){
        //从键值表读取信息
        val INFO_PlayerSingleton = getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
        val state_MediaType = INFO_PlayerSingleton.getString("state_MediaType", "error") ?: "error"
        var MediaInfo_FileName = INFO_PlayerSingleton.getString("MediaInfo_FileName", "error") ?: "error"
        var MediaInfo_VideoArtist = INFO_PlayerSingleton.getString("MediaInfo_VideoArtist", "error") ?: "error"
        val MediaInfo_VideoUri = INFO_PlayerSingleton.getString("MediaInfo_VideoUri", "error") ?: "error"
        //检查上次播放媒体信息
        if (MediaInfo_VideoUri == "error"){
            closePlayingCard()
            return
        }
        if (MediaInfo_FileName == "error"){
            MediaInfo_FileName = "未知媒体标题"
        }
        if (MediaInfo_VideoArtist == "error"){
            MediaInfo_VideoArtist = "未知艺术家"
        }
        //比对上次播放媒体信息与当前播放媒体信息
        val currentUri = PlayerSingleton.getMediaInfoUri()
        //覆写媒体信息到播放器单例
        PlayerSingleton.setMediaInfo(state_MediaType, MediaInfo_FileName, MediaInfo_VideoArtist, MediaInfo_VideoUri)
        //启动播放
        if (currentUri != MediaInfo_VideoUri){
            PlayerSingleton.getPlayer(application)

            val covers_path = File(filesDir, "miniature/cover")
            val cover_img_path = File(covers_path, "${MediaInfo_FileName.hashCode()}.webp")

            val mediaItem = MediaItem.Builder()
                .setUri(MediaInfo_VideoUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(MediaInfo_FileName)
                        .setArtist(MediaInfo_VideoArtist)
                        .setArtworkUri( Uri.parse(cover_img_path.toString()) )
                        .build()
                )
                .build()

            PlayerSingleton.setMediaItem(mediaItem)
        }
        //链接媒体会话
        PlayerSingleton.connectToMediaSession(application)
        //检查播放状态
        checkPlayingMediaThenDecide()

    }
    private fun saveLastMediaItemInfo(type: String, title: String, artist: String, uri: String){
        val INFO_PlayerSingleton = getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
        INFO_PlayerSingleton.edit {
            putString("state_MediaType", type)
            putString("MediaInfo_FileName", title)
            putString("MediaInfo_VideoArtist", artist)
            putString("MediaInfo_VideoUri", uri)
        }
    }
    //从选项菜单中发起后台播放
    private fun startSmallCardPlay(uri: Uri, title: String){
        //比对上次播放媒体信息与当前播放媒体信息
        val newUri = uri.toString()
        val currentUri = PlayerSingleton.getMediaInfoUri()
        if (newUri == currentUri){
            showCustomToast("已在播放该媒体", Toast.LENGTH_SHORT, 3)
            return
        }
        //设置新播放项
        else{
            PlayerSingleton.getPlayer(application)

            val covers_path = File(filesDir, "miniature/cover")
            val cover_img_path = File(covers_path, "${title.hashCode()}.webp")

            val mediaItem = MediaItem.Builder()
                .setUri(newUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist("未知艺术家")
                        .setArtworkUri( Uri.parse(cover_img_path.toString()) )
                        .build()
                )
                .build()

            PlayerSingleton.setMediaItem(mediaItem)
        }
        //写入新媒体信息到播放器单例
        PlayerSingleton.setMediaInfo("video", title, "未知艺术家", newUri)
        //写入新媒体信息到键值对表
        saveLastMediaItemInfo("video", title, "未知艺术家", newUri)
        //链接媒体会话
        stopService(Intent(this, PlayerService::class.java))
        PlayerSingleton.stopMediaSessionController(application)
        PlayerSingleton.connectToMediaSession(application)
        //检查播放状态
        checkPlayingMediaThenDecide()
    }
    //播放卡片按钮刷新
    private fun setPlayingCardButton(){
        if (PlayerSingleton.getIsPlaying()){
            PlayingCard_Button.setImageResource(R.drawable.ic_main_controller_pause)
        }else{
            PlayingCard_Button.setImageResource(R.drawable.ic_main_controller_play)
        }
    }
    //检查是否有媒体播放并完成接下来所有决策
    private fun checkPlayingMediaThenDecide() {
        if (PlayerSingleton.getExitBeforeRead()){ return }
        val currentMediaItem = PlayerSingleton.getCurrentMediaItem()
        if (currentMediaItem == null){
            closePlayingCardWithAnimation()
        }else{
            showPlayingCardWithAnimation()
            getInfoThenDecide()
        }
    }
    //初始化播放中卡片
    private fun getInfoFromSingleton(): Triple<String, String, String>{
        val (type, title, artist) = PlayerSingleton.getMediaInfoForMain()
        return Triple(type, title, artist)
    }
    private fun getInfoThenDecide(){
        //从播放器单例获取媒体信息
        val (type, title, artist) = getInfoFromSingleton()
        Log.d("SuMing", "type: $type, title: $title, artist: $artist")
        //根据媒体类型判断是否需要绑定播放器
        if (type == "video"){ BindPlayingCardSmallPlayer(type) }
        else if (type == "music"){ BindPlayingCardSmallPlayer(type) }
        //显示播放中卡片
        showPlayingCardWithAnimation()
        //设置文本
        setPlayingCardTextInfo(title, artist)
    }
    private fun BindPlayingCardSmallPlayer(type: String){
        if (!state_PlayingCard_inited){ initPlayingCard() }
        if (!state_PlayingCard_showing){ return }

        if (type == "video"){
            PlayingCard_Image.visibility = View.GONE
            PlayingCard_Video.visibility = View.VISIBLE
            PlayingCard_Video.player = null
            PlayingCard_Video.player = PlayerSingleton.getPlayer(application)
        }
        else if (type == "music"){
            PlayingCard_Image.visibility = View.VISIBLE
            PlayingCard_Video.visibility = View.GONE
        }

    } //绑定播放器或视频视图
    private fun setPlayingCardTextInfo(title: String, artist: String){
        if (!state_PlayingCard_inited){ initPlayingCard() }
        if (!state_PlayingCard_showing){ return }

        PlayingCard_MediaName.text = title
        PlayingCard_MediaArtist.text = artist

    } //设置文本
    private fun showPlayingCardWithAnimation(){
        if (state_PlayingCard_showing){ return }
        state_PlayingCard_showing = true

        PlayingCard.visibility = View.VISIBLE
        PlayingCard.translationY = 300f
        PlayingCard.animate().translationY(0f).setDuration(500).start()
    }
    private fun closePlayingCardWithAnimation(){
        if (!state_PlayingCard_showing){ return }
        state_PlayingCard_showing = false

        PlayingCard.animate().translationY(300f).withEndAction {
            PlayingCard.visibility = View.GONE
        }.setDuration(500).start()
    }
    private fun closePlayingCard(){
        state_PlayingCard_showing = false
        PlayingCard.visibility = View.GONE
    }
    private fun initPlayingCard(){
        PlayingCard_MediaName = findViewById(R.id.PlayingCard_MediaName)
        PlayingCard_MediaArtist = findViewById(R.id.PlayingCard_MediaArtist)
        PlayingCard_Image = findViewById(R.id.PlayingCard_Image)
        PlayingCard_Video = findViewById(R.id.PlayingCard_Video)
        state_PlayingCard_inited = true
    }
    //页签切换
    private fun startReLoad(){
        if (PREFS_DefaultTab == "video"){
            loadFromMediaStoreByCheck("video")
        }else if (PREFS_DefaultTab == "music"){
            loadFromMediaStoreByCheck("music")
        }else{
            loadFromMediaStoreByCheck("video")
            showCustomToast("传入参数错误,传入了${PREFS_DefaultTab}", Toast.LENGTH_SHORT, 3)
        }
    }
    private fun startLoad(){
        if (PREFS_DefaultTab == "video"){
            loadVideo()
        }else if (PREFS_DefaultTab == "music"){
            loadMusic()
        }else{
            showCustomToast("传入参数错误,传入了${PREFS_DefaultTab}", Toast.LENGTH_SHORT, 3)
        }
    }
    private fun loadMusic(){
        //按设置加载音乐
        if (PREFS_ReadNewOnEachStart){
            state_FromFirstMediaStoreRead = true
            loadFromMediaStoreByCheck("video")
        }else{
            if (state_MusicMediaStoreReaded){ loadFromDataBase("music") }
            else{
                state_FromFirstMediaStoreRead = true
                loadFromMediaStoreByCheck("music")
            }
        }

    }
    private fun loadVideo(){

        //按设置加载视频
        if (PREFS_ReadNewOnEachStart){
            state_FromFirstMediaStoreRead = true
            loadFromMediaStoreByCheck("video")
        }else{
            if (state_VideoMediaStoreReaded){ loadFromDataBase("video") }
            else{
                state_FromFirstMediaStoreRead = true
                loadFromMediaStoreByCheck("video")
            }
        }

    }
    private fun setMusicElement(){
        state_currentPage = "music"
        title_text.text = "音乐"
        ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))



    }
    private fun setVideoElement(){
        state_currentPage = "video"
        title_text.text = "视频"
        ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))

    }
    private fun setGalleryElement(){
        state_currentPage = "gallery"
        title_text.text = "陈列架"
        ButtonCardGallery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))
    }
    private fun resetElement(avoid: String){
        if (avoid == "music"){
            ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            ButtonCardGallery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
        }else if (avoid == "video"){
            ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            ButtonCardGallery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
        }else if (avoid == "gallery"){
            ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
        }
    }
    //初始化
    private fun preCheckAndInit(){
        //界面实例获取
        main_media_list_adapter_RecyclerView = findViewById(R.id.recyclerview1)
        loadingCard = findViewById(R.id.loadingCard)
        loadingText = findViewById(R.id.loading)
        title_text = findViewById(R.id.toolbar_title)
        ButtonCardMusic = findViewById(R.id.ButtonCardMusic)
        ButtonCardVideo = findViewById(R.id.ButtonCardVideo)
        ButtonCardGallery = findViewById(R.id.ButtonCardGallery)
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
    private fun startLoadFromMediaStore(flag_video_or_music: String){
        showLoadingCard()
        //发起后台线程加载
        lifecycleScope.launch(Dispatchers.IO) {
            if (flag_video_or_music == "video"){
                val mediaReader = MediaStoreReaderForVideo(this@MainActivity, contentResolver)
                mediaReader.readAndSaveAllVideos()
            }else if(flag_video_or_music == "music"){
                val musicReader = MediaStoreReaderForMusic(this@MainActivity, contentResolver)
                musicReader.readAndSaveAllMusics()
            }else{
                showCustomToast("加载类型输入错误", Toast.LENGTH_SHORT, 3)
            }
        }
    }
    private fun loadFromMediaStoreByCheck(flag_video_or_music: String){
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
                    startLoadFromMediaStore(flag_video_or_music)
                }
            }
        }
    }
    //从本地数据库加载+绑定列表+点击事件
    private fun loadFromDataBase(flag_video_or_music: String) {
        //recyclerview设置布局管理器
        main_media_list_adapter_RecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //使用视频adapter
        if (flag_video_or_music == "video"){
            //注册点击事件
            main_video_list_adapter = MainVideoAdapter(
                context = this,
                onItemClick = { uri ->
                    startPlayer(uri)
                },
                onDurationClick = { item ->
                    ToolVibrate().vibrate(this@MainActivity)
                    notice("视频时长:${FormatTime_withChar(item.durationMs)}", 2000)
                },
                onFormatClick = { item,format ->
                    ToolVibrate().vibrate(this@MainActivity)
                    notice("视频格式:${item.format}", 3000)
                },
                onOptionClick = { item ->
                    ToolVibrate().vibrate(this@MainActivity)
                    val popup = PopupMenu(this, main_media_list_adapter_RecyclerView)
                    popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                    popup.setOnMenuItemClickListener { /*handle*/; true }
                    popup.show()
                },
                onItemHideClick = { uri,flag_need_hide ->
                    HideItem(uri, flag_need_hide)
                },
                onSmallCardPlay = { uri, title ->
                    startSmallCardPlay(uri, title)
                }
            )
            //设置adapter
            main_media_list_adapter_RecyclerView.adapter = main_video_list_adapter
            //分页加载
            val pager = Pager(PagingConfig(pageSize = 20)) {
                MediaDataBaseReaderForVideo(context = this@MainActivity)
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    main_video_list_adapter.submitData(pagingData)
                }
            }

        }
        //使用音乐adapter
        else if(flag_video_or_music == "music"){
            //注册点击事件
            main_music_list_adapter = MainMusicAdapter(
                context = this
            )
            //设置adapter
            main_media_list_adapter_RecyclerView.adapter = main_music_list_adapter
            //分页加载
            val pager = Pager(PagingConfig(pageSize = 20)) {
                MediaDataBaseReaderForMusic(context = this@MainActivity)
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    main_music_list_adapter.submitData(pagingData)
                }
            }

        }

    }
    //启动播放器
    @OptIn(UnstableApi::class)
    private fun startPlayer(uri: Uri){
        ToolVibrate().vibrate(this@MainActivity)
        //使用测试播放页
        if (PREFS_UseTestingPlayer){
            /*
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("uri", uri)
            }.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            detailLauncher.launch(intent)
            return

             */
        }
        //使用传统播放页
        if (PREFS_UsePlayerWithSeekBar){
            val intent = Intent(this, PlayerActivitySeekBar::class.java).apply {
                putExtra("uri", uri)
            }
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            detailLauncher.launch(intent)
        }
        //使用新晋播放页
        else{
            val intent = Intent(this, PlayerActivity::class.java)
                .apply {
                    putExtra("uri", uri)
                }
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            detailLauncher.launch(intent)
        }
    }
    //隐藏
    private fun HideItem(uri: Uri, flag_need_hide: Boolean) {
        val uriNumOnly = uri.toString().replace(Regex("[^0-9]"), "")
        if (flag_need_hide){
            showCustomToast("已隐藏", Toast.LENGTH_SHORT, 3)
        }
        else{
            showCustomToast("已取消隐藏", Toast.LENGTH_SHORT, 3)
        }
        //保存到数据库
        lifecycleScope.launch(Dispatchers.IO){
            MediaStoreRepo.get(this@MainActivity).updateHiddenStatus(uriNumOnly,flag_need_hide)
        }
        //刷新列表
        main_video_list_adapter.refresh()
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
                startLoad()
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
                saveLoadState("video")
                //数据已经保存到数据库,开始从数据库解析
                if (state_FromFirstMediaStoreRead){
                    state_FromFirstMediaStoreRead = false
                    loadFromDataBase("video")
                }else{
                    main_video_list_adapter.refresh()
                }
            }
            "MediaStore_Music_Query_Complete" -> {
                //关提示卡
                closeLoadingCard()
                //修改是否完成过加载记录
                saveLoadState("music")
                //数据已经保存到数据库,开始从数据库解析
                if (state_FromFirstMediaStoreRead){
                    state_FromFirstMediaStoreRead = false
                    loadFromDataBase("music")
                }else{
                    main_music_list_adapter.refresh()
                }
            }
            "MediaStore_NoExist_Delete_Complete" -> {
                main_video_list_adapter.refresh()
            }
        }
    }
    private val disposable_withExtraString = ToolEventBus.events_withExtraString.subscribe {
        when (it.type) {
            "PlayerActivity_CoverChanged" -> {
                it.fileName?.let { fileName ->
                    main_video_list_adapter.updateCoverForVideo(fileName)
                }
            }
        }
    }
    //保存加载状态
    private fun saveLoadState(type: String) {
        if (type == "video"){
            PREFS_MediaStore.edit { putBoolean("state_VideoMediaStoreReaded", true).apply() }
        }
        else if (type == "music"){
            PREFS_MediaStore.edit { putBoolean("state_MusicMediaStoreReaded", true).apply() }
        }
    }

//class END
}

