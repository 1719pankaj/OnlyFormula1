package com.example.of1.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Session
import com.example.of1.databinding.ItemSessionBinding

class SessionListAdapter : ListAdapter<Session, SessionListAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
    }

    class SessionViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            // Use the safe call operator (?.) and the Elvis operator (?:) for all fields.
            binding.tvSessionName.text = session.sessionName
            binding.tvCircuitName.text = session.circuitShortName
            binding.tvCircuitKey.text = session.circuitKey.toString()
            // ... bind other fields similarly ...
        }
    }
    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.sessionKey == newItem.sessionKey
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }
}