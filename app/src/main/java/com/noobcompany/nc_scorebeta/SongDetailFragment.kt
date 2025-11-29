package com.noobcompany.nc_scorebeta

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class SongDetailFragment : Fragment() {

    private var songId: String? = null
    private var currentSong: Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songId = arguments?.getString("SONG_ID")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_song_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (songId != null) {
            loadSongData(songId!!)
        }

        val btnOpenScore = view.findViewById<Button>(R.id.btnOpenScore)
        btnOpenScore.setOnClickListener {
            currentSong?.let { song ->
                // Use SongHandler's logic to open the score
                context?.let { ctx -> SongHandler.openScore(ctx, song) }
            } ?: run {
                Toast.makeText(context, "Song data not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSongData(id: String) {
        FirebaseFirestore.getInstance().collection("songs").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val song = document.toObject(Song::class.java)
                    currentSong = song
                    if (song != null) {
                        updateUI(song)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading song", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun updateUI(song: Song) {
        val view = view ?: return
        
        val ivDetailImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvDetailTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailArtist = view.findViewById<TextView>(R.id.tvDetailArtist)
        val tvLyrics = view.findViewById<TextView>(R.id.tvLyrics)
        val webViewYoutube = view.findViewById<WebView>(R.id.webViewYoutube)

        tvDetailTitle.text = song.title
        tvDetailArtist.text = song.getFormattedArtist()
        
        Glide.with(this).load(song.albumCover).into(ivDetailImage)

        if (song.lyrics.isNotEmpty()) {
            tvLyrics.text = song.lyrics.replace("\\n", "\n")
        } else {
            tvLyrics.text = "No lyrics available."
        }

        // Setup YouTube Embed
        if (song.youtubeLink.isNotEmpty()) {
            webViewYoutube.settings.javaScriptEnabled = true
            webViewYoutube.settings.domStorageEnabled = true // Required for some players
            webViewYoutube.webChromeClient = WebChromeClient()
            webViewYoutube.webViewClient = WebViewClient()
            
            val embedUrl = getEmbedUrl(song.youtubeLink)
            // Autoplay enabled in URL parameters (Note: Mobile browsers often block autoplay with sound, but let's try)
            val autoplayUrl = "$embedUrl?autoplay=1"
            
            val html = """
                <iframe width="100%" height="100%" src="$autoplayUrl" frameborder="0" allow="autoplay; encrypted-media" allowfullscreen></iframe>
            """.trimIndent()
            
            webViewYoutube.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
        } else {
            webViewYoutube.visibility = View.GONE
        }
    }

    private fun getEmbedUrl(url: String): String {
        // Simple logic to convert standard YouTube URL to embed URL
        // https://www.youtube.com/watch?v=VIDEO_ID -> https://www.youtube.com/embed/VIDEO_ID
        // https://youtu.be/VIDEO_ID -> https://www.youtube.com/embed/VIDEO_ID
        
        var videoId = ""
        if (url.contains("v=")) {
            videoId = url.split("v=")[1].split("&")[0]
        } else if (url.contains("youtu.be/")) {
            videoId = url.split("youtu.be/")[1].split("?")[0]
        }
        
        return if (videoId.isNotEmpty()) "https://www.youtube.com/embed/$videoId" else url
    }
}
