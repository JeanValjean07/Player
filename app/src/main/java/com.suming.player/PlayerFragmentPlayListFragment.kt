package com.suming.player

import android.net.Uri
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
import kotlinx.coroutines.launch

class PlayerFragmentPlayListFragment(
    private val flag: Int = 0,
    private val onPlayClick: (String) -> Unit,
    private val onAddToListClick: (String) -> Unit,
) : Fragment(R.layout.activity_player_fragment_play_list_page_live) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_video_adapter: PlayerFragmentPlayListVideoAdapter
    private lateinit var recyclerView_music_adapter: PlayerFragmentPlayListMusicAdapter

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        recyclerView = view.findViewById(R.id.recyclerView)

        //视频列表
        if (flag == 0) {
            //设置管理器
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            //初始化adapter + 设置点击事件
            recyclerView_video_adapter = PlayerFragmentPlayListVideoAdapter(
                requireContext(),
                onAddToListClick = { uri -> onAddToListClick(uri) },
                onPlayClick = {
                    onPlayClick(it)
                },
            )
            //设置适配器
            recyclerView.adapter = recyclerView_video_adapter
            //分页加载
            val pager = Pager(PagingConfig(pageSize = 20)) {
                PlayerFragmentPlayListVideoPagingSource(requireContext())
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    recyclerView_video_adapter.submitData(pagingData)
                }
            }
        }
        //音乐列表
        else if (flag == 1) {
            //设置管理器
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            //初始化adapter + 设置点击事件
            recyclerView_music_adapter = PlayerFragmentPlayListMusicAdapter(
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
                PlayerFragmentPlayListMusicPagingSource(requireContext())
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    recyclerView_music_adapter.submitData(pagingData)
                }
            }
        }


    }




    fun InstigateRefresh(){



    }



}