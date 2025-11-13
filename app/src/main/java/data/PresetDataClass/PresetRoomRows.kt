package data.PresetDataClass

object PresetRoomRows {


    const val PRESET_PREFS_BackgroundPlay = false
    const val PRESET_PREFS_LoopPlay = false
    const val PRESET_PREFS_TapJump = false
    const val PRESET_PREFS_LinkScroll = true
    const val PRESET_PREFS_AlwaysSeek = true
    const val PRESET_PREFS_VideoOnly = false
    const val PRESET_PREFS_SoundOnly = false
    const val PRESET_PREFS_PlaySpeed = 1.0f
    const val PRESET_PREFS_SavePositionWhenExit = false
    const val PRESET_SaveState_ExitPosition = 0L
    const val PRESET_SaveFlag_Thumb = "00000000000000000000"
    const val PRESET_SavePath_Cover = ""
    const val PRESET_PREFS_Hide = false


    data class Preset(
        val PREFS_BackgroundPlay: Boolean = PRESET_PREFS_BackgroundPlay,
        val PREFS_LoopPlay: Boolean = PRESET_PREFS_LoopPlay,
        val PREFS_TapJump: Boolean = PRESET_PREFS_TapJump,
        val PREFS_LinkScroll: Boolean = PRESET_PREFS_LinkScroll,
        val PREFS_AlwaysSeek: Boolean = PRESET_PREFS_AlwaysSeek,
        val PREFS_VideoOnly: Boolean = PRESET_PREFS_VideoOnly,
        val PREFS_SoundOnly: Boolean = PRESET_PREFS_SoundOnly,
        val PREFS_PlaySpeed: Float = PRESET_PREFS_PlaySpeed,
        val PREFS_SavePositionWhenExit: Boolean = PRESET_PREFS_SavePositionWhenExit,
        val SaveState_ExitPosition: Long = PRESET_SaveState_ExitPosition,
        val SaveFlag_Thumb: String = PRESET_SaveFlag_Thumb,
        val SavePath_Cover: String = PRESET_SavePath_Cover,
        val PREFS_Hide: Boolean = PRESET_PREFS_Hide,
    )


}