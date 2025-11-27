package com.noobcompany.nc_scorebeta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * RecyclerView Adapter for displaying a list of [Artist] objects.
 *
 * @property artists The list of artists to display.
 * @property onArtistClicked A callback function invoked when an artist item is clicked.
 */
class ArtistAdapter(
    private val artists: List<Artist>,
    private val onArtistClicked: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    /**
     * ViewHolder class for caching view references for each artist item.
     *
     * @param itemView The view for a single artist item.
     */
    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivArtistImage: ImageView = itemView.findViewById(R.id.ivArtistImage)
        val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
    }

    /**
     * Creates a new [ArtistViewHolder].
     *
     * @param parent The parent ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ArtistViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistViewHolder(view)
    }

    /**
     * Binds the data at the specified position to the ViewHolder.
     *
     * It sets the artist name and loads the artist image using Glide.
     * It also sets up the click listener for the item.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
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

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of artists.
     */
    override fun getItemCount() = artists.size
}
