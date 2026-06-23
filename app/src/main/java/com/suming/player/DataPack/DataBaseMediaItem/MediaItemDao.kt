package com.suming.player.DataPack.DataBaseMediaItem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemSetting)

    @Delete
    suspend fun delete(item: MediaItemSetting)

    @Query("SELECT * FROM MediaItemSetting WHERE MARK_UniqueID = :path LIMIT 1")
    suspend operator fun get(path: String): MediaItemSetting?

    //媒体类型
    @Query("UPDATE MediaItemSetting SET INFO_MediaType = :newValue WHERE MARK_UniqueID = :videoId")
    suspend fun update_INFO_MediaType(videoId: String,newValue: String)
    @Query("SELECT INFO_MediaType FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_INFO_MediaType(videoId: String): String

    //后台播放
    @Query("UPDATE MediaItemSetting SET PREFS_BackgroundPlay = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_BackgroundPlay(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_BackgroundPlay FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_BackgroundPlay(videoId: String): Boolean
    //循环播放
    @Query("UPDATE MediaItemSetting SET PREFS_LoopPlay = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_LoopPlay(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_LoopPlay FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_LoopPlay(videoId: String): Boolean


    //AlwaysSeek
    @Query("UPDATE MediaItemSetting SET PREFS_AlwaysSeek = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_AlwaysSeek(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_AlwaysSeek FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_AlwaysSeek(videoId: String): Boolean
    //LinkScroll
    @Query("UPDATE MediaItemSetting SET PREFS_LinkScroll = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_LinkScroll(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_LinkScroll FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_LinkScroll(videoId: String): Boolean
    //TapJump
    @Query("UPDATE MediaItemSetting SET PREFS_TapJump = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_TapJump(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_TapJump FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_TapJump(videoId: String): Boolean

    //仅播视频
    @Query("UPDATE MediaItemSetting SET PREFS_VideoOnly = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_VideoOnly(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_VideoOnly FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_VideoOnly(videoId: String): Boolean
    //仅播音频
    @Query("UPDATE MediaItemSetting SET PREFS_SoundOnly = :newValue1 WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_SoundOnly(videoId: String,newValue1: Boolean)
    @Query("SELECT PREFS_SoundOnly FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_SoundOnly(videoId: String): Boolean

    //保存播放进度
    @Query("UPDATE MediaItemSetting SET PREFS_SaveProgress = :newValue WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_saveLastPosition(videoId: String,newValue: Boolean)
    @Query("SELECT PREFS_SaveProgress FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_saveLastPosition(videoId: String): Boolean
    //具体进度值
    @Query("UPDATE MediaItemSetting SET State_LastPosition = :newValue WHERE MARK_UniqueID = :videoId")
    suspend fun update_value_LastPosition(videoId: String,newValue: Long)
    @Query("SELECT State_LastPosition FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_value_LastPosition(videoId: String): Long

    //播放速度
    @Query("UPDATE MediaItemSetting SET PREFS_PlaySpeed = :newValue WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_PlaySpeed(videoId: String,newValue: Float)
    @Query("SELECT PREFS_PlaySpeed FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_PlaySpeed(videoId: String): Float

    //隐藏媒体
    @Query("UPDATE MediaItemSetting SET PREFS_Hide = :newValue WHERE MARK_UniqueID = :videoId")
    suspend fun update_PREFS_Hide(videoId: String,newValue: Boolean)
    @Query("SELECT PREFS_Hide FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun get_PREFS_Hide(videoId: String): Boolean

    //一次性全部读取
    @Query("SELECT * FROM MediaItemSetting WHERE MARK_UniqueID = :videoId LIMIT 1")
    suspend fun getMediaItemPack(videoId: String): MediaItemSetting?

    //快速预写所有字段为默认空值



}