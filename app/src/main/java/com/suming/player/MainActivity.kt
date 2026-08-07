package com.suming.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.iterator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.ActivityComponent.MainActivity.FragmentMusicStoreSetting
import com.suming.player.ActivityComponent.MainActivity.FragmentVideoStoreSetting
import com.suming.player.ActivityComponent.MainActivity.MainViewModel
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterMusic
import com.suming.player.ActivityComponent.MainActivity.RecyclerAdapterVideo
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreRepo
import com.suming.player.DataPack.DataBaseMusicStore.MusicStoreRepo
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.MediaDataReader.MediaDataBaseReaderForMusic
import com.suming.player.DataPack.MediaDataReader.MediaDataBaseReaderForVideo
import com.suming.player.DataPack.MediaDataReader.MediaStoreReaderForMusic
import com.suming.player.DataPack.MediaDataReader.MediaStoreReaderForVideo
import com.suming.player.DataPack.MediaRecordPack
import com.suming.player.FuncPack_ListManager.ListManagerFragment
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.ConnectCenter
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("NewApi")
@OptIn(UnstableApi::class)
class MainActivity: AppCompatActivity() {
    //连接ViewModel
    private val mainViewModel: MainViewModel by viewModels()

    //防止快速点击
    private var lock_clickMillisLock = 0L



    @SuppressLint("ClickableViewAccessibility" )
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //界面设置
        initDisplay()
        //初始化
        init()



        //注册界面控件
        register()

        //注册Fragment监听器
        registerFragment()

        //主业务
        mainBusiness(savedInstanceState)

        //显示列表
        //showMediaList(savedInstanceState)

        setupEventObserver()








    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun init(){
        //获取MiniView视图
        initMiniView()

        //手势监听(Activity)
        /*
        lifecycleScope.launch (Dispatchers.Main) {
            delay(500)
            //监听返回手势
            onBackPressedDispatcher.addCallback(this@MainActivity, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(false)
                }
            })
        }

         */
    }





