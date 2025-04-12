package com.example.of1.ui.laps

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.R
import com.example.of1.data.model.openf1.Lap
import com.example.of1.data.model.openf1.PitStop
import com.example.of1.databinding.ItemLapBinding

class LapListAdapter : ListAdapter<Lap, LapListAdapter.LapViewHolder>(LapDiffCallback()) {

    var onCarDataClick: ((Lap) -> Unit)? = null // Click listener, no change needed.

    private var pitStops: List<PitStop> = emptyList()


    fun submitPitStops(newPitStops: List<PitStop>) {
        pitStops = newPitStops
        notifyDataSetChanged() // For simplicity. Use DiffUtil in a real app!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val binding = ItemLapBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LapViewHolder(binding)
    }


    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        val lap = getItem(position)
        // Find the pit stop data for this lap, if it exists
        val pitStop = pitStops.firstOrNull { it.lapNumber == lap.lapNumber && it.driverNumber == lap.driverNumber}
        holder.bind(lap, pitStop) // Pass both Lap and PitStop (which can be null)
        holder.binding.btnCarData.setOnClickListener {
            onCarDataClick?.invoke(lap) // Invoke the listener, passing the Lap object
        }
    }

    inner class LapViewHolder(val binding: ItemLapBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(lap: Lap, pitStop: PitStop?) {
            binding.tvLapNumber.text = "Lap ${lap.lapNumber}"
            binding.tvDriverNumber.text = "#${lap.driverNumber}"

            if (lap.lapDuration != null) {
                binding.tvLapTime.text = formatLapTime(lap.lapDuration)
            } else {
                binding.tvLapTime.text = "In Pit / Out Lap"
            }

            binding.tvSector1.text = "${if(lap.durationSector1 != null) "${lap.durationSector1}s" else "N/A"}"
            binding.tvSector2.text = "${if(lap.durationSector2 != null) "${lap.durationSector2}s" else "N/A"}"
            binding.tvSector3.text = "${if(lap.durationSector3 != null) "${lap.durationSector3}s" else "N/A"}"

            // Set sector time colors based on segments
            setSectorColor(binding.tvSector1, lap.segmentsSector1)
            setSectorColor(binding.tvSector2, lap.segmentsSector2)
            setSectorColor(binding.tvSector3, lap.segmentsSector3)


            binding.tvI1Speed.text = "${if (lap.i1Speed != null) "${lap.i1Speed} Km/h" else "N/A"}"
            binding.tvI2Speed.text = "${if (lap.i2Speed != null) "${lap.i2Speed} Km/h" else "N/A"}"
            binding.tvStSpeed.text = "${if (lap.stSpeed != null) "${lap.stSpeed} Km/h" else "N/A"}"

            // Pit stop data
            if (pitStop != null) {
                binding.tvPitTime.text = "Pit: ${pitStop.pitDuration}s"
                binding.tvPitTime.visibility = View.VISIBLE
                binding.ivPitStopIcon.visibility = View.VISIBLE // Show icon
            } else {
                binding.tvPitTime.visibility = View.GONE // Hide if no pit stop
                binding.ivPitStopIcon.visibility = View.GONE // Hide icon
            }
        }

        private fun formatLapTime(seconds: Double): String {
            val minutes = (seconds / 60).toInt()
            val remainingSeconds = seconds % 60
            return String.format("%d:%06.3f", minutes, remainingSeconds)
        }

        private fun setSectorColor(textView: TextView, segments: List<Int>?) {
            // Clear any existing background first
            textView.background = null

            if (segments == null) {
                return
            }

            // Determine the color based on the last segment value
            val underlineColor = when (segments.lastOrNull()) {
                2048 -> Color.YELLOW  // Yellow
                2049 -> Color.GREEN   // Green
                2051 -> Color.MAGENTA // Purple
                0, 2050, 2052, 2068 -> Color.LTGRAY // Not available, or unknown
                2064 -> Color.BLUE    // Pitlane
                else -> Color.LTGRAY  // Default
            }

            // Create a custom drawable that only draws the bottom line
            val underlineDrawable = object : Drawable() {
                private val paint = Paint().apply {
                    color = underlineColor
                    strokeWidth = 10f  // Thickness of the underline
                    style = Paint.Style.STROKE
                }

                override fun draw(canvas: Canvas) {
                    // Draw only the bottom line
                    val bottom = bounds.bottom.toFloat() - 1
                    canvas.drawLine(
                        bounds.left.toFloat(),
                        bottom,
                        bounds.right.toFloat(),
                        bottom,
                        paint
                    )
                }

                override fun setAlpha(alpha: Int) {
                    paint.alpha = alpha
                }

                override fun setColorFilter(colorFilter: ColorFilter?) {
                    paint.colorFilter = colorFilter
                }

                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }

            textView.background = underlineDrawable
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