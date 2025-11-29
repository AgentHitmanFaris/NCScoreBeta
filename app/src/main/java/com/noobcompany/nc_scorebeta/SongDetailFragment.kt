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

/**
 * Fragment that displays detailed information about a specific song.
 *
 * It shows the song's cover art, title, artist, lyrics, and an embedded YouTube video if available.
 * It also provides a button to open the sheet music (score).
 */
class SongDetailFragment : Fragment() {

    private var songId: String? = null
    private var currentSong: Song? = null

    /**
     * Called to do initial creation of a fragment.
     *
     * Retrieves the song ID from the arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songId = arguments?.getString("SONG_ID")
    }

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
        return inflater.inflate(R.layout.fragment_song_detail, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned.
     *
     * Sets up the toolbar navigation, loads song data if an ID is present, and configures the "Open Score" button.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Fetches the song data from Firestore using the provided ID.
     *
     * @param id The ID of the song to fetch.
     */
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

    /**
     * Updates the UI elements with the details of the fetched song.
     *
     * Sets the title, artist, cover image, lyrics, and configures the YouTube WebView.
     *
     * @param song The [Song] object containing the data.
     */
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
            
            val finalEmbedUrl: String
            
            // Check if the link is already an embed URL (e.g., youtube.com/embed/ or youtube-nocookie.com/embed/)
            if (song.youtubeLink.contains("/embed/")) {
                AppLogger.log("SongDetail", "Link is already an embed URL. Using as-is.")
                finalEmbedUrl = song.youtubeLink.trim()
            } else {
                val videoId = extractVideoId(song.youtubeLink)
                AppLogger.log("SongDetail", "Extracted Video ID: '$videoId'")
                
                if (videoId.isNotEmpty()) {
                    finalEmbedUrl = "https://www.youtube.com/embed/$videoId"
                } else {
                    AppLogger.error("SongDetail", "Failed to extract video ID from: ${song.youtubeLink}")
                    webViewYoutube.visibility = View.GONE
                    return // Exit if we can't get an embed URL
                }
            }
            
            AppLogger.log("SongDetail", "Final Embed URL for WebView: $finalEmbedUrl")

            val html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;">
                    <iframe width="100%" height="100%" src="$finalEmbedUrl" frameborder="0" allowfullscreen></iframe>
                </body>
                </html>
            """.trimIndent()
            
            webViewYoutube.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
        } else {
            webViewYoutube.visibility = View.GONE
        }
    }

    /**
     * Extracts the YouTube video ID from a given URL.
     *
     * It handles various YouTube URL formats (watch?v=, youtu.be, embed/, etc.) as well as raw video IDs.
     *
     * @param url The YouTube URL or ID.
     * @return The extracted video ID, or an empty string if not found.
     */
    private fun extractVideoId(url: String): String {
        val cleanUrl = url.trim()
        AppLogger.log("SongDetail", "extractVideoId: Cleaned URL: '$cleanUrl'")
        
        // Case 1: Raw Video ID (11 chars, alphanumeric + _ -)
        if (cleanUrl.length == 11 && cleanUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            AppLogger.log("SongDetail", "extractVideoId: Identified as raw video ID.")
            return cleanUrl
        }

        // Case 2: Extract from URL using Regex
        // Matches: youtube.com/watch?v=ID, youtu.be/ID, youtube.com/embed/ID, etc.
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        AppLogger.log("SongDetail", "extractVideoId: Using regex pattern: '$pattern'")
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(cleanUrl)
        
        return if (matcher.find()) {
            val videoId = matcher.group()
            AppLogger.log("SongDetail", "extractVideoId: Regex found match: '$videoId'")
            videoId
        } else {
            AppLogger.log("SongDetail", "extractVideoId: Regex found no match.")
            "" // Return empty if no match found
        }
    }
}
