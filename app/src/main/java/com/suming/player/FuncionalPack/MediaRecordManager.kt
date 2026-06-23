package com.suming.player.FuncionalPack

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

@Suppress("unused")
class MediaRecordManager(context: Context) {

    //记录单
    private var INFO_MediaRecord: SharedPreferences = context.getSharedPreferences("INFO_MediaRecord",
        Context.MODE_PRIVATE
    )

    //默认空字段
    val string_null = ""
    //表单标识
    val info_item_uri_standard = "info_item_uri_standard"
    val info_item_file_name = "info_item_file_name"
    val info_item_artist = "info_item_artist"


    //写入一条播放记录
    fun writeOneRecord(uriStandard: String, filename: String = string_null, artist: String = string_null){
        INFO_MediaRecord.edit {
            putString(info_item_uri_standard, uriStandard)
            putString(info_item_file_name, filename)
            putString(info_item_artist, artist)
        }
    }

    //获取一条记录
    fun takeOneRecord(): Triple<String,String, String>{
        val MediaInfo_UriStandard =  INFO_MediaRecord.getString(info_item_uri_standard ,"") ?: ""
        val MediaInfo_FileName = INFO_MediaRecord.getString(info_item_file_name, "") ?: ""
        val MediaInfo_MediaArtist = INFO_MediaRecord.getString(info_item_artist, "") ?: ""


        return Triple(MediaInfo_UriStandard,MediaInfo_FileName, MediaInfo_MediaArtist)
    }
    //获取一条记录(仅获取uri)
    fun takeOneRecordUri(): String {
        val MediaInfo_UriStandard =  INFO_MediaRecord.getString(info_item_uri_standard ,"") ?: ""


        return MediaInfo_UriStandard
    }





    //置空保存的媒体项
    fun clear_MediaInfo(){
        INFO_MediaRecord.edit {
            putString(info_item_uri_standard, "")
            putString(info_item_file_name, "")
            putString(info_item_artist, "")
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