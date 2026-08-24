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


    const val Undefined = ""
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
        //去除尾部多余参数
        val cleanUri = mediaUriString.substringBefore('?').substringBefore('#')

        //使用正则表达式判断(目前通过video和audio两种类型)
        val regex = """^content://media/(?:external|internal)/(?:video|audio)/media/\d+$""".toRegex()


        return regex.matches(cleanUri)
    }

    fun convertFileUriToMediaUri(context: Context, uri: Uri): Uri {
        //检查是否已经是media URI
        if (uri.toString().contains("/(video|audio)/media/")){
            consoleLog("convertFileUriToMediaUri-已经是media URI: $uri")
            return uri
        }

        // 从file URI中提取ID或路径
        val filePath = GET_FilePath(context,uri)
        consoleLog("convertFileUriToMediaUri-获取文件路径 :filePath = $filePath")
        if (filePath == "") {
            consoleLog("convertFileUriToMediaUri-获取文件路径失败:filePath 为空")
            return uri
        }

        //查询video表获取标准URI
        val videoUri = searchUriBySysMediaApi(filePath, context)
        consoleLog("convertFileUriToMediaUri-查询video表获取标准URI :videoUri = $videoUri")

        return videoUri
    }

    //获取标准媒体Uri
    fun GET_StandardMediaUri(mediaUriString: String, context: Context): String {
        if (mediaUriString.isEmpty()) return Undefined

        //去除参数
        val cleanUri = mediaUriString.substringBefore("?")

        //检查是否标准
        if (isMediaUriStandard(cleanUri)) return cleanUri

        //
        val uri = cleanUri.toUri()
        val standardUri = when (uri.scheme) {
            "content" -> convertContentUriDirectly(context, uri)
            "file", null -> convertFileUriToMedia(context, uri)
            else -> null
        }

        return standardUri?.toString() ?: Undefined
    }

    private fun convertContentUriDirectly(context: Context, uri: Uri): Uri? {
        // 直接通过ID查询所有表
        val id = uri.path?.substringAfterLast("/")?.toLongOrNull() ?: return null

        val tables = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )

        for (tableUri in tables) {
            val testUri = ContentUris.withAppendedId(tableUri, id)
            context.contentResolver.query(testUri, arrayOf("_ID"), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    return testUri
                }
            }
        }
        return null
    }

    private fun convertFileUriToMedia(context: Context, uri: Uri): Uri? {
        // 从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
        return searchUriBySysMediaApi(uri.path?.substringBefore("?") ?: return null, context)
    }

    //从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
    fun searchUriBySysMediaApi(file_path: String, context: Context): Uri {
        //尝试所有可能的表
        val tables = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )

        for (tableUri in tables) {
            val uri = queryTable(context, tableUri, file_path)
            if (uri != Uri.EMPTY) {
                consoleLog("searchUriBySysMediaApi -在${tableUri}表中找到文件")
                return uri
            }
        }

        consoleLog("searchUriBySysMediaApi -所有表都未找到文件: $file_path")
        return Uri.EMPTY
    }

    private fun queryTable(context: Context, tableUri: Uri, file_path: String): Uri {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(file_path)

        context.contentResolver.query(tableUri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(tableUri, id)
                }
            }
        return Uri.EMPTY
    }


    //检测媒体uri类型模式(false = 降级链接)
    const val uriType_null = " uriType_null"
    const val uriType_full_permission = " uriType_full_permission"
    const val uriType_low_permission = " uriType_low_permission"
    fun detectMediaUriTypeMode(uri: Uri?): String {
        if (uri == null) return uriType_null

        val uriString = uri.toString()

        //检查是否是 content://
        if (!uriString.startsWith("content://")) {
            return uriType_null
        }

        //检查是否是 media
        if (uri.authority != "media") {
            return uriType_null
        }

        //获取路径部分
        val path = uri.path ?: return uriType_null

        return when {
            //匹配视频媒体路径: /external/video/media/ 或 /internal/video/media/
            path.contains("/video/media/") -> uriType_full_permission

            //匹配音频媒体路径
            path.contains("/audio/media/") -> uriType_full_permission

            //匹配图片媒体路径
            path.contains("/images/media/") -> uriType_full_permission

            //匹配文件通用路径: /external/file/ 或 /internal/file/
            path.contains("/file/") -> uriType_low_permission

            // 其他路径
            else -> uriType_null
        }
    }



    //从uri获取文件绝对路径
    fun GET_FilePath(context: Context, uri: Uri): String {
        try{
            //去除尾部多余参数
            val cleanUri = if (uri.scheme == null || uri.scheme == "file"){
                Uri.fromFile(File(uri.path?.substringBefore("?") ?: ""))
            }else{
                val baseUri = "${uri.scheme}://${uri.authority}${uri.path}"

                baseUri.toUri()
            }

            //根据uri类型获取绝对路径
            val absolutePath: String? = when (cleanUri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {

                    val projection = arrayOf(
                        MediaStore.MediaColumns.DATA,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Images.Media.DATA
                    )

                    var path: String? = null
                    val uris = listOf(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Files.getContentUri("external")
                    )

                    for (tableUri in uris) {
                        //从URI中提取ID
                        val id = cleanUri.path?.substringAfterLast("/")?.toLongOrNull()
                        if (id != null) {
                            val idUri = ContentUris.withAppendedId(tableUri, id)
                            context.contentResolver.query(idUri, projection, null, null, null)?.use { c ->
                                if (c.moveToFirst()) {
                                    for (column in projection) {
                                        val index = c.getColumnIndex(column)
                                        if (index != -1) {
                                            path = c.getString(index)
                                            if (!path.isNullOrEmpty()) break
                                        }
                                    }
                                }
                            }
                        }
                        if (!path.isNullOrEmpty()) break
                    }
                    path
                }
                ContentResolver.SCHEME_FILE    -> cleanUri.path
                else                           -> cleanUri.path
            }

            val filePath = absolutePath?.takeIf { File(it).exists() }

            return filePath ?: ""
        }catch(e: Exception){
            consoleLog("GET_FilePath-获取文件路径失败: $e")

            return ""
        }
    }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaUriManager: $msg")
        }
    }

}