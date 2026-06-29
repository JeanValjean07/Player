package com.suming.player.DataPack

import android.net.Uri

data class MediaInfo(

    var MediaInfo_MediaUniqueID: String,      //媒体唯一ID,使用uriNumOnly担任,区别在于ID是字符串,uriNumOnly是Long
    var MediaInfo_DataBaseID : String,          //数据库专用ID
    var MediaInfo_MediaUri: Uri,
    var MediaInfo_MediaUriString : String,         //原始获取媒体链接字符串
    var MediaInfo_MediaUriStandard : String,       //标准链接格式(content://media/external/(?video|audio)/media/114514)
    var MediaInfo_MediaUriNumOnly : Long,         //跟UniqueID一样,暂时保留
    //
    var MediaInfo_MediaType : String,
    var MediaInfo_AbsolutePath : String,
    var MediaInfo_FileName : String,
    var MediaInfo_MediaTitle : String,
    var MediaInfo_MediaArtist : String,
    //
    var MediaInfo_Duration : Long,
    //
    var MediaInfo_Video_Width : Int,          //视频宽度
    var MediaInfo_Video_Height : Int,         //视频高度
    //
    var MediaInfo_RealFps: Float = 0f  //实际帧率


)
