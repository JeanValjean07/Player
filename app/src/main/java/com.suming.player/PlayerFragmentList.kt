package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@SuppressLint("ComposableNaming")
@UnstableApi
class PlayerFragmentList: DialogFragment() {

    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()

    private val viewModelScope = CoroutineScope(Dispatchers.IO)



    companion object {
        fun newInstance(): PlayerFragmentList =
            PlayerFragmentList().apply {
                arguments = bundleOf(

                )
            }
    }

    data class MediaItem_video(
        val uri: String,
        val name: String
    )



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

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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

        //先显示空列表
        val composableView = view.findViewById<View>(R.id.composableView)
        val composeView = composableView as androidx.compose.ui.platform.ComposeView
        composeView.setContent {
            showVideoList("")
        }

        //读取播放列表 (暂时放在主线程)


            val PREFS_List = requireContext().getSharedPreferences("PREFS_List", Context.MODE_PRIVATE)
            if (PREFS_List.contains("CurrentPlayList")){

                val playListString = PREFS_List.getString("CurrentPlayList", "错误") ?: "错误"

                val composableView = view.findViewById<View>(R.id.composableView)
                val composeView = composableView as androidx.compose.ui.platform.ComposeView

                    composeView.setContent {
                        showVideoList(playListString)
                    }

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

    } //onViewCreated END

    //Composable
    @Composable
    fun showVideoList(playlistString: String) {
        val list = remember(playlistString) { parsePlaylistString(playlistString) }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            list.forEach { item ->

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
                            text = item.uri,
                            color = colorResource(R.color.HeadText2),
                            fontSize = 8.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(0.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(item) },
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
                        onClick = { onPlayClick(item) },
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
                Divider(
                    color = colorResource(R.color.divider),
                    thickness = 0.5.dp
                )

            }//list.forEach
        }
    }


    //傻逼预览
    /*
    @Preview(showBackground = true)
    @Composable
    fun PlainVideoListPreview() {
        showVideoList(
            playlistString = """
            MediaItem_video(uri=content://media/external/video/media/3248, name=角色入场4.mp4),
            MediaItem_video(uri=content://media/external/video/media/537, name=角色入场3.mp4)
        """.trimIndent(),
        )
    }

     */


    private fun onPlayClick(item: MediaItem_video) {

        val itemString = item.toString()
        val itemUri = item.uri
        val result = bundleOf("KEY" to "switchItem", "new_item" to itemString, "new_item_uri" to itemUri)
        setFragmentResult("FROM_FRAGMENT_PLAY_LIST", result)

        dismiss()

    }

    private fun onDeleteClick(item: MediaItem_video) {

       Log.d("SuMing", "onDeleteClick: $item")



    }


    //Functions
    //字符串转list<MediaItem_video>
    fun parsePlaylistString(str: String): List<MediaItem_video> {
        val regex = """MediaItem_video\(uri=([^,]+),\s*name=([^)]+)\)""".toRegex()
        return regex.findAll(str).map {
            MediaItem_video(
                uri = it.groupValues[1],
                name = it.groupValues[2]
            )
        }.toList()
    }



}