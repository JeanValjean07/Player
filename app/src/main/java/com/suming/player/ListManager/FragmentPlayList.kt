package com.suming.player.ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.ToolVibrate
import com.suming.player.showCustomToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.core.content.edit

@SuppressLint("ComposableNaming")
@RequiresApi(Build.VERSION_CODES.Q)
@UnstableApi
@Suppress("unused")
class FragmentPlayList: DialogFragment() {
    //静态方法
    companion object {
        fun newInstance(): FragmentPlayList =
            FragmentPlayList().apply {
                arguments = bundleOf(

                )
            }
    }
    //共享ViewModel
    private val vm: PlayerListViewModel by activityViewModels()
    //设置
    private lateinit var PREFS_List: SharedPreferences
    private var PREFS_AcquiescePage = 0
    private var state_LastPage = 0
    //协程作用域
    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    //横向按钮
    private lateinit var ButtonCardCustomList: CardView
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardMusic: CardView
    //横滑页签
    private lateinit var TabScrollView: HorizontalScrollView
    private lateinit var ButtonCurrentListIcon: ImageView



    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }

                /*
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }

                 */

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
        //读取设置
        PREFS_List = requireContext().getSharedPreferences("PREFS_List", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_play_list, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化控件：不包含viewPager
        fun initElement(){
            TabScrollView = view.findViewById(R.id.TabScrollView)
            ButtonCardCustomList = view.findViewById(R.id.ButtonCardCustomList)
            ButtonCardVideo = view.findViewById(R.id.ButtonCardVideo)
            ButtonCardMusic = view.findViewById(R.id.ButtonCardMusic)
        }
        initElement()
        //文本填写
        fun initText(){
            //循环模式
            setLoopModeText()
        }
        initText()
        //加载设置
        loadSettings()


        //按钮：退出
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            Dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            Dismiss()
        }
        //按钮：锁定页面
        val ButtonLock = view.findViewById<ImageButton>(R.id.buttonLock)
        ButtonLock.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            lockPage = !lockPage
            if (lockPage) {
                ButtonLock.setImageResource(R.drawable.ic_more_button_lock_on)
            } else {
                ButtonLock.setImageResource(R.drawable.ic_more_button_lock_off)
            }
        }
        //按钮：上一曲
        val ButtonPreviousMedia = view.findViewById<ImageButton>(R.id.ButtonPreviousMedia)
        ButtonPreviousMedia.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.switchToPreviousMediaItem()
            customDismiss(false)
        }
        //按钮：下一曲
        val ButtonNextMedia = view.findViewById<ImageButton>(R.id.ButtonNextMedia)
        ButtonNextMedia.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.switchToNextMediaItem()
            customDismiss(false)
        }
        //循环模式
        val ButtonLoopMode = view.findViewById<TextView>(R.id.ButtonLoopMode)
        ButtonLoopMode.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            val popup = PopupMenu(requireContext(), ButtonLoopMode)
            popup.menuInflater.inflate(R.menu.activity_player_popup_loop_mode, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.LoopMode_ONE -> {
                        chooseLoopMode("ONE"); true
                    }
                    R.id.LoopMode_ALL -> {
                        chooseLoopMode("ALL"); true
                    }
                    R.id.LoopMode_OFF -> {
                        chooseLoopMode("OFF"); true
                    }
                    else -> true
                }
            }
            popup.show()
        }
        //按钮：停止播放
        val ButtonStopPlaying = view.findViewById<ImageButton>(R.id.ButtonStopPlaying)
        ButtonStopPlaying.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //记录当前位置
            PlayerSingleton.savePositionToRoom()
            //清除上次播放信息
            PlayerSingleton.clearLastRecord(requireContext())
            //销毁播放器
            PlayerSingleton.DevastatePlayBundle(requireContext())
            //发回命令信息
            val result = bundleOf("KEY" to "stopPlaying")
            setFragmentResult("FROM_FRAGMENT_PLAY_LIST", result)
            //退出
            customDismiss(false)
        }
        //按钮：当前播放列表
        val ButtonCurrentList = view.findViewById<CardView>(R.id.ButtonCurrentList)
        ButtonCurrentListIcon = view.findViewById(R.id.ButtonCurrentListIcon)
        ButtonCurrentList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //弹出菜单切换当前播放列表
            val popup = PopupMenu(requireContext(), ButtonCurrentList)
            popup.menuInflater.inflate(R.menu.activity_play_list_popup_current_play_list, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.list_custom -> {
                        ToolVibrate().vibrate(requireContext())

                        setCurrentPlayList(0)

                        true
                    }

                    R.id.list_video_live -> {
                        ToolVibrate().vibrate(requireContext())

                        setCurrentPlayList(1)

                        true
                    }

                    R.id.list_music_live -> {
                        ToolVibrate().vibrate(requireContext())

                        setCurrentPlayList(2)

                        true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        setCurrentPlayListIcon()
        //按钮：切换列表
        ButtonCardCustomList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            switchToCustomPageByButton()
        }
        ButtonCardVideo.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            switchToVideoPageByButton()
        }
        ButtonCardMusic.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            switchToMusicPageByButton()
        }




        //ViewPager2
        ViewPager = view.findViewById(R.id.ViewPager)
        viewPagerAdapter = ViewPagerAdapter(
            this,
            onPlayClick = { uri -> onPlayClick(uri) },
            onAddToListClick = { uri -> onAddToListClick(uri) },
            onDeleteClick = { uriNumOnly -> onDeleteClick(uriNumOnly) },
            onPlayListChange = { flag -> onPlayListChange(flag) },
            onDefaultPageChange = { flag -> onAcquiescePageChange(flag) }
        )
        ViewPager.adapter = viewPagerAdapter
        startViewPagerListener()
        //设置ViewPager缓存页面数量
        ViewPager.offscreenPageLimit = 3
        //默认显示列表
        ViewPager.post {
            if (viewPagerAdapter.itemCount > 0){
                //使用上一次的页面
                if (PREFS_AcquiescePage == -1){
                    ViewPager.setCurrentItem(state_LastPage, false)
                }
                //使用设置的固定默认页面
                else if (PREFS_AcquiescePage in 0..2){
                    ViewPager.setCurrentItem(PREFS_AcquiescePage, false)
                }
            }
        }



        //监听返回手势(DialogFragment)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Dismiss(false)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    //onViewCreated END
    }

    //ViewPager
    //横向viewPager内部adapter类
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private class ViewPagerAdapter(
        fragment: Fragment,
        private val onPlayClick: (String) -> Unit,
        private val onAddToListClick: (String) -> Unit,
        private val onDeleteClick: (Long) -> Unit,
        private val onPlayListChange: (Int) -> Unit,
        private val onDefaultPageChange: (Int) -> Unit,
    ): FragmentStateAdapter(fragment) {
        //保持子类引用
        private lateinit var FragmentPlayListCustomFragment: FragmentPlayListCustomFragment
        private lateinit var FragmentPlayListVideoFragment: FragmentPlayListVideoFragment
        private lateinit var FragmentPlayListMusicFragment: FragmentPlayListMusicFragment

        //viewPager参数设置
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> {
                    FragmentPlayListCustomFragment(onPlayClick = onPlayClick, onDeleteClick = onDeleteClick, onPlayListChange = onPlayListChange, onDefaultPageChange = onDefaultPageChange)
                        .also { FragmentPlayListCustomFragment = it }
                }
                1 -> {
                    FragmentPlayListVideoFragment(onPlayClick = onPlayClick, onAddToListClick = onAddToListClick, onPlayListChange = onPlayListChange, onDefaultPageChange = onDefaultPageChange)
                        .also { FragmentPlayListVideoFragment = it }
                }
                2 -> {
                    FragmentPlayListMusicFragment(
                        onPlayClick = onPlayClick,
                        onAddToListClick = onAddToListClick,
                        onPlayListChange = onPlayListChange,
                        onDefaultPageChange = onDefaultPageChange
                    )
                        .also { FragmentPlayListMusicFragment = it }
                }
                else -> ListFragment()
            }

        //功能传递
        fun sendDataToFragment(position: Int, data: Any) {
            when (position) {
                0 -> FragmentPlayListCustomFragment.receiveInstruction(data)
                1 -> FragmentPlayListVideoFragment.receiveInstruction(data)
                2 -> FragmentPlayListMusicFragment.receiveInstruction(data)
            }
        }
    }
    //viewPager页面切换监听器
    private lateinit var ViewPager: ViewPager2
    private var ViewPagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            scrolledToPage(position)
        }
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {  }
        override fun onPageScrollStateChanged(state: Int) {  }
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
    //页签更新：位置 + 颜色d
    private fun scrolledToPage(position: Int){
        when(position){
            0 -> switchedToCustomPageByScroll()
            1 -> switchedToVideoPageByScroll()
            2 -> switchedToMusicPageByScroll()
        }
    }
    private fun switchToCustomPageByButton(){
        if (ViewPager.currentItem == 0) {
            viewPagerAdapter.sendDataToFragment(0, "go_top")
            return
        }
        ViewPager.currentItem = 0
        updateCardColor(0)
    }
    private fun switchToVideoPageByButton(){
        if (ViewPager.currentItem == 1) {
            viewPagerAdapter.sendDataToFragment(1, "go_top")
            return
        }
        ViewPager.currentItem = 1
        updateCardColor(1)
    }
    private fun switchToMusicPageByButton(){
        if (ViewPager.currentItem == 2) {
            viewPagerAdapter.sendDataToFragment(2, "go_top")
            return
        }
        ViewPager.currentItem = 2
        updateCardColor(2)
    }
    private fun switchedToCustomPageByScroll(){
        viewPagerAdapter.sendDataToFragment(0, "switch_to_you")
        saveLastPage()
        updateCardPosition(0)
        updateCardColor(0)
    }
    private fun switchedToVideoPageByScroll(){
        viewPagerAdapter.sendDataToFragment(1, "switch_to_you")
        saveLastPage()
        updateCardPosition(1)
        updateCardColor(1)
    }
    private fun switchedToMusicPageByScroll(){
        viewPagerAdapter.sendDataToFragment(2, "switch_to_you")
        saveLastPage()
        updateCardPosition(2)
        updateCardColor(2)
    }
    private fun updateCardColor(position: Int){
        when(position){
            0 -> {
                ButtonCardCustomList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
                ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            }
            1 -> {
                ButtonCardCustomList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
                ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            }

            2 -> {
                ButtonCardCustomList.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
                ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
            }
        }
    }
    private fun updateCardPosition(position: Int){
        when (position) {
            0 -> {
                TabScrollView.smoothScrollTo(0, 0)
            }
            1 -> {
                val left = ButtonCardVideo.left
                TabScrollView.smoothScrollTo(left, 0)
            }
            2 -> {
                val left = ButtonCardMusic.left
                TabScrollView.smoothScrollTo(left, 0)
            }
        }
    }
    //显示当前播放列表
    private fun setCurrentPlayListIcon(){
        val currentPlayList = PlayerListManager.getCurrentList(requireContext())
        when (currentPlayList) {
            0 -> {
                ButtonCurrentListIcon.setImageResource(R.drawable.ic_play_list_custom_list)
            }
            1 -> {
                ButtonCurrentListIcon.setImageResource(R.drawable.ic_main_fragment_video_icon)
            }
            2 -> {
                ButtonCurrentListIcon.setImageResource(R.drawable.ic_main_fragment_music_icon)
            }
        }

    }
    private fun onPlayListChange(flag: Int){
        setCurrentPlayListIcon()
    }
    private fun setCurrentPlayList(flag: Int){
        when(flag){
            0 -> {
                PlayerListManager.setPlayList("custom")
            }
            1 -> {
                PlayerListManager.setPlayList("video")
            }
            2 -> {
                PlayerListManager.setPlayList("music")
            }
        }
        setCurrentPlayListIcon()
        //发布消息
        for (f in 0..2){
            viewPagerAdapter.sendDataToFragment(f, "changed_current_list")
        }
    }
    private fun saveLastPage(){
        PREFS_List.edit { putInt("state_LastPage", ViewPager.currentItem) }
        vm.state_LastPage = ViewPager.currentItem
    }
    //设置默认展示列表
    private fun setAcquiescePage(flag: Int){
        if (flag == vm.PREFS_AcquiescePage){
            PREFS_List.edit { putInt("PREFS_AcquiescePage", -1) }
            vm.PREFS_AcquiescePage = -1
            requireContext().showCustomToast("已无默认页签，将使用上一次关闭时的页签作为默认页签", Toast.LENGTH_SHORT, 2)
        }else{
            PREFS_List.edit { putInt("PREFS_AcquiescePage", flag) }
            vm.PREFS_AcquiescePage = flag
        }
    }
    private fun onAcquiescePageChange(flag: Int){
        setAcquiescePage(flag)
    }

    //播放点击事件
    private fun onPlayClick(uriString: String) {
        if (uriString == PlayerSingleton.getMediaInfoUriString()){
            PlayerSingleton.continuePlay(true, force_request = true, need_fadeIn = false)
            requireContext().showCustomToast("已在播放该媒体", Toast.LENGTH_SHORT, 3)
        }else{
            PlayerSingleton.setMediaItem(uriString.toUri(), true)
            customDismiss()
        }
    }
    //删除点击事件
    private fun onDeleteClick(uriNumOnly: Long) {
        PlayerListManager.DeleteItemFromCustomList(uriNumOnly)
        viewPagerAdapter.sendDataToFragment(0, "update")
    }
    //添加到自定义列表点击事件
    private fun onAddToListClick(uriString: String) {
        PlayerListManager.InsertItemToCustomList(uriString, requireContext())
    }





    //Functions
    //读取设置信息
    private fun loadSettings(){
        //默认显式页的签
        PREFS_AcquiescePage = PREFS_List.getInt("PREFS_AcquiescePage", 0)
        vm.PREFS_AcquiescePage = PREFS_AcquiescePage
        //上一次显式的页签
        state_LastPage = PREFS_List.getInt("state_LastPage", 0)
        vm.state_LastPage = state_LastPage
    }
    //循环模式
    private fun chooseLoopMode(loopMode: String){
        ToolVibrate().vibrate(requireContext())
        //设置循环模式
        PlayerListManager.setLoopMode(when (loopMode) {
            "ONE" -> "ONE"
            "ALL" -> "ALL"
            "OFF" -> "OFF"
            else -> "OFF"
        }, requireContext())

        //刷新显示文本
        setLoopModeText()
        //不主动退出
    }
    private fun setLoopModeText(){
        val currentLoopMode = PlayerListManager.getLoopMode(requireContext())
        val ButtonLoopMode = view?.findViewById<TextView>(R.id.ButtonLoopMode)
        ButtonLoopMode?.text = when (currentLoopMode) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知模式"
        }
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(flag_need_vibrate: Boolean = true){
        if (!lockPage) {
            Dismiss(flag_need_vibrate)
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ ToolVibrate().vibrate(requireContext()) }

        stopViewPagerListener()

        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()
    }


}

