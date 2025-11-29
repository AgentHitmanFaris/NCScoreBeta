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
 * This adapter manages the binding of artist data to the views defined in `item_artist.xml`.
 *
 * @property artists The list of [Artist] objects to be displayed.
 * @property onArtistClicked A callback function that is invoked when an artist item is clicked. It receives the clicked [Artist] as a parameter.
 */
class ArtistAdapter(
    private val artists: List<Artist>,
    private val onArtistClicked: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    /**
     * ViewHolder class for caching view references for each artist item.
     *
     * It holds references to the artist's image view and name text view.
     *
     * @param itemView The view for a single artist item.
     */
    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivArtistImage: ImageView = itemView.findViewById(R.id.ivArtistImage)
        val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
    }

    /**
     * Creates a new [ArtistViewHolder] by inflating the item layout.
     *
     * @param parent The parent [ViewGroup] into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [ArtistViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistViewHolder(view)
    }

    /**
     * Binds the artist data at the specified position to the ViewHolder.
     *
     * It sets the artist name, loads the artist image using Glide with a circular crop and placeholder,
     * and sets up the click listener for the item.
     *
     * @param holder The [ArtistViewHolder] which should be updated to represent the contents of the item at the given position in the data set.
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
     * @return The total number of artists in the list.
     */
    override fun getItemCount() = artists.size
}
