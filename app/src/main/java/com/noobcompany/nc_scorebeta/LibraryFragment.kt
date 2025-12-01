package com.noobcompany.nc_scorebeta

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment that manages the display of the user's personal "Library" (Favorites).
 *
 * This fragment acts as the bookmarks view. It retrieves the list of favorite song IDs
 * stored locally in SharedPreferences, then performs batch queries against Firestore
 * to retrieve the full metadata for those songs.
 *
 * Features:
 * - Local storage integration via [FavoritesManager].
 * - Batch Firestore queries to respect API limits.
 * - Local search/filtering within the favorited items.
 */
class LibraryFragment : Fragment() {

    private lateinit var adapter: SongAdapter
    private var allSongsList: List<Song> = ArrayList()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    /**
     * Inflates the layout XML for the Library screen.
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
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    /**
     * Called immediately after the view is created.
     *
     * Initializes the RecyclerView, search bar, and starts the data loading process.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        setupRecyclerView(view)
        setupSearch(view)
        fetchSongsAndFilter()
    }

    /**
     * Lifecycle method: Called when the fragment resumes interaction.
     *
     * Re-applies the current search filter. This is important if the user navigates away
     * and returns, ensuring the list state is consistent with the search text.
     */
    override fun onResume() {
        super.onResume()
        if (allSongsList.isNotEmpty()) {
            // Safely access view in fragment
            val view = view
            if (view != null) {
                filterFavoritesAndSearch(view.findViewById<EditText>(R.id.etSearch).text.toString())
            }
        }
    }

    /**
     * Configures the RecyclerView to use a Grid layout.
     *
     * @param view The root view of the fragment.
     */
    private fun setupRecyclerView(view: View) {
        val rvLibrary = view.findViewById<RecyclerView>(R.id.rvLibrary)
        rvLibrary.layoutManager = GridLayoutManager(context, 2)

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
        rvLibrary.adapter = adapter
    }

    /**
     * Sets up the local search filter.
     *
     * Listens for text changes in the search bar and filters the *already loaded* list of favorites.
     * (Unlike BrowseFragment, which queries the server).
     *
     * @param view The root view of the fragment.
     */
    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFavoritesAndSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Loads the user's favorite songs.
     *
     * 1. Gets IDs from [FavoritesManager].
     * 2. Splits IDs into chunks of 10 (Firestore `whereIn` limit).
     * 3. Executes parallel queries.
     * 4. Aggregates results and populates the adapter.
     */
    private fun fetchSongsAndFilter() {
        val context = context ?: return
        val favorites = FavoritesManager.getFavorites(context).toList()

        if (favorites.isEmpty()) {
            allSongsList = emptyList()
            adapter.submitList(emptyList())
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No favorites yet!\nGo browse and heart some songs."
            return
        }

        progressBar.visibility = View.VISIBLE

        // Firestore 'in' queries are limited to 10 items
        val batches = favorites.chunked(10)
        val tasks = batches.map { batch ->
            db.collection("songs")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(tasks)
            .addOnSuccessListener { results ->
                val combinedSongs = ArrayList<Song>()
                for (snapshot in results) {
                    combinedSongs.addAll(snapshot.toObjects(Song::class.java))
                }
                allSongsList = combinedSongs
                
                // Initial filter (empty query)
                filterFavoritesAndSearch("")
                
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading library", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    /**
     * Filters the local list of favorites based on the search query.
     *
     * Checks both Song Title and Artist Name.
     *
     * @param query The search keyword.
     */
    private fun filterFavoritesAndSearch(query: String) {
        val context = context ?: return
        // We don't need to re-check FavoritesManager here because we only fetched favorites.
        // However, if the user unfavorited something while in this screen, we might want to handle it.
        // For now, let's just filter the 'allSongsList' which IS the favorite list.

        val lowerCaseQuery = query.lowercase()
        val finalFilteredList = allSongsList.filter { song ->
            song.title.lowercase().contains(lowerCaseQuery) ||
                    song.getFormattedArtist().lowercase().contains(lowerCaseQuery)
        }

        if (finalFilteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            if (allSongsList.isEmpty()) {
                 tvEmpty.text = "No favorites yet!\nGo browse and heart some songs."
            } else {
                 tvEmpty.text = "No matches found."
            }
        } else {
            tvEmpty.visibility = View.GONE
        }

        adapter.submitList(finalFilteredList)
    }
}
