package com.suming.player.DataPack.DataBaseMusicStore

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.suming.player.SettingsRequestCenter

class MusicStoreRepo(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: MusicStoreRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MusicStoreRepo(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val dao = MusicStoreDataBase.get(context).musicStoreDao()

    suspend fun saveSetting(item: MusicStoreSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MusicStoreSetting? = dao[path]


    suspend fun insertOrUpdateAll(items: List<MusicStoreSetting>) = dao.insertOrUpdateAll(items)

    //保存单个视频信息
    suspend fun saveMusic(video: MusicStoreSetting) = dao.insertOrUpdate(video)

    //批量保存视频信息
    suspend fun saveAllMusics(videos: List<MusicStoreSetting>) = dao.insertOrUpdateAll(videos)

    //获取单个视频信息
    suspend fun getMusic(uriNumOnly: String): MusicStoreSetting? = dao[uriNumOnly]

    //获取所有视频信息
    suspend fun getAllMusics(): List<MusicStoreSetting> = dao.getAllMusics()

    //根据排序方式读取,不支持动态传入排序参数,全部列出
    suspend fun getMusicsPagedByOrder(page: Int, pageSize: Int, sortOrder: String): List<MusicStoreSetting> {
        val offset = page * pageSize

        return when (sortOrder) {
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByTitleAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_filename} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByTitleDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByDateAddedAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_date_added} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByDateAddedDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByDurationAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_duration} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByDurationDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByFileSizeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_file_size} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByFileSizeDesc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_ASC}" -> dao.getAllMusicsPagedByMimeTypeAsc(pageSize, offset)
            "${SettingsRequestCenter.sort_method_mime_type} ${SettingsRequestCenter.sort_orientation_DESC}" -> dao.getAllMusicsPagedByMimeTypeDesc(pageSize, offset)
            else -> dao.getAllMusicsPagedByTitleDesc(pageSize, offset)
        }
    }

    //根据排序方法获取所有视频
    suspend fun getAllMusicsSorted11(sortField: String = "info_title", sortOrientation: String = "ASC"): List<MusicStoreSetting> {
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
    //排序方式: info_title / info_date_added / info_file_size / info_duration / info_mime_type
    //排序方向: ASC / DESC
    suspend fun getAllMusicsSorted(
        sortOrder: String,
        sortOrientation: String
    ): List<MusicStoreSetting> {
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


    //搜索视频
    suspend fun searchMusics(query: String): List<MusicStoreSetting> = dao.searchMusics(query)


    //获取音乐总数
    suspend fun getTotalMusicCount(): Int = dao.getTotalMusicCount()

    //删除音乐
    suspend fun deleteMusic(music: MusicStoreSetting) = dao.delete(music)

    //清空所有数据
    suspend fun clearAll() = dao.clearAll()

    //检查该库是否为空
    suspend fun isEmpty(): Boolean = dao.getCount() == 0








}