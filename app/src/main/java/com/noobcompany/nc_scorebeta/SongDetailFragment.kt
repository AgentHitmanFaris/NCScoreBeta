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
 * Fragment that displays detailed metadata and media content for a selected Song.
 *
 * This screen serves as the "landing page" for a piece of music. It presents:
 * - Album Cover Art.
 * - Song Title and Artist.
 * - Full Lyrics (if available).
 * - Embedded YouTube video (Performance or Tutorial).
 * - A primary call-to-action button to view the sheet music.
 */
class SongDetailFragment : Fragment() {

    private var songId: String? = null
    private var currentSong: Song? = null

    /**
     * Initializes the fragment.
     *
     * Retrieves the "SONG_ID" passed in the arguments bundle.
     *
     * @param savedInstanceState Saved state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songId = arguments?.getString("SONG_ID")
    }

    /**
     * Inflates the layout XML for the Song Detail screen.
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
        return inflater.inflate(R.layout.fragment_song_detail, container, false)
    }

    /**
     * Called immediately after the view is created.
     *
     * Sets up the toolbar back navigation, triggers the Firestore data load for the specific song ID,
     * and binds the "Open Score" button action.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState Saved state bundle.
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
                // Delegate opening logic to the central handler
                context?.let { ctx -> SongHandler.openScore(ctx, song) }
            } ?: run {
                Toast.makeText(context, "Song data not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Fetches the specific song document from the "songs" collection in Firestore.
     *
     * @param id The unique ID of the song to fetch.
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
     * Updates the UI components with data from the fetched [Song] object.
     *
     * - Sets text fields.
     * - Loads images using Glide.
     * - Configures the WebView for YouTube playback if a link is present.
     *
     * @param song The [Song] data object.
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

            webViewYoutube.settings.javaScriptEnabled = true // JavaScript enabled per user request
            webViewYoutube.settings.domStorageEnabled = true
            webViewYoutube.webChromeClient = WebChromeClient()
            webViewYoutube.webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    AppLogger.error("SongDetail", "WebView Error: ${error?.description} (Code: ${error?.errorCode})")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    // If the user clicks "Watch on YouTube", open in external app/browser
                    if (url.contains("youtube.com/watch") || url.contains("youtu.be")) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            view?.context?.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            AppLogger.error("SongDetail", "Failed to open external YouTube link", e)
                        }
                    }
                    return false
                }
            }
            
            val videoId = extractVideoId(song.youtubeLink)
            AppLogger.log("SongDetail", "Extracted Video ID: '$videoId'")

            if (videoId.isNotEmpty()) {
                // Generate a random 'si' parameter (Session Info)
                val si = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                val embedUrl = "https://www.youtube.com/embed/$videoId?si=$si"
                AppLogger.log("SongDetail", "Generated Embed URL: $embedUrl")

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:0;">
                        <iframe width="100%" height="100%" src="$embedUrl" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
                    </body>
                    </html>
                """.trimIndent()

                webViewYoutube.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            } else {
                AppLogger.error("SongDetail", "Failed to extract video ID from: ${song.youtubeLink}")
                webViewYoutube.visibility = View.GONE
            }
        } else {
            webViewYoutube.visibility = View.GONE
        }
    }

    /**
     * Extracts the 11-character YouTube Video ID from a given URL.
     *
     * Handles standard formats (watch?v=), short formats (youtu.be/), and embed formats.
     *
     * @param url The input YouTube URL.
     * @return The extracted Video ID, or an empty string if not found.
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
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        AppLogger.log("SongDetail", "extractVideoId: Using regex pattern: '$pattern'")
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(cleanUrl)
        
        return if (matcher.find()) {
            val rawId = matcher.group()
            // Ensure we strip any trailing query params if the regex missed them (e.g. ?si=...)
            val videoId = rawId.substringBefore('?')
            AppLogger.log("SongDetail", "extractVideoId: Regex found match: '$videoId' (from '$rawId')")
            videoId
        } else {
            AppLogger.log("SongDetail", "extractVideoId: Regex found no match.")
            "" // Return empty if no match found
        }
    }
}
