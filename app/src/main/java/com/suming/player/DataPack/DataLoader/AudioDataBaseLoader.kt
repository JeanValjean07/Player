package com.suming.player.DataPack.DataLoader

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForAudio
import com.suming.player.SettingsRequestCenter

class AudioDataBaseLoader(
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
            //排序字段合成
            val sortOrder = SettingsRequestCenter.get_PREFS_audio_sortMethod(context)
            val sortOrientation = SettingsRequestCenter.get_PREFS_audio_sortOrientation(context)
            val sortMethod = "$sortOrder $sortOrientation"

            //按页获取数据
            val musicStoreSettings = musicStoreRepo.getMusicsPagedByOrder(page, limit, sortMethod)

            //合成MediaItem
            val musicItems = musicStoreSettings.map { setting ->
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

                        media_audio_bitrate = setting.media_audio_bitrate,
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

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "AudioDataBaseLoader: $msg")
        }
    }

}
