package com.example.of1.ui.positions

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Position
import com.example.of1.databinding.ItemPositionBinding

class PositionListAdapter : ListAdapter<Position, PositionListAdapter.PositionViewHolder>(PositionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val binding = ItemPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PositionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val positionData = getItem(position)
        holder.bind(positionData)
    }

    class PositionViewHolder(private val binding: ItemPositionBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(position: Position) {
            binding.tvDriverNumber.text = "#${position.driverNumber}"
            binding.tvPosition.text = "P${position.position}"
            // Add other bindings as needed (driver name, etc. - you'll need to join with other data)
        }
    }

    class PositionDiffCallback : DiffUtil.ItemCallback<Position>() {
        override fun areItemsTheSame(oldItem: Position, newItem: Position): Boolean {
            // Compare based on driver number and the latest date.  This is crucial for live updates.
            return oldItem.driverNumber == newItem.driverNumber && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Position, newItem: Position): Boolean {
            return oldItem == newItem
        }
    }
}