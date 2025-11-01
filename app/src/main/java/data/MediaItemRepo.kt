package data

import android.content.Context

class MediaItemRepo private constructor(context: Context) {

    private val dao = MediaItemDataBase.get(context).mediaItemDao()

    suspend fun saveSetting(item: MediaItemSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MediaItemSetting? = dao[path]

    suspend fun HideVideo(filename: String,flag_need_hide: Boolean) = dao.HideVideo(filename,flag_need_hide)



    companion object {
        @Volatile
        private var INSTANCE: MediaItemRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MediaItemRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
}