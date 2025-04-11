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
                viewModel.uiPositions.collectLatest { uiPositionsList ->
                    // Update adapter data
                    positionAdapter.submitList(uiPositionsList)
                    Log.d("PositionsFragment", "Success: Submitted ${uiPositionsList.size} UI positions")

                    // --- Simplified Loading Indicator Logic ---
                    // Hide loading indicator as soon as we have *any* data to display
                    // Or if the underlying position flow is no longer loading (even if error)
                    val positionsLoading = viewModel.positions.value is Resource.Loading
                    binding.progressBar.visibility = if (positionsLoading && uiPositionsList.isEmpty()) View.VISIBLE else View.GONE
                    Log.d("PositionsFragment", "Loading Indicator Check: positionsLoading=$positionsLoading, listIsEmpty=${uiPositionsList.isEmpty()}")
                    // --- End Simplified Logic ---
                }
            }
        }
        // Observe individual flows for ERROR handling only
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.positions.collectLatest { if (it is Resource.Error) handleResourceError("Position", it.message) }
                }
                launch {
                    viewModel.drivers.collectLatest { if (it is Resource.Error) handleResourceError("Driver", it.message)}
                }
                // Optionally observe team radio error
                // launch { viewModel.teamRadioRaw.collectLatest { ... } }
            }
        }
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

    // Helper for error logging/display
    private fun handleResourceError(source: String, message: String?) {
        val errorMsg = "$source Error: ${message ?: "Unknown error"}"
        Log.e("PositionsFragment", errorMsg)
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show() // Show brief error
    }


    // Remove updateLoadingIndicator - logic is now simpler within observeUiUpdates

    // Helper function within PositionsFragment
    private fun findAdapterPosition(url: String?): Int {
        return url?.let { u ->
            positionAdapter.currentList.indexOfFirst { it.latestRadio?.recordingUrl == u }
        } ?: -1
    }
}
