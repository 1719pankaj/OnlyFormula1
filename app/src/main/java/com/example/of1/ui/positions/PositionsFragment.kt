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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentPositionsBinding
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class PositionsFragment : Fragment() {

    private lateinit var binding: FragmentPositionsBinding
    private val viewModel: PositionsViewModel by viewModels()
    private lateinit var positionAdapter: PositionListAdapter
    private val args: PositionsFragmentArgs by navArgs() // Get navigation arguments

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPositionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observePositions()

        // Get season and round from navigation arguments
        val meetingKey = args.meetingKey
        val sessionKey = args.sessionKey
        val isLive = args.isLive
        viewModel.getPositions(meetingKey, sessionKey, isLive) // Fetch results and pass isLive
    }

    private fun setupRecyclerView() {
        positionAdapter = PositionListAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = positionAdapter
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling() // Stop polling when the fragment is paused/invisible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPolling()  // VERY IMPORTANT: Stop polling in onDestroyView
    }

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
                            positionAdapter.submitList(positions) // Update adapter
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
}