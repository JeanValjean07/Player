package com.suming.player

import android.content.Context
import java.io.File

object StorageManager {


    //缩略图保存路径
    private const val ArtworkPath_lastSegment_video = "Artwork/cover/video"
    private const val ArtworkPath_lastSegment_audio = "Artwork/cover/audio"



    //获取视频文件的缩略图保存路径
    fun get_ArtworkPath_cover_video(context: Context): File{
        //视频文件的缩略图保存路径
        val ArtworkPath_cover_video = File(context.filesDir, ArtworkPath_lastSegment_video)
        //确保文件夹存在
        if (!ArtworkPath_cover_video.exists()) ArtworkPath_cover_video.mkdirs()


        return ArtworkPath_cover_video
    }
    //获取音频文件的缩略图保存路径
    fun get_ArtworkPath_cover_audio(context: Context): File{
        //音频文件的缩略图保存路径
        val ArtworkPath_cover_audio = File(context.filesDir, ArtworkPath_lastSegment_audio)
        //确保文件夹存在
        if (!ArtworkPath_cover_audio.exists()) ArtworkPath_cover_audio.mkdirs()


        return File(context.filesDir, ArtworkPath_lastSegment_audio)
    }

}