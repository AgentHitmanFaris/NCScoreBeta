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
import com.google.firebase.firestore.Query

import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.Source
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.view.animation.AnimationUtils

/**
 * Fragment representing the application's Home Dashboard.
 *
 * This is the landing screen for the user. It aggregates content into sections:
 * - **Hero**: A large banner showcasing the latest or featured release.
 * - **Trending**: A horizontally scrolling list of popular songs.
 * - **New Releases**: A horizontally scrolling list of recently added songs.
 */
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class HomeFragment : Fragment() {

    private lateinit var trendingAdapter: SongAdapter
    private lateinit var newReleasesAdapter: SongAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val db = FirebaseFirestore.getInstance()
    private val songsCollection = db.collection("songs")

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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        setupViews(view)
        setupNavigation(view)
        
        swipeRefresh.setOnRefreshListener {
            fetchDashboardData()
        }
        
        // Fetch data once using Coroutines for better performance
        fetchDashboardData()
    }

    /**
     * Configures the navigation buttons (e.g., Search).
     *
     * @param view The root view of the fragment.
     */
    private fun setupNavigation(view: View) {
        val btnSearch = view.findViewById<ImageButton>(R.id.btnSearch)
        btnSearch.setOnClickListener {
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
        
        // Apply Fall-down animation
        val resId = R.anim.layout_animation_fall_down
        val animation = AnimationUtils.loadLayoutAnimation(context, resId)
        rvTrending.layoutAnimation = animation
        rvNewReleases.layoutAnimation = animation
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
     * Fetches dashboard data using Coroutines.
     *
     * This method:
     * 1. Fetches songs ordered by creation date.
     * 2. Prioritizes cache if available for instant loading.
     * 3. Updates the UI on the main thread.
     */
    private fun fetchDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AppLogger.log("HomeFragment", "Fetching dashboard data...")
                
                // Fetch from Cache first for speed, then Server for freshness
                // Using await() from kotlinx-coroutines-play-services
                val snapshot = songsCollection
                    .orderBy("dateAdded", Query.Direction.DESCENDING)
                    .limit(30)
                    .get()
                    .await()

                val allSongs = snapshot.toObjects(Song::class.java)

                if (allSongs.isNotEmpty()) {
                    val newReleases = allSongs.take(10)
                    updateHeroSection(newReleases[0])
                    newReleasesAdapter.submitList(newReleases)

                    val trendingSongs = allSongs.drop(10).shuffled().take(5)
                    trendingAdapter.submitList(trendingSongs)
                    
                    // Trigger animations
                    view?.findViewById<RecyclerView>(R.id.rvTrending)?.scheduleLayoutAnimation()
                    view?.findViewById<RecyclerView>(R.id.rvNewReleases)?.scheduleLayoutAnimation()
                }
                swipeRefresh.isRefreshing = false
            } catch (e: Exception) {
                AppLogger.error("HomeFragment", "Failed to fetch dashboard", e)
                Toast.makeText(context, "Connection error. Using cached data.", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
        }
    }
}

