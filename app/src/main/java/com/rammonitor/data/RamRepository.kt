package com.rammonitor.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Debug
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RamRepository(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager = context.packageManager
    private val db = AppDatabase.getInstance(context)
    private val dao = db.ramHistoryDao()

    /** Returns true if the user has granted Usage Access (PACKAGE_USAGE_STATS). */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Read current system RAM snapshot */
    fun getRamInfo(): RamInfo {
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        val used = mi.totalMem - mi.availMem
        return RamInfo(
            totalRam = mi.totalMem,
            availableRam = mi.availMem,
            usedRam = used,
            usagePercent = used.toFloat() / mi.totalMem.toFloat() * 100f
        )
    }

    /**
     * Returns per-app memory for USER-FACING apps only (no Android core / MIUI system apps).
     *
     * Sources merged in priority order:
     *  1. ActivityManager.getRunningAppProcesses()  — works on MIUI even on Android 8+
     *  2. /proc filesystem scan                     — memory data + extra PIDs
     *  3. UsageStatsManager                         — recently-used list (needs permission)
     *
     * Filter: only apps that have a launcher intent (= visible to user in app drawer).
     */
    suspend fun getAppMemInfoList(): List<AppMemInfo> = withContext(Dispatchers.IO) {
        val launcherPackages = getLauncherPackages()
        val runningProcessPackages = getRunningProcessPackages()
        val procMemMap = buildProcMemMap()
        val amMemMap = buildActivityManagerMemMap()

        var recentPackages = emptySet<String>()
        if (hasUsageStatsPermission()) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                recentPackages = collectRecentPackagesFromEvents(usm, now - 12 * 60 * 60 * 1000L, now)
            } catch (_: Exception) {}
        }

        val candidatePackages = linkedSetOf<String>().apply {
            addAll(launcherPackages)
            addAll(recentPackages)
            addAll(runningProcessPackages)
        }

        val result = mutableListOf<AppMemInfo>()

        for (pkg in candidatePackages) {
            if (pkg == context.packageName) continue
            if (isExcludedCorePackage(pkg)) continue
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                if (!isAllowedUserApp(appInfo)) continue

                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val procMem = procMemMap[pkg]
                val amMem = amMemMap[pkg]
                val pssKb = listOfNotNull(procMem?.first, amMem?.first).maxOrNull() ?: 0
                val rssKb = listOfNotNull(procMem?.second, amMem?.second).maxOrNull() ?: 0

                result.add(AppMemInfo(
                    packageName = pkg,
                    appName = appName,
                    pssKb = pssKb,
                    ussKb = 0,
                    rssKb = rssKb
                ))
            } catch (_: Exception) {}
        }

        result
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<AppMemInfo> { it.pssKb.coerceAtLeast(it.rssKb) > 0 }
                    .thenByDescending { it.pssKb.coerceAtLeast(it.rssKb) }
                    .thenBy { it.appName.lowercase() }
            )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildProcMemMap(): Map<String, Pair<Int, Int>> {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        try {
            File("/proc").listFiles { f ->
                f.isDirectory && f.name.all { it.isDigit() }
            }?.forEach { pidDir ->
                try {
                    // Process name from cmdline
                    var processName: String? = null
                    try {
                        val raw = File(pidDir, "cmdline").readBytes()
                        val name = raw.takeWhile { it != 0.toByte() }.toByteArray()
                            .toString(Charsets.UTF_8).trim()
                        if (name.isNotBlank()) processName = name
                    } catch (_: Exception) {}

                    if (processName.isNullOrBlank()) return@forEach
                    val pkg = processName.substringBefore(":")

                    // Memory: smaps_rollup → status fallback
                    var pssKb = 0; var rssKb = 0
                    try {
                        File(pidDir, "smaps_rollup").forEachLine { line ->
                            when {
                                line.startsWith("Pss:") -> pssKb = extractKb(line)
                                line.startsWith("Rss:") -> rssKb = extractKb(line)
                            }
                        }
                    } catch (_: Exception) {
                        try {
                            File(pidDir, "status").forEachLine { line ->
                                if (line.startsWith("VmRSS:")) rssKb = extractKb(line)
                            }
                            pssKb = rssKb
                        } catch (_: Exception) {}
                    }

                    if (pssKb > 0 || rssKb > 0) {
                        val prev = map[pkg]
                        if (prev == null) {
                            map[pkg] = Pair(pssKb, rssKb)
                        } else {
                            map[pkg] = Pair(prev.first + pssKb, prev.second + rssKb)
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return map
    }

    private fun buildActivityManagerMemMap(): Map<String, Pair<Int, Int>> {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        val running = activityManager.runningAppProcesses ?: return emptyMap()

        for (proc in running) {
            try {
                val memInfo = activityManager.getProcessMemoryInfo(intArrayOf(proc.pid))
                    .firstOrNull() ?: continue
                val pssKb = memInfo.totalPss.coerceAtLeast(0)
                val rssKb = estimateRssKb(memInfo)

                val packages = proc.pkgList?.filterNot { it.isNullOrBlank() }
                    ?.map { it.trim() }
                    ?.ifEmpty { listOf(proc.processName.substringBefore(":")) }
                    ?: listOf(proc.processName.substringBefore(":"))

                for (pkg in packages) {
                    if (pkg.isBlank()) continue
                    val prev = map[pkg]
                    map[pkg] = if (prev == null) {
                        Pair(pssKb, rssKb)
                    } else {
                        Pair(prev.first + pssKb, prev.second + rssKb)
                    }
                }
            } catch (_: Exception) {}
        }

        return map
    }

    private fun estimateRssKb(memInfo: Debug.MemoryInfo): Int {
        val totalKb = memInfo.totalPrivateDirty + memInfo.totalSharedDirty + memInfo.totalPrivateClean
        return totalKb.coerceAtLeast(0)
    }

    private fun getRunningProcessPackages(): Set<String> {
        val running = activityManager.runningAppProcesses ?: return emptySet()
        val packages = linkedSetOf<String>()
        for (proc in running) {
            proc.pkgList?.forEach { pkg ->
                if (!pkg.isNullOrBlank()) packages.add(pkg)
            }
            val processPkg = proc.processName.substringBefore(":")
            if (processPkg.isNotBlank()) packages.add(processPkg)
        }
        return packages
    }

    private fun getLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .toSet()
    }

    private fun collectRecentPackagesFromEvents(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long
    ): Set<String> {
        val lastSeen = linkedMapOf<String, Long>()
        return try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName.isNullOrBlank()) continue
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        event.eventType in setOf(
                            UsageEvents.Event.ACTIVITY_RESUMED,
                            UsageEvents.Event.ACTIVITY_PAUSED,
                            UsageEvents.Event.ACTIVITY_STOPPED
                        ) -> lastSeen[event.packageName] = event.timeStamp
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        event.eventType in setOf(
                            @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_FOREGROUND,
                            @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_BACKGROUND
                        ) -> lastSeen[event.packageName] = event.timeStamp
                }
            }

            val cutoff = endTime - 12 * 60 * 60 * 1000L
            lastSeen.entries
                .filter { it.value >= cutoff }
                .sortedByDescending { it.value }
                .map { it.key }
                .toCollection(linkedSetOf())
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun isAllowedUserApp(appInfo: ApplicationInfo): Boolean {
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystemApp || isUpdatedSystemApp
    }

    private fun isExcludedCorePackage(packageName: String): Boolean {
        val exact = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.miui.home",
            "com.mi.android.globallauncher"
        )
        if (packageName in exact) return true

        val prefixes = listOf(
            "android.",
            "com.android.",
            "com.miui.",
            "com.xiaomi.",
            "com.qualcomm.",
            "org.codeaurora.",
            "com.qti.",
            "vendor."
        )
        return prefixes.any { packageName.startsWith(it) }
    }

    private fun extractKb(line: String): Int =
        line.trim().split(Regex("\\s+")).getOrNull(1)?.toIntOrNull() ?: 0

    /** Save snapshot to Room */
    suspend fun saveSnapshot(info: RamInfo) {
        dao.insert(RamHistoryEntry(
            totalRam = info.totalRam, usedRam = info.usedRam, usagePercent = info.usagePercent
        ))
        dao.deleteOlderThan(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
    }

    fun getHistoryFlow() = dao.getLast100()
    suspend fun getAverageUsageLast1h() = dao.getAverageUsage(System.currentTimeMillis() - 60 * 60 * 1000L) ?: 0f
    suspend fun getPeakUsageLast1h() = dao.getPeakUsage(System.currentTimeMillis() - 60 * 60 * 1000L) ?: 0f

    /** Snapshot of per-uid (rx, tx) cumulative bytes from boot. No permissions needed. */
    fun getTrafficSnapshot(): Map<Int, Pair<Long, Long>> {
        val map = mutableMapOf<Int, Pair<Long, Long>>()
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in apps) {
            val uid = appInfo.uid
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx != TrafficStats.UNSUPPORTED.toLong() && tx != TrafficStats.UNSUPPORTED.toLong()) {
                val prev = map[uid]
                if (prev == null) {
                    map[uid] = Pair(rx, tx)
                } else {
                    // same UID shared by multiple packages — keep max
                    map[uid] = Pair(maxOf(prev.first, rx), maxOf(prev.second, tx))
                }
            }
        }
        return map
    }

    /** Returns global device RX/TX bytes since boot. */
    fun getGlobalTrafficSnapshot(): Pair<Long, Long> =
        Pair(TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes())

    /** Given two snapshots deltaMs apart, compute per-app live speed list. */
    suspend fun computeLiveTraffic(
        snap1: Map<Int, Pair<Long, Long>>,
        snap2: Map<Int, Pair<Long, Long>>,
        deltaMs: Long
    ): List<LiveTrafficInfo> = withContext(Dispatchers.IO) {
        if (deltaMs <= 0) return@withContext emptyList()
        val result = mutableListOf<LiveTrafficInfo>()
        for ((uid, bytes2) in snap2) {
            val bytes1 = snap1[uid] ?: continue
            val rxDelta = (bytes2.first - bytes1.first).coerceAtLeast(0)
            val txDelta = (bytes2.second - bytes1.second).coerceAtLeast(0)
            if (rxDelta == 0L && txDelta == 0L) continue
            val rxPerSec = rxDelta * 1000L / deltaMs
            val txPerSec = txDelta * 1000L / deltaMs

            val packages = try {
                packageManager.getPackagesForUid(uid)?.toList() ?: continue
            } catch (_: Exception) { continue }
            val pkg = packages.firstOrNull() ?: continue
            val appName = try {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (_: Exception) { pkg }

            result.add(LiveTrafficInfo(uid, pkg, appName, rxPerSec, txPerSec))
        }
        result.sortedByDescending { it.totalBytesPerSec }
    }

    /**
     * Cumulative per-uid (rx, tx) bytes from [startMs] to now, via NetworkStatsManager.
     * Call this twice with the SAME startMs and diff the results to get live speed.
     */
    @Suppress("DEPRECATION")
    suspend fun getNsmSnapshot(startMs: Long): Map<Int, Pair<Long, Long>> =
        withContext(Dispatchers.IO) {
            if (!hasUsageStatsPermission()) return@withContext emptyMap()
            val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
                ?: return@withContext emptyMap()

            val now = System.currentTimeMillis()
            val map = mutableMapOf<Int, Pair<Long, Long>>()

            for (type in listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
                try {
                    val stats = nsm.querySummary(type, null, startMs, now)
                    val bucket = NetworkStats.Bucket()
                    while (stats.hasNextBucket()) {
                        stats.getNextBucket(bucket)
                        val uid = bucket.uid
                        if (uid < 0) continue
                        val prev = map[uid] ?: Pair(0L, 0L)
                        map[uid] = Pair(prev.first + bucket.rxBytes, prev.second + bucket.txBytes)
                    }
                    stats.close()
                } catch (_: Exception) {}
            }
            map
        }

    /**
     * Diffs two NSM snapshots (taken with the same startMs) to produce per-app bytes/sec.
     * This is the reliable way to get live traffic — avoids NSM cache/window issues.
     */
    suspend fun computeLiveTrafficFromNsm(
        snap1: Map<Int, Pair<Long, Long>>,
        snap2: Map<Int, Pair<Long, Long>>,
        deltaMs: Long
    ): List<LiveTrafficInfo> = withContext(Dispatchers.IO) {
        if (deltaMs <= 0) return@withContext emptyList()
        val result = mutableListOf<LiveTrafficInfo>()

        for (uid in (snap1.keys + snap2.keys).toSet()) {
            val b1 = snap1[uid] ?: Pair(0L, 0L)
            val b2 = snap2[uid] ?: Pair(0L, 0L)
            val rxDelta = (b2.first - b1.first).coerceAtLeast(0)
            val txDelta = (b2.second - b1.second).coerceAtLeast(0)
            if (rxDelta == 0L && txDelta == 0L) continue

            val rxPerSec = rxDelta * 1000L / deltaMs
            val txPerSec = txDelta * 1000L / deltaMs

            val packages = try {
                packageManager.getPackagesForUid(uid)?.toList()
            } catch (_: Exception) { null }
            val pkg = packages?.firstOrNull() ?: "uid:$uid"
            val appName = if (packages != null) {
                try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (_: Exception) { pkg }
            } else "UID $uid"

            result.add(LiveTrafficInfo(uid, pkg, appName, rxPerSec, txPerSec))
        }
        result.sortedByDescending { it.totalBytesPerSec }
    }

    /** Returns per-app network usage for the last 24 hours, sorted by total bytes descending. */
    @Suppress("DEPRECATION")
    suspend fun getNetworkUsageList(): List<NetworkUsageInfo> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) return@withContext emptyList()
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return@withContext emptyList()

        val end = System.currentTimeMillis()
        val start = end - 24 * 60 * 60 * 1000L

        val usageMap = mutableMapOf<Int, Pair<Long, Long>>() // uid -> (rx, tx)

        fun mergeStats(bucket: NetworkStats.Bucket) {
            val uid = bucket.uid
            if (uid < 0) return
            val prev = usageMap[uid] ?: Pair(0L, 0L)
            usageMap[uid] = Pair(prev.first + bucket.rxBytes, prev.second + bucket.txBytes)
        }

        for (type in listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            try {
                val stats = nsm.querySummary(type, null, start, end)
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    mergeStats(bucket)
                }
                stats.close()
            } catch (_: Exception) {}
        }

        val result = mutableListOf<NetworkUsageInfo>()
        for ((uid, bytes) in usageMap) {
            if (bytes.first == 0L && bytes.second == 0L) continue
            val packages = try {
                packageManager.getPackagesForUid(uid)?.toList() ?: continue
            } catch (_: Exception) { continue }

            val pkg = packages.firstOrNull() ?: continue
            val appName = try {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (_: Exception) { pkg }

            result.add(NetworkUsageInfo(uid, pkg, appName, bytes.first, bytes.second))
        }

        result.sortedByDescending { it.totalBytes }
    }
}
