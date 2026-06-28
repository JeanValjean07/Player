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

    //此对象承担缩略图的获取和保存工作
    //注意：文件名使用媒体的唯一ID
    //{item.uriNumOnly}
    //类型为 Long


    //缩略图保存路径结构
    private const val artwork_path_video = "Artwork/cover/video"
    private const val artwork_path_audio = "Artwork/cover/audio"
    //媒体类型标识
    const val artwork_type_video = "video"
    const val artwork_type_audio = "audio"
    const val artwork_type_music = "music"


    //缓存文件路径实例
    private var artwork_File_path_video: File? = null
    private var artwork_File_path_audio: File? = null
    private fun initFile(context: Context){
        artwork_File_path_video = get_Artwork_Path_video(context)
        artwork_File_path_audio = get_Artwork_Path_music(context)
    }

    //获取Artwork图片(自动获取)
    fun get_Artwork_Frame_Bitmap(context: Context,type: String, artwork_name_uriNumOnly: Long): Bitmap? {
        var type = type
        if(type == artwork_type_music){
            type = artwork_type_audio
        }
        when(type){
            artwork_type_video -> {
                //拿到保存路径
                if (artwork_File_path_video == null){
                    initFile(context)
                }
                //根据文件名去找图
                val artwork_Frame_File = File(artwork_File_path_video, "${artwork_name_uriNumOnly}.webp")

                //检查目标图是否存在
                if (artwork_Frame_File.exists()){
                    //取出图片
                    val artwork_Frame_Bitmap = BitmapFactory.decodeFile(artwork_Frame_File.absolutePath)

                    return artwork_Frame_Bitmap
                }else{
                    //未找到视频文件的缩略图,记录日志
                    consoleLog("ArtworkFrameManager: 未找到视频文件的缩略图: uriNumOnly=${artwork_name_uriNumOnly}")
                    return null
                }
            }
            artwork_type_audio -> {
                //音频文件的原保存路径
                val ArtworkPath_cover_audio = get_Artwork_Path_music(context)
                //根据文件名去找图
                val artwork_Frame_File = File(ArtworkPath_cover_audio, "${artwork_name_uriNumOnly}.webp")
                //检查目标图是否存在
                if (artwork_Frame_File.exists()){
                    //取出图片
                    val artwork_Frame_Bitmap = BitmapFactory.decodeFile(artwork_Frame_File.absolutePath)

                    return artwork_Frame_Bitmap
                }else{
                    //未找到音频文件的缩略图,记录日志
                    consoleLog("ArtworkFrameManager: 未找到音频文件的缩略图: uriNumOnly=${artwork_name_uriNumOnly}")
                    return null
                }
            }
            else -> {
                //传入了非目标类型,记录日志
                consoleLog("ArtworkFrameManager: 传入了非目标类型: $type")
                return null
            }
        }
    }

    //获取Artwork的uri
    fun get_Artwork_Frame_Uri(context: Context,type: String, artwork_name_uriNumOnly: Long): Uri? {
        consoleLog("ArtworkFrameManager: 获取缩略图uri: type=$type, uriNumOnly=${artwork_name_uriNumOnly}")
        when(type){
            artwork_type_video -> {
                //拿到保存路径
                if (artwork_File_path_video == null){
                    initFile(context)
                }
                //根据文件名去找图
                val artwork_Frame_File = File(artwork_File_path_video, "${artwork_name_uriNumOnly}.webp")
                //拿到文件uri
                if(artwork_Frame_File.exists()){
                    return try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", artwork_Frame_File)
                    }catch (e: Exception){
                        consoleLog("ArtworkFrameManager: 获取视频文件的缩略图uri失败: $e , ${e.message}")
                        null
                    }
                }
                return null
            }
            artwork_type_audio -> {
                //拿到保存路径
                if (artwork_File_path_audio == null){
                    initFile(context)
                }
                //根据文件名去找图
                val artwork_Frame_File = File(artwork_File_path_audio, "${artwork_name_uriNumOnly}.webp")
                //拿到文件uri
                if(artwork_Frame_File.exists()){
                    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", artwork_Frame_File)
                }
                return null
            }
            artwork_type_music -> {
                consoleLog("ArtworkFrameManager:artwork_type_music, uriNumOnly=${artwork_name_uriNumOnly}")
                //拿到保存路径
                if (artwork_File_path_audio == null){
                    initFile(context)
                }
                //根据文件名去找图
                val artwork_Frame_File = File(artwork_File_path_audio, "${artwork_name_uriNumOnly}.webp")
                //拿到文件uri
                if(artwork_Frame_File.exists()){
                    consoleLog("ArtworkFrameManager: 文件存在")
                    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", artwork_Frame_File)
                }
                consoleLog("ArtworkFrameManager: 文件不存在")
                return null
            }
            else -> {
                //传入了非目标类型,记录日志
                consoleLog("ArtworkFrameManager: 传入了非目标类型: $type")
                return null
            }
        }
    }


    //获取artwork的原保存路径
    fun get_Artwork_Path_video(context: Context): File {
        //视频文件的原保存路径
        val ArtworkPath_cover_video = File(context.filesDir, artwork_path_video)
        //确保文件夹存在
        if (!ArtworkPath_cover_video.exists()){
            ArtworkPath_cover_video.mkdirs()
        }



        return ArtworkPath_cover_video
    }
    fun get_Artwork_Path_music(context: Context): File {
        //音频文件的原保存路径
        val ArtworkPath_cover_audio = File(context.filesDir, artwork_path_audio)
        //确保文件夹存在
        if (!ArtworkPath_cover_audio.exists()){
            ArtworkPath_cover_audio.mkdirs()
        }


        return ArtworkPath_cover_audio
    }

    //保存Bitmap
    fun save_Artwork_Frame_Bitmap(context: Context ,type: String, artwork_name_uriNumOnly: Long, artwork_Frame_Bitmap: Bitmap){
        var type = type
        if(type == artwork_type_music){
            type = artwork_type_audio
        }
        when(type){
            artwork_type_video -> {
                if (artwork_File_path_video == null){
                    initFile(context)
                }

                val cover_item_file = File(artwork_File_path_video, "${artwork_name_uriNumOnly}.webp")
                cover_item_file.outputStream().use {
                    artwork_Frame_Bitmap.compress(Bitmap.CompressFormat.WEBP, 20, it)
                }

            }
            artwork_type_audio -> {
                if (artwork_File_path_audio == null){
                    initFile(context)
                }

                val cover_item_file = File(artwork_File_path_audio, "${artwork_name_uriNumOnly}.webp")
                cover_item_file.outputStream().use {
                    artwork_Frame_Bitmap.compress(Bitmap.CompressFormat.WEBP, 20, it)
                }
            }
            else -> {
                //传入了非目标类型,记录日志
                consoleLog("ArtworkFrameManager: 传入了非目标类型: $type")
            }
        }
    }





    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", msg)
        }
    }
}