package com.example.of1.ui.laps

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
import com.example.of1.databinding.FragmentLapsBinding
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LapsFragment : Fragment() {

    private lateinit var binding: FragmentLapsBinding
    private val viewModel: LapsViewModel by viewModels()
    private lateinit var lapListAdapter: LapListAdapter  // You'll create this adapter
    private val args: LapsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeLaps()

        val sessionKey = args.sessionKey
        val driverNumber = args.driverNumber
        val isLive = args.isLive

        viewModel.getLaps(sessionKey, driverNumber, isLive) // Pass isLive

        binding.btnCarData.setOnClickListener {  // Add car data click listener
            val action = LapsFragmentDirections.actionLapsFragmentToCarDataFragment(
                driverNumber = args.driverNumber,
                meetingKey = args.meetingKey,
                sessionKey = args.sessionKey,
                isLive = args.isLive
            )
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        lapListAdapter = LapListAdapter() // Initialize your adapter
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lapListAdapter
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

    private fun observeLaps() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.laps.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("LapsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val laps = resource.data ?: emptyList()
                            lapListAdapter.submitList(laps)
                            Log.d("LapsFragment", "Success: ${laps.size} laps")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("LapsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}