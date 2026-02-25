package com.suming.player

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit

@Suppress("unused")
class MediaRecordManager(context: Context) {

    //键值对文件实例
    private var INFO_MediaRecord: SharedPreferences = context.getSharedPreferences("INFO_MediaRecord", MODE_PRIVATE)


    //外部写入(2+2)
    //外部写入媒体项基本信息(文件名, 艺术家)
    fun save_MediaInfo_toRecord(filename: String, artist: String){
        INFO_MediaRecord.edit {
            putString("MediaInfo_FileName", filename)
            putString("MediaInfo_MediaArtist", artist)
        }
    }
    //外部写入媒体项唯一识别ID
    fun save_MediaInfo_toRecordUniqueID(mediaID: String,type: String){
        INFO_MediaRecord.edit {
            putString("MediaInfo_MediaUniqueID", mediaID)
            putString("MediaInfo_MediaType", type)
        }
    }

    //外部写入(3+1)
    //外部写入媒体项基本信息(文件名, 艺术家, 类型)
    fun save_MediaInfo_toRecord_main(filename: String, artist: String, type: String){
        INFO_MediaRecord.edit {
            putString("MediaInfo_FileName", filename)
            putString("MediaInfo_MediaArtist", artist)
            putString("MediaInfo_MediaType", type)
        }
    }
    //外部写入媒体项唯一识别ID
    fun save_MediaInfo_toRecordUniqueID_main(mediaID: String){
        INFO_MediaRecord.edit {
            putString("MediaInfo_MediaUniqueID", mediaID)
        }
    }


    //外部读取保存的媒体项 < 文件名, 艺术家>
    fun get_MediaInfo(): Pair<String, String>{
        val MediaInfo_FileName = INFO_MediaRecord.getString("MediaInfo_FileName", "") ?: ""
        val MediaInfo_MediaArtist = INFO_MediaRecord.getString("MediaInfo_MediaArtist", "") ?: ""

        return Pair(MediaInfo_FileName, MediaInfo_MediaArtist)
    }
    //外部读取保存的媒体项 < 唯一ID, 类型 >
    fun get_MediaInfo_UniqueID(): Pair<String, String>{
        val MediaInfo_MediaUniqueID = INFO_MediaRecord.getString("MediaInfo_MediaUniqueID", "") ?: ""
        val MediaInfo_MediaType = INFO_MediaRecord.getString("MediaInfo_MediaType", "") ?: ""

        return Pair(MediaInfo_MediaUniqueID, MediaInfo_MediaType)
    }



    //置空保存的媒体项
    fun clear_MediaInfo(){
        INFO_MediaRecord.edit {
            putString("MediaInfo_MediaUniqueID", "")
            putString("MediaInfo_FileName", "")
            putString("MediaInfo_MediaArtist", "")
        }
    }

    //删除保存的媒体项(目前没有场景用到)
    fun delete_MediaInfo(){
        INFO_MediaRecord.edit {
            remove("MediaInfo_MediaUniqueID")
            remove("MediaInfo_FileName")
            remove("MediaInfo_MediaArtist")
        }
    }

//object class END
}