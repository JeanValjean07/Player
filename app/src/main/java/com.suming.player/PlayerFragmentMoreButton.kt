package com.suming.player

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import com.google.android.material.button.MaterialButton
import com.suming.player.PlayerExoSingleton.player

@UnstableApi
class PlayerFragmentMoreButton: DialogFragment() {

    //共享ViewModel
    private val vm: PlayerExoViewModel by activityViewModels()
    //开关初始化
    private lateinit var Switch_BackgroundPlay: SwitchCompat
    private lateinit var Switch_LoopPlay: SwitchCompat
    private lateinit var Switch_SealOEL: SwitchCompat
    private lateinit var Switch_OnlyAudio: SwitchCompat
    private lateinit var Switch_OnlyVideo: SwitchCompat
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
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }
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
    ): View = inflater.inflate(R.layout.activity_player_fragment_more_button, container, false)

    @SuppressLint("UseGetLayoutInflater", "InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //信息预写
        //1.倍速预写
        val currentSpeed: Float = player.playbackParameters.speed
        val currentSpeedText = view.findViewById<TextView>(R.id.current_speed)
        currentSpeedText.text = currentSpeed.toString()
        //开关置位
        Switch_BackgroundPlay = view.findViewById(R.id.Switch_BackgroundPlay)
        Switch_LoopPlay = view.findViewById(R.id.Switch_LoopPlay)
        Switch_SealOEL = view.findViewById(R.id.Switch_SealOEL)
        Switch_OnlyAudio = view.findViewById(R.id.Switch_OnlyAudio)
        Switch_OnlyVideo = view.findViewById(R.id.Switch_OnlyVideo)

        Switch_BackgroundPlay.isChecked = vm.PREFS_BackgroundPlay
        Switch_LoopPlay.isChecked = vm.PREFS_LoopPlay
        Switch_SealOEL.isChecked = vm.PREFS_SealOEL
        Switch_OnlyAudio.isChecked = vm.PREFS_OnlyAudio
        Switch_OnlyVideo.isChecked = vm.PREFS_OnlyVideo



        //按钮：退出
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            dismiss()
        }
        //按钮：锁定页面
        val buttonLock = view.findViewById<ImageButton>(R.id.buttonLock)
        buttonLock.setOnClickListener {
            lockPage = !lockPage
            if (lockPage){
                buttonLock.setImageResource(R.drawable.ic_more_button_lock_on)
            }
            else{
                buttonLock.setImageResource(R.drawable.ic_more_button_lock_off)
            }
        }
        //按钮：更改倍速
        val buttonChangeSpeed = view.findViewById<TextView>(R.id.buttonChangeSpeed)
        buttonChangeSpeed.setOnClickListener {
            val popup = PopupMenu(requireContext(), buttonChangeSpeed)
            popup.menuInflater.inflate(R.menu.activity_player_popup_video_speed, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.MenuAction_0_5 -> { vm.player.setPlaybackSpeed(0.5f); customDismiss(); true }

                    R.id.MenuAction_1_0 -> { vm.player.setPlaybackSpeed(1.0f); customDismiss(); true }

                    R.id.MenuAction_1_5 -> { vm.player.setPlaybackSpeed(1.5f); customDismiss(); true }

                    R.id.MenuAction_2_0 -> { vm.player.setPlaybackSpeed(2.0f); customDismiss(); true }

                    R.id.MenuAction_Input -> { setSpeed(); customDismiss(); true }

                    else -> true
                }
            }
            popup.show()
        }
        //开关：后台播放
        Switch_BackgroundPlay.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_BackgroundPlay = isChecked
            //发回更改命令
            val result = bundleOf("KEY" to "BackgroundPlay")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //开关：循环播放
        Switch_LoopPlay.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_LoopPlay = isChecked
            //发回更改命令
            val result = bundleOf("KEY" to "LoopPlay")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //开关：关闭方向监听器(OEL：OrientationEventListener)
        Switch_SealOEL.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_SealOEL = isChecked
            Log.d("SuMing", "Switch_SealOEL: $isChecked")
            //发回更改命令
            val result = bundleOf("KEY" to "SealOEL")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //开关：仅播放音频
        Switch_OnlyAudio.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_OnlyAudio = isChecked
            if (isChecked){
                vm.PREFS_OnlyVideo = false
                Switch_OnlyVideo.isChecked = false
            }
            //发回更改命令
            val result = bundleOf("KEY" to "OnlyAudio")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //开关：仅播放视频
        Switch_OnlyVideo.setOnCheckedChangeListener { _, isChecked ->
            vm.PREFS_OnlyVideo = isChecked
            if (isChecked){
                vm.PREFS_OnlyAudio = false
                Switch_OnlyAudio.isChecked = false
            }
            //发回更改命令
            val result = bundleOf("KEY" to "OnlyVideo")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }




        //按钮：AlwaysSeek
        val buttonAlwaysSeek = view.findViewById<FrameLayout>(R.id.buttonActualAlwaysSeek)
        val buttonAlwaysMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialAlwaysSeek)
        if (vm.PREFS_AlwaysSeek) {
            buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonAlwaysMaterial.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
        }
        buttonAlwaysSeek.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_AlwaysSeek) {
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
            }else{
                buttonAlwaysMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
            }
            //发回更改命令
            val result = bundleOf("KEY" to "AlwaysSeek")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //按钮：单击跳转
        val buttonTap = view.findViewById<FrameLayout>(R.id.buttonActualTap)
        val buttonTapMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialTap)
        if (vm.PREFS_TapJump) {
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
        }
        buttonTap.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_TapJump) {
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
            } else {
                buttonTapMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
            }
            //发回更改命令
            val result = bundleOf("KEY" to "TapScroll")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }
        //按钮：链接滚动条与视频进度
        val buttonLink = view.findViewById<FrameLayout>(R.id.buttonActualLink)
        val buttonLinkMaterial = view.findViewById<MaterialButton>(R.id.buttonMaterialLink)
        if (vm.PREFS_LinkScroll) {
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
        } else {
            buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
        }
        buttonLink.setOnClickListener {
            //先给按钮改颜色
            if (vm.PREFS_LinkScroll) {
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBgClosed))
            } else {
                buttonLinkMaterial.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ButtonBg))
            }
            //发回更改命令
            val result = bundleOf("KEY" to "LinkScroll")
            setFragmentResult("FROM_FRAGMENT", result)

            customDismiss()
        }

        //视频信息
        val buttonVideoInfo = view.findViewById<TextView>(R.id.buttonVideoInfo)
        buttonVideoInfo.setOnClickListener {
            //发回更改命令
            val result = bundleOf("KEY" to "VideoInfo")
            setFragmentResult("FROM_FRAGMENT", result)

            dismiss()
        }
        //分享
        val buttonSysShare = view.findViewById<TextView>(R.id.buttonSysShare)
        buttonSysShare.setOnClickListener {
            //发回更改命令
            val result = bundleOf("KEY" to "SysShare")
            setFragmentResult("FROM_FRAGMENT", result)

            dismiss()
        }
        //音频
        val buttonEqualizer = view.findViewById<TextView>(R.id.buttonEqualizer)
        buttonEqualizer.setOnClickListener {
            //发回更改命令
            val result = bundleOf("KEY" to "Equalizer")
            setFragmentResult("FROM_FRAGMENT", result)

            dismiss()
        }




    } //onViewCreated END


    //Functions
    private fun setSpeed(){
        //发回更改命令
        val result = bundleOf("KEY" to "SetSpeed")
        setFragmentResult("FROM_FRAGMENT", result)

        dismiss()
    }

    private fun customDismiss(){
        if (!lockPage) { dismiss() }
    }

}