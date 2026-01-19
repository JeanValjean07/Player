package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.KeyEvent
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.google.android.material.button.MaterialButton
import com.suming.player.ListManager.PlayerListManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@UnstableApi
@SuppressLint("UseGetLayoutInflater", "InflateParams","SetTextI18n")
@RequiresApi(Build.VERSION_CODES.Q)
class PlayerFragmentMoreButton: DialogFragment() {
    companion object {
        fun newInstance(): PlayerFragmentMoreButton = PlayerFragmentMoreButton().apply { arguments = bundleOf(  ) }
    }
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //协程
    private var coroutine_registerSwitch = CoroutineScope(Dispatchers.Main)
    private var coroutine_registerMenuButton = CoroutineScope(Dispatchers.Main)
    private var coroutine_registerBasicButton = CoroutineScope(Dispatchers.Main)
    private var coroutine_registerFunctionalButtonTop = CoroutineScope(Dispatchers.Main)
    private var coroutine_registerFunctionalButton = CoroutineScope(Dispatchers.Main)
    private var coroutine_registerSeekBarStuff = CoroutineScope(Dispatchers.Main)





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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_more_button, container, false)

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //设置卡片高度
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            val MainCard = view.findViewById<CardView>(R.id.main_card)
            MainCard.layoutParams.height = (resources.displayMetrics.heightPixels * 0.7).toInt()
        }




        //注册基础按钮
        coroutine_registerBasicButton.launch {
            //按钮：退出
            val ButtonExit = view.findViewById<ImageButton>(R.id.buttonExit)
            ButtonExit.setOnClickListener {
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
            //点击顶部区域回顶
            val TopBarArea = view.findViewById<View>(R.id.TopBarArea)
            TopBarArea.setOnClickListener {
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                if (NestedScrollView.scrollY == 0) { return@setOnClickListener }
                ToolVibrate().vibrate(requireContext())
                //回到顶部
                NestedScrollView.smoothScrollTo(0, 0)
            }
            //面板下滑关闭注册
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
                                    Dismiss(false)
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
                                    Dismiss(false)
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
        }

        //注册开关
        coroutine_registerSwitch.launch {
            //开启方向监听器
            val switch_EnableOriListener = view.findViewById<SwitchCompat>(R.id.EnableOriListener)
            switch_EnableOriListener.isChecked = SettingsRequestCenter.get_PREFS_EnableOrientationListener(requireContext())
            switch_EnableOriListener.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //修改设置
                val isChecked = switch_EnableOriListener.isChecked
                SettingsRequestCenter.set_PREFS_EnableOrientationListener(isChecked)
                //发回结果
                val result = bundleOf("KEY" to "EnableOriListener","target" to isChecked)
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                customDismiss()
            }
            //后台播放
            val switch_BackgroundPlay = view.findViewById<SwitchCompat>(R.id.Switch_BackgroundPlay)
            switch_BackgroundPlay.isChecked = SettingsRequestCenter.get_PREFS_BackgroundPlay(requireContext())
            switch_BackgroundPlay.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //修改设置
                val isChecked = switch_BackgroundPlay.isChecked
                SettingsRequestCenter.set_PREFS_BackgroundPlay(isChecked)
                //发回结果
                val result = bundleOf("KEY" to "BackgroundPlay","target" to isChecked)
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                customDismiss()
            }
            //仅在播放完成后退出
            val switch_ExitWhenMediaEnd = view.findViewById<SwitchCompat>(R.id.Switch_ExitWhenMediaEnd)
            switch_ExitWhenMediaEnd.isChecked = SettingsRequestCenter.get_PREFS_OnlyStopUnMediaEnd(requireContext())
            switch_ExitWhenMediaEnd.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //修改设置
                val isChecked = switch_ExitWhenMediaEnd.isChecked
                SettingsRequestCenter.set_PREFS_OnlyStopUnMediaEnd(isChecked)
                //发回结果
                val result = bundleOf("KEY" to "ExitWhenMediaEnd","target" to isChecked)
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                customDismiss()
            }
            //保存播放进度
            val switch_saveLastPosition = view.findViewById<SwitchCompat>(R.id.Switch_SavePositionWhenExit)
            switch_saveLastPosition.isChecked = PlayerSingleton.get_Para_saveLastProgress()
            switch_saveLastPosition.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //修改设置
                val isChecked = switch_saveLastPosition.isChecked
                PlayerSingleton.set_Para_saveLastProgress(isChecked)

                //不发回结果
                customDismiss()
            }
        }

        //注册选单按钮
        coroutine_registerMenuButton.launch {
            //循环模式
            updateLoopModeText(view)
            val ButtonCardLoopMode = view.findViewById<CardView>(R.id.ButtonCardLoopMode)
            ButtonCardLoopMode.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val popup = PopupMenu(requireContext(), ButtonCardLoopMode)
                popup.menuInflater.inflate(R.menu.activity_player_popup_loop_mode, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.LoopMode_ONE -> {
                            chooseLoopMode("ONE",view); true
                        }
                        R.id.LoopMode_ALL -> {
                            chooseLoopMode("ALL",view); true
                        }
                        R.id.LoopMode_OFF -> {
                            chooseLoopMode("OFF",view); true
                        }
                        else -> true
                    }
                }
                popup.show()
            }
            //倍速管理
            updatePlaySpeedText(view)
            val ButtonCardPlaySpeed = view.findViewById<CardView>(R.id.ButtonCardPlaySpeed)
            ButtonCardPlaySpeed.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val popup = PopupMenu(requireContext(), ButtonCardPlaySpeed)
                popup.menuInflater.inflate(R.menu.activity_player_popup_video_speed, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.MenuAction_0_5 -> {
                            choosePresetSpeed(0.5f,view); true
                        }
                        R.id.MenuAction_1_0 -> {
                            choosePresetSpeed(1.0f,view); true
                        }
                        R.id.MenuAction_1_5 -> {
                            choosePresetSpeed(1.5f,view); true
                        }
                        R.id.MenuAction_2_0 -> {
                            choosePresetSpeed(2.0f,view); true
                        }
                        R.id.MenuAction_Input -> {
                            setSpeedByInput(); true
                        }
                        else -> true
                    }
                }
                popup.show()
            }
            //定时关闭
            updateAutoShutText(view)
            val ButtonCardAutoShut = view.findViewById<CardView>(R.id.ButtonCardAutoShut)
            ButtonCardAutoShut.setOnClickListener { _ ->
                ToolVibrate().vibrate(requireContext())
                val popup = PopupMenu(requireContext(), ButtonCardAutoShut)
                popup.menuInflater.inflate(R.menu.activity_player_popup_timer_shut_down, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.MenuAction_0 -> {
                            chooseCountDownDuration(0,view); true
                        }
                        R.id.MenuAction_15 -> {
                            chooseCountDownDuration(15,view); true
                        }
                        R.id.MenuAction_30 -> {
                            chooseCountDownDuration(30,view); true
                        }
                        R.id.MenuAction_60 -> {
                            chooseCountDownDuration(60,view); true
                        }
                        R.id.MenuAction_90 -> {
                            chooseCountDownDuration(90,view); true
                        }
                        R.id.MenuAction_Input -> {
                            setShutDownTimeByInput(); true
                        }
                        else -> true
                    }
                }
                popup.show()
            }

        }

        //注册顶部功能键
        coroutine_registerFunctionalButtonTop.launch {
            //截屏
            val ButtonCapture = view.findViewById<ImageButton>(R.id.buttonCapture)
            ButtonCapture.setOnClickListener {
                val result = bundleOf("KEY" to "Capture")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                Dismiss()
            }
            //按钮：播放列表
            val ButtonPlayList = view.findViewById<ImageButton>(R.id.ButtonPlayList)
            if (vm.state_FromSysStart) {
                ButtonPlayList.visibility = View.GONE
            }
            ButtonPlayList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val result = bundleOf("KEY" to "PlayList")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                dismiss()
            }
            //按钮：回到开头
            val ButtonBackToStart = view.findViewById<ImageButton>(R.id.buttonBackToStart)
            ButtonBackToStart.setOnClickListener {
                val result = bundleOf("KEY" to "BackToStart")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                Dismiss()
            }

        }

        //注册功能键
        coroutine_registerFunctionalButton.launch {






            //开启小窗
            val ButtonStartPiP = view.findViewById<TextView>(R.id.ButtonStartPiP)
            ButtonStartPiP.setOnClickListener {
                val result = bundleOf("KEY" to "StartPiP")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()
            }
            //按钮：更新封面
            val ButtonUpdateCover = view.findViewById<TextView>(R.id.buttonUpdateCover)
            ButtonUpdateCover.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val popup = PopupMenu(requireContext(), ButtonUpdateCover)
                popup.menuInflater.inflate(R.menu.activity_player_popup_change_cover, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.item_useCurrentFrame -> {
                            ToolVibrate().vibrate(requireContext())

                            val result = bundleOf("KEY" to "updateCoverFrame", "Method" to "useCurrentFrame")
                            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                            Dismiss();true
                        }

                        R.id.item_useDefaultCover -> {
                            ToolVibrate().vibrate(requireContext())

                            val result = bundleOf("KEY" to "updateCoverFrame", "Method" to "useDefaultCover")
                            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                            customDismiss();true
                        }

                        R.id.item_pickFromLocal -> {
                            ToolVibrate().vibrate(requireContext())

                            val result = bundleOf("KEY" to "updateCoverFrame", "Method" to "pickFromLocal")
                            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                            customDismiss();true
                        }

                        else -> true
                    }
                }
                popup.show()
            }
            //按钮：提取帧
            val ButtonExtractFrame = view.findViewById<ImageButton>(R.id.buttonExtractFrame)
            ButtonExtractFrame.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                context?.showCustomToast("暂不开放此功能", Toast.LENGTH_SHORT, 3)
            }

            //视频信息
            val ButtonVideoInfo = view.findViewById<TextView>(R.id.buttonVideoInfo)
            ButtonVideoInfo.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                val result = bundleOf("KEY" to "VideoInfo")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                dismiss()
            }
            //分享
            val ButtonSysShare = view.findViewById<TextView>(R.id.buttonSysShare)
            ButtonSysShare.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                val result = bundleOf("KEY" to "SysShare")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()
            }
            //均衡器
            val ButtonEqualizer = view.findViewById<TextView>(R.id.buttonEqualizer)
            ButtonEqualizer.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                context?.showCustomToast("暂不开放此功能", Toast.LENGTH_SHORT, 3)

                /*
                val result = bundleOf("KEY" to "Equalizer")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()

                 */
            }
            //重新生成进度条缩略图
            val ButtonReCreateThumb = view.findViewById<TextView>(R.id.ButtonReCreateThumb)
            ButtonReCreateThumb.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                requireContext().showCustomToast("重新打开视频后生效", Toast.LENGTH_SHORT, 3)

                val result = bundleOf("KEY" to "clearMiniature")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()
            }
            //解除亮度控制卡片+按钮
            val CardBrightness = view.findViewById<LinearLayout>(R.id.ContainerUnBindBrightness)
            val ButtonUnBindBrightness = view.findViewById<TextView>(R.id.ButtonUnBindBrightness)
            if (!vm.BrightnessChanged){
                CardBrightness.visibility = View.GONE
            }
            ButtonUnBindBrightness.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                val result = bundleOf("KEY" to "UnBindBrightness")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                Dismiss()
            }
            //绑定播放视图
            val ButtonBindPlayView = view.findViewById<TextView>(R.id.ButtonBindPlayView)
            ButtonBindPlayView.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                val result = bundleOf("KEY" to "BindPlayView")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                Dismiss()
            }

        }

        //注册进度条相关功能
        coroutine_registerSeekBarStuff.launch {
            val CardScrollerStuff = view.findViewById<CardView>(R.id.card_scrollerStuff)
            //经典播放页
            if(vm.state_player_type == "Oro"){
                CardScrollerStuff.visibility = View.GONE
            }
            //新晋播放页
            else if (vm.state_player_type == "Neo"){
                //按钮：AlwaysSeek
                val ButtonAlwaysSeek = view.findViewById<FrameLayout>(R.id.ButtonActualAlwaysSeek)
                val ButtonAlwaysSeekMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialAlwaysSeek)
                fun updateButtonAlwaysSeekColor(){
                    if (vm.PREFS_AlwaysSeek) {
                        ButtonAlwaysSeekMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_ON))
                    }else{
                        ButtonAlwaysSeekMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_OFF))
                    }
                }
                updateButtonAlwaysSeekColor()
                ButtonAlwaysSeek.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !vm.PREFS_AlwaysSeek
                    vm.PREFS_AlwaysSeek = target
                    SettingsRequestCenter.set_PREFS_EnableAlwaysSeek(target)

                    //按钮改为目标颜色
                    updateButtonAlwaysSeekColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "AlwaysSeek","target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
                }

                //按钮：链接滚动条与视频进度
                val ButtonLinkScroll = view.findViewById<FrameLayout>(R.id.ButtonActualLinkScroll)
                val ButtonLinkScrollMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialLinkScroll)
                fun updateButtonLinkScrollColor(){
                    if (vm.PREFS_LinkScroll) {
                        ButtonLinkScrollMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_ON))
                    }else{
                        ButtonLinkScrollMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_OFF))
                    }
                }
                updateButtonLinkScrollColor()
                ButtonLinkScroll.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !vm.PREFS_LinkScroll
                    vm.PREFS_LinkScroll = target
                    SettingsRequestCenter.set_PREFS_EnableLinkScroll(target)

                    //按钮改为目标颜色
                    updateButtonLinkScrollColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "LinkScroll","target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
                }

                //按钮：单击跳转
                val ButtonTapJump = view.findViewById<FrameLayout>(R.id.ButtonActualTapJump)
                val ButtonTapJumpMaterial = view.findViewById<MaterialButton>(R.id.ButtonMaterialTapJump)
                fun updateButtonTapJumpColor(){
                    if (vm.PREFS_TapJump) {
                        ButtonTapJumpMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_ON))
                    }else{
                        ButtonTapJumpMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.Button_Switch_Background_OFF))
                    }
                }
                updateButtonTapJumpColor()
                ButtonTapJump.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //立即切换变量并写入数据库
                    val target = !vm.PREFS_TapJump
                    vm.PREFS_TapJump = target
                    SettingsRequestCenter.set_PREFS_EnableTapJump(target)

                    //按钮改为目标颜色
                    updateButtonTapJumpColor()

                    //发回并关闭
                    val result = bundleOf("KEY" to "TapJump","target" to target)
                    setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
                    customDismiss()
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




    //Functions
    //循环模式
    private fun chooseLoopMode(loopMode: String,view: View){
        ToolVibrate().vibrate(requireContext())
        //设置循环模式
        PlayerListManager.setLoopMode(when (loopMode) {
            "ONE" -> "ONE"
            "ALL" -> "ALL"
            "OFF" -> "OFF"
            else -> "OFF"
        },requireContext())

        //刷新显示文本
        updateLoopModeText(view)

        //不主动退出
    }
    private fun updateLoopModeText(view: View){
        val ButtonTextLoopMode = view.findViewById<TextView>(R.id.ButtonTextLoopMode)
        ButtonTextLoopMode.text = when (PlayerListManager.getLoopMode(requireContext())) {
            "ONE" -> "单集循环"
            "ALL" -> "列表循环"
            "OFF" -> "播完暂停"
            else -> "未知"
        }
    }
    //倍速
    private fun choosePresetSpeed(speed: Float,view: View){
        ToolVibrate().vibrate(requireContext())
        //设置倍速
        PlayerSingleton.setPlaySpeed(speed)

        //刷新显示文本
        updatePlaySpeedText(view)

        //不主动退出
    }
    private fun setSpeedByInput(){
        val dialog = Dialog(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description:TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)
        //修改提示文本
        title.text = "自定义倍速"
        Description.text = "输入您的自定义倍速,最大允许数值为5.0"
        EditText.hint = ""
        Button.text = "确定"
        //设置点击事件
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val userInput = EditText.text.toString()
            if (userInput.isEmpty()){
                requireContext().showCustomToast("未输入内容", Toast.LENGTH_SHORT, 3)
            }
            else {
                val inputValue = userInput.toFloat()
                if(inputValue > 0.0 && inputValue <= 5.0){
                    //向播放器发起设置倍速
                    PlayerSingleton.setPlaySpeed(inputValue)

                    //刷新显示文本
                    updatePlaySpeedText(dialogView)

                    requireContext().showCustomToast("已将倍速设置为$inputValue", Toast.LENGTH_SHORT, 3)
                }
                else {
                    requireContext().showCustomToast("不允许该值", Toast.LENGTH_SHORT, 3)
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
    private fun updatePlaySpeedText(view: View){
        val ButtonTextPlaySpeed = view.findViewById<TextView>(R.id.ButtonTextPlaySpeed)

        val (_, originalSpeed) = PlayerSingleton.getPlaySpeed()

        ButtonTextPlaySpeed.text = "${originalSpeed}X"

    }
    //自动关闭倒计时
    private fun chooseCountDownDuration(countDownDuration_Min: Int,view: View){
        ToolVibrate().vibrate(requireContext())
        //设置自动关闭倒计时
        PlayerSingleton.setCountDownTimer(countDownDuration_Min)

        //刷新显示文本
        updateAutoShutText(view)

        //不主动退出
    }
    private fun updateAutoShutText(view: View){
        val ButtonTextAutoShut = view.findViewById<TextView>(R.id.ButtonTextAutoShut)
        val shutDownMoment = PlayerSingleton.getShutDownMoment()
        if (shutDownMoment == ""){
            ButtonTextAutoShut.text = "未设置"
        }
        else if (shutDownMoment == "shutdown_when_end"){
            ButtonTextAutoShut.text = "本次播放结束后关闭"
        }
        else{
            ButtonTextAutoShut.text = "将在${shutDownMoment}关闭"
        }
    }
    private fun setShutDownTimeByInput(){
        val dialog = Dialog(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_player_dialog_input_time, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description:TextView = dialogView.findViewById(R.id.dialog_description)
        val EditTextHour: EditText = dialogView.findViewById(R.id.dialog_input_hour)
        val EditTextMinute: EditText = dialogView.findViewById(R.id.dialog_input_minute)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)
        //修改提示文本
        title.text = "自定义：定时关闭时间"
        Description.text = "请输入您的自定定时关闭时间"
        EditTextHour.hint = "              "
        EditTextMinute.hint = "              "
        Button.text = "确定"
        //设置点击事件
        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
                requireContext().showCustomToast("未输入内容", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            if (hour == 0 && minute == 0){
                requireContext().showCustomToast("您选择了立即关闭", Toast.LENGTH_SHORT, 3)
                lifecycleScope.launch {
                    delay(2000)
                    //关闭播放器
                    //PlayerSingleton.savePositionToRoom()
                    PlayerSingleton.onTaskRemoved()
                    PlayerSingleton.DevastatePlayBundle(requireContext())
                    //结束进程
                    val pid = Process.myPid()
                    Process.killProcess(pid)
                }
            }
            //输入数值合规：转为分钟传入
            val totalMinutes = hour * 60 + minute
            //设置自动关闭倒计时
            PlayerSingleton.setCountDownTimer(totalMinutes)
            //刷新显示文本
            updateAutoShutText(dialogView)

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
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            Dismiss()
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ ToolVibrate().vibrate(requireContext()) }
        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()

    }

}