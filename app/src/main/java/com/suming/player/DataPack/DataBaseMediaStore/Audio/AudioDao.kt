package com.suming.player.DataPack.DataBaseMediaStore.Audio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: AudioDataClass)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<AudioDataClass>)


    //根据 NUM_ID 查找单个音频项
    @Query("SELECT * FROM tableAudioList WHERE media_api_NUM_ID = :NUM_ID LIMIT 1")
    suspend operator fun get(NUM_ID: Long): AudioDataClass?

    //获取所有音频项
    @Query("SELECT * FROM tableAudioList")
    suspend fun getAllMusics(): List<AudioDataClass>



    //分页和排序查询
    @Query("SELECT * FROM tableAudioList ORDER BY file_name ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileNameAsc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY file_name DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileNameDesc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_api_dateAdded ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDateAddedAsc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_api_dateAdded DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDateAddedDesc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_durationMs ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDurationAsc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_durationMs DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByDurationDesc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY file_size ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileSizeAsc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY file_size DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByFileSizeDesc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_format ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByMimeTypeAsc(limit: Int, offset: Int): List<AudioDataClass>
    @Query("SELECT * FROM tableAudioList ORDER BY media_format DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMusicsPagedByMimeTypeDesc(limit: Int, offset: Int): List<AudioDataClass>

    //根据排序方法获取所有音频项(未使用)(分页下不支持)
    @RawQuery(observedEntities = [AudioDataClass::class])
    suspend fun getAllMusicsSorted(query: SupportSQLiteQuery): List<AudioDataClass>

    //根据标题搜索音频项
    @Query("SELECT * FROM tableAudioList WHERE file_name LIKE '%' || :searchQuery || '%'")
    suspend fun searchMusics(searchQuery: String): List<AudioDataClass>

    //获取音频项总数
    @Query("SELECT COUNT(*) FROM tableAudioList")
    suspend fun getTotalMusicCount(): Int

    //删除单个音频项
    @Delete
    suspend fun delete(item: AudioDataClass)

    //清空整个表
    @Query("DELETE FROM tableAudioList")
    suspend fun clearAll(): Int

    //检查该表是否为空
    @Query("SELECT COUNT(*) FROM tableAudioList")
    suspend fun getCount(): Int




}