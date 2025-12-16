package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.URI
import kotlin.math.abs

@SuppressLint("ComposableNaming")
@UnstableApi
class PlayerFragmentPlayList: DialogFragment() {
    //设置
    private lateinit var PREFS: SharedPreferences
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //协程作用域
    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    //自动关闭标志位
    private var lockPage = false
    //静态方法
    companion object {
        fun newInstance(): PlayerFragmentPlayList =
            PlayerFragmentPlayList().apply {
                arguments = bundleOf(

                )
            }
    }


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
        //检查播放列表是否加载完成
        val isPlayListProcessComplete = PlayerSingleton.getPlayListProcessComplete()
        if (!isPlayListProcessComplete){
            requireContext().showCustomToast("播放列表未加载完成", Toast.LENGTH_SHORT, 3)
            Dismiss()
        }
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

            PlayerSingleton.switchToPreviousMediaItem()

            customDismiss()
        }
        //按钮：下一曲
        val ButtonNextMedia = view.findViewById<ImageButton>(R.id.ButtonNextMedia)
        ButtonNextMedia.setOnClickListener {

            PlayerSingleton.switchToNextMediaItem()

            customDismiss()
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
        //声明式显示列表
        val composableView = view.findViewById<View>(R.id.composableView)
        val composeView = composableView as androidx.compose.ui.platform.ComposeView
        composeView.setContent {
            showVideoList()
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


    //声明式UI
    @Composable
    private fun showVideoList() {
        val mediaItems = PlayerSingleton.getPlayList(requireContext())

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            mediaItems.forEach { item ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp,5.dp,10.dp,5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            color = colorResource(R.color.HeadText),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(0.dp)
                        )

                        Text(
                            text = item.uri.toString(),
                            color = colorResource(R.color.HeadText2),
                            fontSize = 8.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(0.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(item.uri) },
                        modifier = Modifier
                            .padding(5.dp,0.dp,0.dp,0.dp)
                            .size(25.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_list_delete),
                            contentDescription = "移除",
                            tint = colorResource(R.color.HeadText),
                        )
                    }
                    IconButton(
                        onClick = { onPlayClick(item.uri, item.name) },
                        modifier = Modifier
                            .padding(5.dp,0.dp,0.dp,0.dp)
                            .size(25.dp)

                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_list_play),
                            contentDescription = "播放",
                            tint = colorResource(R.color.HeadText),
                        )
                    }
                }
                //分割线
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorResource(R.color.divider)
                )
            }//list.forEach
        }
    }

    private fun onPlayClick(uri: Uri, name: String) {
        val itemUri = uri.toString()
        val result = bundleOf("KEY" to "switchItem", "new_item_uri" to itemUri, "new_item_name" to name)
        setFragmentResult("FROM_FRAGMENT_PLAY_LIST", result)
        dismiss()
    }

    private fun onDeleteClick(uri: Uri) {  }

    //Functions
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