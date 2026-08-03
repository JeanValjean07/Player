package com.suming.player.DataPack.MediaDataReader

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreRepo
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.SettingsRequestCenter

class MediaDataBaseReaderForVideo(private val context: Context) : PagingSource<Int, MediaItemForVideo>() {



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
            val sortOrder = SettingsRequestCenter.get_PREFS_video_sortMethod(context)
            val sortOrientation = SettingsRequestCenter.get_PREFS_video_sortOrientation(context)
            consoleLog("sortOrder: $sortOrder, sortOrientation: $sortOrientation")

            val sortMethod = "$sortOrder $sortOrientation"

            //按页获取数据
            val mediaStoreSettings = mediaStoreRepo.getVideosPagedByOrder(page, limit, sortMethod)

            //合成MediaItem
            val mediaItems = mediaStoreSettings
                .map { setting ->
                    MediaItemForVideo(
                        file_path = setting.file_path,
                        file_name = setting.file_name,
                        file_size = setting.file_size,
                        media_api_id = setting.media_api_id,
                        media_api_dateAdded = setting.media_api_dateAdded,
                        content_uriString = setting.content_uriString,
                        custom_media_Type = setting.custom_media_Type,
                        media_title = setting.media_title,
                        media_artist = setting.media_artist,
                        media_durationMs = setting.media_durationMs,
                        media_video_resolution = setting.media_video_resolution,
                        media_format = setting.media_format,
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


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "MediaDataBaseReaderForVideo: $msg")
        }
    }

}
