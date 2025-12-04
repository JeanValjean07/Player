package data.DataBaseMediaStore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaStoreDao {

    //插入或更新单个
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaStoreSetting)

    //批量插入或更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<MediaStoreSetting>)

    //根据URI查找单个视频
    @Query("SELECT * FROM MediaStore WHERE MARK_Uri_numOnly = :path LIMIT 1")
    suspend operator fun get(path: String): MediaStoreSetting?

    //获取所有视频(包括隐藏的)
    @Query("SELECT * FROM MediaStore")
    suspend fun getAllVideos(): List<MediaStoreSetting>

    //居然不支持动态传入排序
    @Query("SELECT * FROM MediaStore ORDER BY info_title ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByTitleAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_title DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByTitleDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_date_added ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_date_added DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDateAddedDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_duration ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_duration DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByDurationDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_file_size ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_file_size DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFileSizeDesc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_format ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFormatAsc(limit: Int, offset: Int): List<MediaStoreSetting>
    @Query("SELECT * FROM MediaStore ORDER BY info_format DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllVideosPagedByFormatDesc(limit: Int, offset: Int): List<MediaStoreSetting>

    //根据URI获取隐藏状态
    @Query("SELECT info_is_hidden FROM MediaStore WHERE MARK_Uri_numOnly = :uriNumOnly LIMIT 1")
    suspend fun getHideStatus(uriNumOnly: String): Boolean?

    //根据标题搜索视频
    @Query("SELECT * FROM MediaStore WHERE info_title LIKE '%' || :searchQuery || '%'")
    suspend fun searchVideos(searchQuery: String): List<MediaStoreSetting>

    //更新视频的隐藏状态
    @Query("UPDATE MediaStore SET info_is_hidden = :isHidden WHERE MARK_Uri_numOnly = :uriNumOnly")
    suspend fun updateHiddenStatus(uriNumOnly: String, isHidden: Boolean)

    //获取视频总数
    @Query("SELECT COUNT(*) FROM MediaStore")
    suspend fun getTotalVideoCount(): Int

    //删除单个视频
    @Delete
    suspend fun delete(item: MediaStoreSetting)

    //清空整个表
    @Query("DELETE FROM MediaStore")
    suspend fun clearAll()




}