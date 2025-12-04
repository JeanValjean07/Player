package data.DataBaseMediaStore

import android.content.Context

class MediaStoreRepo private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MediaStoreRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MediaStoreRepo(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val dao = MediaStoreDataBase.get(context).mediaStoreDao()

    suspend fun saveSetting(item: MediaStoreSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MediaStoreSetting? = dao[path]


    suspend fun insertOrUpdateAll(items: List<MediaStoreSetting>) = dao.insertOrUpdateAll(items)

    //保存单个视频信息
    suspend fun saveVideo(video: MediaStoreSetting) = dao.insertOrUpdate(video)

    //批量保存视频信息
    suspend fun saveAllVideos(videos: List<MediaStoreSetting>) = dao.insertOrUpdateAll(videos)

    //获取单个视频信息
    suspend fun getVideo(uriNumOnly: String): MediaStoreSetting? = dao[uriNumOnly]

    //获取所有视频信息
    suspend fun getAllVideos(): List<MediaStoreSetting> = dao.getAllVideos()

    //根据排序方式读取,不支持动态传入排序参数,全部列出
    suspend fun getVideosPagedByOrder(page: Int, pageSize: Int, sortOrder: String): List<MediaStoreSetting> {
        val offset = page * pageSize

        return when (sortOrder) {
            "info_title ASC" -> dao.getAllVideosPagedByTitleAsc(pageSize, offset)
            "info_title DESC" -> dao.getAllVideosPagedByTitleDesc(pageSize, offset)
            "info_date_added ASC" -> dao.getAllVideosPagedByDateAddedAsc(pageSize, offset)
            "info_date_added DESC" -> dao.getAllVideosPagedByDateAddedDesc(pageSize, offset)
            "info_duration ASC" -> dao.getAllVideosPagedByDurationAsc(pageSize, offset)
            "info_duration DESC" -> dao.getAllVideosPagedByDurationDesc(pageSize, offset)
            "info_file_size ASC" -> dao.getAllVideosPagedByFileSizeAsc(pageSize, offset)
            "info_file_size DESC" -> dao.getAllVideosPagedByFileSizeDesc(pageSize, offset)
            "info_format ASC" -> dao.getAllVideosPagedByFormatAsc(pageSize, offset)
            "info_format DESC" -> dao.getAllVideosPagedByFormatDesc(pageSize, offset)
            else -> dao.getAllVideosPagedByTitleDesc(pageSize, offset)
        }
    }


    //搜索视频
    suspend fun searchVideos(query: String): List<MediaStoreSetting> = dao.searchVideos(query)

    //更新视频隐藏状态
    suspend fun getHideStatus(uriNumOnly: String): Boolean? = dao.getHideStatus(uriNumOnly)
    suspend fun updateHiddenStatus(uriNumOnly: String, isHidden: Boolean) = dao.updateHiddenStatus(uriNumOnly, isHidden)

    //获取视频总数
    suspend fun getTotalCount(): Int = dao.getTotalVideoCount()

    //删除视频
    suspend fun deleteVideo(video: MediaStoreSetting) = dao.delete(video)

    //清空所有数据
    suspend fun clearAll() = dao.clearAll()








}