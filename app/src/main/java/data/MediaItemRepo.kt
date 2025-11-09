package data

import android.content.Context

class MediaItemRepo private constructor(context: Context) {

    private val dao = MediaItemDataBase.get(context).mediaItemDao()

    suspend fun saveSetting(item: MediaItemSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MediaItemSetting? = dao[path]

    suspend fun update_PREFS_HideThisItem(filename: String,flag_need_hide: Boolean) = dao.update_PREFS_HideThisItem(filename,flag_need_hide)

    suspend fun update_PREFS_AlwaysSeek(filename: String,flag_need_always_seek: Boolean) = dao.update_PREFS_AlwaysSeek(filename,flag_need_always_seek)

    suspend fun update_PREFS_LinkScroll(filename: String,flag_need_link_scroll: Boolean) = dao.update_PREFS_LinkScroll(filename,flag_need_link_scroll)

    suspend fun update_PREFS_TapJump(filename: String,flag_need_tap_jump: Boolean) = dao.update_PREFS_TapJump(filename,flag_need_tap_jump)

    suspend fun update_PREFS_VideoOnly(filename: String,flag_need_video_only: Boolean) = dao.update_PREFS_VideoOnly(filename,flag_need_video_only)

    suspend fun update_PREFS_SoundOnly(filename: String,flag_need_sound_only: Boolean) = dao.update_PREFS_SoundOnly(filename,flag_need_sound_only)

    suspend fun update_State_PositionWhenExit(filename: String,position_when_exit: Long) = dao.update_State_PositionWhenExit(filename,position_when_exit)

    suspend fun update_Flag_SavedThumbPos(filename: String,flag_saved_thumb: String) = dao.update_Flag_SavedThumbPos(filename,flag_saved_thumb)

    suspend fun update_PREFS_SavePositionWhenExit(filename: String,flag_save_position_when_exit: Boolean) = dao.update_PREFS_SavePositionWhenExit(filename,flag_save_position_when_exit)


    suspend fun preset_Flag_SavedThumbPos(filename: String) = dao.preset_Flag_SavedThumbPos(filename)



    companion object {
        @Volatile
        private var INSTANCE: MediaItemRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MediaItemRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
}