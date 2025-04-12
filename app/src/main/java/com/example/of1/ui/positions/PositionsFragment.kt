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
import com.example.of1.databinding.FragmentPositionsBinding
import com.example.of1.utils.AudioPlayerManager // Import AudioPlayerManager
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
    private val args: PositionsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPositionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val meetingKey = args.meetingKey
        val sessionKey = args.sessionKey
        val isLive = args.isLive

        setupRecyclerView(args.isLive) // Pass isLive
        observeUiUpdates() // Consolidated observer
        observePlaybackState() // Keep this for radio button updates

        viewModel.getPositions(args.meetingKey, args.sessionKey, args.isLive)
    }

    override fun onResume() {
        super.onResume()
        if (args.isLive) {
            viewModel.startPolling() // Start polling in onResume
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling() // Stop polling in onPause
        AudioPlayerManager.stop() // Stop audio when fragment pauses
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPolling() // Also stop polling here
        AudioPlayerManager.stop() // Ensure stopped on destruction
    }


    private fun setupRecyclerView(isLive: Boolean) {
        positionAdapter = PositionListAdapter()
        // positionAdapter.setIsLive(isLive) // Removed

        // Pass listeners directly to adapter instance
        positionAdapter.onPositionClick = { uiPosition ->
            // ... (navigation to Laps) ...
            val action = PositionsFragmentDirections.actionPositionsFragmentToLapsFragment(
                driverNumber = uiPosition.position.driverNumber,
                meetingKey = uiPosition.position.meetingKey,
                sessionKey = uiPosition.position.sessionKey,
                isLive = args.isLive // Use the fragment's args.isLive
            )
            findNavController().navigate(action)
        }
        positionAdapter.onPlayRadioClick = { teamRadio ->
            // ... (call AudioPlayerManager.play) ...
            AudioPlayerManager.play(requireContext(), teamRadio.recordingUrl)
            // No need to notify adapter here, observePlaybackState handles it
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = positionAdapter
        }
    }

    // Consolidated observer for UI state and data
    private fun observeUiUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // --- Observer 1: Update the RecyclerView Adapter ---
                launch {
                    viewModel.uiPositions.collectLatest { uiPositionsList ->
                        positionAdapter.submitList(uiPositionsList)
                        Log.d("PositionsFragment", "Success: Submitted ${uiPositionsList.size} UI positions")
                        // Hide progress bar ONLY if we successfully submitted non-empty data
                        if (uiPositionsList.isNotEmpty()) {
                            binding.progressBar.visibility = View.GONE
                            Log.d("PositionsFragment", "Hiding progress bar because UI list is not empty.")
                        }
                        // If the list is empty, the loading state below will control the bar
                    }
                }

                // --- Observer 2: Control Loading Indicator and Handle Errors ---
                launch {
                    // Combine the *Resource* states of essential data sources
                    combine(viewModel.positions, viewModel.drivers) { posResource, driverResource ->
                        // Determine overall loading state (show if *either* essential source is loading)
                        val isLoading = posResource is Resource.Loading || driverResource is Resource.Loading

                        // Determine if there's a critical error
                        val errorResource = if (posResource is Resource.Error) posResource
                        else if (driverResource is Resource.Error) driverResource // Prioritize position error?
                        else null

                        Triple(isLoading, errorResource, viewModel.uiPositions.value.isNotEmpty()) // Pass loading, error, and if UI has data

                    }.collectLatest { (isLoading, errorResource, hasUiData) ->

                        if (errorResource != null) {
                            // Handle error (show message, potentially hide loading)
                            handleResourceError(if (viewModel.positions.value is Resource.Error) "Position" else "Driver", errorResource.message)
                            binding.progressBar.visibility = View.GONE // Hide on error
                            Log.d("PositionsFragment", "Error occurred, hiding progress bar.")
                        } else if (isLoading && !hasUiData) {
                            // Show loading only if actually loading AND UI isn't populated yet
                            binding.progressBar.visibility = View.VISIBLE
                            Log.d("PositionsFragment", "Showing progress bar (Loading: $isLoading, HasData: $hasUiData)")
                        } else if (!isLoading && hasUiData) {
                            // Hide loading if not loading AND we have data
                            binding.progressBar.visibility = View.GONE
                            Log.d("PositionsFragment", "Hiding progress bar (Loading: $isLoading, HasData: $hasUiData)")
                        }
                        // If !isLoading and !hasUiData (e.g., empty success), keep progress bar hidden (or show empty state)
                        else if (!isLoading && !hasUiData) {
                            binding.progressBar.visibility = View.GONE
                            Log.d("PositionsFragment", "Hiding progress bar (Not Loading, No Data)")
                            // Consider showing an "Empty" message TextView here
                        }
                    }
                }
            }
        }
    }

    private fun handleResourceError(source: String, message: String?) {
        val errorMsg = "$source Error: ${message ?: "Unknown error"}"
        Log.e("PositionsFragment", errorMsg)
        // Avoid showing toast multiple times if both fail around the same time
        // Maybe track if an error was already shown recently? For now, simple toast.
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }

    // Observer for playback state (updates button icons)
    private fun observePlaybackState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AudioPlayerManager.playbackState.collectLatest { state ->
                    Log.d("PositionsFragment", "Playback State Changed: $state")
                    // Trigger redraw of relevant items
                    val affectedUrl: String? = when (state) {
                        is AudioPlayerManager.PlaybackState.Playing -> state.url
                        is AudioPlayerManager.PlaybackState.Stopped -> state.previousUrl
                        else -> null
                    }
                    val changedPosition = findAdapterPosition(affectedUrl)
                    if (changedPosition != -1) {
                        Log.d("PositionsFragment", "Notifying item change for playback state at pos $changedPosition")
                        positionAdapter.notifyItemChanged(changedPosition)
                    }
                }
            }
        }
    }


    // Helper function within PositionsFragment
    private fun findAdapterPosition(url: String?): Int {
        return url?.let { u ->
            positionAdapter.currentList.indexOfFirst { it.latestRadio?.recordingUrl == u }
        } ?: -1
    }
}
