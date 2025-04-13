package com.example.of1.ui.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log // Make sure Log is imported
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.R
import com.example.of1.data.model.Practice
import com.example.of1.data.model.Race
import com.example.of1.databinding.ItemRaceMainBinding
import com.example.of1.databinding.ItemSessionDetailBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit


class MainRaceListAdapter(
    private val onItemToggled: (round: String) -> Unit,
    private val onSessionClicked: (race: Race, sessionName: String, sessionDate: String, sessionTime: String?) -> Unit
) : ListAdapter<Race, MainRaceListAdapter.RaceViewHolder>(RaceDiffCallback()) {

    val expandedItemRounds = mutableSetOf<String>()
    var targetRedRound: String? = null // Track which round should be RED

    fun setExpanded(round: String, isExpanded: Boolean) {
        val changed = if (isExpanded) {
            expandedItemRounds.clear() // Collapse others if needed, or allow multiple
            expandedItemRounds.add(round)
        } else {
            expandedItemRounds.remove(round)
        }
        val position = currentList.indexOfFirst { it.round == round }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    // *** RENAMED FUNCTION ***
    // Method to set the specific round that should have the red indicator
    fun updateTargetRedRound(round: String?) {
        if (targetRedRound != round) {
            val oldTargetPos = currentList.indexOfFirst { it.round == targetRedRound }
            val newTargetPos = currentList.indexOfFirst { it.round == round }
            targetRedRound = round // Update the internal property
            // Notify previous and new targets to update their indicator color
            if (oldTargetPos != -1) notifyItemChanged(oldTargetPos)
            if (newTargetPos != -1) notifyItemChanged(newTargetPos)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val binding = ItemRaceMainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val race = getItem(position)
        val isExpanded = expandedItemRounds.contains(race.round)
        val isRedTarget = race.round == targetRedRound // Check if this item should be red
        holder.bind(race, isExpanded, isRedTarget)
    }

    // ViewHolder class remains the same internally
    inner class RaceViewHolder(private val binding: ItemRaceMainBinding) : RecyclerView.ViewHolder(binding.root) {

        private val raceDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val sessionTimeFormat = SimpleDateFormat("HH:mm:ss'Z'", Locale.US)
        private val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time // Get current UTC time once

        init {
            raceDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            sessionTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
        }

        @SuppressLint("SetTextI18n")
        fun bind(race: Race, isExpanded: Boolean, isRedTarget: Boolean) {
            binding.tvRound.text = "Round ${race.round}"
            binding.tvDate.text = formatDate(race.date)
            binding.tvRaceNameMain.text = race.raceName
            binding.tvCircuitNameMain.text = "${race.circuit.circuitName}, ${race.circuit.location.country}"

            val status = determineRaceStatus(race)
            setIndicatorBarColor(status, isRedTarget) // Pass the isRedTarget flag

            binding.clickableArea.setOnClickListener {
                onItemToggled(race.round)
            }

            binding.ivExpandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            binding.layoutSessionDetails.isVisible = isExpanded
            if (isExpanded) {
                populateSessionDetails(race)
            } else {
                binding.layoutSessionDetails.removeAllViews()
            }
        }

        private fun setIndicatorBarColor(status: RaceStatus, isRedTarget: Boolean) {
            val context = binding.root.context
            val colorRes = when {
                // Prioritize the RED target flag
                isRedTarget -> R.color.race_status_indicator_live_or_next
                // Otherwise, use status for other colors (optional)
                status == RaceStatus.PAST -> R.color.race_status_indicator_past
                status == RaceStatus.FUTURE -> R.color.race_status_indicator_future
                // Default to transparent if not red, past, or future
                else -> R.color.race_status_indicator_default
            }
            binding.statusIndicatorBar.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }


        private fun determineRaceStatus(race: Race): RaceStatus {
            val raceDateStr = race.date
            val earliestSessionDateStr = listOfNotNull(
                race.firstPractice?.date, race.secondPractice?.date, race.thirdPractice?.date,
                race.qualifying?.date, race.sprint?.date, race.date
            ).minOrNull() ?: race.date

            if (earliestSessionDateStr == null || raceDateStr == null) return RaceStatus.FUTURE

            try {
                val earliestCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = raceDateFormat.parse(earliestSessionDateStr) ?: return RaceStatus.FUTURE }
                val raceEndCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = raceDateFormat.parse(raceDateStr) ?: return RaceStatus.FUTURE; add(Calendar.DAY_OF_MONTH, 1) } // End of race day

                val weekendStart = earliestCal.time
                val weekendEnd = raceEndCal.time

                if (now.after(weekendEnd)) {
                    return RaceStatus.PAST
                }

                // Define live window more precisely? Maybe start from FP1 time?
                // For now, using the whole day for simplicity
                if (now.after(weekendStart) && now.before(weekendEnd)) {
                    return RaceStatus.LIVE
                }


                val upcomingThreshold = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = now; add(Calendar.DAY_OF_YEAR, 7) }.time
                if (weekendStart.after(now) && weekendStart.before(upcomingThreshold)) {
                    return RaceStatus.UPCOMING
                }

                if (weekendStart.after(now)) {
                    return RaceStatus.FUTURE
                }

                return RaceStatus.PAST // Should be covered by the first check, but as a fallback

            } catch (e: ParseException) {
                Log.e("MainRaceListAdapter", "Error parsing dates for status: ${race.raceName}, ${e.message}")
                return RaceStatus.FUTURE
            }
        }

        private fun populateSessionDetails(race: Race) {
            binding.layoutSessionDetails.removeAllViews()
            val context = binding.root.context
            val inflater = LayoutInflater.from(context)

            fun addSessionRow(sessionName: String, practice: Practice?, isRace: Boolean = false, isQuali: Boolean = false) {
                val dateStr = practice?.date ?: if (isRace) race.date else null
                val timeStr = practice?.time ?: if (isRace) race.time else null

                if (dateStr != null) {
                    // Inflate using the detail binding
                    val detailBinding = ItemSessionDetailBinding.inflate(inflater, binding.layoutSessionDetails, false)

                    val formattedDateTime = formatSessionDateTime(dateStr, timeStr)
                    detailBinding.tvSessionNameDetail.text = "$sessionName: $formattedDateTime"

                    val sessionStartTime = parseSessionDateTime(dateStr, timeStr)
                    // Estimate session end time (e.g., 3 hours after start) - adjust as needed
                    val sessionEndTime = sessionStartTime?.let { Date(it.time + TimeUnit.HOURS.toMillis(3)) }

                    val isSessionLive = sessionStartTime != null && sessionEndTime != null && now.after(sessionStartTime) && now.before(sessionEndTime)
                    val isSessionFuture = sessionStartTime != null && sessionStartTime.after(now)

                    // Set Green Dot visibility
                    detailBinding.ivLiveSessionDot.isVisible = isSessionLive

                    if (isSessionFuture) {
                        detailBinding.tvSessionNameDetail.isEnabled = false
                        detailBinding.tvSessionNameDetail.setTextColor(ContextCompat.getColor(context, R.color.session_disabled_text))
                        detailBinding.ivSessionIcon.alpha = 0.5f
                        detailBinding.root.isClickable = false
                        detailBinding.root.setOnClickListener(null)
                        detailBinding.root.background = null // Remove selectable background
                    } else {
                        detailBinding.tvSessionNameDetail.isEnabled = true
                        detailBinding.tvSessionNameDetail.setTextColor(ContextCompat.getColor(context, R.color.black)) // Or your default text color
                        detailBinding.ivSessionIcon.alpha = 1.0f
                        detailBinding.root.isClickable = true
                        detailBinding.root.setOnClickListener {
                            onSessionClicked(race, sessionName, dateStr, timeStr)
                        }
                        detailBinding.root.setBackgroundResource(R.drawable.item_selector_background) // Add selectable background back
                    }

                    val iconRes = when {
                        isRace -> R.drawable.ic_flag
                        isQuali -> R.drawable.ic_timer
                        sessionName.contains("Sprint", ignoreCase = true) -> R.drawable.ic_sprint
                        else -> R.drawable.ic_practice
                    }
                    detailBinding.ivSessionIcon.setImageResource(iconRes)

                    binding.layoutSessionDetails.addView(detailBinding.root)
                }
            }

            // Add sessions in logical order
            addSessionRow("Practice 1", race.firstPractice)
            addSessionRow("Practice 2", race.secondPractice)
            addSessionRow("Practice 3", race.thirdPractice)
            addSessionRow("Sprint", race.sprint) // Add Sprint if available
            addSessionRow("Qualifying", race.qualifying, isQuali = true)
            addSessionRow("Race", null, isRace = true)
        }

        // --- Formatting and Parsing Helpers (Copied from previous implementation) ---
        private val outputDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val outputSessionFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        private val inputSessionDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val inputSessionTimeFormat = SimpleDateFormat("HH:mm:ss'Z'", Locale.US) // Use 'Z' for UTC

        init {
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            inputSessionDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            inputSessionTimeFormat.timeZone = TimeZone.getTimeZone("UTC")

            outputDateFormat.timeZone = TimeZone.getDefault()
            outputSessionFormat.timeZone = TimeZone.getDefault()
        }

        private fun formatDate(dateString: String?): String {
            if (dateString == null) return "N/A"
            return try {
                val date = inputDateFormat.parse(dateString)
                date?.let { outputDateFormat.format(it) } ?: "N/A"
            } catch (e: ParseException) {
                dateString
            }
        }

        private fun formatSessionDateTime(dateString: String?, timeString: String?): String {
            val sessionDate = parseSessionDateTime(dateString, timeString)
            return sessionDate?.let { outputSessionFormat.format(it) } ?: "TBD" // To Be Determined
        }

        private fun parseSessionDateTime(dateString: String?, timeString: String?): Date? {
            if (dateString == null) return null

            try {
                val datePart = inputSessionDateFormat.parse(dateString) ?: return null

                if (timeString != null) {
                    val timePart = inputSessionTimeFormat.parse(timeString) ?: return datePart // Return just date if time parse fails

                    val calDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calDate.time = datePart
                    val calTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calTime.time = timePart

                    calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY))
                    calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE))
                    calDate.set(Calendar.SECOND, calTime.get(Calendar.SECOND))
                    calDate.set(Calendar.MILLISECOND, 0) // Reset milliseconds for clean comparison

                    return calDate.time
                } else {
                    // If no time, assume start of the day for comparison purposes? Or handle as TBD?
                    // Let's return the date part, comparisons might be slightly off if only date is known.
                    return datePart
                }
            } catch (e: ParseException) {
                Log.e("MainRaceListAdapter", "Error parsing session date/time: $dateString $timeString", e)
                return null
            }
        }
    }


    // --- DiffUtil Callback ---
    class RaceDiffCallback : DiffUtil.ItemCallback<Race>() {
        override fun areItemsTheSame(oldItem: Race, newItem: Race): Boolean {
            return oldItem.round == newItem.round && oldItem.season == newItem.season
        }

        override fun areContentsTheSame(oldItem: Race, newItem: Race): Boolean {
            // Compare relevant fields that affect display, including expansion state indirectly
            // If we store expansion in the item itself, compare here. Otherwise, default works.
            return oldItem == newItem
        }
        // Optimization: Implement getChangePayload if only expansion or status changes
    }
}