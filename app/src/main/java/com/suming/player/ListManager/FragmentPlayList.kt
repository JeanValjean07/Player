package com.suming.player.ListManager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ComposableNaming")
@RequiresApi(Build.VERSION_CODES.Q)
@UnstableApi
//@Suppress("unused")
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
    private val viewModel: PlayerListViewModel by activityViewModels()
    //协程
    private val coroutine_registerComponent = CoroutineScope(Dispatchers.IO)


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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_play_list, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化界面
        initInterface(view)


        //注册操作按钮
        coroutine_registerComponent.launch {
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

            //按钮：上一曲
            val ButtonPreviousMedia = view.findViewById<ImageButton>(R.id.ButtonPreviousMedia)
            ButtonPreviousMedia.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                PlayerSingleton.switchToPreviousMediaItem()
            }
            //按钮：下一曲
            val ButtonNextMedia = view.findViewById<ImageButton>(R.id.ButtonNextMedia)
            ButtonNextMedia.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                PlayerSingleton.switchToNextMediaItem()
            }
            //循环模式
            updateLoopModeText()
            val ButtonCardLoopMode = view.findViewById<CardView>(R.id.ButtonCardLoopMode)
            ButtonCardLoopMode.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val popup = PopupMenu(requireContext(), ButtonCardLoopMode)
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
                //关闭播放器
                PlayerSingleton.stopPlayBundle(true,requireContext())
                //发回命令信息
                val result = bundleOf("KEY" to "stopPlaying")
                setFragmentResult("FROM_FRAGMENT_PLAY_LIST", result)
                //退出
                Dismiss(false)
            }


            //卡片选单按钮：当前播放列表
            val ButtonCurrentList = view.findViewById<CardView>(R.id.ButtonCurrentList)
            ButtonCurrentListIcon = view.findViewById(R.id.ButtonCurrentListIcon)
            ButtonCurrentList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //弹出菜单切换当前播放列表
                val popup = PopupMenu(requireContext(), ButtonCurrentList)
                popup.menuInflater.inflate(R.menu.activity_play_list_popup_current_play_list, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        //选择自定义列表
                        R.id.list_custom -> {
                            ToolVibrate().vibrate(requireContext())
                            //选择目标播放列表
                            set_currentPlayList(0)

                            true
                        }
                        //选择视频列表
                        R.id.list_video_live -> {
                            ToolVibrate().vibrate(requireContext())
                            //选择目标播放列表
                            set_currentPlayList(1)

                            true
                        }
                        //选择音乐列表
                        R.id.list_music_live -> {
                            ToolVibrate().vibrate(requireContext())
                            //选择目标播放列表
                            set_currentPlayList(2)

                            true
                        }
                        //其他
                        else -> true
                    }
                }
                popup.show()
            }
            //横滑页签按钮
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


        //coroutine_registerComponent END
        }



        //启动viewPager
        ViewPager = view.findViewById(R.id.ViewPager)
        viewPagerAdapter = ViewPagerAdapter(this)
        ViewPager.adapter = viewPagerAdapter
        //开启页面监听器
        startViewPagerListener()
        //设置ViewPager缓存页面数量
        ViewPager.offscreenPageLimit = 2
        //默认显示列表
        ViewPager.post {
            if (viewPagerAdapter.itemCount > 0){
                val acquiescePage = PlayerListManager.get_PREFS_AcquiescePage(requireContext())
                //使用上一次的页面
                if (acquiescePage == -1){
                    val lastPageSign = PlayerListManager.get_state_LastPageSign(requireContext())
                    ViewPager.setCurrentItem(lastPageSign, false)
                }
                //使用设置的固定默认页面
                else if (acquiescePage in 0..2){
                    ViewPager.setCurrentItem(acquiescePage, false)
                }
            }
        }


        //接收子Fragment返回值
        childFragmentManager.setFragmentResultListener("FRAGMENT_CUSTOM_LIST_FRAGMENT", this){ _, bundle ->
            val token = bundle.getString("TOKEN") ?: return@setFragmentResultListener
            when(token){
                //需要刷新当前列表指示图标
                "FRAGMENT_RETURN_UPDATE_LIST_ICON" -> {
                    updateIcon_currentPlayList()
                }

            }
        }
        childFragmentManager.setFragmentResultListener("FRAGMENT_VIDEO_LIST_FRAGMENT", this){ _, bundle ->
            val token = bundle.getString("TOKEN") ?: return@setFragmentResultListener
            when(token){
                //需要刷新当前列表指示图标
                "FRAGMENT_RETURN_UPDATE_LIST_ICON" -> {
                    updateIcon_currentPlayList()
                }


            }
        }
        childFragmentManager.setFragmentResultListener("FRAGMENT_MUSIC_LIST_FRAGMENT", this){ _, bundle ->
            val token = bundle.getString("TOKEN") ?: return@setFragmentResultListener
            when(token){
                //需要刷新当前列表指示图标
                "FRAGMENT_RETURN_UPDATE_LIST_ICON" -> {
                    updateIcon_currentPlayList()
                }


            }
        }


        //监听返回手势
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Dismiss(false)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    //onViewCreated END
    }



    //viewPager
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private class ViewPagerAdapter(
        innerFragment: Fragment,
    ):FragmentStateAdapter(innerFragment){

        //
        override fun getItemCount(): Int = 3
        //
        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> FragmentPlayListCustomFragment()

                1 -> FragmentPlayListVideoFragment()

                2 -> FragmentPlayListMusicFragment()

                else -> ListFragment()
            }

    }
    //viewPager页面切换监听器
    private lateinit var ViewPager: ViewPager2
    private var ViewPagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            saveLastPageSign(position)
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


    //向子Fragment传递消息
    private fun sendDataToChildFragment(target: Any, data: String) {
        var position = 0
        //合成position
        if (target is String){
            position = when(target){
                "custom" -> 0
                "video" -> 1
                "music" -> 2
                else -> {
                    requireContext().showCustomToast("标签未命中任何有效目标(String)")
                    return
                }
            }
        }else if (target is Int){
            if (target in 0..2){
                position = target
            }else{
                requireContext().showCustomToast("标签未命中任何有效目标(Int)")
                return
            }
        }

        //发布消息
        when (position) {
            0 -> {
                childFragmentManager.setFragmentResult("FRAGMENT_TOCHILD_CUSTOM", bundleOf(
                    "TOKEN" to data
                ))
            }
            1 -> {
                childFragmentManager.setFragmentResult("FRAGMENT_TOCHILD_VIDEO", bundleOf(
                    "TOKEN" to data
                ))
            }
            2 -> {
                childFragmentManager.setFragmentResult("FRAGMENT_TOCHILD_MUSIC", bundleOf(
                    "TOKEN" to data
                ))
            }
        }
    }



    //页签焦点
    private fun scrolledToPage(position: Int){
        when(position){
            0 -> onFocusPage_Custom()
            1 -> onFocusPage_Video()
            2 -> onFocusPage_Music()
        }
    }
    private fun onFocusPage_Custom(){
        sendDataToChildFragment(0, "FRAGMENT_PASSIN_FOCUS")
        updateCardPosition(0)
        updateCardColor(0)
    }
    private fun onFocusPage_Video(){
        sendDataToChildFragment(1, "FRAGMENT_PASSIN_FOCUS")
        updateCardPosition(1)
        updateCardColor(1)
    }
    private fun onFocusPage_Music(){
        sendDataToChildFragment(2, "FRAGMENT_PASSIN_FOCUS")
        updateCardPosition(2)
        updateCardColor(2)
    }
    //页签点击切换
    private fun switchToCustomPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 0) {
            sendDataToChildFragment(0, "FRAGMENT_PASSIN_SCROLLTOP")
            return
        }
        //切换到自定义页签
        ViewPager.currentItem = 0
    }
    private fun switchToVideoPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 1) {
            sendDataToChildFragment(1, "FRAGMENT_PASSIN_SCROLLTOP")
            return
        }
        //切换到视频页签
        ViewPager.currentItem = 1
    }
    private fun switchToMusicPageByButton(){
        //已在此页时回到顶部
        if (ViewPager.currentItem == 2) {
            sendDataToChildFragment(2, "FRAGMENT_PASSIN_SCROLLTOP")
            return
        }
        //切换到音乐页签
        ViewPager.currentItem = 2
    }
    //页签样式/位置更新
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
    //更新当前播放列表卡片指示图标丨消息触发
    private fun updateIcon_currentPlayList(){
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
    //设置当前播放列表
    private fun set_currentPlayList(position: Int){
        var success = false
        when(position){
            0 -> {
                success = PlayerListManager.setPlayList("custom")
            }
            1 -> {
                success = PlayerListManager.setPlayList("video")
            }
            2 -> {
                success = PlayerListManager.setPlayList("music")
            }
        }
        //通知子Fragment更新
        val currentPage = ViewPager.currentItem
        sendDataToChildFragment(currentPage, "FRAGMENT_PASSIN_CURRENT_LIST_UPDATE")
        //更新当前播放列表指示图标
        updateIcon_currentPlayList()
        //更新当前播放列表图标
        if (success){
            requireContext().showCustomToast("设置成功",2)
        }else{
            requireContext().showCustomToast("设置失败",2)
        }
    }
    //保存上次页签
    private var state_saveLastPageSign_First = true
    private fun saveLastPageSign(position: Int){
        //首个指令不保存
        if (state_saveLastPageSign_First){
            state_saveLastPageSign_First = false
            return
        }
        //向列表管理器设置
        PlayerListManager.set_state_LastPageSign(requireContext(),position)

    }


    //播放点击事件
    private fun onPlayClick(uriString: String) {
        if (uriString == PlayerSingleton.getMediaInfoUriString()){
            PlayerSingleton.continuePlay(true, force_request = true, need_fadeIn = false)
            requireContext().showCustomToast("已在播放该媒体",3)
        }else{
            PlayerSingleton.setMediaItem(uriString.toUri(), true,requireContext())
        }
    }
    //删除点击事件
    private fun onDeleteClick(uriNumOnly: Long)  {
        PlayerListManager.DeleteItemFromCustomList(uriNumOnly)
        sendDataToChildFragment(0, "update")
    }
    //添加到自定义列表点击事件
    private fun onAddToListClick(uriString: String) {
        PlayerListManager.InsertItemToCustomList(uriString, requireContext())
    }




    //初始化界面
    private fun initInterface(view: View){
        //设置卡片高度
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            val MainCard = view.findViewById<CardView>(R.id.main_card)
            MainCard.layoutParams.height = (resources.displayMetrics.heightPixels * 0.7).toInt()
        }
        //初始化全局元素
        TabScrollView = view.findViewById(R.id.TabScrollView)
        ButtonCardCustomList = view.findViewById(R.id.ButtonCardCustomList)
        ButtonCardVideo = view.findViewById(R.id.ButtonCardVideo)
        ButtonCardMusic = view.findViewById(R.id.ButtonCardMusic)

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
        updateLoopModeText()

    }
    private fun updateLoopModeText(){
        val currentLoopMode = PlayerListManager.getLoopMode(requireContext())
        val ButtonTextLoopMode = view?.findViewById<TextView>(R.id.ButtonTextLoopMode)
        ButtonTextLoopMode?.text = when (currentLoopMode) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知模式"
        }
    }

    //自定义退出逻辑
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ ToolVibrate().vibrate(requireContext()) }

        stopViewPagerListener()

        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()
    }

//Fragment END
}

