package com.suming.player.DataPack.DataBaseMediaStore.Audio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tableAudioList")
data class AudioDataClass(
    @PrimaryKey
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
    val media_audio_bitrate: String = "", //音频比特率
    val media_audio_album: String = "", //专辑名称
    val media_audio_albumId: Long = 0, //专辑ID
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  AudioDataClass
        return media_api_SPECIFIC_ID == other.media_api_SPECIFIC_ID &&
                media_api_NUM_ID == other.media_api_NUM_ID &&
                media_api_dateAdded == other.media_api_dateAdded &&
                media_SPECIFIC_MediaType == other.media_SPECIFIC_MediaType &&
                content_uriString == other.content_uriString &&
                file_path == other.file_path &&
                file_name == other.file_name &&
                file_size == other.file_size &&
                media_title == other.media_title &&
                media_artist == other.media_artist &&
                media_durationMs == other.media_durationMs &&
                media_format == other.media_format &&
                media_audio_bitrate == other.media_audio_bitrate &&
                media_audio_album == other.media_audio_album &&
                media_audio_albumId == other.media_audio_albumId

    }


    override fun hashCode(): Int {
        return media_api_SPECIFIC_ID.hashCode()
    }
}