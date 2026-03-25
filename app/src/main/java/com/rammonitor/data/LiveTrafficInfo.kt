package com.rammonitor.data

data class LiveTrafficInfo(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long
) {
    val totalBytesPerSec: Long get() = rxBytesPerSec + txBytesPerSec

    val rxKbps: Float get() = rxBytesPerSec / 1024f
    val txKbps: Float get() = txBytesPerSec / 1024f
    val totalKbps: Float get() = totalBytesPerSec / 1024f

    fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / 1024f / 1024f)
        bytesPerSec >= 1024        -> "%.1f KB/s".format(bytesPerSec / 1024f)
        else                       -> "${bytesPerSec} B/s"
    }

    val rxSpeedLabel: String get() = formatSpeed(rxBytesPerSec)
    val txSpeedLabel: String get() = formatSpeed(txBytesPerSec)
    val totalSpeedLabel: String get() = formatSpeed(totalBytesPerSec)
}

