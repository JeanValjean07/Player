package data.DataBaseMusicStore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MusicStore")
data class MusicStoreSetting(
    @PrimaryKey
    val MARK_Uri_numOnly: String,
    val info_uri_full: String = "",
    val info_filename: String = "",
    val info_title: String = "",
    val info_artist: String = "",
    val info_duration : Long = 0L,
    val info_date_added : Long = 0L,
    val info_file_size: Long = 0L,
    val info_format: String = "",
    val info_album_id: Long = 0L,
    val info_album: String = "",

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MusicStoreSetting
        return MARK_Uri_numOnly == other.MARK_Uri_numOnly &&
                info_uri_full == other.info_uri_full &&
                info_filename == other.info_filename &&
                info_title == other.info_title &&
                info_artist == other.info_artist &&
                info_duration == other.info_duration &&
                info_date_added == other.info_date_added &&
                info_file_size == other.info_file_size &&
                info_format == other.info_format &&
                info_album_id == other.info_album_id &&
                info_album == other.info_album
    }

    //修改数据库结构时记得同步修改预置数据类

    override fun hashCode(): Int {
        return MARK_Uri_numOnly.hashCode()
    }
}