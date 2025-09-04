package com.suming.player.data.model


import java.io.File

data class ThumbItem(
    val videoPath: String,
    var thumbnailPath: File? = null,
    val duration: Long = 0,
    val captureInterval: Long = 5, // 缩略图捕获间隔(秒)
    var isLoaded: Boolean = false, // 是否已加载
    var isSaved: Boolean = false, // 是否已保存
    var running: Boolean = false,
    var coverPath: File? = null,
    var isSet: Boolean = false,
    var showPlaceholder: Boolean = true, // 是否显示占位图
    var isCoverPlaced: Boolean = false,




    var realThumbGenerated: Boolean = false,

    var currentThumbType: Boolean = false,

    var coverThumbBinded: Boolean = false,
    var realThumbBinded: Boolean = false,
)

