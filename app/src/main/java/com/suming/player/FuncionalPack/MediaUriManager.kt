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
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URLDecoder


@Suppress("/unused")
object MediaUriManager {

    //空字段
    const val Undefined = ""





    //传入 FileProvider_URI 输出 标准MediaStore URI + 文件路径
    fun detect_FileProvider_URI(uri: Uri, context: Context): Pair<Uri, String> {

        //从 FileProvider_URI 中 提取文件路径
        val file_path = GET_file_path_from_file_uri(uri)
        consoleLog("detect_FileProvider_URI -获取文件路径 -file_path:$file_path")
        if (file_path == Undefined) {
            consoleLog("detect_FileProvider_URI -获取文件路径失败:file_path 为空")
            return Pair(uri, file_path)
        }

        //获取标准 MediaStore URI
        //检查是否已经是 MediaStore 标准 MediaURI
        if (uri.toString().contains("/(video|audio)/media/")){
            consoleLog("detect_FileProvider_URI -已经是media URI: $uri")
            return Pair(uri, file_path)
        }

        //查询 MediaStore 获取 标准MediaStore URI
        val URI_Standard = searchUriBySysMediaApi(file_path, context)
        consoleLog("detect_FileProvider_URI -查询video表获取标准URI :URI_Standard = $URI_Standard")
        if (URI_Standard == Uri.EMPTY){
            consoleLog("detect_FileProvider_URI -查询video表获取标准URI失败:file_path = $file_path")
            return Pair(uri, file_path)
        }


        return Pair(URI_Standard, file_path)

    }

    //传入 文件路径 输出 标准MediaStore URI
    fun detect_FilePath(file_path: String, context: Context): String {
        //查询 MediaStore 获取 标准MediaStore URI
        val URI_Standard = searchUriBySysMediaApi(file_path, context)
        //consoleLog("detect_FilePath -查询video表获取标准URI :URI_Standard = $URI_Standard")
        if (URI_Standard == Uri.EMPTY){
            consoleLog("detect_FilePath -查询video表获取标准URI失败:file_path = $file_path")
            return Undefined
        }


        return URI_Standard.toString()
    }


    //检测媒体uri类型模式 传入 URI 输出 UriTypeMode
    const val uriType_null = "uriType_null"
    const val uriType_media_store_detail = "uriType_media_store_detail"
    const val uriType_media_store_file = "uriType_media_store_file"
    const val uriType_contain_file_path = "uriType_contain_file_path"
    fun detectMediaUriTypeMode(URI: Uri?): String {
        //consoleLog("detectMediaUriTypeMode -URI = $URI")
        if (URI == null) return uriType_null
        //缓存 URI_String
        val URI_String = URI.toString()

        //检查是否是 content:// 起手
        if (URI_String.startsWith("content://")){
            //consoleLog("detectMediaUriTypeMode -是content:// URI: $URI_String")
            //检查authority和path
            val authority = URI.authority ?: Undefined
            val path = URI.path ?: Undefined
            //consoleLog("detectMediaUriTypeMode -检查authority和path authority:${authority},path:$path")

            when {
                //MediaStore URI Authority
                (authority.contains("media")) -> {
                    //获取路径部分
                    val path = URI.path ?: return uriType_null

                    return when {
                        //匹配视频媒体路径: /external/video/media/ 或 /internal/video/media/
                        path.contains("/video/media/") -> uriType_media_store_detail

                        //匹配音频媒体路径
                        path.contains("/audio/media/") -> uriType_media_store_detail

                        //匹配图片媒体路径
                        path.contains("/images/media/") -> uriType_media_store_detail

                        //匹配文件通用路径: /external/file/ 或 /internal/file/
                        path.contains("/file/") -> uriType_media_store_file

                        //其他路径
                        else -> uriType_null
                    }
                }
                //包含文件路径
                ((path.contains("storage/emulated/0"))) -> {
                    return uriType_contain_file_path
                }

                //其他
                else -> return uriType_null

            }
        }else{
            //consoleLog("detectMediaUriTypeMode -不是content:// URI: $URI_String")
            //包含文件路径
            if (URI_String.contains("storage/emulated/0")){
                return uriType_contain_file_path
            }else{
                return uriType_null
            }
        }

    }

    //检测 URI 对应媒体 是否存在(传入URI 输出 是否存在)(只能判断 标准 MediaStore URI,其他URI是不行的)
    fun isUriExistInMediaStore(URI: Uri, context: Context): Boolean {
        consoleLog("isUriExistInMediaStore -URI = $URI")
        if (URI == Uri.EMPTY) {
            consoleLog("isUriExistInMediaStore -URI 为空")
            return false
        }

        val resolver = context.contentResolver
        var cursor: Cursor? = null
        try {
            //从URI中提取ID (只能处理 标准MediaStore URI)
            val id = ContentUris.parseId(URI)
            consoleLog("isUriExistInMediaStore -从 URI 中提取 ID :id = $id")


            val queryUri: Uri?
            val authority = URI.authority ?: return false
            if (MediaStore.AUTHORITY == authority){
                queryUri = URI
            }else{
                consoleLog("isUriExistInMediaStore -URI 不是 MediaStore URI")
                return false
            }

            val column = MediaStore.MediaColumns._ID
            val projection: Array<String> = arrayOf(column)
            cursor = resolver.query(queryUri, projection, null, null, null)


            //游标可移动代表媒体存在
            return cursor != null && cursor.moveToFirst()

        }catch(e: IllegalArgumentException){
            consoleLog("isUriExistInMediaStore -发生错误: $e")
            return false
        }finally{
            cursor?.close()
        }
    }

