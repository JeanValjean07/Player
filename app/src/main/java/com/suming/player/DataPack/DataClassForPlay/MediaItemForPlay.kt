package com.suming.player.DataPack.DataClassForPlay

import android.net.Uri

data class MediaItemForPlay (
    val media_api_SPECIFIC_ID: String = "",
    val media_api_NUM_ID: Long = 0,
    val media_api_dateAdded: Long = 0,
    val media_SPECIFIC_MediaType: String = "",
    val content_uriString: String = "",
    val content_uriStandard: String = "",
    val file_path: String = "",
    val file_name: String = "",
    val file_size: Long = 0L,
    val media_title: String = "",
    val media_artist: String = "",
    val media_durationMs: Long = 0L,
    val media_format: String = "",

    //类型专属
    val video_videoHeight: Long = 0L,
    val video_videoWidth: Long = 0L,
    var video_actualFPS: Float = 0f,


    )