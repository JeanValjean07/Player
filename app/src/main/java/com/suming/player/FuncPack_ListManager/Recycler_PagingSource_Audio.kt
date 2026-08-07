package com.suming.player.FuncPack_ListManager

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForAudio

@UnstableApi
@Suppress("unused")
class Recycler_PagingSource_Audio(
    private val context: Context,
) : PagingSource<Int, MediaItemFullForAudio>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItemFullForAudio>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemFullForAudio> {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize
            //读取数据库
            val musicStoreRepo = AudioRepo.get(context)
            val totalCount = musicStoreRepo.getTotalMusicCount()
            //排序字段
            var sortOrder: String
            var sortOrientation: String

            //按页获取数据
            val musicStoreSettings = musicStoreRepo.getMusicsPagedByOrder(page, limit, "info_title DESC")

            //合成MediaItem
            val musicItems = musicStoreSettings
                .map { setting ->
                    MediaItemFullForAudio(
                        media_api_SPECIFIC_ID = setting.media_api_SPECIFIC_ID,
                        media_api_NUM_ID = setting.media_api_NUM_ID,
                        media_api_dateAdded = setting.media_api_dateAdded,
                        media_SPECIFIC_MediaType = setting.media_SPECIFIC_MediaType,
                        content_uriString = setting.content_uriString,
                        file_path = setting.file_path,
                        file_name = setting.file_name,
                        file_size = setting.file_size,
                        media_title = setting.media_title,
                        media_artist = setting.media_artist,
                        media_durationMs = setting.media_durationMs,
                        media_format = setting.media_format,
                        //-----------------------------------
                        media_audio_bitrate = setting.media_audio_bitrate,
                        media_audio_album = setting.media_audio_album,
                        media_audio_albumId = setting.media_audio_albumId,
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
