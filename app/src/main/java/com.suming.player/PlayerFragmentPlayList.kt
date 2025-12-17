package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.shape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.collect.Multimaps.index
import data.MediaDataReader.MediaDataBaseReaderForVideo
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    //设置
    private lateinit var PREFS: SharedPreferences
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //协程作用域
    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    //自动关闭标志位
    private var lockPage = false
    //声明式显示列表区域
    private lateinit var composableView: View
    private lateinit var composeView: androidx.compose.ui.platform.ComposeView
    private lateinit var mediaItemsMutableSnapshot: SnapshotStateList<MediaItemForVideo>
    //当前正在播放项
    private var currentPlayingUri = ""
    //RecyclerView
    private lateinit var play_list_recyclerView: RecyclerView
    private lateinit var play_list_recyclerView_adapter: PlayerFragmentPlayListAdapter


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
        //初始化ComposeView
        composableView = view.findViewById(R.id.composableView)
        composeView = composableView as ComposeView
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
        //按钮：刷新
        val ButtonRefreshMediaList = view.findViewById<ImageButton>(R.id.ButtonRefreshMediaList)
        ButtonRefreshMediaList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            PlayerSingleton.updateMediaList(requireContext())
            composeView.setContent {  }
            startShowVideoListRunnable()
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

        initRecyclerView()

        setPlayListRecyclerViewAdapter()

        //声明式显示列表
        /*
        composeView.setContent {
            showVideoList()
        }

         */




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

    private fun initRecyclerView(){

        play_list_recyclerView = view!!.findViewById(R.id.PlayListRecyclerView)

        play_list_recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    private fun setPlayListRecyclerViewAdapter(){

        play_list_recyclerView_adapter = PlayerFragmentPlayListAdapter(
            requireContext(),
            onPlayClick = { itemUri -> onPlayClick(itemUri) },
            onDeleteClick = { itemUri -> onDeleteClick(itemUri) }
        )

        play_list_recyclerView.adapter = play_list_recyclerView_adapter
        //分页加载
        val pager = Pager(PagingConfig(pageSize = 20)) {
            PlayerFragmentPlayListPagingSource(requireContext())
        }
        //分页加载数据
        lifecycleScope.launch {
            pager.flow.collect { pagingData ->
                play_list_recyclerView_adapter.submitData(pagingData)
            }
        }
    }

    //声明式UI
    @Composable
    private fun showVideoList() {
        //获取正在播放项
        currentPlayingUri = PlayerSingleton.getMediaInfoUri()
        //获取播放列表
        mediaItemsMutableSnapshot = PlayerSingleton.getMediaList(requireContext())
        //播放列表为空
        if (mediaItemsMutableSnapshot.isEmpty()){
            Text(
                text = "播放列表为空",
                color = colorResource(R.color.HeadText),
                style  = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 5.dp, 10.dp, 5.dp)
            )
            return
        }
        //播放列表不为空
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        )
        {
            items(
                items = mediaItemsMutableSnapshot,
                key = { it.uri },
                contentType = { "media_card" }
            ) { item ->
                var visible by remember { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = visible,
                    exit =  shrinkVertically()
                )
                {
                    EachItemCard(
                        index = mediaItemsMutableSnapshot.indexOf(item),
                        item = item,
                        onDelete = {
                            visible = false
                        },
                        onPlay = { onPlayClick(item.uri) }
                    )
                }

                LaunchedEffect(visible)
                {
                    if (!visible) {
                        kotlinx.coroutines.delay(300)
                        mediaItemsMutableSnapshot.remove(item)
                        onDeleteClick(item.uri)
                    }
                }
            }
            items(mediaItemsMutableSnapshot) { item ->

            }//items(mediaItems)
        }

    }
    @Composable
    private fun EachItemCard(index: Int, item: MediaItemForVideo, onDelete: () -> Unit, onPlay: () -> Unit)
    {
        Card(
            shape = RoundedCornerShape(6.dp),
            //elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(if (item.uri.toString() == currentPlayingUri){
                        R.color.PlayListCard_ON
                    } else{
                        R.color.PlayListCard_OFF
                    })),
            modifier = Modifier.fillMaxWidth()
                .then(
                    if (index == mediaItemsMutableSnapshot.lastIndex)
                        Modifier.padding(bottom = 300.dp)
                        //Modifier.border(1.dp, colorResource(R.color.HeadText2), shape).padding(bottom = 300.dp)
                    else
                        Modifier
                ),
        )
        {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 5.dp, 10.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Column(
                    Modifier.weight(1f)
                )
                {
                    Text(
                        text = item.name,
                        color = colorResource(R.color.HeadText),
                        style  = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.uri.toString(),
                        color = colorResource(R.color.HeadText2),
                        fontSize = 8.sp,
                        style  = MaterialTheme.typography.bodyMedium
                    )
                }
                //按钮：移除该项
                IconButton(
                    onClick = {
                        onDelete()
                    },
                    modifier = Modifier.size(20.dp)
                )
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_list_delete),
                        contentDescription = "移除",
                        tint = colorResource(R.color.HeadText2)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                //按钮：立即播放
                IconButton(
                    onClick = { onPlayClick(item.uri) },
                    modifier = Modifier.size(22.dp)
                )
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_list_play),
                        contentDescription = "播放",
                        tint = colorResource(R.color.HeadText2)
                    )
                }
            }
        }
    }
    //播放点击事件
    private fun onPlayClick(uri: Uri) {
        if (uri.toString() == PlayerSingleton.getMediaInfoUri()){
            PlayerSingleton.playPlayer()
            requireContext().showCustomToast("已在播放该视频", Toast.LENGTH_SHORT, 3)
        }else{
            PlayerSingleton.setMediaItem(uri, true)
            customDismiss()
        }
    }
    //删除点击事件
    private fun onDeleteClick(uri: Uri) {
        if (uri.toString() == PlayerSingleton.getMediaInfoUri()){
            //本地和播放器都要闪一次
            mediaItemsMutableSnapshot.removeIf { it.uri == uri }
            PlayerSingleton.deleteMediaItem(uri)
            if (mediaItemsMutableSnapshot.isNotEmpty()){
                PlayerSingleton.switchToNextMediaItem()
                requireContext().showCustomToast("已切换至下一曲", Toast.LENGTH_SHORT, 3)
            }else{
                PlayerSingleton.clearMediaItem()
                PlayerSingleton.clearMediaInfo()
                PlayerSingleton.ReleaseSingletonPlayer(requireContext())
                requireContext().showCustomToast("当前列表已无曲目", Toast.LENGTH_SHORT, 3)
                ToolEventBus.sendEvent("PlayerSingleton_MediaItemChanged")
                customDismiss(true)
            }
        }
        else{
            //移除列表中的项
            mediaItemsMutableSnapshot.removeIf { it.uri == uri }
        }
    }

    //Functions
    //自定义退出逻辑
    private fun customDismiss(flag_need_vibrate: Boolean = true){
        if (!lockPage) {
            Dismiss(flag_need_vibrate)
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ ToolVibrate().vibrate(requireContext()) }
        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        stopShowVideoListRunnable()
        dismiss()
    }
    //Runnable:重新显示媒体列表
    private val showVideoListHandler = Handler(Looper.getMainLooper())
    private val showVideoList = object : Runnable {
        override fun run() {
            val isComplete = PlayerSingleton.isMediaListProcessComplete()
            if (isComplete){
                composeView.setContent {
                    showVideoList()
                }
            }
            else{
                showVideoListHandler.postDelayed(this, 50)
            }
        }
    }
    private fun startShowVideoListRunnable() {
        showVideoListHandler.post(showVideoList)
    }
    private fun stopShowVideoListRunnable() {
        showVideoListHandler.removeCallbacks(showVideoList)
    }

}