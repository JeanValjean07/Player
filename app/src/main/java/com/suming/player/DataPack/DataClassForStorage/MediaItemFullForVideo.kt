package com.suming.player.DataPack.DataClassForStorage

import android.os.Parcel
import android.os.Parcelable

data class MediaItemFullForVideo (
    val media_api_SPECIFIC_ID: String = "",
    val media_api_NUM_ID: Long = 0,
    val media_api_dateAdded: Long = 0,
    val media_SPECIFIC_MediaType: String = "",
    val content_uriString: String,
    val file_path: String = "",
    val file_name: String = "",
    val file_size: Long,
    val media_title: String = "",
    val media_artist: String = "",
    val media_durationMs: Long,
    val media_format: String = "",
    //-----------------------------------
    val media_video_resolution: String = "",
    val media_video_bitrate: String = "", //视频码率
): Parcelable{
    companion object CREATOR : Parcelable.Creator<MediaItemFullForVideo> {

        override fun createFromParcel(parcel: Parcel): MediaItemFullForVideo {
            return MediaItemFullForVideo(
                media_api_SPECIFIC_ID = parcel.readString()!!,
                media_api_NUM_ID = parcel.readLong(),
                media_api_dateAdded = parcel.readLong(),
                media_SPECIFIC_MediaType = parcel.readString()!!,
                content_uriString = parcel.readString()!!,
                file_path = parcel.readString()!!,
                file_name = parcel.readString()!!,
                file_size = parcel.readLong(),
                media_title = parcel.readString()!!,
                media_artist = parcel.readString()!!,
                media_durationMs = parcel.readLong(),
                media_format = parcel.readString()!!,
                //-----------------------------------
                media_video_resolution = parcel.readString()!!,
                media_video_bitrate = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MediaItemFullForVideo?> {
            return arrayOfNulls(size)
        }

    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(media_api_SPECIFIC_ID)
        dest.writeLong(media_api_NUM_ID)
        dest.writeLong(media_api_dateAdded)
        dest.writeString(media_SPECIFIC_MediaType)
        dest.writeString(content_uriString)
        dest.writeString(file_path)
        dest.writeString(file_name)
        dest.writeLong(file_size)
        dest.writeString(media_title)
        dest.writeString(media_artist)
        dest.writeLong(media_durationMs)
        dest.writeString(media_format)
        //-----------------------------------
        dest.writeString(media_video_resolution)
        dest.writeString(media_video_bitrate)
    }

    override fun describeContents(): Int = 0

}













