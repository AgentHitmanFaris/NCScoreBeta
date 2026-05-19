package com.noobcompany.nc_scorebeta

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a Song entity.
 *
 * This class serves as the data model for song documents stored in the "songs" collection of Firestore.
 * It encapsulates all essential metadata about a musical piece, including its title, associated artists,
 * media links (album cover, YouTube video), and access rights (premium status).
 *
 * @property id The unique identifier for the song document. This field is automatically populated by Firestore's @DocumentId.
 * @property title The title of the song.
 * @property artistNames A list containing the names of all artists performing or composing the song. This maps to the "artistNames" field in the database.
 * @property albumCover The HTTP URL pointing to the album art image for the song.
 * @property isPremium A boolean flag indicating if the song is restricted to premium users. Defaults to false.
 * @property createdAt The server-generated timestamp recording when the song was added. Used for sorting new releases.
 * @property lyrics The full text of the song's lyrics. May be empty if not available.
 * @property youtubeLink The URL or unique ID of a YouTube video associated with the song (e.g., a performance or tutorial). Mapped to "video".
 * @property midiUrl URL to the MIDI version of the song for interactive playback.
 * @property musicXmlUrl URL to the MusicXML version of the song for dynamic rendering.
 * @property backingTrackUrl URL to an audio accompaniment track.
 * @property difficulty A descriptor of the song's playing difficulty (e.g., "Easy", "Medium", "Hard").
 */
data class Song(
    @DocumentId
    val id: String = "",

    var title: String = "",

    @get:PropertyName("artistNames") @set:PropertyName("artistNames")
    var artistNames: List<String> = emptyList(),

    var albumCover: String = "",

    @field:JvmField
    var isPremium: Boolean = false,

    @get:PropertyName("dateAdded") @set:PropertyName("dateAdded")
    @com.google.firebase.firestore.ServerTimestamp
    var createdAt: Timestamp? = null,

    var lyrics: String = "",
    @get:PropertyName("video") @set:PropertyName("video")
    var youtubeLink: String = "",

    var midiUrl: String = "",
    var musicXmlUrl: String = "",
    var backingTrackUrl: String = "",
    
    var artistIds: List<String> = emptyList(),
    var originalKey: String = "",
    var bpm: Int = 0,
    
    @field:JvmField
    var isFeatured: Boolean = false,
    
    @field:JvmField
    var isComingSoon: Boolean = false
) {
    /**
     * Helper function to generate a human-readable string of artist names.
     *
     * It concatenates the list of artist names into a single string separated by commas.
     *
     * @return A single [String] containing all artist names, or "Unknown Artist" if the list is empty.
     */
    fun getFormattedArtist(): String {
        return if (artistNames.isNotEmpty()) {
            artistNames.joinToString(", ")
        } else {
            "Unknown Artist"
        }
    }
}
