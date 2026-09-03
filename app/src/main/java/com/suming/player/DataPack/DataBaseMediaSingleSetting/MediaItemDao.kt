package com.suming.player.DataPack.DataBaseMediaSingleSetting

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemDataClass)

    @Delete
    suspend fun delete(item: MediaItemDataClass)

    @Query("SELECT * FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :path LIMIT 1")
    suspend operator fun get(path: String): MediaItemDataClass?

    //检查数据库中是否有键值为uniqueID_URI_S_FP的项
    @Query("SELECT EXISTS(SELECT 1 FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :uniqueID_URI_S_FP)")
    suspend fun checkExist(uniqueID_URI_S_FP: String): Boolean
    //创建项
    suspend fun createMediaItem(uniqueID_URI_S_FP: String) = insertOrUpdate(MediaItemDataClass(uniqueID_URI_S_FP))



    //媒体类型(暂时不知道有什么用,先留着)
    @Query("UPDATE MediaItemSetting SET INFO_MediaType = :newValue WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_INFO_MediaType(media_id: String,newValue: String)
    @Query("SELECT INFO_MediaType FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_INFO_MediaType(media_id: String): String


    //后台播放
    @Query("UPDATE MediaItemSetting SET PREFS_BackgroundPlay = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_BackgroundPlay(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_BackgroundPlay FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_BackgroundPlay(media_id: String): Boolean


    //AlwaysSeek
    @Query("UPDATE MediaItemSetting SET PREFS_AlwaysSeek = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_AlwaysSeek(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_AlwaysSeek FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_AlwaysSeek(media_id: String): Boolean
    //LinkScroll
    @Query("UPDATE MediaItemSetting SET PREFS_LinkScroll = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_LinkScroll(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_LinkScroll FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_LinkScroll(media_id: String): Boolean
    //TapJump
    @Query("UPDATE MediaItemSetting SET PREFS_TapJump = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_TapJump(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_TapJump FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_TapJump(media_id: String): Boolean

    //仅播视频
    @Query("UPDATE MediaItemSetting SET PREFS_VideoOnly = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_VideoOnly(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_VideoOnly FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_VideoOnly(media_id: String): Boolean
    //仅播音频
    @Query("UPDATE MediaItemSetting SET PREFS_SoundOnly = :newValue1 WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_SoundOnly(media_id: String,newValue1: Boolean)
    @Query("SELECT PREFS_SoundOnly FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_SoundOnly(media_id: String): Boolean

    //保存播放进度
    @Query("UPDATE MediaItemSetting SET PREFS_SaveProgress = :newValue WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_saveLastPosition(media_id: String,newValue: Boolean)
    @Query("SELECT PREFS_SaveProgress FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_saveLastPosition(media_id: String): Boolean
    //具体进度值
    @Query("UPDATE MediaItemSetting SET State_LastPosition = :newValue WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_value_LastPosition(media_id: String,newValue: Long)
    @Query("SELECT State_LastPosition FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_value_LastPosition(media_id: String): Long

    //播放速度
    @Query("UPDATE MediaItemSetting SET PREFS_PlaySpeed = :newValue WHERE uniqueID_URI_S_FP = :media_id")
    suspend fun update_PREFS_PlaySpeed(media_id: String,newValue: Float)
    @Query("SELECT PREFS_PlaySpeed FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun get_PREFS_PlaySpeed(media_id: String): Float


    //一次性全部读取
    @Query("SELECT * FROM MediaItemSetting WHERE uniqueID_URI_S_FP = :media_id LIMIT 1")
    suspend fun getMediaItemPack(media_id: String): MediaItemDataClass?




}