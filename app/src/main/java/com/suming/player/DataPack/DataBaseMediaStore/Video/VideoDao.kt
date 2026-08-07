package com.suming.player.DataPack.DataBaseMediaStore.Video

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: VideoDataClass)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<VideoDataClass>)



    //根据 SPECIFIC_ID 查找单个视频项
    @Query("SELECT * FROM tableVideoList WHERE media_api_NUM_ID = :NUM_ID LIMIT 1")
    suspend operator fun get(NUM_ID: Long): VideoDataClass?

    //获取所有视频项
    @Query("SELECT * FROM tableVideoList")
    suspend fun getAllVideos(): List<VideoDataClass>


    //分页和排序查询
    @Query("SELECT * FROM tableVideoList ORDER BY file_name ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileNameAsc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY file_name DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileNameDesc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_api_dateAdded ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedAsc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_api_dateAdded DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedDesc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_durationMs ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationAsc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_durationMs DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationDesc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY file_size ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeAsc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY file_size DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeDesc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_format ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByMimeTypeAsc(limit: Int, offset: Int): List<VideoDataClass>
    @Query("SELECT * FROM tableVideoList ORDER BY media_format DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByMimeTypeDesc(limit: Int, offset: Int): List<VideoDataClass>


    //根据排序方法获取所有视频项(未使用)(分页下不支持)
    @RawQuery(observedEntities = [VideoDataClass::class])
    suspend fun getAllVideosSorted(query: SupportSQLiteQuery): List<VideoDataClass>


    //根据文件名 file_name 搜索视频项
    @Query("SELECT * FROM tableVideoList WHERE file_name LIKE '%' || :searchQuery || '%'")
    suspend fun searchVideos(searchQuery: String): List<VideoDataClass>

    //获取视频项总数
    @Query("SELECT COUNT(*) FROM tableVideoList")
    suspend fun getTotalVideoCount(): Int

    //删除单个视频项
    @Delete
    suspend fun delete(item: VideoDataClass)

    //清空整个表
    @Query("DELETE FROM tableVideoList")
    suspend fun clearAll()

    //检查该表是否为空
    @Query("SELECT COUNT(*) FROM tableVideoList")
    suspend fun getCount(): Int



}