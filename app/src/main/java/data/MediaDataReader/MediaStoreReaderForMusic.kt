package data.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
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
            MediaStore.Audio.Media.DISPLAY_NAME, //文件名
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID, //专辑ID
            MediaStore.Audio.Media.ARTIST, //艺术家
            MediaStore.Audio.Media.ALBUM, //专辑
            MediaStore.Audio.Media.TITLE //标题
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
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) //专辑ID
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST) //艺术家
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM) //专辑
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE) //标题
                //读取
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    val dur = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mimeType = cursor.getString(mimeTypeCol).orEmpty()
                    val format = if (mimeType.contains('/')) mimeType.substringAfterLast('/') else mimeType
                    val albumId = cursor.getLong(albumIdCol) //专辑ID
                    val artist = cursor.getString(artistCol).orEmpty() //艺术家
                    val album = cursor.getString(albumCol).orEmpty() //专辑
                    val title = cursor.getString(titleCol).orEmpty() //标题
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
                                albumId = albumId, //专辑ID
                                artist = artist, //艺术家
                                album = album, //专辑
                                title = title, //标题
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
                            albumId = albumId, //专辑ID
                            artist = artist, //艺术家
                            album = album, //专辑
                            title = title, //标题
                        )
                    }
                }
                //return
                list
            } ?: emptyList()
        }

    }


    //Functions
    //保存到数据库
    suspend fun saveMusicsToDatabase(musics: List<MediaItemForMusic>) {

        val musicStoreRepo = MusicStoreRepo.get(context)

        val musicStoreSettings = musics.map { music ->
            Log.d("SuMing", "saveMusicsToDatabase: $music")
            MusicStoreSetting(
                MARK_Uri_numOnly = music.id.toString(),
                info_filename = music.name,
                info_title = music.title,
                info_artist = music.artist,
                info_duration = music.durationMs,
                info_file_size = music.sizeBytes,
                info_uri_full = music.uri.toString(),
                info_date_added = music.dateAdded,
                info_format = music.format,
                info_album_id = music.albumId,
                info_album = music.album,
            )

        }

        withContext(Dispatchers.IO) {

            musicStoreRepo.saveAllMusics(musicStoreSettings)

            cleanupDeletedMusics(musics.map { it.id.toString() }, musicStoreRepo)

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
        catch (_: Exception) { false }
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
        ToolEventBus.sendEvent("QueryFromMediaStoreMusicComplete")
    }

}