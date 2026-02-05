package com.suming.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PlayerServiceLinker {

    private var MediaInfo_MediaType = ""
    private var MediaInfo_MediaUriString: String = ""
    private var MediaInfo_FileName: String = ""
    private var MediaInfo_MediaArtist: String = ""


    //未使用
    private val _MediaType = MutableStateFlow(0L)
    val MediaType: StateFlow<Long> = _MediaType


    //外部写入媒体类型
    fun setMediaInfo_MediaType(mediaType: String) {

        MediaInfo_MediaType = mediaType
    }
    //获取媒体类型
    fun getMediaInfo_MediaType(): String {
        return MediaInfo_MediaType
    }


    //写入媒体基本信息< uri字符串 丨 文件名 丨 参与创作的艺术家 >
    fun setMediaBasicInfo(uriString: String, fileName: String, mediaArtist: String) {

        MediaInfo_MediaUriString = uriString

        MediaInfo_FileName = fileName

        MediaInfo_MediaArtist = mediaArtist

    }
    //获取媒体基本信息< uri字符串 丨 文件名 丨 参与创作的艺术家 >
    fun getMediaBasicInfo(): Triple<String, String, String> {
        return Triple(MediaInfo_MediaUriString, MediaInfo_FileName, MediaInfo_MediaArtist)
    }





//object END
}

