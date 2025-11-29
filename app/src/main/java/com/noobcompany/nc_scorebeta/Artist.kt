package com.noobcompany.nc_scorebeta

/**
 * Data class representing an Artist.
 *
 * This class maps to the "artists" collection in Firestore and holds information about a specific artist.
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
     * Retrieves a valid image URL for the artist.
     *
     * It checks the primary `imageUrl` first. If it is empty, it falls back to the `image` property.
     *
     * @return The determined URL of the artist's image as a [String].
     */
    fun getSafeImage(): String {
        return if (imageUrl.isNotEmpty()) imageUrl else image
    }
}
