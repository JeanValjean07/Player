package com.suming.player.ListManager

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import kotlinx.coroutines.launch

class FragmentPlayListCustomFragment(
    private val onPlayClick: (String) -> Unit,
    private val onDeleteClick: (Long) -> Unit,
) : Fragment(R.layout.activity_player_fragment_play_list_custom_page) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_custom_list_adapter: FragmentPlayListCustomAdapter

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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


    }

}