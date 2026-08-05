package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
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
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
@SuppressLint("ComposableNaming","NewApi")
//@Suppress("unused")
class ListManagerFragment: DialogFragment() {
    companion object {
        fun newInstance(): ListManagerFragment =
            ListManagerFragment().apply {
                arguments = bundleOf(

                )
            }
    }



    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { _, _ -> WindowInsetsCompat.CONSUMED }
                //三星专用:显示到挖空区域
                dialog?.window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                dialog?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOutHorizontal)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_play_list, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化界面
        init(view)

        //注册界面控件
        register(view)
        //注册子Fragment通信
        registerChildFragment()

        //启动ViewPager
        registerViewPager(view)


    }

    override fun onResume() {
        super.onResume()
        //发布开启事件
        returnFragmentResult(FragmentConnector.fragment_event_open)
    }

    override fun onPause() {
        super.onPause()
        //发布关闭事件
        returnFragmentResult(FragmentConnector.fragment_event_close)
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


            //选单-循环模式
            updateLoopModeText()
            val ButtonCardLoopMode = view.findViewById<CardView>(R.id.ButtonCardLoopMode)
            ButtonCardLoopMode.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                startLoopModeMenu(ButtonCardLoopMode)
            }
            //选单-当前播放列表
            val ButtonCurrentList = view.findViewById<CardView>(R.id.ButtonCurrentList)
            ButtonIcon_currentPlayList = view.findViewById(R.id.ButtonCurrentListIcon)
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
    private fun registerViewPager(view: View){
        //启动viewPager
        ViewPager = view.findViewById(R.id.ViewPager)
        viewPagerAdapter = ViewPagerAdapter(this)
        ViewPager.adapter = viewPagerAdapter
        //开启页面监听器
        startViewPagerListener()
        //设置ViewPager缓存页面数量
        ViewPager.offscreenPageLimit = 3
        //默认显示列表
        ViewPager.post {
            if (viewPagerAdapter.itemCount > 0){
                val acquiesceShowPage = ListManagerHelper.GET_PRFR_AcquiesceShowingPage()
                //使用上一次停留的页签
                if (acquiesceShowPage == ListManagerHelper.ListMark_UseLast){
                    val LastShowingListMark = ListManagerHelper.GET_STE_LastShowingListMark()
                    when(LastShowingListMark){
                        ListManagerHelper.ListMark_Custom -> {
                            ViewPager.setCurrentItem(0, false)
                        }
                        ListManagerHelper.ListMark_History -> {
                            ViewPager.setCurrentItem(1, false)
                        }
                        ListManagerHelper.ListMark_Video -> {
                            ViewPager.setCurrentItem(2, false)
                        }
                        ListManagerHelper.ListMark_Audio -> {
                            ViewPager.setCurrentItem(3, false)
                        }
                        else -> {
                            ViewPager.setCurrentItem(0, false)
                        }
                    }
                }else{
                    //使用固定默认页签
                    when(acquiesceShowPage){
                        ListManagerHelper.ListMark_Custom -> {
                            ViewPager.setCurrentItem(0, false)
                        }
                        ListManagerHelper.ListMark_History -> {
                            ViewPager.setCurrentItem(1, false)
                        }
                        ListManagerHelper.ListMark_Video -> {
                            ViewPager.setCurrentItem(2, false)
                        }
                        ListManagerHelper.ListMark_Audio -> {
                            ViewPager.setCurrentItem(3, false)
                        }
                        else -> {
                            ViewPager.setCurrentItem(0, false)
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


    //注册子Fragment通信
    private fun registerChildFragment(){
        //自定义列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_custom, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }

            }
        }
        //历史列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_history, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }


            }
        }
        //视频列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_video, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }


            }
        }
        //音乐列表
        childFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_audio, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){
                //刷新当前播放列表指示图标
                ListManagerHelper.event_detail_general_update_currentPlayingList_icon -> {
                    updateIcon_currentPlayingList()
                }


            }
        }

    }
    //向子Fragment传递事件
    private fun sendChildFragmentEvent(targetList: Any, data: String) {
        //合成position
        val position = if (targetList is String){
            viewPagerPageMarkMapString[targetList] ?: return
        }else targetList as? Int ?: return

        //发布消息
        when(position){
            0 -> {
                //自定义列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_custom, bundleOf(
                    ListManagerHelper.event_key_general to data
                ))
            }
            1 -> {
                //历史列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_history, bundleOf(
                    ListManagerHelper.event_key_general to data
                ))
            }
            2 -> {
                //视频列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_video, bundleOf(
                    ListManagerHelper.event_key_general to data
                ))
            }
            3 -> {
                //音乐列表
                childFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_audio, bundleOf(
                    ListManagerHelper.event_key_general to data
                ))
            }
            else -> {
                requireContext().showCustomToast("标签未命中任何有效目标(String)")
                return
            }
        }
    }


    //显示更多操作菜单
    private fun showMoreOptMenu(anchor: CircleButton){
        //使用弹出菜单选择
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
                    //停止播放
                    PlayerSingleton.clearMediaItem()
                    dismiss()
                    true
                }
                R.id.opt_clear_record -> {
                    ToolVibrate().vibrate(requireContext())
                    //停止播放
                    PlayerSingleton.clearMediaItem()
                    //清除播放记录
                    val MediaRecordManager = MediaRecordManager()
                    MediaRecordManager.clear_MediaInfo(requireContext())
                    //关闭
                    dismiss()
                    true
                }
                else -> true
            }
        }
        popup.show()

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
    }
    private fun onFocusPage_Audio(){
        updateCardPosition(3)
        updateCardColor(3)
    }
    //页签点击切换
    private lateinit var ButtonCard_Area: HorizontalScrollView
    private lateinit var ButtonCard_customList: CardView
    private lateinit var ButtonCard_historyList: CardView
    private lateinit var ButtonCard_videoList: CardView
    private lateinit var ButtonCard_musicList: CardView
    private fun switchToCustomPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 0) {
            sendChildFragmentEvent(0, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到自定义页签
        ViewPager.currentItem = 0
    }
    private fun switchToHistoryPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 1) {
            sendChildFragmentEvent(1, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到视频页签
        ViewPager.currentItem = 1
    }
    private fun switchToVideoPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 2) {
            sendChildFragmentEvent(2, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到视频页签
        ViewPager.currentItem = 2
    }
    private fun switchToAudioPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 3) {
            sendChildFragmentEvent(3, ListManagerHelper.event_detail_general_goto_list_top)
            return
        }
        //切换到音乐页签
        ViewPager.currentItem = 3
    }
    //页签样式/位置更新
    private fun updateCardColor(position: Int){
        when(position){
            0 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            }
            1 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            }
            2 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            }
            3 -> {
                ButtonCard_customList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_historyList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_videoList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCard_musicList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
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
        sendChildFragmentEvent(currentPage, ListManagerHelper.event_detail_general_update_list_state)
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



    //播放点击事件
    private fun onPlayClick(uriString: String) {
        if (uriString == PlayerSingleton.getState_currentMediaItem_Uri().second.toString()){
            PlayerSingleton.continuePlay(true)
            requireContext().showCustomToast("已在播放该媒体",3)
        }else{
            PlayerSingleton.setMediaItem(uriString.toUri(), true,requireContext())
        }
    }
    //删除点击事件
    private fun onDeleteClick(uriNumOnly: Long)  {

    }
    //添加到自定义列表点击事件
    private fun onAddToListClick(uriString: String) {

    }

    //发布事件回Activity
    private fun returnFragmentResult(event: String){
        val result = bundleOf(FragmentConnector.receive_key to event)
        setFragmentResult(FragmentConnector.fragment_request_key_play_list, result)
    }
    private fun returnFragmentResult(event: String,extra: String){
        val result = bundleOf(FragmentConnector.receive_key to event,FragmentConnector.extra_key to extra)
        setFragmentResult(FragmentConnector.fragment_request_key_play_list, result)
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
                ListManagerHelper.setLoopMode(loopMode, requireContext())
                //设为单集循环时,有必要可自动开始
                PlayerSingleton.checkPlayEndAndRePlay()
            }
            ListManagerHelper.LOOP_MODE_OFF -> {
                ListManagerHelper.setLoopMode(loopMode, requireContext())
            }
            ListManagerHelper.LOOP_MODE_ALL -> {
                ListManagerHelper.setLoopMode(loopMode, requireContext())
            }
        }
        //刷新显示文本
        updateLoopModeText()

    }
    private fun updateLoopModeText(){
        val currentLoopMode = ListManagerHelper.getLoopMode(requireContext())
        val ButtonTextLoopMode = view?.findViewById<TextView>(R.id.ButtonTextLoopMode)
        ButtonTextLoopMode?.text = when (currentLoopMode) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知模式"
        }
    }

    //设置面板几何
    private fun display(view: View){
        //获取状态栏高度
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

            mainCard.post {
                if (targetScreenHeightDp < 50){
                    mainCard.layoutParams.width = screenWidthPx
                }else{
                    mainCard.layoutParams.width = targetScreenWidthPx
                }
                //把高度改为match parent
                mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                val statusBarHeight = getStatusBarHeightFromView(mainCard)
                mainCard.setContentPadding(0, statusBarHeight, 0, 0)

                mainCard.requestLayout()
            }

        }else{
            //计算目标高度
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()

            mainCard.post {
                if (targetScreenHeightDp < 450){
                    mainCard.layoutParams.height = screenHeightPx
                }else{
                    mainCard.layoutParams.height = targetHeightPx
                }
                mainCard.requestLayout()
            }
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
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ListManagerFragment: $msg")
        }
    }

}

