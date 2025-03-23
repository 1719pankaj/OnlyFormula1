package com.example.of1.ui.sessions

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
import com.example.of1.databinding.FragmentSessionsBinding
import com.example.of1.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class SessionsFragment : Fragment() {

    private lateinit var binding: FragmentSessionsBinding
    private val viewModel: SessionsViewModel by viewModels()
    private val args: SessionsFragmentArgs by navArgs()
    private lateinit var sessionListAdapter: SessionListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSessions()

        val meetingKey = args.meetingKey
        viewModel.getSessions(meetingKey)
    }
    private fun setupRecyclerView() {
        sessionListAdapter = SessionListAdapter { session ->

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val startDate = dateFormat.parse(session.dateStart!!)
            val endDate = dateFormat.parse(session.dateEnd!!)
            val now = Date()
            val isLive = now in startDate..endDate
            Log.d("SessionsFragment", "Session is live: $isLive, session name ${session.sessionName}")
            // Navigate to PositionsFragment, passing meetingKey, sessionKey and isLive
            val action = SessionsFragmentDirections.actionSessionsFragmentToPositionsFragment(session.meetingKey, session.sessionKey, isLive)
            findNavController().navigate(action)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionListAdapter
        }
    }

    private fun observeSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessions.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("SessionsFragment", "Loading...")
                        }
                        is Resource.Success -> {
                            val sessions = resource.data ?: emptyList()
                            sessionListAdapter.submitList(sessions)
                            Log.d("SessionsFragment", "Success: ${sessions.size} sessions")

                        }
                        is Resource.Error -> {
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                             Log.e("SessionsFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
}