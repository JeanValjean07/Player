package data.DataBaseMediaStore

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaStore")
data class MediaStoreSetting(
    @PrimaryKey
    val MARK_ID: String,
    val info_uri_string: String = "",
    val info_uri_numOnly: Long = 0L,
    val info_filename: String = "",
    val info_title: String = "",
    val info_artist: String = "",
    val info_duration : Long = 0L,
    val info_resolution: String = "",
    val info_path: String = "",
    val info_file_size: Long = 0L,
    val info_date_added : Long = 0L,
    val info_format: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MediaStoreSetting
        return MARK_ID == other.MARK_ID &&
                info_uri_string == other.info_uri_string &&
                info_uri_numOnly == other.info_uri_numOnly &&
                info_filename == other.info_filename &&
                info_title == other.info_title &&
                info_artist == other.info_artist &&
                info_duration == other.info_duration &&
                info_resolution == other.info_resolution &&
                info_path == other.info_path &&
                info_file_size == other.info_file_size &&
                info_date_added == other.info_date_added &&
                info_format == other.info_format

    }

    //修改数据库结构时记得同步修改预置数据类

    override fun hashCode(): Int {
        return MARK_ID.hashCode()
    }
}