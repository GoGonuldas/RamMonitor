package com.rammonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class RamInfo(
    val totalRam: Long,       // bytes
    val availableRam: Long,   // bytes
    val usedRam: Long,        // bytes
    val usagePercent: Float,  // 0-100
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalMb: Float get() = totalRam / 1024f / 1024f
    val availableMb: Float get() = availableRam / 1024f / 1024f
    val usedMb: Float get() = usedRam / 1024f / 1024f
    val totalGb: Float get() = totalMb / 1024f
    val usedGb: Float get() = usedMb / 1024f
}

data class AppMemInfo(
    val packageName: String,
    val appName: String,
    val pssKb: Int,           // Proportional Set Size in KB
    val ussKb: Int,           // Unique Set Size in KB
    val rssKb: Int            // Resident Set Size in KB
) {
    val pssMb: Float get() = pssKb / 1024f
}

@Entity(tableName = "ram_history")
data class RamHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalRam: Long,
    val usedRam: Long,
    val usagePercent: Float,
    val timestamp: Long = System.currentTimeMillis()
)
