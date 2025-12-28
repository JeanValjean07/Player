package data.DataBaseMediaItem

import android.content.Context

class MediaItemRepo private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MediaItemRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MediaItemRepo(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val dao = MediaItemDataBase.get(context).mediaItemDao()

    suspend fun saveSetting(item: MediaItemSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MediaItemSetting? = dao[path]




    suspend fun update_PREFS_AlwaysSeek(filename: String,flag_need_always_seek: Boolean) = dao.update_PREFS_AlwaysSeek(filename,flag_need_always_seek)

    suspend fun update_PREFS_LinkScroll(filename: String,flag_need_link_scroll: Boolean) = dao.update_PREFS_LinkScroll(filename,flag_need_link_scroll)

    suspend fun update_PREFS_TapJump(filename: String,flag_need_tap_jump: Boolean) = dao.update_PREFS_TapJump(filename,flag_need_tap_jump)

    suspend fun update_PREFS_VideoOnly(filename: String,flag_need_video_only: Boolean) = dao.update_PREFS_VideoOnly(filename,flag_need_video_only)
    suspend fun get_PREFS_VideoOnly(filename: String): Boolean = dao.get_PREFS_VideoOnly(filename)

    suspend fun update_PREFS_SoundOnly(filename: String,flag_need_sound_only: Boolean) = dao.update_PREFS_SoundOnly(filename,flag_need_sound_only)
    suspend fun get_PREFS_SoundOnly(filename: String): Boolean = dao.get_PREFS_SoundOnly(filename)



    //是否保存退出时的位置 + 退出时的位置
    suspend fun update_PREFS_saveLastPosition(filename: String,flag_save_position_when_exit: Boolean) = dao.update_PREFS_saveLastPosition(filename,flag_save_position_when_exit)
    suspend fun get_PREFS_saveLastPosition(filename: String): Boolean = dao.get_PREFS_saveLastPosition(filename)
    suspend fun update_value_LastPosition(filename: String,position_when_exit: Long) = dao.update_value_LastPosition(filename,position_when_exit)
    suspend fun get_value_LastPosition(filename: String): Long = dao.get_value_LastPosition(filename)




    suspend fun preset_all_row_default(filename: String) = dao.preset_all_row_default(filename)



}