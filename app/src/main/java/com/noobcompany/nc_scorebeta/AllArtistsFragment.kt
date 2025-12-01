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
 * Fragment that displays a comprehensive list of all artists in the database.
 *
 * This screen fetches the "artists" collection from Firestore and presents it in a vertical list.
 * It serves as the primary directory for users to find specific composers or performers.
 */
class AllArtistsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_artists, container, false)
    }

    /**
     * Called immediately after the view hierarchy has been created.
     *
     * Sets up the UI logic:
     * 1. Configures the "Back" button to pop the current fragment from the stack.
     * 2. Initiates the network request to fetch artist data.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
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
     * Asynchronously fetches the list of artists from Firestore.
     *
     * Shows a progress bar while loading. On success, populates the RecyclerView with an [ArtistAdapter].
     * On failure, hides the progress bar and displays an error toast.
     *
     * @param view The root view of the fragment.
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
