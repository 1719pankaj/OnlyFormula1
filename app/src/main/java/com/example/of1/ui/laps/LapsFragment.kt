package com.example.of1.ui.laps

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat // Import ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.R // Import R
import com.example.of1.data.model.openf1.TeamRadio
import com.example.of1.databinding.FragmentLapsBinding
import com.example.of1.utils.AudioPlayerManager // Import manager
import com.example.of1.utils.Resource
import com.google.android.material.chip.Chip // Import Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.isNotEmpty

@AndroidEntryPoint
class LapsFragment : Fragment() {

    private lateinit var binding: FragmentLapsBinding
    private val viewModel: LapsViewModel by viewModels()
    private lateinit var lapListAdapter: LapListAdapter
    private val args: LapsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeLaps()
        observeTeamRadioChips() // Renamed for clarity
        observePlaybackStateForChips() // ADDED: Observe playback state for chips

        val sessionKey = args.sessionKey
        val driverNumber = args.driverNumber
        val isLive = args.isLive

        viewModel.initializeData(sessionKey, driverNumber, isLive)


        // Live Car Data Button
        binding.btnLiveCarData.visibility = if (isLive) View.VISIBLE else View.GONE
        if (isLive) {
            binding.btnLiveCarData.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val latestLap = viewModel.laps.value.data?.lastOrNull()
                    val startDate = latestLap?.dateStart

                    if (startDate == null && latestLap != null) {
                        Log.w("LapsFragment", "Latest lap exists but has null dateStart, cannot start live car data.")
                        Toast.makeText(context, "Cannot get live start time.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (latestLap == null) {
                        Log.w("LapsFragment", "No laps available to determine live car data start time.")
                        Toast.makeText(context, "No lap data available yet.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val action = LapsFragmentDirections.actionLapsFragmentToCarDataFragment(
                        driverNumber = args.driverNumber,
                        meetingKey = args.meetingKey,
                        sessionKey = args.sessionKey,
                        startDate = startDate,
                        endDate = null,
                        isLive = true
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }

    // Start/stop polling in onResume/onPause
    override fun onResume() {
        super.onResume()
        if (args.isLive) {
            viewModel.startPolling()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
        AudioPlayerManager.stop() // Stop audio
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPolling()
        AudioPlayerManager.stop() // Stop audio
    }


    private fun setupRecyclerView() {
        lapListAdapter = LapListAdapter()
        lapListAdapter.onCarDataClick = { lap ->
            // Navigate to CarDataFragment, passing start and end dates for this lap
            val startDate = lap.dateStart
            val endDate = if (lap.lapDuration != null && lap.dateStart != null) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                try {
                    val startDateObj = inputFormat.parse(lap.dateStart.replace("000+00:00", "+00:00"))
                    val endDateObj = Date(startDateObj.time + (lap.lapDuration * 1000).toLong())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    outputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    outputFormat.format(endDateObj)
                } catch (e: ParseException) {
                    Log.e("LapsFragment", "Error parsing date for end date calculation", e)
                    null
                }
            } else { null }

            val action = LapsFragmentDirections.actionLapsFragmentToCarDataFragment(
                driverNumber = lap.driverNumber,
                meetingKey = args.meetingKey,
                sessionKey = args.sessionKey,
                startDate = startDate,
                endDate = endDate,
                isLive = false
            )
            findNavController().navigate(action)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lapListAdapter
        }
    }


    private fun observeLaps() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.laps.collectLatest { resource ->
                    binding.progressBar.visibility = if (resource is Resource.Loading && resource.isLoading) View.VISIBLE else View.GONE

                    when (resource) {
                        is Resource.Success -> {
                            val laps = resource.data ?: emptyList()
                            lapListAdapter.submitList(laps)
                            Log.d("LapsFragment", "Success: ${laps.size} laps")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("LapsFragment", "Error: ${resource.message}")
                        }
                        is Resource.Loading -> {
                            if (resource.isLoading) Log.d("LapsFragment", "Loading laps...")
                        }
                    }
                }
            }
        }
    }

    // Renamed and focuses ONLY on creating/updating chips based on radio data
    private fun observeTeamRadioChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.teamRadioForDriverList.collectLatest { radios ->
                    updateRadioChips(radios) // Separate function to update chips
                }
            }
        }
    }

    // *** NEW: Observe Playback State specifically for updating chip icons ***
    private fun observePlaybackStateForChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AudioPlayerManager.playbackState.collectLatest { state ->
                    Log.d("LapsFragment", "Chip Playback State Changed: $state")
                    // Update *all* chips based on the new state
                    updateRadioChipIcons(state)
                }
            }
        }
    }

    // Updates the entire ChipGroup based on the list of radios
    private fun updateRadioChips(radios: List<TeamRadio>) {
        binding.chipGroupRadio.removeAllViews() // Clear previous chips

        if (radios.isNotEmpty()) {
            binding.radioScrollView.visibility = View.VISIBLE // Show ScrollView

            val sortedRadios = radios.sortedByDescending { it.date }

            for (radio in sortedRadios) {
                val chip = Chip(context).apply {
                    text = formatRadioTimestamp(radio.date)
                    tag = radio.recordingUrl
                    isCheckable = true // Use checkable for visual state
                    isChipIconVisible = true
                    // Set initial icon state (will be potentially updated by observePlaybackStateForChips)
                    chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_play)
                    isChecked = false // Start unchecked
                }

                chip.setOnClickListener { view ->
                    val url = view.tag as? String ?: return@setOnClickListener
                    Log.d("LapsFragment", "Chip clicked for URL: $url")
                    // Just call the manager, the observer will handle UI
                    AudioPlayerManager.play(requireContext(), url)
                }
                binding.chipGroupRadio.addView(chip)
            }
            // After adding all chips, update icons based on current playback state
            updateRadioChipIcons(AudioPlayerManager.playbackState.value)

        } else {
            binding.radioScrollView.visibility = View.GONE // Hide if no radios
        }
    }

    // Updates the icons of ALL chips based on the provided playback state
    private fun updateRadioChipIcons(state: AudioPlayerManager.PlaybackState) {
        val playingUrl = (state as? AudioPlayerManager.PlaybackState.Playing)?.url

        for (i in 0 until binding.chipGroupRadio.childCount) {
            (binding.chipGroupRadio.getChildAt(i) as? Chip)?.let { chip ->
                val chipUrl = chip.tag as? String
                if (chipUrl == playingUrl) {
                    chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_stop)
                    chip.isChecked = true
                } else {
                    chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_play)
                    chip.isChecked = false
                }
            }
        }
    }


    // Helper to format timestamp for chips
    private fun formatRadioTimestamp(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val dateObj = inputFormat.parse(dateString.replace("000+00:00", "+00:00"))
            val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // Display in local time? Or UTC?
            outputFormat.format(dateObj)
        } catch (e: Exception) {
            Log.e("LapsFragment", "Error formatting radio date: $dateString", e)
            "Radio" // Fallback text
        }
    }
}