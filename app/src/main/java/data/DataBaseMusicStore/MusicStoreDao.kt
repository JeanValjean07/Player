package data.DataBaseMusicStore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface MusicStoreDao {

    //插入或更新单个
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MusicStoreSetting)

    //批量插入或更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<MusicStoreSetting>)

    //根据URI查找单个视频
    @Query("SELECT * FROM MusicStore WHERE MARK_ID = :path LIMIT 1")
    suspend operator fun get(path: String): MusicStoreSetting?

    //获取所有视频(包括隐藏的)
    @Query("SELECT * FROM MusicStore")
    suspend fun getAllMusics(): List<MusicStoreSetting>

    //分页+排序
    @Query("SELECT * FROM MusicStore ORDER BY info_title ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByTitleAsc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_title DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByTitleDesc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_date_added ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDateAddedAsc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_date_added DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDateAddedDesc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_duration ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDurationAsc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_duration DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDurationDesc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_file_size ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileSizeAsc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_file_size DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileSizeDesc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_format ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByMimeTypeAsc(limit: Int, offset: Int): List<MusicStoreSetting>
    @Query("SELECT * FROM MusicStore ORDER BY info_format DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByMimeTypeDesc(limit: Int, offset: Int): List<MusicStoreSetting>

    //根据排序方法获取所有视频
    @RawQuery(observedEntities = [MusicStoreSetting::class])
    suspend fun getAllMusicsSorted(query: SupportSQLiteQuery): List<MusicStoreSetting>



    //根据标题搜索视频
    @Query("SELECT * FROM MusicStore WHERE info_title LIKE '%' || :searchQuery || '%'")
    suspend fun searchMusics(searchQuery: String): List<MusicStoreSetting>

    //获取视频总数
    @Query("SELECT COUNT(*) FROM MusicStore")
    suspend fun getTotalMusicCount(): Int

    //删除单个视频
    @Delete
    suspend fun delete(item: MusicStoreSetting)

    //清空整个表
    @Query("DELETE FROM MusicStore")
    suspend fun clearAll()




}