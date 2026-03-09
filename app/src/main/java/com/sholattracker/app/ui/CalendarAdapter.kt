package com.sholattracker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sholattracker.app.R
import java.time.LocalDate

enum class DayStatus { NONE, MISSED, PARTIAL, COMPLETE, FUTURE, EMPTY }

data class CalendarDay(
    val date: LocalDate?,
    val status: DayStatus,
    val isToday: Boolean = false
)

class CalendarAdapter(
    private val onDayClick: (CalendarDay, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    private var days = listOf<CalendarDay>()

    fun submitDays(list: List<CalendarDay>) {
        days = list
        notifyDataSetChanged()
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvCalDay)
        val dot: View = view.findViewById(R.id.calDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = days[position]
        val ctx = holder.view.context

        if (item.date == null || item.status == DayStatus.EMPTY) {
            holder.tvDay.text = ""
            holder.dot.visibility = View.INVISIBLE
            holder.view.background = null
            holder.view.setOnClickListener(null)
            return
        }

        holder.tvDay.text = item.date.dayOfMonth.toString()
        holder.dot.visibility = View.VISIBLE

        when (item.status) {
            DayStatus.COMPLETE -> {
                holder.dot.setBackgroundResource(R.drawable.cal_dot_green)
                holder.tvDay.setTextColor(ctx.getColor(R.color.green_text))
                holder.view.setBackgroundResource(
                    if (item.isToday) R.drawable.cal_today_green else R.drawable.cal_bg_green
                )
            }
            DayStatus.PARTIAL -> {
                holder.dot.setBackgroundResource(R.drawable.cal_dot_gray)
                holder.tvDay.setTextColor(ctx.getColor(R.color.text_muted))
                holder.view.setBackgroundResource(
                    if (item.isToday) R.drawable.cal_today_gray else R.drawable.cal_bg_gray
                )
            }
            DayStatus.MISSED -> {
                holder.dot.setBackgroundResource(R.drawable.cal_dot_red)
                holder.tvDay.setTextColor(ctx.getColor(R.color.red))
                holder.view.setBackgroundResource(
                    if (item.isToday) R.drawable.cal_today_red else R.drawable.cal_bg_red
                )
            }
            DayStatus.NONE, DayStatus.FUTURE -> {
                holder.dot.visibility = View.INVISIBLE
                holder.tvDay.setTextColor(
                    ctx.getColor(if (item.isToday) R.color.gold else R.color.text_muted2)
                )
                holder.view.setBackgroundResource(
                    if (item.isToday) R.drawable.cal_today_outline else 0
                )
            }
            DayStatus.EMPTY -> {
                holder.tvDay.text = ""
                holder.dot.visibility = View.INVISIBLE
                holder.view.background = null
            }
        }

        if (item.status != DayStatus.FUTURE && item.status != DayStatus.EMPTY && item.status != DayStatus.NONE) {
            holder.view.setOnClickListener { onDayClick(item, holder.view) }
        } else {
            holder.view.setOnClickListener(null)
        }
    }
}