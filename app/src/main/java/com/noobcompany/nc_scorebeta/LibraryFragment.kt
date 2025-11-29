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
 * Fragment that displays the user's library of favorite songs.
 *
 * It retrieves the list of favorite song IDs from local preferences, fetches their details from Firestore,
 * and displays them in a grid. It also includes a search bar to filter the displayed favorites.
 */
class LibraryFragment : Fragment() {

    private lateinit var adapter: SongAdapter
    private var allSongsList: List<Song> = ArrayList()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

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
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned.
     *
     * Sets up the RecyclerView, search bar, and initiates the fetching of favorite songs.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
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
     * Called when the fragment is resumed.
     *
     * Re-applies the search filter to ensure the list is up-to-date with any text in the search bar.
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
     * Sets up the RecyclerView with a grid layout.
     *
     * Configures the adapter with click listeners for song selection and artist navigation.
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
     * Sets up the search bar to filter the favorite songs list locally.
     *
     * Adds a TextWatcher to the search EditText to trigger filtering on text changes.
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
     * Fetches the details of favorite songs from Firestore.
     *
     * It retrieves the list of favorite IDs from [FavoritesManager] and then queries Firestore using 'in' queries.
     * Because Firestore limits 'in' queries to 10 items, it batches the requests.
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
     * Filters the locally stored list of favorite songs based on the search query.
     *
     * The filter matches against the song title and formatted artist string (case-insensitive).
     * It updates the adapter with the filtered list and toggles the empty view visibility.
     *
     * @param query The search string.
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
