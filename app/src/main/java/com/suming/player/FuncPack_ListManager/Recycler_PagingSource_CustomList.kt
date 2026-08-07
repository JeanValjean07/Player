package com.suming.player.FuncPack_ListManager

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.DataPack.DataClassForPlay.MediaItemForList

@UnstableApi
@Suppress("unused")
class Recycler_PagingSource_CustomList(
    private val context: Context,
) : PagingSource<Int, MediaItemForList>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItemForList>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemForList> {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize


            //按页获取数据
            val MiniMediaItems = ListManagerHelper.ListContent_CustomList
            val totalCount = ListManagerHelper.ListContent_CustomList.size

            //合成MediaItem
            val mediaItems = MiniMediaItems
                .map { item ->
                    MediaItemForList(
                        media_api_SPECIFIC_ID = item.type,
                        media_api_NUM_ID = 114514L,
                        media_api_dateAdded = 114514L,
                        media_SPECIFIC_MediaType = "",
                        content_uriString = "",
                        file_path = "",
                        file_name = "",
                        file_size = 0,
                        media_title = "",
                        media_artist = "",
                        media_durationMs = 0L,

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

