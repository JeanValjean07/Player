package com.suming.player.FuncionalPack

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.suming.player.DataPack.MediaRecordPack

@Suppress("unused")
class MediaRecordManager {

    //SharedPreferences
    private val SPF_NAME_MediaRecord = "SPF_MediaRecord"
    private var SPF_MediaRecord: SharedPreferences ?= null
    private fun INIT_SPF(context: Context){
        if (SPF_MediaRecord == null) {
            SPF_MediaRecord = context.getSharedPreferences(SPF_NAME_MediaRecord, Context.MODE_PRIVATE)
        }
    }

    //记录所需字段
    val string_null = ""
    //表单标识
    val item_SPECIFIC_ID = "item_SPECIFIC_ID"
    val item_uri_standard = "item_uri_standard"
    val item_file_name = "item_file_name"
    val item_artist = "item_artist"


    //写入记录
    fun writeRecord(context: Context, mediaRecordPack: MediaRecordPack){
        INIT_SPF(context)

        SPF_MediaRecord?.edit {
            putString(item_SPECIFIC_ID, mediaRecordPack.SPECIFIC_ID)
            putString(item_uri_standard, mediaRecordPack.uriStandard)
            putString(item_file_name, mediaRecordPack.fileName)
            putString(item_artist, mediaRecordPack.mediaArtist)
        }
    }
    //读取记录
    fun readRecord(context: Context): MediaRecordPack{
        INIT_SPF(context)

        val MediaInfo_SPECIFIC_ID =  SPF_MediaRecord?.getString(item_SPECIFIC_ID ,string_null) ?: string_null
        val MediaInfo_UriStandard =  SPF_MediaRecord?.getString(item_uri_standard ,string_null) ?: string_null
        val MediaInfo_FileName = SPF_MediaRecord?.getString(item_file_name, string_null) ?: string_null
        val MediaInfo_MediaArtist = SPF_MediaRecord?.getString(item_artist, string_null) ?: string_null


        return MediaRecordPack(MediaInfo_SPECIFIC_ID,MediaInfo_UriStandard,MediaInfo_FileName, MediaInfo_MediaArtist)
    }


    //读取记录(仅获取uri)
    fun takeOneRecordUri(context: Context): String {
        INIT_SPF(context)

        val MediaInfo_UriStandard =  SPF_MediaRecord?.getString(item_uri_standard ,string_null) ?: ""


        return MediaInfo_UriStandard
    }



    //置空保存的媒体项
    fun clear_MediaInfo(context: Context){
        INIT_SPF(context)

        SPF_MediaRecord?.edit {
            putString(item_uri_standard, "")
            putString(item_file_name, "")
            putString(item_artist, "")
        }
    }
    //删除保存的媒体项
    fun delete_MediaInfo(context: Context){
        INIT_SPF(context)

        SPF_MediaRecord?.edit {
            remove(item_uri_standard)
            remove(item_file_name)
            remove(item_artist)
        }
    }


}