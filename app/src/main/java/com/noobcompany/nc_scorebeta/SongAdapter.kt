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
 * A flexible RecyclerView Adapter for displaying lists of [Song] items.
 *
 * This adapter is designed to handle multiple view types (List, Grid, Carousel) based on configuration flags.
 * It integrates with [ListAdapter] to support efficient list updates via [DiffUtil].
 *
 * @property useGrid If set to `true`, the adapter inflates the grid layout (`item_song_grid`).
 * @property useCarousel If set to `true`, the adapter inflates the carousel layout (`item_song_carousel`).
 *                       Takes precedence over `useGrid` if both are true (logic in onCreateViewHolder).
 * @property onSongClicked Callback triggered when the main song item is clicked.
 * @property onArtistClicked Callback triggered when the artist name within the item is clicked.
 */
class SongAdapter(
    private val useGrid: Boolean = false,
    private val useCarousel: Boolean = false,
    private val onSongClicked: (Song) -> Unit,
    private val onArtistClicked: ((String) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    // Backward compatibility constructor - defaults artist click to null
    /**
     * Convenience constructor for basic list usage without carousel support or artist click handling.
     *
     * @param initialList The starting list of songs.
     * @param useGrid Whether to use grid layout.
     * @param onSongClicked Item click listener.
     */
    constructor(initialList: List<Song>, useGrid: Boolean = false, onSongClicked: (Song) -> Unit) : this(useGrid = useGrid, useCarousel = false, onSongClicked = onSongClicked, onArtistClicked = null) {
        submitList(initialList)
    }

    /**
     * Convenience constructor for the simplest list usage.
     *
     * @param initialList The starting list of songs.
     * @param onSongClicked Item click listener.
     */
    constructor(initialList: List<Song>, onSongClicked: (Song) -> Unit) : this(initialList, false, onSongClicked)

    /**
     * ViewHolder for Song items.
     *
     * Maintains references to the visual elements of a song card/row.
     *
     * @param itemView The root view.
     */
    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** Displays the album cover art. */
        val ivAlbumCover: ImageView = itemView.findViewById(R.id.ivAlbumCover)
        /** Displays the song title. */
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        /** Displays the artist name(s). */
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        /** Icon indicating premium status. */
        val ivPremiumStar: ImageView = itemView.findViewById(R.id.ivPremiumStar)
        /** Icon for toggling favorites. */
        val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        /** Reference to the root view for click handling. */
        val root: View = itemView
    }

    /**
     * Inflates the appropriate layout XML based on the adapter's configuration.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type (unused as this adapter handles logic internally via flags).
     * @return A new SongViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutId = when {
            useGrid -> R.layout.item_song_grid
            useCarousel -> R.layout.item_song_carousel
            else -> R.layout.item_song_card
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SongViewHolder(view)
    }

    /**
     * Binds [Song] data to the views.
     *
     * Sets text, loads images, manages visibility of premium badges, and configures
     * the favorite icon state. Also attaches specific click listeners.
     *
     * @param holder The ViewHolder to update.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        val context = holder.itemView.context

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.getFormattedArtist()
        holder.ivPremiumStar.visibility = if (song.isPremium == true) View.VISIBLE else View.GONE

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
            HapticUtils.success(it)
            FavoritesManager.toggleFavorite(context, song.id)
            updateFavoriteIcon()
        }

        Glide.with(holder.itemView.context)
            .load(song.albumCover)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)
            .into(holder.ivAlbumCover)

        holder.root.setOnClickListener {
            HapticUtils.viewTap(it)
            onSongClicked(song)
        }
    }

    /**
     * DiffUtil Callback for calculating list updates.
     */
    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        /**
         * Checks if two items refer to the same logical entity (by ID).
         * @param oldItem The old item.
         * @param newItem The new item.
         * @return `true` if IDs match.
         */
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Checks if the content of two items is identical (for UI update purposes).
         * @param oldItem The old item.
         * @param newItem The new item.
         * @return `true` if content matches.
         */
        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}
