package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("ComposableNaming")
@UnstableApi
class PlayerFragmentPlayList: DialogFragment() {
    //静态方法
    companion object {
        fun newInstance(): PlayerFragmentPlayList =
            PlayerFragmentPlayList().apply {
                arguments = bundleOf(

                )
            }
    }
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //设置
    private lateinit var PREFS: SharedPreferences
    //协程作用域
    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    //横向按钮
    private lateinit var ButtonCardVideo: CardView
    private lateinit var ButtonCardMusic: CardView


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
        //检查播放列表是否加载完成
        val isMediaListProcessComplete = PlayerSingleton.isMediaListProcessComplete()
        if (!isMediaListProcessComplete){
            requireContext().showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
            Dismiss()
            return
        }
        //读取设置
        PREFS = requireContext().getSharedPreferences("PREFS", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_play_list, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        //按钮：刷新视频列表
        val ButtonRefreshVideoList = view.findViewById<ImageButton>(R.id.ButtonRefreshVideoList)
        ButtonRefreshVideoList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.updateMediaList(requireContext())



        }
        //按钮：刷新音乐列表
        val ButtonRefreshMusicList = view.findViewById<ImageButton>(R.id.ButtonRefreshMusicList)
        ButtonRefreshMusicList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.updateMediaList(requireContext())


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
        fun setLoopModeText(){
            val currentRepeatMode = PlayerSingleton.getRepeatMode()
            ButtonLoopMode.text = when (currentRepeatMode) {
                "ONE" -> "单集循环"
                "ALL" -> "列表循环"
                "OFF" -> "播完暂停"
                else -> "未知"
            }
        }
        setLoopModeText()
        ButtonLoopMode.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            val currentRepeatMode = PlayerSingleton.getRepeatMode()
            when (currentRepeatMode) {
                "OFF" -> {
                    PlayerSingleton.setRepeatMode("ONE")
                    setLoopModeText()
                }
                "ONE" -> {
                    PlayerSingleton.setRepeatMode("ALL")
                    setLoopModeText()
                }
                "ALL" -> {
                    PlayerSingleton.setRepeatMode("OFF")
                    setLoopModeText()
                }
                else -> {
                    PlayerSingleton.setRepeatMode("OFF")
                    setLoopModeText()
                }
            }
        }

        ButtonCardVideo = view.findViewById(R.id.ButtonCardVideo)
        ButtonCardMusic = view.findViewById(R.id.ButtonCardMusic)
        ButtonCardVideo.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            switchToVideoPage()
        }
        ButtonCardMusic.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            switchToMusicPage()
        }


        ViewPager = view.findViewById(R.id.ViewPager)
        ViewPager.adapter = ViewPagerAdapter(
            this,
            ::onPlayClick,
            onDeleteClick = { uri, flag -> onDeleteClick(uri, flag) } )
        startViewPagerListener()






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
    //横向viewPager内部adapter类
    private class ViewPagerAdapter(
        fragment: Fragment,
        private val onPlayClick: (Uri) -> Unit,
        private val onDeleteClick: (Uri, Int) -> Unit
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment =
            when (position) {
                0, 1 -> PlayerFragmentPlayListFragment(flag = position, onPlayClick = onPlayClick, onDeleteClick = onDeleteClick)
                else -> ListFragment()
            }



    }
    //viewPager页面监听器
    private lateinit var ViewPager: ViewPager2
    private var ViewPagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateCardState(position)
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // 滚动过程中持续回调，可做联动动画
        }

        override fun onPageScrollStateChanged(state: Int) {
            // 状态：SCROLL_STATE_IDLE / DRAGGING / SETTLING
        }
    }
    private fun startViewPagerListener(){
        ViewPager.registerOnPageChangeCallback(ViewPagerListener)
    }
    private fun stopViewPagerListener(){
        ViewPager.unregisterOnPageChangeCallback(ViewPagerListener)
    }
    //卡片颜色切换
    private fun switchToVideoPage(){
        ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
        ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
        ViewPager.currentItem = 0

    }
    private fun switchToMusicPage(){
        ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
        ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
        ViewPager.currentItem = 1

    }
    private fun updateCardState(position: Int){
        if (position == 0){
            ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
            ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
        }
        else if (position == 1){
            ButtonCardMusic.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_ON))
            ButtonCardVideo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonCard_OFF))
        }
    }


    //播放点击事件
    private fun onPlayClick(uri: Uri) {
        if (uri.toString() == PlayerSingleton.getMediaInfoUri()){
            PlayerSingleton.playPlayer()
            requireContext().showCustomToast("已在播放该媒体", Toast.LENGTH_SHORT, 3)
        }else{
            PlayerSingleton.setMediaItem(uri, true)
            customDismiss()
        }
    }
    //删除点击事件
    private fun onDeleteClick(uri: Uri, flag: Int) {
        when (flag){
            0 -> {
                //删除视频

            }
            1 -> {
                //删除音乐

            }
        }

    }

    //Functions
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