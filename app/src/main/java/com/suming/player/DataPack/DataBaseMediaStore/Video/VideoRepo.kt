package com.suming.player.DataPack.DataBaseMediaStore.Video

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreDataBase
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoDataClass
import com.suming.player.SettingsRequestCenter

class VideoRepo(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VideoRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                VideoRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
    //关联Dao层
    private val dao = MediaStoreDataBase.get(context).VideoTableDao()




    //保存单个视频项信息
    suspend fun saveVideoItem(video: VideoDataClass) = dao.insertOrUpdate(video)
    //批量保存视频项信息
    suspend fun saveVideoItems(videos: List<VideoDataClass>) = dao.insertOrUpdateAll(videos)

    //获取单个视频项信息
    suspend fun getVideoItem(media_api_NUM_ID: Long): VideoDataClass? = dao[media_api_NUM_ID]
    //获取所有视频项信息
    suspend fun getAllVideoItems(): List<VideoDataClass> = dao.getAllVideos()



    //根据排序方式读取,不支持动态传入排序参数,全部列出
    suspend fun getVideosPagedByOrder(page: Int, pageSize: Int, sortOrder: String): List<VideoDataClass> {
        val offset = page * pageSize

        return when (sortOrder) {
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllVideosPagedByFileNameAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllVideosPagedByFileNameDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_ASC}"  -> dao.getAllVideosPagedByDateAddedAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_DESC}"  -> dao.getAllVideosPagedByDateAddedDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_ASC}"  -> dao.getAllVideosPagedByDurationAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_DESC}"  -> dao.getAllVideosPagedByDurationDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_ASC}"  -> dao.getAllVideosPagedByFileSizeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_DESC}"  -> dao.getAllVideosPagedByFileSizeDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_ASC}"  -> dao.getAllVideosPagedByMimeTypeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_DESC}"  -> dao.getAllVideosPagedByMimeTypeDesc(pageSize, offset)
            else -> dao.getAllVideosPagedByMimeTypeDesc(pageSize, offset)

        }
    }



    //搜索视频
    suspend fun searchVideos(query: String): List<VideoDataClass> = dao.searchVideos(query)

    //获取视频总数
    suspend fun getTotalCount(): Int = dao.getTotalVideoCount()
    //检查该库是否为空
    suspend fun isEmpty(): Boolean = dao.getCount() == 0

    //删除视频项
    suspend fun deleteVideoItem(video: VideoDataClass) = dao.delete(video)
    //清空所有视频项
    suspend fun clearAll(): Int = dao.clearAll()



    //未使用方法
    //根据排序方法获取所有视频(不可用:分页情况下不支持)
    suspend fun getAllVideosSortedEnhanced(sortField: String = "info_title", sortOrientation: String = "ASC"): List<VideoDataClass> {
        //防注入白名单
        val safeField = when (sortField) {
            SettingsRequestCenter.sort_method_filename,
            SettingsRequestCenter.sort_method_date_added,
            SettingsRequestCenter.sort_method_file_size,
            SettingsRequestCenter.sort_method_duration,
            SettingsRequestCenter.sort_method_mime_type -> sortField
            else -> SettingsRequestCenter.sort_method_date_added

        }
        val safeOrder = when (sortOrientation) {
            SettingsRequestCenter.sort_orientation_ASC, SettingsRequestCenter.sort_orientation_DESC -> sortOrientation
            else -> SettingsRequestCenter.sort_orientation_ASC
        }

        val sql = "SELECT * FROM MediaStore ORDER BY $safeField $safeOrder"
        val query = SimpleSQLiteQuery(sql)
        return dao.getAllVideosSorted(query)
    }
    suspend fun getAllVideosSorted( sortOrder: String, sortOrientation: String ): List<VideoDataClass> {
        //白名单防注入
        val safeField = when (sortOrder) {
            "info_title", "info_date_added", "info_file_size", "info_duration", "info_mime_type" -> sortOrder
            else -> "info_title"
        }
        val safeOrder = when (sortOrientation) {
            "ASC", "DESC" -> sortOrientation
            else -> "ASC"
        }
        val sql = "SELECT * FROM MediaStore ORDER BY $safeField $safeOrder"
        val query = SimpleSQLiteQuery(sql)
        return dao.getAllVideosSorted(query)
    }


}