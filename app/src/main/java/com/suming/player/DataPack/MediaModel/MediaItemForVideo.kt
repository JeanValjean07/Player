package com.suming.player.DataPack.MediaModel

import android.os.Parcel
import android.os.Parcelable

data class MediaItemForVideo (
    val file_path: String = "",  //改用file_path作为主键
    val file_name: String = "",
    val file_size: Long,
    val media_api_id: Long = 0,   //ID不适用与主键,因为ID数字无法体现是视频还是音频
    val media_api_dateAdded: Long = 0,
    val content_uriString: String,
    val custom_media_Type: String = "",
    val media_title: String = "",
    val media_artist: String = "",
    val media_durationMs: Long,
    val media_video_resolution: String = "",
    val media_format: String = "",
): Parcelable{
    companion object CREATOR : Parcelable.Creator<MediaItemForVideo> {

        @Suppress("DEPRECATION")
        override fun createFromParcel(parcel: Parcel): MediaItemForVideo {
            return MediaItemForVideo(
                file_path = parcel.readString()!!,
                file_name = parcel.readString()!!,
                file_size = parcel.readLong(),
                media_api_id = parcel.readLong(),
                media_api_dateAdded = parcel.readLong(),
                content_uriString = parcel.readString()!!,
                custom_media_Type = parcel.readString()!!,
                media_title = parcel.readString()!!,
                media_artist = parcel.readString()!!,
                media_durationMs = parcel.readLong(),
                media_video_resolution = parcel.readString()!!,
                media_format = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MediaItemForVideo?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(file_path)
        dest.writeString(file_name)
        dest.writeLong(file_size)
        dest.writeLong(media_api_id)
        dest.writeLong(media_api_dateAdded)
        dest.writeString(content_uriString)
        dest.writeString(custom_media_Type)
        dest.writeString(media_title)
        dest.writeString(media_artist)
        dest.writeLong(media_durationMs)
        dest.writeString(media_video_resolution)
        dest.writeString(media_format)
    }

    override fun describeContents(): Int = 0

}













