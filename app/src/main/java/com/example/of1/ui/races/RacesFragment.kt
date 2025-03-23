package com.example.of1.ui.races

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
import com.example.of1.data.repository.SessionRepository
import com.example.of1.databinding.FragmentRacesBinding
import com.example.of1.ui.races.adapter.RaceListAdapter
import com.example.of1.ui.races.viewmodel.RacesViewModel
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class RacesFragment : Fragment() {
    private lateinit var binding: FragmentRacesBinding
    private val viewModel: RacesViewModel by viewModels()
    private lateinit var raceListAdapter: RaceListAdapter
    private val args: RacesFragmentArgs by navArgs()

    @Inject
    lateinit var sessionRepository: SessionRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeRaces()

        val selectedSeason = args.seasonYear
        binding.tvSelectedSeason.text = "Season: $selectedSeason" // Show the year

        viewModel.getRaces(selectedSeason) // Fetch races for the selected season
    }

    private fun setupRecyclerView() {
        raceListAdapter = RaceListAdapter { race ->
            lifecycleScope.launch {
                val year = try {
                    race.season.toInt()
                } catch (e: NumberFormatException) {
                    Log.e("RacesFragment", "Invalid year format: ${race.season}", e)
                    Toast.makeText(context, "Error: Invalid year format.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // --- Date Calculation Logic (Modified) ---
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                // Create a list of all possible session dates (handling nulls)
                val sessionDates = listOfNotNull(
                    race.firstPractice?.date,
                    race.secondPractice?.date,
                    race.thirdPractice?.date,
                    race.qualifying?.date,
                    race.sprint?.date,
                    race.date // Include the race date itself
                ).mapNotNull { dateString ->
                    try {
                        inputFormat.parse(dateString) // Parse each date string
                    } catch (e: ParseException){
                        Log.e("RacesFragment", "date parsing failed: $dateString", e)
                        null // Ignore if the date string can't be parsed.
                    }
                }

                if (sessionDates.isEmpty()) {
                    // Handle the case where there are NO valid dates (very unlikely, but good to be safe)
                    Toast.makeText(context, "Error: No session dates found.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Find the earliest date
                val earliestDate = sessionDates.minOrNull()!! // We know sessionDates isn't empty

                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.time = earliestDate // Set to earliest date

                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                outputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val dateStart = outputFormat.format(calendar.time) // Earliest date

                //Calculate the race end date.
                calendar.time = inputFormat.parse(race.date)!! // Set calendar to race date.
                calendar.add(Calendar.DAY_OF_MONTH, 1)       // Add *one* day to race date.
                val dateEnd = outputFormat.format(calendar.time)     //One day after race date.


                val formattedDateStart = dateStart
                val formattedDateEnd = dateEnd

                Log.d("RacesFragment", "Fetching sessions for year: $year, dateStart: $formattedDateStart, dateEnd: $formattedDateEnd")

                sessionRepository.getSessionsByDate(year, formattedDateStart, formattedDateEnd).collectLatest { sessionResult ->

                    // ... (rest of the handling of sessionResult (Success, Error, Loading) remains the same) ...
                    when (sessionResult) {
                        is Resource.Success -> {
                            Log.d("RacesFragment", "Sessions fetched successfully ${sessionResult.data?.size}")
                            // Find a session with session_type "Race" if available, otherwise take the first
                            //Since we are getting all sessions, we no loger need to get specifically the Race
                            val session = sessionResult.data?.firstOrNull() // Changed Here

                            if (session != null) {
                                val meetingKey = session.meetingKey
                                Log.d("RacesFragment", "Navigating with meetingKey: $meetingKey, sessionKey: ${session.sessionKey}")
                                // Navigate to SessionsFragment, passing the meetingKey
                                val action = RacesFragmentDirections.actionRacesFragmentToSessionsFragment(meetingKey)
                                findNavController().navigate(action)
                            } else {
                                Toast.makeText(context, "No sessions found for this race", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is Resource.Error -> {
                            // Handle error (e.g., show a Toast)
                            Toast.makeText(context, "Error fetching sessions: ${sessionResult.message}", Toast.LENGTH_LONG).show()
                        }
                        is Resource.Loading -> {
                            // You could show a loading indicator here if needed, but it's likely very fast.
                        }
                    }
                }
            }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = raceListAdapter
        }
    }

    private fun observeRaces() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.races.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("RacesFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val races = resource.data ?: emptyList()
                            raceListAdapter.submitList(races)
                            Log.d("RacesFragment", "Success: ${races.size} races")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("RacesFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}