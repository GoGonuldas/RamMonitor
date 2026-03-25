package com.rammonitor.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RamHistoryDao {
    @Insert
    suspend fun insert(entry: RamHistoryEntry)

    @Query("SELECT * FROM ram_history ORDER BY timestamp DESC LIMIT 100")
    fun getLast100(): Flow<List<RamHistoryEntry>>

    @Query("SELECT * FROM ram_history WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getSince(since: Long): List<RamHistoryEntry>

    @Query("DELETE FROM ram_history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT AVG(usagePercent) FROM ram_history WHERE timestamp > :since")
    suspend fun getAverageUsage(since: Long): Float?

    @Query("SELECT MAX(usagePercent) FROM ram_history WHERE timestamp > :since")
    suspend fun getPeakUsage(since: Long): Float?
}

@Database(entities = [RamHistoryEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ramHistoryDao(): RamHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ram_monitor.db"
                ).build().also { INSTANCE = it }
            }
    }
}
