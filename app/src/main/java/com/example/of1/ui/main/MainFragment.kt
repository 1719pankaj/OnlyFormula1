package com.example.of1.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.of1.R
import com.example.of1.data.model.Race
import com.example.of1.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var raceListAdapter: MainRaceListAdapter
    private var initialScrollAndExpandPerformed = false // Renamed flag for clarity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset the flag when the view is created to allow re-processing on return
        initialScrollAndExpandPerformed = false

        setupRecyclerView()
        observeViewModel()

        binding.tvCurrentSeasonTitle.text = "OnlyFormula1"

        binding.btnAllSeasons.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_yearSelectionFragment)
        }
    }

    private fun setupRecyclerView() {
        raceListAdapter = MainRaceListAdapter(
            onItemToggled = { round ->
                val isCurrentlyExpanded = raceListAdapter.expandedItemRounds.contains(round)
                raceListAdapter.setExpanded(round, !isCurrentlyExpanded)
            },
            onSessionClicked = { race, sessionName, sessionDate, sessionTime ->
                Log.d("MainFragment", "Session clicked: ${race.raceName} - $sessionName")
                viewModel.findSessionAndPrepareNavigation(race, sessionName, sessionDate, sessionTime)
            }
        )

        binding.recyclerViewRaces.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = raceListAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                var currentRaces: List<Race> = emptyList()

                // Observe the race list data
                launch {
                    viewModel.displayedRaces.collectLatest { races ->
                        Log.d("MainFragment", "Submitting ${races.size} races to adapter (chronological)")
                        currentRaces = races
                        // Submit the list first
                        raceListAdapter.submitList(races) {
                            // This callback runs after the list is updated.
                            // Now, process the target race regardless of initial load for the red indicator.
                            if (races.isNotEmpty()) {
                                val (targetIndex, targetRoundForRed, targetRoundForExpand) = findTargetRaceInfo(races)

                                // --- Always update the red indicator ---
                                raceListAdapter.updateTargetRedRound(targetRoundForRed)

                                // --- Perform ONE-TIME scroll and expand ---
                                if (!initialScrollAndExpandPerformed) {
                                    performInitialScrollAndExpand(targetIndex, targetRoundForExpand)
                                    initialScrollAndExpandPerformed = true
                                }
                            } else {
                                // If list becomes empty, clear the red target
                                raceListAdapter.updateTargetRedRound(null)
                            }
                        }
                    }
                }


                // Observe loading state
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        Log.d("MainFragment", "isLoading collected: $isLoading")
                        binding.progressBarMain.isVisible = isLoading
                        // Check completion state based on loading *and* having processed data
                        // Reset initialScrollAndExpandPerformed if loading completes with empty list
                        if (!isLoading && !initialScrollAndExpandPerformed && currentRaces.isEmpty()) {
                            // Don't set initialScrollAndExpandPerformed = true here, wait for data
                            Log.d("MainFragment", "Loading finished, no items yet.")
                        }
                    }
                }

                // Observe error messages
                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        if (message != null) {
                            Toast.makeText(context, "Error loading races: $message", Toast.LENGTH_LONG).show()
                            Log.e("MainFragment", "Error loading races: $message")
                            // Reset initialScrollAndExpandPerformed on error? Maybe not needed if list remains empty.
                            // if (!initialScrollAndExpandPerformed) {
                            //    initialScrollAndExpandPerformed = true // Prevent scroll/expand attempts on error
                            //    Log.d("MainFragment", "Marking initial actions complete (error encountered).")
                            // }
                        }
                    }
                }


                // Observe navigation events (no change needed)
                launch {
                    viewModel.navigationEvent.collectLatest { event ->
                        Log.d("MainFragment", "Navigation event received: SessionKey=${event.sessionKey}, Live=${event.isLive}")
                        val action = MainFragmentDirections.actionMainFragmentToPositionsFragment(
                            meetingKey = event.meetingKey,
                            sessionKey = event.sessionKey,
                            isLive = event.isLive,
                            sessionType = event.sessionType
                        )
                        findNavController().navigate(action)
                    }
                }

                // Observe bridging state changes (no change needed)
                launch {
                    viewModel.bridgingState.collectLatest { stateMap ->
                        stateMap.forEach { (key, state) ->
                            if (state is BridgingState.Error) {
                                val (round, session) = key.split("_", limit = 2)
                                Toast.makeText(context, "Failed to load $session for Round $round: ${state.message}", Toast.LENGTH_SHORT).show()
                                Log.e("MainFragment", "Bridging error for $key: ${state.message}")
                                viewModel.clearBridgingState(key)
                            }
                        }
                    }
                }
            }
        }
    }

    // Renamed function to return all needed info
    private fun findTargetRaceInfo(races: List<Race>): Triple<Int, String?, String?> {
        val now = Date()
        var liveRaceIndex = -1
        var nextUpcomingRaceIndex = -1
        var targetIndex = -1
        var targetRoundForRed: String? = null
        var targetRoundForExpand: String? = null

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        for ((index, race) in races.withIndex()) {
            val earliestDateStr = listOfNotNull(
                race.firstPractice?.date, race.secondPractice?.date, race.thirdPractice?.date,
                race.qualifying?.date, race.sprint?.date, race.date
            ).minOrNull() ?: race.date
            val raceDateStr = race.date
            try {
                val earliestDate = inputFormat.parse(earliestDateStr)
                val raceEndDate = inputFormat.parse(raceDateStr)
                if (earliestDate != null && raceEndDate != null) {
                    val weekendStartCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = earliestDate }
                    val weekendEndCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = raceEndDate; add(Calendar.DAY_OF_MONTH, 1); add(Calendar.SECOND, -1) }

                    if (liveRaceIndex == -1 && now.after(weekendStartCal.time) && now.before(weekendEndCal.time)) {
                        liveRaceIndex = index
                    }
                    if (nextUpcomingRaceIndex == -1 && earliestDate.after(now)) {
                        nextUpcomingRaceIndex = index
                    }
                    // Optimization: if we found both live and next, we can stop early
                    if (liveRaceIndex != -1 && nextUpcomingRaceIndex != -1) break
                }
            } catch (e: ParseException) { Log.e("MainFragment", "Error parsing dates for target check: ${race.raceName}") }
        }

        // Determine final target index and rounds for UI updates
        if (liveRaceIndex != -1) {
            targetIndex = liveRaceIndex
            targetRoundForRed = races[liveRaceIndex].round
            targetRoundForExpand = races[liveRaceIndex].round
            Log.d("MainFragment", "Target is LIVE race: Round ${targetRoundForRed}")
        } else if (nextUpcomingRaceIndex != -1) {
            targetIndex = nextUpcomingRaceIndex
            targetRoundForRed = races[nextUpcomingRaceIndex].round
            targetRoundForExpand = races[nextUpcomingRaceIndex].round
            Log.d("MainFragment", "Target is NEXT UPCOMING race: Round ${targetRoundForRed}")
        } else {
            Log.d("MainFragment", "No live or upcoming race found.")
            if (races.isNotEmpty()) {
                targetIndex = races.size - 1 // Default to last race if all are past
            }
        }

        return Triple(targetIndex, targetRoundForRed, targetRoundForExpand)
    }

    // Renamed function for clarity
    private fun performInitialScrollAndExpand(targetIndex: Int, targetRoundForExpand: String?) {
        if (targetIndex != -1) {
            binding.recyclerViewRaces.post {
                val layoutManager = binding.recyclerViewRaces.layoutManager as? LinearLayoutManager
                layoutManager?.scrollToPositionWithOffset(targetIndex, 0)
                Log.d("MainFragment", "Performed ONE-TIME scroll to index: $targetIndex")

                targetRoundForExpand?.let { round ->
                    binding.recyclerViewRaces.post { // Post again ensures it runs after scroll potentially finishes
                        Log.d("MainFragment", "Performed ONE-TIME expand for round: $round")
                        raceListAdapter.setExpanded(round, true)
                    }
                }
            }
        }
    }
}