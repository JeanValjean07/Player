package com.suming.player.DataPack.DataBaseMediaStore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface MediaStoreDao {

    //插入或更新单个
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaStoreSetting)

    //批量插入或更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<MediaStoreSetting>)

    //根据URI查找单个视频
    @Query("SELECT * FROM MediaStore WHERE file_path = :path LIMIT 1")
    suspend operator fun get(path: String): MediaStoreSetting?

    //获取所有视频(包括隐藏的)
    @Query("SELECT * FROM MediaStore")
    suspend fun getAllVideos(): List<MediaStoreSetting>

    //分页+排序
    @Query("SELECT * FROM MediaStore ORDER BY media_title ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByTitleAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_title DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByTitleDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_api_dateAdded ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_api_dateAdded DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_durationMs ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_durationMs DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY file_size ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY file_size DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_format ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByMimeTypeAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY media_format DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByMimeTypeDesc(limit: Int, offset: Int): List<MediaStoreSetting>

    //根据排序方法获取所有视频
    @RawQuery(observedEntities = [MediaStoreSetting::class])
    suspend fun getAllVideosSorted(query: SupportSQLiteQuery): List<MediaStoreSetting>


    //根据文件名搜索视频
    @Query("SELECT * FROM MediaStore WHERE file_name LIKE '%' || :searchQuery || '%'")
    suspend fun searchVideos(searchQuery: String): List<MediaStoreSetting>

    //获取视频总数
    @Query("SELECT COUNT(*) FROM MediaStore")
    suspend fun getTotalVideoCount(): Int

    //删除单个视频
    @Delete
    suspend fun delete(item: MediaStoreSetting)

    //清空整个表
    @Query("DELETE FROM MediaStore")
    suspend fun clearAll()

    //检查该库是否为空
    @Query("SELECT COUNT(*) FROM MediaStore")
    suspend fun getCount(): Int



}