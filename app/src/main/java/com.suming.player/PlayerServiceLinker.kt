package com.suming.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PlayerServiceLinker {

    private var MediaInfo_MediaType = ""
    private var MediaInfo_MediaUriString: String = ""
    private var MediaInfo_FileName: String = ""
    private var MediaInfo_MediaArtist: String = ""


    private val _MediaType = MutableStateFlow(0L)
    val MediaType: StateFlow<Long> = _MediaType


    fun setMediaInfo_MediaType(mediaType: String) {


        MediaInfo_MediaType = mediaType


    }


    fun setMediaBasicInfo(uriString: String, fileName: String, mediaArtist: String) {

        MediaInfo_MediaUriString = uriString

        MediaInfo_FileName = fileName

        MediaInfo_MediaArtist = mediaArtist

    }

    fun getMediaBasicInfo(): Triple<String, String, String> {
        return Triple(MediaInfo_MediaUriString, MediaInfo_FileName, MediaInfo_MediaArtist)
    }

    fun getMediaInfo_MediaType(): String {
        return MediaInfo_MediaType
    }



//object END
}

