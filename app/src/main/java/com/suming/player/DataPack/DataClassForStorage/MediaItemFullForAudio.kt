package com.suming.player.DataPack.DataClassForStorage

import android.os.Parcel
import android.os.Parcelable

data class MediaItemFullForAudio (
    val media_api_SPECIFIC_ID: String = "",
    val media_api_NUM_ID: Long = 0,
    val media_api_dateAdded: Long = 0,
    val media_SPECIFIC_MediaType: String = "",
    val URI_S_FP: String,
    val file_path: String = "",
    val file_name: String = "",
    val file_size: Long,
    val media_title: String = "",
    val media_artist: String = "",
    val media_durationMs: Long,
    val media_format: String = "",
    //-----------------------------------
    val media_audio_bitrate: String = "", //音频比特率
    val media_audio_album: String = "", //专辑
    val media_audio_albumId: Long = 0, //专辑ID

): Parcelable{
    companion object CREATOR : Parcelable.Creator<MediaItemFullForAudio> {

        override fun createFromParcel(parcel: Parcel): MediaItemFullForAudio {
            return MediaItemFullForAudio(
                media_api_SPECIFIC_ID = parcel.readString()!!,
                media_api_NUM_ID = parcel.readLong(),
                media_api_dateAdded = parcel.readLong(),
                media_SPECIFIC_MediaType = parcel.readString()!!,
                URI_S_FP = parcel.readString()!!,
                file_path = parcel.readString()!!,
                file_name = parcel.readString()!!,
                file_size = parcel.readLong(),
                media_title = parcel.readString()!!,
                media_artist = parcel.readString()!!,
                media_durationMs = parcel.readLong(),
                media_format = parcel.readString()!!,
                //-----------------------------------
                media_audio_bitrate = parcel.readString()!!,
                media_audio_album = parcel.readString()!!,
                media_audio_albumId = parcel.readLong(),

            )
        }

        override fun newArray(size: Int): Array<MediaItemFullForAudio?> {
            return arrayOfNulls(size)
        }

    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(media_api_SPECIFIC_ID)
        dest.writeLong(media_api_NUM_ID)
        dest.writeLong(media_api_dateAdded)
        dest.writeString(media_SPECIFIC_MediaType)
        dest.writeString(URI_S_FP)
        dest.writeString(file_path)
        dest.writeString(file_name)
        dest.writeLong(file_size)
        dest.writeString(media_title)
        dest.writeString(media_artist)
        dest.writeLong(media_durationMs)
        dest.writeString(media_format)
        //-----------------------------------
        dest.writeString(media_audio_bitrate)
        dest.writeString(media_audio_album)
        dest.writeLong(media_audio_albumId)
    }

}













