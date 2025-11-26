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

class ArtistDetailFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_artist_detail, container, false)
    }

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