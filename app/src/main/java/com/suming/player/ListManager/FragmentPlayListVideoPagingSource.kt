package com.suming.player.ListManager

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo

@UnstableApi
class FragmentPlayListVideoPagingSource(
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
                        id = setting.MARK_ID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_ID.toLongOrNull() ?: 0,
                        filename = setting.info_filename,
                        title = setting.info_title,
                        artist = setting.info_artist,
                        durationMs = setting.info_duration,
                        //视频专属
                        res = setting.info_resolution,
                        //其他
                        path = setting.info_path,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
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