    //解码文件路径(万一路径里面有编码)
    fun decode_file_path(file_path_e: String): String {
        val decoded_file_path = URLDecoder.decode(file_path_e, "UTF-8")
        //consoleLog("decode_file_path -去除URI编码后的文件路径: $decoded_file_path")

        return decoded_file_path
    }




    //检查文件是否存在(基于Stream 输入URI)
    fun isFileExist(resolver: ContentResolver, uri: Uri): Boolean {
        try {
            val `is` = resolver.openInputStream(uri)
            if (`is` != null) {
                `is`.close() //能打开说明文件存在且可读
                return true
            }else{
                consoleLog("isFileExist -文件打不开")
                return false
            }
        }catch(e: FileNotFoundException) {
            consoleLog("isFileExist -文件不存在: $e")
            return false
        }catch(e: IOException) {
            consoleLog("isFileExist -其他 IO 错误: $e")
            return false
        }
    }
    //检查文件是否存在(基于IO File 输入 文件路径)
    fun isFileExist(file_path: String): Boolean {
        if (file_path == Undefined) {
            consoleLog("isFileExist -文件路径为空")
            return false
        }

        return File(file_path).exists()
    }


    //检查URI 是否是 MediaStore 标准 MediaURI
    fun spy_is_string_matches_a_MediaStore_S_URI(URI_String: String): Boolean {
        //去除尾部多余参数
        val cleanUri = URI_String.substringBefore('?').substringBefore('#')

        //使用正则表达式判断(目前通过video和audio两种类型)
        val regex = """^content://media/(?:external|internal)/(?:video|audio)/media/\d+$""".toRegex()


        return regex.matches(cleanUri)
    }
    //检查URI 是否是 MediaStore 标准 MediaURI并返回末尾数字(用字符串返回)(保底返回0)
    fun GET_NUM_ID_from_MediaStoreURI(URI_String: String): String {
        //使用正则表达式判断(目前通过video和audio两种类型)
        val regex = """^content://media/(?:external|internal)/(?:video|audio)/media/\d+$""".toRegex()
        if (URI_String.matches(regex)){
            val matchResult = regex.find(URI_String)

            return matchResult?.groupValues?.get(1) ?: Undefined
        }else{
            return Undefined
        }

    }

    //检查一个字符串是否实际上是一个文件路径
    fun spy_is_string_actually_a_file_path(s: String): Boolean {
        if (s.startsWith("content://")) return false
        if (s.startsWith("file://")) return false

        return s.contains("storage/emulated/0")
    }

    //传入 任意类型URI 输出 标准 MediaStore URI,降级返回值为空
    fun GET_STD_MediaStoreURI_from_Any_URI(URI_String: String, context: Context): String {
        if (URI_String.isEmpty()) return Undefined

        //仅在原URI 是 MediaStore 标准 MediaURI 才时才尝试转换
        if (!URI_String.startsWith("content://media/external/")) return Undefined

        //去除可能存在的额外参数
        val cleanUri = URI_String.substringBefore("?")

        //检查是否标准
        if (spy_is_string_matches_a_MediaStore_S_URI(cleanUri)) return cleanUri

        //转换为标准 MediaStore URI
        val uri = cleanUri.toUri()
        val standardUri = when (uri.scheme) {
            "content" -> convertContentUriDirectly(context, uri)
            else -> null
        }

        return standardUri?.toString() ?: Undefined
    }

