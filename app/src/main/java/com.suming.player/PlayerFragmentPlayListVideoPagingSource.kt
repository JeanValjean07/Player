package com.suming.player

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo

@UnstableApi
class PlayerFragmentPlayListVideoPagingSource(
    private val context: Context,
) : PagingSource<Int, MediaItemForVideo>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItemForVideo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemForVideo> {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize
            //读取数据库
            val mediaStoreRepo = MediaStoreRepo.get(context)
            val totalCount = mediaStoreRepo.getTotalCount()
            //排序字段
            var sortOrder: String
            var sortOrientation: String

            //按页获取数据
            val mediaStoreSettings = mediaStoreRepo.getVideosPagedByOrder(page, limit, "info_title DESC")

            //合成MediaItem
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        id = setting.MARK_Uri_numOnly.toLongOrNull() ?: 0,
                        uri = setting.info_uri_full.toUri(),
                        name = setting.info_title,
                        durationMs = setting.info_duration,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
                        isHidden = setting.info_is_hidden
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
