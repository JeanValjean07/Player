package data.DataBaseMediaItem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.PresetDataClass.PresetRoomRows

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemSetting)

    @Query("SELECT * FROM MediaItemSetting WHERE MARK_FileName = :path LIMIT 1")
    suspend operator fun get(path: String): MediaItemSetting?

    //快速修改单个位
    @Query("UPDATE MediaItemSetting SET SavePath_Cover = :newValue")
    suspend fun removeAllThumbPath(newValue: String)

    @Query("UPDATE MediaItemSetting SET PREFS_Hide = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_HideThisItem(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_AlwaysSeek = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_AlwaysSeek(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_LinkScroll = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_LinkScroll(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_TapJump = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_TapJump(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_VideoOnly = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_VideoOnly(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_SoundOnly = :newValue1 WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_SoundOnly(videoId: String,newValue1: Boolean)

    @Query("UPDATE MediaItemSetting SET PREFS_SavePositionWhenExit = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_PREFS_SavePositionWhenExit(videoId: String,newValue: Boolean)

    @Query("UPDATE MediaItemSetting SET SaveState_ExitPosition = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_State_PositionWhenExit(videoId: String,newValue: Long)

    @Query("UPDATE MediaItemSetting SET SavePath_Cover = :newValue WHERE MARK_FileName = :videoId")
    suspend fun update_cover_path(videoId: String,newValue: String)


    //快速预写
    @Query("""UPDATE MediaItemSetting SET
            PREFS_BackgroundPlay = :PREFS_BackgroundPlay,
            PREFS_LoopPlay = :PREFS_LoopPlay,
            PREFS_TapJump = :PREFS_TapJump,
            PREFS_LinkScroll = :PREFS_LinkScroll,
            PREFS_AlwaysSeek = :PREFS_AlwaysSeek,
            PREFS_VideoOnly = :PREFS_VideoOnly,
            PREFS_SoundOnly = :PREFS_SoundOnly,
            PREFS_PlaySpeed = :PREFS_PlaySpeed,
            PREFS_SavePositionWhenExit = :PREFS_SavePositionWhenExit,
            SaveState_ExitPosition = :SaveState_ExitPosition,
            SavePath_Cover = :SavePath_Cover,
            PREFS_Hide = :PREFS_Hide
            WHERE MARK_FileName = :MARK_FileName
    """)
    suspend fun preset_all_row(
        MARK_FileName: String, PREFS_BackgroundPlay: Boolean, PREFS_LoopPlay: Boolean, PREFS_TapJump : Boolean, PREFS_LinkScroll : Boolean, PREFS_AlwaysSeek : Boolean,
        PREFS_VideoOnly: Boolean, PREFS_SoundOnly: Boolean, PREFS_PlaySpeed: Float, PREFS_SavePositionWhenExit: Boolean, SaveState_ExitPosition: Long,
        SavePath_Cover: String, PREFS_Hide: Boolean
    )
    suspend fun preset_all_row_default(MARK_FileName: String){
        with(PresetRoomRows.Preset()){
            preset_all_row(
                    MARK_FileName,
                    PREFS_BackgroundPlay,
                    PREFS_LoopPlay,
                    PREFS_TapJump,
                    PREFS_LinkScroll,
                    PREFS_AlwaysSeek,
                    PREFS_VideoOnly,
                    PREFS_SoundOnly,
                    PREFS_PlaySpeed,
                    PREFS_SavePositionWhenExit,
                    SaveState_ExitPosition,
                    SavePath_Cover,
                    PREFS_Hide
                )
        }
    }
    suspend fun preset_all_row_without_cover_path(MARK_FileName: String, savePathCover: String)
    { with(PresetRoomRows.Preset()) {
            preset_all_row(
                MARK_FileName,
                PREFS_BackgroundPlay,
                PREFS_LoopPlay,
                PREFS_TapJump,
                PREFS_LinkScroll,
                PREFS_AlwaysSeek,
                PREFS_VideoOnly,
                PREFS_SoundOnly,
                PREFS_PlaySpeed,
                PREFS_SavePositionWhenExit,
                SaveState_ExitPosition,
                savePathCover,
                PREFS_Hide
            )
        } }

    //快速读取
    @Query("SELECT SavePath_Cover FROM MediaItemSetting WHERE MARK_FileName = :filename LIMIT 1")
    suspend fun get_saved_cover_path(filename: String): String?








    @Delete
    suspend fun delete(item: MediaItemSetting)
}