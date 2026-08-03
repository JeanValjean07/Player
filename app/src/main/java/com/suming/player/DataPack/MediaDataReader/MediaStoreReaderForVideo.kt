package com.suming.player.DataPack.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.suming.player.AddonTools.ToolEventBus
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreRepo
import com.suming.player.DataPack.DataBaseMediaStore.MediaStoreSetting
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.SettingsRequestCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreReaderForVideo(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {

    //设置项
    private var PREFS_EnableFileExistCheck: Boolean = false

    //初始化设置项
    private fun init(){
        PREFS_EnableFileExistCheck = SettingsRequestCenter.get_PREFS_EnableFileExistCheck(context)
    }




    suspend fun readAllVideos(): List<MediaItemForVideo> {

        return withContext(Dispatchers.IO) {
            //初始化设置项
            init()

            //初始化列表
            val list = mutableListOf<MediaItemForVideo>()
            //排序方式
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            //查询投影
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME, //文件名
                MediaStore.Video.Media.TITLE, //标题
                MediaStore.Video.Media.ARTIST, //艺术家
                MediaStore.Video.Media.DURATION,
                //视频专属
                MediaStore.Video.Media.RESOLUTION,
                //其他
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE,
            )




            //查询视频文件
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val filenameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                //视频专属
                val resCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
                //其他
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                //读取媒体文件
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uriString = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                    val filename = cursor.getString(filenameCol).orEmpty()
                    val title = cursor.getString(titleCol).orEmpty()
                    val artist = cursor.getString(artistCol).orEmpty()
                    val dur = cursor.getLong(durCol)
                    //视频专属
                    val res = cursor.getString(resCol).orEmpty()
                    //其他
                    val path = cursor.getString(pathCol).orEmpty() //文件路径：可参与存在检查
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mimeType = cursor.getString(mimeTypeCol).orEmpty()
                    val mediaType = if(mimeType.contains("video")) MediaType.Video else MediaType.Undefined
                    val format = if (mimeType.contains('/')) mimeType.substringAfterLast('/') else mimeType

                    consoleLog("读取到视频文件: id: $id, uriString: $uriString, filename: $filename, title: $title, artist: $artist, dur: $dur, " +
                            "res: $res, " +
                            "path: $path, size: $size, dateAdded: $dateAdded, mimeType: $mimeType, format: $format")

                    //检查文件是否应该添加
                    val save = when {
                        //检查是否属于视频
                        mediaType == MediaType.Video -> {
                            //检查文件是否存在
                            val fileExists = if (PREFS_EnableFileExistCheck) {
                                isFileExist(path)
                            }else{
                                true
                            }
                            //检查文件是否有内容
                            val hasContent = dur > 0 && size > 0

                            fileExists && hasContent
                        }
                        //不是视频时丢弃
                        else -> false
                    }

                    //汇总需要添加的条目
                    if (save) {
                        list += MediaItemForVideo(
                            id = id,
                            uriString = uriString,
                            uriNumOnly = id,
                            filename = filename,
                            title = title,
                            artist = artist,
                            durationMs = dur,
                            //视频专属
                            res = res,
                            //其他
                            path = path,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            mediaType = mediaType,
                            format = format,
                        )
                    }
                }
                list
            } ?: emptyList()
        }

    }

    //保存到数据库
    suspend fun saveVideosToDatabase(videos: List<MediaItemForVideo>) {

        val mediaStoreRepo = MediaStoreRepo.get(context)

        val mediaStoreSettings = videos.map { video ->


            MediaStoreSetting(
                //基本：唯一标识：视频的媒体库id,同时也是uriNumOnly的值
                MARK_MediaUniqueID = video.id.toString(),
                info_uri_string = video.uriString,
                info_uri_numOnly = video.uriNumOnly,
                info_filename = video.filename,
                info_title = video.title,
                info_artist = video.artist,
                info_duration = video.durationMs,
                //视频专属
                info_resolution = video.res,
                //其他
                info_path = video.path,
                info_file_size = video.sizeBytes,
                info_date_added = video.dateAdded,
                info_media_type = video.mediaType,
                info_format = video.format,
            )
        }

        withContext(Dispatchers.IO) {

            mediaStoreRepo.saveAllVideos(mediaStoreSettings)

            cleanupDeletedVideos(videos.map { it.id.toString() }, mediaStoreRepo)

        }
    }

    //读取所有视频并保存到数据库
    suspend fun readAndSaveAllVideos(): List<MediaItemForVideo> {
        val videos = readAllVideos()

        saveVideosToDatabase(videos)

        return videos
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
    private suspend fun cleanupDeletedVideos(currentVideoIds: List<String>, mediaStoreRepo: MediaStoreRepo) {
        val allVideos = mediaStoreRepo.getAllVideos()
        //找出数据库中存在但不在当前读取列表中的视频ID
        val deletedVideoIds = allVideos
            .map { it.MARK_MediaUniqueID }
            .filterNot { currentVideoIds.contains(it) }
        //批量删除
        if (deletedVideoIds.isNotEmpty()) {
            deletedVideoIds.forEach { videoId ->
                mediaStoreRepo.getVideo(videoId)?.let { video ->
                    mediaStoreRepo.deleteVideo(video)
                }
            }
        }

        //发布完成通知
        DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_success)

    }

    //备注：注意：媒体库方法无法读取被.nomedia标记的文件夹中的内容
    //备注：媒体库典型返回值
    //来自公共文件夹的媒体
    /*
    //DCIM/Camera
    读取到视频文件: id: 5703, uriString: content://media/external/video/media/5703, filename: 20260801_160535.mp4, title: 20260801_160535, artist: <unknown>,
    dur: 10586, res: 3840×2160, path: /storage/emulated/0/DCIM/Camera/20260801_160535.mp4, size: 64536810, dateAdded: 1785571533, mimeType: video/mp4, format: mp4
    //Picture/自建
    3769, uriString: content://media/external/video/media/3769, filename: 2023白路赛波加查单飞80公里.mp4, title: 2023白路赛波加查单飞80公里, artist: <unknown>,
    dur: 207867, res: 1280×720, path: /storage/emulated/0/Pictures/音乐视频/2023白路赛波加查单飞80公里.mp4, size: 87550559, dateAdded: 1777839330, mimeType: video/mp4, format: mp4
    //Movies
    id: 5720, uriString: content://media/external/video/media/5720, filename: Ava & Nikki.mp4, title: Ava & Nikki, artist: <unknown>, dur: 480480,
    res: 1280×720, path: /storage/emulated/0/Movies/Ava & Nikki.mp4, size: 110965035, dateAdded: 1785662054, mimeType: video/mp4, format: mp4
    //Download
    id: 5721, uriString: content://media/external/video/media/5721, filename: Ava & Nikki.mp4, title: Ava & Nikki, artist: <unknown>, dur: 480480,
    res: 1280×720, path: /storage/emulated/0/Download/Ava & Nikki.mp4, size: 110965035, dateAdded: 1785662097, mimeType: video/mp4, format: mp4
    //根目录自建文件夹
    id: 5722, uriString: content://media/external/video/media/5722, filename: Ava & Nikki.mp4, title: Ava & Nikki, artist: <unknown>, dur: 480480,
    res: 1280×720, path: /storage/emulated/0/A2文件夹/Ava & Nikki.mp4, size: 110965035, dateAdded: 1785662735, mimeType: video/mp4, format: mp4
    //App建立的文件夹
    id: 5723, uriString: content://media/external/video/media/5723, filename: Ava & Nikki.mp4, title: Ava & Nikki, artist: <unknown>, dur: 480480,
    res: 1280×720, path: /storage/emulated/0/tencent/Ava & Nikki.mp4, size: 110965035, dateAdded: 1785662799, mimeType: video/mp4, format: mp4
     */
    //来自非公共文件夹的媒体
    /*
    //Android/media
    id: 5724, uriString: content://media/external/video/media/5724, filename: Ava & Nikki.mp4, title: Ava & Nikki, artist: <unknown>, dur: 480480,
    res: 1280×720, path: /storage/emulated/0/Android/media/Ava & Nikki.mp4, size: 110965035, dateAdded: 1785662867, mimeType: video/mp4, format: mp4
    //Android/data
    !!!无法读取


     */

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaStoreReaderForVideo: $msg")
        }
    }


}