package com.suming.player

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import data.DataBaseMediaStore.MediaStoreRepo
import data.DataBaseMusicStore.MusicStoreRepo
import data.MediaModel.MediaItemForMusic
import data.MediaModel.MediaItemForVideo

@UnstableApi
class PlayerFragmentPlayListMusicPagingSource(
    private val context: Context,
) : PagingSource<Int, MediaItemForMusic>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItemForMusic>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemForMusic> {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize
            //读取数据库
            val musicStoreRepo = MusicStoreRepo.get(context)
            val totalCount = musicStoreRepo.getTotalMusicCount()
            //排序字段
            var sortOrder: String
            var sortOrientation: String

            //按页获取数据
            val musicStoreSettings = musicStoreRepo.getMusicsPagedByOrder(page, limit, "info_title DESC")

            //合成MediaItem
            val musicItems = musicStoreSettings
                .map { setting ->
                    MediaItemForMusic(
                        id = setting.MARK_ID.toLongOrNull() ?: 0,
                        uriString = setting.info_uri_string,
                        uriNumOnly = setting.MARK_ID.toLongOrNull() ?: 0,
                        filename = setting.info_filename,
                        title = setting.info_title,
                        artist = setting.info_artist,
                        durationMs = setting.info_duration,
                        //音频专属
                        albumId = setting.info_album_id,
                        album = setting.info_album,
                        //其他
                        path = setting.info_path,
                        sizeBytes = setting.info_file_size,
                        dateAdded = setting.info_date_added,
                        format = setting.info_format,
                    )
                }
                //计算下页键
            val nextKey = if ((page * limit) + musicItems.size < totalCount) page + 1 else null

            return LoadResult.Page(
                data = musicItems,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        }
        catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

}
