package com.sholattracker.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sholattracker.app.R
import com.sholattracker.app.data.MOTIVATIONAL_QUOTES
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.databinding.ActivityMainBinding
import com.sholattracker.app.notification.NotificationScheduler
import com.sholattracker.app.pdf.PdfExporter
import com.sholattracker.app.widget.WidgetUpdateReceiver
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: SholatRepository
    private lateinit var sholatAdapter: SholatAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var scheduler: NotificationScheduler
    private var activeTooltip: android.widget.PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = SholatRepository(this)
        scheduler = NotificationScheduler(this)

        setupSholatList()
        setupCalendar()
        requestNotificationPermission()
        scheduler.scheduleAll(repo.getNotifTimes())
        render()

        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
        // Auto-refresh jadwal jika belum fetch hari ini
        if (!repo.wasScheduleFetchedToday()) {
            fetchJadwalSilent()
        }
    }

    private fun fetchJadwalSilent() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                repo.fetchAndSaveMonthlySchedule()
            }
            if (success) {
                scheduler.scheduleAll(repo.getNotifTimesForDate(repo.todayKey()))
                render()
            }
        }
    }

    private fun setupSholatList() {
        sholatAdapter = SholatAdapter { sholatId ->
            repo.toggle(repo.todayKey(), sholatId)
            render()
            WidgetUpdateReceiver.send(this)
        }
        binding.rvSholat.layoutManager = LinearLayoutManager(this)
        binding.rvSholat.adapter = sholatAdapter
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { day, anchor ->
            showTooltip(day, anchor)
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(this, 7)
        binding.rvCalendar.adapter = calendarAdapter
    }

    private fun render() {
        val today = repo.todayKey()
        val record = repo.getRecord(today)
        val streak = repo.calcStreak()

        // Date
        binding.tvDate.text = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        )

        // ── Progress bar motivasi ──
        binding.progressBar.progress = record.count
        val (progressLabel, progressColor) = when (record.count) {
            0 -> Pair("Mulai hari dengan sholat Subuh ✨", R.color.text_muted)
            1 -> Pair("Bagus! Jangan berhenti di sini 💪", R.color.text_muted)
            2 -> Pair("Setengah perjalanan, teruskan! 🌙", R.color.text_muted)
            3 -> Pair("3 dari 5 — kamu hebat! ⭐", R.color.gold)
            4 -> Pair("Tinggal satu lagi, hampir sempurna! 🔥", R.color.gold)
            5 -> Pair("Alhamdulillah, sempurna! Semoga diterima ﷻ", R.color.green)
            else -> Pair("", R.color.text_muted)
        }
        binding.tvProgress.text = "${record.count}/5"
        binding.tvProgressMotivasi.text = progressLabel
        binding.tvProgressMotivasi.setTextColor(ContextCompat.getColor(this, progressColor))

        // ── Streak ──
        if (streak > 0) {
            binding.tvStreak.text = "🔥 $streak hari berturut-turut sholat lengkap"
            binding.cardStreak.visibility = android.view.View.VISIBLE
        } else {
            binding.cardStreak.visibility = android.view.View.GONE
        }

        // ── Sholat list ──
        val nextId = getNextSholatId(record.completed)
        val notifTimes = repo.getNotifTimesForDate(today).associateBy { it.sholatId }
        val nowTime = LocalTime.now()
        sholatAdapter.submitList(SHOLAT_LIST.map { sholat ->
            val nt = notifTimes[sholat.id]
            val sholatTime = nt?.let { LocalTime.of(it.hour, it.minute) }
            val isDisabled = !record.completed.contains(sholat.id) &&
                    sholatTime != null && nowTime.isBefore(sholatTime)
            SholatItem(
                sholat = sholat,
                isChecked = record.completed.contains(sholat.id),
                isNext = sholat.id == nextId,
                displayTime = nt?.let {
                    String.format("%02d:%02d", it.hour, it.minute)
                } ?: sholat.defaultTime,
                isDisabled = isDisabled
            )
        })

        // ── Quote ──
        if (record.isComplete) {
            binding.tvQuote.text = "Alhamdulillah, semua sholat hari ini selesai.\nSemoga diterima Allah ﷻ"
            binding.cardQuote.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gold_dim))
        } else {
            binding.tvQuote.text = MOTIVATIONAL_QUOTES[LocalDate.now().dayOfMonth % MOTIVATIONAL_QUOTES.size]
            binding.cardQuote.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface2))
        }

        // ── Kalender bulan ini ──
        renderCalendar()

        WidgetUpdateReceiver.send(this)
    }

    private fun renderCalendar() {
        val now = LocalDate.now()
        val installDate = repo.getInstallDate()
        val yearMonth = YearMonth.of(now.year, now.month)
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()

        binding.tvCalendarMonth.text = now.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
        ).replaceFirstChar { it.uppercase() }

        val days = mutableListOf<CalendarDay>()

        repeat(firstDayOfWeek) {
            days.add(CalendarDay(null, DayStatus.EMPTY))
        }

        for (d in 1..daysInMonth) {
            val date = yearMonth.atDay(d)
            val isToday = date == now
            val status = when {
                date > now -> DayStatus.FUTURE
                date < installDate -> DayStatus.NONE   // sebelum install = kosong
                else -> {
                    val rec = repo.getRecord(date.toString())
                    when {
                        rec.count == 0 && date < now -> DayStatus.MISSED
                        rec.count == 5 -> DayStatus.COMPLETE
                        rec.count in 1..4 -> DayStatus.PARTIAL
                        else -> DayStatus.NONE
                    }
                }
            }
            days.add(CalendarDay(date, status, isToday))
        }

        calendarAdapter.submitDays(days)
    }

    private fun getNextSholatId(completed: Set<String>): String? {
        val now = LocalTime.now()
        val times = mapOf(
            "subuh"   to LocalTime.of(4, 30),
            "dzuhur"  to LocalTime.of(12, 0),
            "ashar"   to LocalTime.of(15, 30),
            "maghrib" to LocalTime.of(18, 0),
            "isya"    to LocalTime.of(19, 15),
        )
        for (s in SHOLAT_LIST) {
            if (completed.contains(s.id)) continue
            if ((times[s.id] ?: continue) >= now) return s.id
        }
        return SHOLAT_LIST.firstOrNull { !completed.contains(it.id) }?.id
    }

    private fun showTooltip(day: CalendarDay, anchor: android.view.View) {
        val date = day.date ?: return

        activeTooltip?.dismiss()

        val dateStr = date.format(DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID")))
        val rec = repo.getRecord(date.toString())

        val (statusText, statusColor) = when (day.status) {
            DayStatus.COMPLETE -> Pair("Lengkap 5/5", R.color.green)
            DayStatus.PARTIAL  -> Pair("Sebagian ${rec.count}/5", R.color.gold)
            DayStatus.MISSED   -> Pair("Terlewat 0/5", R.color.red)
            else               -> Pair("Belum ada data", R.color.text_muted)
        }

        // Aktivitas terakhir dari timestamps
        val lastEntry = rec.lastCheckedEntry()
        val lastActivityText = if (lastEntry != null) {
            val sholatName = SHOLAT_LIST.find { it.id == lastEntry.first }?.name ?: lastEntry.first
            "Terakhir: $sholatName · ${lastEntry.second}"
        } else null

        val tooltipView = layoutInflater.inflate(R.layout.tooltip_calendar, null)
        tooltipView.findViewById<android.widget.TextView>(R.id.tvTooltipDate).text = dateStr
        tooltipView.findViewById<android.widget.TextView>(R.id.tvTooltipStatus).apply {
            text = statusText
            setTextColor(ContextCompat.getColor(this@MainActivity, statusColor))
        }

        // Tampilkan aktivitas terakhir jika ada
        val tvLastActivity = tooltipView.findViewById<android.widget.TextView?>(R.id.tvTooltipLastActivity)
        if (tvLastActivity != null) {
            if (lastActivityText != null) {
                tvLastActivity.text = lastActivityText
                tvLastActivity.visibility = android.view.View.VISIBLE
            } else {
                tvLastActivity.visibility = android.view.View.GONE
            }
        }

        val popup = android.widget.PopupWindow(
            tooltipView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        tooltipView.measure(
            android.view.View.MeasureSpec.UNSPECIFIED,
            android.view.View.MeasureSpec.UNSPECIFIED
        )
        val xOff = -(tooltipView.measuredWidth / 2) + (anchor.width / 2)
        val yOff = -(tooltipView.measuredHeight) - anchor.height - 4

        popup.showAsDropDown(anchor, xOff, yOff)
        activeTooltip = popup
        anchor.postDelayed({ popup.dismiss() }, 3000)
    }

    private fun exportPdf() {
        try {
            val file = PdfExporter(this, repo).export()
            PdfExporter(this, repo).shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal export PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_notif_settings -> {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
            true
        }
        R.id.action_refresh -> {
            lifecycleScope.launch {
                Toast.makeText(this@MainActivity, "Memperbarui jadwal...", Toast.LENGTH_SHORT).show()
                val success = withContext(Dispatchers.IO) {
                    repo.fetchAndSaveMonthlySchedule()
                }
                if (success) {
                    scheduler.scheduleAll(repo.getNotifTimesForDate(repo.todayKey()))
                    render()
                    Toast.makeText(this@MainActivity, "Jadwal berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Gagal memperbarui, periksa koneksi.", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}