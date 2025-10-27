package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.io.File

data class MediaItem_video (

    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val markCount: Int = 0,
    var Media_Cover_Path: String? = null

): Parcelable{
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(uri, flags)
        dest.writeString(name)
        dest.writeLong(durationMs)
        dest.writeLong(sizeBytes)
        dest.writeInt(markCount)
        dest.writeString(Media_Cover_Path)
    }

    companion object CREATOR : Parcelable.Creator<MediaItem_video> {

        val EMPTY = MediaItem_video(0, Uri.EMPTY, "", 0, 0)

        override fun createFromParcel(parcel: Parcel): MediaItem_video {
            return MediaItem_video(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                durationMs = parcel.readLong(),
                sizeBytes = parcel.readLong(),
                markCount = parcel.readInt(),
                Media_Cover_Path = parcel.readString()
            )
        }

        override fun newArray(size: Int): Array<MediaItem_video?> {
            return arrayOfNulls(size)
        }
    }
}













