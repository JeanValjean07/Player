package com.suming.player.ActivityComponent.PlayerActivity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.google.android.material.button.MaterialButton
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.SettingsRequestCenter
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaDataBaseMaster
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@UnstableApi
@Suppress("unused","NewApi")
@SuppressLint("UseGetLayoutInflater", "InflateParams","SetTextI18n")
class PlayerFragmentMoreButton: DialogFragment() {
    companion object {
        fun newInstance(): PlayerFragmentMoreButton = PlayerFragmentMoreButton().apply { arguments =
            bundleOf()
        }
    }

    //连接到共享ViewModel
    private val viewModel: PlayerViewModel by activityViewModels()




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
        //
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):View{
        //获得view
        val view = inflater.inflate(R.layout.fragment_more_button, container, false)

        //初始化界面
        init(view)

        return view
    }

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        //注册控件
        register(view)

    }

    override fun onResume() {
        super.onResume()
        //发布开启事件(暂时转移到mainBusiness末尾)
        returnFragment(FragmentConnector.fragment_event_open)
    }
    override fun onPause() {
        super.onPause()
        //发布关闭事件
        returnFragment(FragmentConnector.fragment_event_close)
    }

    private fun init(view: View){
        //设置卡片
        display(view)

    }




    //Main Thread Functions
    @SuppressLint("ClickableViewAccessibility")
    private fun register(view: View){
        lifecycleScope.launch(Dispatchers.Main) {
            //循环模式选单
            val ButtonCardLoopMode = view.findViewById<CardView>(R.id.ButtonCardLoopMode)
            ButtonCardLoopMode.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                chooseLoopMode(ButtonCardLoopMode)
            }
            updateLoopModeText()
            //倍速管理选单
            val ButtonCardPlaySpeed = view.findViewById<CardView>(R.id.ButtonCardPlaySpeed)
            ButtonCardPlaySpeed.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                choosePlaySpeed(ButtonCardPlaySpeed)
            }
            updatePlaySpeedText()
            //定时关闭倒计时选单
            val ButtonCardAutoShut = view.findViewById<CardView>(R.id.ButtonCardAutoShut)
            ButtonCardAutoShut.setOnClickListener { _ ->
                ToolVibrate().vibrate(requireContext())
                chooseAutoShut(ButtonCardAutoShut)
            }
            updateAutoShutText()
            //退出
            val ButtonExit = view.findViewById<CircleButton>(R.id.buttonExit)
            ButtonExit.setOnClickListener {
                dismiss()
            }
            //点击空白区域退出
            val topArea = view.findViewById<View>(R.id.out_area)
            topArea.setOnClickListener {
                dismiss()
            }
            //锁定页面
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
            //点击顶部区域回顶
            val TopBarArea = view.findViewById<View>(R.id.TopBarArea)
            TopBarArea.setOnClickListener {
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                if (NestedScrollView.scrollY == 0) { return@setOnClickListener }
                ToolVibrate().vibrate(requireContext())
                //回到顶部
                NestedScrollView.smoothScrollTo(0, 0)
            }
            //面板下滑关闭
            if (!SettingsRequestCenter.get_PREFS_DisableFragmentGesture(requireContext())){
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                    var down_y = 0f
                    var deltaY = 0f
                    var deltaY_ReachPadding = false
                    val RootCard = view.findViewById<CardView>(R.id.main_card)
                    val RootCardOriginY = RootCard.translationY
                    val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                    var NestedScrollViewAtTop = true
                    NestedScrollView.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                deltaY_ReachPadding = false
                                if (NestedScrollView.scrollY != 0){
                                    NestedScrollViewAtTop = false
                                    return@setOnTouchListener false
                                }else{
                                    NestedScrollViewAtTop = true
                                    down_y = event.rawY
                                }
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (!NestedScrollViewAtTop){
                                    return@setOnTouchListener false
                                }
                                deltaY = event.rawY - down_y
                                if (deltaY < 0){
                                    return@setOnTouchListener false
                                }
                                if (deltaY >= 400f){
                                    if (!deltaY_ReachPadding){
                                        deltaY_ReachPadding = true
                                        ToolVibrate().vibrate(requireContext())
                                    }
                                }
                                RootCard.translationY = RootCardOriginY + deltaY
                                return@setOnTouchListener true
                            }
                            MotionEvent.ACTION_UP -> {
                                if (deltaY >= 400f){
                                    dismiss()
                                }else{
                                    RootCard.animate()
                                        .translationY(0f)
                                        .setInterpolator(DecelerateInterpolator(1f))
                                        .duration = 300
                                }

                            }
                        }
                        return@setOnTouchListener false
                    }
                }
                else if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                    var down_y = 0f
                    var deltaY = 0f
                    var down_x = 0f
                    var deltaX = 0f
                    var deltaX_ReachPadding = false
                    var Y_move_ensure = false
                    val RootCard = view.findViewById<CardView>(R.id.main_card)
                    val RootCardOriginX = RootCard.translationX
                    val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                    NestedScrollView.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                down_x = event.rawX
                                down_y = event.rawY
                                Y_move_ensure = false
                                deltaX_ReachPadding = false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                deltaY = event.rawY - down_y
                                deltaX = event.rawX - down_x
                                if (deltaX < 0){
                                    return@setOnTouchListener false
                                }
                                if (deltaX >= 200f){
                                    if (!deltaX_ReachPadding){
                                        deltaX_ReachPadding = true
                                        ToolVibrate().vibrate(requireContext())
                                    }
                                }
                                if (Y_move_ensure){
                                    return@setOnTouchListener false
                                }
                                if (abs(deltaY) > abs(deltaX)){
                                    Y_move_ensure = true
                                    return@setOnTouchListener false
                                }
                                RootCard.translationX = RootCardOriginX + deltaX
                                return@setOnTouchListener true
                            }
                            MotionEvent.ACTION_UP -> {
                                if (Y_move_ensure){
                                    return@setOnTouchListener false
                                }
                                if (deltaX >= 200f){
                                    dismiss()
                                }else{
                                    RootCard.animate()
                                        .translationX(0f)
                                        .setInterpolator(DecelerateInterpolator(1f))
                                        .duration = 300
                                }
                            }
                        }
                        return@setOnTouchListener false
                    }
                }
            }

            //开启方向监听器
            val switch_EnableOriListener = view.findViewById<SwitchCompat>(R.id.EnableOriListener)
            switch_EnableOriListener.isChecked = SettingsRequestCenter.get_PREFS_EnableOrientationListener(requireContext())
            switch_EnableOriListener.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //读取目标状态并修改设置
                SettingsRequestCenter.set_PREFS_EnableOrientationListener(switch_EnableOriListener.isChecked)
                //发回结果(仅告知设置变更,不返回值,自行读取)
                returnFragment(FragmentConnector.fragment_more_button_switch_ori_listener)
                customDismiss()
            }
            //后台播放
            val switch_BackgroundPlay = view.findViewById<SwitchCompat>(R.id.Switch_BackgroundPlay)
            switch_BackgroundPlay.isChecked = SettingsRequestCenter.get_PREFS_BackgroundPlay(requireContext())
            switch_BackgroundPlay.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //仅修改设置即可
                SettingsRequestCenter.set_PREFS_BackgroundPlay(switch_BackgroundPlay.isChecked)

                customDismiss()
            }
            //仅在播放完成后退出
            val switch_ExitWhenMediaEnd = view.findViewById<SwitchCompat>(R.id.Switch_ExitWhenMediaEnd)
            switch_ExitWhenMediaEnd.isChecked = SettingsRequestCenter.get_PREFS_OnlyStopUnMediaEnd(requireContext())
            switch_ExitWhenMediaEnd.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //仅修改设置即可
                SettingsRequestCenter.set_PREFS_OnlyStopUnMediaEnd(switch_ExitWhenMediaEnd.isChecked)

                customDismiss()
            }
            //保存播放进度
            val switch_saveLastPosition = view.findViewById<SwitchCompat>(R.id.Switch_SavePositionWhenExit)
            switch_saveLastPosition.isChecked = MediaDataBaseMaster.get_PREFS_saveProgress("",requireContext())
            switch_saveLastPosition.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //检查能否进行此项设置(是否可获取到媒体类型和NUM_ID)
                val (MediaType,NUM_ID) = GET_MediaType_and_NUM_ID()
                if (MediaType.isEmpty() || NUM_ID <= 0){

                    AlertDialog.Builder(requireContext())
                        .setTitle("媒体详情获取失败")
                        .setMessage("媒体文件可能存在于非公开目录，无法获取开启此选项所需的必要信息")
                        .setPositiveButton("了解") { dialog, which ->
                            ToolVibrate().vibrate(requireContext())

                            customDismiss()

                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()

                    switch_saveLastPosition.isChecked = false

                    return@setOnClickListener
                }


                //修改设置
                val isChecked = switch_saveLastPosition.isChecked
                MediaDataBaseMaster.set_PREFS_saveProgress("",isChecked,requireContext())

                //不发回结果
                customDismiss()
            }

            //截屏
            val ButtonCapture = view.findViewById<ImageButton>(R.id.buttonCapture)
            ButtonCapture.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_capture_frame)
                dismiss()
            }
            //打开播放列表
            val ButtonPlayList = view.findViewById<ImageButton>(R.id.ButtonPlayList)
            ButtonPlayList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_start_play_list)
                dismiss()
            }
            //回到视频起始
            val ButtonBackToStart = view.findViewById<ImageButton>(R.id.buttonBackToStart)
            ButtonBackToStart.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_back_to_start)
                dismiss()
            }

            //开启小窗
            val ButtonStartPiP = view.findViewById<TextView>(R.id.ButtonStartPiP)
            ButtonStartPiP.setOnClickListener {
                returnFragment(FragmentConnector.fragment_more_button_start_pip_window)
                dismiss()
            }
            //更新封面
            val ButtonUpdateCover = view.findViewById<TextView>(R.id.buttonUpdateCover)
            ButtonUpdateCover.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //检查能否进行此项设置(是否可获取到媒体类型和NUM_ID)
                val (MediaType,NUM_ID) = GET_MediaType_and_NUM_ID()
                if (MediaType.isEmpty() || NUM_ID <= 0){

                    AlertDialog.Builder(requireContext())
                        .setTitle("媒体详情获取失败")
                        .setMessage("媒体文件可能存在于非公开目录，无法获取开启此选项所需的必要信息")
                        .setPositiveButton("了解") { dialog, which ->
                            ToolVibrate().vibrate(requireContext())

                            customDismiss()

                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()

                    return@setOnClickListener
                }

                updateCoverFrame(ButtonUpdateCover)
            }
            //提取所有帧
            val ButtonExtractFrame = view.findViewById<ImageButton>(R.id.buttonExtractFrame)
            ButtonExtractFrame.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                requireContext().showCustomToast("暂不开放此功能", 3)
            }

            //保持屏幕常亮
            val SC_KeepScreenOn = view.findViewById<SwitchCompat>(R.id.SC_KeepScreenOn)
            SC_KeepScreenOn.isChecked = SettingsRequestCenter.GET_PRF_KeepScreenOn(requireContext())
            SC_KeepScreenOn.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //修改设置
                SettingsRequestCenter.SET_PRF_KeepScreenOn(requireContext(), SC_KeepScreenOn.isChecked)
                //发回刷新消息
                returnFragment(FragmentConnector.fragment_more_button_update_keep_screen_on)

                customDismiss()
            }

            //视频信息
            val ButtonVideoInfo = view.findViewById<TextView>(R.id.buttonVideoInfo)
            ButtonVideoInfo.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_open_video_info)
                dismiss()
            }
            //分享
            val ButtonSysShare = view.findViewById<TextView>(R.id.buttonSysShare)
            ButtonSysShare.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //检查能否进行此项设置(是否可获取到媒体类型和NUM_ID)
                val (MediaType,NUM_ID) = GET_MediaType_and_NUM_ID()
                if (MediaType.isEmpty() || NUM_ID <= 0){

                    AlertDialog.Builder(requireContext())
                        .setTitle("媒体详情获取失败")
                        .setMessage("媒体文件可能存在于非公开目录，无法获取开启此选项所需的必要信息")
                        .setPositiveButton("了解") { dialog, which ->
                            ToolVibrate().vibrate(requireContext())

                            customDismiss()

                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()

                    return@setOnClickListener
                }

                returnFragment(FragmentConnector.fragment_more_button_sys_share_video)
                dismiss()
            }
            //均衡器
            val ButtonEqualizer = view.findViewById<TextView>(R.id.buttonEqualizer)
            ButtonEqualizer.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                context?.showCustomToast("暂不开放此功能", 3)

                /*
                val result = bundleOf("KEY" to "Equalizer")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()

                 */
            }
            //清除当前进度条缩略图
            val ButtonClearMiniature = view.findViewById<TextView>(R.id.ButtonReCreateThumb)
            ButtonClearMiniature.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //检查能否进行此项设置(是否可获取到媒体类型和NUM_ID)
                val (MediaType,NUM_ID) = GET_MediaType_and_NUM_ID()
                if (MediaType.isEmpty() || NUM_ID <= 0){

                    AlertDialog.Builder(requireContext())
                        .setTitle("媒体详情获取失败")
                        .setMessage("媒体文件可能存在于非公开目录，无法获取开启此选项所需的必要信息")
                        .setPositiveButton("了解") { dialog, which ->
                            ToolVibrate().vibrate(requireContext())

                            customDismiss()

                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()

                    return@setOnClickListener
                }


                AlertDialog.Builder(requireContext())
                    .setTitle("确定删除进度条缩略图吗?")
                    .setMessage("")
                    .setPositiveButton("确认") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        returnFragment(FragmentConnector.fragment_more_button_clear_miniature)

                        customDismiss()

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()


            }
            //删除自定义封面图
            val ButtonDeleteCustomCover = view.findViewById<TextView>(R.id.ButtonDeleteCustomCover)
            ButtonDeleteCustomCover.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                //检查能否进行此项设置(是否可获取到媒体类型和NUM_ID)
                val (MediaType,NUM_ID) = GET_MediaType_and_NUM_ID()
                if (MediaType.isEmpty() || NUM_ID <= 0){

                    AlertDialog.Builder(requireContext())
                        .setTitle("媒体详情获取失败")
                        .setMessage("媒体文件可能存在于非公开目录，无法获取开启此选项所需的必要信息")
                        .setPositiveButton("了解") { dialog, which ->
                            ToolVibrate().vibrate(requireContext())

                            customDismiss()

                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()

                    return@setOnClickListener
                }


                AlertDialog.Builder(requireContext())
                    .setTitle("确定删除自定义封面吗?")
                    .setMessage("")
                    .setPositiveButton("确认") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        returnFragment(FragmentConnector.fragment_more_button_delete_custom_cover)

                        customDismiss()

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()

            }
            //解除亮度控制
            val ButtonUnBindBrightness = view.findViewById<TextView>(R.id.ButtonUnBindBrightness)
            ButtonUnBindBrightness.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_unlock_brightness_control)
                dismiss()
            }
            //绑定播放视图
            val ButtonBindPlayView = view.findViewById<TextView>(R.id.ButtonBindPlayView)
            ButtonBindPlayView.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                returnFragment(FragmentConnector.fragment_more_button_bind_play_view)
                dismiss()
            }
            //重新启用播放感知
            val Button_RestartPerception = view.findViewById<TextView>(R.id.Button_RestartPerception)
            Button_RestartPerception.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                AlertDialog.Builder(requireContext())
                    .setTitle("重新启用播放感知")
                    .setMessage("使用播控中心控制播放时，可能导致播放感知被无意关闭。查阅指南或发布页可了解详细逻辑。")
                    .setPositiveButton("了解") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        PlayerListener.state_perception_on = true

                        customDismiss()

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()

            }
            //使进度条脱离异常状态
            val Button_EscapeScrollerError = view.findViewById<TextView>(R.id.Button_EscapeScrollerError)
            Button_EscapeScrollerError.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                AlertDialog.Builder(requireContext())
                    .setTitle("正在开发中")
                    .setMessage("暂未发现导致进度条异常的场景，如果遇到，先退出播放页重进")
                    .setPositiveButton("了解") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        customDismiss()

                        dialog.dismiss()
                    }
                    .setNegativeButton("立即退出播放页") { dialog, which ->
                        ToolVibrate().vibrate(requireContext())

                        returnFragment(FragmentConnector.fragment_more_button_exit_right_now)

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()

            }


            //注册进度条相关功能(经典播放页时不显示进度条相关功能)
            val CardScrollerStuff = view.findViewById<CardView>(R.id.card_scrollerStuff)
            if(viewModel.state_s_area_type == S_Area_Helper.S_AreaType_SEEKBAR){
                CardScrollerStuff.visibility = View.GONE
            }else if(viewModel.state_s_area_type == S_Area_Helper.S_AreaType_SCROLLER){
                //按钮：AlwaysSeek
                val ButtonAlwaysSeek = view.findViewById<FrameLayout>(R.id.ButtonActualAlwaysSeek)
                val ButtonAlwaysSeekMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialAlwaysSeek)
                fun updateButtonAlwaysSeekColor(){
                    if (viewModel.PREFS_AlwaysSeek){
                        ButtonAlwaysSeekMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_ON))
                    }else{
                        ButtonAlwaysSeekMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_OFF))
                    }
                }
                updateButtonAlwaysSeekColor()
                ButtonAlwaysSeek.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !viewModel.PREFS_AlwaysSeek
                    viewModel.PREFS_AlwaysSeek = target
                    SettingsRequestCenter.set_PREFS_EnableAlwaysSeek(target)

                    //按钮改为目标颜色
                    updateButtonAlwaysSeekColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "AlwaysSeek", "target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
                }

                //按钮：链接滚动条与视频进度
                val ButtonLinkScroll = view.findViewById<FrameLayout>(R.id.ButtonActualLinkScroll)
                val ButtonLinkScrollMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialLinkScroll)
                fun updateButtonLinkScrollColor(){
                    if (viewModel.PREFS_LinkScroll){
                        ButtonLinkScrollMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_ON))
                    }else{
                        ButtonLinkScrollMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_OFF))
                    }
                }
                updateButtonLinkScrollColor()
                ButtonLinkScroll.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !viewModel.PREFS_LinkScroll
                    viewModel.PREFS_LinkScroll = target
                    SettingsRequestCenter.set_PREFS_EnableLinkScroll(target)

                    //按钮改为目标颜色
                    updateButtonLinkScrollColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "LinkScroll", "target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
                }

                //按钮：单击跳转
                val ButtonTapJump = view.findViewById<FrameLayout>(R.id.ButtonActualTapJump)
                val ButtonTapJumpMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialTapJump)
                fun updateButtonTapJumpColor(){
                    if (viewModel.PREFS_TapJump){
                        ButtonTapJumpMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_ON))
                    }else{
                        ButtonTapJumpMaterial.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.THEME_1_Background_ButtonCircle_OFF))
                    }
                }
                updateButtonTapJumpColor()
                ButtonTapJump.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !viewModel.PREFS_TapJump
                    viewModel.PREFS_TapJump = target
                    SettingsRequestCenter.set_PREFS_EnableTapJump(target)

                    //按钮改为目标颜色
                    updateButtonTapJumpColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "TapJump", "target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
                }

            }

            //未显示进度条
            if (SettingsRequestCenter.GET_PRF_PlayPageType(requireContext()) == SettingsRequestCenter.PlayPageType_Neo){
                if (viewModel.state_s_area_type == S_Area_Helper.S_AreaType_SEEKBAR){

                    val text = "这是由于媒体所在的文件夹为非公有文件夹，或者文件夹被.nomedia标记，导致程序无法获取所需的必要信息，同时导致媒体信息等可能无法查看。" +
                            "\n\n要解决此问题，请将媒体文件移动至公开文件夹，他们是：DCIM, Pictures, Movies, Music, Downloads, Documents，并保证文件夹未受到.nomedia等标记的影响。"


                    val LinearLayout_whyNotShowScroller = view.findViewById<LinearLayout>(R.id.LinearLayout_whyNotShowScroller)
                    val Button_whyNotShowScroller = view.findViewById<TextView>(R.id.Button_whyNotShowScroller)
                    LinearLayout_whyNotShowScroller.visibility = View.VISIBLE
                    Button_whyNotShowScroller.setOnClickListener {
                        ToolVibrate().vibrate(requireContext())

                        AlertDialog.Builder(requireContext())
                            .setTitle("未显示进度条？")
                            .setMessage(text)
                            .setPositiveButton("了解") { dialog, which ->
                                ToolVibrate().vibrate(requireContext())

                                customDismiss()

                                dialog.dismiss()
                            }
                            .setCancelable(true)
                            .show()



                    }
                }
            }



        }
    }

    //发布事件
    private fun returnFragment(event: String){
        val result = bundleOf(FragmentConnector.receive_key to event)
        setFragmentResult(FragmentConnector.fragment_request_key_more_button, result)
    }
    private fun returnFragment(event: String,extra: String){
        val result = bundleOf(FragmentConnector.receive_key to event,FragmentConnector.extra_key to extra)
        setFragmentResult(FragmentConnector.fragment_request_key_more_button, result)
    }


    //获取当前媒体类型和NUM_ID
    private fun GET_MediaType_and_NUM_ID(): Pair<String, Long>{
        val MediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        val NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()

        return Pair(MediaType,NUM_ID)
    }

    //更新封面选单
    private fun updateCoverFrame(anchor: TextView){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_player_popup_change_cover, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                //更新封面-截取视频当前帧
                R.id.item_useCurrentFrame -> {
                    ToolVibrate().vibrate(requireContext())
                    returnFragment(FragmentConnector.fragment_more_button_update_cover_frame,FragmentConnector.update_cover_frame_use_current_frame)
                    dismiss();true
                }
                //更新封面-使用默认封面
                R.id.item_useDefaultCover -> {
                    ToolVibrate().vibrate(requireContext())
                    returnFragment(FragmentConnector.fragment_more_button_update_cover_frame,FragmentConnector.update_cover_frame_use_default_frame)
                    dismiss();true
                }
                //更新封面-选择本地图片
                R.id.item_pickFromLocal -> {
                    ToolVibrate().vibrate(requireContext())
                    returnFragment(FragmentConnector.fragment_more_button_update_cover_frame,FragmentConnector.update_cover_frame_pick_local_frame)
                    dismiss();true
                }
                else -> true
            }
        }
        popup.show()
    }
    //循环模式选单
    private fun chooseLoopMode(anchor: CardView){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_player_popup_loop_mode, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.LoopMode_ONE -> {
                    chooseLoopMode("ONE")
                    //设为单集循环时,必要时可自动开始
                    PlayerSingleton.checkPlayEndAndRePlay()

                    true
                }
                R.id.LoopMode_ALL -> {
                    chooseLoopMode("ALL")

                    true
                }
                R.id.LoopMode_OFF -> {
                    chooseLoopMode("OFF")

                    true
                }
                else -> true
            }
        }
        popup.show()
    }
    private fun chooseLoopMode(loopMode: String){
        ToolVibrate().vibrate(requireContext())
        //设置循环模式
        ListManagerHelper.setLoopMode(when (loopMode) {
            "ONE" -> "ONE"
            "ALL" -> "ALL"
            "OFF" -> "OFF"
            else -> "OFF"
        },requireContext())

        //刷新显示文本
        updateLoopModeText()
    }
    private fun updateLoopModeText(){
        val ButtonTextLoopMode = view?.findViewById<TextView>(R.id.ButtonTextLoopMode)
        ButtonTextLoopMode?.text = when (ListManagerHelper.getLoopMode(requireContext())) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知"
        }
    }
    //倍速管理选单
    private fun choosePlaySpeed(anchor: CardView){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_player_popup_video_speed, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.MenuAction_0_5 -> {
                    choosePlaySpeed(0.5f); true
                }
                R.id.MenuAction_1_0 -> {
                    choosePlaySpeed(1.0f); true
                }
                R.id.MenuAction_1_5 -> {
                    choosePlaySpeed(1.5f); true
                }
                R.id.MenuAction_2_0 -> {
                    choosePlaySpeed(2.0f); true
                }
                R.id.MenuAction_Input -> {
                    setSpeedByInput(); true
                }
                else -> true
            }
        }
        popup.show()
    }
    private fun choosePlaySpeed(speed: Float){
        ToolVibrate().vibrate(requireContext())
        //设置倍速
        PlayerSingleton.setPlaySpeed(speed)

        //刷新显示文本
        updatePlaySpeedText()
    }
    private fun setSpeedByInput(){
        val dialog = Dialog(requireContext()).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)
        //修改提示文本
        title.text = "自定义倍速"
        Description.text = "输入您的自定义倍速,最大允许数值为5.0"
        EditText.hint = ""
        Button.text = "确定"
        //设置点击事件
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val userInput = EditText.text.toString()
            if (userInput.isEmpty()){
                requireContext().showCustomToast("未输入内容", 3)
            }
            else {
                val inputValue = userInput.toFloat()
                if(inputValue > 0.0 && inputValue <= 5.0){
                    //向播放器发起设置倍速
                    PlayerSingleton.setPlaySpeed(inputValue)

                    //刷新显示文本
                    updatePlaySpeedText()

                    requireContext().showCustomToast("已将倍速设置为$inputValue", 3)
                }
                else {
                    requireContext().showCustomToast("不允许该值", 3)
                }
            }
            dialog.dismiss()
        }
        dialog.show()
        //自动弹出键盘
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun updatePlaySpeedText(){
        val ButtonTextPlaySpeed = view?.findViewById<TextView>(R.id.ButtonTextPlaySpeed)

        val (_, originalSpeed) = PlayerSingleton.getPlaySpeed()

        ButtonTextPlaySpeed?.text = "${originalSpeed}X"

    }
    //自动关闭倒计时选单
    private fun chooseAutoShut(anchor: CardView){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_player_popup_timer_shut_down, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.MenuAction_0 -> {
                    chooseCountDownDuration(0); true
                }
                R.id.MenuAction_15 -> {
                    chooseCountDownDuration(15); true
                }
                R.id.MenuAction_30 -> {
                    chooseCountDownDuration(30); true
                }
                R.id.MenuAction_60 -> {
                    chooseCountDownDuration(60); true
                }
                R.id.MenuAction_90 -> {
                    chooseCountDownDuration(90); true
                }
                R.id.MenuAction_Input -> {
                    setShutDownTimeByInput(); true
                }
                else -> true
            }
        }
        popup.show()
    }
    private fun chooseCountDownDuration(countDownDuration_Min: Int){
        ToolVibrate().vibrate(requireContext())
        //设置自动关闭倒计时
        PlayerSingleton.set_timer_autoShut(countDownDuration_Min)

        //刷新显示文本
        updateAutoShutText()
    }
    private fun updateAutoShutText(){
        val ButtonTextAutoShut = view?.findViewById<TextView>(R.id.ButtonTextAutoShut)

        val shutDownMoment = PlayerSingleton.get_timer_autoShut()

        if (shutDownMoment == ""){
            ButtonTextAutoShut?.text = "未设置"
        }else if(shutDownMoment == "shutdown_when_end"){
            ButtonTextAutoShut?.text = "本次播放结束后关闭"
        }else{
            ButtonTextAutoShut?.text = "将在${shutDownMoment}关闭"
        }
    }
    private fun setShutDownTimeByInput(){
        val dialog = Dialog(requireContext()).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_time, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditTextHour: EditText = dialogView.findViewById(R.id.dialog_input_hour)
        val EditTextMinute: EditText = dialogView.findViewById(R.id.dialog_input_minute)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)
        //修改提示文本
        title.text = "定时关闭"
        Description.text = "设置您期望的倒计时时长"
        EditTextHour.hint = ""
        EditTextMinute.hint = ""
        Button.text = "确定"
        //设置点击事件
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val hourInput = EditTextHour.text.toString().toIntOrNull()
            val minuteInput = EditTextMinute.text.toString().toIntOrNull()
            var hour: Int
            var minute: Int
            //提取小时
            if (hourInput == null || hourInput == 0 ){
                hour = 0
            }else{
                hour = hourInput
            }
            //提取分钟
            if (minuteInput == null || minuteInput == 0 ){
                minute = 0
            }else{
                minute = minuteInput
            }
            //不合规检查
            if (hourInput == null && minuteInput == null){
                requireContext().showCustomToast("未输入内容", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            if (hour == 0 && minute == 0){
                requireContext().showCustomToast("即将关闭", 3)
                lifecycleScope.launch {
                    delay(2000)
                    //关闭播放器
                    PlayerSingleton.pausePlay()
                    //发回信息让播放页关闭
                    returnFragment(FragmentConnector.fragment_more_button_exit_right_now)
                }
                return@setOnClickListener
            }
            //输入数值合规：转为分钟传入
            val totalMinutes = hour * 60 + minute
            //设置自动关闭倒计时
            PlayerSingleton.set_timer_autoShut(totalMinutes)
            //刷新显示文本
            updateAutoShutText()

            //关闭对话框
            dialog.dismiss()
        }
        dialog.show()
        //自动弹出键盘
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditTextHour.requestFocus()
            imm.showSoftInput(EditTextHour, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    //Tool Functions
    //设置面板高度
    private fun display(view: View){
        //获取当前屏幕方向
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //操作主卡片视图
        val mainCard = view.findViewById<CardView>(R.id.main_card)
        //读取屏幕信息
        val screenHeightPx = resources.displayMetrics.heightPixels
        val screenWidthPx = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density

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


}