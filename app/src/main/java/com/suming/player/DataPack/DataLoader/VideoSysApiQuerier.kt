package com.suming.player.DataPack.DataLoader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoDataClass
import com.suming.player.DataPack.DataBaseStateConnector
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForVideo
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.SettingsRequestCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoSysApiQuerier(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {

    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()



    //调用入口
    suspend fun readAndSaveAllVideos(): List<MediaItemFullForVideo> {
        //读取所有视频文件
        val videos = readAllVideos()
        //保存到数据库
        saveVideosToDatabase(videos)
        //保存到播放列表(需要转为VideoDataClass)
        ListManagerHelper.SET_VideoList_fromReader(videos.map { it.toVideoDataClass() })

        return videos
    }




    suspend fun readAllVideos(): List<MediaItemFullForVideo> {
        return withContext(Dispatchers.IO) {

            //通知状态变更(开始加载)(二合一项和视频独占项都更新)
            DataBaseStateConnector.setState_queryDisk_Video_state(DataBaseStateConnector.state_queryDisk_start)
            DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_start)

            //初始化设置项
            val PRF_EnableFileExistCheck = SettingsRequestCenter.get_PREFS_EnableFileExistCheck(context)

            //初始化列表
            val list = mutableListOf<MediaItemFullForVideo>()
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
                val col_media_api_id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val col_file_name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val col_media_title = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val col_media_artist = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val col_media_duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val col_media_resolution = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
                val col_media_path = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val col_media_size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val col_media_date_added = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val col_media_mime_type = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)


                //读取媒体文件
                while (cursor.moveToNext()) {
                    val media_api_NUM_ID = cursor.getLong(col_media_api_id)
                    val content_uriString = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, media_api_NUM_ID).toString()
                    val file_name = cursor.getString(col_file_name).orEmpty()
                    val media_title = cursor.getString(col_media_title).orEmpty()
                    val media_artist = cursor.getString(col_media_artist).orEmpty()
                    val media_durationMs = cursor.getLong(col_media_duration)
                    val media_video_resolution = cursor.getString(col_media_resolution).orEmpty()
                    val file_path = cursor.getString(col_media_path).orEmpty()
                    val file_size = cursor.getLong(col_media_size)
                    val media_api_dateAdded = cursor.getLong(col_media_date_added)
                    val media_mimeType = cursor.getString(col_media_mime_type).orEmpty()
                    val mediaType = if(media_mimeType.contains("video")) MediaType.Video else MediaType.Undefined
                    val media_format = if (media_mimeType.contains('/')) media_mimeType.substringAfterLast('/') else media_mimeType
                    val media_api_SPECIFIC_ID = MediaInfoRetriever.calculate_SPECIFIC_ID(mediaType, media_api_NUM_ID.toString())

                    //日志
                    /*
                    consoleLog("读取到视频文件: " +
                            "media_api_SPECIFIC_ID: $media_api_SPECIFIC_ID, " +
                            "media_api_NUM_ID: $media_api_NUM_ID, " +
                            "content_uriString: $content_uriString, " +
                            "file_name: $file_name, " +
                            "media_title: $media_title, " +
                            "media_artist: $media_artist, " +
                            "media_durationMs: $media_durationMs, " +
                            "media_video_resolution: $media_video_resolution, " +
                            "file_path: $file_path, " +
                            "file_size: $file_size, " +
                            "media_api_dateAdded: $media_api_dateAdded, " +
                            "media_mimeType: $media_mimeType, " +
                            "mediaType: $mediaType, " +
                            "media_format: $media_format, "
                    )
                     */

                    //检查文件是否应该添加
                    val save = when {
                        //检查是否属于视频
                        mediaType == MediaType.Video -> {
                            //检查文件是否存在
                            val fileExists = if (PRF_EnableFileExistCheck) {
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
                        list += MediaItemFullForVideo(
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

                            media_video_resolution = media_video_resolution,
                            media_video_bitrate = "114514",  //TODO
                        )
                    }
                }
                list
            } ?: emptyList()
        }
    }

    //保存到数据库
    suspend fun saveVideosToDatabase(videos: List<MediaItemFullForVideo>) {
        withContext(Dispatchers.IO) {
            //链接数据库仓库
            val mediaStoreRepo = VideoRepo.get(context)

            //汇总视频数据
            val mediaStoreSettings = videos.map { video ->
                VideoDataClass(
                    media_api_SPECIFIC_ID = video.media_api_SPECIFIC_ID,
                    media_api_NUM_ID = video.media_api_NUM_ID,
                    media_api_dateAdded = video.media_api_dateAdded,
                    media_SPECIFIC_MediaType = video.media_SPECIFIC_MediaType,
                    URI_S_FP = video.URI_S_FP,
                    file_path = video.file_path,
                    file_name = video.file_name,
                    file_size = video.file_size,
                    media_title = video.media_title,
                    media_artist = video.media_artist,
                    media_durationMs = video.media_durationMs,
                    media_format = video.media_format,

                    media_video_resolution = video.media_video_resolution,
                    media_video_bitrate = video.media_video_bitrate,
                )
            }

            //批量保存
            mediaStoreRepo.saveVideoItems(mediaStoreSettings)

            //去重
            cleanupDeletedVideos(videos.map { it.media_api_NUM_ID }, mediaStoreRepo)

        }
    }



    //转换格式
    private fun MediaItemFullForVideo.toVideoDataClass(): VideoDataClass {
        return VideoDataClass(
            media_api_SPECIFIC_ID = media_api_SPECIFIC_ID,
            media_api_NUM_ID = media_api_NUM_ID,
            media_api_dateAdded = media_api_dateAdded,
            media_SPECIFIC_MediaType = media_SPECIFIC_MediaType,
            URI_S_FP = URI_S_FP,
            file_path = file_path,
            file_name = file_name,
            file_size = file_size,
            media_title = media_title,
            media_artist = media_artist,
            media_durationMs = media_durationMs,
            media_format = media_format,

            media_video_resolution = media_video_resolution,

            media_video_bitrate = media_video_bitrate,


        )
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
    private suspend fun cleanupDeletedVideos(currentVideoIds: List<Long>, mediaStoreRepo: VideoRepo) {
        val allVideos = mediaStoreRepo.getAllVideoItems()

        //找出数据库中存在但不在当前读取列表中的视频ID
        val deletedVideoIds = allVideos
            .map { it.media_api_NUM_ID }
            .filterNot { currentVideoIds.contains(it) }

        //批量删除
        if (deletedVideoIds.isNotEmpty()) {
            deletedVideoIds.forEach { videoId ->
                mediaStoreRepo.getVideoItem(videoId)?.let { video ->
                    mediaStoreRepo.deleteVideoItem(video)
                }
            }
        }

        withContext(Dispatchers.Main){
            //通知状态变更(完成加载)(二合一项和视频独占项都更新)
            DataBaseStateConnector.setState_queryDisk_Video_state(DataBaseStateConnector.state_queryDisk_success)
            DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_success)

            //触发刷新后回到idle状态
            DataBaseStateConnector.setState_queryDisk_Video_state(DataBaseStateConnector.state_queryDisk_idle)
            DataBaseStateConnector.setState_queryDisk(DataBaseStateConnector.state_queryDisk_idle)
        }


    }

    //备注：注意：媒体库方法无法读取被.nomedia标记的文件夹中的内容
    //备注：媒体库典型返回值
    //来自公共文件夹的媒体
    /*
    //DCIM/Camera
    media_api_id: 3773, content_uriString: content://media/external/video/media/3773, file_name: shots.mp4, media_title: shots, media_artist: <unknown>,
    media_durationMs: 241139, media_video_resolution: 1280×720, file_path: /storage/emulated/0/Pictures/音乐视频/shots.mp4, file_size: 54937066,
    media_api_dateAdded: 1777839330, media_mimeType: video/mp4, mediaType: MediaType_Video, media_format: mp4

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