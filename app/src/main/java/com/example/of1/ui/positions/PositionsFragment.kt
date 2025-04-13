package com.example.of1.ui.positions

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
import androidx.recyclerview.widget.RecyclerView // Import RecyclerView
import com.example.of1.databinding.FragmentPositionsBinding
import com.example.of1.utils.AudioPlayerManager
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PositionsFragment : Fragment() {

    private lateinit var binding: FragmentPositionsBinding
    private val viewModel: PositionsViewModel by viewModels()
    private lateinit var positionAdapter: PositionListAdapter
    private lateinit var raceControlAdapter: RaceControlAdapter // Add adapter for RC
    private val args: PositionsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPositionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews() // Renamed
        observeUiData()
        observePlaybackState()
        observeRaceControlMessages() // Add observer for RC messages

        // Initialize ViewModel which handles fetching based on isLive
        viewModel.initializeData(args.meetingKey, args.sessionKey, args.sessionType, args.isLive)
    }

    override fun onResume() {
        super.onResume()
        // ViewModel now handles the isLive check internally for starting polling
        if (args.isLive) {
            viewModel.startPolling()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
        AudioPlayerManager.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPolling()
        AudioPlayerManager.stop()
    }


    private fun setupRecyclerViews() {
        // Position Adapter Setup (Unchanged)
        positionAdapter = PositionListAdapter()
        positionAdapter.onPositionClick = { uiPosition ->
            val action = PositionsFragmentDirections.actionPositionsFragmentToLapsFragment(
                driverNumber = uiPosition.position.driverNumber,
                meetingKey = uiPosition.position.meetingKey,
                sessionKey = uiPosition.position.sessionKey,
                isLive = args.isLive
            )
            findNavController().navigate(action)
        }
        positionAdapter.onPlayRadioClick = { teamRadio ->
            AudioPlayerManager.play(requireContext(), teamRadio.recordingUrl)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = positionAdapter
        }

        // Race Control Adapter Setup (Unchanged)
        raceControlAdapter = RaceControlAdapter()
        binding.raceControlRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = raceControlAdapter
            itemAnimator = null // Consider keeping this to avoid flicker on updates
        }
    }

    // Observe Race Control Messages
    private fun observeRaceControlMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.displayedRaceControlMessages.collectLatest { messages ->
                    // Only show the RC recycler if live and messages exist
                    val isVisible = args.isLive && messages.isNotEmpty()
                    binding.raceControlRecyclerView.visibility = if (isVisible) View.VISIBLE else View.GONE

                    // Only submit list if visible to potentially save resources
                    if (isVisible) {
                        Log.d("PositionsFragment", "Submitting ${messages.size} RC messages to adapter.")
                        raceControlAdapter.submitList(messages)
                        // Optional: Scroll to bottom/top when new messages arrive
                        // if (messages.isNotEmpty()) {
                        //    binding.raceControlRecyclerView.smoothScrollToPosition(messages.size - 1) // Scroll to bottom
                        // }
                    } else if (!args.isLive) {
                        // Ensure it's hidden if not live, even if VM holds old messages briefly
                        binding.raceControlRecyclerView.visibility = View.GONE
                        // Optionally clear the adapter's list when not live
                        // raceControlAdapter.submitList(emptyList())
                    }

                }
            }
        }
    }


    // Consolidated observer for Loading/Error states of primary data
    private fun observeUiData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // --- Observer 1: Update the Position RecyclerView Adapter ---
                launch {
                    viewModel.uiPositions.collectLatest { uiPositionsList ->
                        Log.d("PositionsFragment", "Updating position adapter with ${uiPositionsList.size} items.")
                        positionAdapter.submitList(uiPositionsList)
                        // Visibility of progress bar is handled below based on Resource states
                    }
                }

                // --- Observer 2: Control Loading Indicator and Handle PRIMARY Errors ---
                launch {
                    // Combine only the Resources needed for the *initial* position list display
                    combine(
                        viewModel.positions, // Resource<List<Position>>
                        viewModel.drivers    // Resource<List<OF1Driver>>
                        // We don't include intervals here for loading state,
                        // as it might load later without blocking the initial positions
                    ) { posResource, driverResource ->

                        // Show loading if EITHER positions OR drivers are in initial Loading state
                        val isLoading = (posResource is Resource.Loading && posResource.isLoading && posResource.data == null) || // Check data==null for initial load
                                (driverResource is Resource.Loading && driverResource.isLoading && driverResource.data == null)

                        // Find the first critical error (positions or drivers)
                        val errorResource = listOf(posResource, driverResource)
                            .filterIsInstance<Resource.Error<*>>()
                            .firstOrNull()

                        val errorSource = when (errorResource) {
                            posResource -> "Position"
                            driverResource -> "Driver"
                            else -> null
                        }
                        // Pass loading state, error details, and whether the UI list has data *yet*
                        Triple(isLoading, errorSource to errorResource?.message, viewModel.uiPositions.value.isNotEmpty())

                    }.collectLatest { (isLoading, errorDetails, hasUiData) ->
                        val (errorSource, errorMessage) = errorDetails

                        if (errorSource != null) {
                            // Handle critical error (positions/drivers failed)
                            handleResourceError(errorSource, errorMessage)
                            binding.progressBar.visibility = View.GONE // Hide loading on error
                        } else if (isLoading && !hasUiData) {
                            // Show loading indicator ONLY if loading AND the combined UI list isn't populated yet
                            binding.progressBar.visibility = View.VISIBLE
                            Log.d("PositionsFragment", "Showing progress bar (Initial Loading: $isLoading, HasData: $hasUiData)")
                        } else {
                            // Hide loading indicator if not loading OR if UI data has arrived
                            binding.progressBar.visibility = View.GONE
                            // Log.d("PositionsFragment", "Hiding progress bar (Loading: $isLoading, HasData: $hasUiData)")
                        }
                    }
                }
            }
        }
    }


    private fun handleResourceError(source: String, message: String?) {
        val errorMsg = "$source Error: ${message ?: "Unknown error"}"
        Log.e("PositionsFragment", errorMsg)
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }

    // Observer for playback state (updates button icons)
    private fun observePlaybackState() {
        // ... (This logic remains unchanged) ...
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AudioPlayerManager.playbackState.collectLatest { state ->
                    Log.d("PositionsFragment", "Playback State Changed: $state")
                    val affectedUrl: String? = when (state) {
                        is AudioPlayerManager.PlaybackState.Playing -> state.url
                        is AudioPlayerManager.PlaybackState.Stopped -> state.previousUrl
                    }
                    val positionsToUpdate = findAdapterPositionsByURL(affectedUrl)

                    // Also need to update the previously playing item when stopping
                    if (state is AudioPlayerManager.PlaybackState.Stopped) {
                        findAdapterPositionsByURL(state.previousUrl).forEach { pos ->
                            if (!positionsToUpdate.contains(pos)) positionsToUpdate.add(pos)
                        }
                    }

                    if (positionsToUpdate.isNotEmpty()) {
                        positionsToUpdate.distinct().forEach { pos ->
                            Log.d("PositionsFragment", "Notifying item change for playback state at pos $pos")
                            positionAdapter.notifyItemChanged(pos)
                        }
                    }
                }
            }
        }
    }


    // Helper function within PositionsFragment - adjusted to return a List
    private fun findAdapterPositionsByURL(url: String?): MutableList<Int> {
        val positions = mutableListOf<Int>()
        url?.let { u ->
            positionAdapter.currentList.forEachIndexed { index, uiPosition ->
                if (uiPosition.latestRadio?.recordingUrl == u) {
                    positions.add(index)
                }
            }
        }
        return positions
    }

    private fun observeLapStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lapStatusText.collectLatest { statusText ->
                    if (statusText.isNotBlank()) {
                        binding.tvLapStatus.text = statusText
                        binding.tvLapStatus.visibility = View.VISIBLE
                    } else {
                        binding.tvLapStatus.visibility = View.GONE
                    }
                }
            }
        }
    }
}