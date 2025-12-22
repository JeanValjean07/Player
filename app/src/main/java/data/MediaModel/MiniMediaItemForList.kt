package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MiniMediaItemForList (
    val id: Long = 0,
    val uri: Uri,
    val uriNumOnly: Long = 0,
    val filename: String,
    val title: String = "",
    val artist: String = "",
    val type: String = ""
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MiniMediaItemForList> {

        //空对象模板
        val EMPTY = MiniMediaItemForList(0, Uri.EMPTY, 0, "", "", "")

        @Suppress("DEPRECATION")
        override fun createFromParcel(parcel: Parcel): MiniMediaItemForList {
            return MiniMediaItemForList(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                uriNumOnly = parcel.readLong(),
                filename = parcel.readString()!!,
                title = parcel.readString()!!,
                artist = parcel.readString()!!,
                type = parcel.readString()!!
            )
        }

        override fun newArray(size: Int): Array<MiniMediaItemForList?> {
            return arrayOfNulls(size)
        }

    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeParcelable(uri, flags)
        dest.writeLong(uriNumOnly)
        dest.writeString(filename)
        dest.writeString(title)
        dest.writeString(artist)
        dest.writeString(type)
    }

}













