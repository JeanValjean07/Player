package com.suming.player.FuncPack_ListManager

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState

@UnstableApi
@Suppress("unused")
class Recycler_PagingSource_CustomList(
    private val context: Context,
) : PagingSource<Int, MiniMediaItemForList>() {

    override fun getRefreshKey(state: PagingState<Int, MiniMediaItemForList>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MiniMediaItemForList> {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize


            //按页获取数据
            val MiniMediaItems = ListManagerHelper.ListContent_CustomList
            val totalCount = ListManagerHelper.ListContent_CustomList.size

            //合成MediaItem
            val mediaItems = MiniMediaItems
                .map { item ->
                    MiniMediaItemForList(
                        id = 114514,
                        uri = Uri.EMPTY,
                        uriNumOnly = 114514,
                        filename = "item.filename",
                        title = "item.title",
                        artist = "item.artist",
                        type = item.type,
                    )
                }
            //计算下页键
            val nextKey = if ((page * limit) + mediaItems.size < totalCount) page + 1 else null

            return LoadResult.Page(
                data = mediaItems,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        }
        catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

}

