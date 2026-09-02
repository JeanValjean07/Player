package com.suming.player.FuncPack_ListManager

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.DataLoader.VideoDataBaseLoader
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
import com.suming.player.DataPack.DataLoader.VideoSysApiQuerier
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.PlayerSingleton
import com.suming.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@Suppress("NewApi","/unused")
class InnerFragment_Video :Fragment(R.layout.fragment_play_list_live_page){
    companion object {
        fun newInstance(): InnerFragment_Video {
            return InnerFragment_Video().apply{
                arguments = bundleOf()
            }
        }
    }
    //当前页签(固定值)
    private val flag_currentPage = ListManagerHelper.ListMark_Video

    //context
    private lateinit var context: Context


    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //获取context
        context = requireContext()

        //初始化组件
        init(view)

        //组件注册
        register(view)

        //注册来自 父Fragment 的消息监听器
        register_P_Fragment_Listener()

        //启动RecyclerView
        startRecyclerView(view)


    }

    override fun onResume() {
        super.onResume()

        //刷新必要内容
        onFragmentFocused()
    }

    private fun init(view:View){
        ButtonSetAsCurrentListText = view.findViewById(R.id.ButtonSetAsCurrentListText)
        ButtonSetAsCurrentListIcon = view.findViewById(R.id.ButtonSetAsCurrentListIcon)
        topBar_bottomLine = view.findViewById(R.id.topBar_bottomLine)
        //加载提示
        LoadingLayout = view.findViewById(R.id.LoadingLayout)
        LoadingLayoutText = view.findViewById(R.id.LoadingLayoutText)

    }



    //组件注册
    private fun register(view: View){
        //组件注册
        lifecycleScope.launch(Dispatchers.Main){
            //页面设置按钮
            val pageSettingButton = view.findViewById<View>(R.id.pageSettingButton)
            pageSettingButton.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //
                recyclerView.stopScroll()
                //
                startPageSettingMenu(pageSettingButton)
            }

            //按钮：设为当前播放列表/已是当前播放列表
            val ButtonSetAsCurrentList = view.findViewById<View>(R.id.ButtonSetAsCurrentList)
            ButtonSetAsCurrentListText = view.findViewById(R.id.ButtonSetAsCurrentListText)
            ButtonSetAsCurrentListIcon = view.findViewById(R.id.ButtonSetAsCurrentListIcon)
            updateCurrentListStateText()
            ButtonSetAsCurrentList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //
                recyclerView.stopScroll()
                //
                setAs_currentPlayingList()
            }

            //按钮：总项数
            val ButtonItemCount = view.findViewById<CardView>(R.id.ButtonItemCount)
            ButtonItemCount.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //
                recyclerView.stopScroll()
                //未加载完成前拒绝访问
                if (!state_adapter_load_complete) return@setOnClickListener
                //显示列表中项数
                val itemCount = recyclerView_video_adapter.itemCount
                if (itemCount == 0) {
                    requireContext().showCustomToast("目前还没有视频", 2)
                }
                else{
                    requireContext().showCustomToast("包含${itemCount}条视频", 2)
                }
            }

            //强制刷新(此页面无需主动刷新)
            val ButtonForceRefresh = view.findViewById<CardView>(R.id.ButtonForceRefresh)
            ButtonForceRefresh.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //
                recyclerView.stopScroll()
                //发起重读数据库
                startLoadBySystem()

            }


        }
        //延时注册
        lifecycleScope.launch(Dispatchers.Main){
            delay(1000)
            //启用分隔线监听器
            applyScrollListener()
            startListUnderTopObserver()
        }
    }
    //启动recyclerView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_video_adapter: Recycler_Adaptor_Video
    private lateinit var layoutManager: LinearLayoutManager
    private var state_adapter_load_complete = false
    private fun startRecyclerView(view: View){
        //初始化recyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        //设置内部间距
        recyclerView.setPadding(0, 0, 0, 200)
        //设置管理器
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        //初始化adapter + 设置点击事件
        recyclerView_video_adapter = Recycler_Adaptor_Video(
            requireContext(),
            onAddToListClick = { item -> onAddToListClick(item) },
            onPlayItemClick = { item -> onPlayItemClick(item) },
        )
        //添加页脚
        /*
        val adapterWithFooter = recyclerView_video_adapter.withLoadStateFooter(footer = ListBottomSloganAdapter {
            recyclerView_video_adapter.retry()
        })

         */
        //设置adapter
        recyclerView.adapter = recyclerView_video_adapter
        //开始分页加载
        lifecycleScope.launch(Dispatchers.IO) {
            val pager = Pager(PagingConfig(pageSize = 20)) {
                VideoDataBaseLoader(requireContext())
            }
            pager.flow.collect { pagingData ->
                recyclerView_video_adapter.submitData(pagingData)
            }
        }
        //添加加载状态监听器
        recyclerView_video_adapter.addLoadStateListener { loadState ->
            when (loadState.refresh) {
                is LoadState.Loading -> {
                    showLoadingNotice()
                }
                is LoadState.NotLoading -> {
                    LoadingComplete(view)
                }
                is LoadState.Error -> {
                    showErrorNotice()
                }
            }
        }
    }



    //Fragment通信
    //注册来自 父Fragment 的消息监听
    private fun register_P_Fragment_Listener(){
        parentFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_video, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            val extra = bundle.getString(ListManagerHelper.event_key_extra) ?: ""
            when(key){
                //回滚到顶部
                ListManagerHelper.event_detail_general_goto_list_top -> {
                    recyclerView.smoothScrollToPosition(0)
                }
                //更新页签状态
                ListManagerHelper.event_detail_general_update_list_state -> {
                    onFragmentFocused()
                }
                //列表adapter刷新
                ListManagerHelper.event_video_list_refresh -> {
                    recyclerView_video_adapter.refresh()
                }
                //播放项变更
                ListManagerHelper.event_detail_general_media_item_update -> {
                    onMediaStateUpdate()
                }
                //播放状态变更
                ListManagerHelper.event_detail_general_media_state_update -> {
                    onMediaStateUpdate()
                }
            }
        }
    }
    //向 父Fragment 发送事件结果
    private fun send_P_Fragment_Event(event: String){
        val result = Bundle().apply { putString(ListManagerHelper.event_key_general, event) }
        parentFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_video_reverse, result)
    }
    private fun send_P_Fragment_Event(event: String,extra: String){
        val result = Bundle().apply { putString(ListManagerHelper.event_key_general, event); putString(ListManagerHelper.event_key_extra, extra) }
        parentFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_video_reverse, result)
    }

    //发起数据库重读本机
    private fun startLoadBySystem(){
        lifecycleScope.launch(Dispatchers.IO){
            val videoReader = VideoSysApiQuerier(context, context.contentResolver)
            videoReader.readAndSaveAllVideos()

            withContext(Dispatchers.Main){
                //延迟200Ms自动回顶部
                //delay(200)
                //recyclerView.smoothScrollToPosition(0)
            }
        }
    }


    //播放状态变更
    private fun onMediaStateUpdate(){
        //获取当前播放项
        val ongoing_URI_S_FP = PlayerInfoCenter.observableMediaItem.value.URI_S_FP
        //获取当前播放/暂停状态
        val isPlaying = PlayerInfoCenter.observableIsPlaying.value
        //consoleLog("onMediaStateUpdate (视频列表): $ongoing_URI_S_FP, $isPlaying")

        //仅通知当前项的数据
        recyclerView_video_adapter.update_ongoingMediaState(
            ongoing_URI_S_FP,
            isPlaying,
            recyclerView
        )

    }


    //页面获得焦点
    private fun onFragmentFocused(){
        //刷新列表
        updateCurrentListStateText()
        //刷新列表
        recyclerView_video_adapter.refresh()

    }

    //页签设置选单
    private fun startPageSettingMenu(anchor: View){
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.activity_play_list_popup_page_setting, popup.menu)
        val menuItem_default_page = popup.menu.findItem(R.id.setting_set_as_default_show_list)
        val acquiescePage = ListManagerHelper.GET_PRFR_AcquiesceShowingPage()
        if (flag_currentPage == acquiescePage){
            menuItem_default_page.title = "取消设为默认显示页签"
        }else{
            menuItem_default_page.title = "设为默认显示页签"
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                //设为当前播放列表
                R.id.setting_set_as_current_list -> {
                    ToolVibrate().vibrate(requireContext())

                    setAs_currentPlayingList()

                    true
                }
                //设置默认显示列表
                R.id.setting_set_as_default_show_list -> {
                    ToolVibrate().vibrate(requireContext())

                    setAs_AcquiesceShowingPage()

                    true
                }

                else -> true
            }
        }
        popup.show()
    }


    //添加到自定义
    private fun onAddToListClick(item: MediaItemFullForVideo){
        //consoleLog("添加到自定义列表: ${item.file_name}")



    }
    //发起播放视频
    private fun onPlayItemClick(item: MediaItemFullForVideo){
        //传回 父Fragment 统一处理
        val URI_S_FP = item.URI_S_FP
        //通知 父Fragment 播放
        send_P_Fragment_Event(ListManagerHelper.event_detail_general_play_new_item,URI_S_FP)

    }


    //设为默认显示列表
    private fun setAs_AcquiesceShowingPage(){
        //判断是否已经是默认列表
        val currentAcquiescePage = ListManagerHelper.GET_PRFR_AcquiesceShowingPage()
        if (currentAcquiescePage == flag_currentPage){
            val success = ListManagerHelper.SET_PRFR_AcquiesceShowingPage(ListManagerHelper.ListMark_UseLast)
            if (success) {
                requireContext().showCustomToast("已取消默认页签,默认使用上次页签",2)
                updateCurrentListStateText()
            }else{
                requireContext().showCustomToast("设置失败",2)
            }
        }else{
            val success = ListManagerHelper.SET_PRFR_AcquiesceShowingPage(flag_currentPage)
            if (success) {
                requireContext().showCustomToast("设置成功",2)
                updateCurrentListStateText()
            }else{
                requireContext().showCustomToast("设置失败",2)
            }
        }
    }
    //设置为当前播放列表
    private fun setAs_currentPlayingList(){
        val success = ListManagerHelper.SET_STE_CurrentPlayingListMark(flag_currentPage)
        //更新当前播放列表
        updateCurrentListStateText()
        //更新当前播放列表图标
        if (success){
            requireContext().showCustomToast("设置成功",2)
            //通知父Fragment更新当前播放列表图标
            send_P_Fragment_Event(ListManagerHelper.event_detail_general_update_currentPlayingList_icon)
        }else{
            requireContext().showCustomToast("设置失败",2)
        }
    }


    //列表位置监控
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val isAtTop = !recyclerView.canScrollVertically(-1)
            isListUnderTop.value = isAtTop
        }
    }
    private val isListUnderTop = MutableStateFlow(true)
    val isListUnderTopFlow: StateFlow<Boolean> = isListUnderTop.asStateFlow()
    private fun startListUnderTopObserver(){
        //手动检查一次
        isListUnderTop.value = !recyclerView.canScrollVertically(-1)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                isListUnderTopFlow.collect{
                    if (it){
                        topBar_bottomLine_Out()
                    }else{
                        topBar_bottomLine_In()
                    }
                }
            }
        }
    }
    //为列表应用位置监控
    private fun applyScrollListener(){
        recyclerView.addOnScrollListener(scrollListener)
    }
    //顶部分隔线显示控制(In代表显示,Out代表隐藏)
    private lateinit var topBar_bottomLine : View
    private var isTopBar_bottomLine_In = false
    private fun topBar_bottomLine_In(){
        if (isTopBar_bottomLine_In) return
        isTopBar_bottomLine_In = true

        topBar_bottomLine.visibility = View.VISIBLE
        topBar_bottomLine.alpha = 0f
        topBar_bottomLine.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

    }
    private fun topBar_bottomLine_Out(){
        if (!isTopBar_bottomLine_In) return
        isTopBar_bottomLine_In = false

        topBar_bottomLine.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { topBar_bottomLine.visibility = View.GONE }
            .start()
    }



    //刷新当前播放列表状态提示词
    private lateinit var ButtonSetAsCurrentListText: TextView
    private lateinit var ButtonSetAsCurrentListIcon: ImageView
    private fun updateCurrentListStateText(){
        //判断是否是当前播放列表
        if (ListManagerHelper.GET_STE_CurrentPlayingListMark() == flag_currentPage){
            if (ButtonSetAsCurrentListText.text != ListManagerHelper.string_already_set_playing_list){
                ButtonSetAsCurrentListText.text = ListManagerHelper.string_already_set_playing_list
                ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_play_list_checkmark)
            }
        }else{
            if (ButtonSetAsCurrentListText.text != ListManagerHelper.string_set_as_playing_list){
                ButtonSetAsCurrentListText.text = ListManagerHelper.string_set_as_playing_list
                ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_general_add)
            }
        }
    }
    //加载状态提示
    private lateinit var LoadingLayout: LinearLayout
    private lateinit var LoadingLayoutText: TextView
    private fun showLoadingNotice(){
        if (state_adapter_load_complete) return

        LoadingLayoutText.text = "加载中"
        LoadingLayout.visibility = View.VISIBLE
    }
    private fun showErrorNotice(){
        if (state_adapter_load_complete) return

        LoadingLayoutText.text = "加载失败"
        LoadingLayout.visibility = View.VISIBLE
    }
    private fun showLoadingSuccess(){
        LoadingLayoutText.text = "加载成功"
        LoadingLayout.visibility = View.GONE
    }
    private fun LoadingComplete(view: View) {
        //刷新状态
        state_adapter_load_complete = true
        //更新总项数文字
        val itemCount = recyclerView_video_adapter.itemCount
        showItemCount(view)
        if (itemCount == 0) {
            showEmptyNotice()
        }else{
            showLoadingSuccess()
        }
    }
    private fun showEmptyNotice() {
        LoadingLayoutText.text = "什么都没有"
        LoadingLayout.visibility = View.VISIBLE
    }
    //从数据库读取总数
    private fun showItemCount(view: View) {
        val TextItemCount = view.findViewById<TextView>(R.id.TextItemCount)
        //从数据库读取总数
        lifecycleScope.launch(Dispatchers.IO){
            val count = VideoRepo(context).getTotalCount()
            withContext(Dispatchers.Main){
                TextItemCount.text = "$count"
            }
        }
    }

    //dp转换为px
    @Suppress("unused")
    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "InnerFragment_Video: $msg")
        }
    }


}