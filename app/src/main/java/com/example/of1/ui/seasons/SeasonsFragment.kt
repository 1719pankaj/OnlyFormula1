package com.example.of1.ui.seasons

import android.content.Intent
import android.net.Uri
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentSeasonsBinding
import com.example.of1.ui.seasons.adapter.SeasonListAdapter
import com.example.of1.ui.seasons.viewmodel.SeasonsViewModel
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class SeasonsFragment : Fragment() {

    private lateinit var binding: FragmentSeasonsBinding
    private val viewModel: SeasonsViewModel by viewModels()
    private lateinit var seasonAdapter: SeasonListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSeasonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSeasons()
        viewModel.getSeasons() // Fetch (or re-fetch) data here.
    }

    private fun setupRecyclerView() {
        seasonAdapter = SeasonListAdapter(
            onSeasonClick = { season ->
                // Navigate to RacesFragment, passing the selected year
                val action = SeasonsFragmentDirections.actionSeasonsFragmentToRacesFragment(season.year)
                findNavController().navigate(action)
            },
            onInfoClick = { url ->
                // Open URL in browser
                openUrlInBrowser(url)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = seasonAdapter
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    private fun observeSeasons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seasons.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("SeasonsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val seasons = resource.data ?: emptyList()
                            seasonAdapter.submitList(seasons) // Update adapter
                            Log.d("SeasonsFragment", "Success: ${seasons.size} seasons")
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("SeasonsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}