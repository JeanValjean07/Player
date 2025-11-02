package data.MediaDataReader

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.suming.player.PlayListManager
import data.MediaItemRepo
import data.MediaModel.MediaItem_video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaReader_video(
    private val context: Context,
    private val contentResolver: ContentResolver,
) : PagingSource<Int, MediaItem_video>() {

    private val coroutineScopeRoom = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getRefreshKey(state: PagingState<Int, MediaItem_video>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1) ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem_video> {
        val page   = params.key ?: 0
        val limit  = params.loadSize
        val offset = page * limit
        //查询结果列表
        val list = mutableListOf<MediaItem_video>()
        //查询投影
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        //配置变量初始化
        var sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} DESC"
        var sort_orientation: String
        var show_hide_items = false
        //读取配置
        val PREFS = context.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)
        if (!PREFS.contains("sort_orientation")){
            PREFS.edit { putString("sort_orientation", "DESC") }
            sort_orientation = "DESC"
        }else{
            sort_orientation = PREFS.getString("sort_orientation", "DESC") ?: "DESC"
        }
        if (!PREFS.contains("sort_type")){
            PREFS.edit { putString("sort_type", "DISPLAY_NAME") }
        }else{
            val sort_type = PREFS.getString("sort_type", "DISPLAY_NAME") ?: "DISPLAY_NAME"
            if (sort_type == "DISPLAY_NAME"){
                if (sort_orientation == "DESC"){
                    sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} DESC"
                }else{
                    sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
                }
            }
            else if (sort_type == "DURATION"){
                sortOrder = if (sort_orientation == "DESC"){
                    "${MediaStore.Video.Media.DURATION} DESC"
                }else{
                    "${MediaStore.Video.Media.DURATION} ASC"
                }
            }
            else if (sort_type == "DATE_ADDED"){
                sortOrder = if (sort_orientation == "DESC"){
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                }else{
                    "${MediaStore.Video.Media.DATE_ADDED} ASC"
                }
            }
            else if (sort_type == "RESOLUTION"){
                sortOrder = if (sort_orientation == "DESC"){
                    "${MediaStore.Video.Media.RESOLUTION} DESC"
                }else{
                    "${MediaStore.Video.Media.RESOLUTION} ASC"
                }
            }
        }
        if (!PREFS.contains("show_hide_items")){
            PREFS.edit { putBoolean("show_hide_items", false) }
        }else{
            show_hide_items = PREFS.getBoolean("show_hide_items", false)
        }

        //播放列表


        //发起查询
        //<editor-fold desc="查询参数">
        //1.uri：MediaStore.Video(指定视频).Media.EXTERNAL_CONTENT_URI(指定外部储存)
        //2.projection：指定目标列: array of {...}
        //3.selection：限制查询条件,目前未使用
        //4.selectionArgs：限制查询条件的安全参数,避免sql注入
        //5.sortOrder：结果排序方式
        //</editor-fold desc="查询参数">
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            //查询结果处理
            //获取列索引
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            //获取列索引数据
            if (cursor.moveToPosition(offset)) {
                var left = limit
                do {
                    val id   = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    val dur  = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    //检查数据库
                    //检查数据库是否存在该视频
                    val setting = MediaItemRepo.get(context).getSetting(name)
                    //视频没记录过数据(初次打开)
                    if (setting == null){
                        list += MediaItem_video(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                            name = name,
                            durationMs = dur,
                            sizeBytes = size,
                            Media_Cover_Path = ""
                        )
                        continue
                    }

                    //视频已隐藏
                    if (setting.PREFS_Hide){
                        if (show_hide_items){
                            //视频有数据,但目标位为空
                            if (setting.SavePath_Cover == ""){
                                list += MediaItem_video(
                                    id = id,
                                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                                    name = name,
                                    durationMs = dur,
                                    sizeBytes = size,
                                    Media_Cover_Path = ""
                                )
                            }
                            //视频已有缩略图路径
                            else{
                                list += MediaItem_video(
                                    id = id,
                                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                                    name = name,
                                    durationMs = dur,
                                    sizeBytes = size,
                                    Media_Cover_Path = setting.SavePath_Cover
                                )
                            }
                        }
                    }
                    //视频未隐藏
                    else{
                        //视频有数据,但目标位为空
                        if (setting.SavePath_Cover == ""){
                            list += MediaItem_video(
                                id = id,
                                uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                                name = name,
                                durationMs = dur,
                                sizeBytes = size,
                                Media_Cover_Path = ""
                            )
                        }
                        //视频已有缩略图路径
                        else{
                            list += MediaItem_video(
                                id = id,
                                uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                                name = name,
                                durationMs = dur,
                                sizeBytes = size,
                                Media_Cover_Path = setting.SavePath_Cover
                            )
                        }
                    }


                    left--
                }
                while (left > 0 && cursor.moveToNext())
            }
        }

        //记录到播放列表
        if (page == 0) { PlayListManager.getInstance(context).updatePlayList_MediaStore(list) }


        //return
        return LoadResult.Page(
            data = list,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (list.isEmpty()) null else page + 1
        )
    }


}