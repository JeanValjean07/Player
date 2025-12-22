package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.appcompat.widget.DialogTitle

data class MediaItemForVideo (
    val id: Long = 0,
    val uriString: String,
    val uriNumOnly: Long = 0,
    val filename: String = "",
    val title: String = "",
    val artist: String = "",
    val durationMs: Long,
    val res: String = "",
    val path: String = "",
    val sizeBytes: Long,
    val dateAdded: Long = 0,
    val format: String = "",
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MediaItemForVideo> {

        //空对象模板
        val EMPTY = MediaItemForVideo(0, "", 0L, "", "", "", 0L, "0", "", 0L)

        @Suppress("DEPRECATION")
        override fun createFromParcel(parcel: Parcel): MediaItemForVideo {
            return MediaItemForVideo(
                //基础
                id = parcel.readLong(),
                uriString = parcel.readString()!!,
                uriNumOnly = parcel.readLong(),
                filename = parcel.readString()!!,
                title = parcel.readString()!!,
                artist = parcel.readString()!!,
                durationMs = parcel.readLong(),
                //基础：视频专属
                res = parcel.readString()!!,
                //其他
                path = parcel.readString()!!,
                sizeBytes = parcel.readLong(),
                dateAdded = parcel.readLong(),
                format = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MediaItemForVideo?> {
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
        dest.writeString(res)
        dest.writeString(path)
        dest.writeLong(sizeBytes)
        dest.writeLong(dateAdded)
        dest.writeString(format)
    }

}













