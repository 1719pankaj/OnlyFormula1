package com.example.of1.ui.laps

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentLapsBinding
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        observePitStops() // Add observation for pit stops

        val sessionKey = args.sessionKey
        val driverNumber = args.driverNumber
        val isLive = args.isLive

        viewModel.getLaps(sessionKey, driverNumber, isLive)


        // Live Car Data Button (only visible/clickable when isLive is true)
        binding.btnLiveCarData.visibility = if (isLive) View.VISIBLE else View.GONE
        if (isLive) { // Only set the click listener if isLive is true
            binding.btnLiveCarData.setOnClickListener {
                // Navigate to CarDataFragment with only startDate (for live updates)
                viewLifecycleOwner.lifecycleScope.launch{
                    val latestLap = viewModel.laps.value.data?.lastOrNull() // find the latest.
                    val startDate = latestLap?.dateStart

                    val action = LapsFragmentDirections.actionLapsFragmentToCarDataFragment(
                        driverNumber = args.driverNumber,
                        meetingKey = args.meetingKey,
                        sessionKey = args.sessionKey,
                        startDate = startDate,
                        endDate = null, // No endDate for live updates
                        isLive = true // This is live data
                    )
                    findNavController().navigate(action)
                }
            }
        }

    }

    // Start/stop polling in onResume/onPause
    override fun onResume() {
        super.onResume()
        if (args.isLive) {  // Only start polling if isLive is true
            viewModel.startPolling()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling() // Stop polling
    }


    private fun setupRecyclerView() {
        lapListAdapter = LapListAdapter()
        lapListAdapter.onCarDataClick = { lap ->
            // Navigate to CarDataFragment, passing start and end dates for this lap
            val startDate = lap.dateStart // NOW AVAILABLE!
            val endDate = if (lap.lapDuration != null && lap.dateStart != null) {
                // Convert dateStart to Date object and add lapDuration
                val inputFormat = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    Locale.getDefault()
                ) // Use correct format
                inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Set correct timezone

                try { // Add try-catch for ParseException
                    val startDateObj = inputFormat.parse(
                        lap.dateStart.replace(
                            "000+00:00",
                            "+00:00"
                        )
                    ) // Parse and remove 000
                    val endDateObj = Date(startDateObj.time + (lap.lapDuration * 1000).toLong())

                    // Format endDate back to string
                    val outputFormat =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    outputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    outputFormat.format(endDateObj)
                } catch (e: ParseException) {
                    Log.e("LapsFragment", "Error parsing date", e)
                    null // Handle the error appropriately, e.g., show a message
                }

            } else {
                null
            }

            // Navigate to CarDataFragment, pass isLive as false
            val action = LapsFragmentDirections.actionLapsFragmentToCarDataFragment(
                driverNumber = lap.driverNumber,
                meetingKey = args.meetingKey,
                sessionKey = args.sessionKey,
                startDate = startDate, // Pass the start date
                endDate = endDate, // Pass the calculated end date.
                isLive = false // This is NOT live data
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
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("LapsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val laps = resource.data ?: emptyList()
                            lapListAdapter.submitList(laps)
                            Log.d("LapsFragment", "Success: ${laps.size} laps")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("LapsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

    private fun observePitStops() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pitStops.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // You might want a separate loading indicator for pit stops,
                            // or just reuse the existing one.  For simplicity, I'm
                            // not adding a separate indicator here.
                        }
                        is Resource.Success -> {
                            val pitStops = resource.data ?: emptyList()
                            lapListAdapter.submitPitStops(pitStops) // Update adapter
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, "Error fetching pit stops: ${resource.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}