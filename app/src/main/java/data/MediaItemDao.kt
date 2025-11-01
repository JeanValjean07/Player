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


    @Query("UPDATE video_settings SET SavePath_Cover = :newValue")
    suspend fun removeAllThumbPath(newValue: String)

    @Query("UPDATE video_settings SET PREFS_Hide = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun HideVideo(videoId: String,newValue1: Boolean)


    @Delete
    suspend fun delete(item: MediaItemSetting)
}