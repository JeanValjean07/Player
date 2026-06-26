package com.suming.player

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.MainActivity.FragmentMusicStoreSetting
import com.suming.player.ActivityComponent.MainActivity.FragmentVideoStoreSetting
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterMusic
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterVideo
import com.suming.player.ActivityComponent.MainActivity.MainViewModel
import com.suming.player.AddonTools.ToolEventBus
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreRepo
import com.suming.player.DataPack.DataBaseMusicStore.MusicStoreRepo
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.FuncPack_ListManager.FragmentPlayList
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.DataPack.MediaDataReader.MediaDataBaseReaderForMusic
import com.suming.player.DataPack.MediaDataReader.MediaDataBaseReaderForVideo
import com.suming.player.DataPack.MediaDataReader.MediaStoreReaderForMusic
import com.suming.player.DataPack.MediaDataReader.MediaStoreReaderForVideo
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.ConnectCenter
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaTypeCenter
import com.suming.player.FuncionalPack.PlayerInFoCenter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

//@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(UnstableApi::class)
class MainActivity: AppCompatActivity() {
    //连接ViewModel
    private val mainViewModel: MainViewModel by viewModels()

    //防止快速点击
    private var lock_clickMillisLock = 0L


    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //界面设置
        display()
        //初始化
        init()



        //注册界面控件
        register()

        //注册Fragment监听器
        registerFragment()

        //主业务
        mainBusiness()

        //显示列表
        showMediaList(savedInstanceState)

