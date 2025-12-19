package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MediaItemForMusic (
    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long = 0,
    val format: String = "",
    var isHidden: Boolean = false, //是否隐藏
    val albumId: Long = 0, //专辑ID
    val artist: String = "", //艺术家
    val album: String = "", //专辑
    val title: String = "", //标题
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MediaItemForMusic> {

        val EMPTY = MediaItemForMusic(0, Uri.EMPTY, "", 0, 0)

        override fun createFromParcel(parcel: Parcel): MediaItemForMusic {
            return MediaItemForMusic(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                durationMs = parcel.readLong(),
                sizeBytes = parcel.readLong(),
                dateAdded = parcel.readLong()
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
        dest.writeLong(durationMs)
        dest.writeLong(sizeBytes)
        dest.writeLong(dateAdded)
        dest.writeString(format)
        dest.writeBoolean(isHidden) //是否隐藏
        dest.writeLong(albumId) //专辑ID
        dest.writeString(artist) //艺术家
        dest.writeString(album) //专辑
        dest.writeString(title) //标题
    }

}













