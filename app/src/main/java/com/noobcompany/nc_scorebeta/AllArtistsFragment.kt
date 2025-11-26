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

class AllArtistsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_artists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { 
            parentFragmentManager.popBackStack()
        }

        fetchArtists(view)
    }

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