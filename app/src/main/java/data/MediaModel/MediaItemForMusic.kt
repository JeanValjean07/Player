package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MediaItemForMusic (
    val id: Long = 0,
    val uriString: String,
    val uriNumOnly: Long = 0,
    val filename: String,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long,
    val albumId: Long = 0,
    val album: String = "",
    val path: String = "",
    val sizeBytes: Long,
    val dateAdded: Long = 0,
    val format: String = "",
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MediaItemForMusic> {

        //空对象模板
        val EMPTY = MediaItemForMusic(0, "", 0L, "", "", "", 0L, 0L, "", "", 0L, 0L, "")

        @Suppress("DEPRECATION")
        override fun createFromParcel(parcel: Parcel): MediaItemForMusic {
            return MediaItemForMusic(
                //基础
                id = parcel.readLong(),
                uriString = parcel.readString()!!,
                uriNumOnly = parcel.readLong(),
                filename = parcel.readString()!!,
                title = parcel.readString()!!,
                artist = parcel.readString()!!,
                durationMs = parcel.readLong(),
                //基础：音频专属
                albumId = parcel.readLong(),
                album = parcel.readString()!!,
                //其他
                path = parcel.readString()!!,
                sizeBytes = parcel.readLong(),
                dateAdded = parcel.readLong(),
                format = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MediaItemForMusic?> {
            return arrayOfNulls(size)
        }

    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(uriString)
        dest.writeLong(uriNumOnly)
        dest.writeString(filename)
        dest.writeString(title)
        dest.writeString(artist)
        dest.writeLong(durationMs)
        dest.writeLong(albumId)
        dest.writeString(album)
        dest.writeString(path)
        dest.writeLong(sizeBytes)
        dest.writeLong(dateAdded)
        dest.writeString(format)

    }

}













