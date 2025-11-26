package com.noobcompany.nc_scorebeta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ArtistAdapter(
    private val artists: List<Artist>,
    private val onArtistClicked: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivArtistImage: ImageView = itemView.findViewById(R.id.ivArtistImage)
        val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artists[position]
        holder.tvArtistName.text = artist.name
        
        Glide.with(holder.itemView.context)
            .load(artist.getSafeImage())
            .placeholder(android.R.drawable.ic_menu_gallery)
            .circleCrop()
            .into(holder.ivArtistImage)

        holder.itemView.setOnClickListener { onArtistClicked(artist) }
    }

    override fun getItemCount() = artists.size
}