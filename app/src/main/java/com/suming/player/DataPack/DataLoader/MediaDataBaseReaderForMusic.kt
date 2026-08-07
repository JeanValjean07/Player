package com.suming.player.DataPack.DataLoader

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataClass.MediaItemFullForAudio
import com.suming.player.SettingsRequestCenter

class MediaDataBaseReaderForMusic(
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

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaDataBaseReaderForMusic-音频读取器-来自数据库缓存: $msg")
        }
    }

}
