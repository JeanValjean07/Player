package com.suming.player

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.suming.player.PlayerExoSingleton.player
import data.MediaItemRepo
import data.MediaItemSetting
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@UnstableApi
class PlayerFragmentMoreButton: DialogFragment() {

    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //开关初始化
    private lateinit var Switch_BackgroundPlay: SwitchCompat
    private lateinit var Switch_LoopPlay: SwitchCompat
    private lateinit var Switch_SealOEL: SwitchCompat
    private lateinit var Switch_OnlyAudio: SwitchCompat
    private lateinit var Switch_OnlyVideo: SwitchCompat
    private lateinit var Switch_ExitWhenMediaEnd: SwitchCompat
    private lateinit var Switch_SavePositionWhenExit: SwitchCompat
    //自动关闭标志位
    private var lockPage = false


    //companion object
    companion object { fun newInstance(): PlayerFragmentMoreButton = PlayerFragmentMoreButton().apply { arguments = bundleOf(  ) } }


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

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //信息预写
        //1.倍速预写
        val currentSpeed: Float = player.playbackParameters.speed
        val currentSpeedText = view.findViewById<TextView>(R.id.current_speed)
        currentSpeedText.text = currentSpeed.toString()
        val timerShutDown = view.findViewById<TextView>(R.id.StateTimerShutDown)
        if (vm.PREFS_TimerShutDown) {
            timerShutDown.text = "将在${vm.shutDownTime}关闭"
        } else {
            timerShutDown.text = "未开启"
        }

        //开关置位
        Switch_BackgroundPlay = view.findViewById(R.id.Switch_BackgroundPlay)
        Switch_LoopPlay = view.findViewById(R.id.Switch_LoopPlay)
        Switch_SealOEL = view.findViewById(R.id.Switch_SealOEL)
        Switch_OnlyAudio = view.findViewById(R.id.Switch_OnlyAudio)
        Switch_OnlyVideo = view.findViewById(R.id.Switch_OnlyVideo)
        Switch_ExitWhenMediaEnd = view.findViewById(R.id.Switch_ExitWhenMediaEnd)

        Switch_BackgroundPlay.isChecked = vm.PREFS_BackgroundPlay
        Switch_LoopPlay.isChecked = vm.PREFS_LoopPlay
        Switch_SealOEL.isChecked = vm.PREFS_SealOEL
        Switch_OnlyAudio.isChecked = vm.PREFS_OnlyAudio
        Switch_OnlyVideo.isChecked = vm.PREFS_OnlyVideo
        Switch_ExitWhenMediaEnd.isChecked = vm.PREFS_ShutDownWhenMediaEnd

