package data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_settings")
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
    val SaveFlag_Thumb: Long = 0L,
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
                SaveFlag_Thumb == other.SaveFlag_Thumb
    }



    override fun hashCode(): Int {
        return MARK_FileName.hashCode()
    }
}