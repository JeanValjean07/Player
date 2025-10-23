package data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemSetting)

    @Query("SELECT * FROM video_settings WHERE MARK_FileName = :path LIMIT 1")
    suspend operator fun get(path: String): MediaItemSetting?




    @Delete
    suspend fun delete(item: MediaItemSetting)
}