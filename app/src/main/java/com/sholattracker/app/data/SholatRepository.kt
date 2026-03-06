package com.sholattracker.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Models ──────────────────────────────────────────────

data class Sholat(
    val id: String,
    val name: String,
    val arabicName: String,
    val defaultTime: String  // "HH:mm"
)

data class DayRecord(
    val date: String,           // "YYYY-MM-DD"
    val completed: Set<String>  // set of sholat ids
) {
    val count: Int get() = completed.size
    val isComplete: Boolean get() = count == 5
}

data class NotifTime(
    val sholatId: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

// ── Constants ────────────────────────────────────────────

val SHOLAT_LIST = listOf(
    Sholat("subuh",   "Subuh",   "الصُّبْح",    "04:30"),
    Sholat("dzuhur",  "Dzuhur",  "الظُّهْر",    "12:00"),
    Sholat("ashar",   "Ashar",   "الْعَصْر",    "15:30"),
    Sholat("maghrib", "Maghrib", "الْمَغْرِب",  "18:00"),
    Sholat("isya",    "Isya",    "الْعِشَاء",   "19:15"),
)

val MOTIVATIONAL_QUOTES = listOf(
    "Sesungguhnya sholat itu mencegah dari perbuatan keji dan mungkar.",
    "Dan dirikanlah sholat untuk mengingat-Ku. (QS. Taha: 14)",
    "Sholat adalah tiang agama. Barang siapa mendirikannya, ia menegakkan agama.",
    "Amal yang pertama kali dihisab pada hari kiamat adalah sholat.",
    "Tidaklah seorang hamba sujud kepada Allah, melainkan Allah angkat derajatnya.",
)

// ── Repository ───────────────────────────────────────────

class SholatRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sholat_tracker", Context.MODE_PRIVATE)

    private val notifPrefs: SharedPreferences =
        context.getSharedPreferences("sholat_notif", Context.MODE_PRIVATE)

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayKey(): String = LocalDate.now().format(fmt)

    // ── Sholat Data ──

    fun getRecord(date: String): DayRecord {
        val raw = prefs.getString(date, null) ?: return DayRecord(date, emptySet())
        return try {
            val json = JSONObject(raw)
            val completed = mutableSetOf<String>()
            SHOLAT_LIST.forEach { s ->
                if (json.optBoolean(s.id, false)) completed.add(s.id)
            }
            DayRecord(date, completed)
        } catch (e: Exception) {
            DayRecord(date, emptySet())
        }
    }

    fun toggle(date: String, sholatId: String) {
        val record = getRecord(date)
        val newCompleted = record.completed.toMutableSet()
        if (newCompleted.contains(sholatId)) newCompleted.remove(sholatId)
        else newCompleted.add(sholatId)
        saveRecord(DayRecord(date, newCompleted))
    }

    fun resetDay(date: String) {
        prefs.edit().remove(date).apply()
    }

    private fun saveRecord(record: DayRecord) {
        val json = JSONObject()
        SHOLAT_LIST.forEach { s ->
            json.put(s.id, record.completed.contains(s.id))
        }
        prefs.edit().putString(record.date, json.toString()).apply()
    }

    fun getAllRecords(): List<DayRecord> {
        return prefs.all.keys
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .sortedDescending()
            .map { getRecord(it) }
    }

    // ── Streak ──

    fun calcStreak(): Int {
        var streak = 0
        var date = LocalDate.now().minusDays(1)
        repeat(365) {
            val record = getRecord(date.format(fmt))
            if (record.isComplete) {
                streak++
                date = date.minusDays(1)
            } else return streak
        }
        return streak
    }

    // ── Notification Times ──

    fun getNotifTimes(): List<NotifTime> {
        return SHOLAT_LIST.map { s ->
            val default = s.defaultTime.split(":")
            val hour = notifPrefs.getInt("${s.id}_hour", default[0].toInt())
            val minute = notifPrefs.getInt("${s.id}_min", default[1].toInt())
            val enabled = notifPrefs.getBoolean("${s.id}_enabled", true)
            NotifTime(s.id, hour, minute, enabled)
        }
    }

    fun saveNotifTime(sholatId: String, hour: Int, minute: Int, enabled: Boolean) {
        notifPrefs.edit()
            .putInt("${sholatId}_hour", hour)
            .putInt("${sholatId}_min", minute)
            .putBoolean("${sholatId}_enabled", enabled)
            .apply()
    }
}
