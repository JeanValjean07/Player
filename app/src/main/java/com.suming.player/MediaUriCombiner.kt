package com.suming.player

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri

object MediaUriCombiner {



    //根据媒体储存唯一ID合成uri
    fun getMediaUriByMediaID(mediaStoreID: Long): Uri {

        val uri = "content://media/external/audio/media/$mediaStoreID".toUri()


        return uri
    }
    //根据媒体储存唯一ID合成uri字符串
    fun getMediaUriStringByMediaID(mediaStoreID: Long): String {

        val uri = "content://media/external/audio/media/$mediaStoreID".toUri()

        return uri.toString()

    }


    //获取媒体ID
    fun getMediaIDByMediaUri(mediaUri: Uri): String {
        Log.d("SuMing", "getMediaIDByMediaUri: $mediaUri")
        val mediaID = mediaUri.lastPathSegment

        Log.d("SuMing", "getMediaIDByMediaUri: $mediaID")

        return mediaID ?: ""

    }




}