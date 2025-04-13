package com.example.of1.ui.positions

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface // Import Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.of1.R
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.OpenF1IntervalResponse
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.databinding.ItemPositionBinding
import com.example.of1.utils.AudioPlayerManager
import java.util.Locale
import java.util.concurrent.TimeUnit

// Data class defined in ViewModel file includes isOverallFastest flag

class PositionListAdapter : ListAdapter<UiPosition, PositionListAdapter.PositionViewHolder>(PositionDiffCallback()) {

    var onPositionClick: ((UiPosition) -> Unit)? = null
    var onPlayRadioClick: ((TeamRadio) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val binding = ItemPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PositionViewHolder(binding, onPositionClick, onPlayRadioClick)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PositionViewHolder(
        private val binding: ItemPositionBinding,
        private val onPositionClick: ((UiPosition) -> Unit)?,
        private val onPlayRadioClick: ((TeamRadio) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(uiPosition: UiPosition) {
            val position = uiPosition.position
            val driver = uiPosition.driver
            val latestRadio = uiPosition.latestRadio
            val intervalData = uiPosition.intervalData
            val fastestLapTime = uiPosition.fastestLapTime
            val isOverallFastest = uiPosition.isOverallFastest // Get the flag

            // --- Bind Position, Driver, Team Name, Country (Unchanged) ---
            binding.tvPosition.text = "P${position.position}"
            if (driver != null) {
                binding.tvDriverName.text = driver.fullName
                binding.tvDriverNumber.text = "#${position.driverNumber}"
                binding.tvTeamName.text = driver.teamName
                binding.tvCountryCode.text = driver.countryCode ?: ""
                Glide.with(binding.ivHeadshot.context)
                    .load(driver.headshotUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.no_headshot)
                    .fallback(R.drawable.no_headshot)
                    .into(binding.ivHeadshot)
                try {
                    binding.ivHeadshotCard.setCardBackgroundColor(("#" + driver.teamColour).toColorInt())
                } catch (e: Exception) {
                    Log.w("PositionVH", "Color parse error for ${driver.teamColour}: ${e.message}")
                    binding.ivHeadshotCard.setCardBackgroundColor(Color.LTGRAY)
                }
            } else {
                binding.tvDriverName.text = "Driver #${position.driverNumber}"
                binding.tvDriverNumber.text = "#${position.driverNumber}"
                binding.tvTeamName.text = "N/A"
                binding.tvCountryCode.text = ""
                binding.ivHeadshot.setImageResource(R.drawable.no_headshot)
                binding.ivHeadshotCard.setCardBackgroundColor(Color.LTGRAY)
            }

            // --- Bind Interval OR Fastest Lap (Conditional) ---
            // Reset styles and drawables first
            binding.tvInterval.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            binding.tvFastestLap.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            binding.tvFastestLap.setTypeface(null, Typeface.NORMAL) // Reset typeface
             binding.tvFastestLap.setTextColor(ContextCompat.getColor(binding.root.context, R.color.black)) // Reset color


            if (intervalData != null) {
                // Show Interval, Hide Fastest Lap
                binding.tvInterval.visibility = View.VISIBLE
                binding.tvFastestLap.visibility = View.GONE
                binding.tvInterval.text = formatInterval(intervalData.interval, "Int:")

                // --- Conditionally set stopwatch drawable on INTERVAL view ---
                if (isOverallFastest) {
                    binding.tvInterval.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, // start
                        0, // top
                        R.drawable.ic_stopwatch, // end
                        0  // bottom
                    )
                } else {
                    binding.tvInterval.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0) // Clear drawable
                }

            } else if (fastestLapTime != null && fastestLapTime > 0) {
                // Show Fastest Lap, Hide Interval
                binding.tvInterval.visibility = View.GONE
                binding.tvFastestLap.visibility = View.VISIBLE
                binding.tvFastestLap.text = formatLapTime(fastestLapTime)

                // --- Conditionally set stopwatch drawable on FASTEST LAP view ---
                if (isOverallFastest) {
                    binding.tvFastestLap.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, R.drawable.ic_stopwatch, 0
                    )
                    // Apply emphasis
                    binding.tvFastestLap.setTypeface(null, Typeface.BOLD_ITALIC) // Example emphasis
                    binding.tvFastestLap.setTextColor(ContextCompat.getColor(binding.root.context, com.google.android.material.R.color.design_default_color_primary)) // Example color
                } else {
                    binding.tvFastestLap.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0) // Clear drawable
                    // Reset styles handled at the start of the block
                }

            } else {
                // Hide Both if neither is available
                binding.tvInterval.visibility = View.GONE
                binding.tvFastestLap.visibility = View.GONE
            }

            // --- Bind Radio Button State (Unchanged) ---
            if (latestRadio != null) {
                 binding.btnPlayRadio.visibility = View.VISIBLE
                 binding.btnPlayRadio.tag = latestRadio.recordingUrl
                 binding.btnPlayRadio.setIconResource(
                    if (AudioPlayerManager.currentUrl == latestRadio.recordingUrl && AudioPlayerManager.isPlaying) R.drawable.ic_stop
                    else R.drawable.ic_play
                 )
                 binding.btnPlayRadio.setOnClickListener { onPlayRadioClick?.invoke(latestRadio) }
            } else {
                 binding.btnPlayRadio.visibility = View.GONE
                 binding.btnPlayRadio.setOnClickListener(null)
                 binding.btnPlayRadio.tag = null
            }

            // --- Set Item Click Listener (Unchanged) ---
            itemView.setOnClickListener { onPositionClick?.invoke(uiPosition) }
        }

        // --- Helper Functions (Unchanged) ---
        private fun formatInterval(value: String?, prefix: String): String {
             return when {
                 value == null -> "$prefix -"
                 value.contains("LAP", ignoreCase = true) -> "$prefix $value"
                 else -> try { String.format(Locale.US, "%s +%.3fs", prefix, value.toDouble()) }
                         catch (e: NumberFormatException) { "$prefix $value" }
             }
         }
        private fun formatLapTime(seconds: Double): String {
             if (seconds <= 0) return "-"
             val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
             val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
             return String.format(Locale.US, "%d:%06.3f", minutes, remainingSeconds)
         }
    }

    // --- DiffUtil Callback (isOverallFastest already included) ---
    class PositionDiffCallback : DiffUtil.ItemCallback<UiPosition>() {
        override fun areItemsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            return oldItem.position.driverNumber == newItem.position.driverNumber
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
             return oldItem == newItem // Data class comparison includes all fields
        }
    }
}