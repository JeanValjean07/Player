package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
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
import com.suming.player.ListManager.FragmentPlayList
import data.MediaDataReader.MediaDataBaseReaderForMusic
import data.MediaDataReader.MediaDataBaseReaderForVideo
import data.MediaDataReader.MediaStoreReaderForMusic
import data.MediaDataReader.MediaStoreReaderForVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(UnstableApi::class)
class MainActivity: AppCompatActivity() {
    //ViewModel
    private lateinit var MainViewModel: MainViewModel
    //权限检查
    private val REQUEST_STORAGE_PERMISSION = 1001
    //防止快速点击
    private var lock_clickMillisLock = 0L
    //界面控件元素
    //<editor-fold desc="界面控件元素">
    private lateinit var main_video_list_adapter: MainVideoAdapter
    private lateinit var main_music_list_adapter: MainMusicAdapter
    private lateinit var main_video_list_adapter_RecyclerView: RecyclerView
    private lateinit var main_music_list_adapter_RecyclerView: RecyclerView
    private lateinit var AppBarTitle: TextView
    private lateinit var AppBarNoticeText: TextView
    private lateinit var ButtonCardMusic: CardView
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardGallery: CardView
    //</editor-fold>
    //设置和设置项
    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_QueryNewVideoOnStart = false
    private var PREFS_AcquiesceTab = "video"
    //状态信息
    //<editor-fold desc="状态信息">
    private var state_FromFirstMediaStoreRead = false
    private var state_currentPage = ""
    private var state_lastPage = ""
    private var state_PlayingCard_inited = false
    private var state_onFirstStart = false
    private var state_PlayingCard_showing = false
    private var state_PlayingCard_gone = true
    //</editor-fold>
    //播放中卡片
    //<editor-fold desc="播放中卡片">
    private lateinit var PlayingCard: CardView
    private lateinit var PlayingCard_MediaName: TextView
    private lateinit var PlayingCard_MediaArtist: TextView
    private lateinit var PlayingCard_Image: ImageView
    private lateinit var PlayingCard_Video: PlayerView
    private lateinit var PlayingCard_Button: ImageButton
    private lateinit var PlayingCard_List: ImageButton
    //</editor-fold>


    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //界面设置
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_old)
        //启动播放器单例
        PlayerSingleton.startPlayerSingleton(application)
        //界面实例获取
        preCheckAndInit()
        //表明首次启动信息
        state_onFirstStart = savedInstanceState == null


        //内容避让状态栏并预读取状态栏高度写入设置
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            if (!SettingsRequestCenter.isStatusBarHeightExist(this)){
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                SettingsRequestCenter.set_VALUE_Int_statusBarHeight(statusBarHeight)
            }
            insets
        }
        //读取媒体库设置
        PREFS_MediaStore = getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
        PREFS_MediaStore.edit { putBoolean("PREFS_showHideItems", false).apply() }
        if (PREFS_MediaStore.contains("PREFS_QueryNewVideoOnStart")){
            PREFS_QueryNewVideoOnStart = PREFS_MediaStore.getBoolean("PREFS_QueryNewVideoOnStart", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("PREFS_QueryNewVideoOnStart", false).apply() }
            PREFS_QueryNewVideoOnStart = false
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
        if (PREFS_MediaStore.contains("PREFS_AcquiesceTab")){
            PREFS_AcquiesceTab = PREFS_MediaStore.getString("PREFS_AcquiesceTab", "video")?: "error"
            if (PREFS_AcquiesceTab == "error"){
                PREFS_AcquiesceTab = "video"
                PREFS_MediaStore.edit { putString("PREFS_AcquiesceTab", "video").apply() }
            }
            if (PREFS_AcquiesceTab != "video" && PREFS_AcquiesceTab != "music" && PREFS_AcquiesceTab != "gallery" && PREFS_AcquiesceTab != "last"){
                PREFS_AcquiesceTab = "video"
                PREFS_MediaStore.edit { putString("PREFS_AcquiesceTab", "video").apply() }
                showCustomToast("修复了默认页签设置", Toast.LENGTH_SHORT, 3)
            }
        }else{
            PREFS_MediaStore.edit { putString("PREFS_AcquiesceTab", "video").apply() }
            PREFS_AcquiesceTab = "video"
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
            if (state_currentPage == "video"){
                MainFragVideoStoreSetting.newInstance().show(supportFragmentManager, "MainFragVideoStoreSetting")
            }
            else if (state_currentPage == "music"){
                MainFragMusicStoreSetting.newInstance().show(supportFragmentManager, "MainFragMusicStoreSetting")
            }
        }
        //页签按钮
        ButtonCardMusic.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            //显示音乐列表
            showMusicList(false)
        }
        ButtonCardVideo.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            //显示视频列表
            showVideoList(false)

        }
        ButtonCardGallery.setOnClickListener {
            //处理交互
            ToolVibrate().vibrate(this@MainActivity)
            //需要重做为单独的页面
            showCustomToast("陈列架功能暂未开放", Toast.LENGTH_SHORT, 3)
        }
        //播放卡片
        PlayingCard = findViewById(R.id.PlayingCard)
        PlayingCard_Button = findViewById(R.id.PlayingCard_Button)
        PlayingCard_List = findViewById(R.id.PlayingCard_List)
        PlayingCard.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //启动播放页
            val uri = PlayerSingleton.getMediaInfoUri()
            startPlayerFromSmallCard(uri)
        }
        PlayingCard_Button.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            if (PlayerSingleton.getIsPlaying()){
                PlayerSingleton.recessPlay(need_fadeOut = false)
            }else{
                PlayerSingleton.continuePlay(true, force_request = true, need_fadeIn = false)
            }
            setPlayingCardButton()
        }
        PlayingCard_List.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //防止快速点击
            if (System.currentTimeMillis() - lock_clickMillisLock < 800) {
                return@setOnClickListener
            }
            lock_clickMillisLock = System.currentTimeMillis()
            //启动播放列表
            FragmentPlayList.newInstance().show(supportFragmentManager, "PlayerListFragment")
        }

        //初次启动:
        if (savedInstanceState == null){
            //使用上一次的页面
            if (PREFS_AcquiesceTab == "last"){
                when (state_lastPage){
                    "video" -> {
                        showVideoList(false)
                    }
                    "music" -> {
                        showMusicList(false)
                    }
                }
            }
            //使用默认页面
            else if (PREFS_AcquiesceTab == "video"){
                showVideoList(false)
            }
            else if (PREFS_AcquiesceTab == "music"){
                showMusicList(false)
            }
            else{
                showCustomToast("默认加载页面标识符错误，不知道要加载哪个页面", Toast.LENGTH_SHORT, 3)
            }
        }
        //恢复状态启动:
        else{
            Log.d("SuMing", "恢复状态启动: ${savedInstanceState.getString("state_currentPage", "video")}")
            state_currentPage = savedInstanceState.getString("state_currentPage", "video")?: "error"
            if (state_currentPage == "video"){
                showVideoList(true)
            }
            else if (state_currentPage == "music"){
                showMusicList(true)
            }
            else{
                showCustomToast("从Bundle中恢复了错误的页面标识", Toast.LENGTH_SHORT, 3)
            }
            //滚动到上次位置
            //NestedScrollView_VideoList.post { NestedScrollView_VideoList.scrollY = savedInstanceState.getInt("state_NestedScrollView_Y", 0) }
        }

        //视频媒体库设置返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_VIDEO_MediaStore", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                "RenovateAdapter" -> {
                    main_video_list_adapter.refresh()
                }
                "QueryFromMediaStoreVideo" -> {
                    startLoadFromMediaStore("video")
                }
            }
        }
        //音乐媒体库设置返回值
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_MUSIC_MediaStore", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                "RenovateAdapter" -> {
                    main_music_list_adapter.refresh()
                }
                "QueryFromMediaStoreMusic" -> {
                    startLoadFromMediaStore("music")
                }
            }
        }
        //播放列表返回值 FROM_FRAGMENT_PLAY_LIST
        supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_PLAY_LIST", this) { _, bundle ->
            val ReceiveKey = bundle.getString("KEY")
            when(ReceiveKey){
                //切换逻辑由播放器单例接管
                //退出逻辑
                "Dismiss" -> {

                }
                //更新小卡片
                "stopPlaying" -> {
                    ResetPlayingCard()
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

        //注册事件总线监听器
        setupEventBus()

        //刷新状态和设置
        state_MediaStore_refreshed = false

        //首次启动时延迟检查上次的媒体
        if(state_onFirstStart){
            state_onFirstStart = false
            Handler(Looper.getMainLooper()).postDelayed({
                checkLastPlayingMedia()
            }, 1000)
        }else{ ResetPlayingCard() }

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("state_currentPage", state_currentPage)
    }

    override fun onPause() {
        super.onPause()
        //关闭事件总线监听器
        disposeEventBus()
    }

    override fun onDestroy() {
        super.onDestroy()

    }




    //Functions
    //显示视频列表 !主链路入口
    private fun showVideoList(flag_re_onCreate: Boolean){
        //页面标识防重复
        if (state_currentPage == "video" && state_VideoDataBaseReaded_N_AdapterBinded){
            listGoTop()
            return
        }
        state_currentPage = "video"
        //界面切换
        setVideoElement()
        resetElement("video")
        //加载事务
        Handler(Looper.getMainLooper()).postDelayed({
            generalLoadVideo()
        }, 100)


    }  //!主链路入口
    //显示音乐列表 !主链路入口
    private fun showMusicList(flag_re_onCreate: Boolean){
        //页面标识防重复
        if (state_currentPage == "music" && state_MusicDataBaseReaded_N_AdapterBinded){
            listGoTop()
            return
        }
        state_currentPage = "music"
        //界面切换
        setMusicElement()
        resetElement("music")
        //加载事务
        Handler(Looper.getMainLooper()).postDelayed({
            generalLoadMusic()
        }, 100)

    }  //!主链路入口

    //onResume:检查正在播放的媒体并更新播放卡片
    private fun ResetPlayingCard(){
        //检查正在播放的媒体
        val isNowPlying = checkPlayingItem()
        if (!isNowPlying){ return }
        //获取播放中媒体信息
        val (MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist) = getPlayingMediaItemInfo()
        //Log.d("SuMing", "ResetPlayingCard : $MediaInfo_MediaType, $MediaInfo_FileName, $MediaInfo_MediaArtist")
        //更新播放卡片
        updatePlayingCard(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist)

    }  //!主链路入口
    //onCreate:每次启动检查上次在播放的媒体
    private fun checkLastPlayingMedia(){
        val (MediaInfo_MediaUriString, isMediaValid) = getLastMediaRecord()
        if (isMediaValid){
            if (isUriStringValid(MediaInfo_MediaUriString)){
                val isNowPlying = checkPlayingItem()
                if (isNowPlying){
                    ResetPlayingCard()
                }else{
                    setNewMediaItem(MediaInfo_MediaUriString.toUri(), false)
                }
            }
            else{
                closePlayingCard()
            }
        }else{
            closePlayingCard()
        }
    }  //!主链路入口
    private fun isUriStringValid(uriString: String): Boolean{
        val retriever = MediaMetadataRetriever()
        try{
            val uri = uriString.toUri()
            retriever.setDataSource(this,uri)
        }
        catch (_: Exception){
            return false
        }
        finally {
            retriever.release()
        }

        return true
    }
    //播放卡片功能方法
    private fun setPlayingCardButton(){
        if (PlayerSingleton.getIsPlaying()){
            PlayingCard_Button.setImageResource(R.drawable.ic_main_controller_pause)
        }else{
            PlayingCard_Button.setImageResource(R.drawable.ic_main_controller_play)
        }
    } //播放卡片按钮刷新
    private fun updatePlayingCard(MediaInfo_MediaType: String, MediaInfo_FileName: String, MediaInfo_MediaArtist: String){
        //确保已显示播放中卡片
        showPlayingCardWithAnimation()
        //绑定播放器或者显示缩略图
        BindPlayingCardSmallPlayer(MediaInfo_MediaType, MediaInfo_FileName)
        //设置文本
        setPlayingCardTextInfo(MediaInfo_FileName, MediaInfo_MediaArtist)
        //播放卡片按钮刷新
        setPlayingCardButton()
    }
    private fun getPlayingMediaItemInfo(): Triple<String, String, String>{
        //获取播放中媒体信息
        var (MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist) = PlayerSingleton.getMediaInfoForMain()
        if (MediaInfo_FileName == "" || MediaInfo_FileName == "error"){ MediaInfo_FileName = "未知媒体标题" }
        if (MediaInfo_MediaArtist == "" || MediaInfo_MediaArtist == "error"){ MediaInfo_MediaArtist = "未知艺术家" }
        return Triple(MediaInfo_MediaType, MediaInfo_FileName, MediaInfo_MediaArtist)
    } //获取并过滤播放信息
    private fun checkPlayingItem(): Boolean{
        val currentMediaItem = PlayerSingleton.getCurrentMediaItem()
        //无播放媒体:关闭播放卡片
        if (currentMediaItem == null){
            closePlayingCard()
            return false
        }
        //有播放媒体:显示播放卡片
        else{
            showPlayingCardWithAnimation()
            return true
        }
    } //检查是否在播放中
    private fun BindPlayingCardSmallPlayer(type: String, filename: String){
        if (!state_PlayingCard_inited){ initPlayingCard() }
        if (!state_PlayingCard_showing){ return }
        //视频
        if (type == "video"){
            if (SettingsRequestCenter.get_PREFS_DisableMainPageSmallPlayer(this)){
                PlayingCard_Video.player = null
                PlayingCard_Video.visibility = View.VISIBLE
                PlayingCard_Image.visibility = View.VISIBLE
                //获取封面图
                val covers_path = File(filesDir, "miniature/video_cover")
                val filename = PlayerSingleton.getMediaInfoFileName()
                val cover_img = File(covers_path, "${filename.hashCode()}.webp")
                val cover_img_uri = if (cover_img.exists()) {
                    try {
                        FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", cover_img)
                    }
                    catch (e: Exception) {
                        if (cover_img.canRead()) {
                            cover_img.toUri()
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
                PlayingCard_Image.setImageURI(cover_img_uri)
            }else{
                PlayingCard_Image.visibility = View.GONE
                PlayingCard_Video.visibility = View.VISIBLE
                PlayingCard_Video.player = null
                PlayingCard_Video.player = PlayerSingleton.getPlayer(application)
            }
        }
        //音乐
        else if (type == "music"){
            PlayingCard_Image.visibility = View.VISIBLE
            PlayingCard_Video.visibility = View.GONE
            //获取封面图
            val uriNumOnly = PlayerSingleton.getMediaInfoUri().lastPathSegment
            val covers_path = File(filesDir, "miniature/music_cover")
            val cover_img = File(covers_path, "${uriNumOnly}.webp")
            val cover_img_uri = if (cover_img.exists()) {
                try {
                    FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", cover_img)
                }
                catch (e: Exception) {
                    if (cover_img.canRead()) {
                        cover_img.toUri()
                    } else {
                        null
                    }
                }
            } else {
                Log.d("SuMing", "BindPlayingCardSmallPlayer : 封面图不存在")
                null
            }
            PlayingCard_Image.setImageURI(cover_img_uri)
        }

    } //绑定播放器或视频视图
    private fun setPlayingCardTextInfo(title: String, artist: String){
        if (!state_PlayingCard_inited){ initPlayingCard() }
        if (!state_PlayingCard_showing){ return }

        PlayingCard_MediaName.text = title
        PlayingCard_MediaArtist.text = artist

    } //播放卡片文本
    //播放卡片初始化和显示/隐藏
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
    //读取上次播放记录
    private fun getLastMediaRecord(): Pair<String, Boolean>{
        val lastRecord = getSharedPreferences("lastRecord", MODE_PRIVATE)
        val MediaInfo_MediaUriString = lastRecord.getString("MediaInfo_MediaUriString", "error") ?: "error"
        val isMediaValid = lastRecord.getBoolean("MediaInfo_MediaValid", false)
        return Pair(MediaInfo_MediaUriString, isMediaValid)
    }
    //设置新媒体项
    private fun setNewMediaItem(MediaInfo_MediaUri: Uri, playWhenReady: Boolean){
        PlayerSingleton.getPlayer(application)
        PlayerSingleton.addPlayerStateListener()
        //启动播放器
        PlayerSingleton.startSingletonExoPlayer(application)
        //确认设置新媒体项
        PlayerSingleton.setMediaItem(MediaInfo_MediaUri, playWhenReady, this)

    }
    //保存上次播放的项信息
    private fun saveLastMediaItemInfo(type: String, title: String, artist: String, uriString: String){
        if (uriString == ""){ return }
        val INFO_PlayerSingleton = getSharedPreferences("INFO_PlayerSingleton", MODE_PRIVATE)
        INFO_PlayerSingleton.edit {
            putString("MediaInfo_MediaType", type)
            putString("MediaInfo_FileName", title)
            putString("MediaInfo_MediaArtist", artist)
            putString("MediaInfo_MediaUriString", uriString)
        }
    }
    //从选项菜单中发起后台播放
    private fun startSmallCardPlay(uri: Uri, filename: String){
        //比对上次播放媒体信息与当前播放媒体信息
        val newUri = uri
        val currentUri = PlayerSingleton.getMediaInfoUri()
        if (newUri == currentUri){
            showCustomToast("已在播放该媒体", Toast.LENGTH_SHORT, 3)
            PlayerSingleton.continuePlay(true, force_request = true, need_fadeIn = false)
            return
        }
        //设置新播放项
        setNewMediaItem(newUri, true)

    }
    //停止视频播放区域
    private fun UnBindSmallCardVideo(){
        PlayingCard_Video.player = null
    }
    //页签切换
    private var state_MusicMediaStoreReaded = false
    private var state_MusicDataBaseReaded_N_AdapterBinded = false
    @SuppressLint("NewApi")
    private fun generalLoadMusic(){
        //检查权限
        val isPermissionGranted = isPermissionGranted()
        if (!isPermissionGranted){
            if (isVersionAboveTiramisu){
                showCustomToast("请先开启管理所有文件权限", Toast.LENGTH_SHORT, 3)
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                this.startActivity(intent)
            }else{
                showCustomToast("请先开启媒体访问文件权限", Toast.LENGTH_SHORT, 3)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply { data = "package:$packageName".toUri() }
                startActivity(intent)
            }
            return
        }
        //本次启动第一次加载音乐
        if (PREFS_QueryNewVideoOnStart){
            if (!state_MusicDataBaseReaded_N_AdapterBinded){
                checkPermissionThenStartLoad("music")
            }
        }
        else{
            if (state_MusicMediaStoreReaded){
                LoadDataBase_N_BindAdapter("music")
            }
            else{
                checkPermissionThenStartLoad("music")
            }
        }
    }  //!主链路入口
    private var state_VideoMediaStoreReaded = false
    private var state_VideoDataBaseReaded_N_AdapterBinded = false
    @SuppressLint("NewApi")
    private fun generalLoadVideo(){
        //检查权限
        val isPermissionGranted = isPermissionGranted()
        if (!isPermissionGranted){
            if (isVersionAboveTiramisu){
                showCustomToast("请先开启管理所有文件权限", Toast.LENGTH_SHORT, 3)
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                this.startActivity(intent)
            }else{
                showCustomToast("请先开启媒体访问文件权限", Toast.LENGTH_SHORT, 3)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply { data = "package:$packageName".toUri() }
                startActivity(intent)
            }
            return
        }
        //本次启动第一次加载视频
        if (PREFS_QueryNewVideoOnStart){
            //Log.d("SuMing", "generalLoadVideo : 11111")
            if (!state_VideoDataBaseReaded_N_AdapterBinded){
                //Log.d("SuMing", "generalLoadVideo : 22222")
                checkPermissionThenStartLoad("video")
            }
        }
        else{
            //Log.d("SuMing", "generalLoadVideo : 33333")
            if (state_MusicMediaStoreReaded){
                //Log.d("SuMing", "generalLoadVideo : 44444")
                LoadDataBase_N_BindAdapter("video")
            }
            else{
                //Log.d("SuMing", "generalLoadVideo : 55555")
                checkPermissionThenStartLoad("video")
            }
        }

    }  //!主链路入口
    //页签切换变更页面信息
    private fun setMusicElement(){
        state_currentPage = "music"
        AppBarTitle.text = "音乐"
        ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))

        main_video_list_adapter_RecyclerView.visibility = View.GONE
        main_music_list_adapter_RecyclerView.visibility = View.VISIBLE

        PREFS_MediaStore.edit{ putString("state_lastPage", "music") }

    }
    private fun setVideoElement(){
        state_currentPage = "video"
        AppBarTitle.text = "视频"
        ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))

        main_video_list_adapter_RecyclerView.visibility = View.VISIBLE
        main_music_list_adapter_RecyclerView.visibility = View.GONE

        PREFS_MediaStore.edit{ putString("state_lastPage", "video") }

    }
    private fun resetElement(avoid: String){
        if (avoid == "music"){
            ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            ButtonCardGallery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
        }
        else if (avoid == "video"){
            ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            ButtonCardGallery.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
        }
        else{
            showCustomToast("界面重置函数接收到预期外的参数", Toast.LENGTH_SHORT, 3)
        }
    }
    private fun listGoTop(){
        when (state_currentPage) {
            "music" -> {
                if (state_MusicDataBaseReaded_N_AdapterBinded) {
                    main_music_list_adapter.refresh()
                }
                main_music_list_adapter_RecyclerView.smoothScrollToPosition(0)
            }
            "video" -> {
                if (state_VideoDataBaseReaded_N_AdapterBinded) {
                    main_video_list_adapter.refresh()
                }
                main_video_list_adapter_RecyclerView.smoothScrollToPosition(0)
            }
            else -> {
                showCustomToast("列表回顶函数接收到预期外的参数", Toast.LENGTH_SHORT, 3)
            }
        }
    }
    //初始化
    private fun preCheckAndInit(){
        //界面实例获取
        main_video_list_adapter_RecyclerView = findViewById(R.id.recyclerview_video_list)
        main_music_list_adapter_RecyclerView = findViewById(R.id.recyclerview_music_list)
        AppBarNoticeText = findViewById(R.id.AppBarNoticeText)
        AppBarTitle = findViewById(R.id.AppBarTitle)
        ButtonCardMusic = findViewById(R.id.ButtonCardMusic)
        ButtonCardVideo = findViewById(R.id.ButtonCardVideo)
        ButtonCardGallery = findViewById(R.id.ButtonCardGallery)
        //事件总线注册
        setupEventBus()

    }
    //提示内容合集
    private var setLoadingTextJob: Job? = null
    private fun setLoadingText(text: String,delay_then_close: Boolean, delay_value_ms: Long){
        AppBarNoticeText.text = text
        AppBarNoticeText.visibility = View.VISIBLE
        //延迟关闭
        setLoadingTextJob?.cancel()
        if (delay_then_close){
            setLoadingTextJob = lifecycleScope.launch(Dispatchers.Main) {
                delay(delay_value_ms)
                removeLoadingText()
            }
        }
    }
    private fun removeLoadingText(){
        AppBarNoticeText.text = ""
        AppBarNoticeText.visibility = View.GONE
    }
    private fun setNeedPermissionIcon(){


    }
    //从媒体库接口读取
    private fun startLoadFromMediaStore(flag_video_or_music: String){
        setLoadingText("正在读取媒体库", false, 0)
        //发起后台线程加载
        lifecycleScope.launch(Dispatchers.IO) {
            //视频
            if (flag_video_or_music == "video"){
                val mediaReader = MediaStoreReaderForVideo(this@MainActivity, contentResolver)
                mediaReader.readAndSaveAllVideos()
            }
            //音乐
            else if(flag_video_or_music == "music"){
                val musicReader = MediaStoreReaderForMusic(this@MainActivity, contentResolver)
                musicReader.readAndSaveAllMusics()
            }
            //类型未命中
            else{
                showCustomToast("加载类型输入错误", Toast.LENGTH_SHORT, 3)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermissionThenStartLoad(flag_video_or_music: String){
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main){
                //权限检查
                isVersionAboveTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                if (isVersionAboveTiramisu){
                    if (Environment.isExternalStorageManager()){
                        Log.d("SuMing", "generalLoadVideo : 66666")
                        startLoadFromMediaStore(flag_video_or_music)
                    }
                    else{
                        Log.d("SuMing", "generalLoadVideo : 77777")
                        showCustomToast("请先开启管理所有文件权限", Toast.LENGTH_SHORT, 3)
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        this@MainActivity.startActivity(intent)
                    }
                }
                else{
                    val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE
                    //权限未授予
                    if (ContextCompat.checkSelfPermission(this@MainActivity, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(requiredPermission), REQUEST_STORAGE_PERMISSION)
                        //权限提示
                        setNeedPermissionIcon()

                        startCheckPermission()
                    }
                    //权限已授予
                    else{
                        startLoadFromMediaStore(flag_video_or_music)
                    }
                }
            }
        }
    }
    //从本地数据库加载+绑定列表+点击事件+包含视频和音频
    private fun LoadDataBase_N_BindAdapter(flag_video_or_music: String) {
        //使用视频adapter
        if (flag_video_or_music == "video"){
            if (state_VideoDataBaseReaded_N_AdapterBinded){ return }
            state_VideoDataBaseReaded_N_AdapterBinded = true
            //设置列表布局管理器
            main_video_list_adapter_RecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            //注册点击事件
            main_video_list_adapter = MainVideoAdapter(
                context = this,
                onItemClick = { uri ->
                    startVideoPlayer(uri)
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
                    val popup = PopupMenu(this, main_video_list_adapter_RecyclerView)
                    popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                    popup.setOnMenuItemClickListener { /*handle*/; true }
                    popup.show()
                },
                onSmallCardPlay = { uri, title ->
                    startSmallCardPlay(uri.toUri(), title)
                }
            )
            //设置adapter
            main_video_list_adapter_RecyclerView.adapter = main_video_list_adapter
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
            if (state_MusicDataBaseReaded_N_AdapterBinded){ return }
            state_MusicDataBaseReaded_N_AdapterBinded = true
            //设置列表布局管理器
            main_music_list_adapter_RecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            //注册点击事件
            main_music_list_adapter = MainMusicAdapter(
                context = this,
                onItemClick = { uri ->
                    ToolVibrate().vibrate(this@MainActivity)
                    startMusicPlayer(uri)
                },
                onOptionsClick = { item, view ->
                    ToolVibrate().vibrate(this@MainActivity)

                },
            )
            //设置adapter
            main_music_list_adapter_RecyclerView.adapter = main_music_list_adapter
            //分页加载
            val pager = Pager(PagingConfig(
                pageSize = 25,
                prefetchDistance = 40,
                enablePlaceholders = false,
                initialLoadSize = 200,
                maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,
                jumpThreshold = Int.MIN_VALUE)) {
                MediaDataBaseReaderForMusic(context = this@MainActivity)
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    main_music_list_adapter.submitData(pagingData)
                }
            }

        }
        //严重错误
        else{
            notice("严重错误:未知的加载板块flag", 5000)
        }
    }
    //启动播放器
    private fun startVideoPlayer(uri: Uri){
        ToolVibrate().vibrate(this@MainActivity)
        //防止快速发起
        if (System.currentTimeMillis() - lock_clickMillisLock < 800) {
            return
        }
        lock_clickMillisLock = System.currentTimeMillis()
        //确认启动
        val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)
        when(playPageType){
            0 -> {
                //构建intent
                val intent = Intent(this, PlayerActivityOro::class.java).apply { putExtra("uri", uri) }
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                //构建可选参数
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.slide_in_vertical,
                    R.anim.slide_dont_move
                )

                //启动活动
                if (state_PlayingCard_showing){
                    detailLauncher.launch(intent, options)

                }else{
                    detailLauncher.launch(intent)
                }

            }
            1 -> {
                //构建intent
                val intent = Intent(this, PlayerActivityNeo::class.java).apply { putExtra("uri", uri) }
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                //构建可选参数
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.slide_in_vertical,
                    R.anim.slide_dont_move
                )

                //启动活动
                if (state_PlayingCard_showing){
                    detailLauncher.launch(intent, options)

                }else{
                    detailLauncher.launch(intent)
                }
            }
        }

    }
    private fun startMusicPlayer(uri: Uri){
        setNewMediaItem(uri, true)

        //使用其他播放器播放
        /*
        try {

            val playIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "audio/*")  // 关键：设置类型为audio/*
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val packageManager = this.packageManager
            if (playIntent.resolveActivity(packageManager) != null) {
                this.startActivity(playIntent)
            }else{
                Toast.makeText(this, "未找到可用的音乐播放器", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: ActivityNotFoundException) {
            Log.e("SuMing", "未找到可用的播放器应用", e)
            Toast.makeText(this, "无法播放：未找到播放器应用", Toast.LENGTH_SHORT).show()
        }
        catch (e: SecurityException) {
            Log.e("SuMing", "权限不足，无法播放", e)
            Toast.makeText(this, "权限不足，无法访问此文件", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Log.e("SuMing", "播放失败", e)
            Toast.makeText(this, "播放失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }

       */*/

         */
    }
    private fun startPlayerFromSmallCard(uri: Uri){
        val MediaInfo_MediaType = PlayerSingleton.getMediaInfoType()
        when (MediaInfo_MediaType) {
            "video" -> {
                startVideoPlayer(uri)
            }
            "music" -> {
                showCustomToast("暂不支持打开音乐播放页面", Toast.LENGTH_SHORT, 3)
                //startMusicPlayer(uri)
            }
            else -> {
                showCustomToast("严重错误:未知的媒体类型", Toast.LENGTH_SHORT, 3)
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
    private fun isPermissionGranted(): Boolean{
        isVersionAboveTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Environment.isExternalStorageManager()
        }
        else{
            val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED
        }
    }
    private val checkPermissionHandler = Handler(Looper.getMainLooper())
    private var isVersionAboveTiramisu = false
    private var checkPermission = object : Runnable{
        @RequiresApi(Build.VERSION_CODES.R)
        override fun run() {
            if (isVersionAboveTiramisu){
                if (!Environment.isExternalStorageManager()){
                    val intent = Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                        "package:${this@MainActivity.packageName}".toUri()
                    )
                    this@MainActivity.startActivity(intent)
                }
            }
            else{
                val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this@MainActivity, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(requiredPermission), REQUEST_STORAGE_PERMISSION)
                    checkPermissionHandler.postDelayed(this, 100)
                }
                //进行最终读取操作
                else{
                    startLoadFromMediaStore("video")
                    startLoadFromMediaStore("music")
                }
            }
        }
    }
    private fun startCheckPermission() {
        isVersionAboveTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        checkPermissionHandler.post(checkPermission)
    }
    private fun stopCheckPermission() {
        checkPermissionHandler.removeCallbacks(checkPermission)
    }
    //RxJava事件总线
    private var disposable: io.reactivex.rxjava3.disposables.Disposable? = null
    private var disposableInitialized = false
    private fun setupEventBus() {
        if (disposableInitialized){ return }
        disposableInitialized = true
        disposable = ToolEventBus.events
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe({
                HandlePlayerEvent(it)
            }, {
                showCustomToast("事件总线函数出现错误:${it.message}", Toast.LENGTH_SHORT,3)
            })
    }
    private fun disposeEventBus() {
        disposable?.dispose()
        disposableInitialized = false
    }
    private fun HandlePlayerEvent(event: String) {
        when (event) {
            //视频更新完成
            "QueryFromMediaStoreVideoComplete" -> {
                Log.d("SuMing", "HandlePlayerEvent: MediaStore_Video_Query_Complete")
                //关提示卡
                setLoadingText("读取完成", true, 5000)
                //修改是否完成过加载记录
                saveLoadState("video")
                //数据已经保存到数据库,开始从数据库解析
                if (state_VideoDataBaseReaded_N_AdapterBinded){
                    main_video_list_adapter.refresh()
                    Log.d("SuMing", "HandlePlayerEvent: MediaStore_Video_Query_Complete_2")

                }else{
                    Log.d("SuMing", "HandlePlayerEvent: MediaStore_Video_Query_Complete_3")
                    LoadDataBase_N_BindAdapter("video")
                }

            }
            //音乐更新完成
            "QueryFromMediaStoreMusicComplete" -> {
                //关提示卡
                setLoadingText("读取完成", true, 5000)
                //修改是否完成过加载记录
                saveLoadState("music")
                //数据已经保存到数据库,开始从数据库解析
                if (state_MusicDataBaseReaded_N_AdapterBinded){
                    main_music_list_adapter.refresh()

                }else{
                    Log.d("SuMing", "HandlePlayerEvent: MediaStore_Music_Query_Complete")
                    LoadDataBase_N_BindAdapter("music")
                }
            }
            //播放器状态变更
            "PlayerSingleton_PlaybackStateChanged" -> {
                setPlayingCardButton()
            }
            "PlayerSingleton_MediaItemChanged" -> {
                ResetPlayingCard()
                setHighRefreshRate()
            }

            //????
            "MediaStore_Refresh_Complete" -> {
                main_video_list_adapter.refresh()
                //让播放器重读媒体列表
                //Log.d("SuMing", "HandlePlayerEvent: MediaStore_Video_Query_Complete")
                PlayerSingleton.getMediaListByDataBaseChange(application)
            }
            "ExistInvalidMediaItem" -> {
                existInvalidMediaItem()
            }
        }
    }
    private val disposable_withExtraString = ToolEventBus.events_withExtraString.subscribe {
        when (it.key) {
            "PlayerActivity_CoverChanged" -> {
                it.stringInfo?.let { uriNumOnlyString ->
                    val uriNumOnly = uriNumOnlyString.toLong()
                    main_video_list_adapter.updateCoverForVideo(uriNumOnly)
                }
            }
        }
    }
    //刷新列表
    private var state_MediaStore_refreshed = false
    private fun existInvalidMediaItem(){
        if (state_MediaStore_refreshed){ return }
        state_MediaStore_refreshed = true
        showCustomToast("存在已失效的媒体项,将刷新列表", Toast.LENGTH_SHORT,3)

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
    //设置高刷新率
    @Suppress("DEPRECATION")
    private fun setHighRefreshRate() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = windowManager
            val mode = wm.defaultDisplay.mode
            val fps = mode.refreshRate
            window.attributes = window.attributes.apply {
                preferredRefreshRate = fps
            }
        }

    }

//class END
}

