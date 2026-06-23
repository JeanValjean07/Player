package com.suming.player.DataPack.DataBaseMediaItem

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
    //数据库操作
    private val dao = MediaItemDataBase.get(context).mediaItemDao()
    //保存设置
    suspend fun saveSetting(item: MediaItemSetting) = dao.insertOrUpdate(item)
    //获取设置
    suspend fun getSetting(path: String): MediaItemSetting? = dao[path]


    //媒体类型
    suspend fun update_INFO_MediaType(id: String, media_type: String) = dao.update_INFO_MediaType(id,media_type)
    suspend fun get_INFO_MediaType(id: String): String = dao.get_INFO_MediaType(id)

    //后台播放
    suspend fun update_PREFS_BackgroundPlay(id: String, flag_need_background_play: Boolean) = dao.update_PREFS_BackgroundPlay(id,flag_need_background_play)
    suspend fun get_PREFS_BackgroundPlay(id: String): Boolean = dao.get_PREFS_BackgroundPlay(id)

    //循环播放
    suspend fun update_PREFS_LoopPlay(id: String, flag_need_loop_play: Boolean) = dao.update_PREFS_LoopPlay(id,flag_need_loop_play)
    suspend fun get_PREFS_LoopPlay(id: String): Boolean = dao.get_PREFS_LoopPlay(id)

    //AlwaysSeek
    suspend fun update_PREFS_AlwaysSeek(id: String, flag_need_always_seek: Boolean) = dao.update_PREFS_AlwaysSeek(id,flag_need_always_seek)
    suspend fun get_PREFS_AlwaysSeek(id: String): Boolean = dao.get_PREFS_AlwaysSeek(id)

    //LinkScroll
    suspend fun update_PREFS_LinkScroll(id: String, flag_need_link_scroll: Boolean) = dao.update_PREFS_LinkScroll(id,flag_need_link_scroll)
    suspend fun get_PREFS_LinkScroll(id: String): Boolean = dao.get_PREFS_LinkScroll(id)

    //TapJump
    suspend fun update_PREFS_TapJump(id: String, flag_need_tap_jump: Boolean) = dao.update_PREFS_TapJump(id,flag_need_tap_jump)
    suspend fun get_PREFS_TapJump(id: String): Boolean = dao.get_PREFS_TapJump(id)

    //仅播视频
    suspend fun update_PREFS_VideoOnly(id: String, flag_need_video_only: Boolean) = dao.update_PREFS_VideoOnly(id,flag_need_video_only)
    suspend fun get_PREFS_VideoOnly(id: String): Boolean = dao.get_PREFS_VideoOnly(id)
    //仅播音频
    suspend fun update_PREFS_SoundOnly(id: String, flag_need_sound_only: Boolean) = dao.update_PREFS_SoundOnly(id,flag_need_sound_only)
    suspend fun get_PREFS_SoundOnly(id: String): Boolean = dao.get_PREFS_SoundOnly(id)



    //保存播放进度
    suspend fun update_PREFS_saveLastPosition(id: String, flag_save_position_when_exit: Boolean) = dao.update_PREFS_saveLastPosition(id,flag_save_position_when_exit)
    suspend fun get_PREFS_saveLastPosition(id: String): Boolean = dao.get_PREFS_saveLastPosition(id)
    //进度具体值
    suspend fun update_value_LastPosition(id: String, position_when_exit: Long) = dao.update_value_LastPosition(id,position_when_exit)
    suspend fun get_value_LastPosition(id: String): Long = dao.get_value_LastPosition(id)


    //播放速度
    suspend fun update_PREFS_PlaySpeed(id: String, playback_speed: Float) = dao.update_PREFS_PlaySpeed(id,playback_speed)
    suspend fun get_PREFS_PlaySpeed(id: String): Float = dao.get_PREFS_PlaySpeed(id)

    //隐藏媒体
    suspend fun update_PREFS_Hide(id: String, flag_hide_video: Boolean) = dao.update_PREFS_Hide(id,flag_hide_video)
    suspend fun get_PREFS_Hide(id: String): Boolean = dao.get_PREFS_Hide(id)



    //获取该媒体的一行全部数据
    suspend fun getMediaItemPack(id: String): MediaItemSetting? = dao.getMediaItemPack(id)





}