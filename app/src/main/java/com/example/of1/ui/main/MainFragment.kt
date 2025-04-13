package com.example.of1.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Import AlertDialog
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
import com.example.of1.databinding.DialogYearRaceSelectionBinding // Import dialog binding
import com.example.of1.databinding.FragmentMainBinding
import com.example.of1.utils.Resource // Import Resource
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
    private var initialLoadComplete = false

    // --- onCreateView, onViewCreated, setupRecyclerView ---
    // (Keep these as they are from the previous correct version)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialLoadComplete = false // Reset flag

        setupRecyclerView()
        observeViewModel() // Main list observers

        binding.tvCurrentSeasonTitle.text = "OnlyFormula1"

        binding.btnAllSeasons.setOnClickListener {
             showYearRaceSelectionDialog() // Call dialog function
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

    // --- observeViewModel ---
    // (Keep this as it is from the previous correct version)
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                var currentRaces: List<Race> = emptyList()

                // Observe main race list data
                launch {
                    viewModel.displayedRaces.collectLatest { races ->
                        Log.d("MainFragment", "Submitting ${races.size} races to adapter (chronological)")
                        currentRaces = races
                        raceListAdapter.submitList(races) {
                            if (!initialLoadComplete && races.isNotEmpty()) {
                                val (targetIndex, targetRoundForRed, targetRoundForExpand) = findTargetRaceInfo(races)
                                raceListAdapter.updateTargetRedRound(targetRoundForRed)
                                performInitialScrollAndExpand(targetIndex, targetRoundForExpand)
                                initialLoadComplete = true
                            } else if (races.isEmpty()) {
                                raceListAdapter.updateTargetRedRound(null)
                            }
                        }
                    }
                }

                // Observe main list loading state
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        Log.d("MainFragment", "isLoading collected: $isLoading")
                        binding.progressBarMain.isVisible = isLoading
                        if (!isLoading && !initialLoadComplete && currentRaces.isEmpty()) {
                             Log.d("MainFragment", "Marking initial load complete (loading finished, no items).")
                        }
                    }
                }

                // Observe main list error messages
                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        if (message != null) {
                            Toast.makeText(context, "Error loading races: $message", Toast.LENGTH_LONG).show()
                            Log.e("MainFragment", "Error loading races: $message")
                        }
                    }
                }


                // Observe MAIN navigation events (from item clicks)
                launch {
                    viewModel.mainNavigationEvent.collectLatest { event ->
                        Log.d("MainFragment", "Main Nav event: SessionKey=${event.sessionKey}, Live=${event.isLive}")
                        val action = MainFragmentDirections.actionMainFragmentToPositionsFragment(
                            meetingKey = event.meetingKey,
                            sessionKey = event.sessionKey,
                            isLive = event.isLive,
                            sessionType = event.sessionType
                        )
                        findNavController().navigate(action)
                    }
                }

                 // Observe bridging state changes (from item clicks)
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

                 // Observe DIALOG navigation events
                 launch {
                      viewModel.dialogNavigationEvent.collectLatest { event ->
                           Log.d("MainFragment", "Dialog Nav event: Year=${event.year}, Start=${event.dateStart}, End=${event.dateEnd}")
                           val action = MainFragmentDirections.actionMainFragmentToSessionsFragment(
                               seasonYear = event.year,
                               dateStart = event.dateStart,
                               dateEnd = event.dateEnd
                           )
                           findNavController().navigate(action)
                      }
                 }
            }
        }
    }

    // --- Dialog Implementation ---
    private fun showYearRaceSelectionDialog() {
        val dialogBinding = DialogYearRaceSelectionBinding.inflate(layoutInflater)
        val yearSpinner = dialogBinding.spinnerYear
        val raceSpinner = dialogBinding.spinnerRace
        val btnGo = dialogBinding.btnGo
        val progressBar = dialogBinding.progressBarDialog
        val errorTextView = dialogBinding.tvDialogError
        val layoutRaceSpinner = dialogBinding.layoutRaceSpinner

        var selectedRace: Race? = null // Store the chosen race
        var currentDialogRaces: List<Race> = emptyList() // *** Store the list used for the adapter ***

        // 1. Setup Year Spinner
        val years = viewModel.yearsForDialog.value
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years)
        yearSpinner.setAdapter(yearAdapter)

        // 2. Year Selection Listener
        yearSpinner.setOnItemClickListener { parent, view, position, id ->
            val selectedYear = parent.getItemAtPosition(position) as String
            Log.d("Dialog", "Year selected: $selectedYear")

            // Reset race selection
            raceSpinner.text = null
            raceSpinner.setAdapter(null)
            layoutRaceSpinner.isEnabled = false
            layoutRaceSpinner.visibility = View.INVISIBLE
            btnGo.isEnabled = false
            btnGo.visibility = View.INVISIBLE
            errorTextView.visibility = View.GONE
            selectedRace = null
            currentDialogRaces = emptyList() // *** Clear the stored list ***

            // Fetch races
            progressBar.visibility = View.VISIBLE
            viewModel.fetchRacesForYearDialog(selectedYear)
        }

        // 3. Observe Race Results for Dialog
        viewLifecycleOwner.lifecycleScope.launch {
            // Use repeatOnLifecycle associated with the Fragment's view
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                 viewModel.dialogRaces.collectLatest { resource ->
                    // This block will re-execute if the dialog is closed and reopened
                    // Need to ensure we are updating the *correct* dialog instance if it exists

                    // Check if the dialog is still showing (or use a flag)
                    // For simplicity, we update the local variable directly here
                    // but in complex scenarios, manage dialog lifecycle better.

                    progressBar.isVisible = resource is Resource.Loading && resource.isLoading
                    errorTextView.isVisible = resource is Resource.Error

                    if (resource is Resource.Error) {
                        errorTextView.text = resource.message ?: "Unknown error"
                        layoutRaceSpinner.isEnabled = false
                        layoutRaceSpinner.visibility = View.INVISIBLE
                        currentDialogRaces = emptyList() // *** Clear stored list on error ***
                    } else if (resource is Resource.Success) {
                        val races = resource.data ?: emptyList()
                        currentDialogRaces = races // *** Store the successful list ***
                        if (races.isNotEmpty()) {
                            val raceNames = races.map { "R${it.round}: ${it.raceName}" }
                            val raceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, raceNames)
                            raceSpinner.setAdapter(raceAdapter) // Set adapter only when data is ready
                            layoutRaceSpinner.isEnabled = true
                            layoutRaceSpinner.visibility = View.VISIBLE
                            errorTextView.visibility = View.GONE
                        } else {
                            errorTextView.text = "No races found for this year."
                            errorTextView.visibility = View.VISIBLE
                            layoutRaceSpinner.isEnabled = false
                            layoutRaceSpinner.visibility = View.INVISIBLE
                        }
                        // Reset race selection state after loading new races
                        btnGo.isEnabled = false
                        btnGo.visibility = View.INVISIBLE
                        raceSpinner.text = null
                        selectedRace = null
                    }
                }
            }
        }


        // 4. Race Selection Listener - *** MODIFIED LOGIC ***
        raceSpinner.setOnItemClickListener { parent, view, position, id ->
             Log.d("Dialog", "Race selected at position: $position")

             if (position >= 0 && position < currentDialogRaces.size) {
                 selectedRace = currentDialogRaces[position] // Get race by index
                 Log.d("Dialog", "Found race: ${selectedRace?.raceName}")
                 btnGo.isEnabled = true
                 btnGo.visibility = View.VISIBLE
                 errorTextView.visibility = View.GONE
             } else {
                 // This case should ideally not happen if adapter/list are in sync
                 Log.e("Dialog", "Invalid position ($position) selected for currentDialogRaces size (${currentDialogRaces.size})!")
                 selectedRace = null
                 btnGo.isEnabled = false
                 btnGo.visibility = View.INVISIBLE
                 errorTextView.text = "Error selecting race." // Show UI error
                 errorTextView.visibility = View.VISIBLE
             }
        }


        // 5. Build and Show Dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .create()

        // 6. Go Button Listener (Inside Dialog Setup)
        btnGo.setOnClickListener {
            selectedRace?.let { race ->
                Log.d("Dialog", "Go button clicked for: ${race.raceName}")
                viewModel.prepareDialogNavigation(race)
                dialog.dismiss()
            } ?: run {
                 // This should ideally not happen if button is enabled correctly
                 Log.e("Dialog", "Go button clicked but selectedRace is null!")
                 Toast.makeText(context, "Please select a race first.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }


    // --- Helper functions findTargetRaceInfo, performInitialScrollAndExpand ---
    // (Keep these as they are from the previous correct version)
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
                   if (liveRaceIndex != -1 && nextUpcomingRaceIndex != -1) break
               }
           } catch (e: ParseException) { Log.e("MainFragment", "Error parsing dates for target check: ${race.raceName}") }
       }

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
               targetIndex = races.size - 1
           }
       }

       return Triple(targetIndex, targetRoundForRed, targetRoundForExpand)
   }

    private fun performInitialScrollAndExpand(targetIndex: Int, targetRoundForExpand: String?) {
       if (targetIndex != -1) {
           binding.recyclerViewRaces.post {
               val layoutManager = binding.recyclerViewRaces.layoutManager as? LinearLayoutManager
               layoutManager?.scrollToPositionWithOffset(targetIndex, 0)
               Log.d("MainFragment", "Performed ONE-TIME scroll to index: $targetIndex")

               targetRoundForExpand?.let { round ->
                   binding.recyclerViewRaces.post {
                         Log.d("MainFragment", "Performed ONE-TIME expand for round: $round")
                         raceListAdapter.setExpanded(round, true)
                   }
               }
           }
       }
   }
}