package com.sholattracker.app.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sholattracker.app.R
import com.sholattracker.app.data.NotifTime
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.databinding.ActivityNotifSettingsBinding
import com.sholattracker.app.databinding.ItemNotifSettingBinding
import com.sholattracker.app.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class JadwalHarian(
    val tanggal: Int,
    val hari: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val isToday: Boolean = false
)

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotifSettingsBinding
    private lateinit var repo: SholatRepository
    private lateinit var scheduler: NotificationScheduler
    private lateinit var adapter: NotifSettingAdapter
    private val currentTimes = mutableListOf<NotifTime>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotifSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Pengingat Sholat"

        repo = SholatRepository(this)
        scheduler = NotificationScheduler(this)
        currentTimes.addAll(repo.getNotifTimes())

        adapter = NotifSettingAdapter(currentTimes) { index, hour, minute ->
            currentTimes[index] = currentTimes[index].copy(hour = hour, minute = minute)
            adapter.notifyItemChanged(index)
        }

        binding.rvNotifSettings.layoutManager = LinearLayoutManager(this)
        binding.rvNotifSettings.adapter = adapter

        binding.btnSave.setOnClickListener {
            currentTimes.forEach { nt ->
                repo.saveNotifTime(nt.sholatId, nt.hour, nt.minute, nt.enabled)
            }
            scheduler.scheduleAll(currentTimes)
            Toast.makeText(this, "Pengingat disimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnFetchSchedule.setOnClickListener {
            showJadwalDialog()
        }
    }

    private fun showJadwalDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_jadwal_bulanan)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val today = LocalDate.now()
        val monthName = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")))
            .replaceFirstChar { it.uppercase() }

        dialog.findViewById<TextView>(R.id.tvDialogTitle).text = "Jadwal Sholat $monthName"

        val layoutLoading = dialog.findViewById<View>(R.id.layoutLoading)
        val rvJadwal = dialog.findViewById<RecyclerView>(R.id.rvJadwal)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)
        val btnTerapkan = dialog.findViewById<View>(R.id.btnTerapkan)
        val btnTutup = dialog.findViewById<View>(R.id.btnTutup)

        var jadwalList = listOf<JadwalHarian>()

        btnTerapkan.isEnabled = false

        btnTutup.setOnClickListener { dialog.dismiss() }

        btnTerapkan.setOnClickListener {
            if (jadwalList.isNotEmpty()) {
                applyJadwalBulanan(jadwalList, today)
                Toast.makeText(this, "Jadwal 1 bulan berhasil diterapkan!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()

        // Fetch data
        val month = today.monthValue
        val year = today.year
        val url = "https://api.aladhan.com/v1/calendarByCity?city=Surabaya&country=Indonesia&method=20&month=$month&year=$year"

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val json = URL(url).readText()
                    parseFullMonth(json, today)
                }

                jadwalList = result
                val jadwalAdapter = JadwalAdapter(result)
                rvJadwal.layoutManager = LinearLayoutManager(this@NotificationSettingsActivity)
                rvJadwal.adapter = jadwalAdapter

                val todayIndex = result.indexOfFirst { it.isToday }
                if (todayIndex >= 0) rvJadwal.scrollToPosition(todayIndex)

                layoutLoading.visibility = View.GONE
                rvJadwal.visibility = View.VISIBLE
                btnTerapkan.isEnabled = true

            } catch (e: Exception) {
                layoutLoading.visibility = View.GONE
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun applyJadwalBulanan(jadwalList: List<JadwalHarian>, today: LocalDate) {
        fun toHourMinute(time: String): Pair<Int, Int> {
            val parts = time.split(":")
            return Pair(parts[0].toInt(), parts[1].toInt())
        }

        val year = today.year
        val month = today.monthValue

        // Simpan semua hari ke storage per tanggal
        jadwalList.forEach { jadwal ->
            val dateStr = "%04d-%02d-%02d".format(year, month, jadwal.tanggal)
            val times = listOf(jadwal.fajr, jadwal.dhuhr, jadwal.asr, jadwal.maghrib, jadwal.isha)
            SHOLAT_LIST.forEachIndexed { i, sholat ->
                val (h, m) = toHourMinute(times[i])
                repo.saveNotifTimeForDate(dateStr, sholat.id, h, m)
            }
        }

        // Terapkan jadwal hari ini ke alarm aktif + update tampilan
        val todayJadwal = jadwalList.find { it.isToday } ?: return
        val times = listOf(todayJadwal.fajr, todayJadwal.dhuhr, todayJadwal.asr, todayJadwal.maghrib, todayJadwal.isha)
        times.forEachIndexed { i, t ->
            val (h, m) = toHourMinute(t)
            currentTimes[i] = currentTimes[i].copy(hour = h, minute = m)
            repo.saveNotifTime(SHOLAT_LIST[i].id, h, m, currentTimes[i].enabled)
        }
        adapter.notifyDataSetChanged()
        scheduler.scheduleAll(currentTimes)
    }

    private fun parseFullMonth(json: String, today: LocalDate): List<JadwalHarian> {
        val root = JSONObject(json)
        val data = root.getJSONArray("data")
        val list = mutableListOf<JadwalHarian>()

        fun parseTime(raw: String): String {
            return raw.substringBefore(" ").trim()
        }

        for (i in 0 until data.length()) {
            val entry = data.getJSONObject(i)
            val dateObj = entry.getJSONObject("date").getJSONObject("gregorian")
            val day = dateObj.getString("day").toInt()
            val dayName = entry.getJSONObject("date").getJSONObject("gregorian")
                .getJSONObject("weekday").getString("en").take(3)
            val timings = entry.getJSONObject("timings")

            list.add(JadwalHarian(
                tanggal = day,
                hari = dayName,
                fajr = parseTime(timings.getString("Fajr")),
                dhuhr = parseTime(timings.getString("Dhuhr")),
                asr = parseTime(timings.getString("Asr")),
                maghrib = parseTime(timings.getString("Maghrib")),
                isha = parseTime(timings.getString("Isha")),
                isToday = day == today.dayOfMonth
            ))
        }
        return list
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

// ── Adapter tabel jadwal bulanan ──────────────────────────

class JadwalAdapter(
    private val list: List<JadwalHarian>
) : RecyclerView.Adapter<JadwalAdapter.VH>() {

    class VH(val view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvFajr: TextView = view.findViewById(R.id.tvFajr)
        val tvDhuhr: TextView = view.findViewById(R.id.tvDhuhr)
        val tvAsr: TextView = view.findViewById(R.id.tvAsr)
        val tvMaghrib: TextView = view.findViewById(R.id.tvMaghrib)
        val tvIsha: TextView = view.findViewById(R.id.tvIsha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_row, parent, false)
        return VH(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        val ctx = holder.view.context

        holder.tvTanggal.text = item.tanggal.toString()
        holder.tvFajr.text = item.fajr
        holder.tvDhuhr.text = item.dhuhr
        holder.tvAsr.text = item.asr
        holder.tvMaghrib.text = item.maghrib
        holder.tvIsha.text = item.isha

        if (item.isToday) {
            holder.view.setBackgroundColor(ctx.getColor(R.color.gold_dim))
            holder.tvTanggal.setTextColor(ctx.getColor(R.color.gold))
            listOf(holder.tvFajr, holder.tvDhuhr, holder.tvAsr, holder.tvMaghrib, holder.tvIsha)
                .forEach { it.setTextColor(ctx.getColor(R.color.gold_soft)) }
        } else {
            holder.view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            holder.tvTanggal.setTextColor(ctx.getColor(R.color.gold_soft))
            listOf(holder.tvFajr, holder.tvDhuhr, holder.tvAsr, holder.tvMaghrib, holder.tvIsha)
                .forEach { it.setTextColor(ctx.getColor(R.color.text_muted)) }
        }
    }
}

// ── Adapter pengaturan notif ──────────────────────────────

class NotifSettingAdapter(
    private val times: MutableList<NotifTime>,
    private val onTimeChanged: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<NotifSettingAdapter.VH>() {

    inner class VH(private val b: ItemNotifSettingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(index: Int) {
            val nt = times[index]
            val sholat = SHOLAT_LIST.first { it.id == nt.sholatId }
            b.tvSholatName.text = sholat.name
            b.tvArabic.text = sholat.arabicName
            b.tvTime.text = String.format("%02d:%02d", nt.hour, nt.minute)
            b.root.setOnClickListener { openTimePicker(index, nt) }
            b.tvTime.setOnClickListener { openTimePicker(index, nt) }
        }

        private fun openTimePicker(index: Int, nt: NotifTime) {
            TimePickerDialog(b.root.context, { _, h, m ->
                onTimeChanged(index, h, m)
                b.tvTime.text = String.format("%02d:%02d", h, m)
            }, nt.hour, nt.minute, true).show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemNotifSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)
    override fun getItemCount() = times.size
}