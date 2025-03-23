package com.example.of1.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Session
import com.example.of1.databinding.ItemSessionBinding

class SessionListAdapter(private val onItemClick: (Session) -> Unit) : ListAdapter<Session, SessionListAdapter.SessionViewHolder>(SessionDiffCallback()){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionListAdapter.SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionListAdapter.SessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
        holder.itemView.setOnClickListener { onItemClick(session) }
    }
    inner class SessionViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: Session) {
            binding.tvSessionName.text = session.sessionName
            binding.tvCircuitName.text = session.circuitShortName // Example: Bind circuit name
            // Bind other data as needed
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.sessionKey == newItem.sessionKey // Use a unique identifier
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem // Compare all relevant fields
        }
    }
}