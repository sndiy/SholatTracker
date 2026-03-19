package com.sholattracker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

        val adapter = HistoryAdapter(repo)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        val records = repo.getAllRecords()
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            adapter.submitList(records)
        }

        val totalDays = records.size
        val completeDays = records.count { it.isComplete }
        val totalSholat = records.sumOf { it.count }
        binding.tvStats.text = "$completeDays hari lengkap dari $totalDays hari · $totalSholat sholat total"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

class HistoryAdapter(
    private val repo: SholatRepository
) : ListAdapter<DayRecord, HistoryAdapter.VH>(Diff()) {

    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("id", "ID"))
    private val expandedDates = mutableSetOf<String>()

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

            val dots = listOf(b.dot1, b.dot2, b.dot3, b.dot4, b.dot5)
            SHOLAT_LIST.forEachIndexed { i, sholat ->
                dots[i].setBackgroundResource(
                    if (record.completed.contains(sholat.id)) R.drawable.dot_filled
                    else R.drawable.dot_empty
                )
            }

            // Log container
            val logContainer = b.root.findViewById<LinearLayout>(R.id.logContainer)
            val isExpanded = expandedDates.contains(record.date)
            logContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                renderLogs(logContainer, record)
            }

            b.root.setOnClickListener {
                if (expandedDates.contains(record.date)) {
                    expandedDates.remove(record.date)
                    logContainer.visibility = View.GONE
                } else {
                    expandedDates.add(record.date)
                    logContainer.visibility = View.VISIBLE
                    renderLogs(logContainer, record)
                }
            }
        }

        private fun renderLogs(container: LinearLayout, record: DayRecord) {
            container.removeAllViews()
            val ctx = container.context
            val today = LocalDate.now().toString()
            val isPastDay = record.date < today

            SHOLAT_LIST.forEach { sholat ->
                val isChecked = record.completed.contains(sholat.id)
                val timestamp = record.timestamps[sholat.id]

                when {
                    isChecked -> {
                        // Pernah diceklis — tampilkan dengan jam
                        addLogRow(container, ctx, sholat.name, timestamp ?: "", true)
                    }
                    isPastDay -> {
                        // Hari sudah lewat dan tidak diceklis — missed tanpa jam
                        addLogRow(container, ctx, sholat.name, null, false)
                    }
                    // Hari ini belum diceklis — tidak ditampilkan
                }
            }
        }

        private fun addLogRow(container: LinearLayout, ctx: android.content.Context, sholatName: String, time: String?, isChecked: Boolean) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 5, 0, 5)
            }

            val icon = TextView(ctx).apply {
                text = if (isChecked) "✓" else "✗"
                setTextColor(ctx.getColor(if (isChecked) R.color.green else R.color.red))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(48, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val tvName = TextView(ctx).apply {
                text = sholatName
                setTextColor(ctx.getColor(if (isChecked) R.color.text_primary else R.color.text_muted2))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTime = TextView(ctx).apply {
                text = if (!time.isNullOrEmpty()) time else ""
                setTextColor(ctx.getColor(R.color.text_muted))
                textSize = 11f
                gravity = android.view.Gravity.END
            }

            row.addView(icon)
            row.addView(tvName)
            row.addView(tvTime)
            container.addView(row)
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