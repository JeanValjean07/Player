package com.suming.player.DataPack.DataLoader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioDataClass
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForAudio
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.SettingsRequestCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioSysApiQuerier(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()
    //设置项
    private var PREFS_EnableFileExistCheck: Boolean = false
    //初始化(自建)
    private fun init(){
        PREFS_EnableFileExistCheck = SettingsRequestCenter.get_PREFS_EnableFileExistCheck(context)
    }


    //读取所有音乐
    suspend fun readAllMusics(): List<MediaItemFullForAudio> {
        //读取设置
        init()

        //初始化列表
        val list = mutableListOf<MediaItemFullForAudio>()
        //排序方式
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} DESC"
        //查询投影
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            //音频专属
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            //其他
            MediaStore.Audio.Media.DATA,
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
                val col_media_api_id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val col_file_name = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val col_media_title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE) //标题
                val col_media_artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST) //艺术家
                val col_media_duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                //音频专属
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) //专辑ID
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM) //专辑
                //其他
                val col_media_path = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) //文件路径
                val col_media_size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val col_media_date_added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val col_media_mime_type = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)


                //读取
                while (cursor.moveToNext()) {
                    val media_api_NUM_ID = cursor.getLong(col_media_api_id)
                    val content_uriString = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, media_api_NUM_ID).toString()
                    val file_name = cursor.getString(col_file_name).orEmpty()
                    val media_title = cursor.getString(col_media_title).orEmpty()
                    val media_artist = cursor.getString(col_media_artist).orEmpty()
                    val media_durationMs = cursor.getLong(col_media_duration)
                    val albumId = cursor.getLong(albumIdCol)
                    val albumName = cursor.getString(albumCol).orEmpty()
                    val file_path = cursor.getString(col_media_path).orEmpty()
                    val file_size = cursor.getLong(col_media_size)
                    val media_api_dateAdded = cursor.getLong(col_media_date_added)
                    val media_mimeType = cursor.getString(col_media_mime_type).orEmpty()
                    val mediaType = if(media_mimeType.contains("audio")) MediaType.Audio else MediaType.Undefined
                    val media_format = if (media_mimeType.contains('/')) media_mimeType.substringAfterLast('/') else media_mimeType
                    val media_api_SPECIFIC_ID = MediaInfoRetriever.calculate_SPECIFIC_ID(mediaType, media_api_NUM_ID.toString())

                    //日志
                    /*
                    consoleLog("MediaStoreReaderForMusic: 读取到音频文件, " +
                            "media_api_SPECIFIC_ID: $media_api_SPECIFIC_ID, " +
                            "media_api_NUM_ID: $media_api_NUM_ID, " +
                            "content_uriString: $content_uriString, " +
                            "file_name: $file_name, " +
                            "media_title: $media_title, " +
                            "media_artist: $media_artist, " +
                            "media_durationMs: $media_durationMs, " +
                            "file_path: $file_path, " +
                            "file_size: $file_size, " +
                            "media_api_dateAdded: $media_api_dateAdded, " +
                            "media_mimeType: $media_mimeType, " +
                            "mediaType: $mediaType, " +
                            "media_format: $media_format, "+
                            "albumId: $albumId, "+
                            "albumName: $albumName, "
                    )
                    */

                    //检查文件是否应该添加
                    val save = when {
                        //检查是否属于音频文件
                        mediaType == MediaType.Audio -> {
                            //检查文件是否存在
                            val fileExists = if (PREFS_EnableFileExistCheck) {
                                isFileExist(file_path)
                            }else{
                                true
                            }
                            //检查文件是否有内容
                            val hasContent = media_durationMs > 0 && file_size > 0

                            fileExists && hasContent
                        }
                        //不是视频时丢弃
                        else -> false
                    }
                    if (save) {
                        list += MediaItemFullForAudio(
                            media_api_SPECIFIC_ID = media_api_SPECIFIC_ID,
                            media_api_NUM_ID = media_api_NUM_ID,
                            media_api_dateAdded = media_api_dateAdded,
                            media_SPECIFIC_MediaType = mediaType,
                            URI_S_FP = content_uriString,
                            file_path = file_path,
                            file_name = file_name,
                            file_size = file_size,
                            media_title = media_title,
                            media_artist = media_artist,
                            media_durationMs = media_durationMs,
                            media_format = media_format,
                            //-----------------------------------
                            media_audio_bitrate = "",
                            media_audio_album = albumName,
                            media_audio_albumId = albumId,
                        )
                    }
                }
                list
            } ?: emptyList()
        }

    }


    //Functions
    //保存到数据库
    suspend fun saveMusicsToDatabase(musics: List<MediaItemFullForAudio>) {
        withContext(Dispatchers.IO) {

            val musicStoreRepo = AudioRepo.get(context)

            val musicStoreSettings = musics.map { music ->
                AudioDataClass(
                    media_api_SPECIFIC_ID = music.media_api_SPECIFIC_ID,
                    media_api_NUM_ID = music.media_api_NUM_ID,
                    media_api_dateAdded = music.media_api_dateAdded,
                    media_SPECIFIC_MediaType = music.media_SPECIFIC_MediaType,
                    URI_S_FP = music.URI_S_FP,
                    file_path = music.file_path,
                    file_name = music.file_name,
                    file_size = music.file_size,
                    media_title = music.media_title,
                    media_artist = music.media_artist,
                    media_durationMs = music.media_durationMs,
                    media_format = music.media_format,
                    //-----------------------------------
                    media_audio_bitrate = "",
                    media_audio_album = music.media_audio_album,
                    media_audio_albumId = music.media_audio_albumId,
                )
            }

            musicStoreRepo.saveAllAudios(musicStoreSettings)

            cleanupDeletedMusics(musics.map { it.media_api_NUM_ID }, musicStoreRepo)

        }
    }
    //类功能主入口：读取所有音乐并保存到数据库
    suspend fun readAndSaveAllMusics(): List<MediaItemFullForAudio> {
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
    //去除数据库中已无对应音频的条目
    private suspend fun cleanupDeletedMusics(currentMusicIds: List<Long>, musicStoreRepo: AudioRepo) {
        val allMusics = musicStoreRepo.getAllMusics()
        //找出数据库中存在但不在当前读取列表中的音乐ID
        val deletedMusicIds = allMusics
            .map { it.media_api_NUM_ID }
            .filterNot { currentMusicIds.contains(it) }
        //批量删除
        if (deletedMusicIds.isNotEmpty()) {
            deletedMusicIds.forEach { media_api_NUM_ID ->
                musicStoreRepo.getMusicItem(media_api_NUM_ID)?.let { music ->
                    musicStoreRepo.deleteMusic(music)
                }
            }
        }

        //发布完成通知
        DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_success)
    }

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaStoreReaderForMusic: $msg")
        }
    }

}