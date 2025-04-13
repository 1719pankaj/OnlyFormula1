package com.example.of1.ui.yearselection // Renamed package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// REMOVED: import com.example.of1.data.model.Season // Remove import
import com.example.of1.databinding.ItemYearBinding // Renamed layout binding

// Renamed Adapter, change List type to String, simplify constructor
class YearListAdapter(
    private val onYearClick: (String) -> Unit
    // REMOVED: private val onInfoClick: (String) -> Unit // Removed info click
) : ListAdapter<String, YearListAdapter.YearViewHolder>(YearDiffCallback()) { // Change type to String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        val binding = ItemYearBinding.inflate( // Use renamed binding
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return YearViewHolder(binding)
    }

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class YearViewHolder(private val binding: ItemYearBinding) : // Use renamed binding
        RecyclerView.ViewHolder(binding.root) {

        fun bind(year: String) { // Accept String
            binding.tvYear.text = year // Bind string to tvYear (assume ID change in layout)

            binding.root.setOnClickListener {
                onYearClick(year) // Pass the year string
            }
        }
    }

    class YearDiffCallback : DiffUtil.ItemCallback<String>() { // Change type to String
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem // Simple string comparison
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}