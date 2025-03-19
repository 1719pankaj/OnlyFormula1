package com.example.of1.ui.main

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentMainBinding
import com.example.of1.ui.main.adapter.SessionListAdapter
import com.example.of1.ui.main.viewmodel.MainViewModel
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var sessionAdapter: SessionListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSessions()

        // Example of how to trigger a refresh (you might do this on a button click or swipe-to-refresh)
        // viewModel.getSessions("Belgium", "Sprint", 2023)
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionListAdapter()
        binding.recyclerView.apply {  // Assuming you add a RecyclerView with id 'recyclerView' to fragment_main.xml
            layoutManager = LinearLayoutManager(context)
            adapter = sessionAdapter
        }
    }


    private fun observeSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessions.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Show loading indicator
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE //Added isloading check to hide the progress bar
                            Log.d("MainFrag", "Loading...")
                        }
                        is Resource.Success -> {
                            // Update RecyclerView
                            val sessions = resource.data ?: emptyList()
                            sessionAdapter.submitList(sessions)  // Use submitList for ListAdapter
                            Log.d("MainFrag", "Success: ${sessions.size} sessions")
                        }
                        is Resource.Error -> {
                            // Show error message
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("MainFrag", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}