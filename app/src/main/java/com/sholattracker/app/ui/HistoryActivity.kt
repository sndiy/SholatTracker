package com.sholattracker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sholattracker.app.R
import com.sholattracker.app.data.DayRecord
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import com.sholattracker.app.databinding.ActivityHistoryBinding
import com.sholattracker.app.databinding.ItemHistoryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repo: SholatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Riwayat Sholat"

        repo = SholatRepository(this)

        val adapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        val records = repo.getAllRecords()
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            adapter.submitList(records)
        }

        // Overall stats
        val totalDays = records.size
        val completeDays = records.count { it.isComplete }
        val totalSholat = records.sumOf { it.count }
        binding.tvStats.text = "$completeDays hari lengkap dari $totalDays hari · $totalSholat sholat total"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }
}

class HistoryAdapter : ListAdapter<DayRecord, HistoryAdapter.VH>(Diff()) {

    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("id", "ID"))

    inner class VH(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(record: DayRecord) {
            val date = LocalDate.parse(record.date)
            b.tvDate.text = date.format(dateFmt)
            b.tvCount.text = "${record.count}/5"

            b.tvCount.setTextColor(
                b.root.context.getColor(
                    if (record.isComplete) R.color.gold else R.color.text_secondary
                )
            )

            // Dot indicators per sholat
            val dots = listOf(b.dot1, b.dot2, b.dot3, b.dot4, b.dot5)
            SHOLAT_LIST.forEachIndexed { i, sholat ->
                val filled = record.completed.contains(sholat.id)
                dots[i].setBackgroundResource(
                    if (filled) R.drawable.dot_filled else R.drawable.dot_empty
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<DayRecord>() {
        override fun areItemsTheSame(a: DayRecord, b: DayRecord) = a.date == b.date
        override fun areContentsTheSame(a: DayRecord, b: DayRecord) = a == b
    }
}
