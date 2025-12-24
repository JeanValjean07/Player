package com.suming.player.ListManager

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.PlayerViewModel
import com.suming.player.R
import com.suming.player.ToolVibrate
import kotlinx.coroutines.launch
import kotlin.getValue

@UnstableApi
class FragmentPlayListMusicFragment(
    private val onPlayClick: (String) -> Unit,
    private val onAddToListClick: (String) -> Unit,
) : Fragment(R.layout.activity_player_fragment_play_list_live_page) {
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //加载中卡片
    private lateinit var LoadingState: LinearLayout
    private lateinit var LoadingStateText: TextView
    private lateinit var TextItemCount: TextView
    //RecyclerView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_music_adapter: FragmentPlayListMusicAdapter

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //加载中卡片
        LoadingState = view.findViewById(R.id.LoadingState)
        LoadingStateText = view.findViewById(R.id.LoadingStateText)
        TextItemCount = view.findViewById(R.id.TextItemCount)

        //页面设置按钮
        val pageSettingButton = view.findViewById<View>(R.id.pageSettingButton)
        pageSettingButton.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            Log.d("SuMing", "pageSettingButton - music")
            val popup = PopupMenu(requireContext(), pageSettingButton)
            popup.menuInflater.inflate(R.menu.activity_play_list_popup_page_setting, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.setting_set_as_current_list -> {
                        ToolVibrate().vibrate(requireContext())
                        Log.d("SuMing", "pageSettingButton  setting_set_as_current_list  music")
                        true
                    }

                    R.id.setting_set_as_default_show_list -> {
                        ToolVibrate().vibrate(requireContext())
                        Log.d("SuMing", "pageSettingButton  setting_set_as_default_show_list  music")
                        true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        //按钮：设为当前播放列表/已是当前播放列表
        val ButtonSetAsCurrentList = view.findViewById<View>(R.id.ButtonSetAsCurrentList)
        val ButtonSetAsCurrentListText = view.findViewById<View>(R.id.ButtonSetAsCurrentListText)
        val ButtonSetAsCurrentListIcon = view.findViewById<View>(R.id.ButtonSetAsCurrentListIcon)
        ButtonSetAsCurrentList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            Log.d("SuMing", "ButtonSetAsCurrentList - music")
        }

        //初始化recyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        //设置管理器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        //初始化adapter + 设置点击事件
        recyclerView_music_adapter = FragmentPlayListMusicAdapter(
            requireContext(),
            onAddToListClick = { uri -> onAddToListClick(uri) },
            onPlayClick = {
                onPlayClick(it)
            },
        )
        //设置适配器
        recyclerView.adapter = recyclerView_music_adapter
        //分页加载
        val pager = Pager(PagingConfig(pageSize = 20)) {
            FragmentPlayListMusicPagingSource(requireContext())
        }
        //分页加载数据
        lifecycleScope.launch {
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
                    LoadingComplete()
                }
                is LoadState.Error -> {
                    showErrorNotice()
                }
            }
        }



    }










    //stable Functions
    //加载状态提示
    private fun showLoadingNotice() {
        LoadingStateText.text = "加载中"
        LoadingState.visibility = View.VISIBLE
    }
    private fun showErrorNotice() {
        LoadingStateText.text = "加载出现异常"
        LoadingState.visibility = View.VISIBLE
    }
    private fun LoadingComplete() {
        val itemCount = recyclerView_music_adapter.itemCount
        showItemCount(itemCount)
        if (itemCount == 0) {
            showEmptyNotice()
        }
        else{
            LoadingStateText.text = "加载完成"
            LoadingState.visibility = View.GONE
        }
    }
    private fun showEmptyNotice() {
        LoadingStateText.text = "列表为空"
        LoadingState.visibility = View.VISIBLE
    }
    private fun showItemCount(count: Int) {
        TextItemCount.text = count.toString()
    }

}