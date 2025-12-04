package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MediaItemForVideo (
    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long = 0,
    val format: String = "",
    var isHidden: Boolean = false
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MediaItemForVideo> {

        val EMPTY = MediaItemForVideo(0, Uri.EMPTY, "", 0, 0)

        override fun createFromParcel(parcel: Parcel): MediaItemForVideo {
            return MediaItemForVideo(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                durationMs = parcel.readLong(),
                sizeBytes = parcel.readLong(),
                dateAdded = parcel.readLong()
            )
        }

        override fun newArray(size: Int): Array<MediaItemForVideo?> {
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
        dest.writeBoolean(isHidden)
    }

}













