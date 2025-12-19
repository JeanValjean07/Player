package data.DataBaseMusicStore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MusicStore")
data class MusicStoreSetting(
    @PrimaryKey
    val MARK_Uri_numOnly: String,
    val info_uri_full: String = "",
    val info_filename: String = "",
    val info_artwork_path: String = "",
    val info_duration : Long = 0L,
    val info_date_added : Long = 0L,
    val info_is_hidden: Boolean = false,
    val info_file_size: Long = 0L,
    val info_format: String = "", //格式
    val info_album_id: Long = 0L, //专辑ID
    val info_artist: String = "", //艺术家
    val info_album: String = "", //专辑
    val info_title: String = "", //标题
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as  MusicStoreSetting
        return MARK_Uri_numOnly == other.MARK_Uri_numOnly &&
                info_uri_full == other.info_uri_full &&
                info_filename == other.info_filename &&
                info_artwork_path == other.info_artwork_path &&
                info_duration == other.info_duration &&
                info_date_added == other.info_date_added &&
                info_is_hidden == other.info_is_hidden &&
                info_file_size == other.info_file_size &&
                info_format == other.info_format &&
                info_album_id == other.info_album_id &&
                info_artist == other.info_artist &&
                info_album == other.info_album &&
                info_title == other.info_title

    }

    //修改数据库结构时记得同步修改预置数据类

    override fun hashCode(): Int {
        return MARK_Uri_numOnly.hashCode()
    }
}