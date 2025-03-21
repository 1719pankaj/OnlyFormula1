package com.example.of1.ui.races

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
import com.example.of1.databinding.FragmentRacesBinding
import com.example.of1.ui.races.adapter.RaceListAdapter
import com.example.of1.ui.races.viewmodel.RacesViewModel
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RacesFragment : Fragment() {
    private lateinit var binding: FragmentRacesBinding
    private val viewModel: RacesViewModel by viewModels()
    private lateinit var raceListAdapter: RaceListAdapter
    private val args: RacesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeRaces()

        val selectedSeason = args.seasonYear
        binding.tvSelectedSeason.text = "Season: $selectedSeason" // Show the year

        viewModel.getRaces(selectedSeason) // Fetch races for the selected season
    }

    private fun setupRecyclerView() {
        raceListAdapter = RaceListAdapter { race ->
            // Navigate to ResultsFragment, passing season and round
            val action = RacesFragmentDirections.actionRacesFragmentToResultsFragment(race.season, race.round)
            findNavController().navigate(action)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = raceListAdapter
        }
    }

    private fun observeRaces() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.races.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("RacesFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val races = resource.data ?: emptyList()
                            raceListAdapter.submitList(races)
                            Log.d("RacesFragment", "Success: ${races.size} races")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("RacesFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}