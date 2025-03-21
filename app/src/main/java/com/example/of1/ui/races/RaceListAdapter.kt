package com.example.of1.ui.races.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Race
import com.example.of1.databinding.ItemRaceBinding

class RaceListAdapter(private val onItemClick: (Race) -> Unit) :  // Add the click listener
    ListAdapter<Race, RaceListAdapter.RaceViewHolder>(RaceDiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val binding = ItemRaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val race = getItem(position)
        holder.bind(race)
        holder.itemView.setOnClickListener { onItemClick(race) } // Set the click listener
    }

    class RaceViewHolder(private val binding: ItemRaceBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(race: Race){
            binding.tvRaceName.text = race.raceName
            binding.tvCircuitName.text = race.circuit.circuitName
        }
    }
    class RaceDiffCallBack: DiffUtil.ItemCallback<Race>(){
        override fun areItemsTheSame(oldItem: Race, newItem: Race): Boolean {
            return oldItem.raceName == newItem.raceName && oldItem.season == newItem.season //Using racename and season just for now.
        }

        override fun areContentsTheSame(oldItem: Race, newItem: Race): Boolean {
            return oldItem == newItem
        }

    }
}