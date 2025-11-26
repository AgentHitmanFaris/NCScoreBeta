package com.noobcompany.nc_scorebeta

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private lateinit var trendingAdapter: SongAdapter
    private lateinit var newReleasesAdapter: SongAdapter

    private val db = FirebaseFirestore.getInstance()
    private val songsCollection = db.collection("songs")
    private var songListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupNavigation(view)
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    private fun setupNavigation(view: View) {
        val btnSearch = view.findViewById<ImageButton>(R.id.btnSearch)
        btnSearch.setOnClickListener {
            // Navigate to Browse Tab via Parent Activity or replace fragment
            // For now, simpler to let MainActivity handle tab switching if we exposed a method,
            // but typically search button might just open the browse tab.
            // Accessing MainActivity's bottom nav:
            (activity as? MainActivity)?.switchToBrowse()
        }
    }

    private fun setupViews(view: View) {
        val rvTrending = view.findViewById<RecyclerView>(R.id.rvTrending)
        trendingAdapter = SongAdapter(useGrid = false, onSongClicked = { song ->
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            navigateToArtist(artistName)
        })
        rvTrending.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvTrending.adapter = trendingAdapter

        val rvNewReleases = view.findViewById<RecyclerView>(R.id.rvNewReleases)
        newReleasesAdapter = SongAdapter(useGrid = false, onSongClicked = { song ->
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            navigateToArtist(artistName)
        })
        rvNewReleases.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvNewReleases.adapter = newReleasesAdapter
    }
    
    private fun navigateToArtist(artistName: String) {
        val fragment = ArtistDetailFragment()
        val args = Bundle()
        args.putString("ARTIST_NAME", artistName)
        fragment.arguments = args
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateHeroSection(song: Song) {
        val view = view ?: return
        val tvHeroTitle = view.findViewById<TextView>(R.id.tvHeroTitle)
        val tvHeroArtist = view.findViewById<TextView>(R.id.tvHeroArtist)
        val ivHeroImage = view.findViewById<ImageView>(R.id.ivHeroImage)
        val heroCard = view.findViewById<CardView>(R.id.heroCard)

        tvHeroTitle.text = song.title
        tvHeroArtist.text = song.getFormattedArtist()

        Glide.with(this).load(song.albumCover).into(ivHeroImage)

        heroCard.setOnClickListener { 
            context?.let { SongHandler.onSongClicked(it, song) }
        }
    }

    private fun startListening() {
        if (songListener != null) return

        songListener = songsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener

            if (snapshots != null) {
                val allSongs = ArrayList<Song>()
                for (document in snapshots) {
                    try {
                        val song = document.toObject(Song::class.java)
                        allSongs.add(song)
                    } catch (error: Exception) {
                        Log.e("HomeFragment", "Error converting song", error)
                    }
                }

                if (allSongs.isNotEmpty()) {
                    val sortedByDate = allSongs.sortedByDescending { it.createdAt }
                    updateHeroSection(sortedByDate[0])
                    newReleasesAdapter.submitList(sortedByDate.take(5))
                    val trendingSongs = allSongs.shuffled().take(5)
                    trendingAdapter.submitList(trendingSongs)
                }
            }
        }
    }

    private fun stopListening() {
        songListener?.remove()
        songListener = null
    }
}