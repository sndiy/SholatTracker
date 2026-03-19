package com.sholattracker.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Models ──────────────────────────────────────────────

data class Sholat(
    val id: String,
    val name: String,
    val arabicName: String,
    val defaultTime: String
)

data class DayRecord(
    val date: String,
    val completed: Set<String>,
    val timestamps: Map<String, String> = emptyMap() // sholatId -> "HH:mm:ss"
) {
    val count: Int get() = completed.size
    val isComplete: Boolean get() = count == 5

    // Entri terakhir yang diceklis
    fun lastCheckedEntry(): Pair<String, String>? {
        return timestamps.entries
            .filter { completed.contains(it.key) }
            .maxByOrNull { it.value }
            ?.let { Pair(it.key, it.value) }
    }
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
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        if (!prefs.contains("install_date")) {
            prefs.edit().putString("install_date", LocalDate.now().format(fmt)).apply()
        }
    }

    fun getInstallDate(): LocalDate {
        val raw = prefs.getString("install_date", null)
        return if (raw != null) LocalDate.parse(raw, fmt) else LocalDate.now()
    }

    fun todayKey(): String = LocalDate.now().format(fmt)

    // ── Sholat Data ──

    fun getRecord(date: String): DayRecord {
        val raw = prefs.getString(date, null) ?: return DayRecord(date, emptySet())
        return try {
            val json = JSONObject(raw)
            val completed = mutableSetOf<String>()
            val timestamps = mutableMapOf<String, String>()
            SHOLAT_LIST.forEach { s ->
                if (json.optBoolean(s.id, false)) completed.add(s.id)
                val ts = json.optString("${s.id}_ts", "")
                if (ts.isNotEmpty()) timestamps[s.id] = ts
            }
            DayRecord(date, completed, timestamps)
        } catch (e: Exception) {
            DayRecord(date, emptySet())
        }
    }

    fun toggle(date: String, sholatId: String) {
        val record = getRecord(date)
        val newCompleted = record.completed.toMutableSet()
        val newTimestamps = record.timestamps.toMutableMap()

        val isChecking = !newCompleted.contains(sholatId)
        if (isChecking) {
            newCompleted.add(sholatId)
            // Hanya simpan timestamp pertama kali centang, tidak overwrite
            if (!newTimestamps.containsKey(sholatId)) {
                newTimestamps[sholatId] = LocalTime.now().format(timeFmt)
            }
        } else {
            newCompleted.remove(sholatId)
            // Timestamp tetap disimpan meski un-centang (sebagai bukti pernah diceklis)
        }

        saveRecord(DayRecord(date, newCompleted, newTimestamps))
    }

    fun resetDay(date: String) {
        prefs.edit().remove(date).apply()
    }

    private fun saveRecord(record: DayRecord) {
        val json = JSONObject()
        SHOLAT_LIST.forEach { s ->
            json.put(s.id, record.completed.contains(s.id))
            record.timestamps[s.id]?.let { json.put("${s.id}_ts", it) }
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

    fun saveNotifTimeForDate(date: String, sholatId: String, hour: Int, minute: Int) {
        notifPrefs.edit()
            .putInt("${date}_${sholatId}_hour", hour)
            .putInt("${date}_${sholatId}_min", minute)
            .apply()
    }

    fun getNotifTimesForDate(date: String): List<NotifTime> {
        return SHOLAT_LIST.map { s ->
            val default = s.defaultTime.split(":")
            val defaultHour = notifPrefs.getInt("${s.id}_hour", default[0].toInt())
            val defaultMinute = notifPrefs.getInt("${s.id}_min", default[1].toInt())
            val hour = notifPrefs.getInt("${date}_${s.id}_hour", defaultHour)
            val minute = notifPrefs.getInt("${date}_${s.id}_min", defaultMinute)
            val enabled = notifPrefs.getBoolean("${s.id}_enabled", true)
            NotifTime(s.id, hour, minute, enabled)
        }
    }

    fun hasScheduleForDate(date: String): Boolean {
        return notifPrefs.contains("${date}_subuh_hour")
    }

    // ── Auto-fetch jadwal dari API ──

    fun wasScheduleFetchedToday(): Boolean {
        val today = LocalDate.now().toString()
        return prefs.getString("last_fetch_date", null) == today
    }

    fun markFetchedToday() {
        prefs.edit().putString("last_fetch_date", LocalDate.now().toString()).apply()
    }

    // Panggil dari coroutine IO thread
    fun fetchAndSaveMonthlySchedule(): Boolean {
        return try {
            val today = LocalDate.now()
            val month = today.monthValue
            val year = today.year
            val url = "https://api.aladhan.com/v1/calendarByCity?city=Surabaya&country=Indonesia&method=20&month=$month&year=$year"
            val json = URL(url).readText()

            val root = org.json.JSONObject(json)
            val data = root.getJSONArray("data")

            fun parseTime(raw: String): Pair<Int, Int> {
                val clean = raw.substringBefore(" ").trim().split(":")
                return Pair(clean[0].toInt(), clean[1].toInt())
            }

            val editor = notifPrefs.edit()
            for (i in 0 until data.length()) {
                val entry = data.getJSONObject(i)
                val day = entry.getJSONObject("date").getJSONObject("gregorian")
                    .getString("day").toInt()
                val dateStr = "%04d-%02d-%02d".format(year, month, day)
                val timings = entry.getJSONObject("timings")

                val sholatKeys = listOf(
                    "subuh" to "Fajr",
                    "dzuhur" to "Dhuhr",
                    "ashar" to "Asr",
                    "maghrib" to "Maghrib",
                    "isya" to "Isha"
                )
                sholatKeys.forEach { (id, key) ->
                    val (h, m) = parseTime(timings.getString(key))
                    editor.putInt("${dateStr}_${id}_hour", h)
                    editor.putInt("${dateStr}_${id}_min", m)
                    // Update default juga untuk hari ini
                    if (day == today.dayOfMonth) {
                        editor.putInt("${id}_hour", h)
                        editor.putInt("${id}_min", m)
                    }
                }
            }
            editor.apply()
            markFetchedToday()
            true
        } catch (e: Exception) {
            false
        }
    }
}