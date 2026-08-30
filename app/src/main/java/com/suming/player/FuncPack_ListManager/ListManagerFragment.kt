package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.DataLoader.AudioSysApiQuerier
import com.suming.player.DataPack.DataLoader.VideoSysApiQuerier
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.SettingsRequestCenter
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@SuppressLint("ComposableNaming","NewApi")
@Suppress("/unused")
class ListManagerFragment: DialogFragment(){
    companion object {
        fun newInstance(): ListManagerFragment =
            ListManagerFragment().apply {
                arguments = bundleOf(

                )
            }
    }

    //空字段
    private val Undefined = ""
    //ctx
    private lateinit var context: Context
    //MediaInfoRetriever
    val MediaInfoRetriever = MediaInfoRetriever()


    override fun onStart() {
        super.onStart()
        //初始化显示
        initDisplay()
    }

    @Suppress("DEPRECATION")
    private fun initDisplay(){
        //获取window
        val window = dialog?.window ?: return
        //检查横竖屏状态
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //检查深色模式
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        //执行通用设置
        //设置状态栏背景为透明(否则有色块跟随动画飞出)
        window.statusBarColor = Color.TRANSPARENT
        //设置背景压暗幅度
        window.setDimAmount(0f)

        //执行绑定屏幕方向的设置
        if (isLandscape){
            //横屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOutHorizontal)


            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, _ -> WindowInsetsCompat.CONSUMED }

                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }else{
            //竖屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOut)



            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                //ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //获得上下文
        context = requireContext()
        //设置样式
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):View{
        //获得view
        val view = inflater.inflate(R.layout.fragment_play_list, container, false)

        //初始化界面
        init(view)

        return view
    }

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //注册界面控件
        register(view)

        //注册当前播放项观察者
        startMediaItemObserver()

        //启动ViewPager
        registerViewPager()

        //更新当前播放列表指示图标
        updateIcon_currentPlayingList()


