package com.sholattracker.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sholattracker.app.R
import com.sholattracker.app.data.NotifTime
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.ui.MainActivity
import java.util.Calendar

const val CHANNEL_ID = "sholat_reminder"
const val EXTRA_SHOLAT_ID = "sholat_id"

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Re-schedule after reboot
                val repo = SholatRepository(context)
                NotificationScheduler(context).scheduleAll(repo.getNotifTimes())
            }
            "com.sholattracker.SHOLAT_ALARM" -> {
                val sholatId = intent.getStringExtra(EXTRA_SHOLAT_ID) ?: return
                val sholat = SHOLAT_LIST.find { it.id == sholatId } ?: return
                showNotification(context, sholat.name, sholatId)
            }
        }
    }

    private fun showNotification(context: Context, sholatName: String, sholatId: String) {
        createChannel(context)

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktunya Sholat $sholatName 🕌")
            .setContentText("Jangan tunda sholatmu. Sholat lebih baik dari tidur.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Waktunya sholat $sholatName. Tinggalkan sejenak aktivitasmu dan hadap kepada Allah ﷻ"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(sholatId.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat Sholat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi pengingat waktu sholat"
            enableVibration(true)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}

// ── Scheduler ──────────────────────────────────────────

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(times: List<NotifTime>) {
        times.forEach { nt ->
            if (nt.enabled) schedule(nt) else cancel(nt.sholatId)
        }
    }

    private fun schedule(nt: NotifTime) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.sholattracker.SHOLAT_ALARM"
            putExtra(EXTRA_SHOLAT_ID, nt.sholatId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nt.sholatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nt.hour)
            set(Calendar.MINUTE, nt.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback to inexact
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancel(sholatId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, sholatId.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }
}
