package com.suming.player.ListManager

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
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
import com.suming.player.ToolVibrate
import com.suming.player.showCustomToast
import kotlinx.coroutines.launch

@UnstableApi
@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.Q)
class FragmentPlayListCustomFragment(
    private val onPlayClick: (String) -> Unit,
    private val onDeleteClick: (Long) -> Unit,
    private val onPlayListChange: (Int) -> Unit,
    private val onDefaultPageChange: (Int) -> Unit
) : Fragment(R.layout.activity_player_fragment_play_list_custom_page) {
    //共享ViewModel
    private val vm: PlayerListViewModel by activityViewModels()
    //当前页签
    private val currentPageFlag = 0
    //加载中卡片
    private lateinit var LoadingState: LinearLayout
    private lateinit var LoadingStateText: TextView
    private lateinit var TextItemCount: TextView
    //当前播放列表
    private lateinit var ButtonSetAsCurrentListText: TextView
    private lateinit var ButtonSetAsCurrentListIcon: ImageView
    //recyclerView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_custom_list_adapter: FragmentPlayListCustomAdapter
    private var state_adapter_load_complete = false


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
            //弹出页面选项菜单
            val popup = PopupMenu(requireContext(), pageSettingButton)
            popup.menuInflater.inflate(R.menu.activity_play_list_popup_page_setting, popup.menu)
            val menuItem_default_page = popup.menu.findItem(R.id.setting_set_as_default_show_list)
            if (currentPageFlag == vm.PREFS_AcquiescePage) {
                menuItem_default_page.title = "取消设为默认显示页签"
            }else{
                menuItem_default_page.title = "设为默认显示页签"
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.setting_set_as_current_list -> {
                        ToolVibrate().vibrate(requireContext())

                        setAsCurrentPlayList()

                        true
                    }

                    R.id.setting_set_as_default_show_list -> {
                        ToolVibrate().vibrate(requireContext())
                        //设置默认显示列表
                        onDefaultPageChange(0)
                        true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        //按钮：设为当前播放列表/已是当前播放列表
        val ButtonSetAsCurrentList = view.findViewById<View>(R.id.ButtonSetAsCurrentList)
        ButtonSetAsCurrentListText = view.findViewById(R.id.ButtonSetAsCurrentListText)
        ButtonSetAsCurrentListIcon = view.findViewById(R.id.ButtonSetAsCurrentListIcon)
        setCurrentListState()
        ButtonSetAsCurrentList.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            setAsCurrentPlayList()
        }
        //横滑选项按钮
        //按钮：全部删除
        val ButtonDeleteAllListItem = view.findViewById<View>(R.id.ButtonDeleteAllListItem)
        ButtonDeleteAllListItem.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //清空自定义列表
            PlayerListManager.clearCustomList()
            //刷新适配器
            recyclerView_custom_list_adapter.refresh()
        }
        //按钮：总项数
        val ButtonItemCount = view.findViewById<CardView>(R.id.ButtonItemCount)
        ButtonItemCount.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //未加载完成前拒绝访问
            if (!state_adapter_load_complete) return@setOnClickListener
            //显示列表中项数
            val itemCount = recyclerView_custom_list_adapter.itemCount
            if (itemCount == 0) {
                requireContext().showCustomToast("目前还没有媒体，可在其他列表中添加", Toast.LENGTH_SHORT, 2)
            }
            else{
                requireContext().showCustomToast("包含${itemCount}个媒体", Toast.LENGTH_SHORT, 2)
            }


        }
        //按钮：立即刷新
        val ButtonUpdate = view.findViewById<CardView>(R.id.ButtonUpdate)
        ButtonUpdate.setOnClickListener {
            ToolVibrate().vibrate(requireContext())

            recyclerView_custom_list_adapter.refresh()

        }




        //初始化recyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        //设置管理器
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        //合成适配器
        recyclerView_custom_list_adapter = FragmentPlayListCustomAdapter(
            requireContext(),
            onDeleteClick = { uriNumOnly -> onDeleteClick(uriNumOnly) },
            onPlayClick = { onPlayClick(it.toString()) },
        )
        //设置适配器
        recyclerView.adapter = recyclerView_custom_list_adapter
        //分页加载
        val pager = Pager(PagingConfig(pageSize = 20)) {
            FragmentPlayListCustomPagingSource(requireContext())
        }
        //分页加载数据
        lifecycleScope.launch {
            pager.flow.collect { pagingData ->
                recyclerView_custom_list_adapter.submitData(pagingData)
            }
        }
        //添加加载状态监听器
        recyclerView_custom_list_adapter.addLoadStateListener { loadState ->
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


    //onViewCreated END
    }

    //外部Functions
    fun receiveInstruction(data: Any) {
        when (data) {
            is String -> {
                when (data) {
                    "switch_to_you" -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            switchedToThisList()
                        }, 300)
                    }
                    "changed_current_list" -> {
                        setCurrentListState()
                    }
                    "go_top" -> {
                        recyclerView.smoothScrollToPosition(0)
                    }
                    "update" -> {
                        recyclerView_custom_list_adapter.refresh()
                    }
                }
            }

        }
    }


    //切换到此列表
    private fun switchedToThisList(){
        setCurrentListState()
        recyclerView_custom_list_adapter.refresh()
    }
    //设置为当前播放列表
    private fun setAsCurrentPlayList(){
        val isSetSuccess = PlayerListManager.setPlayList("custom")

        //更新当前播放列表
        setCurrentListState()

        if (isSetSuccess){
            //更新当前播放列表
            onPlayListChange(0)
            requireContext().showCustomToast("设置成功", Toast.LENGTH_SHORT, 2)
        }
        else{
            requireContext().showCustomToast("设置失败", Toast.LENGTH_SHORT, 2)
        }

    }

    //stable Functions
    //是否已经是当前播放列表
    private fun setCurrentListState(){
        //判断是否是当前播放列表
        if (PlayerListManager.getCurrentList(requireContext()) == currentPageFlag){
            ButtonSetAsCurrentListText.text = "已设为当前播放列表"
            ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_play_list_checkmark)
        }
        else{
            ButtonSetAsCurrentListText.text = "设为当前播放列表"
            ButtonSetAsCurrentListIcon.setImageResource(R.drawable.ic_play_list_add)
        }
    }
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
        //刷新状态
        state_adapter_load_complete = true
        //更新总项数文字
        val itemCount = recyclerView_custom_list_adapter.itemCount
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