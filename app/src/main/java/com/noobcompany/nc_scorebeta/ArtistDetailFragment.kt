package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment that displays detailed information for a specific Artist.
 *
 * This screen presents the artist's profile (Image, Name, Bio) and a dynamically fetched
 * list of songs associated with that artist.
 */
class ArtistDetailFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Inflates the layout for the artist detail screen.
     *
     * @param inflater The LayoutInflater object.
     * @param container The parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The inflated View.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_artist_detail, container, false)
    }

    /**
     * Called when the view is created.
     *
     * Extracts artist details passed via Bundle arguments, binds them to the UI views,
     * and triggers the loading of the artist's discography.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val artistName = arguments?.getString("ARTIST_NAME") ?: "Unknown"
        val artistBio = arguments?.getString("ARTIST_BIO") ?: "No bio available."
        val artistImage = arguments?.getString("ARTIST_IMAGE") ?: ""

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.tvArtistName).text = artistName
        view.findViewById<TextView>(R.id.tvArtistBio).text = artistBio

        val ivHero = view.findViewById<ImageView>(R.id.ivArtistHero)
        Glide.with(this).load(artistImage).circleCrop().into(ivHero)

        fetchArtistSongs(view, artistName)
    }

    /**
     * Fetches all songs where the artist is listed in the "artistNames" array.
     *
     * Performs a Firestore query `whereArrayContains("artistNames", artistName)`.
     * Results are displayed in a vertical RecyclerView.
     *
     * @param view The root view of the fragment.
     * @param artistName The exact name of the artist to filter by.
     */
    private fun fetchArtistSongs(view: View, artistName: String) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvArtistSongs)
        rvSongs.layoutManager = LinearLayoutManager(context)
        
        // IMPORTANT: This assumes songs have 'artistNames' array field
        db.collection("songs")
            .whereArrayContains("artistNames", artistName)
            .get()
            .addOnSuccessListener { result ->
                val songs = result.toObjects(Song::class.java)
                // Use SongAdapter in list mode (useGrid=false)
                rvSongs.adapter = SongAdapter(useGrid = false, onSongClicked = { song ->
                     context?.let { SongHandler.onSongClicked(it, song) }
                }, onArtistClicked = { 
                    // Do nothing if already on artist page, or maybe reload?
                    // For now, empty is fine to prevent stacking same page
                })
                // We need to submit list because we changed constructor usage
                (rvSongs.adapter as SongAdapter).submitList(songs)
                
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading songs", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }
}
