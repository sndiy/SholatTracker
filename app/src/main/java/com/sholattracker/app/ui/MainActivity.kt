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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: SholatRepository
    private lateinit var adapter: SholatAdapter
    private lateinit var scheduler: NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = SholatRepository(this)
        scheduler = NotificationScheduler(this)

        setupRecyclerView()
        requestNotificationPermission()
        scheduler.scheduleAll(repo.getNotifTimes())
        render()

        binding.btnReset.setOnClickListener { confirmReset() }
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun setupRecyclerView() {
        adapter = SholatAdapter { sholatId ->
            repo.toggle(repo.todayKey(), sholatId)
            render()
        }
        binding.rvSholat.layoutManager = LinearLayoutManager(this)
        binding.rvSholat.adapter = adapter
    }

    private fun render() {
        val today = repo.todayKey()
        val record = repo.getRecord(today)
        val streak = repo.calcStreak()

        // Date
        binding.tvDate.text = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        )

        // Progress
        binding.progressBar.progress = record.count
        binding.tvProgress.text = "${record.count} / 5 sholat"

        // Streak
        if (streak > 0) {
            binding.tvStreak.text = "🔥 $streak hari berturut-turut sholat lengkap"
            binding.cardStreak.visibility = android.view.View.VISIBLE
        } else {
            binding.cardStreak.visibility = android.view.View.GONE
        }

        // Adapter
        val nextId = getNextSholatId(record.completed)
        adapter.submitList(SHOLAT_LIST.map { sholat ->
            SholatItem(
                sholat = sholat,
                isChecked = record.completed.contains(sholat.id),
                isNext = sholat.id == nextId
            )
        })

        // Quote
        if (record.isComplete) {
            binding.tvQuote.text =
                "Alhamdulillah, semua sholat hari ini selesai.\nSemoga diterima Allah ﷻ"
            binding.cardQuote.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.gold_dim)
            )
        } else {
            binding.tvQuote.text =
                MOTIVATIONAL_QUOTES[LocalDate.now().dayOfMonth % MOTIVATIONAL_QUOTES.size]
            binding.cardQuote.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.surface2)
            )
        }

        // Update homescreen widget
        WidgetUpdateReceiver.send(this)
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

    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Hari Ini?")
            .setMessage("Semua centang hari ini akan dihapus.")
            .setPositiveButton("Reset") { _, _ ->
                repo.resetDay(repo.todayKey())
                render()
            }
            .setNegativeButton("Batal", null)
            .show()
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
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
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
        else -> super.onOptionsItemSelected(item)
    }
}
