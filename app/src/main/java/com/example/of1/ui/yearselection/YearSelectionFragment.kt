package com.example.of1.ui.yearselection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.of1.databinding.FragmentYearSelectionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class YearSelectionFragment : Fragment() {

    private lateinit var binding: FragmentYearSelectionBinding
    private val viewModel: YearSelectionViewModel by viewModels()
    private lateinit var yearAdapter: YearListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentYearSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeYears()
    }

    private fun setupRecyclerView() {
        yearAdapter = YearListAdapter(
            onYearClick = { year ->
                val action = YearSelectionFragmentDirections.actionYearSelectionFragmentToRacesFragment(year) // Use renamed action ID
                findNavController().navigate(action)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = yearAdapter
        }
    }

    // REMOVED: private fun openUrlInBrowser(url: String) { ... } // Remove method

    private fun observeYears() { // Renamed method
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.years.collectLatest { yearList ->
                    Log.d("YearSelectionFragment", "Updating adapter with ${yearList.size} years")
                    yearAdapter.submitList(yearList) // Submit the list directly
                }
            }
        }
    }
}