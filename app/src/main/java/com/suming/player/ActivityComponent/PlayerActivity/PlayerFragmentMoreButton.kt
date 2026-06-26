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
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
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
import com.suming.player.FuncPack_ListManager.PlayerListManager
import com.suming.player.PlayerSingleton
import com.suming.player.R
import com.suming.player.SettingsRequestCenter
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaDataBaseMaster
import com.suming.player.ViewWidget.CircleButton
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
        fun newInstance(): PlayerFragmentMoreButton = PlayerFragmentMoreButton().apply { arguments =
            bundleOf()
        }
    }

    //连接到共享ViewModel
    private val viewModel: PlayerViewModel by activityViewModels()



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
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_more_button, container, false)

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化
        init(view)

        //注册控件
        register(view)

    }

    override fun onResume() {
        super.onResume()

        //发布开启事件
        returnFragment(FragmentConnector.fragment_event_open)
    }
    override fun onDestroy() {
        super.onDestroy()
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
                updateCoverFrame(ButtonUpdateCover)
            }
            //提取所有帧
            val ButtonExtractFrame = view.findViewById<ImageButton>(R.id.buttonExtractFrame)
            ButtonExtractFrame.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                requireContext().showCustomToast("暂不开放此功能,实际上,以后更是打算取消", 3)
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
                returnFragment(FragmentConnector.fragment_more_button_clear_miniature)
                dismiss()
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

            //注册进度条相关功能(经典播放页时不显示进度条相关功能)
            val CardScrollerStuff = view.findViewById<CardView>(R.id.card_scrollerStuff)
            if(viewModel.state_player_type == "Oro"){
                CardScrollerStuff.visibility = View.GONE
            }else if(viewModel.state_player_type == "Neo"){
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
    private fun chooseLoopMode(loopMode: String){
        ToolVibrate().vibrate(requireContext())
        //设置循环模式
        PlayerListManager.setLoopMode(when (loopMode) {
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
        ButtonTextLoopMode?.text = when (PlayerListManager.getLoopMode(requireContext())) {
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_player_dialog_input_value, null)
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_player_dialog_input_time, null)
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
    fun getStatusBarHeightFromView(view: View): Int {
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            dismiss()
        }
    }


}