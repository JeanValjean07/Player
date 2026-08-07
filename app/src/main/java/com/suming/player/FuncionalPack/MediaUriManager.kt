package com.suming.player.FuncionalPack

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import java.io.File

@Suppress() //"unused",
object MediaUriManager {


    const val Uri_Part_Video = "video"
    const val Uri_Part_Audio = "audio"


    //合成媒体Uri
    fun GET_MediaUriBy(mediaType: String, NUM_ID: Long): Pair<Boolean, String> {
        val type = if (mediaType == MediaType.Video) Uri_Part_Video else if (mediaType == MediaType.Audio) Uri_Part_Audio else {
            return Pair(false, "")
        }

        //合成媒体链接
        try{
            val uri = "content://media/external/$type/media/$NUM_ID".toUri()

            return Pair(true, uri.toString())
        }catch(e: Exception){
            consoleLog("GET_MediaUriBy-合成媒体Uri失败: $e")

            return Pair(false, "")
        }
    }
    fun GET_MediaUriBy(SPECIFIC_ID: String): Pair<Boolean, String> {

        val mediaType = SPECIFIC_ID.substringBefore("_")
        val NUM_ID = SPECIFIC_ID.substringAfter("_")

        //合成媒体链接
        try{
            val uri = "content://media/external/$mediaType/media/$NUM_ID".toUri()

            return Pair(true, uri.toString())
        }catch(e: Exception){
            consoleLog("GET_MediaUriBy-合成媒体Uri失败: $e")

            return Pair(false, "")
        }
    }


    //检查uri是否是标准格式
    fun isMediaUriStandard(mediaUriString: String): Boolean {
        //使用正则表达式判断(目前通过video和audio两种类型)
        val regex = """^content://media/external/(?:video|audio)/media/\d+$""".toRegex()

        return regex.matches(mediaUriString)
    }

    //转换非标准链接为标准链接(自带是否标准检测)
    fun getStandardMediaUri(mediaUriString: String, context: Context): String {
        //再次检查uri是否是标准格式,是标准格式时直接返回
        if (isMediaUriStandard(mediaUriString)) return mediaUriString

        //提取文件路径
        val filePath = GET_FilePath(context,mediaUriString.toUri())
        consoleLog("检查uri是否是标准格式-取到filePath: $filePath")
        if (filePath == null) return ""

        //查询数据库获取媒体Uri
        val mediaUri = searchUriBySysMediaApi(filePath, context)
        consoleLog("检查uri是否是标准格式-取到Uri: $mediaUri")
        if (mediaUri == Uri.EMPTY) return ""


        return mediaUri.toString()
    }

    //从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
    fun searchUriBySysMediaApi(filePath: String, context: Context): Uri {
        //构建查询
        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DATA} = ?"
        val selectionArgs = arrayOf(filePath)
        var cursor: Cursor? = null


        return try{
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
                consoleLog("id: $id")
                //构建标准uri
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }else{
                consoleLog("未查询到媒体ID")
                Uri.EMPTY
            }
        }finally{
            cursor?.close()
        }
    }


    //工具函数
    //从uri获取文件绝对路径
    private fun GET_FilePath(context: Context, uri: Uri): String? {
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


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaUriManager: $msg")
        }
    }

}