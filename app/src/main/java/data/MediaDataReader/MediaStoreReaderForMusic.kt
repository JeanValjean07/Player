package data.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import androidx.core.content.edit
import com.suming.player.ToolEventBus
import data.DataBaseMusicStore.MusicStoreRepo
import data.DataBaseMusicStore.MusicStoreSetting
import data.MediaModel.MediaItemForMusic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class MediaStoreReaderForMusic(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    //协程作用域：保存到数据库
    private val coroutineScope_save_to_room = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //设置项
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_EnableFileExistCheck: Boolean = false


    fun preCheck() {
        PREFS_MediaStore = context.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)
        if (!PREFS_MediaStore.contains("PREFS_EnableFileExistCheck")) {
            PREFS_MediaStore.edit { putBoolean("PREFS_EnableFileExistCheck", false) }
            PREFS_EnableFileExistCheck = false
        }else{
            PREFS_EnableFileExistCheck = PREFS_MediaStore.getBoolean("PREFS_EnableFileExistCheck", false)
        }

    }

    suspend fun readAllMusics(): List<MediaItemForMusic> {
        //读取设置
        preCheck()

        //初始化列表
        val list = mutableListOf<MediaItemForMusic>()
        //排序方式
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} DESC"
        //查询投影
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        //在IO线程执行查询
        return withContext(Dispatchers.IO) {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                //获取列索引
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                //读取
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    val dur = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mimeType = cursor.getString(mimeTypeCol).orEmpty()
                    val format = if (mimeType.contains('/')) mimeType.substringAfterLast('/') else mimeType
                    //使用存在检查
                    if (PREFS_EnableFileExistCheck) {
                        //检查文件是否存在
                        val fileExist = isFileExist(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
                        //文件不存在，跳过
                        if (fileExist && dur > 0 && size > 0 ) {
                            list += MediaItemForMusic(
                                id = id,
                                uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                                name = name,
                                durationMs = dur,
                                sizeBytes = size,
                                dateAdded = dateAdded,
                                format = format,
                            )
                        }
                    }
                    //不使用存在检查
                    else{
                        list += MediaItemForMusic(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            name = name,
                            durationMs = dur,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            format = format,
                        )
                    }
                }
                //读取完后发布通知消息
                ToolEventBus.sendEvent("MediaStore_Music_Query_Complete")
                //return
                list
            } ?: emptyList()
        }

    }


    //Functions
    //保存到数据库
    suspend fun saveMusicsToDatabase(videos: List<MediaItemForMusic>) {

        val musicStoreRepo = MusicStoreRepo.get(context)

        val musicStoreSettings = videos.map { video ->
            //查询是否已存在该记录
            val existingSetting = musicStoreRepo.getMusic(video.id.toString())

            MusicStoreSetting(
                MARK_Uri_numOnly = video.id.toString(),
                info_title = video.name,
                info_duration = video.durationMs,
                info_file_size = video.sizeBytes,
                info_uri_full = video.uri.toString(),
                info_date_added = video.dateAdded,
                info_is_hidden = existingSetting?.info_is_hidden ?: false,
                info_artwork_path = existingSetting?.info_artwork_path ?: "",
                info_format = video.format
            )
        }

        withContext(Dispatchers.IO) {

            musicStoreRepo.saveAllMusics(musicStoreSettings)

            cleanupDeletedMusics(videos.map { it.id.toString() }, musicStoreRepo)

        }
    }
    //类功能主入口：读取所有音乐并保存到数据库
    suspend fun readAndSaveAllMusics(): List<MediaItemForMusic> {
        val musics = readAllMusics()
        saveMusicsToDatabase(musics)

        return musics
    }
    //存在检查
    private fun isFileExist(uri: android.net.Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use {
                return true
            }
            false
        }
        catch (e: Exception) { false }
    }
    //去除数据库中已无对应视频的条目
    private suspend fun cleanupDeletedMusics(currentMusicIds: List<String>, musicStoreRepo: MusicStoreRepo) {
        val allMusics = musicStoreRepo.getAllMusics()
        //找出数据库中存在但不在当前读取列表中的音乐ID
        val deletedMusicIds = allMusics
            .map { it.MARK_Uri_numOnly }
            .filterNot { currentMusicIds.contains(it) }
        //批量删除
        if (deletedMusicIds.isNotEmpty()) {
            deletedMusicIds.forEach { musicId ->
                musicStoreRepo.getMusic(musicId)?.let { music ->
                    musicStoreRepo.deleteMusic(music)
                }
            }
        }
        //发布删除完成通知
        ToolEventBus.sendEvent("MediaStore_NoExist_Delete_Complete")
    }


}