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

/**
 * Fragment that displays the home screen of the application.
 *
 * This fragment features a hero section for the latest release, a list of new releases,
 * and a list of trending songs. It listens to real-time updates from Firestore to keep the content fresh.
 */
class HomeFragment : Fragment() {

    private lateinit var trendingAdapter: SongAdapter
    private lateinit var newReleasesAdapter: SongAdapter

    private val db = FirebaseFirestore.getInstance()
    private val songsCollection = db.collection("songs")
    private var songListener: ListenerRegistration? = null

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned.
     *
     * Initializes views and sets up navigation listeners.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupNavigation(view)
    }

    /**
     * Called when the fragment is visible to the user.
     *
     * Starts listening for real-time updates from Firestore.
     */
    override fun onStart() {
        super.onStart()
        startListening()
    }

    /**
     * Called when the fragment is no longer visible to the user.
     *
     * Stops listening for updates to conserve resources.
     */
    override fun onStop() {
        super.onStop()
        stopListening()
    }

    /**
     * Sets up navigation actions, such as the search button.
     *
     * @param view The root view of the fragment.
     */
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

    /**
     * Initializes the RecyclerViews for trending and new release songs.
     *
     * It configures the adapters with click listeners for song selection and artist navigation.
     *
     * @param view The root view of the fragment.
     */
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
    
    /**
     * Navigates to the [ArtistDetailFragment] for the given artist name.
     *
     * @param artistName The name of the artist to display.
     */
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

    /**
     * Updates the hero section with the details of the latest song.
     *
     * Sets the title, artist, and album cover image. Also adds a click listener to the hero card.
     *
     * @param song The song to feature in the hero section.
     */
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

    /**
     * Starts a real-time listener on the songs collection.
     *
     * Updates the UI whenever data changes in Firestore.
     * It identifies new releases (sorted by date) and trending songs (randomized for now).
     */
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

    /**
     * Removes the Firestore listener.
     */
    private fun stopListening() {
        songListener?.remove()
        songListener = null
    }
}
