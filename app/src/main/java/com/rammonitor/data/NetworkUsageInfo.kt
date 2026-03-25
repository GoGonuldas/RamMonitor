package com.rammonitor.data

data class NetworkUsageInfo(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val rxBytes: Long,
    val txBytes: Long
) {
    val totalBytes: Long get() = rxBytes + txBytes
    val rxMb: Float get() = rxBytes / 1024f / 1024f
    val txMb: Float get() = txBytes / 1024f / 1024f
    val totalMb: Float get() = totalBytes / 1024f / 1024f
}

