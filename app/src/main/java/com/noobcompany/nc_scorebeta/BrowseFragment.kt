package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment that provides the main browsing interface for the song library.
 *
 * Features:
 * - A grid-based layout of all songs.
 * - Infinite scrolling (pagination) to handle large datasets efficiently.
 * - Real-time search functionality to filter by Song Title or Artist Name.
 */
class BrowseFragment : Fragment() {

    private lateinit var adapter: SongAdapter
    private val db = FirebaseFirestore.getInstance()
    private var lastVisible: DocumentSnapshot? = null
    private var isScrolling = false
    private var isLastItemReached = false
    private var currentQueryStr = ""

    // Data source for the adapter
    private val displayedSongs: ArrayList<Song> = ArrayList()

    /**
     * Inflates the layout XML for the Browse screen.
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
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    /**
     * Called immediately after the view is created.
     *
     * Initializes the RecyclerView for grid display and attaches the TextWatcher for search input.
     * Triggers the initial load of song data.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        setupSearch(view)
        
        // Initial load
        loadSongs(true)
    }

    /**
     * Configures the RecyclerView with a [GridLayoutManager] (2 columns).
     *
     * Adds an `OnScrollListener` to detect when the user scrolls to the bottom of the list,
     * triggering the loading of the next page of results.
     *
     * @param view The root view of the fragment.
     */
    private fun setupRecyclerView(view: View) {
        val rvAllScores = view.findViewById<RecyclerView>(R.id.rvAllScores)
        val layoutManager = GridLayoutManager(context, 2)
        rvAllScores.layoutManager = layoutManager
        
        adapter = SongAdapter(useGrid = true, onSongClicked = { song ->
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            val fragment = ArtistDetailFragment()
            val args = Bundle()
            args.putString("ARTIST_NAME", artistName)
            fragment.arguments = args
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        })
        rvAllScores.adapter = adapter

        // Pagination Listener
        rvAllScores.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (isScrolling && (visibleItemCount + firstVisibleItemPosition == totalItemCount) && !isLastItemReached) {
                    isScrolling = false
                    // Load next page
                    if (currentQueryStr.isEmpty()) {
                        loadSongs(false)
                    }
                }
            }
        })
    }

    /**
     * Sets up the search input field.
     *
     * Attaches a [TextWatcher] to the EditText. On text change, it debounces (implicitly via logic)
     * and triggers a new search query.
     *
     * @param view The root view of the fragment.
     */
    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query != currentQueryStr) {
                    currentQueryStr = query
                    performSearch(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Loads songs from the "songs" collection in Firestore.
     *
     * Supports pagination using `startAfter` the last visible document.
     *
     * @param isInitial If true, clears the current list and starts a fresh query.
     */
    private fun loadSongs(isInitial: Boolean) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        if (isInitial) progressBar?.visibility = View.VISIBLE

        var query = db.collection("songs")
            .orderBy("title")
            .limit(20)

        if (!isInitial && lastVisible != null) {
            query = query.startAfter(lastVisible!!)
        }

        query.get()
            .addOnSuccessListener { result ->
                if (isInitial) {
                    displayedSongs.clear()
                }
                
                if (!result.isEmpty) {
                    lastVisible = result.documents[result.size() - 1]
                    val newSongs = result.toObjects(Song::class.java)
                    displayedSongs.addAll(newSongs)
                    adapter.submitList(ArrayList(displayedSongs)) // Submit a copy
                    
                    if (result.size() < 20) {
                        isLastItemReached = true
                    }
                } else {
                    isLastItemReached = true
                }
                progressBar?.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading songs", Toast.LENGTH_SHORT).show()
                progressBar?.visibility = View.GONE
            }
    }

    /**
     * Executes a composite search query.
     *
     * 1. Searches for songs where `title` starts with the query string (prefix search).
     * 2. Searches for songs where `artistNames` contains the query string (exact array match).
     *
     * The results are merged, deduplicated by ID, and displayed.
     *
     * @param query The search term.
     */
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            isLastItemReached = false
            lastVisible = null
            loadSongs(true)
            return
        }

        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.visibility = View.VISIBLE

        // Query 1: Title Prefix Search
        val titleQuery = db.collection("songs")
            .orderBy("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(50)

        // Query 2: Artist Exact Match (best we can do easily)
        // Note: This is case-sensitive and requires exact name match unless we store keywords
        val artistQuery = db.collection("songs")
            .whereArrayContains("artistNames", query)
            .limit(50)

        // Run both
        val titleTask = titleQuery.get()
        val artistTask = artistQuery.get()

        com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(titleTask, artistTask)
            .addOnSuccessListener { results ->
                val mergedList = ArrayList<Song>()
                val seenIds = HashSet<String>()

                // Add Title Results
                for (doc in results[0].documents) {
                    val song = doc.toObject(Song::class.java)
                    if (song != null && seenIds.add(song.id)) {
                        mergedList.add(song)
                    }
                }

                // Add Artist Results
                for (doc in results[1].documents) {
                    val song = doc.toObject(Song::class.java)
                    if (song != null && seenIds.add(song.id)) {
                        mergedList.add(song)
                    }
                }
                
                adapter.submitList(mergedList)
                isLastItemReached = true 
                progressBar?.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                progressBar?.visibility = View.GONE
            }
    }
}
