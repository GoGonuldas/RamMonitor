package com.rammonitor.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val networkStatsManager =
        appContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    @Suppress("DEPRECATION")
    suspend fun getAppNetworkUsageLastMinutes(minutes: Int = 30): List<NetworkUsageInfo> =
        withContext(Dispatchers.IO) {
            val end = System.currentTimeMillis()
            val start = end - minutes * 60_000L
            val usageByUid = linkedMapOf<Int, Pair<Long, Long>>()

            collectUsageForType(ConnectivityManager.TYPE_WIFI, start, end, usageByUid)
            collectUsageForType(ConnectivityManager.TYPE_MOBILE, start, end, usageByUid)

            usageByUid.mapNotNull { (uid, bytes) ->
                if (uid <= Process.FIRST_APPLICATION_UID) return@mapNotNull null

                val pkg = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: return@mapNotNull null
                try {
                    val info = packageManager.getApplicationInfo(pkg, 0)
                    if (!isUserVisibleApp(info)) return@mapNotNull null
                    val label = packageManager.getApplicationLabel(info).toString()
                    NetworkUsageInfo(
                        uid = uid,
                        packageName = pkg,
                        appName = label,
                        rxBytes = bytes.first,
                        txBytes = bytes.second
                    )
                } catch (_: Exception) {
                    null
                }
            }
                .filter { it.totalBytes > 0L }
                .sortedByDescending { it.totalBytes }
        }

    private fun collectUsageForType(
        type: Int,
        start: Long,
        end: Long,
        usageByUid: MutableMap<Int, Pair<Long, Long>>
    ) {
        val stats = try {
            networkStatsManager.querySummary(type, null, start, end)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        } ?: return

        stats.use { ns ->
            val bucket = NetworkStats.Bucket()
            while (ns.hasNextBucket()) {
                ns.getNextBucket(bucket)
                val uid = bucket.uid
                val prev = usageByUid[uid] ?: Pair(0L, 0L)
                usageByUid[uid] = Pair(
                    prev.first + bucket.rxBytes,
                    prev.second + bucket.txBytes
                )
            }
        }
    }

    private fun isUserVisibleApp(info: ApplicationInfo): Boolean {
        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystem || isUpdatedSystem
    }
}


