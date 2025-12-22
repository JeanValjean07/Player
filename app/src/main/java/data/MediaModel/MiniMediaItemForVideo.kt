package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MiniMediaItemForVideo (
    val id: Long = 0,
    val uri: Uri,
    val name: String,
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MiniMediaItemForVideo> {

        val EMPTY = MiniMediaItemForVideo(0, Uri.EMPTY, "")


        override fun createFromParcel(parcel: Parcel): MiniMediaItemForVideo {
            return MiniMediaItemForVideo(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
            )
        }

        override fun newArray(size: Int): Array<MiniMediaItemForVideo?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(uri, flags)
        dest.writeString(name)
    }

}