        //注册来自 子Fragment 的消息监听
        register_C_Fragment_Listener()
        //注册来自 Activity 的消息监听
        register_Activity_Listener()



    }

    override fun onResume() {
        super.onResume()
        //发布开启事件
        return_FragmentResult_toActivity(FragmentConnector.fragment_event_open)
    }
    override fun onPause() {
        super.onPause()
        //发布关闭事件
        return_FragmentResult_toActivity(FragmentConnector.fragment_event_close)
    }

    override fun onDestroy() {
        super.onDestroy()
        //移除ViewPager监听
        stopViewPagerListener()

    }

    private fun init(view: View){
        //初始化全局元素
        ButtonCard_Area = view.findViewById(R.id.TabScrollView)
        ButtonCard_customList = view.findViewById(R.id.ButtonCard_CustomList)
        ButtonCard_historyList = view.findViewById(R.id.ButtonCard_HistoryList)
        ButtonCard_videoList = view.findViewById(R.id.ButtonCardVideo)
        ButtonCard_musicList = view.findViewById(R.id.ButtonCardMusic)

        ViewPager = view.findViewById(R.id.ViewPager)

        ButtonIcon_currentPlayList = view.findViewById(R.id.ButtonCurrentListIcon)

        //执行显示重建
        display(view)

    }






    //Main Thread Functions
    private fun register(view: View){
        lifecycleScope.launch(Dispatchers.Main) {
            //按钮：退出
            val buttonExit = view.findViewById<CircleButton>(R.id.buttonExit)
            buttonExit.setOnClickListener {
                dismiss()
            }
            //按钮：点击空白区域退出
            val topArea = view.findViewById<View>(R.id.out_area)
            topArea.setOnClickListener {
                dismiss()
            }
            //更多按钮
            val ButtonMoreOpt = view.findViewById<CircleButton>(R.id.ButtonMore)
            ButtonMoreOpt.setOnClickListener {
                showMoreOptMenu(ButtonMoreOpt)
            }

            //按钮：锁定页面
            val ButtonLock = view.findViewById<CircleButton>(R.id.ButtonLock)
            ButtonLock.setOnClickListener {
                lockPage = !lockPage
                if (lockPage){
                    ButtonLock.setIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_button_lock_on))
                }
                else{
                    ButtonLock.setIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_button_lock_off))
                }
            }


            //选单-循环模式
            updateLoopModeText()
            val ButtonCardLoopMode = view.findViewById<CardView>(R.id.ButtonCardLoopMode)
            ButtonCardLoopMode.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                startLoopModeMenu(ButtonCardLoopMode)
            }

            //选单-当前播放列表
            val ButtonCurrentList = view.findViewById<CardView>(R.id.ButtonCurrentList)
            ButtonCurrentList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                startCurrentPlayingListMenu(ButtonCurrentList)
            }


            //横滑页签按钮
            ButtonCard_customList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                switchToCustomPageByButton()
            }
            ButtonCard_historyList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                switchToHistoryPageByButton()
            }
            ButtonCard_videoList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                switchToVideoPageByButton()
            }
            ButtonCard_musicList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                switchToAudioPageByButton()
            }

        }
    }

    //启动ViewPager
    private fun registerViewPager(){
        lifecycleScope.launch(Dispatchers.Main) {

            //delay(500)

            //启动viewPager
            viewPagerAdapter = ViewPagerAdapter(this@ListManagerFragment)
            ViewPager.adapter = viewPagerAdapter
            //开启页面监听器
            startViewPagerListener()
            //设置ViewPager缓存页面数量
            ViewPager.offscreenPageLimit = 3
            //默认显示列表
            val smoothScroll = false
            ViewPager.post {
                if (viewPagerAdapter.itemCount > 0){
                    val acquiesceShowPage = ListManagerHelper.GET_PRFR_AcquiesceShowingPage()
                    //使用上一次停留的页签
                    if (acquiesceShowPage == ListManagerHelper.ListMark_UseLast){
                        val LastShowingListMark = ListManagerHelper.GET_STE_LastShowingListMark()
                        when(LastShowingListMark){
                            ListManagerHelper.ListMark_Custom -> {
                                ViewPager.setCurrentItem(0, smoothScroll)
                            }
                            ListManagerHelper.ListMark_History -> {
                                ViewPager.setCurrentItem(1, smoothScroll)
                            }
                            ListManagerHelper.ListMark_Video -> {
                                ViewPager.setCurrentItem(2, smoothScroll)
                            }
                            ListManagerHelper.ListMark_Audio -> {
                                ViewPager.setCurrentItem(3, smoothScroll)
                            }
                            else -> {
                                ViewPager.setCurrentItem(0, smoothScroll)
                            }
                        }
                    }else{
                        //使用固定默认页签
                        when(acquiesceShowPage){
                            ListManagerHelper.ListMark_Custom -> {
                                ViewPager.setCurrentItem(0, smoothScroll)
                            }
                            ListManagerHelper.ListMark_History -> {
                                ViewPager.setCurrentItem(1, smoothScroll)
                            }
                            ListManagerHelper.ListMark_Video -> {
                                ViewPager.setCurrentItem(2, smoothScroll)
                            }
                            ListManagerHelper.ListMark_Audio -> {
                                ViewPager.setCurrentItem(3, smoothScroll)
                            }
                            else -> {
                                ViewPager.setCurrentItem(0, smoothScroll)
                            }
                        }
                    }
                }
            }
        }

    }



    //viewPager
    private lateinit var ViewPager: ViewPager2
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private class ViewPagerAdapter(innerFragment: Fragment):FragmentStateAdapter(innerFragment){
        //getItemCount
        override fun getItemCount(): Int = 4
        //createFragment
        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> InnerFragment_CustomList()

                1 -> InnerFragment_HistoryList()

                2 -> InnerFragment_Video()

                3 -> InnerFragment_Audio()

                else -> ListFragment()
            }

    }
    //viewPager页面切换监听器
    private var ViewPagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            //记录当前显示页签
            saveListMark(position)
            //滚动到当前显示页签
            scrolledToPage(position)
        }
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

        }
        override fun onPageScrollStateChanged(state: Int) {

        }
    }
    private var state_viewPagerListener_started = false
    private fun startViewPagerListener(){
        if (state_viewPagerListener_started){ return }
        ViewPager.registerOnPageChangeCallback(ViewPagerListener)
        state_viewPagerListener_started = true
    }
    private fun stopViewPagerListener(){
        if (!state_viewPagerListener_started){ return }
        ViewPager.unregisterOnPageChangeCallback(ViewPagerListener)
        state_viewPagerListener_started = false
    }
    //viewPager页签对应
    private val viewPagerPageMarkMapNum = mapOf(
        0 to ListManagerHelper.ListMark_Custom,
        1 to ListManagerHelper.ListMark_History,
        2 to ListManagerHelper.ListMark_Video,
        3 to ListManagerHelper.ListMark_Audio,
    )
    private val viewPagerPageMarkMapString = mapOf(
        ListManagerHelper.ListMark_Custom to 0,
        ListManagerHelper.ListMark_History to 1,
        ListManagerHelper.ListMark_Video to 2,
        ListManagerHelper.ListMark_Audio to 3,
    )

    //观察当前播放状态
    private var MediaItemObserverRunning = false
    private fun startMediaItemObserver() {
        if (MediaItemObserverRunning) return
        MediaItemObserverRunning = true

        //观察正在播放的媒体项变更
        lifecycleScope.launch {
            //观察正在播放的媒体项变更
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableMediaItem.collect { _ ->
                    //发送到 子Fragment (两个页面一起更新)
                    send_C_Fragment_Event(2, ListManagerHelper.event_detail_general_media_item_update)
                    send_C_Fragment_Event(3, ListManagerHelper.event_detail_general_media_item_update)

                }
            }
        }
        //观察播放状态变更
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableIsPlaying.collect { _ ->
                    //发送到 子Fragment (两个页面一起更新)
                    send_C_Fragment_Event(2, ListManagerHelper.event_detail_general_media_state_update)
                    send_C_Fragment_Event(3, ListManagerHelper.event_detail_general_media_state_update)

                }
            }
        }
    }

    //播放请求
    private fun onPlayNewItemRequest(URI_S_FP: String){
        //consoleLog("收到请求播放新的媒体项: $URI_S_FP")
        //检查文件是否存在可读
        val success = MediaInfoRetriever.isUriReadable(context,URI_S_FP)
        //consoleLog("isUriReadable: $success")
        if (success){
            if (URI_S_FP == PlayerSingleton.GET_STE_currentMediaItem_Uri().second.toString()){
                if (PlayerInfoCenter.observableIsPlaying.value){
                    PlayerSingleton.pausePlay()
                }else{
                    PlayerSingleton.continuePlay(true)
                }
            }else{
                //确保播放器已经启动
                PlayerSingleton.getInitPlayer()
                //设置播放项
                val result = PlayerSingleton.setMediaItem(URI_UP = URI_S_FP.toUri(),playWhenReady = true)
                when(result){
                    ActivityResultConnector.OBRTV_Engine_RetrieveFailed -> {
                        context.showCustomToast("文件似乎已经不存在",3)
                    }
                    ActivityResultConnector.OBRTV_Engine_SoFrequent -> {
                        context.showCustomToast("设置过于频繁",3)
                    }
                    ActivityResultConnector.OBRTV_Engine_TypeNotSupport -> {
                        context.showCustomToast("不支持的格式",3)
                    }
                }
            }
        }else{
            context.showCustomToast("文件似乎已经不存在",3)
        }
    }

    //删除点击事件
    private fun onDeleteClick(uriNumOnly: Long)  {

    }
    //添加到自定义列表点击事件
    private fun onAddToListClick() {

    }



    //注册 子Fragment 返回消息监听  子Fragment -> 父Fragment  reverse
    private fun register_C_Fragment_Listener(){
        //自定义列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_custom_reverse, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }

            }
        }
        //历史列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_history_reverse, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }


            }
        }
        //视频列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_video_reverse, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            val extra = bundle.getString(ListManagerHelper.event_key_extra) ?: Undefined
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }
                //播放新的媒体项
                ListManagerHelper.event_detail_general_play_new_item -> {
                    onPlayNewItemRequest(extra)
                }


            }
        }
        //音乐列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_audio_reverse, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            val extra = bundle.getString(ListManagerHelper.event_key_extra) ?: Undefined
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }
                //播放新的媒体项
                ListManagerHelper.event_detail_general_play_new_item -> {
                    onPlayNewItemRequest(extra)
                }

            }
        }

    }
    //向 子Fragment 传递事件  父Fragment -> 子Fragment  no_reverse
    private fun send_C_Fragment_Event(targetList: Any, data: String, extra: String = Undefined) {
        //合成position
        val position = if (targetList is String){
            viewPagerPageMarkMapString[targetList] ?: return
        }else targetList as? Int ?: return

        //发布消息
        when(position){
            0 -> {
                //自定义列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_custom, Bundle().apply {
                    putString(ListManagerHelper.event_key_general, data)
                    putString(ListManagerHelper.event_key_extra, extra)
                })
            }
            1 -> {
                //历史列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_history, Bundle().apply {
                    putString(ListManagerHelper.event_key_general, data)
                    putString(ListManagerHelper.event_key_extra, extra)
                })
            }
            2 -> {
                //视频列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_video, Bundle().apply {
                    putString(ListManagerHelper.event_key_general, data)
                    putString(ListManagerHelper.event_key_extra, extra)
                })
            }
            3 -> {
                //音乐列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_audio, Bundle().apply {
                    putString(ListManagerHelper.event_key_general, data)
                    putString(ListManagerHelper.event_key_extra, extra)
                })
            }
            else -> {
                return
            }
        }
    }

    //注册来自 Activity 的消息监听
    private fun register_Activity_Listener(){
        parentFragmentManager.setFragmentResultListener(FragmentConnector.fragment_request_key_play_list, this){ _, bundle ->
            val key = bundle.getString(FragmentConnector.receive_key) ?: return@setFragmentResultListener
            when(key){
                //刷新RecyclerView Adapter (已使用场景:1.外部刷新完成,通知内部刷新)
                ListManagerHelper.event_video_list_refresh -> {
                    send_C_Fragment_Event(2, ListManagerHelper.event_video_list_refresh)
                    send_C_Fragment_Event(3, ListManagerHelper.event_audio_list_refresh)
                }

            }
        }
    }
    //发布事件回 Activity   fragment_request_key_play_list_reverse   Fragment -> Activity
    private fun return_FragmentResult_toActivity(event: String){
        val result = Bundle().apply { putString(FragmentConnector.receive_key, event) }
        setFragmentResult(FragmentConnector.fragment_request_key_play_list_reverse, result)
    }
    private fun return_FragmentResult_toActivity(event: String,extra: String){
        val result = Bundle().apply {
            putString(FragmentConnector.receive_key, event)
            putString(FragmentConnector.extra_key, extra)
        }
        setFragmentResult(FragmentConnector.fragment_request_key_play_list_reverse, result)
    }


    //显示更多操作菜单
    private fun showMoreOptMenu(anchor: CircleButton){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(
            R.menu.popup_menu_list_more_opt,
            popup.menu
        )
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.opt_next_media -> {
                    ToolVibrate().vibrate(requireContext())

                    true
                }
                R.id.opt_previous_media -> {
                    ToolVibrate().vibrate(requireContext())

                    true
                }
                R.id.opt_clear -> {
                    ToolVibrate().vibrate(requireContext())

                    //清除播放项
                    stopPlaying()

                    customDismiss()

                    true
                }
                R.id.opt_clear_record -> {
                    ToolVibrate().vibrate(requireContext())

                    //清除播放记录
                    stopPlaying(true)

                    //关闭
                    customDismiss()

                    true
                }
                R.id.opt_player_escape -> {
                    ToolVibrate().vibrate(requireContext())
                    escapePlayerError()
                    true
                }
                else -> true
            }
        }
        popup.show()
    }

    //停止播放(可选清除记录)
    private fun stopPlaying(clearRecord: Boolean = false){
        //停止播放(仅清除项但播放器在线)
        PlayerSingleton.clearMediaItem()
        //清除播放记录
        if (clearRecord) {
            //清除播放记录
            val MediaRecordManager = MediaRecordManager()
            MediaRecordManager.clear_MediaInfo(requireContext())
        }
    }

    //页签焦点
    private fun scrolledToPage(position: Int){
        when(position){
            0 -> onFocusPage_Custom()
            1 -> onFocusPage_History()
            2 -> onFocusPage_Video()
            3 -> onFocusPage_Audio()
        }
    }
    //页签聚焦操作
    private fun onFocusPage_Custom(){
        updateCardPosition(0)
        updateCardColor(0)
    }
    private fun onFocusPage_History(){
        updateCardPosition(1)
        updateCardColor(1)
    }
    private fun onFocusPage_Video(){
        updateCardPosition(2)
        updateCardColor(2)
        //触发读取(//TODO 放在这里可能导致IO频次太多)
        //检查是否需要读取系统视频
        lifecycleScope.launch(Dispatchers.IO) {
            //检查本地数据库是否已有视频数据
            if (VideoRepo(context).isEmpty()){
                //通知状态变更
                DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_start)

                val mediaReader = VideoSysApiQuerier(context, context.contentResolver)
                mediaReader.readAndSaveAllVideos()

            }
        }
    }
    private fun onFocusPage_Audio(){
        updateCardPosition(3)
        updateCardColor(3)
        //触发读取(//TODO 放在这里可能导致IO频次太多)
        //检查是否需要读取系统音乐
        lifecycleScope.launch(Dispatchers.IO) {
            //检查本地数据库是否已有音乐数据
            if (AudioRepo(context).isEmpty()){
                //通知状态变更
                DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_start)

                val mediaReader = AudioSysApiQuerier(context, context.contentResolver)
                mediaReader.readAndSaveAllMusics()

            }
        }
    }
    //页签点击切换
    private lateinit var ButtonCard_Area: HorizontalScrollView
    private lateinit var ButtonCard_customList: CardView
    private lateinit var ButtonCard_historyList: CardView
    private lateinit var ButtonCard_videoList: CardView
    private lateinit var ButtonCard_musicList: CardView
    private fun switchToCustomPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 0){
            //发送到 子Fragment
            send_C_Fragment_Event(0, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到自定义页签
        ViewPager.currentItem = 0
    }
    private fun switchToHistoryPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 1){
            //发送到 子Fragment
            send_C_Fragment_Event(1, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到视频页签
        ViewPager.currentItem = 1
    }
    private fun switchToVideoPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 2){
            //发送到 子Fragment
            send_C_Fragment_Event(2, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到视频页签
        ViewPager.currentItem = 2
    }
    private fun switchToAudioPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 3){
            //发送到 子Fragment
            send_C_Fragment_Event(3, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到音乐页签
        ViewPager.currentItem = 3
    }
    //页签样式/位置更新
    private fun updateCardColor(position: Int){
        when(position){
            0 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_ON))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
            }
            1 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_ON))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
            }
            2 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_ON))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
            }
            3 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.SecondaryColorPack_CardButtonBackground_state_ON))
            }
        }
    }
    private fun updateCardPosition(position: Int){
        when (position) {
            0 -> {
                ButtonCard_Area.smoothScrollTo(0, 0)
            }
            1 -> {
                val left = ButtonCard_historyList.left
                ButtonCard_Area.smoothScrollTo(left, 0)
            }
            2 -> {
                val left = ButtonCard_videoList.left
                ButtonCard_Area.smoothScrollTo(left, 0)
            }
            3 -> {
                val left = ButtonCard_musicList.left
                ButtonCard_Area.smoothScrollTo(left, 0)
            }
        }
    }


    //更新当前播放列表卡片指示图标
    private lateinit var ButtonIcon_currentPlayList: ImageView
    private fun updateIcon_currentPlayingList(){
        val currentPlayList = ListManagerHelper.GET_STE_CurrentPlayingListMark()
        when (currentPlayList) {
            ListManagerHelper.ListMark_Custom -> {
                ButtonIcon_currentPlayList.setImageResource(R.drawable.ic_play_list_custom_list)
            }
            ListManagerHelper.ListMark_History -> {
                //ButtonIcon_currentPlayList.setImageResource(R.drawable.ic_main_fragment_history_icon)
            }
            ListManagerHelper.ListMark_Video -> {
                ButtonIcon_currentPlayList.setImageResource(R.drawable.ic_main_fragment_video_icon)
            }
            ListManagerHelper.ListMark_Audio -> {
                ButtonIcon_currentPlayList.setImageResource(R.drawable.ic_main_fragment_music_icon)
            }
        }
    }
    //设置当前播放列表
    private fun setCurrentPlayingList(targetList: String){
        //设置当前播放列表
        val success = ListManagerHelper.SET_STE_CurrentPlayingListMark(targetList)

        //通知子Fragment更新
        val currentPage = ViewPager.currentItem
        send_C_Fragment_Event(currentPage, ListManagerHelper.event_detail_general_update_list_state)

        //更新当前播放列表指示图标
        updateIcon_currentPlayingList()
        //更新当前播放列表图标
        if (success){
            requireContext().showCustomToast("设置成功",2)
        }else{
            requireContext().showCustomToast("设置失败",2)
        }
    }
    //选择当前播放列表
    private fun startCurrentPlayingListMenu(anchor: View){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_play_list_popup_current_play_list, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                //选择自定义列表
                R.id.list_custom -> {
                    ToolVibrate().vibrate(requireContext())

                    setCurrentPlayingList(ListManagerHelper.ListMark_Custom)

                    true
                }
                //选择历史播放列表
                R.id.list_history -> {
                    ToolVibrate().vibrate(requireContext())

                    setCurrentPlayingList(ListManagerHelper.ListMark_History)

                    true
                }
                //选择视频列表
                R.id.list_video_live -> {
                    ToolVibrate().vibrate(requireContext())

                    setCurrentPlayingList(ListManagerHelper.ListMark_Video)

                    true
                }
                //选择音乐列表
                R.id.list_music_live -> {
                    ToolVibrate().vibrate(requireContext())

                    setCurrentPlayingList(ListManagerHelper.ListMark_Audio)

                    true
                }
                //其他
                else -> true
            }
        }
        popup.show()
    }


    //保存本次切换至的页签
    private var saveListMark_avoidFirst = false
    private fun saveListMark(targetListNum: Int){
        if (!saveListMark_avoidFirst) {
            saveListMark_avoidFirst = true
            return
        }

        //consoleLog("targetListNum: $targetListNum")
        val targetListString = viewPagerPageMarkMapNum[targetListNum] ?: return
        //consoleLog("targetListString: $targetListString")

        ListManagerHelper.TURNTO_List(targetListString)
    }


    //播放器脱离卡死
    private fun escapePlayerError(){
        AlertDialog.Builder(context)
            .setTitle("确定脱离播放器卡死吗?")
            .setMessage("当无法正常播放时使用。会使正在播放的媒体立即停止")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(context)
                PlayerSingleton.stopPlayEngine()
                dialog.dismiss()

                customDismiss()

            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(context)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }



    //循环模式
    private fun startLoopModeMenu(anchor: View){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_player_popup_loop_mode, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.LoopMode_ONE -> {
                    ToolVibrate().vibrate(requireContext())

                    chooseLoopMode(ListManagerHelper.LOOP_MODE_ONE)

                    true
                }

                R.id.LoopMode_ALL -> {
                    ToolVibrate().vibrate(requireContext())

                    chooseLoopMode(ListManagerHelper.LOOP_MODE_ALL)

                    true
                }

                R.id.LoopMode_OFF -> {
                    ToolVibrate().vibrate(requireContext())

                    chooseLoopMode(ListManagerHelper.LOOP_MODE_OFF)

                    true
                }

                else -> true
            }
        }
        popup.show()
    }
    private fun chooseLoopMode(loopMode: String){
        //设置循环模式
        when (loopMode) {
            ListManagerHelper.LOOP_MODE_ONE -> {
                ListManagerHelper.setLoopMode(loopMode)
                //设为单集循环时,必要时可自动开始
                PlayerSingleton.checkPlayEndAndRePlay()
            }
            ListManagerHelper.LOOP_MODE_OFF -> {
                ListManagerHelper.setLoopMode(loopMode)
            }
            ListManagerHelper.LOOP_MODE_ALL -> {
                ListManagerHelper.setLoopMode(loopMode)
            }
        }
        //刷新显示文本
        updateLoopModeText()

    }
    private fun updateLoopModeText(){
        val currentLoopMode = ListManagerHelper.getLoopMode()
        val ButtonTextLoopMode = view?.findViewById<TextView>(R.id.ButtonTextLoopMode)
        ButtonTextLoopMode?.text = when (currentLoopMode) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知模式"
        }
    }

    //设置面板几何参数
    private fun display(view: View){
        //获取状态栏高度(由于函数调用处提前,已无法获取高度)(这个东西有点意思,留着不要删)
        @Suppress("unused")
        fun getStatusBarHeightFromView(view: View): Int {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            return rect.top
        }


        //获取当前屏幕方向
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //操作主卡片视图
        val mainCard = view.findViewById<CardView>(R.id.main_card)
        //读取屏幕信息
        val screenHeightPx = resources.displayMetrics.heightPixels
        val screenWidthPx = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density


        //执行设置
        if (isLandscape){
            //计算目标宽度
            val targetScreenWidthPx = (screenWidthPx * 0.4).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()
            //进行宽度保底
            if (targetScreenHeightDp < 50){
                mainCard.layoutParams.width = screenWidthPx
            }else{
                mainCard.layoutParams.width = targetScreenWidthPx
            }
            //设置卡片显示参数
            mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            mainCard.setContentPadding(0, DeviceInfo.statusBarHeight, 0, 0)
            //请求布局更新
            mainCard.requestLayout()

        }else{
            //计算目标高度
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()
            //进行高度保底
            if (targetScreenHeightDp < 450){
                mainCard.layoutParams.height = screenHeightPx
            }else{
                mainCard.layoutParams.height = targetHeightPx
            }
            //请求布局更新
            mainCard.requestLayout()

        }
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            dismiss()
        }
    }

    //日志
    @Suppress("unused")
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ListManagerFragment: $msg")
        }
    }

}

