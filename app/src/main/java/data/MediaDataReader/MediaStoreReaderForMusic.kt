package data.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import com.suming.player.ToolEventBus
import data.DataBaseMediaStore.MediaStoreSetting
import data.DataBaseMusicStore.MusicStoreRepo
import data.DataBaseMusicStore.MusicStoreSetting
import data.MediaModel.MediaItemForMusic
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
            MediaStore.Audio.Media._ID, //ID
            MediaStore.Audio.Media.DISPLAY_NAME, //文件名
            MediaStore.Audio.Media.TITLE, //标题：注意和文件名区分
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            //音频专属
            MediaStore.Audio.Media.ALBUM_ID, //专辑ID
            MediaStore.Audio.Media.ALBUM, //专辑名称
            //其他
            MediaStore.Audio.Media.DATA, //文件路径
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        //在IO线程执行查询
        return withContext(Dispatchers.IO) {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                //获取列索引
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val filenameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE) //标题
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST) //艺术家
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                //音频专属
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) //专辑ID
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM) //专辑
                //其他
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) //文件路径
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)


                //读取
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uriString = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    val filename = cursor.getString(filenameCol).orEmpty()
                    val title = cursor.getString(titleCol).orEmpty() //标题
                    val artist = cursor.getString(artistCol).orEmpty() //艺术家
                    val dur = cursor.getLong(durCol)
                    //音频专属
                    val albumId = cursor.getLong(albumIdCol) //专辑ID
                    val album = cursor.getString(albumCol).orEmpty() //专辑
                    //其他
                    val path = cursor.getString(pathCol).orEmpty() //文件路径
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mimeType = cursor.getString(mimeTypeCol).orEmpty()
                    val format = if (mimeType.contains('/')) mimeType.substringAfterLast('/') else mimeType

                    //检查文件有效性
                    val shouldSkip = when {
                        //检查文件是否存在
                        PREFS_EnableFileExistCheck && !isFileExist(path) -> {
                            Log.v("SuMing", "检查到媒体文件不存在：文件媒体ID: $id")
                            true
                        }
                        //检查文件是否有内容
                        dur <= 0 || size <= 0 -> {
                            Log.v("SuMing", "检查到媒体文件没有有效时长或大小：文件媒体ID: $id")
                            true
                        }
                        //直接添加
                        else -> false
                    }

                    //汇总需要添加的条目
                    if (!shouldSkip) {
                        list += MediaItemForMusic(
                            id = id,
                            uriString = uriString,
                            uriNumOnly = id,
                            filename = filename,
                            title = title,
                            artist = artist,
                            durationMs = dur,
                            //音频专属
                            albumId = albumId,
                            album = album,
                            //其他
                            path = path,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            format = format,
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

            MusicStoreSetting(
                //基本：唯一标识：音频的媒体库id,同时也是uriNumOnly的值
                MARK_ID = music.id.toString(),
                info_uri_string = music.uriString,
                info_uri_numOnly = music.uriNumOnly,
                info_filename = music.filename,
                info_title = music.title,
                info_artist = music.artist,
                info_duration = music.durationMs,
                //音频专属
                info_album_id = music.albumId,
                info_album = music.album,
                //其他
                info_path = music.path,
                info_file_size = music.sizeBytes,
                info_date_added = music.dateAdded,
                info_format = music.format
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
    private fun isFileExist(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (_: Exception) {
            false
        }
    }
    //去除数据库中已无对应视频的条目
    private suspend fun cleanupDeletedMusics(currentMusicIds: List<String>, musicStoreRepo: MusicStoreRepo) {
        val allMusics = musicStoreRepo.getAllMusics()
        //找出数据库中存在但不在当前读取列表中的音乐ID
        val deletedMusicIds = allMusics
            .map { it.MARK_ID }
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