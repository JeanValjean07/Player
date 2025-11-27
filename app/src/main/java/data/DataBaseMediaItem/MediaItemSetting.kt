package data.DataBaseMediaItem

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaItemSetting")
data class MediaItemSetting(
    @PrimaryKey
    val MARK_FileName: String,
    val PREFS_BackgroundPlay: Boolean = false,
    val PREFS_LoopPlay: Boolean = false,
    val PREFS_TapJump : Boolean = false,
    val PREFS_LinkScroll : Boolean = true,
    val PREFS_AlwaysSeek : Boolean = true,
    val PREFS_VideoOnly: Boolean = false,
    val PREFS_SoundOnly: Boolean = false,
    val PREFS_PlaySpeed: Float = 1.0f,
    val PREFS_SavePositionWhenExit: Boolean = false,
    val SaveState_ExitPosition: Long = 0L,
    val SavePath_Cover: String = "",
    val PREFS_Hide: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MediaItemSetting
        return MARK_FileName == other.MARK_FileName &&
                PREFS_BackgroundPlay == other.PREFS_BackgroundPlay &&
                PREFS_LoopPlay == other.PREFS_LoopPlay &&
                PREFS_TapJump == other.PREFS_TapJump &&
                PREFS_LinkScroll == other.PREFS_LinkScroll &&
                PREFS_AlwaysSeek == other.PREFS_AlwaysSeek &&
                PREFS_VideoOnly == other.PREFS_VideoOnly &&
                PREFS_SoundOnly == other.PREFS_SoundOnly &&
                PREFS_PlaySpeed == other.PREFS_PlaySpeed &&
                PREFS_SavePositionWhenExit == other.PREFS_SavePositionWhenExit &&
                SaveState_ExitPosition == other.SaveState_ExitPosition &&
                SavePath_Cover == other.SavePath_Cover &&
                PREFS_Hide == other.PREFS_Hide
    }

    //修改数据库结构时记得同步修改预置数据类

    override fun hashCode(): Int {
        return MARK_FileName.hashCode()
    }
}