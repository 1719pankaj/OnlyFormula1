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
        binding.tvSelectedSeason.text = "Season: $selectedSeason"

        viewModel.getRaces(selectedSeason)
    }

    private fun setupRecyclerView() {
        raceListAdapter = RaceListAdapter { race ->
            // Calculate dateStart and dateEnd as before
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val sessionDates = listOfNotNull(
                race.firstPractice?.date, race.secondPractice?.date, race.thirdPractice?.date,
                race.qualifying?.date, race.sprint?.date, race.date
            ).mapNotNull { dateString ->
                try { inputFormat.parse(dateString) } catch (e: ParseException) { null }
            }

            if (sessionDates.isEmpty()) {
                Toast.makeText(context, "Error: No session dates found.", Toast.LENGTH_SHORT).show()
                return@RaceListAdapter // Use return@RaceListAdapter inside lambda
            }

            val earliestDate = sessionDates.minOrNull()!!
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.time = earliestDate

            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val dateStart = outputFormat.format(calendar.time)

            // Ensure race.date is parsed correctly before adding days
            val raceDate = try {
                inputFormat.parse(race.date)
            } catch (e: ParseException) {
                Log.e("RacesFragment", "Error parsing race date: ${race.date}", e)
                Toast.makeText(context, "Error processing race date.", Toast.LENGTH_SHORT).show()
                return@RaceListAdapter
            }

            calendar.time = raceDate!! // Use parsed race date
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dateEnd = outputFormat.format(calendar.time)

            // --- Navigate immediately, passing date range and year ---
            Log.d("RacesFragment", "Navigating to Sessions with year: ${race.season}, start: $dateStart, end: $dateEnd")
            val action = RacesFragmentDirections.actionRacesFragmentToSessionsFragment(
                seasonYear = race.season, // Pass the year as string
                dateStart = dateStart,
                dateEnd = dateEnd
            )
            findNavController().navigate(action)
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