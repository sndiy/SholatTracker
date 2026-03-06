package com.sholattracker.app.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sholattracker.app.data.NotifTime
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.databinding.ActivityNotifSettingsBinding
import com.sholattracker.app.databinding.ItemNotifSettingBinding
import com.sholattracker.app.notification.NotificationScheduler

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotifSettingsBinding
    private lateinit var repo: SholatRepository
    private lateinit var scheduler: NotificationScheduler
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

        val adapter = NotifSettingAdapter(currentTimes) { index, hour, minute ->
            currentTimes[index] = currentTimes[index].copy(hour = hour, minute = minute)
        }

        binding.rvNotifSettings.layoutManager = LinearLayoutManager(this)
        binding.rvNotifSettings.adapter = adapter

        binding.btnSave.setOnClickListener {
            currentTimes.forEachIndexed { i, nt ->
                repo.saveNotifTime(nt.sholatId, nt.hour, nt.minute, nt.enabled)
            }
            scheduler.scheduleAll(currentTimes)
            Toast.makeText(this, "✓ Pengingat disimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

class NotifSettingAdapter(
    private val times: List<NotifTime>,
    private val onTimeChanged: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<NotifSettingAdapter.VH>() {

    inner class VH(private val b: ItemNotifSettingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(index: Int) {
            val nt = times[index]
            val sholat = SHOLAT_LIST.first { it.id == nt.sholatId }
            b.tvSholatName.text = sholat.name
            b.tvArabic.text = sholat.arabicName
            b.tvTime.text = String.format("%02d:%02d", nt.hour, nt.minute)

            b.root.setOnClickListener {
                TimePickerDialog(b.root.context, { _, h, m ->
                    onTimeChanged(index, h, m)
                    b.tvTime.text = String.format("%02d:%02d", h, m)
                }, nt.hour, nt.minute, true).show()
            }

            b.tvTime.setOnClickListener {
                TimePickerDialog(b.root.context, { _, h, m ->
                    onTimeChanged(index, h, m)
                    b.tvTime.text = String.format("%02d:%02d", h, m)
                }, nt.hour, nt.minute, true).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemNotifSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(position)
    override fun getItemCount() = times.size
}
