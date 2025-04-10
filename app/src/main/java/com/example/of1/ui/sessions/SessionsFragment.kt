package com.example.of1.ui.sessions

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
import androidx.navigation.fragment.navArgs // Import navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentSessionsBinding
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class SessionsFragment : Fragment() {

    private lateinit var binding: FragmentSessionsBinding
    private val viewModel: SessionsViewModel by viewModels()
    private val args: SessionsFragmentArgs by navArgs() // Get arguments
    private lateinit var sessionListAdapter: SessionListAdapter
    private var currentMeetingKey: Int? = null // Store meeting key


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSessions() // This will now fetch based on dates

        // Get arguments passed from RacesFragment
        val year = try {
            args.seasonYear.toInt()
        } catch (e: NumberFormatException){
            Log.e("SessionsFragment", "Invalid year format received: ${args.seasonYear}", e)
            Toast.makeText(context, "Error: Invalid year.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack() // Go back if year is invalid
            return
        }

        val dateStart = args.dateStart
        val dateEnd = args.dateEnd

        Log.d("SessionsFragment", "Fetching sessions for year: $year, start: $dateStart, end: $dateEnd")
        // Fetch sessions using the date range
        viewModel.fetchSessionsByDate(year, dateStart, dateEnd)
    }

    private fun setupRecyclerView() {
        sessionListAdapter = SessionListAdapter { session ->
            val meetingKey = currentMeetingKey ?: run { // Use stored meetingKey
                Log.e("SessionsFragment", "Meeting key not available for navigation")
                Toast.makeText(context, "Error: Could not load session details.", Toast.LENGTH_SHORT).show()
                return@SessionListAdapter
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val startDate = session.dateStart?.let { dateFormat.parse(it) } // Handle potential nulls
            val endDate = session.dateEnd?.let { dateFormat.parse(it) }
            val now = Date()

            val isLive = if (startDate != null && endDate != null) {
                now in startDate..endDate
            } else {
                false // Cannot determine liveness if dates are null
            }

            Log.d("SessionsFragment", "Session is live: $isLive, session name ${session.sessionName}")

            // Navigate to PositionsFragment
            val action = SessionsFragmentDirections.actionSessionsFragmentToPositionsFragment(
                meetingKey = meetingKey, // Use the stored/retrieved meetingKey
                sessionKey = session.sessionKey,
                isLive = isLive
            )
            findNavController().navigate(action)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionListAdapter
        }
    }

    private fun observeSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessions.collectLatest { resource ->
                    // Simplified Progress Bar Logic: Show only on Loading(true)
                    binding.progressBar.visibility = if (resource is Resource.Loading && resource.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    when (resource) {
                        is Resource.Success -> {
                            val sessions = resource.data ?: emptyList()
                            if (sessions.isNotEmpty()) {
                                currentMeetingKey = sessions.first().meetingKey
                                Log.d("SessionsFragment", "Successfully fetched sessions, meetingKey: $currentMeetingKey")
                                sessionListAdapter.submitList(sessions)
                            } else {
                                Log.w("SessionsFragment", "No sessions found for the given date range.")
                                Toast.makeText(context, "No sessions found for this race weekend.", Toast.LENGTH_LONG).show()
                                // Optionally navigate back
                                // findNavController().popBackStack()
                            }
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, "Error fetching sessions: ${resource.message}", Toast.LENGTH_LONG).show()
                            Log.e("SessionsFragment", "Error: ${resource.message}")
                            // Consider navigating back here as well if the error is critical
                            findNavController().popBackStack() // Example: Navigate back on error
                        }
                        is Resource.Loading -> {
                            if(resource.isLoading) { // Log only when it's actually loading
                                Log.d("SessionsFragment", "Loading sessions...")
                            }
                        }
                    }
                }
            }
        }
    }
}
