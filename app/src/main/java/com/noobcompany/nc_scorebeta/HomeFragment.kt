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
 * Fragment representing the application's Home Dashboard.
 *
 * This is the landing screen for the user. It aggregates content into sections:
 * - **Hero**: A large banner showcasing the latest or featured release.
 * - **Trending**: A horizontally scrolling list of popular songs.
 * - **New Releases**: A horizontally scrolling list of recently added songs.
 *
 * It uses a real-time Firestore listener to ensure the dashboard reflects the latest database state.
 */
class HomeFragment : Fragment() {

    private lateinit var trendingAdapter: SongAdapter
    private lateinit var newReleasesAdapter: SongAdapter

    private val db = FirebaseFirestore.getInstance()
    private val songsCollection = db.collection("songs")
    private var songListener: ListenerRegistration? = null

    /**
     * Inflates the layout XML for the home screen.
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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    /**
     * Called immediately after the view is created.
     *
     * Initializes the RecyclerView adapters and layout managers, and sets up navigation listeners.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupNavigation(view)
    }

    /**
     * Lifecycle method: Called when the Fragment becomes visible.
     *
     * Starts the real-time Firestore listener to fetch dashboard content.
     */
    override fun onStart() {
        super.onStart()
        startListening()
    }

    /**
     * Lifecycle method: Called when the Fragment is no longer visible.
     *
     * Detaches the Firestore listener to conserve resources and bandwidth.
     */
    override fun onStop() {
        super.onStop()
        stopListening()
    }

    /**
     * Configures the navigation buttons (e.g., Search).
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
     * Sets up the RecyclerViews for the "Trending" and "New Releases" sections.
     *
     * Both sections use a horizontal LinearLayoutManager and the [SongAdapter] in carousel mode.
     *
     * @param view The root view of the fragment.
     */
    private fun setupViews(view: View) {
        val rvTrending = view.findViewById<RecyclerView>(R.id.rvTrending)
        trendingAdapter = SongAdapter(useGrid = false, useCarousel = true, onSongClicked = { song ->
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            navigateToArtist(artistName)
        })
        rvTrending.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvTrending.adapter = trendingAdapter

        val rvNewReleases = view.findViewById<RecyclerView>(R.id.rvNewReleases)
        newReleasesAdapter = SongAdapter(useGrid = false, useCarousel = true, onSongClicked = { song ->
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            navigateToArtist(artistName)
        })
        rvNewReleases.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvNewReleases.adapter = newReleasesAdapter
    }
    
    /**
     * Navigates to the Artist Detail screen.
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
     * Populates the Hero Card with the latest song's data.
     *
     * @param song The [Song] object representing the featured item.
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
     * Initializes the Firestore real-time listener.
     *
     * - Fetches all songs.
     * - Sorts by creation date to find "New Releases".
     * - Randomly selects "Trending" songs (simulated logic).
     * - Updates the UI adapters with the processed lists.
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
                    // Sort by 'createdAt' descending. Nulls (old songs without date) go to the end.
                    val sortedByDate = allSongs.sortedWith(compareByDescending<Song> { it.createdAt }.thenBy { it.title })
                    
                    // Debug log to verify dates
                    if (sortedByDate.isNotEmpty()) {
                         Log.d("HomeFragment", "Latest Song: ${sortedByDate[0].title}, Date: ${sortedByDate[0].createdAt?.toDate()}")
                    }

                    updateHeroSection(sortedByDate[0])
                    newReleasesAdapter.submitList(sortedByDate.take(5))
                    val trendingSongs = allSongs.shuffled().take(5)
                    trendingAdapter.submitList(trendingSongs)
                }
            }
        }
    }

    /**
     * Cleans up the listener when the fragment is stopped.
     */
    private fun stopListening() {
        songListener?.remove()
        songListener = null
    }
}
