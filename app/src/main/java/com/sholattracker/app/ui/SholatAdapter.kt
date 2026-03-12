package com.sholattracker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sholattracker.app.R
import com.sholattracker.app.data.Sholat
import com.sholattracker.app.databinding.ItemSholatBinding

data class SholatItem(
    val sholat: Sholat,
    val isChecked: Boolean,
    val isNext: Boolean,
    val displayTime: String = sholat.defaultTime
)

class SholatAdapter(
    private val onToggle: (String) -> Unit
) : ListAdapter<SholatItem, SholatAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemSholatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SholatItem) {
            binding.tvName.text = item.sholat.name
            binding.tvArabic.text = item.sholat.arabicName
            binding.tvTime.text = item.displayTime

            // Checkbox state
            binding.cbSholat.isChecked = item.isChecked

            // Next badge
            binding.chipNext.visibility = if (item.isNext && !item.isChecked)
                View.VISIBLE else View.GONE

            // Row style
            val ctx = binding.root.context
            when {
                item.isChecked -> {
                    binding.root.setCardBackgroundColor(ctx.getColor(R.color.green_dim))
                    binding.root.strokeColor = ctx.getColor(R.color.green_border)
                    binding.tvName.setTextColor(ctx.getColor(R.color.green_text))
                }
                item.isNext -> {
                    binding.root.setCardBackgroundColor(ctx.getColor(R.color.surface3))
                    binding.root.strokeColor = ctx.getColor(R.color.gold_border)
                    binding.tvName.setTextColor(ctx.getColor(R.color.text_primary))
                }
                else -> {
                    binding.root.setCardBackgroundColor(ctx.getColor(R.color.surface2))
                    binding.root.strokeColor = ctx.getColor(R.color.border)
                    binding.tvName.setTextColor(ctx.getColor(R.color.text_primary))
                }
            }

            binding.root.setOnClickListener { onToggle(item.sholat.id) }
            binding.cbSholat.setOnClickListener { onToggle(item.sholat.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSholatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SholatItem>() {
        override fun areItemsTheSame(old: SholatItem, new: SholatItem) =
            old.sholat.id == new.sholat.id
        override fun areContentsTheSame(old: SholatItem, new: SholatItem) = old == new
    }
}