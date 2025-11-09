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
    suspend fun update_PREFS_HideThisItem(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_AlwaysSeek = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_AlwaysSeek(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_LinkScroll = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_LinkScroll(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_TapJump = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_TapJump(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_VideoOnly = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_VideoOnly(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_SoundOnly = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_SoundOnly(videoId: String,newValue1: Boolean)

    @Query("UPDATE video_settings SET PREFS_SavePositionWhenExit = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_SavePositionWhenExit(videoId: String,newValue: Boolean)

    @Query("UPDATE video_settings SET SaveFlag_Thumb = \"00000000000000000000\" WHERE MARK_FileName = :videoId")
    suspend fun preset_Flag_SavedThumbPos(videoId: String)

    @Query("UPDATE video_settings SET SaveState_ExitPosition = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_State_PositionWhenExit(videoId: String,newValue: Long)

    @Query("UPDATE video_settings SET SaveFlag_Thumb = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_Flag_SavedThumbPos(videoId: String,newValue: String)







    @Delete
    suspend fun delete(item: MediaItemSetting)
}