    //注册Fragment监听器
    private fun registerFragment(){
        lifecycleScope.launch (Dispatchers.Main) {
            delay(500)
            //视频媒体库设置返回值
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_video_store_setting, this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString(FragmentConnector.receive_key)
                when(ReceiveKey){
                    FragmentConnector.fragment_media_store_setting_require_recyclerview_refresh -> {
                        main_video_list_adapter.refresh()
                    }
                    FragmentConnector.fragment_media_store_setting_require_mediastore_api_refresh -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            startLocalMediaReader(MediaType.Video)
                        }
                    }
                }
            }
            //音乐媒体库设置返回值
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_music_store_setting, this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString(FragmentConnector.receive_key)
                when(ReceiveKey){
                    FragmentConnector.fragment_media_store_setting_require_recyclerview_refresh -> {
                        main_music_list_adapter.refresh()
                    }
                    FragmentConnector.fragment_media_store_setting_require_mediastore_api_refresh -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            startLocalMediaReader(MediaType.Audio)
                        }
                    }
                }
            }
            //播放列表返回值
            supportFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_play_list, this@MainActivity) { _, bundle ->
                val ReceiveKey = bundle.getString(FragmentConnector.receive_key)
                when(ReceiveKey){
                    FragmentConnector.fragment_event_close -> {
                        consoleLog("播放列表Fragment关闭")
                    }

                }
            }
        }
    }
    private fun startFragment_MSS(){
        FragmentMusicStoreSetting.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_music_store_setting)
    }
    private fun startFragment_VSS(){
        FragmentVideoStoreSetting.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_video_store_setting)
    }
    //注册界面控件
    private fun register(){
        lifecycleScope.launch (Dispatchers.Main) {
            delay(1000)

            //按钮：指南
            val ButtonGuidance = findViewById<Button>(R.id.buttonGuidance)
            ButtonGuidance.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                ListRecyclerView_Video.stopScroll()
                ListRecyclerView_Music.stopScroll()
                //
                val intent = Intent(this@MainActivity, GuidanceActivity::class.java)
                startActivity(intent)
            }
            ButtonGuidance.visibility = View.VISIBLE
            ButtonGuidance.alpha = 0f
            ButtonGuidance.animate().alpha(1f).setDuration(300).start()
            //按钮：设置
            val ButtonSettings= findViewById<Button>(R.id.buttonSetting)
            ButtonSettings.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                ListRecyclerView_Video.stopScroll()
                ListRecyclerView_Music.stopScroll()

                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
            ButtonSettings.visibility = View.VISIBLE
            ButtonSettings.alpha = 0f
            ButtonSettings.animate().alpha(1f).setDuration(300).start()
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
                ListRecyclerView_Video.stopScroll()
                ListRecyclerView_Music.stopScroll()
                //
                when(mainViewModel.state_current_tab){
                    SettingsRequestCenter.tab_mark_video -> {
                        startFragment_VSS()
                    }
                    SettingsRequestCenter.tab_mark_music -> {
                        startFragment_MSS()
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
    //主业务
    private fun mainBusiness(savedInstanceState: Bundle?){
        lifecycleScope.launch (Dispatchers.IO) {
            //检查隐私与权限
            val (needStart, isStoragePermissionValid) = checkNeedStartPrivacyPermissionActivity()
            //需要显示权限与隐私页面
            if (needStart){
                //Handler(Looper.getMainLooper()).postDelayed({   }, 1000)
                //启动隐私权限面板
                startPrivacyPermissionActivity()
            }else{
                //已获得储存权限,显示主界面
                if (isStoragePermissionValid){
                    //显示列表
                    withContext(Dispatchers.Main){
                        showMediaList(savedInstanceState)


                    }

                    delay(1000)

                    startListUnderTopObserver()
                    //
                    withContext(Dispatchers.Main) {
                        //启动MiniView观察者
                        startMiniViewObserver()

                        val isAnyMediaOngoing = withContext(Dispatchers.Main){ isAnyMediaOngoing().first }
                        if (!isAnyMediaOngoing){
                            //没有媒体正在播放,从记录中获取上次停留的媒体信息(已检查是否有效)
                            val MediaInfo_MediaRecordPack = getLastMediaRecord()
                            if (MediaInfo_MediaRecordPack != null) {
                                withContext(Dispatchers.Main) {
                                    if (SettingsRequestCenter.get_PREFS_EnableContinuePlay(this@MainActivity)){
                                        //setMediaItem(MediaInfo_MediaRecordPack.uriStandard.to, false)
                                    }
                                }
                            }
                        }
                    }



                }else{
                    showOpenFileButton()
                }

            }
        }
    }

    //检查是否需要启动隐私权限面板(返回：是否需要启动,是否已获得储存权限)
    private fun checkNeedStartPrivacyPermissionActivity(): Pair<Boolean, Boolean>{
        val PrivacyPermissionHelper = PrivacyPermissionHelper()
        val isPrivacyAgreed = PrivacyPermissionHelper.checkPrivacyAgreed(this@MainActivity)
        val isStoragePermissionValid = PrivacyPermissionHelper.checkPermissionValidity(this@MainActivity)

        if (!isPrivacyAgreed){
            return Pair(true, isStoragePermissionValid)
        }else{
            val isIgnoreStorageNeverAlert = PrivacyPermissionHelper.GET_PREFS_IgnoreStorageNeverAlert(this@MainActivity)

            //仅在未忽略储存权限且储存权限未通过时才需要弹出请求页面
            return Pair(!isStoragePermissionValid && !isIgnoreStorageNeverAlert, isStoragePermissionValid)
        }
    }
    //启动隐私权限面板(使用DetailedLauncher)
    private fun startPrivacyPermissionActivity(){
        val intent = Intent(this, PrivacyPermissionActivity::class.java)
        //构建可选参数
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_vertical,
            R.anim.slide_dont_move
        )
        //启动活动
        privacyPermissionLauncher.launch(intent,  options)
    }
    //ActivityResult接收器
    private val privacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            //提取数据
            val dataString = result.data?.getStringExtra(ActivityResultConnector.ARAPI_Privacy)
            when(dataString){
                ActivityResultConnector.ARAPI_Privacy_continue_without_storage_permission -> {
                    showOpenFileButton()
                }
                ActivityResultConnector.ARAPI_Privacy_continue_with_success_permit -> {
                    showMediaList()
                }
            }




        }
    }
    //显示打开文件并隐藏列表
    private fun showOpenFileButton(){
        level_list.visibility = View.GONE
        level_openFile.visibility = View.VISIBLE
    }


    //显示页面
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showMediaList(savedInstanceState: Bundle? = null){
        lifecycleScope.launch(Dispatchers.IO){
            //检查权限
            val isPermissionGranted = checkPermissionAndVersion()
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
            }
        }
    }

    //读取上一次的页面
    private fun readLastPageThenShow(): String{
        val State_LastStayTab = SettingsRequestCenter.get_State_LastStayTab(this@MainActivity)
        //consoleLog("showMediaList : State_LastStayTab = $State_LastStayTab")

        return State_LastStayTab
    }
    //显示视频列表
    private fun showVideoList(){
        lifecycleScope.launch(Dispatchers.IO) {
            //页面标识防重复
            if (mainViewModel.state_current_tab == SettingsRequestCenter.tab_mark_video && state_VideoRecyclerView_started){
                withContext(Dispatchers.Main){
                    setListToTop()
                }
                return@launch
            }
            mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_video

            //发起切换
            withContext(Dispatchers.Main){
                //界面切换
                setList(SettingsRequestCenter.tab_mark_video)
                //加载事务
                showVideoListCore()
            }

            //记录状态
            SettingsRequestCenter.set_State_LastStayTab(this@MainActivity, SettingsRequestCenter.tab_mark_video)

        }
    }
    //显示音乐列表
    private fun showMusicList(){
        lifecycleScope.launch(Dispatchers.IO) {
            //页面标识防重复
            if (mainViewModel.state_current_tab == SettingsRequestCenter.tab_mark_music && state_MusicRecyclerView_started){
                withContext(Dispatchers.Main){
                    setListToTop()
                }
                return@launch
            }
            mainViewModel.state_current_tab = SettingsRequestCenter.tab_mark_music

            //发起切换
            withContext(Dispatchers.Main){
                //界面切换
                setList(SettingsRequestCenter.tab_mark_music)
                //加载事务
                showMusicListCore()
            }

            //记录状态
            SettingsRequestCenter.set_State_LastStayTab(this@MainActivity, SettingsRequestCenter.tab_mark_music)

        }
    }

    //视频列表核心
    private fun showVideoListCore(){
        //启动视频列表
        startVideoRecyclerView()
        //检查是否需要读取系统视频
        lifecycleScope.launch(Dispatchers.IO) {
            //获取强制每次读取标识
            val queryNew = SettingsRequestCenter.get_PREFS_QueryNewMediaOnStart(this@MainActivity)
            //检查本地数据库是否已有视频数据
            if (MediaStoreRepo(this@MainActivity).isEmpty() || queryNew){
                //consoleLog("showVideoListCore: 本地数据库视频数据为空 触发读取媒体库视频")
                //从系统读取视频
                startLocalMediaReader(MediaType.Video)
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
            onClick_Duration = { item ->
                ToolVibrate().vibrate(this@MainActivity)
                notice("视频时长:${FormatTime_withChar(item.media_durationMs)}", 2000)
            },
            onClick_tvFormat = { item ->
                ToolVibrate().vibrate(this@MainActivity)
                notice("视频格式:${item.media_format}", 3000)
            },
            onClick_Options = { item, holder ->
                val popup = PopupMenu(holder.itemView.context, holder.tvOption)
                popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
                val popup_update_cover = popup.menu.findItem(R.id.MenuAction_Repic)
                val popup_hide_item = popup.menu.findItem(R.id.MenuAction_Hide)
                val popup_onSmallCardPlay = popup.menu.findItem(R.id.MenuAction_onSmallCardPlay)
                popup.show()
                //注册点击事件
                popup_update_cover.setOnMenuItemClickListener {
                    ToolVibrate().vibrate(this@MainActivity)
                    showCustomToast("进入视频后,可在更多选项面板更新封面", 3)
                    true
                }
                popup_hide_item.setOnMenuItemClickListener {
                    ToolVibrate().vibrate(this@MainActivity)
                    showCustomToast("功能开发中", 3)
                    true
                }
                popup_onSmallCardPlay.setOnMenuItemClickListener {
                    ToolVibrate().vibrate(this@MainActivity)
                    startMiniViewPlay(item.content_uriString.toUri())
                    true
                }
            },
        )
        //设置adapter
        ListRecyclerView_Video.adapter = main_video_list_adapter
        //加载视频数据
        lifecycleScope.launch(Dispatchers.IO) {
            val pager = Pager(PagingConfig(pageSize = 20)) {
                MediaDataBaseReaderForVideo(context = this@MainActivity)
            }
            pager.flow.collect { pagingData ->
                main_video_list_adapter.submitData(pagingData)
            }
        }
    }
    //音乐列表核心
    private fun showMusicListCore(){
        //启动音乐列表
        startMusicRecyclerView()
        //检查本地数据库是否已有音乐数据
        lifecycleScope.launch(Dispatchers.IO) {
            val queryNew = SettingsRequestCenter.get_PREFS_QueryNewMediaOnStart(this@MainActivity)
            if (MusicStoreRepo(this@MainActivity).isEmpty() || queryNew){
                //consoleLog("showMusicList数据库音乐数据为空,触发读取媒体库音乐")
                //从系统读取音乐
                startLocalMediaReader(MediaType.Audio)
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
        //设置adapter
        ListRecyclerView_Music.adapter = main_music_list_adapter
        //分页加载数据
        lifecycleScope.launch(Dispatchers.IO) {
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
            pager.flow.collect { pagingData ->
                main_music_list_adapter.submitData(pagingData)
            }
        }
    }



    //获取上次播放记录
    private fun getLastMediaRecord(): MediaRecordPack?{
        //获取上次播放记录
        val MediaRecordManager = MediaRecordManager()
        val MediaInfo_MediaRecordPack = MediaRecordManager.readRecord(this@MainActivity)

        //检查必要字段是否为空
        if (MediaInfo_MediaRecordPack.fileFullPath.isEmpty()) return null
        //提取记录中的完整文件路径,检查该文件是否还存在
        val file = File(MediaInfo_MediaRecordPack.fileFullPath)
        if (!file.exists()) return null
        //检查是否可解码该文件
        if (!MediaInfoRetriever.isUriStringValid(this,MediaInfo_MediaRecordPack.uriStandard)) return null

        //播放记录有效
        return MediaInfo_MediaRecordPack
    }
    //启动MiniView观察者
    private var miniViewObserverRunning = false
    private fun startMiniViewObserver() {
        if (miniViewObserverRunning) return
        miniViewObserverRunning = true

        //启动MiniView观察者
        lifecycleScope.launch {
            //观察正在播放的媒体项变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.uriString.collect { uriString ->
                    //consoleLog("MiniView观察者 当前媒体: $uriString")
                    showMiniViewLongProcess()
                }
            }
        }
        lifecycleScope.launch {
            //观察播放状态变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.isPlaying.collect { newState ->
                    //consoleLog("MiniView观察者 当前播放状态: $newState")
                    //刷新操作按钮
                    updateMiniViewPauseButton(newState)
                }
            }
        }
    }
    //显示MiniView LongProcess-把任务全部执行完,禁止扔到其他函数作用域
    private fun showMiniViewLongProcess(){
        //从PlayerStateMediaInfo获取所有信息
        val MediaInfoPack = PlayerInfoCenter.getMediaInfoPack()
        if (MediaInfoPack == null) {
            miniView_clear()
            return
        }
        val uriNumOnly = MediaInfoPack.MediaInfo_MediaUriNumOnly   //用于获取缩略图
        val type = MediaInfoPack.MediaInfo_MediaType
        val fileName = MediaInfoPack.MediaInfo_FileName       //显示文件名
        val artist = MediaInfoPack.MediaInfo_MediaArtist   //显示艺术家

        //
        PlayingCard_TextMediaName.text = fileName
        PlayingCard_TextMediaArtist.text = artist
        PlayingCard_ButtonPlay.visibility = View.VISIBLE
        //
        updateMiniViewArtwork(type, uriNumOnly)

    }
    private fun initMiniView(){
        //视图初始化
        PlayingCard = findViewById(R.id.level_miniView)
        PlayingCard_Artwork = findViewById(R.id.PlayingCard_Artwork)
        PlayingCard_InfoContainer = findViewById(R.id.PlayingCard_InfoContainer)
        PlayingCard_TextMediaName = findViewById(R.id.PlayingCard_MediaName)
        PlayingCard_TextMediaArtist = findViewById(R.id.PlayingCard_MediaArtist)
        PlayingCard_ButtonPlay = findViewById(R.id.PlayingCard_ButtonPlay)
        PlayingCard_ButtonList = findViewById(R.id.PlayingCard_ButtonList)
        //点击事件设定
        PlayingCard_InfoContainer.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //停止列表防卡顿
            ListRecyclerView_Video.stopScroll()
            ListRecyclerView_Music.stopScroll()
            //启动播放页
            val uriString = PlayerInfoCenter.getMediaUriString()
            if (uriString != ""){
                val uri = uriString.toUri()
                consoleLog("PlayingCard_InfoContainer 点击事件 触发播放页: $uri")
                startPlayerFromMiniView(uri)
            }else{
                if (state_MiniViewArtwork_type == mini_view_type_null){
                    showCustomToast("选择一项媒体以开始播放")
                }else{
                    showCustomToast("失败")
                }
            }
        }
        PlayingCard_ButtonPlay.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //选择执行播放或暂停
            if (PlayerSingleton.getState_isNowPlaying()){
                PlayerSingleton.pausePlay()
            }else{
                PlayerSingleton.continuePlay(true)
            }
        }
        PlayingCard_ButtonList.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)
            //防止快速点击
            if (System.currentTimeMillis() - lock_clickMillisLock < 800) {
                return@setOnClickListener
            }
            lock_clickMillisLock = System.currentTimeMillis()
            //停止列表防卡顿
            ListRecyclerView_Video.stopScroll()
            ListRecyclerView_Music.stopScroll()
            //启动播放列表
            startPlayListFragment()
        }

    }
    private fun updateMiniViewPauseButton(isPlaying: Boolean){
        //更新操作按钮图标
        PlayingCard_ButtonPlay.setImageResource(if (isPlaying) R.drawable.ic_main_controller_pause else R.drawable.ic_main_controller_play)
    }
    private fun updateMiniViewArtwork(type: String,uriNumOnly: Long){
        val useImage = SettingsRequestCenter.get_PREFS_DisableMainPageSmallPlayer(this)
        if (useImage){
            updateMiniViewArtwork_Image(uriNumOnly.toString(), type)
        }else{
            when(type){
                MediaType.Audio -> updateMiniViewArtwork_Image(uriNumOnly.toString(), type)
                MediaType.Video -> updateMiniViewArtwork_Video()
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
        val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(this, type, uriNumOnly.toLong())

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
    @SuppressLint("InflateParams")
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
                val aspectRatio = PlayerInfoCenter.getMediaAspectRatio()
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
            PlayingCard_Artwork_Video = LayoutInflater.from(this)
                .inflate(R.layout.piece_player_view_texture_ver, null, false) as PlayerView


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
    private fun miniView_clear(){
        //收起卡片(已取消)
        /*
        PlayingCard.animate().translationY(300f)
            .withEndAction{ PlayingCard.visibility = View.GONE }
            .setInterpolator(DecelerateInterpolator(2f))
            .setDuration(800).start()

         */

        //
        PlayingCard_TextMediaName.text = "未在播放"
        PlayingCard_TextMediaArtist.text = "选择一项媒体以开始播放"
        PlayingCard_ButtonPlay.visibility = View.GONE

        PlayingCard_Artwork.removeAllViews()
        state_MiniViewArtwork_type = mini_view_type_null
        PlayingCard_Artwork_Video = null
        PlayingCard_Artwork_Image = null


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
        PlayerSingleton.getInitPlayer()
        PlayerSingleton.addPlayerStateListener()

        //确认设置新媒体项
        PlayerSingleton.setMediaItem(MediaInfo_MediaUri, playWhenReady)
    }
    //从选单发起后台播放
    private fun startMiniViewPlay(uri: Uri){
        //比对上次播放媒体信息与当前播放媒体信息
        val newUri = uri.toString()
        val currentUri = PlayerInfoCenter.getMediaUriString()
        if (newUri == currentUri){
            showCustomToast("已在播放该媒体",3)
            PlayerSingleton.continuePlay(true)
            return
        }
        //设置新播放项
        setMediaItem(uri, true)

    }

    //从读取本地视频和音乐数据
    private suspend fun startLocalMediaReader(mediaType: String){
        withContext(Dispatchers.Main) { setLoadingText("正在读取本地媒体", false, 0) }
        //通知状态变更
        DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_start)
        //发起加载
        when(mediaType){
            MediaType.Video -> {
                val mediaReader = MediaStoreReaderForVideo(this@MainActivity, contentResolver)
                mediaReader.readAndSaveAllVideos()
            }
            MediaType.Audio -> {
                val musicReader = MediaStoreReaderForMusic(this@MainActivity, contentResolver)
                musicReader.readAndSaveAllMusics()
            }
        }

    }

    //页签切换
    private fun setList(target: String){
        var titleText = "列表"
        var targetButtonView : CardView? = null
        val targetListView = when(target) {
            SettingsRequestCenter.tab_mark_music -> {
                titleText = "音乐"
                targetButtonView = ButtonCardMusic

                ListRecyclerView_Music
            }
            SettingsRequestCenter.tab_mark_video -> {
                titleText = "视频"
                targetButtonView = ButtonCardVideo

                ListRecyclerView_Video
            }
            else -> {
                showCustomToast("页面打开失败",3)
                finish()
                return
            }
        }

        //修改标题
        AppBarTitle.text = titleText

        //修改按钮背景颜色
        //if (targetButtonView != null){
            if (targetButtonView == ButtonCardMusic){
                ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))
                ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            }
            if (targetButtonView == ButtonCardVideo){
                ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_ON))
                ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ButtonCard_OFF))
            }
        //}


        //遍历level_list内所有列表
        for (item in level_list){
            if (item == targetListView){
                item.visibility = View.VISIBLE
            }else{
                item.visibility = View.GONE
            }

        }

        //设置列表位置监控
        setScrollListenerForList(target)

    }
    //页签视图
    private lateinit var AppBarTitle: TextView
    private lateinit var AppBarNoticeText: TextView
    private lateinit var ButtonCardMusic: CardView
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardGallery: CardView

    //列表位置监控
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val isAtTop = !recyclerView.canScrollVertically(-1)
            isListUnderTop.value = isAtTop
        }
    }
    private val isListUnderTop = MutableStateFlow(false)
    val isListUnderTopFlow: StateFlow<Boolean> = isListUnderTop.asStateFlow()
    private fun startListUnderTopObserver(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                isListUnderTopFlow.collect{
                    if (it){
                        topBar_bottomLine_Out()
                    }else{
                        topBar_bottomLine_In()
                    }
                }
            }
        }
    }
    //为列表应用位置监控
    private fun setScrollListenerForList(target: String){
        val targetListView = when(target) {
            SettingsRequestCenter.tab_mark_music -> {
                ListRecyclerView_Music
            }
            SettingsRequestCenter.tab_mark_video -> {
                ListRecyclerView_Video
            }
            else -> {
                showCustomToast("页面打开失败",3)
                finish()
                return
            }
        }
        //遍历level_list内所有recyclerView
        for (item in level_list){
            if (item == targetListView){
                //添加列表滚动监听器
                if (item is RecyclerView){
                    item.addOnScrollListener(scrollListener)
                    //额外检查一次是否在顶部(否则切换后未滚动前不会刷新)
                    isListUnderTop.value = !item.canScrollVertically(-1)
                }
            }else{
                //移除列表滚动监听器
                if (item is RecyclerView){
                    item.removeOnScrollListener(scrollListener)
                }
            }
        }
    }
    //顶部分隔线显示控制(In代表显示,Out代表隐藏)
    private lateinit var topBar_bottomLine : View
    private var isTopBar_bottomLine_In = false
    private fun topBar_bottomLine_In(){
        if (isTopBar_bottomLine_In) return
        isTopBar_bottomLine_In = true

        topBar_bottomLine.visibility = View.VISIBLE
        topBar_bottomLine.alpha = 0f
        topBar_bottomLine.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

    }
    private fun topBar_bottomLine_Out(){
        if (!isTopBar_bottomLine_In) return
        isTopBar_bottomLine_In = false

        topBar_bottomLine.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { topBar_bottomLine.visibility = View.GONE }
            .start()
    }


    //页面回到顶部
    private fun setListToTop(){
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


    //通用事件观察
    private var eventObserver_started = false
    private fun setupEventObserver() {
        if (eventObserver_started){ return }
        eventObserver_started = true
        //启动列表加载状态观察
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DataBaseStateConnector.state_queryDisk.collect { state ->
                    if (state.isEmpty()) return@collect
                    //consoleLog("观察到媒体库加载状态变更: new state: $state")
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
        //杂项观察
        lifecycleScope.launch {
            //观察杂项连接器变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ConnectCenter.state_connector.collect { state ->
                    if (state.isEmpty()) return@collect
                    //consoleLog("观察到杂项连接器变更: new state: $state")
                    //更新封面帧
                    if (state.contains(ConnectCenter.connector_event_cover_frame_update)){
                        val (targetFilePath, targetMediaId) = ConnectCenter.getCoverFrameUpdateEvent_targetFileInfo()

                        main_video_list_adapter.updateCoverForItem(targetFilePath, targetMediaId)

                        ConnectCenter.clear_connector()
                    }
                }
            }
        }

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
                startActivity(intent, options.toBundle())


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
                startActivity(intent, options.toBundle())

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
        val MediaInfo_MediaType = PlayerInfoCenter.getMediaInfoType()
        consoleLog("PlayingCard_InfoContainer 点击事件 媒体类型: $MediaInfo_MediaType")
        when (MediaInfo_MediaType) {
            MediaType.Video -> {
                startVideoPlayer(uri)
            }
            MediaType.Audio -> {
                showCustomToast("暂不支持打开音乐播放页面",3)
                //startMusicPlayer(uri)
            }
            else -> {
                showCustomToast("严重错误 未知媒体类型",3)
            }
        }
    }

    //启动播放列表Fragment面板
    private fun startPlayListFragment(){
        ListManagerFragment.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_play_list)
    }

    //显示重组
    private lateinit var level_root: ConstraintLayout
    private lateinit var level_topBar : CardView
    private lateinit var level_list : LinearLayout
    private lateinit var level_openFile : LinearLayout
    private lateinit var level_controllers : LinearLayout
    private lateinit var level_miniView : CardView
    private var isLandscape : Boolean = false
    private fun initDisplay(){
        window.attributes = window.attributes.apply {
            windowAnimations = 0
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activity)
        //初始化视图
        level_root = findViewById(R.id.activity_root_constraint)
        level_topBar = findViewById(R.id.level_topBar)
        level_list = findViewById(R.id.level_list)
        level_openFile = findViewById(R.id.level_openFile)
        level_controllers = findViewById(R.id.level_controllers)
        level_miniView = findViewById(R.id.level_miniView)
        //获取主要列表视图
        ListRecyclerView_Video = findViewById(R.id.recyclerview_video_list)
        ListRecyclerView_Music = findViewById(R.id.recyclerview_music_list)
        AppBarNoticeText = findViewById(R.id.AppBarNoticeText)
        AppBarTitle = findViewById(R.id.AppBarTitle)
        ButtonCardMusic = findViewById(R.id.ButtonCardMusic)
        ButtonCardVideo = findViewById(R.id.ButtonCardVideo)
        ButtonCardGallery = findViewById(R.id.ButtonCardGallery)
        topBar_bottomLine = findViewById(R.id.topBar_bottomLine)

        //获取横竖屏模式
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //获取屏幕信息
        getScreenInfo()

    }
    private fun getScreenInfo(){
        //获取屏幕宽高
        DeviceInfo.screenWidth = resources.displayMetrics.widthPixels
        DeviceInfo.screenHeight = resources.displayMetrics.heightPixels
        //获取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_root_constraint)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            DeviceInfo.statusBarHeight = systemBars.top

            //重组主要视图
            compose()

            insets
        }
    }
    private fun compose(){
        //重组miniView(竖屏时不修改,横排时修改为悬浮并限制长度)
        if (isLandscape){

            //限制控件宽度(不可用,必须用constraintSe)
            /*
            val params = level_miniView.layoutParams as? ConstraintLayout.LayoutParams
            params?.let {
                it.width = 0
                it.matchConstraintMaxWidth = 300.dpToPx()
                level_miniView.layoutParams = it
            }

             */

            /*
            //修改miniView
            val constraintSet = ConstraintSet()
            constraintSet.clone(level_root)
            constraintSet.clear(level_miniView.id)
            constraintSet.connect(
                level_miniView.id,
                ConstraintSet.BOTTOM,
                level_root.id,
                ConstraintSet.BOTTOM,
                20.dpToPx()
            )
            constraintSet.connect(
                level_miniView.id,
                ConstraintSet.RIGHT,
                level_root.id,
                ConstraintSet.RIGHT,
                DisplayInfo.statusBarHeight + 10
            )
            constraintSet.constrainWidth(level_miniView.id, 300.dpToPx())
            constraintSet.constrainHeight(level_miniView.id,  ConstraintSet.WRAP_CONTENT)
            constraintSet.applyTo(level_root)
            level_miniView.radius = 20f
            //设置顶部分隔线
            val topLine = findViewById<View>(R.id.miniView_topLine)
            topLine.visibility = View.GONE

            //取消底部边距(设置高度为0)
            val miniView_bottomSafeArea = findViewById<View>(R.id.miniView_bottomSafeArea)
            val params = miniView_bottomSafeArea.layoutParams
            params.height = 0
            miniView_bottomSafeArea.layoutParams = params
            miniView_bottomSafeArea.requestLayout()

            //设置顶部栏
            constraintSet.clone(level_root)
            constraintSet.clear(level_topBar.id)
            constraintSet.connect(
                level_topBar.id,
                ConstraintSet.TOP,
                level_root.id,
                ConstraintSet.TOP,
                DisplayInfo.statusBarHeight + 10
            )
            constraintSet.connect(
                level_topBar.id,
                ConstraintSet.LEFT,
                level_root.id,
                ConstraintSet.LEFT,
                DisplayInfo.statusBarHeight + 10
            )
            constraintSet.constrainWidth(level_topBar.id, 300.dpToPx())
            constraintSet.constrainHeight(level_topBar.id,  ConstraintSet.WRAP_CONTENT)
            constraintSet.applyTo(level_root)
            level_topBar.radius = 20f

            //取消顶部边距
            val topBar_topSafeArea = findViewById<View>(R.id.topBar_topSafeArea)
            val params_topBar = topBar_topSafeArea.layoutParams
            params_topBar.height = 0
            topBar_topSafeArea.layoutParams = params_topBar
            topBar_topSafeArea.requestLayout()

             */

            val constraintSet = ConstraintSet()
            constraintSet.clone(level_root)
            constraintSet.clear(level_controllers.id)
            constraintSet.connect(
                level_controllers.id,
                ConstraintSet.TOP,
                level_root.id,
                ConstraintSet.TOP,
                0
            )
            constraintSet.connect(
                level_controllers.id,
                ConstraintSet.LEFT,
                level_root.id,
                ConstraintSet.LEFT,
                0
            )
            constraintSet.connect(
                level_controllers.id,
                ConstraintSet.BOTTOM,
                level_root.id,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.constrainWidth(level_controllers.id, (DeviceInfo.screenWidth * 0.3f).toInt())
            constraintSet.applyTo(level_root)

            val constraintSetList = ConstraintSet()
            constraintSetList.clone(level_root)
            constraintSetList.clear(level_list.id)
            constraintSetList.connect(
                level_list.id,
                ConstraintSet.LEFT,
                level_controllers.id,
                ConstraintSet.RIGHT,
                0
            )

            constraintSetList.constrainWidth(level_list.id, (DeviceInfo.screenWidth * 0.7f).toInt())
            constraintSetList.applyTo(level_root)

            val level_controller_rightLine = findViewById<View>(R.id.level_controller_rightLine)
            level_controller_rightLine.visibility = View.VISIBLE




            //设置列表内边距
            ListRecyclerView_Video.setPadding(DeviceInfo.statusBarHeight, 300, DeviceInfo.statusBarHeight, 300)
            ListRecyclerView_Music.setPadding(DeviceInfo.statusBarHeight, 300, DeviceInfo.statusBarHeight, 300)
            ListRecyclerView_Video.requestLayout()
            ListRecyclerView_Music.requestLayout()




        }else{
            //竖屏模式



            //设置顶部边距
            val topBar_topSafeArea = findViewById<View>(R.id.topBar_topSafeArea)
            val params_topSafeArea = topBar_topSafeArea.layoutParams
            params_topSafeArea.height = DeviceInfo.statusBarHeight + 10
            topBar_topSafeArea.layoutParams = params_topSafeArea
            topBar_topSafeArea.requestLayout()

            //获取顶部卡片总高度
            var topBar_totalHeight = level_topBar.height
            level_topBar.post {
                topBar_totalHeight = level_topBar.height

                val targetTopPadding = topBar_totalHeight + 10

                //consoleLog(" 界面重组 compose: targetTopPadding:$targetTopPadding,  statusBarHeight:${DeviceInfo.statusBarHeight}  ")

                //设置列表内边距
                ListRecyclerView_Video.setPadding(0, targetTopPadding, 0, 300)
                ListRecyclerView_Music.setPadding(0, targetTopPadding, 0, 300)
                ListRecyclerView_Video.requestLayout()
                ListRecyclerView_Music.requestLayout()

            }


        }

    }
    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    //显示加载提示
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
    //显示短胶囊通知
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
    //格式化时间
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
    @SuppressLint( "NewApi")
    private fun checkPermissionAndVersion(): Boolean {
        val PrivacyPermissionHelper = PrivacyPermissionHelper()
        val isStoragePermissionValid = PrivacyPermissionHelper.checkPermissionValidity(this)

        return isStoragePermissionValid
    }
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MainActivity: $msg")
        }
    }

}