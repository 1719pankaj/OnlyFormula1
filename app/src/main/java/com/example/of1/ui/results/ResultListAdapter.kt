package com.example.of1.ui.results

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Result
import com.example.of1.databinding.ItemResultBinding

class ResultListAdapter : ListAdapter<Result, ResultListAdapter.ResultViewHolder>(ResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = getItem(position)
        holder.bind(result)
    }

    class ResultViewHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: Result) {
            binding.tvDriverName.text = "${result.driver.givenName} ${result.driver.familyName}"
            binding.tvConstructorName.text = result.constructor.name
            binding.tvDriverNumber.text = result.driverNumber
            binding.tvPosition.text = result.position
            binding.tvFastestLapTime.text = result.fastestLap?.time?.time ?: "N/A" // Handle potential null
            binding.tvPoints.text = result.points
        }
    }
    class ResultDiffCallback : DiffUtil.ItemCallback<Result>() {
        override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
            // Since we don't have a unique ID for results, compare relevant fields
            return oldItem.driver.driverId == newItem.driver.driverId && oldItem.position == newItem.position
        }

        override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem == newItem // Compare all fields
        }
    }
}