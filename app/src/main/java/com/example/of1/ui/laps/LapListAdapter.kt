package com.example.of1.ui.laps

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.openf1.Lap
import com.example.of1.databinding.ItemLapBinding

class LapListAdapter : ListAdapter<Lap, LapListAdapter.LapViewHolder>(LapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val binding = ItemLapBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        val lap = getItem(position)
        holder.bind(lap)
    }

    inner class LapViewHolder(private val binding: ItemLapBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(lap: Lap) {
            binding.tvLapNumber.text = "Lap ${lap.lapNumber}"
            binding.tvDriverNumber.text = "#${lap.driverNumber}"

            if (lap.lapDuration != null) {
                binding.tvLapTime.text = formatLapTime(lap.lapDuration)
            } else {
                binding.tvLapTime.text = "In Pit / Out Lap"
            }

            binding.tvSector1.text = "S1: ${lap.durationSector1 ?: "N/A"}"
            binding.tvSector2.text = "S2: ${lap.durationSector2 ?: "N/A"}"
            binding.tvSector3.text = "S3: ${lap.durationSector3 ?: "N/A"}"

            // Set sector time colors based on segments
            setSectorColor(binding.tvSector1, lap.segmentsSector1)
            setSectorColor(binding.tvSector2, lap.segmentsSector2)
            setSectorColor(binding.tvSector3, lap.segmentsSector3)


            binding.tvI1Speed.text = "I1: ${lap.i1Speed ?: "N/A"}"
            binding.tvI2Speed.text = "I2: ${lap.i2Speed ?: "N/A"}"
            binding.tvStSpeed.text = "ST: ${lap.stSpeed ?: "N/A"}"
        }

        private fun formatLapTime(seconds: Double): String {
            val minutes = (seconds / 60).toInt()
            val remainingSeconds = seconds % 60
            return String.format("%d:%06.3f", minutes, remainingSeconds)
        }

        private fun setSectorColor(textView: TextView, segments: List<Int>?) {
            if (segments == null) {
                textView.setBackgroundColor(Color.LTGRAY) // Default color
                return
            }

            // Use the *last* segment value to determine the color (most representative)
            when (segments.lastOrNull()) {
                2048 -> textView.setBackgroundColor(Color.YELLOW) // Yellow
                2049 -> textView.setBackgroundColor(Color.GREEN)  // Green
                2051 -> textView.setBackgroundColor(Color.MAGENTA) // Purple (using MAGENTA for now)
                0, 2050, 2052, 2068 -> textView.setBackgroundColor(Color.LTGRAY) // Not available, or unknown
                2064 -> textView.setBackgroundColor(Color.BLUE) // Pitlane (example color)
                else -> textView.setBackgroundColor(Color.LTGRAY) // Default
            }
        }
    }


    class LapDiffCallback : DiffUtil.ItemCallback<Lap>() {
        override fun areItemsTheSame(oldItem: Lap, newItem: Lap): Boolean {
            return oldItem.lapNumber == newItem.lapNumber && oldItem.driverNumber == newItem.driverNumber
        }

        override fun areContentsTheSame(oldItem: Lap, newItem: Lap): Boolean {
            return oldItem == newItem
        }
    }
}