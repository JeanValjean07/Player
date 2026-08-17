package com.suming.player.DataPack.DataBaseMediaStore.Audio

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioDataClass
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreDataBase
import com.suming.player.SettingsRequestCenter
import kotlin.text.get

class AudioRepo(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: AudioRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                AudioRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
    //关联Dao层
    private val dao = MediaStoreDataBase.get(context).AudioTableDao()





    //保存单个音频信息
    suspend fun saveAudio(video: AudioDataClass) = dao.insertOrUpdate(video)
    //批量保存音频信息
    suspend fun saveAllAudios(videos: List<AudioDataClass>) = dao.insertOrUpdateAll(videos)

    //获取单个音频信息
    suspend fun getMusicItem(media_api_NUM_ID: Long): AudioDataClass? = dao[media_api_NUM_ID]
    //获取所有音频信息
    suspend fun getAllMusics(): List<AudioDataClass> = dao.getAllMusics()


    //根据排序方式读取,不支持动态传入排序参数,全部列出
    suspend fun getMusicsPagedByOrder(page: Int, pageSize: Int, sortOrder: String): List<AudioDataClass> {
        val offset = page * pageSize

        return when (sortOrder) {
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByFileNameAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByFileNameDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByDateAddedAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByDateAddedDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByDurationAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByDurationDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByFileSizeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByFileSizeDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByMimeTypeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByMimeTypeDesc(pageSize, offset)
            else -> dao.getAllMusicsPagedByMimeTypeDesc(pageSize, offset)
        }
    }



    //搜索视频
    suspend fun searchMusics(query: String): List<AudioDataClass> = dao.searchMusics(query)


    //获取音乐总数
    suspend fun getTotalMusicCount(): Int = dao.getTotalMusicCount()
    //检查该库是否为空
    suspend fun isEmpty(): Boolean = dao.getCount() == 0

    //检查是否存在NUM_ID为目标的项
    suspend fun existsByNUM_ID(media_api_NUM_ID: Long): Boolean = dao.existsByNUM_ID(media_api_NUM_ID)


    //删除音乐
    suspend fun deleteMusic(music: AudioDataClass) = dao.delete(music)
    //清空所有数据
    suspend fun clearAll(): Int = dao.clearAll()


    //根据排序方法获取所有视频
    suspend fun getAllMusicsSorted11(sortField: String = "info_title", sortOrientation: String = "ASC"): List<AudioDataClass> {
        //白名单防注入
        val safeField = when (sortField) {
            "info_title", "info_date_added", "info_file_size", "info_duration", "info_mime_type" -> sortField
            else -> "info_title"
        }
        val safeOrder = when (sortOrientation) {
            "ASC", "DESC" -> sortOrientation
            else -> "ASC"
        }
        val sql = "SELECT * FROM MusicStore ORDER BY $safeField $safeOrder"
        val query = SimpleSQLiteQuery(sql)
        return dao.getAllMusicsSorted(query)
    }
    suspend fun getAllMusicsSorted(sortOrder: String, sortOrientation: String): List<AudioDataClass> {
        //白名单防注入
        val safeField = when (sortOrder) {
            "info_title", "info_date_added", "info_file_size", "info_duration", "info_mime_type" -> sortOrder
            else -> "info_title"
        }
        val safeOrder = when (sortOrientation) {
            "ASC", "DESC" -> sortOrientation
            else -> "ASC"
        }
        val sql = "SELECT * FROM MusicStore ORDER BY $safeField $safeOrder"
        val query = SimpleSQLiteQuery(sql)
        return dao.getAllMusicsSorted(query)
    }


}