package com.suming.player.DataPack.DataClassForPlay

import android.net.Uri

data class MediaItemForPlay (

    var MediaInfo_MediaUniqueID: String,
    var MediaInfo_DataBaseID : String,
    var MediaInfo_MediaUri: Uri,
    var MediaInfo_MediaUriString : String,
    var MediaInfo_MediaUriStandard : String,
    var MediaInfo_MediaUriNumOnly : Long,

    var MediaInfo_MediaType : String,
    var MediaInfo_AbsolutePath : String,
    var MediaInfo_FileName : String,
    var MediaInfo_MediaTitle : String,
    var MediaInfo_MediaArtist : String,

    var MediaInfo_Duration : Long,

    var MediaInfo_Video_Width : Int,
    var MediaInfo_Video_Height : Int,

    var MediaInfo_RealFps: Float = 0f


)