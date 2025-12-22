package data.MediaModel

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MiniMediaItemForMusic (
    val id: Long = 0,
    val uri: Uri,
    val name: String,
    val title: String = "",
    val artist: String = ""
): Parcelable{

    companion object CREATOR : Parcelable.Creator<MiniMediaItemForMusic> {

        val EMPTY = MiniMediaItemForMusic(0, Uri.EMPTY, "", "", "")

        override fun createFromParcel(parcel: Parcel): MiniMediaItemForMusic {
            return MiniMediaItemForMusic(
                id = parcel.readLong(),
                uri = parcel.readParcelable(Uri::class.java.classLoader)!!,
                name = parcel.readString()!!,
                title = parcel.readString()!!,
                artist = parcel.readString()!!
            )
        }

        override fun newArray(size: Int): Array<MiniMediaItemForMusic?> {
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

    }

}













