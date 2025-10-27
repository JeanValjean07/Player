package data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MediaItemSetting)

    @Query("SELECT * FROM video_settings WHERE MARK_FileName = :path LIMIT 1")
    suspend operator fun get(path: String): MediaItemSetting?

    @Query("""UPDATE video_settings SET SaveFlag_Thumb = CASE WHEN :on THEN SaveFlag_Thumb | (1 << :bitIdx) ELSE SaveFlag_Thumb & ~(1 << :bitIdx) END WHERE MARK_FileName = :path """)
    suspend fun updateFlagBit(path: String, bitIdx: Int, on: Boolean)

    @Query("""UPDATE video_settings SET SaveFlag_Thumb = (SaveFlag_Thumb & ~:mask) | (:value & :mask) WHERE MARK_FileName = :path""")
    suspend fun updateFlagMasked(path: String, mask: Int, value: Int)

    /* 3. 读整个 flagInt 做调试或展示 ------------------------ */
    @Query("SELECT SaveFlag_Thumb FROM video_settings WHERE MARK_FileName = :path")
    suspend fun getFlagInt(path: String): Int?


    @Delete
    suspend fun delete(item: MediaItemSetting)
}