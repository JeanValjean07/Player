package data.DataBaseMusicStore

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery

class MusicStoreRepo private constructor(context: Context) {

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
            "info_title ASC" -> dao.getAllMusicsPagedByTitleAsc(pageSize, offset)
            "info_title DESC" -> dao.getAllMusicsPagedByTitleDesc(pageSize, offset)
            "info_date_added ASC" -> dao.getAllMusicsPagedByDateAddedAsc(pageSize, offset)
            "info_date_added DESC" -> dao.getAllMusicsPagedByDateAddedDesc(pageSize, offset)
            "info_duration ASC" -> dao.getAllMusicsPagedByDurationAsc(pageSize, offset)
            "info_duration DESC" -> dao.getAllMusicsPagedByDurationDesc(pageSize, offset)
            "info_file_size ASC" -> dao.getAllMusicsPagedByFileSizeAsc(pageSize, offset)
            "info_file_size DESC" -> dao.getAllMusicsPagedByFileSizeDesc(pageSize, offset)
            "info_mime_type ASC" -> dao.getAllMusicsPagedByMimeTypeAsc(pageSize, offset)
            "info_mime_type DESC" -> dao.getAllMusicsPagedByMimeTypeDesc(pageSize, offset)
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

    //更新视频隐藏状态
    suspend fun getHideStatus(uriNumOnly: String): Boolean? = dao.getHideStatus(uriNumOnly)
    suspend fun updateHiddenStatus(uriNumOnly: String, isHidden: Boolean) = dao.updateHiddenStatus(uriNumOnly, isHidden)

    //获取音乐总数
    suspend fun getTotalMusicCount(): Int = dao.getTotalMusicCount()

    //删除音乐
    suspend fun deleteMusic(music: MusicStoreSetting) = dao.delete(music)

    //清空所有数据
    suspend fun clearAll() = dao.clearAll()








}