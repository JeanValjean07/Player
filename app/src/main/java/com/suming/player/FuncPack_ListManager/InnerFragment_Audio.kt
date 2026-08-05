package com.suming.player.FuncPack_ListManager

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
//@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
class InnerFragment_Audio :Fragment(R.layout.fragment_play_list_live_page){
    companion object {
        fun newInstance(): InnerFragment_Audio {
            return InnerFragment_Audio().apply{
                arguments = bundleOf()
            }
        }
    }
    //当前页签(固定值)
    private val flag_currentPage = ListManagerHelper.ListMark_Audio




    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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


            //按钮：总项数(分页下显示不全)
            val ButtonItemCount = view.findViewById<CardView>(R.id.ButtonItemCount)
            ButtonItemCount.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //未加载完成前拒绝访问
                if (!state_adapter_load_complete) return@setOnClickListener
                //显示列表中项数
                val itemCount = recyclerView_music_adapter.itemCount
                if (itemCount == 0) {
                    requireContext().showCustomToast("目前还没有音乐",2)
                }
                else{
                    requireContext().showCustomToast("包含${itemCount}条音乐",2)
                }


            }
            //强制刷新
            val ButtonForceRefresh = view.findViewById<CardView>(R.id.ButtonForceRefresh)
            ButtonForceRefresh.setOnClickListener {
                ToolVibrate().vibrate(requireContext())

                recyclerView.smoothScrollToPosition(0)
                recyclerView_music_adapter.refresh()
            }

        }
    }








    //Fragment通信
    //注册接收父Fragment返回值
    private fun registerFragmentResultListener(){
        parentFragmentManager.setFragmentResultListener(ListManagerHelper.fragment_request_key_audio, this){ _, bundle ->
            val key = bundle.getString(ListManagerHelper.event_key_general) ?: return@setFragmentResultListener
            when(key){

                //回滚到顶部
                "FRAGMENT_PASSIN_SCROLLTOP" -> {
                    recyclerView.smoothScrollToPosition(0)
                }
                //当前播放列表更新
                "FRAGMENT_PASSIN_CURRENT_LIST_UPDATE" -> {
                    updateCurrentListStateText()
                }

            }
        }
    }
    //发送Fragment结果
    private fun sendFragmentResult(event: String){
        parentFragmentManager.setFragmentResult(ListManagerHelper.fragment_request_key_audio,
            bundleOf(ListManagerHelper.event_key_general to event)
        )
    }
    //启动recyclerView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_music_adapter: Recycler_Adaptor_Audio
    private var state_adapter_load_complete = false
    private fun startRecyclerView(view: View){
        //初始化recyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        //设置管理器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        //初始化adapter + 设置点击事件
        recyclerView_music_adapter = Recycler_Adaptor_Audio(
            requireContext(),
            onAddToListClick = { uriString -> onAddToListClick(uriString.toUri()) },
            onPlayClick = { uriString -> onPlayClick(uriString.toUri()) },
        )
        //添加页脚
        val adapterWithFooter = recyclerView_music_adapter.withLoadStateFooter(footer = ListBottomSloganAdapter {
            recyclerView_music_adapter.retry()
        })
        //设置adapter
        recyclerView.adapter = adapterWithFooter
        //开始分页加载
        lifecycleScope.launch(Dispatchers.IO) {
            val pager = Pager(PagingConfig(pageSize = 20)) { Recycler_PagingSource_Audio(requireContext()) }
            pager.flow.collect { pagingData ->
                recyclerView_music_adapter.submitData(pagingData)
            }
        }
        //添加加载状态监听器
        recyclerView_music_adapter.addLoadStateListener { loadState ->
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

    //页面获得焦点
    private fun onFragmentFocused() {
        updateCurrentListStateText()
        //recyclerView_music_adapter.refresh()
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
    private fun onAddToListClick(uri: Uri){
        ToolVibrate().vibrate(requireContext())



    }
    //播放项
    private fun onPlayClick(uri: Uri){
        ToolVibrate().vibrate(requireContext())


    }


    //设为默认显示列表
    private fun setAs_AcquiesceShowingPage(){
        //判断是否已经是默认列表
        val currentAcquiescePage = ListManagerHelper.GET_PRFR_AcquiesceShowingPage()
        if (currentAcquiescePage == flag_currentPage){
            val success = ListManagerHelper.SET_PRFR_AcquiesceShowingPage(ListManagerHelper.ListMark_Audio)
            if (success) {
                requireContext().showCustomToast("已取消默认页签,默认使用上次页签",2)
                updateCurrentListStateText()
            }
        }else{
            val success = ListManagerHelper.SET_PRFR_AcquiesceShowingPage(flag_currentPage)
            if (success) {
                requireContext().showCustomToast("设置成功",2)
                updateCurrentListStateText()
            }
        }
    }
    //设置为当前播放列表
    private fun setAs_currentPlayingList(){
        val success = ListManagerHelper.SET_STE_CurrentPlayingListMark(flag_currentPage)

        //更新当前播放列表文字
        updateCurrentListStateText()

        //更新当前播放列表图标
        if (success){
            requireContext().showCustomToast("设置成功",2)

            sendFragmentResult(ListManagerHelper.event_detail_general_update_currentPlayingList_icon)

        }else{
            requireContext().showCustomToast("设置失败",2)
        }
    }

    //显示是否已设为当前播放列表
    private lateinit var ButtonSetAsCurrentListText: TextView
    private lateinit var ButtonSetAsCurrentListIcon: ImageView
    private fun updateCurrentListStateText(){
        //判断是否是当前播放列表
        if (ListManagerHelper.GET_STE_CurrentPlayingListMark() == flag_currentPage){
            ButtonSetAsCurrentListText.text = "已设为当前播放列表"
            ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_play_list_checkmark)
        }
        else{
            ButtonSetAsCurrentListText.text = "设为当前播放列表"
            ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_play_list_add)
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
        val itemCount = recyclerView_music_adapter.itemCount
        showItemCount(itemCount,view)
        if (itemCount == 0) {
            showEmptyNotice()
        }else{

        }
    }
    private fun showEmptyNotice() {

    }
    private fun showItemCount(count: Int,view: View) {
        val TextItemCount = view.findViewById<TextView>(R.id.TextItemCount)
        TextItemCount.text = count.toString()
    }



}