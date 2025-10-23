package data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import java.io.ByteArrayOutputStream


class MediaItemRepo private constructor(context: Context) {

    private val dao = MediaItemDataBase.get(context).mediaItemDao()

    suspend fun saveSetting(item: MediaItemSetting) = dao.insertOrUpdate(item)

    suspend fun getSetting(path: String): MediaItemSetting? = dao[path]

    /*
    suspend fun saveSessionThumbPath(path: String) {

        val old = dao[path] ?: MediaItemSetting(videoPath = path)
        dao.insertOrUpdate(old.copy(thumbnail = null))
    }


    suspend fun getSessionThumbPath(path: String): Bitmap? {
        val bytes = dao[path]?.thumbnail ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

     */

    companion object {
        @Volatile
        private var INSTANCE: MediaItemRepo? = null
        fun get(context: Context) =
            INSTANCE ?: synchronized(this) {
                MediaItemRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
}