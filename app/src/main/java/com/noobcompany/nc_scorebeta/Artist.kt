package com.noobcompany.nc_scorebeta

/**
 * Data class representing an Artist.
 *
 * This class maps to the "artists" collection in Firestore.
 *
 * @property id The unique identifier for the artist.
 * @property name The name of the artist.
 * @property bio A biography or description of the artist.
 * @property imageUrl The primary URL for the artist's image.
 * @property image A fallback URL for the artist's image, used if `imageUrl` is empty or if the field is named "image".
 */
data class Artist(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val imageUrl: String = "", // Primary
    val image: String = ""     // Fallback if field is named "image"
) {
    /**
     * Returns a valid image URL for the artist.
     *
     * Checks `imageUrl` first, and falls back to `image` if `imageUrl` is empty.
     *
     * @return The URL of the artist's image.
     */
    fun getSafeImage(): String {
        return if (imageUrl.isNotEmpty()) imageUrl else image
    }
}
