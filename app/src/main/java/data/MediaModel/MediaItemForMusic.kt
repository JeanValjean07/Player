package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MediaItemForMusic (
    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long = 0,
    val format: String = "",
    val albumId: Long = 0,
    val album: String = ""

): Parcelable{

    companion object CREATOR : Parcelable.Creator<MediaItemForMusic> {

        val EMPTY = MediaItemForMusic(0, Uri.EMPTY, "", "", "", 0L, 0L)

        override fun createFromParcel(parcel: Parcel): MediaItemForMusic {
            return MediaItemForMusic(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                title = parcel.readString()!!,
                artist = parcel.readString()!!,
                durationMs = parcel.readLong(),
                sizeBytes = parcel.readLong(),
                dateAdded = parcel.readLong(),
                format = parcel.readString()!!,
                albumId = parcel.readLong(),
                album = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MediaItemForMusic?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(uri, flags)
        dest.writeString(name)
        dest.writeString(title)
        dest.writeString(artist)
        dest.writeLong(durationMs)
        dest.writeLong(sizeBytes)
        dest.writeLong(dateAdded)
        dest.writeString(format)
        dest.writeLong(albumId)
        dest.writeString(album)

    }

}













