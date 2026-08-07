package com.suming.player.DataPack.DataBaseMediaStore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoDao
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioDao
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoDataClass
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioDataClass

@Database(
    //MediaStoreDataBase承担视频和音频两个表
    entities = [
        VideoDataClass::class,
        AudioDataClass::class
       ],
    version = 1,
    exportSchema = false
)
abstract class MediaStoreDataBase : RoomDatabase() {

    //关联视频表
    abstract fun VideoTableDao(): VideoDao
    //关联音频表
    abstract fun AudioTableDao(): AudioDao


    companion object {

        @Volatile
        private var INSTANCE: MediaStoreDataBase? = null

        fun get(context: Context): MediaStoreDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediaStoreDataBase::class.java,
                    "MediaStoreDB.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }

}