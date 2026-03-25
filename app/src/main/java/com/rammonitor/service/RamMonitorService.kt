package com.rammonitor.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rammonitor.R
import com.rammonitor.data.RamRepository
import com.rammonitor.ui.MainActivity
import kotlinx.coroutines.*

class RamMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "ram_monitor_channel"
        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002
        const val ACTION_STOP = "ACTION_STOP"
        var isRunning = false
        const val ALERT_THRESHOLD = 85f   // % — alert above this
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: RamRepository
    private var lastAlertTime = 0L

    override fun onCreate() {
        super.onCreate()
        repo = RamRepository(applicationContext)
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification("RAM izleniyor…", 0f))
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    val info = repo.getRamInfo()
                    repo.saveSnapshot(info)
                    updateNotification(info.usagePercent, info.usedMb, info.totalMb)
                    checkAlert(info.usagePercent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000) // 3 second interval
            }
        }
    }

    private fun updateNotification(percent: Float, usedMb: Float, totalMb: Float) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(
            "RAM: %.1f%% — %.0f / %.0f MB".format(percent, usedMb, totalMb),
            percent
        ))
    }

    private fun buildNotification(text: String, percent: Float): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RamMonitorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RAM Monitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_memory)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stop, "Durdur", stopIntent)
            .setOngoing(true)
            .setProgress(100, percent.toInt(), false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun checkAlert(percent: Float) {
        val now = System.currentTimeMillis()
        if (percent >= ALERT_THRESHOLD && now - lastAlertTime > 60_000) {
            lastAlertTime = now
            sendAlertNotification(percent)
        }
    }

    private fun sendAlertNotification(percent: Float) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Yüksek RAM Kullanımı!")
            .setContentText("RAM kullanımı %.1f%%'e ulaştı".format(percent))
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(ALERT_NOTIF_ID, notif)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RAM Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Anlık RAM kullanım bildirimleri"
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
