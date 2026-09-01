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
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.DataLoader.AudioDataBaseLoader
import com.suming.player.DataPack.DataLoader.VideoDataBaseLoader
import com.suming.player.DataPack.DataLoader.AudioSysApiQuerier
import com.suming.player.DataPack.DataLoader.VideoSysApiQuerier
import com.suming.player.DataPack.MediaRecordPack
import com.suming.player.FuncPack_ListManager.ListManagerFragment
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.ConnectCenter
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.IntentRepo
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

@Suppress("NewApi","/unused")
@OptIn(UnstableApi::class)
class MainActivity: AppCompatActivity() {

    //连接ViewModel
    private val mainViewModel: MainViewModel by viewModels()
    //防止快速点击
    private var lock_clickMillisLock = 0L
    private var lock_clickMillisLock_second = 0L
    //字段
    private val Undefined = ""
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()
    //ctx
    private val context = this@MainActivity
    //
    private var onPaused = false



    @SuppressLint("ClickableViewAccessibility" )
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


        //启动事件观察者
        setupEventObserver()


        //发起列表读取(8秒后才开始)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(8000)
            ListManagerHelper.GET_AudioList_fromDataBase()
            ListManagerHelper.GET_VideoList_fromDataBase()
        }

    }
    //处理新intent(需要singleTask模式)
    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        consoleLog("onNewIntent")
        if (newIntent?.action != null){
            when (newIntent.action) {
                //由于已使用EntranceActivity作为统一入口,其余活动不会再直接收到ACTION_SEND和ACTION_VIEW
                /*
                //系统面板：分享
                Intent.ACTION_SEND -> {

                }
                //系统面板：选择其他应用打开
                Intent.ACTION_VIEW -> {

                }

                 */
                //常规重复调用(来自EntranceActivity)
                IntentRepo.ACTION_ENTRANCE_REQUEST -> {
                    //收到EntranceActivity委托的新 intent
                    val URI_S_FP = newIntent.getStringExtra(IntentRepo.URI) ?: Undefined
                    if (URI_S_FP != Undefined) {
                        //启动播放(检查是否已在播放此媒体项)
                        val ongoing_URI_S_FP = PlayerSingleton.GET_STE_currentMediaItem_Uri().second.toString()
                        if (ongoing_URI_S_FP != URI_S_FP) {
                            //设置媒体项
                            setMediaItem(URI_S_FP.toUri(), true)
                        } else {
                            //继续播放
                            PlayerSingleton.continuePlay()
                        }

                    }

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //解绑播放器视图
        PlayingCard_Artwork_Video?.player = null


    }

    override fun onResume() {
        super.onResume()

        onPaused = false

        /*
        if (PlayingCard_Artwork_Video != null){
            val ongoing = PlayerSingleton.GET_STE_currentMediaItem_Uri().first
            if (ongoing){
                PlayingCard_Artwork_Video?.player = PlayerSingleton.getPlayer()
            }
        }

         */

        showMiniViewLongProcess()


        //检查正在播放的媒体是否还存在
        isFileExist()

    }

    override fun onPause() {
        super.onPause()

        onPaused = true


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
                        //consoleLog("播放列表Fragment关闭")
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
            delay(300)

            //按钮：指南
            val ButtonGuidance = findViewById<Button>(R.id.buttonGuidance)
            ButtonGuidance.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)


                ListRecyclerView_Video.stopScroll()
                ListRecyclerView_Music.stopScroll()

                //启动指南页面
                val intent = Intent(this@MainActivity, GuidanceActivity::class.java)
                startActivity(intent)

            }
            /*
            ButtonGuidance.visibility = View.VISIBLE
            ButtonGuidance.alpha = 0f
            ButtonGuidance.animate().alpha(1f).setDuration(300).start()

             */
            //按钮：设置
            val ButtonSettings= findViewById<Button>(R.id.buttonSetting)
            ButtonSettings.setOnClickListener {
                ToolVibrate().vibrate(this@MainActivity)
                ListRecyclerView_Video.stopScroll()
                ListRecyclerView_Music.stopScroll()

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
            val (no_privacy_permit, storage_permitted) = checkNeedStartPrivacyPermissionActivity()
            //需要显示权限与隐私页面
            if (no_privacy_permit){
                //Handler(Looper.getMainLooper()).postDelayed({   }, 1000)
                //启动隐私权限面板
                startPrivacyPermissionActivity()
            }else{
                //已获得储存权限(核心分支)(显示主界面 + 延迟续播)
                if (storage_permitted){
                    //开启锁定
                    PlayerSingleton.isLocked = true

                    //显示媒体列表(必走流程)
                    withContext(Dispatchers.Main){
                        showMediaList(savedInstanceState)
                    }

                    //读取EntranceActivity委托的intent(根据savedInstanceState)
                    if (savedInstanceState == null) {
                        //首次启动

                        delay(200)

                        //检查启动来源是否EntranceActivity委托
                        val action = intent.action ?: Undefined
                        if (action == IntentRepo.ACTION_ENTRANCE_REQUEST) {
                            //收到EntranceActivity委托的 intent

                            val URI_S_FP = intent.getStringExtra(IntentRepo.URI) ?: Undefined
                            if (URI_S_FP != Undefined){
                                //启动播放(检查是否已在播放此媒体项)
                                withContext(Dispatchers.Main){
                                    val ongoing_URI_S_FP = PlayerSingleton.GET_STE_currentMediaItem_Uri().second.toString()
                                    if (ongoing_URI_S_FP != URI_S_FP){
                                        //设置媒体项
                                        setMediaItem(URI_S_FP.toUri(),true,true)
                                    }else{
                                        //继续播放
                                        PlayerSingleton.continuePlay()
                                    }
                                }
                            }
                        }else{
                            //无特殊委托,进入继续播放上次的媒体流程

                            //检查是否有媒体正在播放
                            val isAnyMediaOngoing = withContext(Dispatchers.Main) { isAnyMediaOngoing().first }
                            if (!isAnyMediaOngoing){
                                //无播放时,检查是否启用继续播放功能
                                if (SettingsRequestCenter.get_PREFS_EnableContinuePlay(context)) {
                                    //没有媒体正在播放,从记录中获取上次停留的媒体信息(已检查是否有效)
                                    val MediaRecordPack = getLastMediaRecord()
                                    if (MediaRecordPack != null) {
                                        withContext(Dispatchers.Main) {
                                            showMiniViewByRecord(MediaRecordPack)
                                        }
                                    }
                                }
                            }

                        }

                    }else{
                        //重建活动(一律绑定正在播放的项)

                        //由媒体项观察者完成

                    }

                    //关闭锁定
                    PlayerSingleton.isLocked = false

                }else{
                    //显示“选择文件以播放”界面
                    showOpenFileButton()
                }
            }



            //启动MiniView观察者
            startMiniViewObserver()

            //启动列表观察者
            startListUnderTopObserver()
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
                    if (ListRecyclerView_Video.canScrollVertically(-1)){
                        setListToTop()
                    }else{
                        main_video_list_adapter.refresh()
                    }
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
                    if (ListRecyclerView_Music.canScrollVertically(-1)){
                        setListToTop()
                    }else{
                        main_music_list_adapter.refresh()
                    }
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
            if (VideoRepo(this@MainActivity).isEmpty() || queryNew){
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
            onItemClick = { uri, file_path ->
                onVideoItemClick(uri, file_path)
            },
            onClick_Duration = { item ->
                notice("视频时长:${FormatTime_withChar(item.media_durationMs)}", 2000)
            },
            onClick_tvFormat = { item ->
                notice("视频格式:${item.media_format}  (${item.media_api_NUM_ID})", 3000)
            },
            onClick_Options = { mediaItem, holder ->
                val popup = PopupMenu(holder.itemView.context, holder.tvOption)
                popup.menuInflater.inflate(R.menu.popup_menu_main_item_v_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.MenuAction_use_mini_view_play -> {
                            ToolVibrate().vibrate(this@MainActivity)

                            startMiniViewPlay(mediaItem.URI_S_FP.toUri())

                            true
                        }
                        R.id.MenuAction_use_whole_play_page -> {
                            ToolVibrate().vibrate(this@MainActivity)

                            startVideoPlayer(mediaItem.URI_S_FP.toUri(), mediaItem.file_path)

                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            },
        )
        //设置adapter
        ListRecyclerView_Video.adapter = main_video_list_adapter
        //加载视频数据
        lifecycleScope.launch(Dispatchers.IO) {
            val pager = Pager(PagingConfig(pageSize = 20)) {
                VideoDataBaseLoader(context = this@MainActivity)
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
            if (AudioRepo(this@MainActivity).isEmpty() || queryNew){
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
                AudioDataBaseLoader(context = this@MainActivity)
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
        if (MediaInfo_MediaRecordPack.SPECIFIC_ID.isEmpty()) return null
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

        //媒体项观察
        lifecycleScope.launch(Dispatchers.Main) {
            //观察正在播放的媒体项变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableMediaItem.collect { _ ->
                    if (onPaused) return@collect
                    //consoleLog("观察到 正在播放的媒体项 发生变更")
                    //显示MiniView
                    showMiniViewLongProcess()
                }
            }
        }
        //播放状态观察
        lifecycleScope.launch(Dispatchers.Main) {
            //观察播放状态变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableIsPlaying.collect { newState ->
                    if (onPaused) return@collect
                    //consoleLog("观察到 播放/暂停 发生变更")
                    //刷新操作按钮
                    updateMiniViewPauseButton(newState)
                }
            }
        }
    }
    //显示MiniView LongProcess-把任务全部执行完,禁止扔到其他函数域
    private fun showMiniViewLongProcess(){
        //从PlayerStateMediaInfo获取所有信息
        val (_,FileName,MediaArtist) = PlayerInfoCenter.GET_Media_MiniView_Pack()
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
        //决定是否能显示MiniView(只要mediaType在就当作能显示,哪怕FileName获取不到)
        if (mediaType.isEmpty()){
            miniView_clear()
        }else{
            //文字上屏
            PlayingCard_TextMediaName.text = FileName
            PlayingCard_TextMediaArtist.text = MediaArtist
            PlayingCard_ButtonPlay.visibility = View.VISIBLE
            //更新艺术图或视频
            if (PlayerInfoCenter.GET_Media_isCache()){
                updateMiniViewArtwork_Image(NUM_ID, mediaType)

            }else{
                updateMiniViewArtwork(mediaType, NUM_ID)
            }

        }
    }
    private fun showMiniViewByRecord(MediaRecordPack: MediaRecordPack){
        //从MediaRecordPack获取信息
        val SPECIFIC_ID = MediaRecordPack.SPECIFIC_ID
        //consoleLog("showMiniViewByRecord: SPECIFIC_ID： $SPECIFIC_ID")
        if (SPECIFIC_ID.isEmpty()){
            miniView_clear()
            return
        }
        val FileName = MediaRecordPack.fileName
        val MediaArtist = MediaRecordPack.mediaArtist
        val URI_S_FP = MediaRecordPack.uriStandard
        //分离部分信息
        var mediaType = Undefined
        var NUM_ID = 0L
        try {
            mediaType = MediaInfoRetriever.split_SPECIFIC_ID(SPECIFIC_ID).first
            NUM_ID = MediaInfoRetriever.split_SPECIFIC_ID(SPECIFIC_ID).second

            //consoleLog("miniView_cache_MediaType: $mediaType")
        }catch (e: Exception){
            consoleLog("showMiniViewByRecord-字符串拆分出错: $e")
        }

        if (SettingsRequestCenter.GET_PRF_ContinuePlay_withEngin(this@MainActivity)){
            //直接启动播放器
            setMediaItem(URI_S_FP.toUri(),false,true)

        }else{
            //写入cache包
            PlayerInfoCenter.SET_Media_CachePack(
                SPECIFIC_ID = SPECIFIC_ID,
                mediaType = mediaType,
                NUM_ID = NUM_ID,
                URI_S_FP = URI_S_FP,
                FileName = FileName,
                MediaArtist = MediaArtist
            )

        }

    }
    private fun initMiniView(){

        //视图初始化
        PlayingCard = findViewById(R.id.level_miniView)
        PlayingCard_InfoContainer = findViewById(R.id.PlayingCard_InfoContainer)
        PlayingCard_Artwork = findViewById(R.id.PlayingCard_Artwork)
        PlayingCard_TextMediaName = findViewById(R.id.PlayingCard_MediaName)
        PlayingCard_TextMediaArtist = findViewById(R.id.PlayingCard_MediaArtist)
        PlayingCard_ButtonPlay = findViewById(R.id.PlayingCard_ButtonPlay)
        PlayingCard_ButtonList = findViewById(R.id.PlayingCard_ButtonList)
        //点击事件设定
        //播放/暂停按钮
        PlayingCard_ButtonPlay.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)

            PlayingCard_TextMediaName.isSelected = true
            PlayingCard_TextMediaArtist.isSelected = true

            //检查是否有媒体在线
            val (ongoing, _) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
            if (!ongoing){
                if (PlayerInfoCenter.GET_Media_isCache()){
                    val uri = PlayerInfoCenter.GET_Media_URI_S_FP().toUri()

                    //播放缓存链接
                    setMediaItem(uri, true)
                }else{
                    showCustomToast("请先选择一项媒体以开始播放")
                }
                return@setOnClickListener

            }

            //选择执行播放或暂停
            if (PlayerSingleton.GET_STE_isNowPlaying()){
                PlayerSingleton.pausePlay()
            }else{
                PlayerSingleton.continuePlay(true)
            }
        }
        //播放列表按钮
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
        //艺术图按钮+容器:均打开播放页
        PlayingCard_Artwork.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)

            onPlayingCard_EnterClick()
        }
        PlayingCard_InfoContainer.setOnClickListener {
            ToolVibrate().vibrate(this@MainActivity)

            onPlayingCard_EnterClick()

        }


    }
    private fun onPlayingCard_EnterClick(){
        //停止列表防卡顿
        ListRecyclerView_Video.stopScroll()
        ListRecyclerView_Music.stopScroll()
        //获取到链接时启动播放页
        val URI = PlayerSingleton.GET_STE_currentMediaItem_Uri().second
        val file_path = PlayerInfoCenter.GET_Media_FilePath()
        if (URI != Uri.EMPTY){
            //唤起播放页
            startPlayerFromMiniView(URI, file_path)

        }else{
            //检查缓存链接
            if (PlayerInfoCenter.GET_Media_isCache()){
                //拿到缓存链接
                val cacheUri = PlayerInfoCenter.GET_Media_URI_S_FP()

                //唤起播放页
                if (cacheUri.isNotEmpty()){
                    startPlayerFromMiniView(cacheUri.toUri(), file_path)
                }else{

                    consoleLog("进入非预期分支,需检查代码")
                }

            }else{
                showCustomToast("选择一项媒体以开始播放")
            }
        }
    }
    private fun updateMiniViewPauseButton(isPlaying: Boolean){
        //更新操作按钮图标
        PlayingCard_ButtonPlay.setImageResource(if (isPlaying) R.drawable.ic_main_controller_pause else R.drawable.ic_main_controller_play)
    }
    private fun updateMiniViewArtwork(type: String,NUM_ID: Long){
        //consoleLog("updateMiniViewArtwork()")
        val useImage = SettingsRequestCenter.GET_PRF_AlwaysUseImageInMiniView(this@MainActivity)
        if (useImage){
            updateMiniViewArtwork_Image(NUM_ID, type)
        }else{
            when(type){
                MediaType.Audio -> updateMiniViewArtwork_Image(NUM_ID, type)
                MediaType.Video -> updateMiniViewArtwork_Video()
            }
        }
    }
    private fun updateMiniViewArtwork_Image(NUM_ID: Long, type: String){
        //NUM_ID需要有效
        if (NUM_ID <= 0L) return
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
        //推送图片到ImageView中
        fun pushImageToImageView(NUM_ID: Long, type: String) {
            val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(this, type, NUM_ID)
            if (Bitmap == null) {
                return
            }

            PlayingCard_Artwork_Image?.setImageBitmap(Bitmap)
            state_MiniViewArtwork_Image_NUM_ID = NUM_ID

            PlayingCard_Artwork.requestLayout()
        }

        //当前不是图片类型时,清除子视图并重建为图片视图
        if (state_MiniViewArtwork_type != mini_view_type_image){
            //解绑播放器视图
            PlayingCard_Artwork_Video?.player = null
            //清除所有子视图
            PlayingCard_Artwork.removeView(PlayingCard_Artwork_Video)
            PlayingCard_Artwork_Video = null
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
            //添加图片视图后,置入图片
            PlayingCard_Artwork_Image?.post{
                //置入图片
                pushImageToImageView(NUM_ID, type)
                //变换卡片宽高
                transformCardSize_toSquare()

            }

        }else{
            //不涉及视图新建时
            transformCardSize_toSquare()
            //判断是否为同一张图片
            if (state_MiniViewArtwork_Image_NUM_ID == NUM_ID) return
            //置入图片
            pushImageToImageView(NUM_ID, type)
        }
    }
    @SuppressLint("InflateParams")
    private fun updateMiniViewArtwork_Video(){
        //绑定到视频
        fun connectToPlayEngine(){
            PlayingCard_Artwork_Video?.player = null
            PlayingCard_Artwork_Video?.player = PlayerSingleton.get_player_ref()
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
                val aspectRatio = PlayerInfoCenter.GET_Media_AspectRatio()
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

            //清除当前的音乐视图
            PlayingCard_Artwork.removeView(PlayingCard_Artwork_Image)
            PlayingCard_Artwork_Image = null
            state_MiniViewArtwork_Image_NUM_ID = 0L
            //创建视频视图
            PlayingCard_Artwork_Video = LayoutInflater.from(this).inflate(R.layout.piece_player_view_texture_ver, null, false) as PlayerView
            //添加视频视图
            PlayingCard_Artwork.addView(PlayingCard_Artwork_Video)
            state_MiniViewArtwork_type = mini_view_type_video
            //添加视频视图后,绑定到视频
            PlayingCard_Artwork_Video?.post {

                //绑定到视频
                connectToPlayEngine()

                //变换卡片宽度
                transformCardSize_adaptVideo()

            }

        }else{
            //当前视图已经是视频视图,仅需要更新宽高比和重连播放器

            //变换卡片宽度
            transformCardSize_adaptVideo()
            //绑定到视频
            connectToPlayEngine()

        }
    }
    val mini_view_type_null = "mini_view_type_null"
    val mini_view_type_image = "mini_view_type_image"
    val mini_view_type_video = "mini_view_type_video"
    private var state_MiniViewArtwork_type = mini_view_type_null
    private var state_MiniViewArtwork_Image_NUM_ID = 0L
    private fun miniView_clear(){
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


        //设置显示文本
        PlayingCard_TextMediaName.text = "未在播放"
        PlayingCard_TextMediaArtist.text = "选择一项媒体以开始播放"

        //清除两个视图(不要清除所有)
        PlayingCard_Artwork.removeView(PlayingCard_Artwork_Video)
        PlayingCard_Artwork.removeView(PlayingCard_Artwork_Image)
        //变换卡片大小
        transformCardSize_toSquare()
        //写入当前类型缓存
        state_MiniViewArtwork_type = mini_view_type_null
        //关闭播放器绑定项
        PlayingCard_Artwork_Video?.player = null
        //清除两个视图引用
        PlayingCard_Artwork_Video = null
        PlayingCard_Artwork_Image = null

    }
    //MiniView视图合集
    private lateinit var PlayingCard: ConstraintLayout
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
        val (ongoing,currentMediaItem) = PlayerSingleton.GET_STE_currentMediaItem_Uri()

        return if (ongoing){
            val currentMediaUriString = currentMediaItem.toString()
            Pair(true,currentMediaUriString)
        }else{
            Pair(false,"")
        }
    }
    //设置新的媒体项
    private fun setMediaItem(MediaInfo_MediaUri: Uri, playWhenReady: Boolean, ignoreLock: Boolean = false){

        //确保播放器已经启动
        PlayerSingleton.init_player_get_ref()

        //确认设置新媒体项
        lifecycleScope.launch (Dispatchers.IO){
            val result = PlayerSingleton.setMediaItem(URI_UP  = MediaInfo_MediaUri,playWhenReady = playWhenReady,ignoreLock = ignoreLock)
            withContext(Dispatchers.Main){
                when(result){
                    ActivityResultConnector.OBRTV_Engine_RetrieveFailed -> {
                        showCustomToast("文件似乎已经不存在",3)
                    }
                    ActivityResultConnector.OBRTV_Engine_SoFrequent -> {
                        showCustomToast("设置过于频繁",3)
                    }
                    ActivityResultConnector.OBRTV_Engine_TypeNotSupport -> {
                        showCustomToast("不支持的格式",3)
                    }
                    ActivityResultConnector.OBRTV_Engine_Locked -> {
                        showCustomToast("播放器处于锁定窗口期", 3)
                    }
                }
            }
        }

    }
    //从选单发起后台播放
    private fun startMiniViewPlay(uri: Uri){
        //比对上次播放媒体信息与当前播放媒体信息
        val newUri = uri.toString()
        val currentUri = PlayerInfoCenter.GET_Media_URI_S_FP()
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
                val mediaReader = VideoSysApiQuerier(this@MainActivity, contentResolver)
                mediaReader.readAndSaveAllVideos()
            }
            MediaType.Audio -> {
                val musicReader = AudioSysApiQuerier(this@MainActivity, contentResolver)
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


    //检查文件是否还存在
    private fun isFileExist(){
        val file_path = PlayerInfoCenter.GET_Media_FilePath()

        if (file_path.isEmpty()) return

        val isAnyMediaOngoing = isAnyMediaOngoing().first
        if (isAnyMediaOngoing) {
            val exist = MediaInfoRetriever.isFileExist(file_path)
            if (exist){
                //文件存在

            }else{
                //文件不存在
                PlayerSingleton.clearMediaItem()

                showCustomToast("媒体已失效")
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
                    //读取完成
                    if (state.contains(DataBaseStateConnector.state_queryDisk_success)) {
                        //刷新列表
                        setLoadingText("读取完成",true,2000)
                        //刷新列表
                        refreshList()
                        //清空字段
                        DataBaseStateConnector.clearLoadState()
                        //读取完成的消息发到Fragment
                        supportFragmentManager.setFragmentResult(
                            FragmentConnector.fragment_request_key_play_list,
                            Bundle().apply {
                                putString(FragmentConnector.receive_key, FragmentConnector.fragment_play_list_require_refresh)
                            }
                        )

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

                        //consoleLog("更新封面帧: $targetFilePath, $targetMediaId")

                        main_video_list_adapter.updateCoverForItem(targetFilePath, targetMediaId)

                        ConnectCenter.clear_connector()
                    }
                }
            }
        }

    }

    //点击列表视频项
    private fun onVideoItemClick(uri: Uri, file_path: String){
        //consoleLog("onVideoItemClick: uri = $uri, file_path = $file_path")
        //防止快速发起
        if (System.currentTimeMillis() - lock_clickMillisLock_second < 800) return
        lock_clickMillisLock_second = System.currentTimeMillis()

        //检查启动方式
        val defaultPlayBehavior = SettingsRequestCenter.GET_PRF_DefaultPlayBehavior(this@MainActivity)
        //consoleLog("defaultPlayBehavior: $defaultPlayBehavior")
        when (defaultPlayBehavior) {
            //仅在MiniView中播放
            SettingsRequestCenter.action_just_in_mini_view -> {

                startMiniViewPlay(uri)
            }
            //弹出完整播放页面
            SettingsRequestCenter.action_use_whole_play_page -> {

                startVideoPlayer(uri, file_path)
            }
        }

    }



    //启动播放器
    private fun startVideoPlayer(uri: Uri, file_path: String){
        //防止快速发起
        if (System.currentTimeMillis() - lock_clickMillisLock < 800) return
        lock_clickMillisLock = System.currentTimeMillis()

        //检查使用的页面类型
        val playPageType = SettingsRequestCenter.GET_PRF_PlayPageType(this@MainActivity)
        when{
            (playPageType == SettingsRequestCenter.PlayPageType_Oro || playPageType == SettingsRequestCenter.PlayPageType_Neo) -> {
                //构建intent
                val intent = Intent(this, PlayerActivityNeo::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .putExtra(IntentRepo.URI, uri)
                    .putExtra(IntentRepo.FILE_PATH, file_path)
                    .putExtra(IntentRepo.SOURCE, 3)

                //是否使用进入动画
                val useSlideInAnim = SettingsRequestCenter.GET_PRF_EnableMiniView(this@MainActivity)
                if (useSlideInAnim){
                    //构建可选参数
                    val options = ActivityOptionsCompat.makeCustomAnimation(
                        this,
                        R.anim.slide_in_vertical,
                        R.anim.slide_dont_move
                    )

                    //启动活动
                    startActivity(intent, options.toBundle())
                }else{
                    //启动活动
                    startActivity(intent)

                }
            }
            playPageType == SettingsRequestCenter.PlayPageType_Test -> {

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
    private fun startPlayerFromMiniView(uri: Uri, file_path: String){
        val MediaInfo_MediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()

        when (MediaInfo_MediaType) {
            MediaType.Video -> {
                startVideoPlayer(uri, file_path)
            }
            MediaType.Audio -> {
                showCustomToast("暂不支持打开音乐播放页面",3)
                //startMusicPlayer(uri)
            }
            else -> {
                showCustomToast("失败",3)
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
    private lateinit var level_miniView : ConstraintLayout
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
        getDisplayInfo()

    }
    private var display_screen_height_pixels: Int = 0
    private var display_screen_width_pixels: Int = 0
    private var display_screen_density: Float = 0f
    private fun getDisplayInfo(){
        //获取状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_root_constraint)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            DeviceInfo.statusBarHeight = systemBars.top

            //重组主要视图
            compose()

            insets
        }
        //获取屏幕宽高和密度
        val DisplayMetrics = resources.displayMetrics
        display_screen_width_pixels = DisplayMetrics.widthPixels
        display_screen_height_pixels = DisplayMetrics.heightPixels
        display_screen_density = DisplayMetrics.density
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
            constraintSet.constrainWidth(level_controllers.id, (display_screen_width_pixels * 0.5f).toInt())
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

            constraintSetList.constrainWidth(level_list.id, (display_screen_width_pixels * 0.5f).toInt())
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
    @Suppress("unused")
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