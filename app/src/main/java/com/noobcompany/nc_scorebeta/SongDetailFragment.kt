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
            .addOnFailureListener { e ->
                AppLogger.error("SongDetail", "Error loading song", e)
                Toast.makeText(context, "Error loading song: ${e.message}", Toast.LENGTH_SHORT).show()
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
        if (song.youtubeLink.isNotBlank()) {
            AppLogger.log("SongDetail", "Processing YouTube Link: '${song.youtubeLink}'")

            webViewYoutube.settings.javaScriptEnabled = true
            webViewYoutube.settings.domStorageEnabled = true
            webViewYoutube.webChromeClient = WebChromeClient()
            webViewYoutube.webViewClient = WebViewClient()
            
            val videoId = extractVideoId(song.youtubeLink)
            AppLogger.log("SongDetail", "Extracted Video ID: '$videoId'")
            
            if (videoId.isNotEmpty()) {
                val embedUrl = "https://www.youtube.com/embed/$videoId"
                AppLogger.log("SongDetail", "Final Embed URL: $embedUrl")
                
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:0;">
                        <iframe width="100%" height="100%" src="$embedUrl" frameborder="0" allowfullscreen></iframe>
                    </body>
                    </html>
                """.trimIndent()
                
                webViewYoutube.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            } else {
                 AppLogger.error("SongDetail", "Failed to extract video ID from: ${song.youtubeLink}")
                 // Fallback if extraction failed but link exists
                 webViewYoutube.visibility = View.GONE
            }
        } else {
            webViewYoutube.visibility = View.GONE
        }
    }

    private fun extractVideoId(url: String): String {
        val cleanUrl = url.trim()
        
        // Case 1: Raw Video ID (11 chars, alphanumeric + _ -)
        if (cleanUrl.length == 11 && cleanUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return cleanUrl
        }

        // Case 2: Extract from URL using Regex
        // Matches: youtube.com/watch?v=ID, youtu.be/ID, youtube.com/embed/ID
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(cleanUrl)
        
        return if (matcher.find()) {
            matcher.group()
        } else {
            "" // Return empty if no match found
        }
    }
}
