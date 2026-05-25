package com.noobcompany.nc_scorebeta

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class BrowseFragment : Fragment() {

    private lateinit var songAdapter: SongAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private val db = FirebaseFirestore.getInstance()
    private var lastVisible: DocumentSnapshot? = null
    private var isScrolling = false
    private var isLastItemReached = false
    private var currentQueryStr = ""

    private val displayedSongs: ArrayList<Song> = ArrayList()
    private var searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        setupSearch(view)
        
        // Hide keyboard when touching the background or RecyclerView
        view.setOnClickListener { hideKeyboard() }
        
        loadSongs(true)
    }

    private fun setupRecyclerView(view: View) {
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvSongs)
        val rvArtists = view.findViewById<RecyclerView>(R.id.rvArtists)
        val layoutManager = GridLayoutManager(context, 2)
        rvSongs.layoutManager = layoutManager
        rvArtists.layoutManager = LinearLayoutManager(context)
        
        songAdapter = SongAdapter(useGrid = true, onSongClicked = { song ->
            hideKeyboard()
            context?.let { SongHandler.onSongClicked(it, song) }
        }, onArtistClicked = { artistName ->
            hideKeyboard()
            navigateToArtist(artistName)
        })
        rvSongs.adapter = songAdapter

        // Correctly initialize ArtistAdapter without a list in the constructor
        artistAdapter = ArtistAdapter { artist ->
            hideKeyboard()
            navigateToArtist(artist.name)
        }
        rvArtists.adapter = artistAdapter

        rvSongs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true
                    hideKeyboard() // Hide keyboard on scroll
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (isScrolling && (visibleItemCount + firstVisibleItemPosition == totalItemCount) && !isLastItemReached) {
                    isScrolling = false
                    if (currentQueryStr.isEmpty()) {
                        loadSongs(false)
                    }
                }
            }
        })
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

    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query != currentQueryStr) {
                    currentQueryStr = query
                    
                    // DEBOUNCE: Wait 300ms after last keystroke before searching
                    searchRunnable?.let { searchHandler.removeCallbacks(it) }
                    searchRunnable = Runnable { performSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, 300)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

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
                // RACE CONDITION FIX: If user has started searching, don't overwrite with default list
                if (currentQueryStr.isNotEmpty()) return@addOnSuccessListener

                if (isInitial) {
                    displayedSongs.clear()
                }
                
                if (!result.isEmpty) {
                    lastVisible = result.documents[result.size() - 1]
                    val newSongs = result.toObjects(Song::class.java)
                    displayedSongs.addAll(newSongs)
                    songAdapter.submitList(ArrayList(displayedSongs)) 
                    
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

    private fun performSearch(query: String) {
        val tvArtistsTitle = view?.findViewById<TextView>(R.id.tvArtistsTitle)
        val rvArtists = view?.findViewById<RecyclerView>(R.id.rvArtists)

        if (query.isEmpty()) {
            isLastItemReached = false
            lastVisible = null
            tvArtistsTitle?.visibility = View.GONE
            rvArtists?.visibility = View.GONE
            artistAdapter.submitList(emptyList())
            loadSongs(true)
            return
        }

        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
        progressBar?.visibility = View.VISIBLE

        // Artist Query
        db.collection("artists")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { artistResults ->
                val artists = artistResults.toObjects(Artist::class.java)
                artistAdapter.submitList(artists)
                if (artists.isNotEmpty()) {
                    tvArtistsTitle?.visibility = View.VISIBLE
                    rvArtists?.visibility = View.VISIBLE
                } else {
                    tvArtistsTitle?.visibility = View.GONE
                    rvArtists?.visibility = View.GONE
                }
            }

        // Song Queries - We run multiple variations to overcome case-sensitivity
        val queryLower = query.lowercase()
        val queryCap = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val tasks = mutableListOf<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>>()
        
        // 1. Original Case
        tasks.add(db.collection("songs").orderBy("title").startAt(query).endAt(query + "\uf8ff").limit(20).get())
        // 2. Lowercase
        if (queryLower != query) {
            tasks.add(db.collection("songs").orderBy("title").startAt(queryLower).endAt(queryLower + "\uf8ff").limit(20).get())
        }
        // 3. Capitalized
        if (queryCap != query && queryCap != queryLower) {
            tasks.add(db.collection("songs").orderBy("title").startAt(queryCap).endAt(queryCap + "\uf8ff").limit(20).get())
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
            .addOnSuccessListener { completedTasks ->
                val mergedList = ArrayList<Song>()
                val seenIds = HashSet<String>()

                for (task in completedTasks) {
                    if (task.isSuccessful) {
                        val result = task.result as com.google.firebase.firestore.QuerySnapshot
                        for (doc in result.documents) {
                            val song = doc.toObject(Song::class.java)
                            if (song != null && seenIds.add(song.id)) {
                                mergedList.add(song)
                            }
                        }
                    }
                }
                
                // If we found nothing by title, try a fallback on artistNames array
                // We do this separately because whereArrayContains is very strict
                if (mergedList.isEmpty()) {
                    db.collection("songs")
                        .whereArrayContains("artistNames", query)
                        .limit(20)
                        .get()
                        .addOnSuccessListener { artistResult ->
                            for (doc in artistResult.documents) {
                                val song = doc.toObject(Song::class.java)
                                if (song != null && seenIds.add(song.id)) {
                                    mergedList.add(song)
                                }
                            }
                            songAdapter.submitList(mergedList)
                            progressBar?.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            songAdapter.submitList(mergedList)
                            progressBar?.visibility = View.GONE
                        }
                } else {
                    songAdapter.submitList(mergedList)
                    progressBar?.visibility = View.GONE
                }
                
                isLastItemReached = true 
            }
            .addOnFailureListener { e ->
                android.util.Log.e("BrowseFragment", "Search Error", e)
                Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                progressBar?.visibility = View.GONE
            }
    }

    /**
     * Helper to hide the soft keyboard.
     */
    private fun hideKeyboard() {
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
