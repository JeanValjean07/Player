package com.suming.player.FuncionalPack

object SupportFormat {

    //支持的视频格式 (逐个测试,先只添加已测试的)
    val VIDEO_FORMATS_SUPPORTED = setOf(
        "mp4"
    )

    //支持的音频格式
    val AUDIO_FORMATS_SUPPORTED = setOf(
        "mp3","mpeg","flac","x-ms-wma"
    )

    //所有支持的格式
    val ALL_FORMATS_SUPPORTED = VIDEO_FORMATS_SUPPORTED + AUDIO_FORMATS_SUPPORTED


    //检查是否支持格式
    fun isFormatSupported(format: String): Boolean {
        return format.lowercase() in ALL_FORMATS_SUPPORTED
    }


}