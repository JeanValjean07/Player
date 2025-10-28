package data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediaItemSetting::class], version = 7, exportSchema = false)
abstract class MediaItemDataBase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        @Volatile
        private var INSTANCE: MediaItemDataBase? = null

        fun get(context: Context): MediaItemDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediaItemDataBase::class.java,
                    "player.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}