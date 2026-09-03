package com.suming.player.DataPack.DataBaseMediaSingleSetting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaItemSetting")
data class MediaItemDataClass(
    @PrimaryKey
    //唯一ID使用 URI_S_FP 承担
    val uniqueID_URI_S_FP: String,
    //单个媒体的可独立设置项
    val INFO_MediaType: String = "",
    val PREFS_BackgroundPlay: Boolean = false,
    val PREFS_TapJump : Boolean = false,
    val PREFS_LinkScroll : Boolean = true,
    val PREFS_AlwaysSeek : Boolean = true,
    val PREFS_VideoOnly: Boolean = false,
    val PREFS_SoundOnly: Boolean = false,
    val PREFS_PlaySpeed: Float = 1.0f,
    val PREFS_SaveProgress: Boolean = false,
    val State_LastPosition: Long = 0L,
    val PREFS_Hide: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MediaItemDataClass
        return uniqueID_URI_S_FP == other.uniqueID_URI_S_FP &&
                INFO_MediaType == other.INFO_MediaType &&
                PREFS_BackgroundPlay == other.PREFS_BackgroundPlay &&
                PREFS_TapJump == other.PREFS_TapJump &&
                PREFS_LinkScroll == other.PREFS_LinkScroll &&
                PREFS_AlwaysSeek == other.PREFS_AlwaysSeek &&
                PREFS_VideoOnly == other.PREFS_VideoOnly &&
                PREFS_SoundOnly == other.PREFS_SoundOnly &&
                PREFS_PlaySpeed == other.PREFS_PlaySpeed &&
                PREFS_SaveProgress == other.PREFS_SaveProgress &&
                State_LastPosition == other.State_LastPosition &&
                PREFS_Hide == other.PREFS_Hide
    }

    override fun hashCode(): Int {
        return uniqueID_URI_S_FP.hashCode()
    }

}