package com.suming.player.ListManager

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import data.MediaModel.MiniMediaItemForList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

object PlayerListManager {


    var customList = mutableListOf<MiniMediaItemForList>()

    private val coroutineScope_updateList = CoroutineScope(Dispatchers.IO + SupervisorJob())


    //自定义列表操作
    fun InsertItemToCustomList(uriString: String, context: Context){
        val item = getMediaInfo(uriString, context)

        if (item == MiniMediaItemForList.EMPTY) return

        if (customList.any { it.uriNumOnly == item.uriNumOnly }) return
        customList.add(item)

    }

    fun DeleteItemFromCustomList(uriNumOnly: Long){
        customList.removeAll { it.uriNumOnly == uriNumOnly }

    }

    fun updateList(newList: List<MiniMediaItemForList>) {
        customList.clear()
        customList.addAll(newList)
    }


    //媒体信息读取
    private lateinit var retriever: MediaMetadataRetriever
    private var state_MediaMetadataRetriever_Running = false
    private fun getMediaInfo(uriString: String, context: Context): MiniMediaItemForList {
        if (!state_MediaMetadataRetriever_Running) {
            retriever = MediaMetadataRetriever()
            state_MediaMetadataRetriever_Running = true
        }
        //获得基本信息
        val MediaInfo_MediaUri = uriString.toUri()
        val MediaInfo_MediaID = MediaInfo_MediaUri.lastPathSegment?.toLongOrNull() ?: 0
        val MediaInfo_MediaUriNumOnly = MediaInfo_MediaUri.lastPathSegment?.toLongOrNull() ?: 0
        //设置解码器数据源
        try { retriever.setDataSource(context, MediaInfo_MediaUri) }
        catch (e: Exception) {
            Log.e("SuMing", "getMediaInfo: 解码媒体信息失败 $e")
            return MiniMediaItemForList.CREATOR.EMPTY
        }
        //解码更多信息
        val MediaInfo_MediaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "error"
        var MediaInfo_MediaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "error"
        val MediaInfo_MediaPath = getFilePath(context, MediaInfo_MediaUri).toString()
        var MediaInfo_MediaFileName = (File(MediaInfo_MediaPath)).name ?: "error"
        var MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "error"
        //处理和过滤取得值
        if (MediaInfo_MediaType.contains("video")) { MediaInfo_MediaType = "video" }
        else if (MediaInfo_MediaType.contains("audio")) { MediaInfo_MediaType = "music" }
        if (MediaInfo_MediaFileName == "error"){ MediaInfo_MediaFileName = "未知媒体标题" }
        if (MediaInfo_MediaArtist == "error") { MediaInfo_MediaArtist = "未知艺术家" }

        return MiniMediaItemForList(
            id = MediaInfo_MediaID,
            uri = MediaInfo_MediaUri,
            uriNumOnly = MediaInfo_MediaUriNumOnly,
            filename = MediaInfo_MediaFileName,
            title = MediaInfo_MediaTitle,
            artist = MediaInfo_MediaArtist,
            type = MediaInfo_MediaType

        )

    }
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
    private fun stopMediaMetadataRetriever() {
        if (!state_MediaMetadataRetriever_Running) {
            return
        }
        retriever.release()
        state_MediaMetadataRetriever_Running = false


        //MiniMediaItemForList需要包含：
        //val id: Long = 0,
        //val uri: Uri,
        //val uriNumOnly: Long = 0,
        //val filename: String,
        //val title: String = "",
        //val artist: String = "",
        //val type: String = ""


    }


    //主动关闭
    fun onDestroy() {
        coroutineScope_updateList.cancel()
    }

}

//MiniMediaItemForList包含：
//id uri uriNumOnly filename title artist type