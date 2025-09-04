package data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.paging.PagingSource
import androidx.paging.PagingState
import data.model.VideoItem
import java.io.File
import java.io.FileOutputStream


class LocalVideoSource(
    private val contentResolver: ContentResolver,
    private val context: Context
) : PagingSource<Int, VideoItem>() {

    override fun getRefreshKey(state: PagingState<Int, VideoItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1) ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoItem> {
        val page   = params.key ?: 0
        val limit  = params.loadSize
        val offset = page * limit

        val list = mutableListOf<VideoItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            if (cursor.moveToPosition(offset)) {
                var left = limit
                do {
                    val id   = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    val dur  = cursor.getLong(durCol)
                    list += VideoItem(
                        id = id,
                        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        name = name,
                        durationMs = dur,
                        thumbnailUri = getVideoThumbnail(id)
                    )
                    left--
                } while (left > 0 && cursor.moveToNext())
            }
        }

        return LoadResult.Page(
            data = list,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (list.isEmpty()) null else page + 1
        )
    }

    private fun getVideoThumbnail(
        videoId: Long,
        width: Int = 512,
        height: Int = 512
    ): Uri? {
        val bmp = try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoId
            )
            contentResolver.loadThumbnail(uri, Size(width, height), null)

        } catch (t: Throwable) {
            null
        } ?: return null

        return saveThumbToCache(bmp)
    }


    private fun saveThumbToCache(bitmap: Bitmap): Uri? {
        return try {
            val file = File(
                context.cacheDir,
                "thumb_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}