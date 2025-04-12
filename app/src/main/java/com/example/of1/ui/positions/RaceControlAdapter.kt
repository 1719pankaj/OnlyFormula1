package com.example.of1.ui.positions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.R
import com.example.of1.databinding.ItemRaceControlBinding

class RaceControlAdapter : ListAdapter<UiRaceControlMessage, RaceControlAdapter.RaceControlViewHolder>(RaceControlDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceControlViewHolder {
        val binding = ItemRaceControlBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RaceControlViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RaceControlViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RaceControlViewHolder(private val binding: ItemRaceControlBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UiRaceControlMessage) {
            binding.tvRcMessage.text = item.message

            // Set background color based on flag/category
            val context = binding.root.context
            val backgroundColor = when (item.flag?.uppercase()) {
                "YELLOW", "DOUBLE YELLOW" -> ContextCompat.getColor(context, R.color.flag_yellow) // Define colors in colors.xml
                "RED" -> ContextCompat.getColor(context, R.color.flag_red)
                "GREEN" -> ContextCompat.getColor(context, R.color.flag_green)
                "CHEQUERED" -> ContextCompat.getColor(context, R.color.flag_chequered) // Use a pattern? For now, gray.
                "BLACK AND WHITE" -> ContextCompat.getColor(context, R.color.flag_black_white) // Use a pattern? For now, light gray.
                else -> when (item.category?.uppercase()) {
                    "SAFETYCAR", "VIRTUALSAFETYCAR" -> ContextCompat.getColor(context, R.color.flag_yellow) // SC/VSC often yellow context
                    // Add more categories if needed
                    else -> ContextCompat.getColor(context, R.color.flag_default) // Default background
                }
            }
            binding.tvRcMessage.setBackgroundColor(backgroundColor)

            // Set text color based on background for contrast (simple example)
            val textColor = when (item.flag?.uppercase()) {
                "YELLOW", "DOUBLE YELLOW", "SAFETYCAR", "VIRTUALSAFETYCAR" -> Color.BLACK // Black text on Yellow
                "BLACK AND WHITE" -> Color.BLACK // Black text on White
                "CHEQUERED" -> Color.BLACK // Black text on Grey/Pattern
                else -> Color.WHITE // White text on Red, Green, Default Dark
            }
            binding.tvRcMessage.setTextColor(textColor)


        }
    }

    class RaceControlDiffCallback : DiffUtil.ItemCallback<UiRaceControlMessage>() {
        override fun areItemsTheSame(oldItem: UiRaceControlMessage, newItem: UiRaceControlMessage): Boolean {
            return oldItem.id == newItem.id // ID is the unique API date string
        }

        override fun areContentsTheSame(oldItem: UiRaceControlMessage, newItem: UiRaceControlMessage): Boolean {
            // Check relevant fields if needed, but ID should be sufficient if message content doesn't change
            return oldItem.message == newItem.message &&
                    oldItem.isPersistent == newItem.isPersistent &&
                    oldItem.displayUntilMillis == newItem.displayUntilMillis // Important for dismissal updates
        }
    }
}