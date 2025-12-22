package data.DataBaseMusicStore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MusicStoreSetting::class], version = 4, exportSchema = false)
abstract class MusicStoreDataBase : RoomDatabase() {
    abstract fun musicStoreDao(): MusicStoreDao

    companion object {
        @Volatile
        private var INSTANCE: MusicStoreDataBase? = null

        fun get(context: Context): MusicStoreDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicStoreDataBase::class.java,
                    "MusicStore.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}