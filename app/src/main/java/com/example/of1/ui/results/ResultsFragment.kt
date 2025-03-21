
package com.example.of1.ui.results

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
import com.example.of1.databinding.FragmentResultsBinding
import com.example.of1.ui.viewmodel.ResultsViewModel
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResultsFragment : Fragment() {

    private lateinit var binding: FragmentResultsBinding
    private val viewModel: ResultsViewModel by viewModels()
    private lateinit var resultAdapter: ResultListAdapter
    private val args: ResultsFragmentArgs by navArgs() // Get navigation arguments

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeResults()

        // Get season and round from navigation arguments
        val season = args.seasonYear
        val round = args.raceRound

        viewModel.getResults(season, round) // Fetch results
    }
    private fun setupRecyclerView() {
        resultAdapter = ResultListAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultAdapter
        }
    }
    private fun observeResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("ResultsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val results = resource.data ?: emptyList()
                            resultAdapter.submitList(results) // Update adapter
                            Log.d("ResultsFragment", "Success: ${results.size} results")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("ResultsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}