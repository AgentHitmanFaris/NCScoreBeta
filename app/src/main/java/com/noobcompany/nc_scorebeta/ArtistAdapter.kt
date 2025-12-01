package com.noobcompany.nc_scorebeta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * A specialized RecyclerView Adapter for managing the presentation of Artist data.
 *
 * This adapter is responsible for inflating the `item_artist` layout and binding
 * [Artist] model data to the views. It supports simple click interactions to navigate
 * to artist details.
 *
 * @property artists The data source containing a list of [Artist] objects to display.
 * @property onArtistClicked A callback function invoked when a user taps on an artist item.
 *                           It passes the selected [Artist] object to the listener.
 */
class ArtistAdapter(
    private val artists: List<Artist>,
    private val onArtistClicked: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    /**
     * ViewHolder class for the Artist Item.
     *
     * Caches references to the sub-views (Image and Text) to minimize `findViewById` calls
     * during scroll operations.
     *
     * @param itemView The inflated view for a single list row.
     */
    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /**
         * ImageView for the artist's profile picture.
         */
        val ivArtistImage: ImageView = itemView.findViewById(R.id.ivArtistImage)

        /**
         * TextView for the artist's display name.
         */
        val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
    }

    /**
     * Called when RecyclerView needs a new [ArtistViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ArtistViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artists[position]
        holder.tvArtistName.text = artist.name
        
        // Load image with Glide, applying circle crop transformation
        Glide.with(holder.itemView.context)
            .load(artist.getSafeImage())
            .placeholder(android.R.drawable.ic_menu_gallery)
            .circleCrop()
            .into(holder.ivArtistImage)

        // Set click listener
        holder.itemView.setOnClickListener { onArtistClicked(artist) }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The size of the artists list.
     */
    override fun getItemCount() = artists.size
}