        //保存进度仅在数据库启用时开启
        val ContainerSavePosition = view.findViewById<LinearLayout>(R.id.ContainerSavePosition)
        if (!vm.PREFS_EnableRoomDatabase) {
            ContainerSavePosition.visibility = View.GONE
        } else {
            Switch_SavePositionWhenExit = view.findViewById(R.id.Switch_SavePositionWhenExit)
            Switch_SavePositionWhenExit.isChecked = vm.PREFS_SavePositionWhenExit
            //开关：退出时保存进度
            Switch_SavePositionWhenExit.setOnCheckedChangeListener { _, isChecked ->
                vm.PREFS_SavePositionWhenExit = isChecked

                val result = bundleOf("KEY" to "SavePosition")
                setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

                customDismiss()
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
            vm.PREFS_BackgroundPlay = isChecked

            val result = bundleOf("KEY" to "BackgroundPlay")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：循环播放
        Switch_LoopPlay.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_LoopPlay = isChecked

            val result = bundleOf("KEY" to "LoopPlay")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：关闭方向监听器(OEL：OrientationEventListener)
        Switch_SealOEL.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_SealOEL = isChecked

            val result = bundleOf("KEY" to "SealOEL")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：仅播放音频
        Switch_OnlyAudio.setOnCheckedChangeListener { _, isChecked ->
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
            vm.PREFS_OnlyVideo = isChecked
            if (isChecked) {
                vm.PREFS_OnlyAudio = false
                Switch_OnlyAudio.isChecked = false
            }

            val result = bundleOf("KEY" to "VideoOnly")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }
        //开关：播放结束时关闭
        Switch_ExitWhenMediaEnd.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_ShutDownWhenMediaEnd = isChecked
        }
        //定时关闭
        val ButtonTimerShutDown = view.findViewById<TextView>(R.id.ButtonTimerShutDown)
        ButtonTimerShutDown.setOnClickListener { item ->
            val popup = PopupMenu(requireContext(), ButtonTimerShutDown)
            popup.menuInflater.inflate(R.menu.activity_player_popup_timer_shut_down, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.MenuAction_1 -> {
                        chooseShutDownTime(1); true
                    }

                    R.id.MenuAction_15 -> {
                        chooseShutDownTime(15); true
                    }

                    R.id.MenuAction_30 -> {
                        chooseShutDownTime(30); true
                    }

                    R.id.MenuAction_60 -> {
                        chooseShutDownTime(60); true
                    }

                    R.id.MenuAction_90 -> {
                        chooseShutDownTime(90); true
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
            val result = bundleOf("KEY" to "UpdateCover")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            Dismiss()
        }
        //按钮：提取帧
        val ButtonExtractFrame = view.findViewById<ImageButton>(R.id.buttonExtractFrame)
        ButtonExtractFrame.setOnClickListener {
            context?.showCustomToast("暂不开放此功能", Toast.LENGTH_SHORT, 3)

            /*
            val result = bundleOf("KEY" to "ExtractFrame")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            Dismiss()

             */
        }


        //按钮：AlwaysSeek
        val buttonAlwaysSeek = view.findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val buttonAlwaysMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (vm.PREFS_AlwaysSeek) {
            buttonAlwaysMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonAlwaysMaterial.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
        }
        buttonAlwaysSeek.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_AlwaysSeek) {
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
            } else {
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(
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
        val buttonTap = view.findViewById<FrameLayout>(R.id.buttonActualTap)
        val buttonTapMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (vm.PREFS_TapJump) {
            buttonTapMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.ButtonBgClosed
                )
            )
        }
        buttonTap.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_TapJump) {
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
            } else {
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(
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
        val buttonLink = view.findViewById<FrameLayout>(R.id.buttonActualLink)
        val buttonLinkMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (vm.PREFS_LinkScroll) {
            buttonLinkMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.ButtonBgClosed
                )
            )
        }
        buttonLink.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_LinkScroll) {
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBgClosed
                    )
                )
            } else {
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.ButtonBg
                    )
                )
            }

            val result = bundleOf("KEY" to "LinkScroll")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            customDismiss()
        }

        //视频信息
        val ButtonVideoInfo = view.findViewById<TextView>(R.id.buttonVideoInfo)
        ButtonVideoInfo.setOnClickListener {
            val result = bundleOf("KEY" to "VideoInfo")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            dismiss()
        }
        //分享
        val ButtonSysShare = view.findViewById<TextView>(R.id.buttonSysShare)
        ButtonSysShare.setOnClickListener {
            val result = bundleOf("KEY" to "SysShare")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            Dismiss()
        }
        //均衡器
        val ButtonEqualizer = view.findViewById<TextView>(R.id.buttonEqualizer)
        ButtonEqualizer.setOnClickListener {
            context?.showCustomToast("暂不开放此功能", Toast.LENGTH_SHORT, 3)

            /*
            val result = bundleOf("KEY" to "Equalizer")
            setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

            Dismiss()

             */
        }


        //面板下滑关闭(NestedScrollView)
        if (!vm.PREFS_CloseFragmentGesture){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginY = RootCard.translationY
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                var NestedScrollViewAtTop = true
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
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
                            RootCard.translationY = RootCardOriginY + deltaY
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (deltaY >= 400f){
                                Dismiss()
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
                        }
                        MotionEvent.ACTION_MOVE -> {
                            deltaY = event.rawY - down_y
                            deltaX = event.rawX - down_x
                            Log.d("SuMing", "deltaY: $deltaY, deltaX: $deltaX")
                            if (deltaX < 0){
                                return@setOnTouchListener false
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
                                Dismiss()
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
                Dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

    } //onViewCreated END




    //Functions
    //设置倍速
    private fun chooseSpeed(speed: Float){
        vm.PREFS_PlaySpeed = speed
        vm.player.setPlaybackSpeed(speed)
        //刷新文字
        val currentSpeedText = view?.findViewById<TextView>(R.id.current_speed)
        currentSpeedText?.text = speed.toString()
        //存表
        lifecycleScope.launch {
            val newSetting = MediaItemSetting(MARK_FileName = vm.fileName, PREFS_PlaySpeed = speed)
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
    private fun chooseShutDownTime(time: Int){

        val result = bundleOf("KEY" to "chooseShutDownTime", "TIME" to time)
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)

        //计算关闭时间
        val nowDateTime: String = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val nowMillis = System.currentTimeMillis()
        val shutDownMillis = nowMillis + (time * 60_000L)  //分钟转毫秒
        //val pattern = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val pattern = java.text.SimpleDateFormat("HH时mm分ss秒", java.util.Locale.getDefault())
        val shutDownTime = pattern.format(java.util.Date(shutDownMillis))
        //更改ViewModel标志位状态
        vm.shutDownTime = shutDownTime
        vm.PREFS_TimerShutDown = true

        //更改显示文本
        val timerShutDown = view?.findViewById<TextView>(R.id.StateTimerShutDown)
        timerShutDown?.text = "将在${shutDownTime}关闭"

        customDismiss()
    }
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
    private fun Dismiss(){
        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()

    }

}