package com.example.of1.ui.positions

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.of1.R
import com.example.of1.data.model.Position
import com.example.of1.data.model.openf1.OF1Driver
import com.example.of1.databinding.ItemPositionBinding

// Create a data class to hold the combined data for the UI
data class UiPosition(
    val position: Position,
    val driver: OF1Driver?
)

class PositionListAdapter : ListAdapter<UiPosition, PositionListAdapter.PositionViewHolder>(PositionDiffCallback()) {

    private var drivers: List<OF1Driver> = emptyList()
    var onPositionClick: ((UiPosition) -> Unit)? = null // Click listener

    // Update the drivers.  This will trigger a diff calculation.
    fun setDrivers(newDrivers: List<OF1Driver>) {
        drivers = newDrivers
        updateCombinedData() // Call a helper function
    }

    // Update the positions (from the base ListAdapter)
    fun setPositions(newPositions: List<Position>) {
        submitList(newPositions.map { position ->
            val driver = drivers.firstOrNull { it.driverNumber == position.driverNumber }
            UiPosition(position, driver)
        })
    }

    // Helper function to create the combined data list
    private fun updateCombinedData() {
        val currentPositions = currentList.map { it.position } // Get current *positions*
        submitList(currentPositions.map { position -> //Use current positions
            val driver = drivers.firstOrNull { it.driverNumber == position.driverNumber }
            UiPosition(position, driver)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val binding = ItemPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PositionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val uiPosition = getItem(position)
        holder.bind(uiPosition)
        holder.itemView.setOnClickListener { onPositionClick?.invoke(uiPosition) } // Set click listener
    }

    class PositionViewHolder(private val binding: ItemPositionBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(uiPosition: UiPosition) { // Receives the UiPosition object
            val position = uiPosition.position
            val driver = uiPosition.driver

            binding.tvDriverNumber.text = "#${position.driverNumber}"
            binding.tvPosition.text = "P${position.position}"

            if (driver != null) {
                binding.tvDriverName.text = driver.fullName
                binding.tvTeamName.text = driver.teamName
                binding.tvCountryCode.text = driver.countryCode ?: ""

                Glide.with(binding.ivHeadshot.context)
                    .load(driver.headshotUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivHeadshot)

                try {
                    val color = Color.parseColor("#${driver.teamColour}")
                    binding.tvTeamColor.setBackgroundColor(color)
                } catch (e: IllegalArgumentException) {
                    Log.e("PositionListAdapter", "Invalid color: ${driver.teamColour}", e)
                }

            } else {
                binding.tvDriverName.text = ""
                binding.tvTeamName.text = ""
                binding.tvCountryCode.text = ""
                binding.ivHeadshot.setImageDrawable(null)
                binding.tvTeamColor.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    class PositionDiffCallback : DiffUtil.ItemCallback<UiPosition>() {
        override fun areItemsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            // Check if the *position* is the same AND if the *driver* is the same
            return oldItem.position.driverNumber == newItem.position.driverNumber &&
                    oldItem.position.date == newItem.position.date && //Crucial for live data
                    oldItem.driver?.driverNumber == newItem.driver?.driverNumber
        }

        override fun areContentsTheSame(oldItem: UiPosition, newItem: UiPosition): Boolean {
            // Compare the contents of both position and driver
            return oldItem.position == newItem.position && oldItem.driver == newItem.driver
        }
    }
}