package com.suming.player

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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
import data.DataBaseMediaItem.MediaItemRepo
import data.DataBaseMediaItem.MediaItemSetting
import kotlinx.coroutines.launch
import kotlin.math.abs

@UnstableApi
class PlayerFragmentMoreButton: DialogFragment() {

    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //设置
    private lateinit var PREFS: SharedPreferences
    //开关初始化
    private lateinit var Switch_BackgroundPlay: SwitchCompat
    private lateinit var Switch_SealOEL: SwitchCompat
    private lateinit var Switch_OnlyAudio: SwitchCompat
    private lateinit var Switch_OnlyVideo: SwitchCompat
    private lateinit var Switch_ExitWhenMediaEnd: SwitchCompat
    private lateinit var Switch_SavePositionWhenExit: SwitchCompat
    //自动关闭标志位
    private var lockPage = false

    companion object { fun newInstance(): PlayerFragmentMoreButton = PlayerFragmentMoreButton().apply { arguments = bundleOf(  ) } }

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
        PREFS = requireContext().getSharedPreferences("PREFS", Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_player_fragment_more_button, container, false)

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //开关置位
        Switch_BackgroundPlay = view.findViewById(R.id.Switch_BackgroundPlay)
        Switch_SealOEL = view.findViewById(R.id.Switch_SealOEL)
        Switch_OnlyAudio = view.findViewById(R.id.Switch_OnlyAudio)
        Switch_OnlyVideo = view.findViewById(R.id.Switch_OnlyVideo)
        Switch_ExitWhenMediaEnd = view.findViewById(R.id.Switch_ExitWhenMediaEnd)
        Switch_SavePositionWhenExit = view.findViewById(R.id.Switch_SavePositionWhenExit)
        Switch_BackgroundPlay.isChecked = vm.PREFS_BackgroundPlay
        Switch_SealOEL.isChecked = vm.PREFS_SealOEL
        Switch_OnlyAudio.isChecked = vm.PREFS_OnlyAudio
        Switch_OnlyVideo.isChecked = vm.PREFS_OnlyVideo

        Switch_SavePositionWhenExit.isChecked = vm.PREFS_SavePositionWhenExit


        //播放倍速
        val currentSpeed: Float = vm.player.playbackParameters.speed
        val currentSpeedText = view.findViewById<TextView>(R.id.current_speed)
        currentSpeedText.text = currentSpeed.toString()
        //定时关闭
        setShutDownTimeText()
        val PREFS_ShutDownWhenMediaEnd = PlayerSingleton.getPREFS_ShutDownWhenMediaEnd()
        Switch_ExitWhenMediaEnd.isChecked = PREFS_ShutDownWhenMediaEnd
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
        //按钮：更改倍速
        val ButtonChangeSpeed = view.findViewById<TextView>(R.id.buttonChangeSpeed)
        ButtonChangeSpeed.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            val popup = PopupMenu(requireContext(), ButtonChangeSpeed)
            popup.menuInflater.inflate(R.menu.activity_player_popup_video_speed, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.MenuAction_0_5 -> {
                        chooseSpeed(0.5f); true
                    }

                    R.id.MenuAction_1_0 -> {
                        chooseSpeed(1.0f); true
                    }

                    R.id.MenuAction_1_5 -> {
                        chooseSpeed(1.5f); true
                    }

                    R.id.MenuAction_2_0 -> {
                        chooseSpeed(2.0f); true
                    }

                    R.id.MenuAction_Input -> {
                        setSpeed(); Dismiss(); true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        //开关：后台播放
        Switch_BackgroundPlay.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())

            vm.PREFS_BackgroundPlay = isChecked

            val result = bundleOf("KEY" to "BackgroundPlay")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：关闭方向监听器(OEL：OrientationEventListener)
        Switch_SealOEL.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())
            vm.PREFS_SealOEL = isChecked
            if (isChecked) {
                context?.showCustomToast("短按或长按横屏按钮可切换至不同方向", Toast.LENGTH_SHORT,3)
            }

