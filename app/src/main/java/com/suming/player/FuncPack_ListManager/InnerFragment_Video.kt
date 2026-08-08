package com.suming.player.FuncPack_ListManager

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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
import com.suming.player.DataPack.DataLoader.VideoDataBaseLoader
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
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

@UnstableApi
@Suppress("NewApi","unused") //"unused",
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




    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化组件
        init(view)

        //组件注册
        register(view)

        //开启Fragment通信
        registerFragmentResultListener()

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

    }



    //组件注册
    private fun register(view: View){
        //组件注册
        lifecycleScope.launch(Dispatchers.Main){
            //页面设置按钮
            val pageSettingButton = view.findViewById<View>(R.id.pageSettingButton)
            pageSettingButton.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                startPageSettingMenu(pageSettingButton)
            }

            //按钮：设为当前播放列表/已是当前播放列表
            val ButtonSetAsCurrentList = view.findViewById<View>(R.id.ButtonSetAsCurrentList)
            ButtonSetAsCurrentListText = view.findViewById(R.id.ButtonSetAsCurrentListText)
            ButtonSetAsCurrentListIcon = view.findViewById(R.id.ButtonSetAsCurrentListIcon)
            updateCurrentListStateText()
            ButtonSetAsCurrentList.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                setAs_currentPlayingList()
            }

            //按钮：总项数
            val ButtonItemCount = view.findViewById<CardView>(R.id.ButtonItemCount)
            ButtonItemCount.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
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

                recyclerView.smoothScrollToPosition(0)
                recyclerView_video_adapter.refresh()
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
            onPlayClick = { item, position -> onPlayClick(item, position) },
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
    //注册接收父Fragment返回值
    private fun registerFragmentResultListener(){
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

                //播放项变更
                ListManagerHelper.event_detail_general_media_item_update -> {
                    onMediaItemUpdate()
                }
                //播放状态变更
                ListManagerHelper.event_detail_general_media_state_update -> {
                    onMediaStateUpdate()
                }
            }
        }
    }
    //发送Fragment结果
    private fun sendFragmentResult(event: String){
        parentFragmentManager.setFragmentResult(
            ListManagerHelper.fragment_request_key_video_reverse,
            bundleOf(ListManagerHelper.event_key_general to event)
        )
    }


    //播放项变更
    private fun onMediaItemUpdate(){
        //获取当前播放项
        val currentItemUri = PlayerInfoCenter.observableMediaItem.value.content_uriString
        consoleLog("onMediaItemUpdate()当前播放项: $currentItemUri")

        //使用payload更新当前播放项指示器
        recyclerView_video_adapter.updateCurrentMediaItem(currentItemUri, ListManagerHelper.payload_event_item_update)

    }
    //播放状态变更
    private fun onMediaStateUpdate(){
        //获取当前播放项
        val currentItemUri = PlayerInfoCenter.observableMediaItem.value.content_uriString
        consoleLog("onMediaStateUpdate()当前播放项: $currentItemUri")

        //使用payload更新当前播放项指示器
        recyclerView_video_adapter.updateCurrentIsPlayingState(currentItemUri, PlayerInfoCenter.observableIsPlaying.value, ListManagerHelper.payload_event_item_state_update)

    }




    //页面获得焦点
    private fun onFragmentFocused(){

        updateCurrentListStateText()

        checkNowOngoingItem()
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

    //检查
    private fun checkNowOngoingItem(){
        val currentMediaType = PlayerInfoCenter.observableMediaItem.value.media_SPECIFIC_MediaType
        //consoleLog("currentMediaType: $currentMediaType")
        if (currentMediaType != MediaType.Video){
            //consoleLog("当前播放项不是视频,清理播放标记")
            //清理播放标记
            recyclerView_video_adapter.clearPlayingItem(ListManagerHelper.payload_event_item_clear_playing_mark)
        }
    }


    //添加到自定义
    private fun onAddToListClick(item: MediaItemFullForVideo){
        consoleLog("添加到自定义列表: ${item.file_name}")



    }
    //播放视频
    private fun onPlayClick(item: MediaItemFullForVideo, position: Int){

        if (item.content_uriString == PlayerSingleton.GET_STE_currentMediaItem_Uri().second.toString()){
            if (PlayerInfoCenter.observableIsPlaying.value){
                PlayerSingleton.pausePlay()
            }else{
                PlayerSingleton.continuePlay(true)
            }
        }else{
            //确保播放器已经启动
            PlayerSingleton.getInitPlayer()
            PlayerSingleton.addPlayerStateListener()
            //设置播放项
            PlayerSingleton.setMediaItem(item.content_uriString.toUri(),true)
        }

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

            sendFragmentResult(ListManagerHelper.event_detail_general_update_currentPlayingList_icon)
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
    //加载状态提示(需要重做)
    private fun showLoadingNotice() {

    }
    private fun showErrorNotice() {

    }
    private fun LoadingComplete(view: View) {
        //刷新状态
        state_adapter_load_complete = true
        //更新总项数文字
        val itemCount = recyclerView_video_adapter.itemCount
        showItemCount(itemCount,view)
        if (itemCount == 0) {
            showEmptyNotice()
        }

    }
    private fun showEmptyNotice() {

    }
    private fun showItemCount(count: Int,view: View) {
        val TextItemCount = view.findViewById<TextView>(R.id.TextItemCount)
        TextItemCount.text = count.toString()
    }

    //dp转换为px
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