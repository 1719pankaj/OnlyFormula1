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
import com.example.of1.data.model.openf1.TeamRadio // Make sure TeamRadio is imported
import com.example.of1.databinding.ItemPositionBinding
import com.example.of1.utils.AudioPlayerManager
import androidx.core.graphics.toColorInt

// Data class remains the same
data class UiPosition(
    val position: Position,
    val driver: OF1Driver?,
    val latestRadio: TeamRadio? = null
)

// ui/positions/PositionListAdapter.kt
class PositionListAdapter : ListAdapter<UiPosition, PositionListAdapter.PositionViewHolder>(PositionDiffCallback()) {

    var onPositionClick: ((UiPosition) -> Unit)? = null
    var onPlayRadioClick: ((TeamRadio) -> Unit)? = null
    // No isLive property needed here anymore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val binding = ItemPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Pass BOTH listeners to the ViewHolder
        return PositionViewHolder(binding, onPositionClick, onPlayRadioClick)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val uiPosition = getItem(position)
        holder.bind(uiPosition) // Pass uiPosition
        // Click listeners are now set inside bind
    }

    // ViewHolder now receives the listener but uses it more simply

    class PositionViewHolder(
        val binding: ItemPositionBinding,
        private val onPositionClick: ((UiPosition) -> Unit)?,
        private val onPlayRadioClick: ((TeamRadio) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(uiPosition: UiPosition) {
            val position = uiPosition.position
            val driver = uiPosition.driver
            val latestRadio = uiPosition.latestRadio

            // --- Bind Position and Driver Data ---
            binding.tvDriverNumber.text = "#${position.driverNumber}"
            binding.tvPosition.text = "P${position.position}"
            // ... (rest of binding logic for driver details) ...
            if (driver != null) {
                binding.tvDriverName.text = driver.fullName
                binding.tvTeamName.text = driver.teamName
                binding.tvCountryCode.text = driver.countryCode ?: ""
                Glide.with(binding.ivHeadshot.context)
                    .load(driver.headshotUrl) // Pass the potentially null URL
                    .placeholder(R.drawable.ic_launcher_background) // Your general placeholder
                    .error(R.drawable.no_headshot) // Use your specific fallback drawable
                    .fallback(R.drawable.no_headshot) // Use fallback if URL is null
                    .into(binding.ivHeadshot)
                try {
                    binding.tvTeamColor.setBackgroundColor("#${driver.teamColour}".toColorInt())
                } catch (e: Exception) { Log.e("PosVH", "Color parse error") }
            } else {
                binding.ivHeadshot.setImageResource(R.drawable.no_headshot)
            }


            // --- Set Item Click Listener for Navigation ---
            itemView.setOnClickListener { // Use itemView
                onPositionClick?.invoke(uiPosition)
            }

            // --- Bind Radio Button State ---
            if (latestRadio != null) {
                binding.btnPlayRadio.visibility = View.VISIBLE
                binding.btnPlayRadio.tag = latestRadio.recordingUrl

                // Set icon based on AudioPlayerManager state
                if (AudioPlayerManager.currentUrl == latestRadio.recordingUrl && AudioPlayerManager.isPlaying) {
                    binding.btnPlayRadio.setImageResource(R.drawable.ic_stop)
                } else {
                    binding.btnPlayRadio.setImageResource(R.drawable.ic_play)
                }

                // Set radio button click listener
                binding.btnPlayRadio.setOnClickListener {
                    onPlayRadioClick?.invoke(latestRadio)
                    // NOTE: We rely on observePlaybackState in Fragment to update the icon
                }
            } else {
                binding.btnPlayRadio.visibility = View.GONE
                binding.btnPlayRadio.setOnClickListener(null) // Clear listener
                binding.btnPlayRadio.tag = null
            }
        }
    }

    // DiffCallback remains the same
    class PositionDiffCallback : DiffUtil.ItemCallback<UiPosition>() {
        override fun areItemsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            return oldItem.position.driverNumber == newItem.position.driverNumber
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            return oldItem == newItem
        }
    }
}