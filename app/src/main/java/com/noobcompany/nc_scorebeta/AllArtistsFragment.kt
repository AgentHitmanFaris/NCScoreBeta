package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment that displays a list of all artists available in the application.
 *
 * This fragment fetches artist data from Firestore and displays it in a RecyclerView.
 * It also handles navigation to the [ArtistDetailFragment] when an artist is selected.
 */
class AllArtistsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

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
        return inflater.inflate(R.layout.fragment_all_artists, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been restored in to the view.
     * Sets up the back button and initiates fetching of artists.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { 
            parentFragmentManager.popBackStack()
        }

        fetchArtists(view)
    }

    /**
     * Fetches the list of artists from Firestore and populates the RecyclerView.
     *
     * It shows a progress bar while loading and handles potential errors by displaying a Toast.
     * On successful fetch, it sets up the [ArtistAdapter] with the retrieved data.
     *
     * @param view The root view of the fragment, used to find UI elements.
     */
    private fun fetchArtists(view: View) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val rvArtists = view.findViewById<RecyclerView>(R.id.rvArtists)
        rvArtists.layoutManager = LinearLayoutManager(context)

        db.collection("artists").get()
            .addOnSuccessListener { result ->
                val artists = result.toObjects(Artist::class.java)
                rvArtists.adapter = ArtistAdapter(artists) { artist ->
                    // Navigate to Detail
                    val fragment = ArtistDetailFragment()
                    val args = Bundle()
                    args.putString("ARTIST_NAME", artist.name)
                    args.putString("ARTIST_ID", artist.id) // Pass ID if available
                    args.putString("ARTIST_BIO", artist.bio)
                    args.putString("ARTIST_IMAGE", artist.imageUrl)
                    fragment.arguments = args

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading artists", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }
}
