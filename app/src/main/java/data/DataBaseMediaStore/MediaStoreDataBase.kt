package data.DataBaseMediaStore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediaStoreSetting::class], version = 10, exportSchema = false)
abstract class MediaStoreDataBase : RoomDatabase() {
    abstract fun mediaStoreDao(): MediaStoreDao

    companion object {
        @Volatile
        private var INSTANCE: MediaStoreDataBase? = null

        fun get(context: Context): MediaStoreDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediaStoreDataBase::class.java,
                    "MediaStore.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}