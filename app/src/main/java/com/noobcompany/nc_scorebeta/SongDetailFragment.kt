package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

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
     * - Configures the YouTubePlayerView for YouTube playback if a link is present.
     *
     * @param song The [Song] data object.
     */
    private fun updateUI(song: Song) {
        val view = view ?: return
        
        val ivDetailImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvDetailTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailArtist = view.findViewById<TextView>(R.id.tvDetailArtist)
        val tvLyrics = view.findViewById<TextView>(R.id.tvLyrics)
        val youtubePlayerView = view.findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        
        // Add lifecycle observer for YouTubePlayerView
        lifecycle.addObserver(youtubePlayerView)

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
            AppLogger.log("SongDetail", "Processing YouTube Link: '${song.youtubeLink}' with YouTubePlayerView")

            val videoId = extractVideoId(song.youtubeLink)
            AppLogger.log("SongDetail", "Extracted Video ID: '$videoId'")

            if (videoId.isNotEmpty()) {
                youtubePlayerView.visibility = View.VISIBLE
                youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f) // cueVideo will load the video but not autoplay
                    }
                })
            } else {
                AppLogger.error("SongDetail", "Failed to extract video ID from: ${song.youtubeLink}")
                youtubePlayerView.visibility = View.GONE
            }
        } else {
            youtubePlayerView.visibility = View.GONE
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
