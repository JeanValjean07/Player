package data.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import com.suming.player.ToolEventBus
import data.DataBaseMediaStore.MediaStoreRepo
import data.DataBaseMediaStore.MediaStoreSetting
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreReaderForVideo(
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

    suspend fun readAllVideos(): List<MediaItemForVideo> {
        //读取设置
        preCheck()

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

        //在IO线程执行查询
        return withContext(Dispatchers.IO) {
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
    suspend fun saveVideosToDatabase(videos: List<MediaItemForVideo>) {

        val mediaStoreRepo = MediaStoreRepo.get(context)

        val mediaStoreSettings = videos.map { video ->
            //查询是否已存在该记录：暂不使用
            //val existingSetting = mediaStoreRepo.getVideo(video.id.toString())
            //用例：info_is_hidden = existingSetting?.info_is_hidden ?: false,

            MediaStoreSetting(
                //基本：唯一标识：视频的媒体库id,同时也是uriNumOnly的值
                MARK_ID = video.id.toString(),
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
                info_format = video.format
            )
        }

        withContext(Dispatchers.IO) {

            mediaStoreRepo.saveAllVideos(mediaStoreSettings)

            cleanupDeletedVideos(videos.map { it.id.toString() }, mediaStoreRepo)

        }
    }
    //类功能主入口：读取所有视频并保存到数据库
    suspend fun readAndSaveAllVideos(): List<MediaItemForVideo> {
        val videos = readAllVideos()

        saveVideosToDatabase(videos)

        return videos
    }  //!主链路入口
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
            .map { it.MARK_ID }
            .filterNot { currentVideoIds.contains(it) }
        //批量删除
        if (deletedVideoIds.isNotEmpty()) {
            deletedVideoIds.forEach { videoId ->
                mediaStoreRepo.getVideo(videoId)?.let { video ->
                    mediaStoreRepo.deleteVideo(video)
                }
            }
        }
        //发布删除完成通知
        ToolEventBus.sendEvent("QueryFromMediaStoreVideoComplete")

    }


}