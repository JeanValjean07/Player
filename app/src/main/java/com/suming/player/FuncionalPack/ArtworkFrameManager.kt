package com.suming.player.FuncionalPack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

@Suppress("unused")
object ArtworkFrameManager {

    //缩略图保存路径结构
    private const val artwork_path_video = "AlbumFrame/VideoCover/"
    private const val artwork_path_audio = "AlbumFrame/AudioAlbumCover/"


    //缓存文件路径
    private var artwork_File_path_video: File? = null
    private var artwork_File_path_audio: File? = null
    private fun initFile(context: Context){
        artwork_File_path_video = get_Artwork_Path_File_video(context)
        artwork_File_path_audio = get_Artwork_Path_File_music(context)
    }
    private fun get_Artwork_Path_File_video(context: Context): File {
        //视频文件的原保存路径
        val ArtworkPath_cover_video = File(context.filesDir, artwork_path_video)
        //确保文件夹存在
        if (!ArtworkPath_cover_video.exists()){
            ArtworkPath_cover_video.mkdirs()
        }

        return ArtworkPath_cover_video
    }
    private fun get_Artwork_Path_File_music(context: Context): File {
        //音频文件的原保存路径
        val ArtworkPath_cover_audio = File(context.filesDir, artwork_path_audio)
        //确保文件夹存在
        if (!ArtworkPath_cover_audio.exists()){
            ArtworkPath_cover_audio.mkdirs()
        }


        return ArtworkPath_cover_audio
    }



    //获取Artwork图片Bitmap
    fun GET_ArtworkFrame_Bitmap(context: Context, type: String, artwork_media_api_id: Long): Bitmap? {
        when(type){
            MediaType.Video -> {
                //初始化路径对象
                if (artwork_File_path_video == null) initFile(context)
                //合成目标文件对象
                val artwork_Frame_File = File(artwork_File_path_video, "${artwork_media_api_id}.webp")

                //检查目标图是否存在
                if (artwork_Frame_File.exists()){
                    //取出图片
                    val artwork_Frame_Bitmap = BitmapFactory.decodeFile(artwork_Frame_File.absolutePath)

                    return artwork_Frame_Bitmap
                }else{
                    return null
                }
            }
            MediaType.Audio -> {
                //初始化路径对象
                if (artwork_File_path_audio == null) initFile(context)
                //合成目标文件对象
                val artwork_Frame_File = File(artwork_File_path_audio, "${artwork_media_api_id}.webp")

                //检查目标图是否存在
                if (artwork_Frame_File.exists()){
                    //取出图片
                    val artwork_Frame_Bitmap = BitmapFactory.decodeFile(artwork_Frame_File.absolutePath)

                    return artwork_Frame_Bitmap
                }else{
                    return null
                }
            }
            else -> {
                return null
            }
        }
    }

    //获取Artwork图片uri
    fun GET_ArtworkFrame_Uri(context: Context, type: String, artwork_media_api_id: Long): Uri? {
        when(type){
            MediaType.Video -> {
                //拿到保存路径
                if (artwork_File_path_video == null) initFile(context)

                //合成目标文件对象
                val artwork_Frame_File = File(artwork_File_path_video, "${artwork_media_api_id}.webp")
                //尝试拿到文件uri
                if(artwork_Frame_File.exists()){
                    return try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", artwork_Frame_File)
                    }catch (e: Exception){
                        null
                    }
                }else{
                    return null
                }
            }
            MediaType.Audio -> {
                //拿到保存路径
                if (artwork_File_path_audio == null) initFile(context)

                //合成目标文件对象
                val artwork_Frame_File = File(artwork_File_path_audio, "${artwork_media_api_id}.webp")
                //尝试拿到文件uri
                if(artwork_Frame_File.exists()){
                    return try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", artwork_Frame_File)
                    }catch (e: Exception){
                        null
                    }
                }else{
                    return null
                }
            }
            else -> {
                return null
            }
        }
    }





    //保存Bitmap到文件系统
    fun SAVE_ArtworkFrame_Bitmap(context: Context ,type: String, artwork_media_api_id: Long, artwork_Frame_Bitmap: Bitmap){
        when(type){
            MediaType.Video -> {
                if (artwork_File_path_video == null) initFile(context)


                CORE_saveFile_Bitmap(artwork_File_path_video!!, "${artwork_media_api_id}.webp", artwork_Frame_Bitmap)

            }
            MediaType.Audio -> {
                if (artwork_File_path_audio == null) initFile(context)


                CORE_saveFile_Bitmap(artwork_File_path_audio!!, "${artwork_media_api_id}.webp", artwork_Frame_Bitmap)
            }
        }
    }




    //可高度通用的核心函数
    //保存Bitmap到文件系统
    fun CORE_saveFile_Bitmap(parent_path: File, file_name: String, bitmap: Bitmap){
        val file = File(parent_path, file_name)
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.WEBP, 20, it)
        }
    }


    //删除缩略图
    fun delete_artwork(context: Context, deleteVideo: Boolean = false, deleteAudio: Boolean = false): Boolean {
        try{
            if (deleteVideo){
                if (artwork_File_path_video != null){
                    File(context.filesDir, artwork_path_video).deleteRecursively()
                    artwork_File_path_video?.mkdirs()
                }
            }
            if (deleteAudio){
                if (artwork_File_path_audio != null){
                    File(context.filesDir, artwork_path_audio).deleteRecursively()
                    artwork_File_path_audio?.mkdirs()
                }
            }

            return true
        }catch(e: Exception){
            consoleLog("删除_artwork失败: ${e.message}")

            return false
        }

    }

    //日志
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "ArtworkFrameManager: $msg")
        }
    }
}