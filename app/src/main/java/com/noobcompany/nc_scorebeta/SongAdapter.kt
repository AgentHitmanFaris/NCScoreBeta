package com.noobcompany.nc_scorebeta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * RecyclerView Adapter for displaying a list of [Song] objects.
 *
 * Supports both list and grid layouts. Handles song clicks, artist clicks, and favorite toggling.
 *
 * @property useGrid If true, uses a grid layout; otherwise, uses a card layout.
 * @property onSongClicked Callback when a song item is clicked.
 * @property onArtistClicked Optional callback when the artist name is clicked.
 */
class SongAdapter(
    private val useGrid: Boolean = false,
    private val onSongClicked: (Song) -> Unit,
    private val onArtistClicked: ((String) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    // Backward compatibility constructor - defaults artist click to null
    constructor(initialList: List<Song>, useGrid: Boolean = false, onSongClicked: (Song) -> Unit) : this(useGrid = useGrid, onSongClicked = onSongClicked, onArtistClicked = null) {
        submitList(initialList)
    }

    constructor(initialList: List<Song>, onSongClicked: (Song) -> Unit) : this(initialList, false, onSongClicked)

    /**
     * ViewHolder for caching view references for a song item.
     *
     * @param itemView The view for a single song item.
     */
    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAlbumCover: ImageView = itemView.findViewById(R.id.ivAlbumCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ivPremiumStar: ImageView = itemView.findViewById(R.id.ivPremiumStar)
        val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        val root: View = itemView
    }

    /**
     * Creates a new [SongViewHolder].
     *
     * Selects the appropriate layout resource based on [useGrid].
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type.
     * @return A new SongViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutId = if (useGrid) R.layout.item_song_grid else R.layout.item_song_card
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SongViewHolder(view)
    }

    /**
     * Binds the song data to the ViewHolder.
     *
     * Sets title, artist, album cover, premium status, and favorite status.
     * Sets up click listeners for the song, artist, and favorite button.
     *
     * @param holder The SongViewHolder.
     * @param position The position in the list.
     */
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        val context = holder.itemView.context

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.getFormattedArtist()
        holder.ivPremiumStar.visibility = if (song.isPremium) View.VISIBLE else View.GONE

        // Handle Artist Click
        holder.tvArtist.setOnClickListener {
            val artistName = song.artistNames.firstOrNull()
            if (!artistName.isNullOrEmpty()) {
                onArtistClicked?.invoke(artistName)
            }
        }

        // Handle Favorite Status
        fun updateFavoriteIcon() {
            val isFav = FavoritesManager.isFavorite(context, song.id)
            val iconRes = if (isFav) android.R.drawable.star_on else android.R.drawable.star_off
            holder.ivFavorite.setImageResource(iconRes)
        }
        updateFavoriteIcon()

        holder.ivFavorite.setOnClickListener {
            FavoritesManager.toggleFavorite(context, song.id)
            updateFavoriteIcon()
        }

        Glide.with(holder.itemView.context)
            .load(song.albumCover)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)
            .into(holder.ivAlbumCover)

        holder.root.setOnClickListener {
            onSongClicked(song)
        }
    }

    /**
     * DiffUtil callback for calculating changes between two lists of songs.
     */
    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}