    private fun convertContentUriDirectly(context: Context, uri: Uri): Uri? {
        //直接通过ID查询所有表
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



    fun convert_MediaStoreFileURI_to_MediaStoreMediaURI(context: Context, uri: Uri): Uri {
        //检查是否已经是 MediaStore 标准 MediaURI
        if (uri.toString().contains("/(video|audio)/media/")){
            consoleLog("convertFileUriToMediaUri-已经是media URI: $uri")
            return uri
        }

        //从 MediaStoreFileURI 中 提取文件路径
        val filePath = GET_file_path_from_media_uri(context,uri)
        consoleLog("convert_MediaStoreFileURI_to_MediaStoreMediaURI -获取文件路径 :filePath = $filePath")
        if (filePath == "") {
            consoleLog("convertFileUriToMediaUri-获取文件路径失败:filePath 为空")
            return uri
        }
        //-获取文件路径 :filePath = /storage/emulated/0/Movies/游戏录像/260527 - 西海岸高速酒吧.mp4

        //查询video表获取标准URI
        val videoUri = searchUriBySysMediaApi(filePath, context)
        consoleLog("convert_MediaStoreFileURI_to_MediaStoreMediaURI -查询video表获取标准URI :videoUri = $videoUri")

        return videoUri
    }


    fun convert_FileManagerFileURI_to_MediaStoreMediaURI(context: Context, uri: Uri): Pair<Uri,String> {
        //检查是否已经是 MediaStore 标准 MediaURI
        if (uri.toString().contains("/(video|audio)/media/")){
            consoleLog("convert_FileManagerFileURI_to_MediaStoreMediaURI -已经是media URI: $uri")
            return Pair(uri, Undefined)
        }

        //从 FileManagerFileURI 中 提取文件路径
        val filePath = GET_file_path_from_file_uri(uri)
        consoleLog("convert_FileManagerFileURI_to_MediaStoreMediaURI -获取文件路径 :filePath = $filePath")
        if (filePath == Undefined) {
            consoleLog("convert_FileManagerFileURI_to_MediaStoreMediaURI -获取文件路径失败:filePath 为空")
            return Pair(uri, filePath)
        }
        //-获取文件路径 :filePath = /storage/emulated/0/Movies/精选/SL/Cecelia Taylor.mp4

        //查询video表获取标准URI
        val videoUri = searchUriBySysMediaApi(filePath, context)
        consoleLog("convert_FileManagerFileURI_to_MediaStoreMediaURI -查询video表获取标准URI :videoUri = $videoUri")

        return Pair(videoUri, filePath)

    }






    private fun convertFileUriToMedia(context: Context, uri: Uri): Uri? {
        // 从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
        return searchUriBySysMediaApi(uri.path?.substringBefore("?") ?: return null, context)
    }

    //从文件路径获取uri(需查询系统媒体库)(路径必须是绝对实际路径)
    fun searchUriBySysMediaApi(file_path: String, context: Context): Uri {
        //所有可能的表
        val tables = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Files.getContentUri("external")
        )

        for (tableUri in tables){
            val uri = queryTable(context, tableUri, file_path)
            if (uri != Uri.EMPTY){
                //找到文件
                consoleLog("searchUriBySysMediaApi -在${tableUri}表中找到文件")
                return uri
            }
        }
        //开始尝试模糊匹配
        for (tableUri in tables){
            val fileName = file_path.substringAfterLast("/")
            val parentDir = file_path.substringBeforeLast("/")
            val uri = queryTableFuzzyMatch(context, tableUri, fileName, parentDir)
            if (uri != Uri.EMPTY){
                //找到文件
                consoleLog("searchUriBySysMediaApi -在${tableUri}表中找到文件")
                return uri
            }
        }

        //未找到任何文件
        //consoleLog("searchUriBySysMediaApi -所有表都未找到文件: $file_path")
        return Uri.EMPTY
    }
    //查单个表工具函数
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
    //查单个表工具函数-模糊匹配
    private fun queryTableFuzzyMatch(context: Context, tableUri: Uri, fileName: String, parentDir: String): Uri {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
            )

            // 使用 DISPLAY_NAME 和 RELATIVE_PATH 进行模糊匹配
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "%$parentDir%")

            context.contentResolver.query(tableUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        return ContentUris.withAppendedId(tableUri, id)
                    }
                }

        } catch (e: Exception) {
            consoleLog("queryTableFuzzyMatch -查询失败: $e")
        }
        return Uri.EMPTY
    }




    //处理特殊 URI (简版,几乎只考虑了华为的私有URI,可扩展) //TODO
    fun processSpecialUri(uriString: String): Uri {
        //去除问号后面的内容
        val cleanUri = uriString.substringBefore("?")
        consoleLog("processSpecialUri -去除问号后面的内容 -cleanUri = $cleanUri")

        //加上前缀
        val contentUri = "content://114514/$cleanUri".toUri()
        consoleLog("processSpecialUri -加上前缀 -contentUri = $contentUri")

        return contentUri
    }


    //从 MediaStore URI 获取文件绝对路径
    fun GET_file_path_from_media_uri(context: Context, uri: Uri): String {
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

            return filePath ?: Undefined
        }catch(e: Exception){
            consoleLog("GET_file_path_from_media_uri -获取文件路径失败: $e")

            return Undefined
        }
    }

    //从 文件URI 获取文件路径
    fun GET_file_path_from_file_uri(uri: Uri): String {
        try{
            val path = uri.path ?: return Undefined

            // 方法1: 直接提取路径
            val filePath = path.replace("/root", "") // 移除 /root 前缀
            // 或者使用更通用的方法
            val storageIndex = path.indexOf("/storage/emulated/")
            if (storageIndex != -1) {
                return path.substring(storageIndex)
            }

            // 解码URL编码的路径
            return try {
                URLDecoder.decode(path, "UTF-8")
            } catch (e: Exception) {
                path
            }

        }catch(e: Exception){
            consoleLog("GET_file_path_from_file_uri -获取文件路径失败: $e")

            return Undefined
        }
    }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaUriManager: $msg")
        }
    }

}