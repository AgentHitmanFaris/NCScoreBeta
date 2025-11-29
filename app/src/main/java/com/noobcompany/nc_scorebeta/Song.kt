package com.noobcompany.nc_scorebeta

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a Song.
 *
 * This class maps to the "songs" collection in Firestore.
 *
 * @property id The unique document ID of the song (automatically populated by Firestore).
 * @property title The title of the song.
 * @property artistNames A list of artist names associated with the song.
 *                       Mapped to the "artistNames" field in Firestore.
 * @property albumCover The URL of the album cover image.
 * @property isPremium Indicates whether this song requires a premium subscription or login to view.
 * @property createdAt The timestamp when the song was added.
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

    // ADDED: This lets us sort by date!
    @com.google.firebase.firestore.ServerTimestamp
    var createdAt: Timestamp? = null,

    var lyrics: String = "",
    @get:PropertyName("video") @set:PropertyName("video")
    var youtubeLink: String = ""
) {
    /**
     * returns a formatted string of artist names.
     *
     * @return A comma-separated string of artist names, or "Unknown Artist" if the list is empty.
     */
    fun getFormattedArtist(): String {
        return if (artistNames.isNotEmpty()) {
            artistNames.joinToString(", ")
        } else {
            "Unknown Artist"
        }
    }
}
