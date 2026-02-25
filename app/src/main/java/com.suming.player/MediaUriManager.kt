package com.suming.player

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import java.io.File

object MediaUriManager {

    //根据媒体储存唯一ID合成uri
    fun getMediaUriByMediaID(mediaStoreID: String, mediaType: String): Uri {
        var type = mediaType
        if (type == "music") {
            type = "audio"
        }

        //合成媒体链接
        val uri = "content://media/external/$type/media/$mediaStoreID".toUri()

        return uri
    }
    //根据媒体储存唯一ID合成uri字符串
    fun getMediaUriStringByMediaID(mediaStoreID: String, mediaType: String): String {
        var type = mediaType
        if (type == "music") {
            type = "audio"
        }


        //合成媒体链接
        val uri = "content://media/external/$type/media/$mediaStoreID"

        return uri
    }

    //根据媒体uri获取媒体ID(若无法获取就直接置空)
    fun getMediaIDByMediaUri(mediaUri: Uri,context: Context): String {
        //获得标准链接(自带是否标准检测)
        val standardMediaUri = getStandardMediaUri(mediaUri, context)
        //直接提取标准链接末尾ID
        val mediaID = standardMediaUri.lastPathSegment

        return mediaID ?: ""
    }

    //检查uri是否是标准格式
    fun isMediaUriStandard(mediaUri: Uri): Boolean {
        //使用正则表达式判断(目前通过video和audio两种类型)
        val regex = """^content://media/external/(?:video|audio)/media/\d+$""".toRegex()

        return regex.matches(mediaUri.toString())
    }

    //转换非标准链接为标准链接(自带是否标准检测)
    fun getStandardMediaUri(mediaUri: Uri, context: Context): Uri {
        //再次检查uri是否是标准格式,是标准格式时直接返回
        if (isMediaUriStandard(mediaUri)) return mediaUri

        //提取文件路径
        val filePath = getFilePath(context, mediaUri)
        if (filePath == null) return Uri.EMPTY

        //查询数据库获取媒体uri
        val mediaUri = getMediaUriByFilePath(filePath, context)
        if (mediaUri == Uri.EMPTY) return Uri.EMPTY


        return mediaUri
    }

    //从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
    fun getMediaUriByFilePath(filePath: String, context: Context): Uri {
        //构建查询
        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DATA} = ?"
        val selectionArgs = arrayOf(filePath)
        var cursor: Cursor? = null


        return try {
            cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val id = cursor.getLong(idColumnIndex)

                //构建标准uri
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }else{
                Uri.EMPTY
            }
        }finally{
            cursor?.close()
        }
    }


    //工具函数
    //从uri获取文件绝对路径
    private fun getFilePath(context: Context, uri: Uri): String? {
        val cleanUri = if (uri.scheme == null || uri.scheme == "file") {
            Uri.fromFile(File(uri.path?.substringBefore("?") ?: return null))
        } else {
            uri
        }
        val absolutePath: String? = when (cleanUri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                context.contentResolver.query(cleanUri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) else null
                }
            }
            ContentResolver.SCHEME_FILE    -> cleanUri.path
            else                           -> cleanUri.path
        }

        return absolutePath?.takeIf { File(it).exists() }
    }


}