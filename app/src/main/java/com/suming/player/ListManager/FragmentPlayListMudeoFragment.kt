package com.suming.player.ListManager

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.PlayerViewModel
import com.suming.player.R
import kotlinx.coroutines.launch
import kotlin.getValue

@UnstableApi
class FragmentPlayListMudeoFragment(
    private val flag: Int = 0,
    private val onPlayClick: (String) -> Unit,
    private val onAddToListClick: (String) -> Unit,
) : Fragment(R.layout.activity_player_fragment_play_list_live_page) {
    //共享ViewModel
    private val vm: PlayerViewModel by activityViewModels()
    //RecyclerView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_video_adapter: FragmentPlayListVideoAdapter
    private lateinit var recyclerView_music_adapter: FragmentPlayListMusicAdapter

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        recyclerView = view.findViewById(R.id.recyclerView)

        //视频列表
        if (flag == 1) {
            //设置管理器
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            //初始化adapter + 设置点击事件
            recyclerView_video_adapter = FragmentPlayListVideoAdapter(
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
                FragmentPlayListVideoPagingSource(requireContext())
            }
            //分页加载数据
            lifecycleScope.launch {
                pager.flow.collect { pagingData ->
                    recyclerView_video_adapter.submitData(pagingData)
                }
            }
        }
        //音乐列表
        else if (flag == 2) {
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
        }

    }


}