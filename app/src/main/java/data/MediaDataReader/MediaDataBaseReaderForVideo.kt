package data.MediaDataReader

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo

class MediaDataBaseReaderForVideo(
    private val context: Context,
) : PagingSource<Int, MediaItemForVideo>() {
    //设置和设置项
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_showHideItems = false


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
            //读取媒体库设置
            PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
            if (PREFS_MediaStore.contains("PREFS_showHideItems")){
                PREFS_showHideItems = PREFS_MediaStore.getBoolean("PREFS_showHideItems", false)
            }else{
                PREFS_MediaStore.edit { putBoolean("PREFS_showHideItems", false).apply() }
            }
            if (PREFS_MediaStore.contains("PREFS_video_sortOrder")){
                sortOrder = PREFS_MediaStore.getString("PREFS_video_sortOrder", "info_title") ?: "info_title"
            }else{
                sortOrder = "info_title"
                PREFS_MediaStore.edit { putString("PREFS_video_sortOrder", "info_title").apply() }
            }
            if (PREFS_MediaStore.contains("PREFS_video_sortOrientation")){
                sortOrientation = PREFS_MediaStore.getString("PREFS_video_sortOrientation", "DESC") ?: "DESC"
            }else{
                sortOrientation = "DESC"
                PREFS_MediaStore.edit { putString("PREFS_video_sortOrientation", "DESC").apply() }
            }
            val sortMethod = "$sortOrder $sortOrientation"

            //按页获取数据
            val mediaStoreSettings = mediaStoreRepo.getVideosPagedByOrder(page, limit, sortMethod)

            //合成MediaItem
            val mediaItems = mediaStoreSettings
                .filter { setting ->
                    PREFS_showHideItems || !setting.info_is_hidden
                }
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
