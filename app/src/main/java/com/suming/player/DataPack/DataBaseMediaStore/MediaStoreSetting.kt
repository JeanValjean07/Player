package com.suming.player.DataPack.DataBaseMediaStore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaStore")
data class MediaStoreSetting(
    @PrimaryKey
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MediaStoreSetting
        return file_path == other.file_path &&
                file_name == other.file_name &&
                file_size == other.file_size &&
                media_api_id == other.media_api_id &&
                media_api_dateAdded == other.media_api_dateAdded &&
                content_uriString == other.content_uriString &&
                custom_media_Type == other.custom_media_Type &&
                media_title == other.media_title &&
                media_artist == other.media_artist &&
                media_durationMs == other.media_durationMs &&
                media_video_resolution == other.media_video_resolution &&
                media_format == other.media_format
    }

    //修改数据库结构时记得同步修改预置数据类

    override fun hashCode(): Int {
        return file_path.hashCode()
    }
}