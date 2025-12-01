package com.noobcompany.nc_scorebeta

/**
 * Data class representing a musical Artist.
 *
 * This class corresponds to the documents found in the "artists" collection within Firestore.
 * It contains profile information such as the artist's name, biography, and image URL.
 *
 * @property id The unique identifier for the artist document.
 * @property name The display name of the artist.
 * @property bio A textual biography providing details about the artist's career and background.
 * @property imageUrl The primary URL for the artist's profile image.
 * @property image A fallback URL for the artist's image, maintained for backward compatibility with older data records.
 */
data class Artist(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val imageUrl: String = "", // Primary
    val image: String = ""     // Fallback if field is named "image"
) {
    /**
     * Resolves the valid image URL for the artist.
     *
     * This utility function checks the primary `imageUrl` field first. If it is null or empty,
     * it attempts to use the fallback `image` field.
     *
     * @return A [String] containing the resolved image URL.
     */
    fun getSafeImage(): String {
        return if (imageUrl.isNotEmpty()) imageUrl else image
    }
}
