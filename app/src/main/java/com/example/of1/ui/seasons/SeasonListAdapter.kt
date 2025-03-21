package com.example.of1.ui.seasons.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.of1.data.model.Season
import com.example.of1.databinding.ItemSeasonBinding

class SeasonListAdapter(
    private val onSeasonClick: (Season) -> Unit,
    private val onInfoClick: (String) -> Unit
) : ListAdapter<Season, SeasonListAdapter.SeasonViewHolder>(SeasonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val binding = ItemSeasonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeasonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeasonViewHolder(private val binding: ItemSeasonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(season: Season) {
            binding.tvSeasonYear.text = season.year.toString()

            binding.root.setOnClickListener {
                onSeasonClick(season)
            }

            binding.btnInfo.setOnClickListener {
                season.wikipediaUrl?.let { url ->
                    onInfoClick(url)
                }
            }
        }
    }

    class SeasonDiffCallback : DiffUtil.ItemCallback<Season>() {
        override fun areItemsTheSame(oldItem: Season, newItem: Season): Boolean {
            return oldItem.year == newItem.year
        }

        override fun areContentsTheSame(oldItem: Season, newItem: Season): Boolean {
            return oldItem == newItem
        }
    }
}