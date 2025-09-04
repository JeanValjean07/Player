package data.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class VideoItem (

    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val markCount: Int = 0,
    val thumbnailUri: Uri? = null

): Parcelable{
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(uri, flags)
        dest.writeString(name)
        dest.writeLong(durationMs)
        dest.writeInt(markCount)
        dest.writeParcelable(thumbnailUri, flags)
    }

    companion object CREATOR : Parcelable.Creator<VideoItem> {
        override fun createFromParcel(parcel: Parcel): VideoItem {
            return VideoItem(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                durationMs = parcel.readLong(),
                markCount = parcel.readInt(),
                thumbnailUri = parcel.readParcelable(Uri::class.java.classLoader)
            )
        }

        override fun newArray(size: Int): Array<VideoItem?> {
            return arrayOfNulls(size)
        }
    }
}