            val result = bundleOf("KEY" to "SealOEL")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
            customDismiss()
        }
        //开关：仅播放音频
        Switch_OnlyAudio.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())

            vm.PREFS_OnlyAudio = isChecked
            if (isChecked) {
                vm.PREFS_OnlyVideo = false
                Switch_OnlyVideo.isChecked = false
            }

            val result = bundleOf("KEY" to "SoundOnly")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：仅播放视频
        Switch_OnlyVideo.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())

            vm.PREFS_OnlyVideo = isChecked
            if (isChecked) {
                vm.PREFS_OnlyAudio = false
                Switch_OnlyAudio.isChecked = false
            }

            val result = bundleOf("KEY" to "VideoOnly")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：保存播放进度
        Switch_SavePositionWhenExit.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())
            vm.PREFS_SavePositionWhenExit = isChecked

            val result = bundleOf("KEY" to "SavePositionWhenExit")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：播放结束时关闭
        Switch_ExitWhenMediaEnd.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.setPREFS_ShutDownWhenMediaEnd(isChecked)
        }
        //定时关闭
        val ButtonTimerShutDown = view.findViewById<TextView>(R.id.ButtonTimerShutDown)
        ButtonTimerShutDown.setOnClickListener { _ ->
            ToolVibrate().vibrate(requireContext())
            val popup = PopupMenu(requireContext(), ButtonTimerShutDown)
            popup.menuInflater.inflate(R.menu.activity_player_popup_timer_shut_down, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.MenuAction_1 -> {
                        chooseCountDownDuration(1); true
                    }

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
                        setShutDownTime(); Dismiss(); true
                    }

                    else -> true
                }
            }
            popup.show()
        }
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

            /*
            val result = bundleOf("KEY" to "ExtractFrame")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            Dismiss()

             */
        }
        //使用传统进度条页面时隐藏滚动条控制卡片
        val CardScrollerStuff = view.findViewById<CardView>(R.id.card_scrollerStuff)
        if(vm.state_playerWithSeekBar){
            CardScrollerStuff.visibility = View.GONE
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

        //按钮：AlwaysSeek
        val ButtonAlwaysSeek = view.findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val ButtonAlwaysMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (vm.PREFS_AlwaysSeek) {
            ButtonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            ButtonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
        }
        ButtonAlwaysSeek.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //先给按钮改颜色
            if (vm.PREFS_AlwaysSeek) {
                ButtonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
            } else {
                ButtonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBg
                    )
                )
            }

            val result = bundleOf("KEY" to "AlwaysSeek")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //按钮：单击跳转
        val ButtonTap = view.findViewById<FrameLayout>(R.id.buttonActualTap)
        val ButtonTapMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (vm.PREFS_TapJump) {
            ButtonTapMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            ButtonTapMaterial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.ButtonBgClosed
                )
            )
        }
        ButtonTap.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //先给按钮改颜色
            if (vm.PREFS_TapJump) {
                ButtonTapMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
            } else {
                ButtonTapMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBg
                    )
                )
            }

            val result = bundleOf("KEY" to "TapJump")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //按钮：链接滚动条与视频进度
        val ButtonLink = view.findViewById<FrameLayout>(R.id.buttonActualLink)
        val ButtonLinkMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (vm.PREFS_LinkScroll) {
            ButtonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            ButtonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                    requireContext(),
                    R.color.ButtonBgClosed
                ))
        }
        ButtonLink.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //先给按钮改颜色
            if (vm.PREFS_LinkScroll) { ButtonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )) }
            else { ButtonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBg
                    )) }

            val result = bundleOf("KEY" to "LinkScroll")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
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



        //面板下滑关闭(NestedScrollView)
        if (!vm.PREFS_CloseFragmentGesture){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                var deltaY_ReachPadding = false
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
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
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
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
    //设置倍速
    private fun chooseSpeed(speed: Float){
        ToolVibrate().vibrate(requireContext())

        vm.PREFS_PlaySpeed = speed
        vm.player.setPlaybackSpeed(speed)
        //刷新文字
        val currentSpeedText = view?.findViewById<TextView>(R.id.current_speed)
        currentSpeedText?.text = speed.toString()
        //存表
        lifecycleScope.launch {
            val newSetting = MediaItemSetting(MARK_FileName = vm.MediaInfo_FileName, PREFS_PlaySpeed = speed)
            MediaItemRepo.get(requireContext()).saveSetting(newSetting)
        }

        customDismiss()
    }
    private fun setSpeed(){

        val result = bundleOf("KEY" to "SetSpeed")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

        Dismiss()
    }
    //设置自动关闭倒计时
    @SuppressLint("SetTextI18n")
    private fun chooseCountDownDuration(countDownDuration_Min: Int){
        ToolVibrate().vibrate(requireContext())

        PlayerSingleton.setCountDownTimer(countDownDuration_Min)

        setShutDownTimeText()

        customDismiss()
    }
    @SuppressLint("SetTextI18n")
    private fun setShutDownTimeText(){
        val timerShutDown = view?.findViewById<TextView>(R.id.StateTimerShutDown)
        val shutDownMoment = PlayerSingleton.getShutDownMoment()
        if (shutDownMoment == ""){
            timerShutDown?.text = "未设置"
        }
        else if (shutDownMoment == "shutdown_when_end"){
            timerShutDown?.text = "本次播放结束后关闭"
        }
        else{
            timerShutDown?.text = "将在${shutDownMoment}关闭"
        }
    } //更改显示文本
    private fun setShutDownTime(){

        val result = bundleOf("KEY" to "setShutDownTime")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

    }
    //自定义退出逻辑
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