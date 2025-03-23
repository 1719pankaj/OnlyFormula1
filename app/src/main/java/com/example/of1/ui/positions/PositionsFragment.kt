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
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PositionsFragment : Fragment() {

    private lateinit var binding: FragmentPositionsBinding
    private val viewModel: PositionsViewModel by viewModels()
    private lateinit var positionAdapter: PositionListAdapter //Will be updated
    private val args: PositionsFragmentArgs by navArgs()

    // ... (onCreateView, onViewCreated, setupRecyclerView - will be modified) ...
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPositionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observePositions()
        observeDrivers() // Observe drivers

        // Get season and round from navigation arguments
        val meetingKey = args.meetingKey
        val sessionKey = args.sessionKey
        val isLive = args.isLive
        viewModel.getPositions(meetingKey, sessionKey, isLive) // Fetch results and pass isLive

    }

    private fun setupRecyclerView() {
        positionAdapter = PositionListAdapter() //Updated adapter
        positionAdapter.onPositionClick = { uiPosition ->
            // Navigate to LapsFragment, passing driverNumber, meetingKey, and sessionKey
            val action = PositionsFragmentDirections.actionPositionsFragmentToLapsFragment(
                driverNumber = uiPosition.position.driverNumber,
                meetingKey = uiPosition.position.meetingKey,
                sessionKey = uiPosition.position.sessionKey,
                isLive = args.isLive // Pass the isLive flag from PositionsFragment
            )
            findNavController().navigate(action)

        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = positionAdapter
        }
    }

    //Replace observePositions
    private fun observePositions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.positions.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("PositionsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val positions = resource.data ?: emptyList()
                            positionAdapter.setPositions(positions)  // Use setPositions
                            Log.d("PositionsFragment", "Success: ${positions.size} positions")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("PositionsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

    //Replace observeDrivers
    private fun observeDrivers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.drivers.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            //binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE // No need for a second progress bar
                            Log.d("PositionsFragment", "Loading Drivers...") // Log for clarity
                        }
                        is Resource.Success -> {
                            val drivers = resource.data ?: emptyList()
                            positionAdapter.setDrivers(drivers) // Use setDrivers
                            Log.d("PositionsFragment", "Success: ${drivers.size} drivers")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, "Driver error: ${resource.message}", Toast.LENGTH_SHORT).show()
                            Log.e("PositionsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

}