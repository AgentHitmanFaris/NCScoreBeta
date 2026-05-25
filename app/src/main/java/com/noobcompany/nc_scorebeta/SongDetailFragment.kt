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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Fragment that displays detailed metadata and media content for a selected Song.
 *
 * Optimized with Coroutines for high-performance parallel data fetching.
 */
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
        toolbar?.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (songId != null) {
            fetchFullSongDetails(songId!!)
        }

        val btnOpenScore = view.findViewById<Button>(R.id.btnOpenScore)
        btnOpenScore?.setOnClickListener {
            currentSong?.let { song ->
                context?.let { ctx -> SongHandler.openScore(ctx, song) }
            } ?: run {
                Toast.makeText(context, "Song data not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Parallel fetch for Song details and Arrangements using Kotlin Coroutines.
     */
    private fun fetchFullSongDetails(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AppLogger.log("SongDetail", "Fetching details for ID: $id")
                
                val db = FirebaseFirestore.getInstance()
                
                // Fetch Song and its Arrangements simultaneously
                val songDeferred = async { db.collection("songs").document(id).get().await() }
                val arrangementDeferred = async {
                    db.collection("songs").document(id).collection("arrangements").get().await()
                }

                val songDoc = songDeferred.await()
                val arrangementSnapshot = arrangementDeferred.await()

                if (songDoc.exists()) {
                    val song = songDoc.toObject(Song::class.java)
                    currentSong = song
                    song?.let { updateUI(it) }
                }

                // Handle Arrangement-specific data (Difficulty)
                if (!arrangementSnapshot.isEmpty) {
                    val arrangement = arrangementSnapshot.documents[0].toObject(Arrangement::class.java)
                    val view = view ?: return@launch
                    val tvDifficulty = view.findViewById<TextView>(R.id.tvDifficulty)
                    
                    if (arrangement != null && arrangement.difficulty.isNotBlank()) {
                        tvDifficulty?.text = arrangement.difficulty
                    }
                }

            } catch (e: Exception) {
                AppLogger.error("SongDetail", "Error loading details", e)
                Toast.makeText(context, "Error loading song details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Binds Song model data to UI views.
     */
    private fun updateUI(song: Song) {
        val view = view ?: return
        
        val ivDetailImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvDetailTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailArtist = view.findViewById<TextView>(R.id.tvDetailArtist)
        val tvBpm = view.findViewById<TextView>(R.id.tvBpm)
        val tvKey = view.findViewById<TextView>(R.id.tvKey)
        val tvLyrics = view.findViewById<TextView>(R.id.tvLyrics)
        val youtubePlayerView = view.findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        
        // SAFE OBSERVER: Bind YouTube player to View lifecycle to prevent leaks and crashes
        youtubePlayerView?.let { 
            viewLifecycleOwner.lifecycle.addObserver(it)
        }

        tvDetailTitle?.text = song.title
        tvDetailArtist?.text = song.getFormattedArtist()
        
        // Metadata binding
        tvBpm?.text = if ((song.bpm ?: 0) > 0) song.bpm.toString() else "--"
        tvKey?.text = if (song.originalKey.isNotBlank()) song.originalKey else "--"
        
        ivDetailImage?.let {
            Glide.with(this).load(song.albumCover).into(it)
        }

        if (song.lyrics.isNotEmpty()) {
            tvLyrics?.text = song.lyrics.replace("\\n", "\n")
        } else {
            tvLyrics?.text = "No lyrics available."
        }

        // YouTube setup
        if (song.youtubeLink.isNotBlank() && youtubePlayerView != null) {
            val videoId = extractVideoId(song.youtubeLink)
            if (videoId.isNotEmpty()) {
                youtubePlayerView.visibility = View.VISIBLE
                
                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                }

                // Fix for Error 152-4: The library now handles origin automatically in v13.0.0
                // Just use the standard addYouTubePlayerListener which is safer with the View lifecycle
                youtubePlayerView.addYouTubePlayerListener(listener)
            } else {
                youtubePlayerView.visibility = View.GONE
            }
        } else {
            youtubePlayerView?.visibility = View.GONE
        }
    }

    private fun extractVideoId(url: String): String {
        val cleanUrl = url.trim()
        if (cleanUrl.length == 11 && cleanUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return cleanUrl

        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(cleanUrl)
        
        return if (matcher.find()) {
            matcher.group().substringBefore('?')
        } else ""
    }
}