        setupEventObserver()

    }
    //onResume时更新一些设置变量
    override fun onResume() {
        super.onResume()

        //更新MiniView
        showMiniViewLongProcess()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun init(){
        //获取主要列表视图
        ListRecyclerView_Video = findViewById(R.id.recyclerview_video_list)
        ListRecyclerView_Music = findViewById(R.id.recyclerview_music_list)
        AppBarNoticeText = findViewById(R.id.AppBarNoticeText)
        AppBarTitle = findViewById(R.id.AppBarTitle)
        ButtonCardMusic = findViewById(R.id.ButtonCardMusic)
        ButtonCardVideo = findViewById(R.id.ButtonCardVideo)
        ButtonCardGallery = findViewById(R.id.ButtonCardGallery)


        //获取MiniView视图
        initMiniView()


        //手势监听
        lifecycleScope.launch (Dispatchers.Main) {
            delay(500)
            //监听返回手势
            onBackPressedDispatcher.addCallback(this@MainActivity, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(false)
                }
            })
        }
    }
    private fun display(){
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            if (!SettingsRequestCenter.isStatusBarHeightExist(this)){
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                SettingsRequestCenter.set_VALUE_Int_statusBarHeight(statusBarHeight)
            }
            insets
        }
    }





    //Main Thread Functions
    //注册Fragment监听器
    private fun registerFragment(){
        lifecycleScope.launch (Dispatchers.Main) {
            delay(500)
            //视频媒体库设置返回值
            supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_VIDEO_MediaStore", this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString("KEY")
                when(ReceiveKey){
                    "RenovateAdapter" -> {
                        main_video_list_adapter.refresh()
                    }
                    "QueryFromMediaStoreVideo" -> {
                        startLocalMediaReader("video")
                    }
                }
            }
            //音乐媒体库设置返回值
            supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_MUSIC_MediaStore", this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString("KEY")
                when(ReceiveKey){
                    "RenovateAdapter" -> {
                        main_music_list_adapter.refresh()
                    }
                    "QueryFromMediaStoreMusic" -> {
                        startLocalMediaReader("music")
                    }
                }
            }
            //播放列表返回值 FROM_FRAGMENT_PLAY_LIST
            supportFragmentManager.setFragmentResultListener("FROM_FRAGMENT_PLAY_LIST", this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString("KEY")
                when(ReceiveKey){
                    //切换逻辑由播放器单例接管
                    //退出逻辑
                    "Dismiss" -> {

                    }

                }
            }
        }
    }
    //注册界面控件
    private fun register(){
        lifecycleScope.launch (Dispatchers.Main) {
            delay(500)

            //按钮：指南
            val ButtonGuidance = findViewById<Button>(R.id.buttonGuidance)
            ButtonGuidance.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                //
                val intent = Intent(this@MainActivity, GuidanceActivity::class.java)
                startActivity(intent)
            }
            //按钮：设置
            val ButtonSettings= findViewById<Button>(R.id.buttonSetting)
            ButtonSettings.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
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
                //
                when(mainViewModel.state_current_tab){
                    SettingsRequestCenter.tab_mark_video -> {
                        FragmentVideoStoreSetting.newInstance().show(supportFragmentManager, "MainFragVideoStoreSetting")
                    }
                    SettingsRequestCenter.tab_mark_music -> {
                        FragmentMusicStoreSetting.newInstance().show(supportFragmentManager, "MainFragMusicStoreSetting")
                    }
                }
            }
            //页签按钮
            ButtonCardMusic.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                //显示音乐列表
                showMusicList()
            }
            ButtonCardVideo.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                //显示视频列表
                showVideoList()

            }
            ButtonCardGallery.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                //需要重做为单独的页面
                showCustomToast("陈列架功能暂未开放",3)
            }
        }
    }
    //主业务(检查上次停留的媒体)
    private fun mainBusiness(){
        lifecycleScope.launch (Dispatchers.IO) {
            delay(700)
            //检查MiniView观察者是否已启动
            withContext(Dispatchers.Main) {
                //启动MiniView观察者
                startMiniViewObserver()
            }
            //MiniView的显示交给观察自动进行,此处只负责媒体的设置
            //检查是否有媒体正在播放
            val isAnyMediaOngoing = withContext(Dispatchers.Main){ isAnyMediaOngoing().first }
            if (!isAnyMediaOngoing){
                //没有媒体正在播放,从记录中获取上次停留的媒体信息(已检查是否有效)
                val MediaInfo_Uri = getLastRecordMedia()
                if (MediaInfo_Uri != null) {
                    withContext(Dispatchers.Main) {
                        if (SettingsRequestCenter.get_PREFS_EnableContinuePlay(this@MainActivity)){
                            setMediaItem(MediaInfo_Uri, false)
                        }
                    }
                }
            }
        }
    }

    //显示页面
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showMediaList(savedInstanceState: Bundle?){
        lifecycleScope.launch(Dispatchers.IO){
            //检查权限
            val (isPermissionGranted, _) = checkPermissionAndVersion()
            if (isPermissionGranted){
                //显示列表
                var targetList = ""
                targetList = if (savedInstanceState == null){
                    SettingsRequestCenter.get_PREFS_AcquiesceTab(this@MainActivity)
                }else{
                    mainViewModel.state_current_tab
                }
                //根据设置项显示列表
                withContext(Dispatchers.Main){
                    when(targetList){
                        SettingsRequestCenter.tab_mark_video -> {
                            showVideoList()
                        }
                        SettingsRequestCenter.tab_mark_music -> {
                            showMusicList()
                        }
                        SettingsRequestCenter.tab_mark_gallery -> {
                            //暂未开放,重定向到视频页
                            showVideoList()
                        }
                        SettingsRequestCenter.tab_mark_last -> {
                            val State_LastStayTab = readLastPageThenShow()
                            when (State_LastStayTab) {
                                SettingsRequestCenter.tab_mark_video -> {
                                    showVideoList()
                                }
                                SettingsRequestCenter.tab_mark_music -> {
                                    showMusicList()
                                }
                                SettingsRequestCenter.tab_mark_gallery -> {
                                    //暂未开放,重定向到视频页
                                    showVideoList()
                                }
                            }
                        }
                    }
                }
            }else{
                withContext(Dispatchers.Main){
                    startSettingPage()
                }
            }
        }
    }

    //读取上一次的页面
    private fun readLastPageThenShow(): String{
        val State_LastStayTab = SettingsRequestCenter.get_State_LastStayTab(this@MainActivity)
        consoleLog("showMediaList : State_LastStayTab = $State_LastStayTab")

        return State_LastStayTab
    }
    //显示视频列表
    private fun showVideoList(){
        lifecycleScope.launch(Dispatchers.IO) {
            //页面标识防重复
            if (mainViewModel.state_current_tab == SettingsRequestCenter.tab_mark_video && state_VideoRecyclerView_started){
                withContext(Dispatchers.Main){
                    listGoTop()
                }
                return@launch
            }
            mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_video
            //记录状态
            SettingsRequestCenter.set_State_LastStayTab(this@MainActivity, SettingsRequestCenter.tab_mark_video)

            withContext(Dispatchers.Main){
                //界面切换
                setVideoElement()
                resetElement("video")
                //加载事务
                showVideoListCore()
            }

        }
    }
    //显示音乐列表
    private fun showMusicList(){
        lifecycleScope.launch(Dispatchers.IO) {
            //页面标识防重复
            if (mainViewModel.state_current_tab == SettingsRequestCenter.tab_mark_music && state_MusicRecyclerView_started){
                withContext(Dispatchers.Main){
                    listGoTop()
                }
                return@launch
            }
            mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_music
            //记录状态
            SettingsRequestCenter.set_State_LastStayTab(this@MainActivity, SettingsRequestCenter.tab_mark_music)

            withContext(Dispatchers.Main){
                //界面切换
                setMusicElement()
                resetElement("music")
                //加载事务
                showMusicListCore()
            }
        }
    }

    //视频列表核心
    @SuppressLint("NewApi")
    private fun showVideoListCore(){
        consoleLog("showVideoListCore")
        //启动视频列表
        startVideoRecyclerView()
        val queryNew = SettingsRequestCenter.get_PREFS_QueryNewMediaOnStart(this)
        //检查本地数据库是否已有音乐数据
        lifecycleScope.launch(Dispatchers.IO) {
            if (MediaStoreRepo(this@MainActivity).isEmpty() || queryNew){
                consoleLog("showVideoListCore : 本地数据库视频数据为空")
                //从系统读取视频
                withContext(Dispatchers.Main){
                    startLocalMediaReader("video")
                }
            }else{
                consoleLog("showVideoListCore : 本地数据库视频数据不为空")
                //直接从本地数据库读取

            }
        }

    }
    private var state_VideoRecyclerView_started = false
    private lateinit var main_video_list_adapter: RecyclerAdapterVideo
    private lateinit var ListRecyclerView_Video: RecyclerView
    private fun startVideoRecyclerView(){
        if (state_VideoRecyclerView_started) return
        state_VideoRecyclerView_started = true
        //设置列表布局管理器
        ListRecyclerView_Video.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //注册点击事件
        main_video_list_adapter = RecyclerAdapterVideo(
            context = this,
            onItemClick = { uri ->
                startVideoPlayer(uri)
            },
            onDurationClick = { item ->
                ToolVibrate().vibrate(this@MainActivity)
                notice("视频时长:${FormatTime_withChar(item.durationMs)}", 2000)
            },
            onFormatClick = { item, format ->
                ToolVibrate().vibrate(this@MainActivity)
                notice("视频格式:${item.format}", 3000)
            },
            onOptionClick = { item ->
                ToolVibrate().vibrate(this@MainActivity)
                val popup = PopupMenu(this, ListRecyclerView_Video)
                popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                popup.setOnMenuItemClickListener { /*handle*/; true }
                popup.show()
            },
            onSmallCardPlay = { uri, title ->
                startMiniViewPlay(uri.toUri())
            }
        )
        //增加sidePadding
        ListRecyclerView_Video.setPadding(0, 0, 0, 300)
        //设置adapter
        ListRecyclerView_Video.adapter = main_video_list_adapter
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
    //音乐列表核心
    @SuppressLint("NewApi")
    private fun showMusicListCore(){
        consoleLog("showMusicListCore")
        //启动音乐列表
        startMusicRecyclerView()
        val queryNew = SettingsRequestCenter.get_PREFS_QueryNewMediaOnStart(this)
        //检查本地数据库是否已有音乐数据
        lifecycleScope.launch(Dispatchers.IO) {
            if (MusicStoreRepo(this@MainActivity).isEmpty() || queryNew){
                consoleLog("showMusicList数据库音乐数据为空")
                //从系统读取音乐
                withContext(Dispatchers.Main){
                    startLocalMediaReader(MediaTypeCenter.mediaType_Music)
                }
            }
        }
    }
    private var state_MusicRecyclerView_started = false
    private lateinit var main_music_list_adapter: RecyclerAdapterMusic
    private lateinit var ListRecyclerView_Music: RecyclerView
    private fun startMusicRecyclerView(){
        if (state_MusicRecyclerView_started) return
        state_MusicRecyclerView_started = true
        //设置列表布局管理器
        ListRecyclerView_Music.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //注册点击事件
        main_music_list_adapter = RecyclerAdapterMusic(
            context = this,
            onItemClick = { uri ->
                ToolVibrate().vibrate(this@MainActivity)
                startMusicPlayer(uri)
            },
            onOptionsClick = { item, view ->
                ToolVibrate().vibrate(this@MainActivity)

            },
        )
        //增加sidePadding
        ListRecyclerView_Music.setPadding(0, 0, 0, 300)
        //设置adapter
        ListRecyclerView_Music.adapter = main_music_list_adapter
        //分页加载
        val pager = Pager(
            PagingConfig(
                pageSize = 25,
                prefetchDistance = 40,
                enablePlaceholders = false,
                initialLoadSize = 200,
                maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,
                jumpThreshold = Int.MIN_VALUE
            )
        ) {
            MediaDataBaseReaderForMusic(context = this@MainActivity)
        }
        //分页加载数据
        lifecycleScope.launch {
            pager.flow.collect { pagingData ->
                main_music_list_adapter.submitData(pagingData)
            }
        }
    }



    //获取上次播放记录
    private fun getLastRecordMedia(): Uri?{
        //获取上次播放记录
        val MediaInfo_MediaUriString = MediaRecordManager(this@MainActivity).takeOneRecordUri()

        //检查上次播放记录是否有效
        if (MediaInfo_MediaUriString.isEmpty()) return null
        if (!MediaInfoRetriever.isUriStringValid(this,MediaInfo_MediaUriString)) return null

        //播放记录有效

        return MediaInfo_MediaUriString.toUri()
    }
    //启动MiniView观察者
    private var state_miniView_showing = false
    private var miniViewObserverRunning = false
    private fun startMiniViewObserver() {
        if (miniViewObserverRunning) return
        miniViewObserverRunning = true
        consoleLog("startMiniViewObserver 启动MiniView观察者 ")
        //启动MiniView观察者
        lifecycleScope.launch {
            //观察媒体变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInFoCenter.uriString.collect { uriString ->
                    consoleLog("MiniView观察者 观察到媒体变更: $uriString")
                    showMiniViewLongProcess()
                }
            }
        }
        lifecycleScope.launch {
            //观察播放状态变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInFoCenter.isPlaying.collect { newState ->
                    consoleLog("MiniView观察者 观察到播放状态变更: newState = $newState")
                    updateMiniViewPauseButton(newState)
                }
            }
        }
    }
    //显示MiniView LongProcess-把任务全部执行完,禁止扔到其他函数作用域
    private fun showMiniViewLongProcess(){
        //从PlayerStateMediaInfo获取所有信息
        val MediaInfoPack = PlayerInFoCenter.getMediaInfoPack()
        if (MediaInfoPack == null) {
            miniView_RetractAnim()
            return
        }
        val uriNumOnly = MediaInfoPack.MediaInfo_MediaUriNumOnly   //用于获取缩略图
        val type = MediaInfoPack.MediaInfo_MediaType
        val fileName = MediaInfoPack.MediaInfo_FileName       //显示文件名
        val artist = MediaInfoPack.MediaInfo_MediaArtist   //显示艺术家
        //确保MiniView已显示
        if (!state_miniView_showing) {
            miniView_ExpandAnim()
        }
        //
        PlayingCard_TextMediaName.text = fileName
        PlayingCard_TextMediaArtist.text = artist
        //
        updateMiniViewArtwork(type, uriNumOnly)

    }
    private fun initMiniView(){
        //视图初始化
        PlayingCard = findViewById(R.id.PlayingCard)
        PlayingCard_Artwork = findViewById(R.id.PlayingCard_Artwork)
        PlayingCard_InfoContainer = findViewById(R.id.PlayingCard_InfoContainer)
        PlayingCard_TextMediaName = findViewById(R.id.PlayingCard_MediaName)
        PlayingCard_TextMediaArtist = findViewById(R.id.PlayingCard_MediaArtist)
        PlayingCard_ButtonPlay = findViewById(R.id.PlayingCard_ButtonPlay)
        PlayingCard_ButtonList = findViewById(R.id.PlayingCard_ButtonList)
        //点击事件设定
        PlayingCard_InfoContainer.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //启动播放页
            val uri = PlayerInFoCenter.getMediaUriString(this).second.toUri()
            consoleLog("PlayingCard_InfoContainer 点击事件 触发播放页: $uri")
            startPlayerFromMiniView(uri)
        }
        PlayingCard_ButtonPlay.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //选择执行播放或暂停
            if (PlayerSingleton.getState_isNowPlaying()){
                PlayerSingleton.pausePlay()
            }else{
                PlayerSingleton.continuePlay(true,this)
            }
        }
        PlayingCard_ButtonList.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //防止快速点击
            if (System.currentTimeMillis() - lock_clickMillisLock < 800) {
                return@setOnClickListener
            }
            lock_clickMillisLock = System.currentTimeMillis()
            //启动播放列表
            FragmentPlayList.Companion.newInstance().show(supportFragmentManager, "PlayerListFragment")
        }

    }
    private fun updateMiniViewPauseButton(isPlaying: Boolean){
        //获取当前播放状态
        PlayingCard_ButtonPlay.setImageResource(if (isPlaying) R.drawable.ic_main_controller_pause else R.drawable.ic_main_controller_play)
    }
    private fun updateMiniViewArtwork(type: String,uriNumOnly: Long){
        if (!state_miniView_showing) return
        val useImage = SettingsRequestCenter.get_PREFS_DisableMainPageSmallPlayer(this)
        if (useImage){
            updateMiniViewArtwork_Image(uriNumOnly.toString(), type)
        }else{
            when(type){
                MediaTypeCenter.mediaType_Music -> updateMiniViewArtwork_Image(uriNumOnly.toString(), type)
                MediaTypeCenter.mediaType_Video -> updateMiniViewArtwork_Video()
            }
        }

    }
    private fun updateMiniViewArtwork_Image(uriNumOnly: String, type: String){
        //变换卡片大小
        fun transformCardSize_toSquare(){
            //保持卡片高度不变
            val cardHeight = PlayingCard_Artwork.height
            val cardWidth = PlayingCard_Artwork.width
            //卡片已是目标宽度
            if (cardWidth == cardHeight) return

            //变换卡片宽度
            val animator = ValueAnimator.ofInt(cardWidth, cardHeight)
            animator.duration = 500
            animator.interpolator = DecelerateInterpolator(2f)
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                val layoutParams = PlayingCard_Artwork.layoutParams
                layoutParams.width = animatedValue
                PlayingCard_Artwork.layoutParams = layoutParams
            }
            animator.start()
            PlayingCard_Artwork.layoutParams.width = cardHeight

        }

        //当前不是图片类型时,清除子视图并重建为图片视图
        if (state_MiniViewArtwork_type != mini_view_type_image){
            //清除所有子视图
            PlayingCard_Artwork.removeAllViews()
            PlayingCard_Artwork_Video = null
            //变换卡片宽高
            transformCardSize_toSquare()
            //创建图片视图
            PlayingCard_Artwork_Image = ImageView(this).apply {

                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                scaleType = ImageView.ScaleType.CENTER_CROP

            }
            //添加图片视图
            PlayingCard_Artwork.addView(PlayingCard_Artwork_Image)
            state_MiniViewArtwork_type = mini_view_type_image
        }

        //判断是否为同一张图片
        if (state_MiniViewArtwork_ImageUri == uriNumOnly) return

        //获取图片
        val Bitmap = ArtworkFrameManager.get_Artwork_Frame_Bitmap(this, type, uriNumOnly.toLong())

        //置入图片
        if (Bitmap == null){
            return
        }else{
            PlayingCard_Artwork_Image?.setImageBitmap(Bitmap)

            PlayingCard_Artwork_Image?.setOnClickListener {
                updateMiniViewArtwork_Image(uriNumOnly, type)
            }

            state_MiniViewArtwork_ImageUri = uriNumOnly
        }
    }
    private fun updateMiniViewArtwork_Video(){
        //绑定到视频
        fun connectToPlayEngine(){
            PlayingCard_Artwork_Video?.player = null
            PlayingCard_Artwork_Video?.player = PlayerSingleton.getPlayer()
        }
        //变换卡片宽度
        fun transformCardSize_adaptVideo(){
            //保持卡片高度不变
            lifecycleScope.launch {
                //获取当前插画区域宽高
                var cardHeight = PlayingCard_Artwork.height
                var cardWidth = PlayingCard_Artwork.width
                //确保获取到正确的宽高(待添加退避措施)
                do {
                    delay(10)
                    cardHeight = PlayingCard_Artwork.height
                    cardWidth = PlayingCard_Artwork.width
                } while (cardWidth == 0)


                //获取视频宽高比,计算目标高度px
                val aspectRatio = PlayerInFoCenter.getMediaAspectRatio(this@MainActivity).second
                //计算目标宽度
                var targetWidth = (cardHeight * aspectRatio).toInt()
                //数值过滤：卡片宽度不得小于高度,不得大于两倍高度
                if (targetWidth < cardHeight) targetWidth = cardHeight
                if (targetWidth > cardHeight * 2) targetWidth = (cardHeight * 2)

                //卡片已是目标宽度时跳过
                if (cardWidth == targetWidth) return@launch

                withContext(Dispatchers.Main) {
                    //变换卡片宽度
                    val animator = ValueAnimator.ofInt(cardWidth, targetWidth)
                    animator.duration = 500
                    animator.interpolator = DecelerateInterpolator(2f)
                    animator.addUpdateListener { animation ->
                        val animatedValue = animation.animatedValue as Int
                        val layoutParams = PlayingCard_Artwork.layoutParams
                        layoutParams.width = animatedValue
                        PlayingCard_Artwork.layoutParams = layoutParams
                    }
                    animator.start()

                }
            }
        }

        //当前不是视频类型时清除所有子视图
        if (state_MiniViewArtwork_type != mini_view_type_video){
            //清除所有子视图
            PlayingCard_Artwork.removeAllViews()
            PlayingCard_Artwork_Image = null
            state_MiniViewArtwork_ImageUri = ""
            //创建视频视图
            PlayingCard_Artwork_Video = PlayerView(this).apply {

                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                useController = false
            }
            //添加视频视图
            PlayingCard_Artwork.addView(PlayingCard_Artwork_Video)
            state_MiniViewArtwork_type = mini_view_type_video
        }

        //变换卡片宽度
        transformCardSize_adaptVideo()

        //绑定到视频
        connectToPlayEngine()

        //(测试用)点击重新绑定视频视图
        PlayingCard_Artwork_Video?.setOnClickListener {
            updateMiniViewArtwork_Video()
        }

    }
    val mini_view_type_null = "mini_view_type_null"
    val mini_view_type_image = "mini_view_type_image"
    val mini_view_type_video = "mini_view_type_video"
    private var state_MiniViewArtwork_type = mini_view_type_null
    private var state_MiniViewArtwork_ImageUri = ""
    private fun miniView_ExpandAnim(){
        if (state_miniView_showing) return
        state_miniView_showing = true

        PlayingCard.visibility = View.VISIBLE
        PlayingCard.translationY = 300f
        PlayingCard.animate()
            .translationY(0f)
            .setInterpolator(DecelerateInterpolator(2f))
            .setDuration(500)
            .start()
    }
    private fun miniView_RetractAnim(){
        if (!state_miniView_showing) return
        state_miniView_showing = false


        PlayingCard.animate().translationY(300f)
            .withEndAction{ PlayingCard.visibility = View.GONE }
            .setInterpolator(DecelerateInterpolator(2f))
            .setDuration(800).start()
    }
    //MiniView视图合集
    private lateinit var PlayingCard: CardView
    private lateinit var PlayingCard_InfoContainer: LinearLayout
    private lateinit var PlayingCard_TextMediaName: TextView
    private lateinit var PlayingCard_TextMediaArtist: TextView
    private lateinit var PlayingCard_Artwork: CardView
    private var PlayingCard_Artwork_Image: ImageView ? = null
    private var PlayingCard_Artwork_Video: PlayerView ? = null
    private lateinit var PlayingCard_ButtonPlay: ImageButton
    private lateinit var PlayingCard_ButtonList: ImageButton


    //检查是否有媒体正在在播放并获取链接
    private fun isAnyMediaOngoing(): Pair<Boolean, String>{
        //从播放器获取当前媒体状态
        val (ongoing,currentMediaItem) = PlayerSingleton.getState_currentMediaItem_Uri()

        return if (ongoing){
            val currentMediaUriString = currentMediaItem.toString()
            Pair(true,currentMediaUriString)
        }else{
            Pair(false,"")
        }
    }

    //设置新的媒体项
    private fun setMediaItem(MediaInfo_MediaUri: Uri, playWhenReady: Boolean){
        //确保播放器已经启动
        PlayerSingleton.getInitPlayer(application)
        PlayerSingleton.addPlayerStateListener()

        //确认设置新媒体项
        PlayerSingleton.setMediaItem(MediaInfo_MediaUri, playWhenReady, this)
    }

    //从选单发起后台播放
    private fun startMiniViewPlay(uri: Uri){
        //比对上次播放媒体信息与当前播放媒体信息
        val newUri = uri.toString()
        val (_,currentUri) = PlayerInFoCenter.getMediaUriString(this)
        if (newUri == currentUri){
            showCustomToast("已在播放该媒体",3)
            PlayerSingleton.continuePlay(true,this)
            return
        }
        //设置新播放项
        setMediaItem(uri, true)

    }

    //从读取本地视频和音乐(列表自动感知刷新)
    private fun startLocalMediaReader(flag_video_or_music: String){
        setLoadingText("正在读取本地媒体", false, 0)
        DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_start)
        //发起加载
        lifecycleScope.launch(Dispatchers.IO) {
            when(flag_video_or_music){
                "video" -> {
                    val mediaReader = MediaStoreReaderForVideo(this@MainActivity, contentResolver)
                    mediaReader.readAndSaveAllVideos()
                }
                "music" -> {
                    val musicReader = MediaStoreReaderForMusic(this@MainActivity, contentResolver)
                    musicReader.readAndSaveAllMusics()
                }
            }
        }
    }


    //页签切换变更页面信息
    private fun setMusicElement(){
        mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_music
        AppBarTitle.text = "音乐"
        ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))

        ListRecyclerView_Video.visibility = View.GONE
        ListRecyclerView_Music.visibility = View.VISIBLE


    }
    private fun setVideoElement(){
        mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_video
        AppBarTitle.text = "视频"
        ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))

        ListRecyclerView_Video.visibility = View.VISIBLE
        ListRecyclerView_Music.visibility = View.GONE

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
            showCustomToast("界面重置函数接收到预期外的参数",3)
        }
    }
    private fun listGoTop(){
        when (mainViewModel.state_current_tab) {
            SettingsRequestCenter.tab_mark_music -> {
                if (!state_MusicRecyclerView_started) return

                main_music_list_adapter.refresh()

                ListRecyclerView_Music.smoothScrollToPosition(0)
            }
            SettingsRequestCenter.tab_mark_video -> {
                if (!state_VideoRecyclerView_started) return

                main_video_list_adapter.refresh()

                ListRecyclerView_Video.smoothScrollToPosition(0)
            }
        }
    }
    //界面控件元素
    private lateinit var AppBarTitle: TextView
    private lateinit var AppBarNoticeText: TextView
    private lateinit var ButtonCardMusic: CardView
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardGallery: CardView

    //刷新列表
    private fun refreshList(){
        //检查当前所在列表
        when (mainViewModel.state_current_tab) {
            SettingsRequestCenter.tab_mark_music -> {
                if (!state_MusicRecyclerView_started) return

                main_music_list_adapter.refresh()

            }
            SettingsRequestCenter.tab_mark_video -> {
                if (!state_VideoRecyclerView_started) return

                main_video_list_adapter.refresh()
            }
        }
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




    //启动播放器
    private fun startVideoPlayer(uri: Uri){
        //防止快速发起
        if (System.currentTimeMillis() - lock_clickMillisLock < 800) return
        lock_clickMillisLock = System.currentTimeMillis()

        //确认启动
        val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)
        when(playPageType){
            0 -> {
                //构建intent
                val intent = Intent(this, PlayerActivityOro::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .putExtra("uri", uri)
                    .putExtra("IntentSource", 3)
                //构建可选参数
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.slide_in_vertical,
                    R.anim.slide_dont_move
                )

                //启动活动
                if (state_miniView_showing){
                    startActivity(intent, options.toBundle())

                }else{
                    startActivity(intent)
                }

            }
            1 -> {
                //构建intent
                val intent = Intent(this, PlayerActivityNeo::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .putExtra("uri", uri)
                    .putExtra("IntentSource", 3)
                //构建可选参数
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.slide_in_vertical,
                    R.anim.slide_dont_move
                )

                //启动活动
                if (state_miniView_showing){
                    startActivity(intent, options.toBundle())

                }else{
                    startActivity(intent)
                }
            }
        }

    }
    private fun startMusicPlayer(uri: Uri){
        //防止快速发起
        if (System.currentTimeMillis() - lock_clickMillisLock < 800) return
        lock_clickMillisLock = System.currentTimeMillis()

        //暂时仅设置音乐
        setMediaItem(uri, true)

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
    private fun startPlayerFromMiniView(uri: Uri){
        val (_,MediaInfo_MediaType) = PlayerInFoCenter.getMediaInfoType(this,uri.toString())
        consoleLog("PlayingCard_InfoContainer 点击事件 媒体类型: $MediaInfo_MediaType")
        when (MediaInfo_MediaType) {
            MediaTypeCenter.mediaType_Video -> {
                startVideoPlayer(uri)
            }
            MediaTypeCenter.mediaType_Music -> {
                showCustomToast("暂不支持打开音乐播放页面",3)
                //startMusicPlayer(uri)
            }
            else -> {
                showCustomToast("严重错误 未知媒体类型",3)
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
    //检查权限
    private fun checkPermissionAndVersion(): Pair<Boolean, Boolean>{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Pair(Environment.isExternalStorageManager(), true)
        }else{
            val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE
            Pair(ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED, false)
        }
    }
    private fun startSettingPage(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            showCustomToast("请先开启管理所有文件权限",3)

            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }else{

            showCustomToast("请先开启媒体访问文件权限",3)

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = "package:$packageName".toUri() }
            startActivity(intent)
        }
    }
    //检查权限并直接跳转
    private fun checkPermissionAndStartSettingPage() {
        val (isPermissionGranted, _) = checkPermissionAndVersion()
        if (!isPermissionGranted){
            startSettingPage()
        }
    }
    //事件观察
    private var eventObserver_started = false
    private fun setupEventObserver() {
        if (eventObserver_started){ return }
        eventObserver_started = true
        //启动观察列表状态
        lifecycleScope.launch {
            //观察加载状态变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DataBaseStateConnector.state_queryDisk.collect { state ->
                    consoleLog("观察到加载状态变更: $state")
                    //读取完成
                    if (state.contains(DataBaseStateConnector.state_queryDisk_success)) {
                        //刷新列表
                        setLoadingText("读取完成",true,2000)

                        //刷新列表
                        refreshList()
                    }
                }
            }
        }
        //启动杂项观察
        lifecycleScope.launch {
            //观察杂项连接器变更
            ConnectCenter.state_connector.collect { state ->
                consoleLog("观察到杂项连接器变更: $state")
                //更新封面帧
                if (state.contains(ConnectCenter.connector_event_cover_frame_update)){
                    val uriNumOnly = ConnectCenter.getCoverFrameUpdateEvent_targetUriNumOnly()
                    main_video_list_adapter.updateCoverForVideo(uriNumOnly)
                }
            }
        }

    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "MainActivity: $msg")
        }
    }

}