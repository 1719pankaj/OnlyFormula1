package com.example.of1.ui.positions

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.of1.R
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.data.model.openf1.OpenF1IntervalResponse // Import Interval model
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.databinding.ItemPositionBinding
import com.example.of1.utils.AudioPlayerManager
import androidx.core.graphics.toColorInt
import java.util.Locale

data class UiPosition(
    val position: Position,
    val driver: OF1Driver?,
    val latestRadio: TeamRadio? = null,
    val intervalData: OpenF1IntervalResponse? = null // Already added
)

class PositionListAdapter : ListAdapter<UiPosition, PositionListAdapter.PositionViewHolder>(PositionDiffCallback()) {

    var onPositionClick: ((UiPosition) -> Unit)? = null
    var onPlayRadioClick: ((TeamRadio) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val binding = ItemPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PositionViewHolder(binding, onPositionClick, onPlayRadioClick)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val uiPosition = getItem(position)
        holder.bind(uiPosition)
    }

    class PositionViewHolder(
        val binding: ItemPositionBinding,
        private val onPositionClick: ((UiPosition) -> Unit)?,
        private val onPlayRadioClick: ((TeamRadio) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(uiPosition: UiPosition) {
            // ... (binding for position, driver, headshot, name, team, country) ...
            val position = uiPosition.position
            val driver = uiPosition.driver
            val latestRadio = uiPosition.latestRadio
            val intervalData = uiPosition.intervalData

            // --- Bind Position and Driver Data ---
            binding.tvDriverNumber.text = "#${position.driverNumber}"
            binding.tvPosition.text = "P${position.position}"
            if (driver != null) {
                binding.tvDriverName.text = driver.fullName
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
                    binding.ivHeadshotCard.setCardBackgroundColor(Color.LTGRAY) // Fallback color
                }
            } else {
                binding.tvDriverName.text = "Unknown Driver"
                binding.tvTeamName.text = ""
                binding.tvCountryCode.text = ""
                binding.ivHeadshot.setImageResource(R.drawable.no_headshot)
                binding.ivHeadshotCard.setCardBackgroundColor(Color.LTGRAY)
            }


            // --- Bind Interval Data ---
            if (intervalData != null) {
                // Interval to car ahead
                binding.tvInterval.visibility = View.VISIBLE
                binding.tvInterval.text = formatInterval(intervalData.interval, "Int:") // Use prefix "Int:"

                // Gap to Leader - REMOVED BINDING
                // binding.tvGapToLeader.visibility = View.VISIBLE // REMOVED
                // binding.tvGapToLeader.text = formatInterval(intervalData.gapToLeader, "Lead:") // REMOVED

            } else {
                // Hide interval if no data
                binding.tvInterval.visibility = View.GONE
                // binding.tvGapToLeader.visibility = View.GONE // REMOVED (TextView doesn't exist)
            }

            // ... (binding for Radio Button and itemView click listener remain the same) ...
            itemView.setOnClickListener {
                onPositionClick?.invoke(uiPosition)
            }

            if (latestRadio != null) {
                binding.btnPlayRadio.visibility = View.VISIBLE
                binding.btnPlayRadio.tag = latestRadio.recordingUrl

                if (AudioPlayerManager.currentUrl == latestRadio.recordingUrl && AudioPlayerManager.isPlaying) {
                    binding.btnPlayRadio.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
                } else {
                    binding.btnPlayRadio.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0)
                }

                binding.btnPlayRadio.setOnClickListener {
                    onPlayRadioClick?.invoke(latestRadio)
                }
            } else {
                binding.btnPlayRadio.visibility = View.GONE
                binding.btnPlayRadio.setOnClickListener(null)
                binding.btnPlayRadio.tag = null
            }
        }

        // Helper function to format interval/gap strings
        // Modified: Removed handling specific to "Lead:" prefix for leader case
        private fun formatInterval(value: String?, prefix: String): String {
            return when {
                // If interval is null (usually only for leader), show prefix and dash.
                value == null -> "$prefix -"
                value.contains("LAP", ignoreCase = true) -> "$prefix $value" // Show "+1 LAP" text directly
                else -> {
                    try {
                        val seconds = value.toDouble()
                        // Show + sign and 3 decimal places for positive intervals
                        String.format(Locale.US, "%s +%.3fs", prefix, seconds)

                    } catch (e: NumberFormatException) {
                        Log.w("PositionVH", "Unexpected interval format: $value")
                        "$prefix $value" // Show raw string if parsing fails
                    }
                }
            }
        }
    }

    // ... (PositionDiffCallback remains the same) ...
    class PositionDiffCallback : DiffUtil.ItemCallback<UiPosition>() {
        override fun areItemsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            return oldItem.position.driverNumber == newItem.position.driverNumber
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            return oldItem.position == newItem.position &&
                    oldItem.driver == newItem.driver &&
                    oldItem.latestRadio?.recordingUrl == newItem.latestRadio?.recordingUrl &&
                    oldItem.intervalData == newItem.intervalData // Still compare the whole object
        }
    }
}