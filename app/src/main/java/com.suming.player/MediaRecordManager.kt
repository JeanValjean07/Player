package com.suming.player

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit

@Suppress("unused")
class MediaRecordManager(context: Context) {

    //保存以下内容
    //1.媒体项的URI字符串
    //2.媒体文件名
    //3.参与创作的艺术家


    //
    private var INFO_MediaRecord: SharedPreferences = context.getSharedPreferences("INFO_MediaRecord", MODE_PRIVATE)



    //外部写入需要保存的媒体项
    fun save_MediaInfo_toRecord(uriString: String, filename: String, artist: String){
        INFO_MediaRecord.edit {
            putString("MediaInfo_MediaUriString", uriString)
            putString("MediaInfo_FileName", filename)
            putString("MediaInfo_MediaArtist", artist)
        }
    }

    //外部读取保存的媒体项
    fun get_MediaInfo(): Triple<String, String, String>{
        val MediaInfo_MediaUriString = INFO_MediaRecord.getString("MediaInfo_MediaUriString", "") ?: ""
        val MediaInfo_FileName = INFO_MediaRecord.getString("MediaInfo_FileName", "") ?: ""
        val MediaInfo_MediaArtist = INFO_MediaRecord.getString("MediaInfo_MediaArtist", "") ?: ""

        return Triple(MediaInfo_MediaUriString, MediaInfo_FileName, MediaInfo_MediaArtist)
    }

    //清除保存的媒体项
    fun clear_MediaInfo(){
        INFO_MediaRecord.edit {
            putString("MediaInfo_MediaUriString", "")
            putString("MediaInfo_FileName", "")
            putString("MediaInfo_MediaArtist", "")
        }
    }




    //删除保存的媒体项(目前没有场景用到)
    fun delete_MediaInfo(){
        INFO_MediaRecord.edit {
            remove("MediaInfo_MediaUriString")
            remove("MediaInfo_FileName")
            remove("MediaInfo_MediaArtist")
        }
    }

//object class END
}